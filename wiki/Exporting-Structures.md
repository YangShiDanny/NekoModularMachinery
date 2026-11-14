# 导出结构

手写 `pattern(...)` 又慢又容易错位。MMCR 提供了游戏内工具，能直接把你搭好的结构导出成 JS 代码。

## 1. 先搭好，再用工具选

1. 在创造模式里搭出你想要的结构（控制器位置先随便放个方块占位）
2. 手持 MMCR 的结构导出工具（或使用命令，取决于你的 MMCR 版本）
3. 选中区域的两个对角
4. 导出 → 得到一段 JS

## 2. 导出的代码长什么样

MMCR 的导出器产出的代码形状与 KubeJS 版一致：

```js
const structure = event.createStructure("mmcr_kubejs:exported_structure")
    .pattern([...])
    .set('B', api.block('minecraft:iron_block'))
    ...
    .build()
```

## 3. 三处必须改

### ① 机器 ID

导出器不知道你的机器叫什么，会用占位 ID。改成你**已注册的机器 ID**：

```js
// 导出器给的
event.createStructure("mmcr_kubejs:exported_structure")

// 改成你的
event.createStructure("my_mod:smelter")
```

注意是**机器 ID**，不是配方池 ID。

### ② 控制器符号

导出器通常把控制器位置标成某个字符。确认两件事：

- `.controller('C')` 里的字符在 pattern 里**只出现一次**
- 那个字符**没有**被 `.set(...)` 绑定过

如果导出器把控制器位置也 `set` 了，删掉那一行 `set`。

### ③ 放进 `startup_scripts`

KubeJS 版导出的是 `server_scripts` 的形状（`event.createStructure`），本附属也能用同样的方法名，但**必须放在 `nekojs/startup_scripts/` 里**，包在 `MMCREvents.startup` 回调中：

```js
MMCREvents.startup(event => {
  const api = event.getAPI()

  event.createStructure('my_mod:smelter')
    .pattern('BBB', 'BCB', 'BBB')
    .set('B', api.block('minecraft:iron_block'))
    .controller('C')
    .build()
})
```

导出器给的是裸语句，你需要自己套上 `MMCREvents.startup(event => { ... })`。

## 4. 反过来理解导出结果

导出器通常会输出多次 `.pattern([...])` 调用。按 [结构](Structures) 里的坐标语义读它：

- 每次调用 = 一个 z 层
- 每个字符串 = 一行，第一个字符串是**最底层**

如果你发现导出的结构在游戏里是「躺着的」而你的建筑是「站着的」，说明你把它当成了 y 层——它其实是 z 层。

## 5. 大结构的处理

超大结构导出后行数会很多。建议：

- 只保留真正需要的 `.pattern(...)` 调用，删掉全空的层
- 用常量抽离重复的层：

```js
const FLOOR = 'BBBBB'
const WALL  = 'B###B'

event.createStructure('my_mod:big')
  .pattern(FLOOR, FLOOR, FLOOR)
  .pattern(WALL,  WALL,  WALL)
  // ...
```

注意 `pattern(...)` 接受字符串参数，也可以接受**字符串数组**，所以可以这么写：

```js
const layer = ['BBBBB', 'B###B', 'BBBBB']

event.createStructure('my_mod:big')
  .pattern(layer)
  .pattern(layer)
  .pattern(layer)
  .set('B', api.block('minecraft:iron_block'))
  .controller('C')
  .build()
```

## 6. 没有导出工具怎么办

手动数。建议流程：

1. 先在纸上/Excel 里画出每一层（z），每层写成若干行（y）
2. 从**最底层**开始写第一行
3. 每层行数一致、每行宽度一致
4. 用 `/mmcr build <机器ID>` 快速试建，看着不对就调

`/mmcr build` 是开发期最省时间的东西——不用手动摆方块，一条命令成型。

## 下一步

- [结构](Structures) —— 完整 API
- [快速开始](Quick-Start) —— 最小可跑的例子
