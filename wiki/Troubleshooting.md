# 排错

按「症状 → 原因 → 处理」组织。先确认一件事：**本附属的日志前缀是 `mmcr_nekojs`**，搜这个就能过滤出相关问题。

## 游戏起不来

### `The following mods have version differences that were not resolved: mmcr_nekojs (version 0.1.1 -> 0.1.2)`

**不是错误，可以忽略。** 这是 NeoForge 在告诉你「这个存档上次是用 0.1.1 打开的，现在是 0.1.2」。版本号来自 `level.dat` 里记录的 mod 列表，不是你装错了。

只有当你**降级**（0.1.2 → 0.1.1）并且新版加了世界内容时，才需要留意。升版本时这条警告无害。

### `File mods/mmcr_nekojs-xxx.jar is not a valid mod file`

`neoforge.mods.toml` 语法错误。最常见的是把两个键值对写在一行：

```toml
# 错
modId="neoforge" type="required" versionRange="[21.1.238,)"
```

TOML 规定一个 key 只能独占一行。从 v0.1.1 起 CI 会先跑一遍 `tomllib` 校验，理论上不会再有这个问题；如果自己改过 toml，检查这一条。

### `Missing or unsupported mandatory dependencies`

缺依赖，或者版本不满足。本附属要求：

| 依赖 | 最低版本 |
|---|---|
| `neoforge` | 21.1.238（1.21.1） / 26.1.2.99（26.1.2） |
| `nekojs` | 1.1.0 |
| `mmcr` | 0.1.7 |

装错 MC 版本的 jar（把 `-26.1.2-` 装到 1.21.1 上）也报这个。

### 崩溃提到 `graal.graalvm.polyglot`

缺 **GraalMC**。NekoJS 把 Graal 排除在自己的 jar 外，必须单独装 `graalmc-neoforge-25.1.3.7.jar`。

### 启动后没有 `nekojs/` 目录

NekoJS 本身没加载成功。先单独排查 NekoJS，本附属无从谈起。

## 脚本没生效

### 日志里 `startup declarations: 0 machine(s)`

机器一个都没注册上。按顺序查：

1. 文件在 `nekojs/startup_scripts/` 里吗？（不是 `kubejs/`）
2. 包在 `MMCREvents.startup(event => { ... })` 里吗？
3. 调了 `.register()` 吗？
4. `/nekojs error` 有报错吗？

第四步最直接——脚本抛异常会被 NekoJS 捕获，不崩游戏，但整个脚本会被标记为 disabled。

### 日志红字：`binding-preflight unknown-identifier pattern`

```
Script binding-preflight unknown-identifier pattern callback exception:
环境: STARTUP
位置: startup_scripts/src/xxx.js:25:6
原因: Unknown identifier 'pattern'.
```

**不崩游戏，脚本照常运行，结构照样注册**，但它会在日志里刷一片红，很容易被当成脚本坏了。

原因不在本附属：NekoJS 在脚本执行前会跑一遍静态预检（`GlobalBindingMemberValidator`），用的是它自己那个轻量解析器
`ValParser`。该解析器在**链式调用的每一级之间只跳过空白、不跳过注释**（`ValParser.parseSuffix` 里用的是
`skipWs()` 而不是 `skipWsCmt()`），所以只要链中间夹了注释，它就把一条链断成两句：

```js
event.createStructure('my_mod:smelter')
  // ← 这一行注释就足以断链
  .pattern('BBB', 'BCB', 'BBB')
```

断链之后 `.pattern(...)` 被当成独立语句，`pattern` 变成"调用了一个叫 pattern 的全局函数"，于是报
`Unknown identifier 'pattern'`。行尾注释同样致命：

```js
.set('B', api.block('minecraft:iron_block'))   // ← 这一行注释也会断链
.controller('C')
```

**规避办法：链式调用中间不写注释。** 解释写在整条链的上方，或写进 `pattern(...)` 的实参内部（不是最后一个实参的行尾）：

