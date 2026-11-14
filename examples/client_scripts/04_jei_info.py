# nekojs/client_scripts/04_jei_info.py
"""JEI 里的附加说明。没装 JEI 时这些注册会被静默忽略。

- addRecipePoolInfo(配方池 id, 翻译键, ...参数)：显示在该池所有配方页上
- addRecipeInfo(配方 id, 翻译键, ...参数)：只显示在那一条配方上

翻译键写在 assets/my_pack/lang/*.json 里。
"""


def add_info(event):
    event.addRecipePoolInfo('my_pack:space_elevator', 'jei.my_pack.space_elevator.info')
    event.addRecipeInfo('my_pack:space_elevator_apple', 'jei.my_pack.space_elevator_apple.info')
    event.addRecipePoolInfo('my_pack:space_miner', 'jei.my_pack.space_miner.info')


MMCREvents.client(add_info)
