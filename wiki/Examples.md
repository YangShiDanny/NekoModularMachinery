# 全部示例

MMCR 官方 wiki 的「全部示例」是按机器逐页给的 KubeJS 脚本。本页给出**翻译规则**和几个完整的 NekoJS 版本，你可以把官方示例逐页套过来。

官方示例页（`mmcr-wiki/zh-cn/KubeJS/` 下）：

| 官方示例 | 主题 | 本页覆盖 |
|---|---|---|
| `blast-furnace` | 高炉，最基础的配方式机器 | ✅ 示例 1 |
| `thermal-smelter` | 热力熔炉，带等级 | ✅ 示例 2 |
| `alloy-furnace` | 合金炉，多输入 | ✅ 示例 3 |
| `distillation-tower` | 蒸馏塔，流体进出 | ✅ 示例 4 |
| `cracker` | 裂解机，并行 | ✅ 示例 5 |
| `reactor` / `purpur-furnace` | 反应堆 / 紫珀炉 | 用示例 1–5 的组合可达 |
| `space-elevator` | 太空电梯，自定义 tick | 需要 [行为回调](Behaviors) |
| `compute-network-*` | 计算网络 | 需要 MMCR 网络接口，超出本页范围 |
| `data-store-test-machine` | 数据存储 | 同上 |
| `*-tick-test-machine` | tick 测试 | 参考 [行为回调](Behaviors) |

## 翻译规则：KubeJS → NekoJS

官方示例基本都是这五个替换，逐条改就行。

| # | KubeJS 写法 | NekoJS 写法 |
|---|---|---|
| 1 | 文件放 `kubejs/startup_scripts/` | 放 `nekojs/startup_scripts/` |
| 2 | 文件放 `kubejs/server_scripts/`（结构） | **挪到 `startup_scripts`**（关键差异） |
| 3 | `ServerEvents.recipes(event => ...)` | 不变，NekoJS 同名 |
| 4 | `api.anyOfItemInput()` | `api.itemInput()` |
| 5 | `api.anyOfItemOutput()` / `api.anyOfEnergyInput()` | `api.itemOutput()` / `api.energyInput()` |

> 第 4、5 条是方法名差异：MMCR 的 `KubeJSApi` 用 `anyOfXxx` 前缀，本附属的 `MMCRApiJS` 用裸名。功能完全一样。

`event.createMachine` / `createStructure` / `.pattern` / `.set` / `.controller` / `.register` / `.build` 全部**同名同义**，不用改。

---

## 示例 1：高炉（最基础）

对应官方 `blast-furnace`。3×3×3 铁壳，中心控制器，四面接口位。

```js
// nekojs/startup_scripts/blast_furnace.js
MMCREvents.startup(event => {
  const api = event.getAPI()

  event.createMachine('my_mod:blast_furnace')
    .recipePool('my_mod:blast_furnace')
    .displayNameKey('machine.my_mod.blast_furnace')
    .allowParallelism()
    .maxParallelAmount(4)
    .register()

  event.createStructure('my_mod:blast_furnace')
    .pattern('BHB', 'HBH', 'BHB')
    .pattern('BHB', 'HCH', 'BHB')
    .pattern('BHB', 'HBH', 'BHB')
    .set('B', api.block('minecraft:blast_furnace'))
    .set('H', api.anyOf(
      api.block('minecraft:blast_furnace'),
      api.itemInput(), api.itemOutput(), api.energyInput()
    ))
    .controller('C')
    .build()
})
```

```js
// nekojs/server_scripts/blast_furnace_recipes.js
ServerEvents.recipes(event => {
  event.recipes.mmcr.machine_recipe('my_mod:blast_furnace', 100, [
    { type: 'minecraft:item', io: 'input',  item: 'minecraft:iron_ore', count: 1 },
    { type: 'minecraft:item', io: 'output', stack: { id: 'minecraft:iron_ingot', count: 1 } },
    { type: 'neoforge:energy', io: 'input', fe_per_tick: 20 }
  ]).id('my_mod:blast_furnace_iron')
})
```

---

## 示例 2：热力熔炉（带等级槽）