```js
// 三片：z=0 / z=1（控制器居中）/ z=2
event.createStructure('my_mod:smelter')
  .pattern('BBB', 'BCB', 'BBB')
  .pattern('BHB', 'HCH', 'BHB')
  .pattern('BBB', 'BCB', 'BBB')
  .set('B', api.block('minecraft:iron_block'))
  .controller('C')
  .build()
```

真要根治得等上游：把 `nekojs` 的 `common/src/main/java/com/tkisor/nekojs/core/compiler/ValParser.java`
里 `parseSuffix()` 开头的 `skipWs()` 改成 `skipWsCmt()` 即可（一行）。

### Python 脚本刷一屏 `binding-preflight unknown-identifier`（每个局部变量都报）

```
位置: startup_scripts/src/examples/01_space_elevator.py:21:5
原因: Unknown identifier 'declare_machines'.
 > 21 | def declare_machines(event):
```

整屏都是 `def` 名、参数名、局部变量名，甚至中文注释里的一串字被当成标识符。
**脚本其实跑成功了**（日志末尾有脚本自己 `print` 的内容），这是 NekoJS **预览版**预检的误报：

- 预检用它自己的轻量 JS 解析器 `ValParser` 分析源码，而 Python 是由 `PythonToJsCompiler` 转译成 JS 之后才执行的；
- 预览版在转译语言上没有把「按原始源码校验」切成「按转译产物校验」，于是 `def`、`#` 注释、缩进全部被 JS 解析器
  碎成一堆伪调用/伪标识符。

上游**只修了编译路径**：主分支的 `NekoModulePipeline.prepareStages()` 会用
`rawPreflightApplies(extension)` 判断，`.py/.ts` 改在校验转译产物；但**入口脚本那条校验路径没有门控**——
`ScriptExecutor.validateGlobalBindings` → `NekoModulePipelineCache.validateEntryBindings` →
`ScriptSourcePreflight.validate` 仍然是拿原始源码直接喂给 `ValParser`（预览版更早的调用点也一样）。
所以升级到主分支编译的版本，这一屏红字依然会出现。已向上游提 issue。

**没有开关能关掉它。** `engine.toml` 里的 `scriptMemberValidation = false` 只 gate 了
`NekoModulePipeline` 那条编译期路径；上面这条入口路径从不读配置（预览版的
`validateGlobalBindings` 里连 config 字段都没访问过，字节码可查），所以置 `false` 之后红字照旧。
只能等上游把 `rawPreflightApplies` 也用到入口路径上。

因此建议把这个开关**保持 `true`**：对 `.py` 它校验的是转译产物（有效检查，能抓到
`api.anyOfItemInput()` 这类把 KubeJS 名字搬过来写错的情况），对 `.js` 校验原始源码。
那些红字是 `ERROR` 级别但脚本照常执行，判断脚本有没有真的跑起来，只看脚本自己的 `print`
和 `MMCR/NekoJS … declarations` 那几行。

### `.py` 报 `Duplicate local ESM binding 'xxx'`

```
[NEKO-1003] 脚本执行失败: nekojs:startup/src/examples/02_space_elevator_structure.py
原因: Duplicate local ESM binding 'z_slice'
```

这次是真的失败：`NekoEsmLinker` 会把 `var` 绑定归到最近的**函数**作用域再查重，同一个函数里两个
`for ... in ...` 用同一个循环变量名就会撞车——虽然 JS 本身允许 `var` 重复声明。

Python 里给两个循环换不同的变量名即可：

```python
for lift_slice in ELEVATOR:
    lift.pattern(*lift_slice)

for module_slice in REASSEMBLER:
    module_machine.pattern(*module_slice)
```

同理，别在一个脚本里把同一个名字当循环变量用两次。

### 报 `Unbound pattern symbol` / 只有单个字母的错误（`原因: L`）

```
Script event mode=normal bus=...StartupEventJS ... callback exception:
位置: startup_scripts/src/x.js:118:5
原因: L
```

MMCR 要求 pattern 里出现的**每个符号都有谓词**。三种常见来源：

