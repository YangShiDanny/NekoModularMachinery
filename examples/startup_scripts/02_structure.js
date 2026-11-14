// nekojs/startup_scripts/02_structure.js
//
// 结构必须声明在 STARTUP 脚本里，不能放 server_scripts。
// 原因：MMCR 在 FMLCommonSetupEvent 收集结构并冻结注册表；NekoJS 的 SERVER 脚本要到
// 服务器启动时才加载，那时窗口已经关了。NekoJS 的 STARTUP 脚本在 NekoJSMod 构造函数
// 里就跑完（早于 FMLConstructModEvent），所以机器/结构/等级/修饰符全部写在这里。
//
// pattern() 的坐标语义（见 MMCR MachineDefinitionConverter.toBlockArray）：
//   - 每次 pattern() = 一个 z 层，按调用顺序从 z 小到大
//   - 同一次调用里，第 1 个字符串是 y 最小（最底层），往后 y 递增
//   - 每个字符串里，第 1 个字符是 x 最小
//   - 空格 ' ' 表示"这一格不检查"，要强制空气请用 api.air() 显式声明
//
// 两条硬规矩：
//   1. 链式调用中间不放 // 注释（NekoJS 静态预检解析器只跳空白不跳注释，会把链断成两句）
//   2. 控制器符号不要再 set() 绑方块——MMCR 自己往那一格放控制器方块；
//      多绑一次会让结构永远成型失败（0.1.5 起这种绑定会被忽略并打一行 warn）
//
// 这里的四个结构 id 和 01_blocks_and_machine.js 里的机器一一对应。

MMCREvents.startup(event => {
    const api = event.getAPI()

    // ---- 例子 1：3×3 竖直墙面，最小可成型结构 ----
    // 只有外壳 + 控制器，没有端口：能成型，但接不了东西。
    event.createStructure('my_pack:simple_furnace')
        .pattern(
            'SSS',
            'SCS',
            'SSS'
        )
        .set('S', api.block('minecraft:stone_bricks'))
        .controller('C')
        .stateSensitive()
        .build()

    // ---- 例子 2：主角机器 5×3×3，前面板带四类接口 ----
    // z=0 前面板：E 能量输入、F 流体输入、I 物品输入、C 控制器、O 物品输出
    // z=1 中空（' ' = 这一格不检查，等于留空）
    // z=2 背板
    event.createStructure('my_pack:blast_furnace')
        .pattern(
            'BEFBB',
            'BICOB',
            'BBBBB'
        )
        .pattern(
            'BBBBB',
            'B   B',
            'BBBBB'
        )
        .pattern(
            'BBBBB',
            'BBBBB',
            'BBBBB'
        )
        .set('B', api.block('my_pack:machine_casing'))
        .set('E', api.energyInput())
        .set('F', api.fluidInput())
        .set('I', api.itemInput())
        .set('O', api.itemOutput())
        .controller('C')
        .build()

    // ---- 例子 3：5×5×3 立方体，带端口数量与等级约束 ----
    // 三次 pattern() = 3 个 z 层，每层 5 行（y）× 5 字符（x）
    // portRequirements 是"校验"：接口摆不到位就不成型，它不会自动帮你生成端口。
    // portTierRequirements 的等级名大小写不敏感，只写族名 = 任意等级。
    event.createStructure('my_pack:big_furnace')
        .pattern(
            'BBBBB',
            'BTTTB',
            'BTTTB',
            'BTTTB',
            'BBBBB'
        )
        .pattern(
            'BBBBB',
            'BTTTB',
            'BTCTB',
            'BTTTB',
            'BBBBB'
        )
        .pattern(
            'BBBBB',
            'BTTTB',
            'BTTTB',
            'BTTTB',
            'BBBBB'
        )
        .set('B', api.block('minecraft:bricks'))
        .set('T', api.anyOf('minecraft:blast_furnace', 'minecraft:furnace'))
        .controller('C')
        .portRequirements({
            item_input_bus: 1,          // 至少 1 个
            item_output_bus: [1, 4],    // 1~4 个
            energy_input_hatch: 1
        })
        .portTierRequirements(
            'energy_input_hatch>=NORMAL',
            ['item_input_bus', 'SMALL']
        )
        .build()

    // ---- 例子 4：带等级槽的结构 ----
    // 'L' 那几格放什么由机器当前等级决定，见 createLevel 的 state()
    event.createLevelType('my_pack:heat').displayNameKey('level.my_pack.heat').register()

    event.createLevel('my_pack:heat_lv1')
        .type('my_pack:heat')
        .state('minecraft:furnace[lit=true]')
        .priority(0)
        .register()

    // 3×3 单层：四个 L 是等级槽，C 是控制器
    event.createStructure('my_pack:heated_furnace')
        .pattern(
            'SLS',
            'SCS',
            'SLS'
        )
        .set('S', api.block('minecraft:stone_bricks'))
        .set('L', event.levelSlot('my_pack:heat'))
        .controller('C')
        .build()
})
