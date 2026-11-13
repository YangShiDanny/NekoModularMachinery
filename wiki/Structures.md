# 结构

```js
MMCREvents.startup(event => {
  event.createStructure('my_pack:smelter')
    .pattern('BBB')
    .pattern('BCB')
    .pattern('BBB')
    .set('B', event.getAPI().block('minecraft:iron_block'))
    .set('C', event.getAPI().block('minecraft:iron_block'))
    .controller('C')
    .build()
})
```

⚠️ **结构必须写在 `startup_scripts`**。放 `server_scripts` 不会报错，但永远不会生效——
那时候 MMCR 的结构表已经冻结了。原因见[首页](Home#一个必须先知道的事)。

---

## `pattern()` 的坐标系

这是最容易搞错的地方。MMCR 的解析规则（`MachineDefinitionConverter.toBlockArray`）：

| 维度 | 由什么决定 | 方向 |
|---|---|---|
| **z** | **每次 `pattern()` 调用** | 第 1 次调用 = z 最小层 |
| **y** | 同一次调用里的第几个字符串 | 第 1 个字符串 = **最底层** |
| **x** | 字符串里的第几个字符 | 第 1 个字符 = x 最小 |

也就是说：

```js
.pattern('BBB')   // z = 0，这一层只有 1 行（y=0），3 格宽
.pattern('BCB')   // z = 1
.pattern('BBB')   // z = 2
```

= **3×1×3 的水平地板**。

而：

```js
.pattern('BBB', 'BCB', 'BBB')   // 只调一次 = 只有 1 个 z 层，3 行 y
```

= **3×3×1 的竖直墙面**。

> 一句话：**要平面就多调几次 `pattern()`，要高度就在一次调用里多写几个字符串。**

### 空格：跳过，不是空气

```js
.pattern('B B')
```

中间那个空格表示"**这一格不检查**"，什么方块都行（包括空气）。

要强制要求空气，必须显式声明：

```js
.set('#', api.air())
```

### 尺寸不一致会错位

同一次 `pattern()` 里所有字符串长度必须一致。混进一个长度不同的，后面的字符全部错位，
结构永远拼不上。

---

## `set()` 能放什么

| 值 | 效果 |
|---|---|
| `api.block('minecraft:iron_block')` | 指定方块（忽略状态） |
| `api.state('minecraft:furnace[lit=true]')` | 指定精确状态 |
| `api.tag('minecraft:logs')` | 方块标签 |
| `api.air()` | 必须是空气 |
| `api.any()` | 任意方块 |
| `'minecraft:iron_block'` | 裸 ID 字符串，自动转 `block()` |
| `['a:b', 'c:d']` | 数组，自动转 `anyOf()` |
| `api.itemInput()` 等端口谓词 | 必须是某方向的端口 |
| `event.levelSlot(typeId)` | **等级槽**，方由等级决定 |
| `event.modifierSlot(id)` | **修饰符槽** |

```js
.set('B', api.block('minecraft:iron_block'))
.set('T', api.anyOf('minecraft:blast_furnace', 'minecraft:furnace'))
.set('#', api.air())
.set('P', api.itemInput())                    // 这格必须是物品输入端口
.set('L', event.levelSlot('my_pack:coil'))    // 等级槽
.set('M', event.modifierSlot('my_pack:speed'))// 修饰符槽
```

---

## 控制器

```js
.controller('C')
```

指定哪个符号是**控制器位置**。两个作用：

1. 成型后那一格变成控制器方块
2. 整个结构会**平移**，让控制器落在原点——所以它决定了成型的锚点

不调的话默认是 `'C'`。符号在 `set()` 里必须有定义（哪怕只是占位方块），否则结构缺一格。

---

## 方块状态敏感性

| 方法 | 行为 |
|---|---|
| `stateSensitive()` | 匹配方块**状态**（默认）。`minecraft:furnace[lit=true]` 和 `[lit=false]` 算不同方块 |
| `stateInsensitive()` | 只匹配**类型**，不管状态 |

如果你的结构里用了熔炉、漏斗这类有状态的方块，用 `stateInsensitive()` 会更宽容。

---

## 端口要求

### `portRequirements(map)`

> ⚠️ 这是**成型校验**，不是"声明端口"。端口位置由 MMCR 在结构表面自动分配；
> 这里写的是"成型时必须至少有/最多有 N 个某类端口"，不满足就**成型失败**。

键是 MMCR 的端口族统计名（不带 `mmcr:` 前缀）：

```js
.portRequirements({
  item_input_bus: 1,          // 至少 1 个物品输入总线
  item_output_bus: [1, 4],    // 1~4 个物品输出总线
  energy_input_hatch: 1       // 至少 1 个能量输入舱口
})
```

| 值的写法 | 含义 |
|---|---|
| `1` | 至少 1 个 |
| `[min, max]` | 数量区间 |

不写就完全不校验（任何数量都行）。

### `portTierRequirements(...)`

端口**等级**要求。两种写法：

```js
// 字符串
.portTierRequirements('energy_input_hatch>=NORMAL', 'item_input_bus>=SMALL')

// 数组
.portTierRequirements(['energy_input_hatch', 'NORMAL'], ['item_input_bus'])
```

- 等级名**大小写不敏感**
- 只写族名 = 任意等级都行
- 26.1.2 上不存在的族（`stress` / `source` / `mana` / `air`）会被**跳过并打 warn**，不会崩

---

## 等级槽和修饰符槽

```js
.set('L', event.levelSlot('my_pack:coil'))
.set('M', event.modifierSlot('my_pack:speed'))
.set('M', event.modifierSlot('my_pack:speed', api.block('minecraft:iron_block')))
```

- **等级槽**：那一格放什么由机器"等级"决定。见[等级与修饰符](Levels-and-Modifiers)。
- **修饰符槽**：那一格可以装/卸某个修饰符。第二个参数是"不装时显示什么方块"（默认空气）。

---

## 完整示例

一个 5×5×3 的机器，外壳砖块，内壁熔炉，中间控制器，带端口约束：

```js
MMCREvents.startup(event => {
  const api = event.getAPI()

  event.createMachine('my_pack:big_furnace')
    .appearance('minecraft:bricks')
    .recipePool('my_pack:big_furnace')
    .register()

  // 三层，每层 5 行 5 列
  event.createStructure('my_pack:big_furnace')
    .pattern(
      'BBBBB',
      'BTTTB',
      'BTTTB',
      'BTTTB',
      'BBBBB'
    )
    .pattern(
      'BBBBB',
      'BTTTB',
      'BTCTB',     // C 在正中间
      'BTTTB',
      'BBBBB'
    )
    .pattern(
      'BBBBB',
      'BTTTB',
      'BTTTB',
      'BTTTB',
      'BBBBB'
    )
    .set('B', api.block('minecraft:bricks'))
    .set('T', api.anyOf('minecraft:blast_furnace', 'minecraft:furnace'))
    .set('C', api.block('minecraft:bricks'))
    .controller('C')
    .portRequirements({
      item_input_bus: 1,
      item_output_bus: [1, 4],
      energy_input_hatch: 1
    })
    .portTierRequirements('energy_input_hatch>=NORMAL')
    .build()
})
```

---

## 常见错误

| 现象 | 原因 |
|---|---|
| 结构永远拼不上 | 某次 `pattern()` 的字符串长度不一致，字符错位 |
| 结构拼上了但控制器位置不对 | `.controller()` 指错了符号，或符号在 `set()` 里没定义 |
| 成型失败，提示缺端口 | `portRequirements` 要求的端口类型/数量没装够 |
| 内壁要求空了但不生效 | 用了空格 `' '`（跳过），应该用 `api.air()` |
| 明明写了结构但游戏里没有 | 结构写进了 `server_scripts`，挪到 `startup_scripts` |
