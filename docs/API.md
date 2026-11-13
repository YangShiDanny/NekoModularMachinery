# `mmcr_nekojs` API 速查

> 纯签名清单，一页扫完。**说明、示例、注意事项在 [wiki](../wiki/Home.md)**，这里只保证「查得到方法名」。
>
> 面向 MMCR **0.1.7+** / NekoJS **1.1.0+**。

## 目录约定

| 目录 | 放什么 | 生效方式 |
|---|---|---|
| `nekojs/startup_scripts/` | 机器、结构、等级、修饰符、JEI workstation、控制屏文本 | 重启 |
| `nekojs/server_scripts/` | `ServerEvents.recipes` 配方 | `/nekojs reload` |
| `nekojs/client_scripts/` | `MMCREvents.client` JEI 说明 | `F3+T` |

## 全局绑定

```
MMCR.getAPI()    → MMCRApiJS
MMCR.getValues() → MMCRValues
```

`MMCRValues` 字段：`INT_MAX` `INT_MIN` `LONG_MAX` `LONG_MIN`

## 事件

```
MMCREvents.startup(cb)   // cb(StartupEventJS)   —— 唯一能注册东西的事件
MMCREvents.server(cb)    // cb(ServerEventJS)    —— 只有 getAPI()
MMCREvents.client(cb)    // cb(ClientInfoEventJS)
```

## `StartupEventJS`

```
getAPI() → MMCRApiJS

createMachine(id)    → MachineBuilderJS       .register()
createStructure(id)  → StructureBuilderJS     .build()
createLevelType(id)  → LevelTypeBuilderJS     .register()
createLevel(id)      → MachineLevelBuilderJS  .register()

levelSlot(typeId)                    → LevelSlotRef
modifierSlot(modifierId)             → ModifierUseRef
modifierSlot(modifierId, replacement)→ ModifierUseRef

registerModifier(id, ModifierBundle)
registerModifier(id, Object[] specs)
registerModifierItem(itemId, modifierId)

addRecipePoolWorkstation(poolId, itemId)
addMachineWorkstation(machineId, recipeTypeId)
addMachineWorkStation(machineId, recipeTypeId)   // 别名

registerControllerScreenText(machineId, Consumer<Object>)
```

## `ServerEventJS`

```
getAPI() → MMCRApiJS
```

## `ClientInfoEventJS`

```
addRecipePoolInfo(poolId, translationKey, ...args)
addRecipeInfo(recipeId, translationKey, ...args)
```

## `MMCRApiJS`

### ID

```
id(value) → ID
```

### 方块条件

```
block(blockId)        state(stateId)       tag(tagId)
air()                 any()                coupler()
anyOf(...conditions)
```

### 接口族条件

```
itemInput()      itemOutput()
fluidInput()     fluidOutput()
energyInput()    energyOutput()
chemicalInput()  chemicalOutput()
radioactiveChemicalInput()   radioactiveChemicalOutput()
heatInput()      heatOutput()

itemPorts()  fluidPorts()  energyPorts()  chemicalPorts()  heatPorts()  ports()

upgradeBus()   parallelControllers()   factoryController()
smartInterface()   dataStorage()   networkInterface()
```

### 修饰符

```
modifier(target, scope, value, operation, chance) → NumericModifierSpec
modifier('parallelized', 'recipe', bool)          → ParallelizationModifierSpec
modifierDefinition(...specs)                      → ModifierBundle
```

`scope`: `input` `output` `machine` `recipe`
`operation`: `add` `multiply` `subtract` `divide`

### 槽位

```
levelSlot(typeId)                    → LevelSlotRef
modifierUse(modifierId, replacement) → ModifierUseRef
```

### requirement 构造（拼配方 JSON 用）

```
itemInputRequirement(itemId, count)
itemOutputRequirement(itemId, count, chance)
fluidInputRequirement(fluidId, amount)
fluidOutputRequirement(fluidId, amount, chance)
energyRequirement(io, fePerTick)
machineRecipe(pool, tickTime, ...requirements)
```

### 数字格式化

```
readableNumber(v)        // 1.23M
readableNumberFull(v)    // 1.20M
readableNumberExact(v)   // 1,234,567
```

### 常量门面

```
screenScope()        → { CONTROLLER, OPERATION }
recipeIO()           → { INPUT, OUTPUT }
outputPolicy()       → { REQUIRE_FULL, ALLOW_PARTIAL }
modifierOperation()  → { ADD, MULTIPLY, SUBTRACT, DIVIDE }
modifierScope()      → { INPUT, OUTPUT, MACHINE, RECIPE }
```

## `MachineBuilderJS`