1. **等级槽 / 修饰符槽的等级还没声明**。`set('L', event.levelSlot('my_mod:coil'))` 那一格，
   谓词是拿「该类型已声明的所有等级」拼出来的，所以 `createLevelType` / `createLevel` 必须写在
   这条结构**之前**（同一个 startup 回调里，靠上）。0.1.6 起会直接报一句可读的错，
   0.1.5 及更早只会甩一个字母。
2. **修饰符槽没给摆放方块**。`event.modifierSlot(id)` 只声明了修饰符，那一格放什么不知道——
   要写成 `event.modifierSlot(id, api.block('...'))`。
3. **符号拼错了**：pattern 里的字符和 `set(...)` 的字符不一致（大小写也算）。

### `Binding 'RegistryEvents' has no member 'register'`

这是 NekoJS 自己的注册 API 版本差异，跟本附属无关：

| NekoJS 版本 | 写法 |
|---|---|
| preview3（1.1.0-preview3） | `RegistryEvents.block(event => event.create('ns:id', b => { b.hardness(4.0) }))`，事件名是 `item` / `block` / `fluid` / `entityType` … |
| 新版本 | `RegistryEvents.register(event => event.block('ns:id', b => { b.hardness = 4.0 }))`，字段是 bean 属性 |

照你装的 NekoJS 版本的 wiki 抄，别跨版本混用。示例脚本因此不注册自定义方块，直接用原版铁块当外壳。

### 崩溃：`Unknown machine definition: xxx:yyy` / `RegistrationException`

```
net.neoforged.fml.ModLoadingException: Loading errors encountered:
  - Modular Machinery CR (mmcr) encountered an error while dispatching FMLCommonSetupEvent
    cn.howxu.mmcr.publicapi.registration.RegistrationException: Unknown machine definition: my_mod:smelter
```

意思是：结构递上去了，但 MMCR 那边没有同名机器。0.1.2 及更早的桥接会在这里直接崩，0.1.3 起只跳过并报错。

对照日志按顺序看：

1. 有没有 `MMCR/NekoJS startup declarations: N machine(s)`？`N = 0` → 先看上一节。
2. 有没有 `MMCR/NekoJS submitted N machine definition(s) through a reopened window`？
   - **有**：机器已进 MMCR，接下来正常。
   - **没有，且有 `could not replay the definition window`**：MMCR 内部注册 API 变了，升级 bridge 或临时加
     `-Dmmcrnekojs.disableStartupReplay=true` 让启动至少不崩（代价是脚本机器不存在）。
3. 末尾若有 `MMCR/NekoJS skipped N declaration(s) that MMCR refused`，列表里会直接写出哪个 id 被拒。

机器 id 必须和结构 id **完全一致**（`createMachine('a:b')` ↔ `createStructure('a:b')`），大小写、命名空间都算。

### 配方脚本报 `Unknown identifier: id`

```
TypeError: invokeMember (id) on com.google.gson.JsonObject failed due to: Unknown identifier: id
```

**0.1.3 及更早的 bug**：`event.recipes.mmcr.machine_recipe(...)` 只构造了 JSON 就把裸的 Gson `JsonObject` 返回给脚本，
既没有真正注册配方，也没有 `.id(...)` 这个方法——所以链式 `.id()` 直接炸，而那个 `id` 就是方法名，不是字段名。

0.1.4 起改为走 `RecipeEventJS.custom(JsonObject)`：注册配方并返回 `RecipeJsonBuilder`，`.id('...')` 正常。升级即可，脚本不用改。

顺便记一下另一个坑：**不要**把 JS 对象字面量直接交给 `event.custom({...})`。
`RecipeEventJS.custom` 同时有 `custom(RecipeJsonValue)` 和 `custom(JsonObject)` 两个重载，GraalJS 无法在两者之间选，
会报 `Multiple applicable overloads found for method name custom`。用 schema 形式
（`event.recipes.mmcr.machine_recipe(...)`）就不会有这个问题。

### 进世界/开 JEI 时崩：`InjectionError ... mmcr$replaceRecipeBackground`

```
org.spongepowered.asm.mixin.injection.throwables.InjectionError: Critical injection failure:
Variable modifier method mmcr$replaceRecipeBackground(...) in
mixins/extras/mmcr.jei.mixins.json:JeiRecipeManagerMixin from mod mmcr failed injection check,
(0/1) succeeded. Scanned 0 target(s). No refMap loaded.
```

