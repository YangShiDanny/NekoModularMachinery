# 结构

> `cn.howxu.mmcr.nekojs.builder.StructureBuilderJS`
>
> 由 `event.createStructure(machineId)` 返回，最后 `.build()` 提交。

注意参数是**已注册的机器 ID**，不是配方池 ID。

**只能在 `startup_scripts` 里声明。** 这是本附属与 KubeJS 版最重要的一处差异——KubeJS 可以把结构放 `server_scripts` 靠 `/reload` 热改，NekoJS 不行，原因见 [生命周期](Getting-Started)。

## 坐标语义（务必先读）

`pattern()` **每调用一次 = 一个 z 层**。同一次调用里：

- 每个字符串 = 一行，沿 **x** 方向一个字符一格
- **第一个字符串是 y 最小（最底层）**
- 多次调用沿 **z** 方向依次堆叠

```
.pattern('ABC', 'DEF', 'GHI')
         │      │      └── y=2  z=0 的这一片里最高的那行
         │      └───────── y=1
         └──────────────── y=0 最底层
```

```js
.pattern('BBB', 'BCB', 'BBB')   // z = 0 这一片
.pattern('BBB', 'B#B', 'BBB')   // z = 1 这一片
```

坑：

| 坑 | 后果 |
|---|---|
| 同一次 `pattern()` 里行宽不一致 | 结构整体错位，**不报错** |
| 以为第一个字符串是最上层 | 结构上下颠倒，成型后看着怪但能成 |
| 以为空格 `' '` 是空气 | 空格是「这一格不检查」，要空气请用 `api.air()` |
| 把 `pattern()` 当成「一次传完整面墙」 | 一面竖直墙其实是一层，不是多层 |

## 1. `pattern(...rows)`

追加一个 z 切片。接受可变参数或字符串数组：

```js
.pattern('BBB', 'B#B', 'BBB')
.pattern(['BBB', 'B#B', 'BBB'])
```

## 2. `set(symbol, condition)`

给一个**单字符**绑定匹配条件。

```js
.set('B', api.block('minecraft:iron_block'))
.set('#', api.air())
.set('T', api.tag('minecraft:logs'))
.set('P', api.state('minecraft:purpur_pillar[axis=y]'))
```

`condition` 可以是：

| 写法 | 含义 |
|---|---|
| `api.block(id)` | 指定方块，忽略状态 |
| `api.state(id)` | 方块 + 状态精确匹配 |
| `api.tag(id)` | 方块标签 |
| `api.air()` | 空气 |
| `api.any()` | 任意 |
| `api.anyOf(...)` | 任一满足 |
| 直接给字符串 `'minecraft:iron_block'` | 等价于 `api.block(...)` |
| `event.levelSlot(...)` | 等级槽（见第 5 节） |
| `event.modifierSlot(...)` | 修饰符槽 |

## 3. `controller(symbol)`

指定哪个字符所在的位置放控制器。

```js
.controller('C')
```

规则：

- 该符号**不要**再用 `set(...)` 绑定。控制器方块由 MMCR 生成，绑定会冲突。
- 该符号在 pattern 里**只应出现一次**。出现多次时 MMCR 只会取其中一处，行为不可预期。
- 不调 `controller(...)` 时默认找 `'C'`。

## 4. 状态敏感

| 方法 | 说明 |
|---|---|
| `stateSensitive()` | 默认。方块状态也要匹配（`[axis=y]` 这类） |
| `stateInsensitive()` | 只匹配方块种类，不管状态 |

如果你的结构里有原木、楼梯这种状态很多的方块，用 `stateInsensitive()` 能避免玩家摆错朝向。

## 5. 等级槽与修饰符槽

等级槽让机器可以「升级」：结构里的某个位置放不同等级的方块，机器性能不同。

```js
event.createLevelType('my_mod:coil').displayNameKey('level_type.my_mod.coil').register()
event.createLevel('my_mod:coil_basic').type('my_mod:coil').state('minecraft:copper_block').register()

event.createStructure('my_mod:smelter')
  .pattern('BLB', 'LCL', 'BLB')
  .set('B', api.block('minecraft:iron_block'))
  .set('L', event.levelSlot('my_mod:coil'))   // ← 等级槽
  .controller('C')
  .build()
```

修饰符槽：

```js
.set('M', event.modifierSlot('my_mod:overclock'))
.set('M', event.modifierSlot('my_mod:overclock', api.block('minecraft:iron_block')))
```

第二个参数是「这个位置没插修饰符时允许用哪个方块占位」。

## 6. 端口数量限制

### `portRequirements(map)`

约束成型时某族接口的**数量**。这是**成型校验**，不满足就成不了型。

```js
.portRequirements({
  item_input_bus: 1,          // 至少 1 个
  item_output_bus: [1, 4],    // 1 到 4 个
  energy_input_hatch: 1
})
```

### `portTierRequirements(...values)`

约束接口的**最低等级**。

```js
.portTierRequirements(
  'item_input_bus>=NORMAL',
  ['energy_input_hatch', 'ADVANCED']
)
```

- `'family>=TIER'` 打包写，或 `['family', 'TIER']` 分开写
- 只给族名不给等级，表示「这一族任意等级都行」
- 族名在**当前 MC 版本不存在**时打一条 warn 并跳过（不崩）

### 端口族名

1.21.1 与 26.1.2 的族数量不同，26.1.2 是 1.21.1 的子集。常见族：

```
item_input_bus / item_output_bus
fluid_input_hatch / fluid_output_hatch
energy_input_hatch / energy_output_hatch
chemical_input_hatch / chemical_output_hatch
heat_input_hatch / heat_output_hatch
```

等级名通常是 `NORMAL` / `ADVANCED` / `ELITE` / `ULTIMATE` 这类，具体以 MMCR 的端口定义为准。写错等级不会崩，只会打 warn 跳过。

> `portRequirements` 不会「创建」端口。端口位置是 MMCR 在结构表面自动分配的，你只能约束数量与等级。想让某个具体位置能接管道，要用 `api.itemInput()` 这类条件去 `set(...)`——见 [快速开始](Quick-Start) 的 `H` 例子。

## 7. `build()`

提交结构声明。

```js
.build()
```

调用前会校验 `pattern()` 至少被调用过一次，否则抛 `IllegalStateException: ... declared no pattern()`。

## 完整例子：三层结构

```js
MMCREvents.startup(event => {
  const api = event.getAPI()

  event.createStructure('my_mod:smelter')
    .pattern('BBB', 'BHB', 'BBB')    // z=0
    .pattern('BHB', 'HCH', 'BHB')    // z=1  控制器在正中
    .pattern('BBB', 'BHB', 'BBB')    // z=2
    .set('B', api.block('minecraft:iron_block'))
    .set('H', api.anyOf(
      api.block('minecraft:iron_block'),
      api.itemInput(), api.itemOutput(), api.energyInput()
    ))
    .controller('C')
    .stateInsensitive()
    .portRequirements({ item_input_bus: 1, item_output_bus: 1 })
    .build()
})
```

## 下一步

- [等级与修饰符](Levels-and-Modifiers) —— 让结构里的槽位真正生效
- [导出结构](Exporting-Structures) —— 用游戏内工具直接生成这段代码