对应官方 `thermal-smelter`。线圈位置可以换铜/金/钻石，等级越高越快。

```js
MMCREvents.startup(event => {
  const api = event.getAPI()

  // 等级类型
  event.createLevelType('my_mod:coil').displayNameKey('level_type.my_mod.coil').register()

  // 三个档位
  event.createLevel('my_mod:coil_copper')
    .type('my_mod:coil').state('minecraft:copper_block').register()

  event.createLevel('my_mod:coil_gold')
    .type('my_mod:coil').state('minecraft:gold_block').priority(10)
    .modifier(api.modifierDefinition(
      api.modifier('duration', 'recipe', 0.75, 'multiply', false)
    ))
    .register()

  event.createLevel('my_mod:coil_diamond')
    .type('my_mod:coil').state('minecraft:diamond_block').priority(20)
    .modifier(api.modifierDefinition(
      api.modifier('duration', 'recipe', 0.5, 'multiply', false),
      api.modifier('energy',   'recipe', 0.8, 'multiply', false)
    ))
    .register()

  // 机器
  event.createMachine('my_mod:thermal_smelter')
    .recipePool('my_mod:thermal_smelter')
    .displayNameKey('machine.my_mod.thermal_smelter')
    .register()

  // 结构：4 个线圈位
  event.createStructure('my_mod:thermal_smelter')
    .pattern('BLB', 'LCL', 'BLB')
    .set('B', api.block('minecraft:iron_block'))
    .set('L', event.levelSlot('my_mod:coil'))
    .controller('C')
    .build()
})
```

配方里要求最低等级：

```js
ServerEvents.recipes(event => {
  event.recipes.mmcr.machine_recipe('my_mod:thermal_smelter', 200, [
    { type: 'minecraft:item', io: 'input', item: 'minecraft:raw_gold', count: 2 },
    { type: 'minecraft:item', io: 'output', stack: { id: 'minecraft:gold_ingot', count: 2 } },
    { type: 'neoforge:energy', io: 'input', fe_per_tick: 30 },
    { type: 'mmcr:level', io: 'input', type_id: 'my_mod:coil', level_id: 'my_mod:coil_gold' }
  ]).id('my_mod:thermal_smelter_gold')
})
```

---

## 示例 3：合金炉（多输入）

对应官方 `alloy-furnace`。两个输入 + 概率副产物。

```js
MMCREvents.startup(event => {
  const api = event.getAPI()

  event.createMachine('my_mod:alloy_furnace')
    .recipePool('my_mod:alloy_furnace')
    .displayNameKey('machine.my_mod.alloy_furnace')
    .allowModifiers()
    .register()

  event.createStructure('my_mod:alloy_furnace')
    .pattern('BHB', 'HCH', 'BHB')
    .set('B', api.block('minecraft:deepslate_bricks'))
    .set('H', api.anyOf(api.block('minecraft:deepslate_bricks'), api.itemInput(), api.itemOutput(), api.heatInput()))
    .controller('C')
    .build()
})
```

```js
ServerEvents.recipes(event => {
  event.recipes.mmcr.machine_recipe('my_mod:alloy_furnace', 300, [
    { type: 'minecraft:item', io: 'input', item: 'minecraft:copper_ingot', count: 3 },
    { type: 'minecraft:item', io: 'input', item: 'minecraft:tin_ingot', count: 1 },
    { type: 'minecraft:item', io: 'output', stack: { id: 'my_mod:bronze_ingot', count: 4 } },
    { type: 'minecraft:item', io: 'output', stack: { id: 'my_mod:slag', count: 1 }, chance: 0.35 },
    { type: 'neoforge:energy', io: 'input', fe_per_tick: 40 }
  ]).id('my_mod:bronze')
})
```

`chance: 0.35` 是 35% 概率产出。配合机器上 `allow_partial_outputs` 可以让一次产出部分成功。

---

## 示例 4：蒸馏塔（流体进出）

对应官方 `distillation-tower`。竖向塔，输入原油流体，产出两种流体。

