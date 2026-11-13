# 配方

MMCR 的配方是**数据驱动**的：一种叫 `mmcr:machine_recipe` 的标准配方类型。所以本附属不需要发明新语法——直接用 NekoJS 的 `ServerEvents.recipes`，写法和 KubeJS 版几乎一样。

这意味着配方**可以热重载**：

```
/nekojs reload      ← 改完配方跑这个，不用重启
```

## 1. 三种写法

### 推荐：`event.recipes.mmcr.machine_recipe(...)`

本附属注册了 `mmcr:machine_recipe` 的 schema，所以位置参数可用：

```js
ServerEvents.recipes(event => {
  event.recipes.mmcr.machine_recipe(
    'my_mod:smelter',     // 配方池
    200,                  // tick_time
    [ /* requirements */ ]
  ).id('my_mod:smelter_iron')
})
```

签名（由附属的 handler 提供）：

| 调用 | 说明 |
|---|---|
| `machine_recipe(pool, tickTime, requirements)` | 完整形式 |
| `machine_recipe(pool, requirements)` | 省略 tick_time 时默认 **20** |
| `machine_recipe(jsonObject)` | 直接给一个已含字段的对象，只补 `type` |

`requirements` 可以是数组，也可以是单个对象。不写 `.id(...)` 时 NekoJS 会自动生成 `nekojs:machine_recipe_<hash>` 这样的 ID。

### 等价：`event.custom({...})`

```js
ServerEvents.recipes(event => {
  event.custom({
    type: 'mmcr:machine_recipe',
    recipe_pool: 'my_mod:smelter',
    tick_time: 200,
    requirements: [ /* ... */ ]
  }).id('my_mod:smelter_iron')
})
```