**和本附属无关，是 MMCR 自己的 JEI 兼容层版本不匹配。** `mmcr-neoforge-mc26.1.2-0.1.7` 是对着
**JEI 29.47.0.110** 编译的（MMCR 源码 `gradle.properties: jei_version=29.47.0.110`），
换更新的 JEI（例如 29.53.x）后 `mezz.jei.library.recipes.RecipeManager` 里被注入的方法已经不在了，
注入检查失败直接崩客户端。

处理办法，任选其一：

1. **JEI 降到 29.47.0.110**（推荐，已验证进标题画面正常）。
2. 不装 JEI。机器、结构、配方都不依赖 JEI，只是没有 JEI 页面。
3. 等 MMCR 更新 JEI 兼容层。

本附属**没有** mixin，不会造成这类崩溃。

### `/nekojs error` 没输出，但也没生效

可能脚本被 `disable` 属性关掉了：

```js
// disable: true
```

或者 `modloaded` 条件不满足：

```js
// modloaded: create, jei
```

属性必须写在**文件开头的注释块**里，中间不能夹代码或块注释。

### 改了结构，`/nekojs reload` 没变化

预期行为。结构在启动期冻结，**必须重启游戏**。见 [生命周期](Getting-Started)。

### `MMCREvents.server` 里注册结构/配方，静默无效

也是预期行为。`ServerEventJS` **只有 `getAPI()`**，没有 `createStructure` / `createRecipe`——就是为了让你在写的时候直接报错，而不是运行时静默丢弃。

结构 → `startup_scripts`；配方 → `ServerEvents.recipes`。

## 机器/结构问题

### 控制器方块显示成 `block.mmcr.xxx_controller`（没有名字）

控制器方块**不是注册在你的命名空间下**。MMCR 把所有机器的控制器统一注册进它自己的
`mmcr` 命名空间，路径才是「机器路径 + `_controller`」：

```
机器 my_pack:space_elevator  ->  方块 mmcr:space_elevator_controller
                                 lang 键 block.mmcr.space_elevator_controller
```

所以写 `block.my_pack.space_elevator_controller` 是**无效**的，游戏里就会显示原始 key。
方块名和物品名是两套键，两个都要写：

```json
{
  "block.mmcr.space_elevator_controller": "太空电梯控制器",
  "item.mmcr.space_elevator_controller": "太空电梯控制器"
}
```

文件放 `nekojs/assets/mmcr/lang/zh_cn.json`（NekoJS 会把 `nekojs/assets` 整个挂成资源包）。
机器自己的名字仍然是 `machine.<机器命名空间>.<路径>`，那套键放你自己命名空间的 lang 文件里。

同一路径的两台机器（`a:smelter` 与 `b:smelter`）会撞同一个控制器方块名，控制器只会注册一个——
给机器起名时避开这种冲突。

### 控制器放下去，右键成型失败

按可能性排序：

1. **方块不匹配**。`api.block(...)` 只校验方块种类，`api.state(...)` 还校验状态。原木、楼梯这类请确认状态。
2. **结构其实没建成你以为的形状**。`pattern()` 是 z 层不是 y 层，第一个字符串是最底层。见 [结构](Structures)。
3. **行宽不一致**。写了 3 个字符的行里混进 4 个字符，整个结构错位，**不报错**。
4. **`portRequirements` 不满足**。这个是真校验，数量不够就成不了型。
5. **控制器符号出现了多次**。`.controller('C')` 的 `C` 应该只出现一次。
6. **控制器符号被 `set(...)` 绑过**。`set('C', api.block(...))` 会让那一格要求你放别的方块（MMCR 只在符号未被绑定时才替换成控制器方块），结构永远成型失败。0.1.5 起会忽略该绑定并打 `symbol 'C' is the controller, so set('C', ...) is ignored`。

用 `/mmcr build <机器ID>` 让 MMCR 自己建一遍，比手动摆快得多，也能快速看出形状对不对。

