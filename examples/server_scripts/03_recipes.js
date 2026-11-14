// nekojs/server_scripts/03_recipes.js
//
// 配方只有一条可靠的路：schema 形式
//   event.recipes.mmcr.machine_recipe(pool, ticks, requirements).id('...')
//
// 为什么不用 event.custom：
//   RecipeEventJS.custom 同时有 custom(RecipeJsonValue) 和 custom(JsonObject) 两个重载，
//   JS 对象字面量传进去会报 Multiple applicable overloads；传 Java Map 则两个都不匹配。
//
// requirement 用 MMCR.getAPI() 的 helper 拼（返回 Java Map）。直接写 { stack: { id: ... } }
// 这种字面量也行，但要求 bridge 0.1.4+：早期版本 machine_recipe 返回裸 Gson JsonObject，
// 链 .id() 会报 Unknown identifier: id，而且配方根本没注册。
//
// 本 mod 不提供程序化配方 builder：它需要真实的 Item 对象，而 MMCR 的配方窗口在
// 服务端脚本加载前就关了，startup 阶段物品注册表又还没就绪。数据 JSON 是唯一可靠的路。

const api = MMCR.getAPI()

ServerEvents.recipes(event => {
  event.recipes.mmcr.machine_recipe('my_pack:blast_furnace', 200, [
    api.itemInputRequirement('minecraft:iron_ingot', 2),
    api.fluidInputRequirement('minecraft:water', 500),
    api.energyRequirement('input', 40),
    api.itemOutputRequirement('minecraft:iron_nugget', 1, 1.0)
  ]).id('my_pack:iron_to_nugget')

  event.recipes.mmcr.machine_recipe('my_pack:blast_furnace', 60, [
    api.itemInputRequirement('minecraft:copper_ingot', 1),
    api.itemOutputRequirement('minecraft:iron_nugget', 3, 1.0)
  ]).id('my_pack:copper_to_nuggets')
})