字段名和 [MMCR 官方文档](https://github.com/Nibelungorum/mmcr-wiki) 完全一致，可以直接从 KubeJS 脚本搬过来。

### 数据包 JSON

不写脚本也行，放 `data/<ns>/recipe/<name>.json`：

```json
{
  "type": "mmcr:machine_recipe",
  "recipe_pool": "my_mod:smelter",
  "tick_time": 200,
  "requirements": [
    { "type": "minecraft:item", "io": "input", "item": "minecraft:iron_ore", "count": 1 },
    { "type": "minecraft:item", "io": "output", "stack": { "id": "minecraft:iron_ingot", "count": 1 } },
    { "type": "neoforge:energy", "io": "input", "fe_per_tick": 10 }
  ]
}
```

## 2. 顶层字段

| 字段 | 类型 | 必填 | 默认 | 说明 |
|---|---|---|---|---|
| `recipe_pool` | string | ✅ | — | **配方池 ID**，不是机器 ID |
| `tick_time` | int | ✅ | 20 | 总耗时（tick）。20 tick = 1 秒 |
| `requirements` | array | ✅ | — | 输入/输出/条件，见下节 |
| `priority` | int | | 0 | 同池多条配方时的匹配优先级，大的先 |
| `max_threads` | int | | 1 | 该配方允许的最大线程数 |
| `parallelized` | boolean | | false | 是否允许并行执行多份 |
| `allow_partial_outputs` | boolean | | false | 输出装不下时是否允许部分产出 |
| `cancelIfPerTickFails` | boolean | | false | 某一 tick 失败时是否整条取消 |
| `required_host_ids` | array | | [] | 要求机器主机是哪些 ID |
| `outputs` / `modifiers` | array | | — | 内部字段，一般不用手写 |

## 3. requirement 类型

每一项靠 `type` 决定形状，靠 `io` 决定输入还是输出。下面这一份把常见类型全放进来了：

```js
const requirements = [
  // 物品输入：item + count
  { type: 'minecraft:item', io: 'input', item: 'minecraft:iron_ore', count: 1 },
  // 物品输入（标签）：tag + count
  { type: 'minecraft:item', io: 'input', tag: 'forge:ores/iron', count: 1 },
  // 物品输出：必须用 stack，别写成输入的 item 格式
  { type: 'minecraft:item', io: 'output', stack: { id: 'minecraft:iron_ingot', count: 1 } },
  // 物品输出 + 概率
  { type: 'minecraft:item', io: 'output', stack: { id: 'minecraft:gold_nugget', count: 1 }, chance: 0.25 },

  // 流体：amount 单位 mB，一桶 = 1000
  { type: 'minecraft:fluid', io: 'input',  fluid: { fluid: 'minecraft:water' }, amount: 1000 },
  { type: 'minecraft:fluid', io: 'output', stack: { id: 'minecraft:lava', amount: 500 } },

  // 能量：fe_per_tick 是功率，不是总量
  { type: 'neoforge:energy', io: 'input', fe_per_tick: 10 },

  // 等级需求：要求结构里装了某个等级
  { type: 'mmcr:level', io: 'input', type_id: 'my_mod:coil', level_id: 'my_mod:coil_advanced' },
  // 阶段需求
  { type: 'mmcr:stage', io: 'input', stage: 2 },
  // 智能接口需求
  { type: 'mmcr:smart_interface', io: 'input', name: 'temperature', min: 500 }
]
```

把它们放进配方：

```js
ServerEvents.recipes(event => {
  event.recipes.mmcr.machine_recipe('my_mod:smelter', 200, requirements)
    .id('my_mod:smelter_iron')
})
```

### 各类型说明

**物品** —— 输入用 `item`（单个）或 `tag`（标签），输出必须用 `stack: { id, count }`。这是最容易写错的一处。

**流体** —— `amount` 单位是 mB，一桶 = 1000。输出同样用 `stack: { id, amount }`。

**能量** —— `fe_per_tick` 是**每 tick 的功率**，不是总量。总耗能 = `fe_per_tick × tick_time × 并行数`。

**等级 / 阶段 / 智能接口** —— `mmcr:` 命名空间下，属于「执行条件」而不是材料。`mmcr:level` 要求结构里装了对应等级槽且等级够高。

**联动模组** —— 联动类型由各自命名空间提供，例如 `botania:mana`、`ars_nouveau:source`、`pneumaticcraft:air`、`mekanism:chemical`。

注意：26.1.2 的 MMCR 没有这些联动，写了会被当成未知类型。用 `Platform.isLoaded('botania')` 判断后再加。

## 4. 概率产出

物品输出可以带 `chance`（0~1 的浮点），表示每个产出周期的成功概率：

```js
const bonus = {
  type: 'minecraft:item',
  io: 'output',
  stack: { id: 'minecraft:gold_nugget', count: 1 },
  chance: 0.25
}
```

配合顶层字段 `allow_partial_outputs: true`，可以让一次产出部分成功（而不是要么全给要么全不给）。

## 5. 并行

要真正并行，三个条件都要满足：

1. 机器上 `.allowParallelism()` + `.maxParallelAmount(n)`
2. 配方上 `"parallelized": true`
3. 机器内部有足够的输入/输出空间

```js
event.recipes.mmcr.machine_recipe('my_mod:smelter', [
  { type: 'minecraft:item', io: 'input',  item: 'minecraft:iron_ore', count: 1 },
  { type: 'minecraft:item', io: 'output', stack: { id: 'minecraft:iron_ingot', count: 1 } }
]).id('my_mod:smelter_iron')
```

要在 JSON 里开并行，用 `event.custom`：

```js
event.custom({
  type: 'mmcr:machine_recipe',
  recipe_pool: 'my_mod:smelter',
  tick_time: 200,
  parallelized: true,
  max_threads: 8,
  requirements: [ /* ... */ ]
})
```

## 6. 配方怎么被选中

机器每个 tick 会从它绑定的所有配方池里找一条**能被执行**的配方。选择顺序：

1. 按 `priority` 从高到低
2. 同优先级按注册顺序
3. 第一条「输入够、空间够、条件满足」的胜出

找不到就进空闲，触发 [行为回调](Behaviors) 里的 `idleStart`。

## 7. 调试

| 现象 | 原因 |
|---|---|
| JEI 里有配方，机器不跑 | 接口位置没设 `itemInput()` 之类的条件；或没通电 |
| 配方完全不出现 | `recipe_pool` 写成了机器 ID（两者可以同名但是不同字段）；或脚本有错 |
| `/nekojs reload` 后配方还在 | 该配方来自数据包而不是脚本；用 `/reload` 而不是 `/nekojs reload` |
| 改了配方没生效 | 检查 `ServerEvents.recipes` 是否在 `server_scripts` 里 |

`/nekojs error` 能看到脚本错误。

## 下一步

- [行为回调](Behaviors) —— 在配方开始/每 tick/结束时插手
- [JEI 信息](JEI) —— 给配方页加说明文字
