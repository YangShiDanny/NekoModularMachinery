# nekojs/server_scripts/03_recipes.py
"""太空电梯 / 太空重组机的配方。

只有一条可靠写法：schema 形式

    event.recipes.mmcr.machine_recipe(pool, tick_time, [requirements]).id('...')

不能写 `event.custom({...})`：那个方法同时有 `custom(RecipeJsonValue)` 和
`custom(JsonObject)` 两个重载，脚本层传对象字面量会报 Multiple applicable overloads。

requirement 用 `MMCR.getAPI()` 的 helper 拼，它们返回 Java Map，直接对应 MMCR 的 JSON：

    api.itemInputRequirement(item, count)
    api.itemOutputRequirement(item, count, chance)
    api.fluidInputRequirement(fluid, amount)      # amount 单位 mB
    api.fluidOutputRequirement(fluid, amount, chance)
    api.energyRequirement('input' / 'output', fe_per_tick)

改完执行 `/nekojs reload`，服务端脚本不需要重启游戏。
"""

api = MMCR.getAPI()


def add_recipes(event):
    # 主机自己的池：1 苹果 -> 3 金苹果
    event.recipes.mmcr.machine_recipe('my_pack:space_elevator', 1000, [
        api.itemInputRequirement('minecraft:apple', 1),
        api.itemOutputRequirement('minecraft:golden_apple', 3, 1.0),
        api.energyRequirement('input', 100)
    ]).id('my_pack:space_elevator_apple')

    # 模块自己的池：4 紫水晶碎片 -> 1 钻石
    event.recipes.mmcr.machine_recipe('my_pack:space_reassembler', 200, [
        api.itemInputRequirement('minecraft:amethyst_shard', 4),
        api.itemOutputRequirement('minecraft:diamond', 1, 1.0),
        api.energyRequirement('input', 40)
    ]).id('my_pack:reassembler_diamond')

    # 同一个模块的第二个池（space_miner）：1 石头 -> 1 铁矿石
    event.recipes.mmcr.machine_recipe('my_pack:space_miner', 100, [
        api.itemInputRequirement('minecraft:stone', 1),
        api.itemOutputRequirement('minecraft:iron_ore', 1, 1.0),
        api.energyRequirement('input', 20)
    ]).id('my_pack:miner_iron_ore')

    print('[example] 3 条机器配方已提交')


ServerEvents.recipes(add_recipes)
