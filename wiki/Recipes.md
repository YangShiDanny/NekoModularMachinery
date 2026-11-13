# 配方

配方写在 `server_scripts/`，改完 `/nekojs reload` 生效，**不用重启**。

---

## 三种写法

### A. schema 形式（推荐）

附属注册了 `mmcr:machine_recipe` 的脚本 schema，开箱可用：

```js
const api = MMCR.getAPI()

ServerEvents.recipes(event => {
  event.recipes.mmcr.machine_recipe('my_pack:smelter', 100, [
    api.itemInputRequirement('minecraft:iron_ore', 1),
    api.energyRequirement('input', 20),
    api.itemOutputRequirement('minecraft:iron_ingot', 1, 1.0)
  ]).id('my_pack:iron_smelting')
})
```

参数：**配方池 ID**、**加工时间（tick）**、**需求数组**。

### B. 原生 JSON

不依赖 schema，任何情况下都能用：

```js
event.custom({
  type: 'mmcr:machine_recipe',
  recipe_pool: 'my_pack:smelter',
  tick_time: 100,
  requirements: [
    { type: 'minecraft:item', io: 'input', item: 'minecraft:iron_ore', count: 1 },
    { type: 'neoforge:energy', io: 'input', fe_per_tick: 20 },
    { type: 'minecraft:item', io: 'output', stack: { id: 'minecraft:iron_ingot', count: 1 }, chance: 1.0 }
  ]
}).id('my_pack:iron_smelting_2')
```

### C. 门面拼装

```js
event.custom(api.machineRecipe('my_pack:smelter', 100,
  api.itemInputRequirement('minecraft:iron_ore', 1),
  api.energyRequirement('input', 20),
  api.itemOutputRequirement('minecraft:iron_ingot', 1, 1.0)
)).id('my_pack:iron_smelting_3')
```

三种方式产出的 JSON 完全等价，选顺手的就行。

---

## 需求项速查

| 类型 | 输入 | 输出 |
|---|---|---|
| **物品** | `{type:'minecraft:item', io:'input', item, count}` | `{type:'minecraft:item', io:'output', stack:{id,count}, chance}` |
| **流体** | `{type:'minecraft:fluid', io:'input', fluid:{fluid:id}, amount}` | `{type:'minecraft:fluid', io:'output', stack:{id,amount}}` |
| **能量** | `{type:'neoforge:energy', io:'input', fe_per_tick}` | `{type:'neoforge:energy', io:'output', fe_per_tick}` |

用门面构造最不容易写错：

| 方法 | 产出 |
|---|---|
| `api.itemInputRequirement(itemId, count)` | 物品输入 |
| `api.itemOutputRequirement(itemId, count, chance)` | 物品输出 |
| `api.fluidInputRequirement(fluidId, amount)` | 流体输入 |
| `api.fluidOutputRequirement(fluidId, amount, chance)` | 流体输出 |
| `api.energyRequirement(io, fePerTick)` | 能量（`io` 是 `'input'` / `'output'`） |

> ⚠️ **流体输入的 `fluid` 必须是对象** `{fluid:'minecraft:water'}`，不能写裸字符串
> `'minecraft:water'`。写错的话整个配方会被**静默丢弃**——不报错，但机器里就是找不到。

---

## 配方字段

| 字段 | 类型 | 必填 | 默认 | 说明 |
|---|---|---|---|---|
| `recipe_pool` | string | ✅ | — | 配方池，要和机器的 `recipePool()` 对上 |
| `tick_time` | int ≥ 1 | ✅ | — | 加工时间，20 = 1 秒 |
| `requirements` | array | ✅ | — | 需求项列表 |
| `outputs` | array | | — | 额外的输出定义 |
| `modifiers` | array | | — | 配方自带的修饰符 |
| `priority` | int | | `0` | 同一池内优先级 |
| `max_threads` | int | | `1` | |
| `parallelized` | bool | | `false` | |
| `allow_partial_outputs` | bool | | `false` | 是否允许部分产出 |
| `cancelIfPerTickFails` | bool | | `false` | |
| `required_host_ids` | array | | `[]` | 需要哪些宿主机器 |

> ⚠️ 旧字段 `machine` / `inputs` / `fluid_outputs` / `energy_per_tick` / `machine_outputs` /
> `level_requirements` 会被**拒绝**。如果你从旧版 MMCR 的脚本搬过来，注意改名。

---

## 概率产出

第三个参数是概率，`1.0` = 100%：

```js
api.itemOutputRequirement('minecraft:gold_nugget', 1, 0.25)   // 25%
```

多次产出就写多条需求项。

---

## 配方池

配方池是**机器和配方之间的连接点**：

```js
// 机器声明它读哪个池
event.createMachine('my_pack:smelter')
  .recipePool('my_pack:smelter')
  .register()

// 配方声明它属于哪个池
event.recipes.mmcr.machine_recipe('my_pack:smelter', 100, [...]).id('...')
```

不调 `recipePool()` 时，默认池**就是机器自己的 ID**。所以最简单的机器连这句都不用写。

想多个机器共享配方，就让它们 `recipePool()` 同一个池名。

---

## 加等级要求

配方可以要求机器达到某个等级：

```js
// 用 RecipeBuilderJS（只能写在 startup_scripts）
event.createRecipe('my_pack:hard_recipe')
  .recipePool('my_pack:smelter')
  .duration(200)
  .levelRequirement('my_pack:coil', 'my_pack:coil_elite')
  .inputItem(...)
  .build()
```

> ⚠️ `RecipeBuilderJS`（`event.createRecipe`）**只能在 `startup_scripts` 用**。
> 它在构造函数里就检查 MMCR 的配方注册窗口，在 `server_scripts` 调用会直接抛异常。
> 日常写配方用 `ServerEvents.recipes` 就够。

---

## 常见错误

| 现象 | 原因 |
|---|---|
| 配方不出现，也不报错 | 流体输入写了裸字符串，整个配方被丢弃 |
| 配方不出现 | `recipe_pool` 和机器的 `recipePool()` 对不上 |
| 配方不出现 | 写进了 `startup_scripts` 的 `ServerEvents` 回调（那个事件不在 startup 阶段） |
| `/nekojs reload` 后配方没变 | 检查日志里有没有解析错误；错误的配方是静默跳过的 |
| 机器有配方但不动 | 缺能量/输入；或者结构没成型 |
