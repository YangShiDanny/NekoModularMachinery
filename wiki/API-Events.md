# 事件与顶层绑定

本页覆盖脚本能直接摸到的所有入口对象。

## 1. `MMCR` —— 全局绑定

> 脚本顶层可用的全局名。类型 `cn.howxu.mmcr.nekojs.MMCRBinding`。

只有两个方法，都是返回门面：

| 方法 | 返回 | 说明 |
|---|---|---|
| `MMCR.getAPI()` → `MMCRApiJS` | 门面 | 方块条件、ID、修饰符、requirement 构造 |
| `MMCR.getValues()` → `MMCRValues` | 常量 | Java 数值边界 |

```js
const api = MMCR.getAPI()
const stone = api.block('minecraft:stone')
```

`MMCR` **不是**注册器。机器、结构、配方都要通过事件对象创建。

## 2. `MMCRValues` —— 数值常量

> `cn.howxu.mmcr.nekojs.MMCRValuesJS`

| 字段 | 值 |
|---|---|
| `INT_MAX` | `2147483647` |
| `INT_MIN` | `-2147483648` |
| `LONG_MAX` | `9223372036854775807` |
| `LONG_MIN` | `-9223372036854775808` |

```js
const maxStack = MMCR.getValues().INT_MAX
```

用途主要是给 `.maxParallelAmount(...)`、`.priority(...)` 这类整型参数填边界值。注意 JS 的 `number` 是双精度浮点，超过 `2^53` 的 `LONG_MAX` 传进去会失真——真要传超大值请用字符串或 `BigInteger`。

## 3. `MMCREvents` —— 事件组

> `cn.howxu.mmcr.nekojs.MMCRNekoEvents`

三个事件，按脚本阶段划分：

| 事件 | 触发阶段 | 放在哪个目录 |
|---|---|---|
| `MMCREvents.startup` | NekoJS `STARTUP` | `nekojs/startup_scripts/` |
| `MMCREvents.server` | NekoJS `SERVER` | `nekojs/server_scripts/` |
| `MMCREvents.client` | NekoJS `CLIENT` | `nekojs/client_scripts/` |

### `MMCREvents.startup(callback)`

**唯一能注册东西的事件。** 回调参数是 `StartupEventJS`。

```js
MMCREvents.startup(event => {
  event.createMachine('my_mod:x').register()
  event.createStructure('my_mod:x').pattern('C').controller('C').build()
})
```

为什么只有它行：NekoJS 在自己的 mod 构造器里就同步跑完了启动脚本，早于 MMCR 的全部三个注册窗口。详见 [生命周期](Getting-Started)。

### `MMCREvents.server(callback)`

回调参数是 `ServerEventJS`，**只有 `getAPI()`**。

这个事件存在只是为了兼容 KubeJS 脚本的形状，**不能注册任何东西**。MMCR 的结构窗口和配方窗口在它之前就关了。

```js
// 可以：纯计算
MMCREvents.server(event => {
  const req = event.getAPI().itemInputRequirement('minecraft:iron_ore', 1)
})
```

配方请用 NekoJS 的 `ServerEvents.recipes`（见 [配方](Recipes)）。

### `MMCREvents.client(callback)`

回调参数是 `ClientInfoEventJS`，给 JEI 配方页加说明文本。

```js
MMCREvents.client(event => {
  event.addRecipePoolInfo('my_mod:smelter', 'jei.my_mod.smelter.info', 10)
})
```

放在 `nekojs/client_scripts/`。没装 JEI 时这些调用被忽略。

> 事件组同时以 `mmcr` 为组名注册（和 KubeJS 版一致）。组内有 `startup` / `server` / `client` 三个事件 ID：`mmcr.startup`、`mmcr.server`、`mmcr.client`。

## 4. `MMCRApiJS` —— API 门面

> `cn.howxu.mmcr.nekojs.MMCRApiJS`

`MMCR.getAPI()` 和 `event.getAPI()` 返回同一种对象，方法完全相同。

### 4.1 ID

| 方法 | 说明 |
|---|---|
| `api.id(value)` → ID | 把 `'my_mod:x'` 转成 ID 对象。绝大多数位置直接传字符串就行，很少需要手动调 |

### 4.2 方块条件

结构里 `set(symbol, 条件)` 接受的东西。

