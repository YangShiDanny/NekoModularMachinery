// nekojs/client_scripts/04_jei_info.js
// Optional: JEI recipe page descriptions. Ignored when JEI is not installed.
MMCREvents.client(event => {
  event.addRecipePoolInfo('my_pack:blast_furnace', 'jei.my_pack.blast_furnace.info', 200)
  event.addRecipeInfo('my_pack:iron_to_nugget', 'jei.my_pack.iron_to_nugget.info')
})
