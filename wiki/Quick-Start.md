# 快速开始

从零做一台能跑的机器：**铁熔炉**——3×3 的铁架，中心是控制器，四周留 4 个接口位，1 铁矿 + 10 FE/t 换 1 铁锭。

> 本页假设你已按 [安装](Installation) 装好 MMCR + NekoJS + GraalMC + 本附属，并成功启动过一次游戏（`nekojs/` 目录已生成）。

## 目录职责

| 目录 | 放什么 | 改完怎么生效 |
|---|---|---|
| `nekojs/startup_scripts/` | 机器定义、结构、等级、修饰符 | 重启游戏 |
| `nekojs/server_scripts/` | 配方（`ServerEvents.recipes`） | `/nekojs reload` |
| `nekojs/client_scripts/` | JEI 配方说明 | `F3+T` |
| `nekojs/assets/<ns>/lang/` | 翻译 | `F3+T` |

子目录（如 `startup_scripts/machines/`）只是整理用，不影响加载阶段。

## 第 1 步：注册机器定义

新建 `nekojs/startup_scripts/smelter.js`：

```js
MMCREvents.startup(event => {
  event
    .createMachine('my_mod:smelter')
    .recipePool('my_mod:smelter')
    .displayNameKey('machine.my_mod.smelter')
    .register()
})
```

逐个说：

- `MMCREvents.startup(cb)` —— 本附属注册的事件组。回调在 NekoJS 的 `STARTUP` 阶段执行。
- `event.createMachine(id)` —— 返回 `MachineBuilderJS`，参数是 `命名空间:名字`。
- `.recipePool(...)` —— 声明这台机器读哪个配方池。省略时默认用机器 ID 同名池。**机器 ID 和配方池 ID 是两个独立概念**，多个机器可以共用一个池。
- `.displayNameKey(key)` —— 翻译键。惯例是 `machine.<命名空间>.<名字>`，但任意键都行。
- `.register()` —— 提交。此时 MMCR 会生成控制器方块，但还没有结构、没有配方。

最简版本可以只写 `event.createMachine('my_mod:smelter').register()`。

## 第 2 步：绑定结构

**同一份脚本**里继续写（不要挪到 `server_scripts`，原因见 [生命周期](Getting-Started)）：

```js
MMCREvents.startup(event => {
  const api = event.getAPI()

  event
    .createMachine('my_mod:smelter')
    .recipePool('my_mod:smelter')
    .displayNameKey('machine.my_mod.smelter')
    .register()

  event
    .createStructure('my_mod:smelter')
    .pattern(
      'BHB',   // y = 0，最底层
      'HCH',   // y = 1
      'BHB'    // y = 2，最顶层
    )
    .set('B', api.block('minecraft:iron_block'))
    .set('H', api.anyOf(
      api.block('minecraft:iron_block'),
      api.itemInput(),
      api.itemOutput(),
      api.energyInput()
    ))
    .controller('C')
    .build()
})
```

### 坐标怎么数

`pattern()` **每调用一次就是一个 z 层**；同一次调用里的每个字符串是一行（x 序列），**第一个字符串是 y 最小（最底层）**。

```
.pattern('BHB', 'HCH', 'BHB')
        │      │      └── y=2  最顶层
        │      └───────── y=1  中间层
        └──────────────── y=0  最底层
```

想往上堆第二层，再调一次 `pattern(...)`——它会成为 z=1 的那一片：

```js
.pattern('BHB', 'HCH', 'BHB')   // z = 0
.pattern('BBB', 'B#B', 'BBB')   // z = 1
```

三个坑：

1. **每次 `pattern()` 里的行必须等宽**。写了 3 个字符就别混进 4 个的，否则整个结构错位且不报错。
2. **空格 `' '` 不是空气**，是「这一格不检查」。要强制空气用 `api.air()`。
3. **`controller('C')` 的符号不要再用 `set('C', ...)` 绑一次**。控制器方块由 MMCR 生成，你只需要占位。

### 关于 `H`

`H` 是接口预留位：`api.anyOf(...)` 表示「这些条件满足任意一个都算对」。所以这个位置放铁块也行，放物品输入接口/物品输出接口/能量接口也行。

