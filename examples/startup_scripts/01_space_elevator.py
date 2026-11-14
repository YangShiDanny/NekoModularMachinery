# nekojs/startup_scripts/01_space_elevator.py
"""太空电梯（HOST）+ 太空重组机（MODULE）：机器声明。

这是 MMCR 官方 KubeJS 示例 `A_Module_Machine.js` 的 NekoJS + Python 版。
官方示例把结构放在 server_scripts 里能生效，是因为 MMCR 给 KubeJS 单独写了
内容重载事务；本附属没有这套事务，所以机器与结构**都必须在 startup 脚本**里声明。

三条硬约束（都是 MMCR 校验的，写错不会崩游戏，但这条声明会被拒绝）：

1. HOST 机器必须至少 `host(...)` 一个模块 ID；非 HOST 不能声明接受模块。
2. 模块机器要 `module()`，否则主机连上它也不会派发配方。
3. 结构里的耦合器（`api.coupler()`）只提供物理安装点，不代表接受关系。

改完要**重启游戏**：startup 脚本 `/nekojs reload` 不会重新执行。
"""

HOST = 'my_pack:space_elevator'
MODULE = 'my_pack:space_reassembler'


def declare_machines(event):
    # ---- 太空电梯：主机 ----
    lift = event.createMachine(HOST)
    lift.recipePool(HOST)
    lift.displayNameKey('machine.my_pack.space_elevator')
    lift.appearance('minecraft:smooth_quartz')
    # 平滑石英没有标准的控制器底图，手动指一个（值是 textures/ 下的路径，不带命名空间也默认为 minecraft）
    lift.controllerBaseTexture('minecraft:block/quartz_block_bottom')
    lift.formedPortBaseTexture('minecraft:block/quartz_block_bottom')
    lift.host(MODULE)
    lift.register()

    # ---- 太空重组机：模块 ----
    # 一台模块可以挂多个配方池；配方用 recipe_pool 决定自己进哪个池。
    module = event.createMachine(MODULE)
    module.recipePool(
        MODULE,
        'my_pack:space_miner',
        'my_pack:space_1',
        'my_pack:space_2',
        'my_pack:space_3'
    )
    module.displayNameKey('machine.my_pack.space_reassembler')
    module.appearance('minecraft:quartz_pillar')
    module.allowMultithreading()
    module.module()
    module.register()

    print('[example] 太空电梯 + 太空重组机已声明')


MMCREvents.startup(declare_machines)
