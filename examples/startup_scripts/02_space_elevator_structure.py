# nekojs/startup_scripts/02_space_elevator_structure.py
"""太空电梯（HOST）与太空重组机（MODULE）的结构。

结构只能写在 startup 脚本里：MMCR 在 FMLCommonSetupEvent 里冻结结构注册表，
那之后没有任何窗口能再塞结构进去（KubeJS 版能热改是因为 MMCR 单独给它写了
内容重载事务，对 NekoJS 没有这种照顾）。

尺寸约定
--------
pattern() 每调用一次 = 沿 Z 追加一层；同一层里每个字符串 = 沿 X 的一行，
**第一个字符串是 y=0（最底层）**；空格 = 该位置不检查（放什么都行，也可以留空）。

    电梯     17 (x) x 12 (y) x 17 (z)
    重组机    3 (x) x  4 (y) x  3 (z)
"""

# ---- 太空电梯：17 层，每层 12 行、行宽 17 ----
# 符号：X = 平滑石英  A = 紫水晶块  B = 模块耦合器
#       D = 平滑石英 / 物品输入 / 物品输出 / 能量输入 / 工厂控制器
#       E = 控制器（整份结构里只出现一次）


ELEVATOR = [
    [
        '        X        ',
        '                 ',
        '                 ',
        '                 ',
        '                 ',
        '                 ',
        '                 ',
        '                 ',
        '                 ',
        '                 ',
        '                 ',
        '                 ',
    ],
    [
        '       XXX       ',
        '                 ',
        '                 ',
        '                 ',
        '                 ',
        '                 ',
        '                 ',
        '                 ',
        '                 ',
        '                 ',
        '                 ',
        '                 ',
    ],
    [
        '      XXXXX      ',
        '                 ',
        '                 ',
        '                 ',
        '                 ',
        '                 ',
        '                 ',
        '                 ',
        '                 ',
        '                 ',
        '                 ',
        '                 ',
    ],
    [
        '     XXAAAXX     ',
        '                 ',
        '                 ',
        '                 ',
        '                 ',
        '                 ',
        '                 ',
        '                 ',
        '                 ',
        '                 ',
        '                 ',
        '                 ',
    ],
    [
        '    XXXAAAXXX    ',
        '        B        ',
        '                 ',
        '                 ',
        '                 ',
        '                 ',
        '                 ',
        '                 ',
        '                 ',
        '                 ',
        '                 ',
        '                 ',
    ],
    [
        '   XXXXAAAXXXX   ',
        '                 ',
        '                 ',
        '                 ',
        '                 ',
        '        X        ',
        '                 ',
        '                 ',
        '                 ',
        '                 ',
        '                 ',
        '                 ',
    ],
    [
        '  XXXXXXXXXXXXX  ',
        '                 ',
        '                 ',
        '                 ',
        '        X        ',
        '       XXX       ',
        '                 ',
        '                 ',
        '                 ',
        '                 ',
        '                 ',
        '                 ',
    ],
    [
        ' XXAAAXXXXXAAAXX ',
        '       XXX       ',
        '       DDD       ',
        '       XXX       ',
        '       XXX       ',
        '      XXXXX      ',
        '       XXX       ',
        '       X X       ',
        '                 ',
        '                 ',
        '                 ',
        '                 ',
    ],
    [
        'XXXAAAXXXXXAAAXXX',
        '    B  X X  B    ',
        '       D D       ',
        '       X X       ',
        '      XX XX      ',
        '     XXX XXX     ',
        '       XXX       ',
        '        X        ',
        '        X        ',
        '        X        ',
        '        X        ',
        '        X        ',
    ],
    [
        ' XXAAAXXXXXAAAXX ',
        '       XXX       ',
        '       DED       ',
        '       XXX       ',
        '       XXX       ',
        '      XXXXX      ',
        '       XXX       ',
        '       X X       ',
        '                 ',
        '                 ',
        '                 ',
        '                 ',
    ],
    [
        '  XXXXXXXXXXXXX  ',
        '                 ',
        '                 ',
        '                 ',
        '        X        ',
        '       XXX       ',
        '                 ',
        '                 ',
        '                 ',
        '                 ',
        '                 ',
        '                 ',
    ],
    [
        '   XXXXXXXXXXX   ',
        '                 ',
        '                 ',
        '                 ',
        '                 ',
        '        X        ',
        '                 ',
        '                 ',
        '                 ',
        '                 ',
        '                 ',
        '                 ',
    ],
    [
        '    XXXXXXXXX    ',
        '                 ',
        '                 ',
        '                 ',
        '                 ',
        '                 ',
        '                 ',
        '                 ',
        '                 ',
        '                 ',
        '                 ',
        '                 ',
    ],
    [
        '     XXXXXXX     ',
        '                 ',
        '                 ',
        '                 ',
        '                 ',
        '                 ',
        '                 ',
        '                 ',
        '                 ',
        '                 ',
        '                 ',
        '                 ',
    ],
    [
        '      XXXXX      ',
        '                 ',
        '                 ',
        '                 ',
        '                 ',
        '                 ',
        '                 ',
        '                 ',
        '                 ',
        '                 ',
        '                 ',
        '                 ',
    ],
    [
        '       XXX       ',
        '                 ',
        '                 ',
        '                 ',
        '                 ',
        '                 ',
        '                 ',
        '                 ',
        '                 ',
        '                 ',
        '                 ',
        '                 ',
    ],
    [
        '        X        ',
        '                 ',
        '                 ',
        '                 ',
        '                 ',
        '                 ',
        '                 ',
        '                 ',
        '                 ',
        '                 ',
        '                 ',
        '                 ',
    ],
]