```
recipePool(...ids)        recipePool(pool)
displayNameKey(key)
appearance(blockId)

controllerTextures(front, side, top, bottom)
controllerTextures(front, otherFaces)
controllerFrontTexture(id)     controllerSideTexture(id)
controllerTopTexture(id)       controllerBottomTexture(id)
controllerBaseTexture(id)      formedPortBaseTexture(id)
controllerIdleOverlayTexture(id)   controllerActiveOverlayTexture(id)

runningSound(id)          finishSound(id)

allowVerticalFacing()     requireVerticalFacing()    fullyRotationallySymmetric()

allowModifiers()          allowMultithreading()      allowParallelism()
maxParallelAmount(n)      maxParallelism(n)
factoryThreads(n)

networkInterface(maxCount, maxConnections)

tickBehavior(Consumer<TickHooks>)
recipeBehavior(Consumer<RecipeHooks>)

register() → this
```

## `StructureBuilderJS`

```
pattern(...rows)             // 一次 = 一个 z 层；第一个字符串是最底层
pattern(List<String> rows)
set(symbol, condition)       // condition: BlockCondition | id 字符串 | slot ref
controller(symbol)
stateSensitive()             stateInsensitive()

portRequirements(Map<String, Object>)
portTierRequirements('family>=TIER' | ['family','TIER'], ...)

build()
```

## `LevelTypeBuilderJS`

```
displayNameKey(key)
register()
```

## `MachineLevelBuilderJS`

```
type(typeId)              // 必填
state(stateId)
priority(n)
modifier(ModifierBundle)
register()
```

## `TickHooks` / `RecipeHooks`

```
TickHooks.serverTick(Consumer<TickContext>)

RecipeHooks.idleStart(Consumer<MachineContext>)
RecipeHooks.idleEnd(Consumer<MachineContext>)
RecipeHooks.beforeStart(Consumer<RecipeStartContext>)
RecipeHooks.recipeTick(Consumer<RecipeTickContext>)
RecipeHooks.beforeFinish(Consumer<RecipeFinishContext>)
RecipeHooks.preServerTick(Consumer<MachineContext>)
RecipeHooks.postServerTick(Consumer<MachineContext>)
```

### Context 方法

```
MachineContext: controller() level() controllerPos() machineId() gameTime()
                isDue(period) screenText() dataStorage() ioView()
                upgradeItems() jadeText()
                countStructureBlocks(block) countStructureBlocks(blockId)

TickContext (extends MachineContext):
                factoryThreadCount() parallelism()
                smartInterfaceValue(name) smartInterfaceValues() ioPlan()

RecipeStartContext: recipe() machineContext() recipeId()
                requestedParallelism() effectiveParallelism()
                duration() setDuration(ticks)
                requirements() setRequirements(list)
                outputs() setOutputs(list)
                replaceExactItemInputCount(item, expected, replacement)
                cancel() cancelled()

RecipeTickContext: machineContext() recipe() currentTick() totalTick()
                parallelism() requirements() outputs() ioSnapshot()

RecipeFinishContext: recipe() machineContext() recipeId()
                requestedParallelism() effectiveParallelism()
                outputs() setOutputs(list) discardOutputs() outputsDiscarded()
                cancel() cancelled()
```

## 配方

```js
// 推荐：schema 位置参数
event.recipes.mmcr.machine_recipe(pool, tickTime, requirements)
event.recipes.mmcr.machine_recipe(pool, requirements)        // tick_time 默认 20
event.recipes.mmcr.machine_recipe(jsonObject)

// 或原始 JSON
event.custom({ type: 'mmcr:machine_recipe', recipe_pool, tick_time, requirements })
```

### requirement type

```
minecraft:item            io + item|tag + count       / io:output + stack{id,count} [+ chance]
minecraft:fluid           io + fluid{fluid} + amount  / io:output + stack{id,amount}
neoforge:energy           io + fe_per_tick
mmcr:level                type_id + level_id
mmcr:stage                stage
mmcr:smart_interface      name + min
botania:mana / ars_nouveau:source / pneumaticcraft:air / mekanism:*   ← 仅 1.21.1
```

### 顶层字段

```
recipe_pool  tick_time  requirements  priority  max_threads
parallelized  allow_partial_outputs  cancelIfPerTickFails  required_host_ids
```

## 与 KubeJS 版的差异

| KubeJS | NekoJS |
|---|---|
| `kubejs/*_scripts/` | `nekojs/*_scripts/` |
| 结构放 `server_scripts` | 结构必须放 `startup_scripts` |
| `api.anyOfItemInput()` | `api.itemInput()` |
| `MachineRecipeBuilderJS`（程序化配方） | 无，用数据 JSON |

详细说明见 [wiki/从 KubeJS 迁移](../wiki/KubeJS-Migration.md)。