| 方法 | 匹配 |
|---|---|
| `api.block(blockId)` | 指定方块（不校验状态） |
| `api.state(stateId)` | 指定方块 **和** 方块状态，如 `'minecraft:purpur_pillar[axis=y]'` |
| `api.tag(tagId)` | 方块标签，自动加 `#` |
| `api.air()` | 空气 |
| `api.any()` | 任意方块 |
| `api.anyOf(...conditions)` | 任一满足（可以嵌套数组） |
| `api.coupler()` | 耦合器 |

### 4.3 接口族条件

判断「这一格是不是某类接口」。

| 方法 | 说明 |
|---|---|
| `api.itemInput()` / `api.itemOutput()` | 物品输入 / 输出接口 |
| `api.fluidInput()` / `api.fluidOutput()` | 流体 |
| `api.energyInput()` / `api.energyOutput()` | 能量 |
| `api.chemicalInput()` / `api.chemicalOutput()` | 化学品 |
| `api.radioactiveChemicalInput()` / `...Output()` | 放射性化学品 |
| `api.heatInput()` / `api.heatOutput()` | 热量 |
| `api.itemPorts()` / `api.fluidPorts()` / `api.energyPorts()` / `api.chemicalPorts()` / `api.heatPorts()` | 某类的任意方向接口 |
| `api.ports()` | 任意接口 |
| `api.upgradeBus()` | 升级总线 |
| `api.parallelControllers()` | 并行控制器 |
| `api.factoryController()` | 工厂控制器 |
| `api.smartInterface()` | 智能接口 |
| `api.dataStorage()` | 数据存储 |
| `api.networkInterface()` | 网络接口 |

用法基本都是塞进 `anyOf`：

```js
.set('H', api.anyOf(
  api.block('minecraft:iron_block'),
  api.itemInput(),
  api.itemOutput(),
  api.energyInput()
))
```

### 4.4 修饰符

| 方法 | 说明 |
|---|---|
| `api.modifier(target, scope, value, operation, chance)` → 数值修饰符 | `target` 是被修饰的量名；`scope` 取 `'input'`/`'output'`/`'machine'`/`'recipe'`；`operation` 取 `'add'`/`'multiply'`/`'subtract'`/`'divide'`；`chance` 为 `true` 时按概率生效 |
| `api.modifier('parallelized', 'recipe', bool)` → 布尔修饰符 | 唯一允许的布尔修饰符，前两个参数必须原样传 |
| `api.modifierDefinition(...specs)` → `ModifierBundle` | 把多个 spec 打成一个定义 |

```js
const bundle = api.modifierDefinition(
  api.modifier('speed', 'recipe', 1.5, 'multiply', false),
  api.modifier('energy_cost', 'recipe', 0.8, 'multiply', false)
)
event.registerModifier('my_mod:overclock', bundle)
```

### 4.5 槽位引用

| 方法 | 说明 |
|---|---|
| `api.levelSlot(typeId)` | 等价于 `event.levelSlot(...)`，给结构里做等级槽 |
| `api.modifierUse(modifierId, replacement)` | 等价于 `event.modifierSlot(...)` |

这两个在 `StartupEventJS` 上也有同名方法，写哪个都行。

### 4.6 requirement 构造

**只在你要拼数据配方 JSON 时有用。** 返回值是普通 Map，可以直接放进 `requirements` 数组。

| 方法 | 产出 |
|---|---|
| `api.itemInputRequirement(itemId, count)` | `{type:'minecraft:item', io:'input', item, count}` |
| `api.itemOutputRequirement(itemId, count, chance)` | `{type:'minecraft:item', io:'output', stack:{id,count}, chance}` |
| `api.fluidInputRequirement(fluidId, amount)` | `{type:'minecraft:fluid', io:'input', fluid:fluidId, amount}` |
| `api.fluidOutputRequirement(fluidId, amount, chance)` | `{type:'minecraft:fluid', io:'output', stack:{id,amount}, chance}` |
| `api.energyRequirement(io, fePerTick)` | `{type:'neoforge:energy', io, fe_per_tick}` |
| `api.machineRecipe(pool, tickTime, ...requirements)` | 完整配方 JSON |

```js
MMCREvents.server(event => {
  const api = event.getAPI()
  const recipe = api.machineRecipe(
    'my_mod:smelter', 200,
    api.itemInputRequirement('minecraft:iron_ore', 1),
    api.itemOutputRequirement('minecraft:iron_ingot', 1, 1.0),
    api.energyRequirement('input', 10)
  )
  // recipe 是普通对象，可以交给 ServerEvents.recipes
})
```

