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

MMCREvents.startup(event => {
    const api = event.getAPI()

    // ---- 例子 1：3×3 竖直墙面（1 个 z 层，3 个 y 层）----
    event.createStructure('my_pack:simple_furnace')
        .pattern(
            'SSS',
            'SMS',
            'SSS'
        )
        .set('S', api.block('minecraft:stone_bricks'))
        .set('M', api.block('my_pack:machine_casing'))
        .controller('M')
        .stateSensitive()
        .build()

    // ---- 例子 2：5×5×3 立方体，带端口数量与等级约束 ----
    // 三次 pattern() = 3 个 z 层，每层 5 行（y）× 5 字符（x）
    // 'C' 那一格也会被替换成控制器方块，绑定只写一次。
    // portRequirements 是"校验"：接口摆不到位就不成型，它不会自动帮你生成端口。
    // portTierRequirements 的等级名大小写不敏感，只写族名 = 任意等级。
    //
    // 注意：本文件的链式调用中间不放 // 注释。NekoJS 的静态预检解析器（ValParser）
    // 在链的每一级之间只跳过空白、不跳过注释，注释会把链断成两句，日志里报
    // "Unknown identifier 'pattern'"。注释一律写在整条链的外面。
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
        .set('C', api.block('minecraft:bricks'))
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

    // ---- 例子 3：带等级槽的结构 ----
    // 'L' 那一格放什么由机器等级决定，见 createLevel 的 state()
    event.createLevelType('my_pack:heat').displayNameKey('level.my_pack.heat').register()

    event.createLevel('my_pack:heat_lv1')
        .type('my_pack:heat')
        .state('minecraft:furnace[lit=true]')
        .priority(0)
        .register()

    // 三片分别对应 z=0 / z=1 / z=2：中间那片 'L' 是等级槽，'M' 是控制器位置。
    event.createStructure('my_pack:heated_furnace')
        .pattern('SSS')
        .pattern('SLS')
        .pattern('SMS')
        .set('S', api.block('minecraft:stone_bricks'))
        .set('L', event.levelSlot('my_pack:heat'))
        .set('M', api.block('my_pack:machine_casing'))
        .controller('M')
        .build()
})