如果不做这一步，结构成型后没有任何地方能接管道和线缆，配方也就没法输入输出。

## 第 3 步：写翻译键

`nekojs/assets/my_mod/lang/zh_cn.json`：

```json
{
  "machine.my_mod.smelter": "铁熔炉",
  "recipe_pool.my_mod.smelter": "铁熔炉",
  "item.mmcr.smelter_controller": "铁熔炉 控制器",
  "block.mmcr.smelter_controller": "铁熔炉 控制器"
}
```

控制器方块**固定在 `mmcr` 命名空间**，你的机器叫 `my_mod:smelter`，控制器就是 `mmcr:smelter_controller`。

## 第 4 步：写配方

新建 `nekojs/server_scripts/smelter_recipes.js`。MMCR 的配方就是 `mmcr:machine_recipe` 这个标准配方类型，所以用 NekoJS 的 `ServerEvents.recipes` 即可——和 KubeJS 版写法一致。

**推荐写法**（用本附属注册的 schema，位置参数）：

```js
ServerEvents.recipes(event => {
  event.recipes.mmcr.machine_recipe(
    'my_mod:smelter',   // 配方池 ID
    200,                // tick_time，200 tick = 10 秒
    [
      { type: 'minecraft:item', io: 'input',  item: 'minecraft:iron_ore', count: 1 },
      { type: 'minecraft:item', io: 'output', stack: { id: 'minecraft:iron_ingot', count: 1 } },
      { type: 'neoforge:energy', io: 'input', fe_per_tick: 10 }
    ]
  ).id('my_mod:smelter_iron')
})
```

**原始 JSON 写法**（等价，字段名和 MMCR 官方文档完全一致）：

```js
ServerEvents.recipes(event => {
  event.custom({
    type: 'mmcr:machine_recipe',
    recipe_pool: 'my_mod:smelter',
    tick_time: 200,
    requirements: [
      { type: 'minecraft:item', io: 'input',  item: 'minecraft:iron_ore', count: 1 },
      { type: 'minecraft:item', io: 'output', stack: { id: 'minecraft:iron_ingot', count: 1 } },
      { type: 'neoforge:energy', io: 'input', fe_per_tick: 10 }
    ]
  }).id('my_mod:smelter_iron')
})
```

字段含义：

| 字段 | 说明 |
|---|---|
| `type` | 固定 `mmcr:machine_recipe` |
| `recipe_pool` | **配方池 ID**，不是机器 ID（本例两者同名，但不是必须） |
| `tick_time` | 总耗时，tick，必须 > 0 |
| `requirements` | 数组；每项用 `type` 定类型、`io` 定输入输出 |

物品输入用 `item` + `count`；物品输出必须用 `stack: { id, count }`——**别把输出写成输入的格式**，这是最常见的错。

进游戏后 `/nekojs reload`，JEI 里就能看到这条配方了。

## 第 5 步：验证

1. `/mmcr build my_mod:smelter` —— 快速修建，不用手动摆
2. 右键控制器成型
3. 往接口里塞铁矿 + 通电
4. 看它开始工作

## 完整脚本

`nekojs/startup_scripts/smelter.js`：

```js
MMCREvents.startup(event => {
  const api = event.getAPI()

  event.createMachine('my_mod:smelter')
    .recipePool('my_mod:smelter')
    .displayNameKey('machine.my_mod.smelter')
    .register()

  event.createStructure('my_mod:smelter')
    .pattern('BHB', 'HCH', 'BHB')
    .set('B', api.block('minecraft:iron_block'))
    .set('H', api.anyOf(
      api.block('minecraft:iron_block'),
      api.itemInput(), api.itemOutput(), api.energyInput()
    ))
    .controller('C')
    .build()
})
```

`nekojs/server_scripts/smelter_recipes.js`：见第 4 步。

## 下一步

- [结构](Structures) —— 多层结构、等级槽、端口数量限制
- [配方](Recipes) —— 流体、概率产出、并行、等级需求
- [机器](Machines) —— 外观、音效、tick 行为、并行度
