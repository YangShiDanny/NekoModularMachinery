# `mmcr_nekojs` API 参考

> 面向 **Modular Machinery: Community Refoxed 0.1.7+** / **NekoJS 1.1.0+**。
> 目标是与 MMCR 官方 KubeJS 集成**同名、同参数、同语义**。凡是与 KubeJS 版不同的地方，
> 本文都用 ⚠️ 标出，最后还有一节[差异汇总](#12-与-kubejs-版的差异汇总)。
>
> 本页是**参考手册**。想看按步骤的教程，去 [`wiki/`](../wiki/Home.md)。

---

## 目录

1. [全局绑定](#1-全局绑定)
2. [生命周期与脚本目录](#2-生命周期与脚本目录)
3. [事件对象](#3-事件对象)
4. [`MMCR.getAPI()` — 门面方法](#4-mmcrgetapi--门面方法)
5. [常量对象](#5-常量对象)
6. [MachineBuilderJS](#6-machinebuilderjs)
7. [StructureBuilderJS](#7-structurebuilderjs)
8. [LevelTypeBuilderJS / MachineLevelBuilderJS](#8-leveltypebuilderjs--machinelevelbuilderjs)
9. [RecipeBuilderJS](#9-recipebuilderjs)
10. [配方 JSON 与需求项](#10-配方-json-与需求项)
11. [配方 schema](#11-配方-schema)
12. [与 KubeJS 版的差异汇总](#12-与-kubejs-版的差异汇总)

---

## 1. 全局绑定

插件注册一个全局绑定 `MMCR`：

| 成员 | 类型 | 说明 |
|---|---|---|
| `MMCR.getAPI()` | `MMCRApiJS` | 门面：ID、方块谓词、修饰符、需求构造、数字格式化 |
| `MMCR.getValues()` | `MMCRValuesJS` | `INT_MAX` / `INT_MIN` / `LONG_MAX` / `LONG_MIN` |

事件对象上也有一份 `event.getAPI()`，与 `MMCR.getAPI()` 是同一套方法，用哪个都行。

```js
// 两种写法等价
MMCR.getAPI().block('minecraft:iron_block')
MMCREvents.startup(event => {
  const api = event.getAPI()
})
```

`MMCRValuesJS` 字段：

| 字段 | 值 |
|---|---|
| `INT_MAX` | `2147483647` |
| `INT_MIN` | `-2147483648` |
| `LONG_MAX` | `9223372036854775807` |
| `LONG_MIN` | `-9223372036854775808` |

---

## 2. 生命周期与脚本目录

MMCR 开三个注册窗口，本附属按窗口投递脚本声明。

| 声明内容 | 事件 | 脚本目录 | 生效方式 |
|---|---|---|---|
| 机器、结构、等级类型、等级、修饰符、修饰符物品、屏幕文本、JEI 工作台 | `MMCREvents.startup` | `startup_scripts/` | **重启游戏** |
| 数据驱动配方 | `ServerEvents.recipes` | `server_scripts/` | `/nekojs reload` ✅ |
| JEI 配方说明 | `MMCREvents.client` | `client_scripts/` | `/nekojs reload client` |

⚠️ **结构必须写在 `startup_scripts`。** KubeJS 版能在 `server` 阶段声明结构，是因为 MMCR 专门为
KubeJS 做了延迟收集（`StartupContentRegistration` 里 `isLoaded("kubejs")` 那条分支）。
NekoJS 没有这个照顾，`FMLCommonSetupEvent` 一到结构表就冻结了。详见 [README §7](../README.md)。

时序（决定了一切）：

```
NekoJSMod 构造函数  →  同步跑完 STARTUP 脚本
        ↓
FMLConstructModEvent →  MMCR 收集机器定义（ServiceLoader → DefinitionsProvider）
        ↓
FMLCommonSetupEvent  →  MMCR 冻结结构表（post RegisterMachineStructuresEvent）
        ↓
ModifyDefaultComponentsEvent → MMCR 收集静态配方（post RegisterMachineRecipesEvent）
        ↓
SERVER 脚本加载（太晚，赶不上上面任何一个窗口）
```

---

## 3. 事件对象

### 3.1 `MMCREvents.startup(event => ...)`

脚本类型：`STARTUP`。事件对象：`StartupEventJS`。

| 方法 | 返回 | 说明 |
|---|---|---|
| `event.getAPI()` | `MMCRApiJS` | 同 `MMCR.getAPI()` |
| `event.createMachine(id)` | `MachineBuilderJS` | 见 [§6](#6-machinebuilderjs) |
| `event.createStructure(id)` | `StructureBuilderJS` | 见 [§7](#7-structurebuilderjs) |
| `event.createLevelType(id)` | `LevelTypeBuilderJS` | 见 [§8](#8-leveltypebuilderjs--machinelevelbuilderjs) |
| `event.createLevel(id)` | `MachineLevelBuilderJS` | 见 [§8](#8-leveltypebuilderjs--machinelevelbuilderjs) |
| `event.levelSlot(typeId)` | `LevelSlotRef` | 给 `structure.set()` 用，表示"这里是某个等级槽" |
| `event.modifierSlot(modifierId)` | `ModifierUseRef` | 同上，表示"这里是某个修饰符槽" |
| `event.modifierSlot(modifierId, replacement)` | `ModifierUseRef` | 带替换谓词：不装修饰符时该格显示什么方块 |
| `event.registerModifier(id, definition)` | `void` | `definition` 是 `api.modifierDefinition([...])` 或 `ModifierSpec[]` |
| `event.registerModifierItem(itemId, modifierId)` | `void` | 用某个物品右键可装/卸该修饰符 |
| `event.addRecipePoolWorkstation(poolId, itemId)` | `void` | JEI：配方池对应的工作站物品 |
| `event.addMachineWorkstation(machineId, recipeTypeId)` | `void` | JEI：机器对应的配方类型（别名 `addMachineWorkStation`） |
| `event.registerControllerScreenText(machineId, handler)` | `void` | 控制器屏幕自定义文本，`handler(ctx)` 见下 |

`registerControllerScreenText` 的 `ctx` 是 MMCR 的 `ControllerTextContext`，可用成员
（取决于 MMCR 版本）：`ctx.machine`、`ctx.level`、`ctx.pos`、`ctx.blockEntity` 等，
以及向列表追加文本的方法。NekoJS 侧直接调用即可。

⚠️ `registerModifierItem` / `addRecipePoolWorkstation` 只接受 **ID 字符串**，不接受
`Item.of(...)`（KubeJS 老版本接受 `ItemStack`，MMCR 已迁移到启动期只存 ID）。

### 3.2 `MMCREvents.server(event => ...)`

脚本类型：`SERVER`。事件对象：`ServerEventJS`。

| 方法 | 返回 | 说明 |
|---|---|---|
| `event.getAPI()` | `MMCRApiJS` | |
| `event.createRecipe(id)` | `RecipeBuilderJS` | ⚠️ 构造时检查 `Recipes.isRegistrationOpen()`，在 SERVER 阶段调用会**抛异常** |

⚠️ 这个事件在 MMCR 的注册窗口之后才触发，所以**几乎什么都不能做**。它存在只是为了兼容
KubeJS 的脚本结构。配方请用 [`ServerEvents.recipes`](#11-配方-schema)。

### 3.3 `MMCREvents.client(event => ...)`

脚本类型：`CLIENT`。事件对象：`ClientInfoEventJS`。

| 方法 | 返回 | 说明 |
|---|---|---|
| `event.addRecipePoolInfo(poolId, translationKey, ...args)` | `void` | 配方池级 JEI 说明 |
| `event.addRecipeInfo(recipeId, translationKey, ...args)` | `void` | 单配方级 JEI 说明 |

`...args` 会作为格式化参数传给 `Component.translatable(key, args...)`。

---

## 4. `MMCR.getAPI()` — 门面方法

### 4.1 ID

| 签名 | 说明 |
|---|---|
| `id(String value) → Id` | 解析 `"ns:path"`；版本无关（1.21.1 返回 `ResourceLocation`，26.1.2 返回 `Identifier`） |

### 4.2 方块谓词

| 签名 | 说明 |
|---|---|
| `block(String blockId)` | 匹配某个方块（忽略方块状态） |
| `state(String stateId)` | 匹配某个精确方块状态，如 `"minecraft:furnace[lit=true]"` |
| `tag(String tagId)` | 匹配方块标签 |
| `air()` | 空气 |
| `any()` | 任意方块 |
| `coupler()` | MMCR 的连接器方块 |
| `anyOf(...values)` | 任意匹配其一；元素可以是谓词对象、ID 字符串或数组 |

### 4.3 端口族谓词

用于 `structure.set()`，标记"这一格必须是某方向的端口"。

**输入 / 输出（12 个）**

`itemInput()` `itemOutput()` `fluidInput()` `fluidOutput()` `energyInput()` `energyOutput()`
`chemicalInput()` `chemicalOutput()` `radioactiveChemicalInput()` `radioactiveChemicalOutput()`
`heatInput()` `heatOutput()`

**任意方向 / 特殊方块（12 个）**

`itemPorts()` `fluidPorts()` `energyPorts()` `chemicalPorts()` `heatPorts()` `ports()`
`upgradeBus()` `parallelControllers()` `factoryController()` `smartInterface()` `dataStorage()` `networkInterface()`

⚠️ 26.1.2 没有 `stress` / `source` / `mana` / `air` 这四个端口族（MMCR 上游 26.1.2 分支砍掉了
对应联动）。共享代码里走 `VersionSupport` 判断，不支持的族会**记一条 warn 后跳过**，不会崩。

### 4.4 修饰符

| 签名 | 说明 |
|---|---|
| `modifier(target, scope, value, operation, chance)` | 数值修饰符。`scope` ∈ `input`/`output`/`machine`/`recipe`；`operation` ∈ `add`/`multiply`/`subtract`/`divide` |
| `modifier('parallelized', 'recipe', bool)` | 布尔修饰符（仅这一个组合合法） |
| `modifierDefinition(...specs)` | 把若干 spec 打包成 `ModifierBundle` |
| `modifierUse(modifierId, replacement)` | 生成 `ModifierUseRef`，用于 `structure.set()` |

`scope` / `operation` 字符串**大小写不敏感**（内部 `toUpperCase`）。

```js
const speed = api.modifier('duration', 'recipe', 0.8, 'multiply', false)
const luck  = api.modifier('output_chance', 'output', 0.1, 'add', true)
event.registerModifier('my_pack:fast', api.modifierDefinition([speed, luck]))
```

### 4.5 等级槽引用

| 签名 | 说明 |
|---|---|
| `levelSlot(String typeId) → LevelSlotRef` | 供 `structure.set()` / 直接传 |

### 4.6 配方 JSON 构造

这些方法返回**普通对象**（`Map`），可以直接丢进 `event.custom(...)`。

| 签名 | 产出 |
|---|---|
| `itemInputRequirement(itemId, count)` | `{type:'minecraft:item', io:'input', item, count}` |
| `itemOutputRequirement(itemId, count, chance)` | `{type:'minecraft:item', io:'output', stack:{id,count}, chance}` |
| `fluidInputRequirement(fluidId, amount)` | `{type:'minecraft:fluid', io:'input', fluid:{fluid}, amount}` |
| `fluidOutputRequirement(fluidId, amount, chance)` | `{type:'minecraft:fluid', io:'output', stack:{id,amount}, chance}` |
| `energyRequirement(io, fePerTick)` | `{type:'neoforge:energy', io, fe_per_tick}` |
| `machineRecipe(pool, tickTime, ...requirements)` | 完整配方 JSON（见 [§10](#10-配方-json-与需求项)） |

### 4.7 数字格式化

| 签名 | 输出示例（输入 `1234567`） |
|---|---|
| `readableNumber(long)` | `1.23M` |
| `readableNumberFull(long)` | `1.234M` → 保留末尾零 |
| `readableNumberExact(long)` | `1,234,567` |

⚠️ `readableNumberBigInt` / `readableNumberForSlot` **未实现**。

---

## 5. 常量对象

```js
const api = MMCR.getAPI()
api.screenScope().CONTROLLER      // 'controller'
api.recipeIO().OUTPUT             // 'output'
api.outputPolicy().ALLOW_PARTIAL  // 'allow_partial'
api.modifierOperation().MULTIPLY  // 'multiply'
api.modifierScope().RECIPE        // 'recipe'
```

| 对象 | 字段 |
|---|---|
| `screenScope()` | `CONTROLLER`, `OPERATION` |
| `recipeIO()` | `INPUT`, `OUTPUT` |
| `outputPolicy()` | `REQUIRE_FULL`, `ALLOW_PARTIAL` |
| `modifierOperation()` | `ADD`, `MULTIPLY`, `SUBTRACT`, `DIVIDE` |
| `modifierScope()` | `INPUT`, `OUTPUT`, `MACHINE`, `RECIPE` |

⚠️ `modifierScope()` 是本附属额外提供的，KubeJS 版把 scope 写成裸字符串。

---

## 6. `MachineBuilderJS`

`event.createMachine('my_pack:blast_furnace')`

所有方法返回 `this`，可以链式调用。最后必须 `register()`。

### 6.1 基础

| 方法 | 说明 |
|---|---|
| `displayNameKey(key)` | 语言键，默认 `machine.<ns>.<path>` |
| `recipePool(...ids)` | 该机器可用的配方池。**默认就是机器自己的 ID 一个池**，调用即覆盖 |
| `register()` | 提交给 MMCR。必须在 `startup` 阶段调用，否则抛 `IllegalStateException` |

### 6.2 外观

| 方法 | 说明 |
|---|---|
| `appearance(blockId)` | 机器外壳方块 |
| `controllerTextures(front, side, top, bottom)` | 控制器四面对应纹理 |
| `controllerTextures(front, otherFaces)` | 正面单独、其余三面相同 |
| `controllerFrontTexture(id)` / `controllerSideTexture(id)` / `controllerTopTexture(id)` / `controllerBottomTexture(id)` | 单独设置某面 |
| `controllerBaseTexture(id)` | 控制器底座纹理 |
| `formedPortBaseTexture(id)` | 成型后端口底座纹理 |
| `controllerIdleOverlayTexture(id)` | 空闲叠加层 |
| `controllerActiveOverlayTexture(id)` | 运行叠加层 |
| `runningSound(id)` / `finishSound(id)` | 运行中 / 完成音效 |

### 6.3 朝向

| 方法 | 说明 |
|---|---|
| `allowVerticalFacing()` | 允许上下朝向 |
| `requireVerticalFacing()` | 强制上下朝向 |
| `fullyRotationallySymmetric()` | 四向完全对称 |

### 6.4 并行与工厂

| 方法 | 说明 |
|---|---|
| `allowModifiers()` | 允许装修饰符 |
| `allowMultithreading()` | 允许多线程 |
| `allowParallelism()` | 允许并行 |
| `maxParallelAmount(long)` | 并行上限 |
| `maxParallelism(long)` | 同上，别名 |
| `factoryThreads(int)` | 开启工厂模式并设定线程数（`>0` 才生效） |
| `networkInterface(maxCount, maxConnections)` | 网络接口上限；`maxCount >= 0` 才发 |

### 6.5 行为钩子

| 方法 | 说明 |
|---|---|
| `tickBehavior(hooks => ...)` | `hooks.serverTick(ctx => ...)` |
| `recipeBehavior(hooks => ...)` | 见下表 |

`recipeBehavior` 的 `hooks` 是 MMCR `RecipeHooks`：

| 方法 | 回调参数 |
|---|---|
| `hooks.idleStart(cb)` | `MachineContext` |
| `hooks.idleEnd(cb)` | `MachineContext` |
| `hooks.beforeStart(cb)` | `RecipeStartContext` |
| `hooks.recipeTick(cb)` | `RecipeTickContext` |
| `hooks.beforeFinish(cb)` | `RecipeFinishContext` |
| `hooks.preServerTick(cb)` | `MachineContext` |
| `hooks.postServerTick(cb)` | `MachineContext` |

```js
event.createMachine('my_pack:smelter')
  .displayNameKey('machine.my_pack.smelter')
  .appearance('minecraft:iron_block')
  .allowParallelism()
  .maxParallelAmount(8)
  .recipeBehavior(hooks => {
    hooks.beforeFinish(ctx => console.info('done at ' + ctx.pos))
  })
  .register()
```

⚠️ 这两个钩子依赖 NekoJS 把 JS 函数转成 Java `Consumer`。如果你的 NekoJS 版本不支持
宿主 `@FunctionalInterface` 桥接，会在调用时抛类型错误——那就改用 Java 侧扩展。

---

## 7. `StructureBuilderJS`

`event.createStructure('my_pack:smelter')`。最后必须 `build()`。

| 方法 | 说明 |
|---|---|
| `pattern(...rows)` | 追加**一个 z 切片**（每个字符串是一层的一行，`#` 等字符是符号） |
| `pattern(List<String>)` | 同上，传数组形式 |
| `set(symbol, value)` | 绑定符号 → 谓词/等级槽/修饰符槽 |
| `controller(symbol)` | 哪个符号是控制器，默认 `'C'` |
| `stateSensitive()` | 方块状态参与匹配（默认开） |
| `stateInsensitive()` | 只看方块类型 |
| `portRequirements(map)` | 端口数量要求 |
| `portTierRequirements(...values)` | 端口等级要求 |
| `build()` | 提交 |

### 7.1 `pattern()` 的展开顺序

MMCR 的 `MachineDefinitionConverter.toBlockArray` 给出的精确语义：

| 维度 | 怎么来 | 方向 |
|---|---|---|
| `z` | **每次 `pattern()` 调用 = 一层**，按调用顺序递增 | 第 1 次调用是 z 最小层 |
| `y` | 同一次调用里的**第 n 个字符串** | 第 1 个字符串是 **y 最小（最底层）** |
| `x` | 字符串里的**第 n 个字符** | 第 1 个字符是 x 最小 |

结构尺寸由最大宽/高/深决定，坐标以结构中心为原点（`x - width/2` 等）。

⚠️ **空格 `' '` 会被跳过**（`if (symbol == ' ') continue;`），表示"这一格不检查"，
**不是**"必须是空气"。要强制空气就 `.set('#', api.air())`。

⚠️ 如果控制器不在 `(0,0,0)`，MMCR 会把整个结构**平移**，让控制器落到原点。
所以 `.controller()` 指定的符号决定成型的锚点。

```js
// 3×3 水平地板：3 次 pattern()，每次一行 3 字符
createStructure('my_pack:smelter')
  .pattern('BBB')      // z = 0
  .pattern('BCB')      // z = 1，中间是控制器
  .pattern('BBB')      // z = 2
  .set('B', api.block('minecraft:iron_block'))
  .set('C', api.block('minecraft:iron_block'))
  .controller('C')
  .build()
```

竖 3×3 的一面墙才是只调一次 `pattern('BBB','BLB','BBB')`（3 个 y 层，1 个 z 层）。

### 7.2 `set()` 接受的值

| 类型 | 效果 |
|---|---|
| `api.block/state/tag/air/any/...` 返回的谓词 | 直接用 |
| ID 字符串 `'minecraft:iron_block'` | 自动转成 `block(...)` |
| 数组 `['a:b', 'c:d']` | 自动转成 `anyOf(...)` |
| `event.levelSlot(typeId)` | 等级槽 |
| `event.modifierSlot(modifierId)` / `modifierSlot(id, replacement)` | 修饰符槽 |

### 7.3 `portRequirements(map)`

键是 MMCR 的**端口族统计别名**（不带 `mmcr:` 前缀），值有两种形式：

| 形式 | 含义 |
|---|---|
| `Number` | 至少这么多个 |
| `[min, max]` | 数量区间 |

```js
.portRequirements({
  item_input_bus: [1, 2],
  energy_input_hatch: 1
})
```

### 7.4 `portTierRequirements(...)`

两种写法：

```js
// 字符串形式
.portTierRequirements('item_input_bus>=normal', 'energy_input_hatch>=reinforced')

// 数组形式（族名, 等级）
.portTierRequirements(['item_input_bus', 'normal'], ['energy_input_hatch'])
```

等级名**大小写不敏感**。只写族名 = 任意等级。

26.1.2 上不存在的族（`stress` / `source` / `mana` / `air`）会被跳过并打 warn。

---

## 8. `LevelTypeBuilderJS` / `MachineLevelBuilderJS`

### 8.1 `createLevelType(id)`

| 方法 | 说明 |
|---|---|
| `displayNameKey(key)` | 默认 `level.<ns>.<path>` |
| `register()` | 提交 |

### 8.2 `createLevel(id)`

| 方法 | 说明 |
|---|---|
| `type(typeId)` | **必填**，对应某个 `createLevelType` |
| `state(stateId)` | **必填**，该等级要求等级槽放什么方块 |
| `priority(int)` | 匹配优先级，数字大的优先 |
| `modifier(bundle)` | 该等级自带的修饰符，`api.modifierDefinition([...])` |
| `register()` | 提交；缺 `type` 或 `state` 会抛异常 |

```js
event.createLevelType('my_pack:tier').displayNameKey('level.my_pack.tier').register()

event.createLevel('my_pack:tier_basic')
  .type('my_pack:tier')
  .state('minecraft:iron_block')
  .priority(0)
  .register()

event.createLevel('my_pack:tier_elite')
  .type('my_pack:tier')
  .state('minecraft:diamond_block')
  .priority(10)
  .modifier(api.modifierDefinition([
    api.modifier('duration', 'recipe', 0.7, 'multiply', false)
  ]))
  .register()
```

---

## 9. `RecipeBuilderJS`

`event.createRecipe(id)` —— ⚠️ **只在 `startup` 阶段可用**（构造时检查注册窗口）。
日常写配方请用 [`ServerEvents.recipes`](#11-配方-schema)。

| 方法 | 说明 |
|---|---|
| `recipePool(pool)` | 必填 |
| `duration(ticks)` / `tickTime(ticks)` | 加工时长，默认 20 |
| `priority(int)` | |
| `maxThreads(int)` | |
| `parallelized(bool)` | |
| `allowPartialOutputs(bool)` | |
| `cancelIfPerTickFails(bool)` | |
| `inputItem(Item \| Ingredient \| TagKey<Item>, count)` | |
| `outputItem(Item, count)` / `outputItem(ItemStack)` | |
| `outputChance(ItemStack, chance)` | 概率产出 |
| `inputFluid(Fluid, amount)` / `inputFluid(Fluid, amount, chance)` | |
| `outputFluid(Fluid, amount)` | |
| `inputEnergy(fePerTick)` / `outputEnergy(fePerTick)` | |
| `levelRequirement(typeId, levelId)` | |
| `stageRequirement(minStage)` | |
| `requiredHost(hostId)` | |
| `modifier(modifierId)` | |
| `build()` | 提交 |

---

## 10. 配方 JSON 与需求项

### 10.1 已知字段

（以 MMCR `MachineRecipeJson.parse` 为准）

| 字段 | 类型 | 必填 | 默认 |
|---|---|---|---|
| `recipe_pool` | string | ✅ | — |
| `tick_time` | int ≥ 1 | ✅ | — |
| `requirements` | array | ✅ | — |
| `outputs` | array | | — |
| `modifiers` | array | | — |
| `priority` | int | | `0` |
| `max_threads` | int | | `1` |
| `parallelized` | bool | | `false` |
| `allow_partial_outputs` | bool | | `false` |
| `cancelIfPerTickFails` | bool | | `false` |
| `required_host_ids` | array | | `[]` |

⚠️ 旧字段 `machine` / `inputs` / `fluid_outputs` / `energy_per_tick` / `machine_outputs` /
`level_requirements` 会被**拒绝**。

### 10.2 需求项

| 类型 | 输入 | 输出 |
|---|---|---|
| `minecraft:item` | `{type, io:'input', item, count}` | `{type, io:'output', stack:{id,count}, chance}` |
| `minecraft:fluid` | `{type, io:'input', fluid:{fluid:id}, amount}` | `{type, io:'output', stack:{id,amount}}` |
| `neoforge:energy` | `{type, io:'input', fe_per_tick}` | `{type, io:'output', fe_per_tick}` |

⚠️ **流体输入的 `fluid` 必须是对象** `{fluid:'minecraft:water'}`，写裸字符串会让整个配方被静默丢弃。

用门面构造最省事：

```js
const api = MMCR.getAPI()
event.custom(api.machineRecipe('my_pack:smelter', 200,
  api.itemInputRequirement('minecraft:iron_ore', 2),
  api.energyRequirement('input', 40),
  api.itemOutputRequirement('minecraft:iron_ingot', 1, 1.0)
)).id('my_pack:iron_from_ore')
```

---

## 11. 配方 schema

Java 侧注册了 `mmcr:machine_recipe` 的脚本 schema，所以可以写：

```js
ServerEvents.recipes(event => {
  event.recipes.mmcr
    .machine_recipe('my_pack:smelter', 200, [
      { type: 'minecraft:item', io: 'input', item: 'minecraft:iron_ore', count: 2 },
      { type: 'neoforge:energy', io: 'input', fe_per_tick: 40 },
      { type: 'minecraft:item', io: 'output', stack: { id: 'minecraft:iron_ingot', count: 1 }, chance: 1.0 }
    ])
    .id('my_pack:iron_from_ore')
})
```

三种等价写法：

```js
// 1) schema
event.recipes.mmcr.machine_recipe(pool, ticks, [reqs]).id('id')

// 2) 原生 JSON —— 永远可用，不依赖 schema
event.custom({ type:'mmcr:machine_recipe', recipe_pool: pool, tick_time: ticks,
               requirements: [reqs] }).id('id')

// 3) 门面
event.custom(api.machineRecipe(pool, ticks, ...reqs)).id('id')
```

`schema` 字段定义（`MachineRecipeSchema`）：

| 字段 | 种类 | 数组 | 可选 | 默认 | 角色 |
|---|---|---|---|---|---|
| `recipe_pool` | STRING | ✗ | ✗ | — | OTHER |
| `tick_time` | INT | ✗ | ✓ | `20` | OTHER |
| `requirements` | JSON | ✓ | ✗ | — | INPUT |
| `outputs` | JSON | ✓ | ✓ | — | OUTPUT |
| `modifiers` | JSON | ✓ | ✓ | — | OTHER |
| `priority` | INT | ✗ | ✓ | `0` | OTHER |
| `max_threads` | INT | ✗ | ✓ | `1` | OTHER |
| `parallelized` | BOOLEAN | ✗ | ✓ | `false` | OTHER |
| `allow_partial_outputs` | BOOLEAN | ✗ | ✓ | `false` | OTHER |
| `cancelIfPerTickFails` | BOOLEAN | ✗ | ✓ | `false` | OTHER |
| `required_host_ids` | JSON | ✓ | ✓ | `[]` | OTHER |

---

## 12. 与 KubeJS 版的差异汇总

| # | 差异 | 原因 | 应对 |
|---|---|---|---|
| 1 | 结构、`createRecipe` 必须写在 `startup_scripts` | MMCR 只为 KubeJS 延迟结构收集 | 直接改目录 |
| 2 | 只有 `MMCREvents` 组，没有 `mmcr` 别名 | NekoJS 事件组命名规则 | 用 `MMCREvents` |
| 3 | `registerModifierItem` / `addRecipePoolWorkstation` 只收 ID 字符串 | MMCR 已迁移到启动期只存 ID | 传 `'minecraft:iron_ingot'` |
| 4 | 方块注册用 NekoJS 的 `RegistryEvents` | 不是 KubeJS 的 `StartupEvents.registry` | 见 wiki |
| 5 | `readableNumberBigInt` / `readableNumberForSlot` 未实现 | 少见 | 用 `readableNumberExact` |
| 6 | 化学品 / 空气 / 魔法类快捷工厂未实现 | 联动模组差异大 | 用 `event.custom` 手写 JSON |
| 7 | 网络请求、智能接口运行时值、控制器渲染器未实现 | 需要 Java 侧扩展 | 见 [wiki/Roadmap](../wiki/Roadmap.md) |
| 8 | 26.1.2 少 4 个端口族 | MMCR 上游砍掉对应联动 | 代码里已自动跳过 + warn |

---

## 附：未实现清单（想补的话从这里下手）

- `DataValue` / `networkInterfaces` / `sendRequest`
- `SmartInterfaces` 类型注册与运行时值
- Mekanism 化学品快捷工厂（`chemicalTagInput` 等）
- 控制器渲染器扩展（`RegisterControllerRenderersEvent`）
- `anyOfItemInput()` 这类 KubeJS 长名别名