# ---- 太空重组机：3 层，每层 4 行、行宽 3 ----
# 符号：X = 石英柱  A = 紫水晶块  B = 平滑石英 / 物品输入 / 物品输出 / 能量输入
#       D = 玻璃  E = 模块耦合器  F = 控制器
REASSEMBLER = [
    [
        'AAA',
        'XBX',
        'XBX',
        'XDX',
    ],
    [
        'AAA',
        'BEB',
        'B B',
        'DDD',
    ],
    [
        'AAA',
        'XFX',
        'XBX',
        'XDX',
    ],
]


def declare_structures(event):
    api = event.getAPI()

    # ---- 太空电梯 ----
    lift = event.createStructure('my_pack:space_elevator')
    for lift_slice in ELEVATOR:
        lift.pattern(*lift_slice)
    lift.set('X', api.block('minecraft:smooth_quartz'))
    lift.set('A', api.block('minecraft:amethyst_block'))
    # api.coupler() 只声明「这一格必须是模块耦合器」。
    # 主机接受哪个模块由机器声明上的 .host(...) 决定，耦合器不替代那个关系。
    lift.set('B', api.coupler())
    lift.set('D', api.anyOf(
        api.block('minecraft:smooth_quartz'),
        api.itemInput(),
        api.itemOutput(),
        api.energyInput(),
        api.factoryController()
    ))
    lift.controller('E')
    lift.build()

    # ---- 太空重组机 ----
    # 循环变量名不要和上面那个重复：NekoJS 的 ESM 链接器把 `var` 归到最近的函数作用域，
    # 同名两次会被判成 Duplicate local ESM binding（JS 本身允许，预览版过严）。
    module_machine = event.createStructure('my_pack:space_reassembler')
    for module_slice in REASSEMBLER:
        module_machine.pattern(*module_slice)
    module_machine.set('X', api.block('minecraft:quartz_pillar'))
    module_machine.set('A', api.block('minecraft:amethyst_block'))
    module_machine.set('B', api.anyOf(
        api.block('minecraft:smooth_quartz'),
        api.itemInput(),
        api.itemOutput(),
        api.energyInput()
    ))
    module_machine.set('D', api.block('minecraft:glass'))
    module_machine.set('E', api.coupler())
    module_machine.controller('F')
    module_machine.build()

    print('[example] 太空电梯 + 太空重组机结构已声明')


MMCREvents.startup(declare_structures)