```js
MMCREvents.startup(event => {
  const api = event.getAPI()

  event.createMachine('my_mod:distillery')
    .recipePool('my_mod:distillery')
    .displayNameKey('machine.my_mod.distillery')
    .register()

  // 3 层高的塔
  event.createStructure('my_mod:distillery')
    .pattern('BHB', 'HFH', 'BHB')
    .pattern('BFB', 'FCF', 'BFB')
    .pattern('BHB', 'HFH', 'BHB')
    .set('B', api.block('minecraft:iron_block'))
    .set('H', api.anyOf(api.block('minecraft:iron_block'), api.fluidInput(), api.fluidOutput()))
    .set('F', api.block('minecraft:glass'))
    .controller('C')
    .build()
})
```

```js
ServerEvents.recipes(event => {
  event.recipes.mmcr.machine_recipe('my_mod:distillery', 400, [
    { type: 'minecraft:fluid', io: 'input',  fluid: { fluid: 'my_mod:crude_oil' }, amount: 1000 },
    { type: 'minecraft:fluid', io: 'output', stack: { id: 'my_mod:diesel', amount: 600 } },
    { type: 'minecraft:fluid', io: 'output', stack: { id: 'my_mod:heavy_residue', amount: 200 } },
    { type: 'neoforge:energy', io: 'input', fe_per_tick: 80 }
  ]).id('my_mod:refine_crude_oil')
})
```

注意 `io: 'input'` 用 `fluid` 字段、`io: 'output'` 用 `stack` 字段——和物品的规则一致。

---

## 示例 5：裂解机（并行 + 修饰符）

对应官方 `cracker`。允许并行、允许插超频芯片。

```js
MMCREvents.startup(event => {
  const api = event.getAPI()

  event.registerModifier('my_mod:overclock', [
    api.modifier('duration', 'recipe', 0.6, 'multiply', false),
    api.modifier('energy',   'recipe', 1.5, 'multiply', false)
  ])
  event.registerModifierItem('my_mod:overclock_chip', 'my_mod:overclock')

  event.createMachine('my_mod:cracker')
    .recipePool('my_mod:cracker')
    .displayNameKey('machine.my_mod.cracker')
    .allowModifiers()
    .allowParallelism()
    .maxParallelAmount(8)
    .allowMultithreading()
    .register()

  event.createStructure('my_mod:cracker')
    .pattern('BMB', 'MCM', 'BMB')
    .set('B', api.block('minecraft:steel_block'))
    .set('M', event.modifierSlot('my_mod:overclock', api.block('minecraft:steel_block')))
    .controller('C')
    .build()
})
```

开并行要在配方 JSON 里声明：

```js
ServerEvents.recipes(event => {
  event.custom({
    type: 'mmcr:machine_recipe',
    recipe_pool: 'my_mod:cracker',
    tick_time: 120,
    parallelized: true,
    max_threads: 8,
    requirements: [
      { type: 'minecraft:fluid', io: 'input', fluid: { fluid: 'my_mod:heavy_residue' }, amount: 500 },
      { type: 'minecraft:item', io: 'output', stack: { id: 'my_mod:coke', count: 2 } }
    ]
  }).id('my_mod:crack_residue')
})
```

---

## 组合出更复杂的机器

官方那些更复杂的示例（反应堆、太空电梯）本质都是上面五种的叠加：

| 想要的效果 | 用哪些 |
|---|---|
| 多档位 | 等级类型 + 多个等级 + 结构里的 `levelSlot` |
| 可插模块 | `allowModifiers()` + `registerModifierItem` + 结构里的 `modifierSlot` |
| 大量产出 | `allowParallelism()` + `maxParallelAmount` + 配方 `parallelized: true` |
| 特殊行为 | `tickBehavior` / `recipeBehavior`，见 [行为回调](Behaviors) |
| 多方块面接口 | `api.anyOf(...)` 里堆 `itemInput()` / `fluidOutput()` / `energyInput()` |
| 端口数量限制 | `portRequirements({...})` / `portTierRequirements(...)` |

## 下一步

- [行为回调](Behaviors) —— 让机器「活」起来
- [从 KubeJS 迁移](KubeJS-Migration) —— 逐条对照
