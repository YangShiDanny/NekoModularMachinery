// nekojs/server_scripts/03_recipes.js
//
// 配方有两条路，都支持 /nekojs reload，不用重启：
//   A. event.recipes.mmcr.machine_recipe(...) —— schema 已由本 mod 注册，开箱可用
//   B. event.custom({ type:'mmcr:machine_recipe', ... }) —— 原始 JSON，一定可用
//
// 注意：MMCREvents.startup 里还有程序化的 event.createRecipe(...)，但 MMCR 只在启动期
// 收集一次静态配方，所以那条路必须写在 startup_scripts，不能放这里。

const api = MMCR.getAPI()

ServerEvents.recipes(event => {
  // (A) schema 形式：字段和构造由本 mod 注册的 mmcr:machine_recipe schema 校验
  event.recipes.mmcr.machine_recipe('my_pack:blast_furnace', 200, [
    api.itemInputRequirement('minecraft:iron_ingot', 2),
    api.fluidInputRequirement('minecraft:water', 500),
    api.energyRequirement('input', 40),
    api.itemOutputRequirement('minecraft:iron_nugget', 1, 1.0)
  ]).id('my_pack:iron_to_nugget')

  // (B) 原始 JSON：不依赖 schema，任何情况下都能用
  event.custom({
    type: 'mmcr:machine_recipe',
    recipe_pool: 'my_pack:blast_furnace',
    tick_time: 100,
    requirements: [
      { type: 'minecraft:item', io: 'input', item: 'minecraft:gold_ingot', count: 1 },
      { type: 'neoforge:energy', io: 'output', fe_per_tick: 20 }
    ]
  }).id('my_pack:gold_to_energy')

  // (C) helper 拼装整份 JSON
  event.custom(api.machineRecipe('my_pack:blast_furnace', 60,
    api.itemInputRequirement('minecraft:copper_ingot', 1),
    api.itemOutputRequirement('minecraft:iron_nugget', 3, 1.0)
  )).id('my_pack:copper_to_nuggets')
})