### 4.7 数字格式化

| 方法 | 结果示例（`1234567`） |
|---|---|
| `api.readableNumber(v)` | `1.23M` |
| `api.readableNumberFull(v)` | `1.23M`（保留尾零：`1.20M`） |
| `api.readableNumberExact(v)` | `1,234,567` |

给控制屏文本用。

### 4.8 常量门面

返回只读 POJO，字段是字符串：

| 方法 | 字段 |
|---|---|
| `api.screenScope()` | `CONTROLLER` = `'controller'`，`OPERATION` = `'operation'` |
| `api.recipeIO()` | `INPUT` = `'input'`，`OUTPUT` = `'output'` |
| `api.outputPolicy()` | `REQUIRE_FULL` = `'require_full'`，`ALLOW_PARTIAL` = `'allow_partial'` |
| `api.modifierOperation()` | `ADD` / `MULTIPLY` / `SUBTRACT` / `DIVIDE` |
| `api.modifierScope()` | `INPUT` / `OUTPUT` / `MACHINE` / `RECIPE` |

```js
.use(api.modifierScope().RECIPE)
```

## 5. `StartupEventJS` —— 启动事件对象

> `cn.howxu.mmcr.nekojs.event.StartupEventJS`

### 5.1 声明类

| 方法 | 返回 | 提交方式 |
|---|---|---|
| `event.createMachine(id)` | `MachineBuilderJS` | `.register()` |
| `event.createStructure(id)` | `StructureBuilderJS` | `.build()` |
| `event.createLevelType(id)` | `LevelTypeBuilderJS` | `.register()` |
| `event.createLevel(id)` | `MachineLevelBuilderJS` | `.register()` |

### 5.2 槽位

| 方法 | 说明 |
|---|---|
| `event.levelSlot(typeId)` | 等级槽，`set('L', event.levelSlot('my_mod:voltage'))` |
| `event.modifierSlot(modifierId, replacement)` | 修饰符槽，`replacement` 是那一格的摆放方块条件（必需，否则该符号没有谓词） |

### 5.3 修饰符

| 方法 | 说明 |
|---|---|
| `event.registerModifier(id, bundle)` | 注册修饰符定义 |
| `event.registerModifier(id, specArray)` | 同上，直接传 `api.modifier(...)` 的数组 |
| `event.registerModifierItem(itemId, modifierId)` | 把某个物品变成修饰符载体（插在升级总线里生效） |

### 5.4 JEI workstation

| 方法 | 说明 |
|---|---|
| `event.addRecipePoolWorkstation(poolId, itemId)` | 给配方池页挂一个「工作台」图标 |
| `event.addMachineWorkstation(machineId, recipeTypeId)` | 给机器挂配方类型 |
| `event.addMachineWorkStation(...)` | 上面的别名（大小写 S），兼容旧脚本 |

### 5.5 控制屏文本

| 方法 | 说明 |
|---|---|
| `event.registerControllerScreenText(machineId, callback)` | 回调收到 MMCR 的文本上下文，用来写自定义显示 |

### 5.6 `getAPI()`

等价于 `MMCR.getAPI()`。

## 6. `ServerEventJS`

> `cn.howxu.mmcr.nekojs.event.ServerEventJS`

| 方法 | 说明 |
|---|---|
| `event.getAPI()` → `MMCRApiJS` | 只有这一个 |

没有 `createStructure` / `createRecipe` 是**故意的**——那两个窗口在服务端脚本加载前就关了，给你 builder 只会得到被静默丢弃的声明。配方请用 `ServerEvents.recipes`。

## 7. `ClientInfoEventJS`

> `cn.howxu.mmcr.nekojs.event.ClientInfoEventJS`

| 方法 | 说明 |
|---|---|
| `event.addRecipePoolInfo(poolId, key, ...args)` | 给配方池页加说明文本 |
| `event.addRecipeInfo(recipeId, key, ...args)` | 给单条配方加说明文本 |

`...args` 会作为 `Component.translatable` 的参数传下去。详见 [JEI 信息](JEI)。

## 下一步

- [机器](Machines) · [结构](Structures) · [配方](Recipes)
