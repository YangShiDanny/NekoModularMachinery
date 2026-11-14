// nekojs/startup_scripts/01_blocks_and_machine.js
// Startup declarations are fixed at boot: editing this file requires a game restart.
//
// 这份和 02_structure.js 是配套的：这里声明机器，02 里给同一批 id 声明结构。
// 机器和结构必须成对——MMCR 会拒绝没有同名机器的结构（日志里是一行 skipped declaration）。
//
// 全文件唯一的硬规矩：链式调用中间不要插 // 注释（NekoJS 的静态预检会把链断成两句）。

// 1) 自定义外壳方块（NekoJS 注册 API，等价于 KubeJS 的 StartupEvents.registry）
//    贴图放 nekojs/assets/my_pack/textures/block/machine_casing.png
//    renderType 是必要的：声明之后 NekoJS 才会给它生成默认 model / blockstate / items 定义。
RegistryEvents.register(event => {
  event.block('my_pack:machine_casing', block => {
    block.hardness = 4.0
    block.resistance = 6.0
    block.requiresTool = true
    block.sound = 'metal'
    block.renderType = 'solid'
  })
})

// 2) 机器定义、等级类型、修饰符
MMCREvents.startup(event => {
  const api = event.getAPI()

  // 主角机器：完整配置（外观块、并行、修饰符、工厂线程）
  event.createMachine('my_pack:blast_furnace')
    .recipePool('my_pack:blast_furnace')
    .displayNameKey('machine.my_pack.blast_furnace')
    .appearance('my_pack:machine_casing')
    .allowModifiers()
    .factoryThreads(2)
    .register()

  // 02 里另外三台结构对应的机器，只写最小集
  event.createMachine('my_pack:simple_furnace')
    .recipePool('my_pack:simple_furnace')
    .displayNameKey('machine.my_pack.simple_furnace')
    .register()

  event.createMachine('my_pack:big_furnace')
    .recipePool('my_pack:big_furnace')
    .displayNameKey('machine.my_pack.big_furnace')
    .allowParallelism()
    .maxParallelAmount(8)
    .register()

  event.createMachine('my_pack:heated_furnace')
    .recipePool('my_pack:heated_furnace')
    .displayNameKey('machine.my_pack.heated_furnace')
    .register()

  // 等级 = 一个方块状态变体；结构里的 levelSlot 按机器当前等级取对应方块
  event.createLevelType('my_pack:coil')
    .displayNameKey('level.my_pack.coil')
    .register()

  event.createLevel('my_pack:coil_cupronickel')
    .type('my_pack:coil')
    .state('minecraft:iron_block')
    .priority(1)
    .register()

  // 修饰符：把配方时长压到 75%，插一个钻石触发
  event.registerModifier('my_pack:speed', api.modifierDefinition([
    api.modifier('duration', 'input', 0.75, 'multiply', false)
  ]))
  event.registerModifierItem('minecraft:diamond', 'my_pack:speed')

  // JEI 工作站（没装 JEI 时这些声明会被忽略）
  event.addRecipePoolWorkstation('my_pack:blast_furnace', 'minecraft:blast_furnace')
  event.addMachineWorkstation('my_pack:blast_furnace', 'minecraft:smelting')
})
