// nekojs/startup_scripts/01_blocks_and_machine.js
// Startup declarations are fixed at boot: editing this file requires a game restart.

// 1) Custom casing blocks (NekoJS registry API, equivalent to KubeJS StartupEvents.registry).
RegistryEvents.register(event => {
  event.block('my_pack:machine_casing', block => {
    // put the texture at nekojs/assets/my_pack/textures/block/machine_casing.png
  })
})

// 2) Machine definition, level type and modifiers.
MMCREvents.startup(event => {
  const api = event.getAPI()

  event.createMachine('my_pack:blast_furnace')
    .recipePool('my_pack:blast_furnace')
    .displayNameKey('machine.my_pack.blast_furnace')
    .appearance('my_pack:machine_casing')
    .allowModifiers()
    .factoryThreads(2)
    .register()

  event.createLevelType('my_pack:coil')
    .displayNameKey('level.my_pack.coil')
    .register()

  event.createLevel('my_pack:coil_cupronickel')
    .type('my_pack:coil')
    .state('minecraft:iron_block')
    .priority(1)
    .register()

  event.registerModifier('my_pack:speed', api.modifierDefinition([
    api.modifier('duration', 'input', 0.75, 'multiply', false)
  ]))
  event.registerModifierItem('minecraft:diamond', 'my_pack:speed')

  // JEI workstations (optional; ignored when JEI is absent)
  event.addRecipePoolWorkstation('my_pack:blast_furnace', 'minecraft:blast_furnace')
  event.addMachineWorkstation('my_pack:blast_furnace', 'minecraft:smelting')
})
