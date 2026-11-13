# 等级与修饰符

两套机制让同一台机器在不同配置下表现不同：

- **等级（Level）** —— 结构里某个位置的方块换一档，机器整体换档。适合做「线圈」「电路」这类可升级部件。
- **修饰符（Modifier）** —— 把物品插进升级总线，临时改某项数值。适合做「超频模块」这类可插拔的东西。

两者都必须在 `startup_scripts` 里注册。

## 第一部分：等级

一套等级需要三个东西：等级类型、等级实例、结构里的等级槽。

### 1. 等级类型 `LevelTypeBuilderJS`

> `cn.howxu.mmcr.nekojs.builder.LevelTypeBuilderJS`

等级类型是「一族等级」的名字，比如「线圈」。

| 方法 | 说明 |
|---|---|
| `displayNameKey(key)` | 本地化键 |
| `register()` | 提交 |

```js
MMCREvents.startup(event => {
  event.createLevelType('my_mod:coil')
    .displayNameKey('level_type.my_mod.coil')
    .register()
})
```

### 2. 等级实例 `MachineLevelBuilderJS`

> `cn.howxu.mmcr.nekojs.builder.MachineLevelBuilderJS`

一个具体档位，比如「铜线圈」。

| 方法 | 说明 |
|---|---|
| `type(typeId)` | **必填**，属于哪个等级类型 |
| `state(stateId)` | 哪个方块状态算这一档，支持 `[axis=y]` 这样的状态 |
| `priority(n)` | 同时满足多个等级时的优先级，大的优先 |
| `modifier(bundle)` | 这一档附带的修饰符 |
| `register()` | 提交 |

```js
event.createLevel('my_mod:coil_copper')
  .type('my_mod:coil')
  .state('minecraft:copper_block')
  .register()

event.createLevel('my_mod:coil_gold')
  .type('my_mod:coil')
  .state('minecraft:gold_block')
  .priority(10)
  .modifier(api.modifierDefinition(
    api.modifier('duration', 'recipe', 0.75, 'multiply', false)
  ))
  .register()
```

### 3. 结构里的等级槽

```js
event.createStructure('my_mod:smelter')
  .pattern('BLB', 'LCL', 'BLB')
  .set('B', api.block('minecraft:iron_block'))
  .set('L', event.levelSlot('my_mod:coil'))
  .controller('C')
  .build()
```

`event.levelSlot('my_mod:coil')` 表示「这个位置要放某个 `my_mod:coil` 类型的等级方块」。玩家放铜块就是铜线圈，放金块就是金线圈。

### 4. 配方里要求等级

```js
const goldOnly = {
  type: 'mmcr:level',
  io: 'input',
  type_id: 'my_mod:coil',
  level_id: 'my_mod:coil_gold'
}
```

把它放进配方的 `requirements` 数组即可。加了这条，机器必须装到金线圈才跑这条配方。

## 第二部分：修饰符

### 1. 造一个 spec

`api.modifier(...)` 有两个重载。

**数值修饰符**

```js
api.modifier(target, scope, value, operation, chance)
```

| 参数 | 取值 |
|---|---|
| `target` | 被修饰的量。常见：`'duration'`、`'energy'`、`'item'`、`'fluid'` |
| `scope` | `'input'` / `'output'` / `'machine'` / `'recipe'` |
| `value` | 数值 |
| `operation` | `'add'` / `'subtract'` / `'multiply'` / `'divide'` |
| `chance` | `true` 表示按概率生效 |

```js
api.modifier('duration', 'recipe', 0.75, 'multiply', false)
```

**布尔修饰符**

```js
api.modifier('parallelized', 'recipe', true)
```

前两个参数必须原样是 `'parallelized'` 和 `'recipe'`，否则抛 `IllegalArgumentException`。

> `target` 名对应 MMCR 内部的调整点。写错不会崩，也不会生效——排查时先确认 target 拼写。

### 2. 打包成 bundle

```js
const bundle = api.modifierDefinition(
  api.modifier('duration',   'recipe', 0.75, 'multiply', false),
  api.modifier('energy',     'recipe', 0.80, 'multiply', false)
)
```

### 3. 注册定义

```js
event.registerModifier('my_mod:overclock', bundle)

// 或者直接传数组
event.registerModifier('my_mod:overclock', [
  api.modifier('duration', 'recipe', 0.75, 'multiply', false),
  api.modifier('energy',   'recipe', 0.80, 'multiply', false)
])
```

### 4. 让物品成为修饰符载体

```js
event.registerModifierItem('my_mod:overclock_chip', 'my_mod:overclock')
```

这个物品插进机器的升级总线后就生效。**只对声明了 `.allowModifiers()` 的机器有效。**

### 5. 结构里的修饰符槽

```js
event.createStructure('my_mod:smelter')
  .pattern('BMB', 'MCM', 'BMB')
  .set('B', api.block('minecraft:iron_block'))
  .set('M', event.modifierSlot('my_mod:overclock'))
  .controller('C')
  .build()
```

第二个参数可选，表示「没插修饰符时允许用什么方块占位」：

```js
.set('M', event.modifierSlot('my_mod:overclock', api.block('minecraft:iron_block')))
```

### 6. 配方里要求修饰符

```js
event.recipes.mmcr.machine_recipe('my_mod:smelter', 200, [
  { type: 'minecraft:item', io: 'input', item: 'minecraft:ancient_debris', count: 1 },
  { type: 'minecraft:item', io: 'output', stack: { id: 'minecraft:netherite_scrap', count: 1 } }
])
```

要在 JSON 里声明修饰符需求，用 `modifiers` 字段（内部字段，一般不需要手写）。

## 常见组合

**超频模块**：耗时 ×0.75，能耗 ×1.8

```js
event.registerModifier('my_mod:overclock', [
  api.modifier('duration', 'recipe', 0.75, 'multiply', false),
  api.modifier('energy',   'recipe', 1.80, 'multiply', false)
])
```

**节能模块**：能耗 ×0.6

```js
event.registerModifier('my_mod:efficiency', [
  api.modifier('energy', 'recipe', 0.6, 'multiply', false)
])
```

**并行模块**：允许并行

```js
event.registerModifier('my_mod:parallel', [
  api.modifier('parallelized', 'recipe', true)
])
```

## 排查

| 现象 | 原因 |
|---|---|
| 等级槽成型失败 | 该位置放的方块没有任何 `MachineLevel` 的 `state` 匹配 |
| 等级换了但没效果 | 等级上没挂 `modifier(...)`，或者配方没写 `mmcr:level` 需求 |
| 插了物品没反应 | 机器没调 `.allowModifiers()`；或 `registerModifierItem` 的 modifier ID 拼错 |
| 修饰符生效但方向反了 | `multiply` 用 1.5 变慢，用 0.5 变快；`divide` 同理反过来 |

## 下一步

- [行为回调](Behaviors) —— 用代码而不是数据改行为
- [结构](Structures) —— 槽位与端口约束
