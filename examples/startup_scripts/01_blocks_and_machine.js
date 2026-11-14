// nekojs/startup_scripts/01_blocks_and_machine.js
// Startup declarations are fixed at boot: editing this file requires a game restart.
//
// 这份和 02_structure.js 是配套的：这里声明机器，02 里给同一批 id 声明结构。
// 机器和结构必须成对——MMCR 会拒绝没有同名机器的结构（日志里是一行 skipped declaration）。
//
// 全文件唯一的硬规矩：链式调用中间不要插 // 注释（NekoJS 的静态预检会把链断成两句）。
//
// 外壳用的是原版铁块，不是自定义方块：NekoJS 的自定义方块注册 API 各版本不一样
// （preview3 是 RegistryEvents.block(event => event.create(id, b => { b.hardness(4) }))，
//  新版是 RegistryEvents.register(event => event.block(id, b => { b.hardness = 4 }))），
// 抄错版本会直接报 "has no member 'register'"。想换自定义外壳，照下面这段改，并确认
// 贴图在 nekojs/assets/my_pack/textures/block/machine_casing.png、lang 键已就位：
//
//   RegistryEvents.block(event => {
//     event.create('my_pack:machine_casing', casing => {
//       casing.hardness(4.0)
//       casing.resistance(6.0)
//       casing.requiresTool()
//       casing.sound('metal')
//       casing.renderType('solid')
//     })
//   })
//
// 然后把下面所有 'minecraft:iron_block' 换成 'my_pack:machine_casing'。

MMCREvents.startup(event => {
  const api = event.getAPI()

  // 主角机器：完整配置（外观块、修饰符、工厂线程）
  event.createMachine('my_pack:blast_furnace')
    .recipePool('my_pack:blast_furnace')
    .displayNameKey('machine.my_pack.blast_furnace')
    .appearance('minecraft:iron_block')
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

  // 修饰符：把配方时长压到 75%，插一个钻石触发
  event.registerModifier('my_pack:speed', api.modifierDefinition([
    api.modifier('duration', 'input', 0.75, 'multiply', false)
  ]))
  event.registerModifierItem('minecraft:diamond', 'my_pack:speed')

  // JEI 工作站（没装 JEI 时这些声明会被忽略）
  event.addRecipePoolWorkstation('my_pack:blast_furnace', 'minecraft:blast_furnace')
  event.addMachineWorkstation('my_pack:blast_furnace', 'minecraft:smelting')
})