### 成型了但没有接口

结构里所有能接管道的位置都被 `api.block(...)` 绑死了。给预留位加 `api.anyOf(...)`：

```js
.set('H', api.anyOf(
  api.block('minecraft:iron_block'),
  api.itemInput(), api.itemOutput(), api.energyInput()
))
```

只加 `api.block(...)` 的话，那个位置永远只认一种方块。

### 等级槽成型失败

那个位置放的方块，没有任何 `MachineLevel` 的 `state` 匹配。检查：

- `event.createLevel(...).state('minecraft:gold_block')` 里的方块 ID 对不对
- 结构里用的是 `event.levelSlot('my_mod:coil')`，而 `coil` 这个**等级类型**注册过

## 配方问题

### 没装 JEI，怎么确认配方进去了

日志里会有这条，属于正常：

```
Mixin config mixins/extras/mmcr.jei.mixins.json from jar(mods/mmcr-...jar) not applied as required mod 'jei' is missing
```

没 JEI 就没法看配方页，但配方本身仍然生效。两个替代验证方式：

1. 直接让机器跑起来——有输入、有能量、产出对得上就说明配方加载了
2. 看启动日志的 `Loaded N recipes`，数字比装本附属之前大

配方**不依赖** JEI，JEI 只是展示层。

### JEI 里有配方，机器不跑

1. 接口位置没有 `itemInput()` / `energyInput()` 条件 → 材料进不去
2. 没通电
3. 配方需要等级，但机器没装
4. `recipe_pool` 写成了机器 ID，而机器绑的池是另一个名字

### 配方在 JEI 里根本不出现

1. 配方脚本报错 → `/nekojs error`
2. `recipe_pool` 字段拼错（是 `recipe_pool` 不是 `machine`）
3. 物品输出写成了输入格式：

```js
// 错：输出用了 item
const wrong = { type: 'minecraft:item', io: 'output', item: 'minecraft:iron_ingot', count: 1 }
// 对：输出用 stack
const right = { type: 'minecraft:item', io: 'output', stack: { id: 'minecraft:iron_ingot', count: 1 } }
```

4. `type` 不是 `mmcr:machine_recipe`

### 改了配方没生效

- 配方在 `server_scripts` → `/nekojs reload`
- 配方在数据包里 → `/reload`

两者不一样，别搞混。

### `parallelized: true` 但还是单份跑

机器的三个条件缺一不可：

```js
.allowParallelism()
.maxParallelAmount(8)     // 不写默认 1
```

加上配方里的 `parallelized: true`。

## 修饰符/等级没效果

| 症状 | 检查 |
|---|---|
| 插了芯片没反应 | 机器有没有 `.allowModifiers()`；`registerModifierItem(itemId, modifierId)` 的 modifier ID 拼对没 |
| 等级换了没变化 | 那个等级有没有挂 `modifier(...)`；或者配方没写 `mmcr:level` 需求 |
| 修饰符生效但方向反 | `multiply` 0.5 = 变快/变少，1.5 = 变慢/变多 |
| target 名写了没效果 | `'duration'` / `'energy'` / `'item'` / `'fluid'` 这几个是内置的；其他名字不会生效也不会报错 |

## 性能

### 服务器卡

`tickBehavior` 里的 `serverTick` 每 tick 每个机器都跑一次。两件事：

1. 用 `ctx.isDue(n)` 降低频率，别每 tick 都干活
2. 回调里不要做区块扫描、不要遍历大列表

`recipeTick` 同理——它每 tick 每条运行中的配方都会调用。

## 收集信息

报 issue 前请附上：

1. `logs/latest.log` 里搜 `mmcr_nekojs` 的部分
2. `/nekojs error` 的输出
3. 出问题的脚本文件
4. MMCR / NekoJS / 本附属 三者的**确切版本号**（不是文件名，是 `mods.toml` 里的版本）

## 还是不行

- [从 KubeJS 迁移](KubeJS-Migration) —— 如果你是照着 KubeJS 示例写的，八成是目录或方法名没改
- [全部示例](Examples) —— 对照一个已知能跑的完整脚本
