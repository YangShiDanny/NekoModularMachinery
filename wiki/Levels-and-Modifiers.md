# 等级与修饰符

两个相关但不同的机制：

- **等级（Level）**：结构里某个"槽"放什么方块 → 机器变强。**升级靠改结构**。
- **修饰符（Modifier）**：往"修饰符槽"里塞物品 → 临时改变数值。**可拆卸**。

都写在 `startup_scripts`。

---

# 等级

## 1. 定义等级类型

等级类型就是一个"分类"，比如"线圈"、"涡轮"。

```js
MMCREvents.startup(event => {
  event.createLevelType('my_pack:coil')
    .displayNameKey('level.my_pack.coil')   // 不写默认 level.<ns>.<path>
    .register()
})
```

## 2. 定义具体等级

```js
event.createLevel('my_pack:coil_basic')
  .type('my_pack:coil')                  // 必填
  .state('minecraft:iron_block')         // 必填：这个等级要求槽里放什么
  .priority(0)                           // 数字大的优先匹配
  .register()

event.createLevel('my_pack:coil_elite')
  .type('my_pack:coil')
  .state('minecraft:diamond_block')
  .priority(10)
  .modifier(api.modifierDefinition([
    api.modifier('duration', 'recipe', 0.7, 'multiply', false)   // 时间 ×0.7
  ]))
  .register()
```

| 方法 | 必填 | 说明 |
|---|---|---|
| `type(typeId)` | ✅ | 对应某个 `createLevelType` |
| `state(stateId)` | ✅ | 等级槽里必须放的方块（可带状态） |
| `priority(int)` | | 多个等级都能匹配时，大的优先 |
| `modifier(bundle)` | | 这个等级自带的修饰符 |
| `register()` | | 缺 `type` 或 `state` 会抛异常 |

## 3. 结构里留出等级槽

```js
event.createStructure('my_pack:smelter')
  .pattern('BBB')
  .pattern('BLB')
  .pattern('BBB')
  .set('B', api.block('minecraft:iron_block'))
  .set('L', event.levelSlot('my_pack:coil'))   // ← 等级槽
  .controller('B')
  .build()
```

成型时 MMCR 会看 `L` 那一格实际放的是什么，去匹配所有 `type` 为 `my_pack:coil` 的等级：

- 放铁块 → 匹配 `coil_basic`
- 放钻石块 → 匹配 `coil_elite`（priority 更高，优先）
- 放别的 → 成型失败

---

# 修饰符

## 1. 定义修饰符

```js
const api = event.getAPI()

event.registerModifier('my_pack:speed', api.modifierDefinition([
  api.modifier('duration', 'recipe', 0.8, 'multiply', false),
  api.modifier('output_chance', 'output', 0.1, 'add', true)
]))
```

`api.modifier(target, scope, value, operation, chance)` 的参数：

| 参数 | 可选值 | 说明 |
|---|---|---|
| `target` | 数值字段名 | 如 `duration`、`output_chance` |
| `scope` | `input` / `output` / `machine` / `recipe` | 作用范围，**大小写不敏感** |
| `value` | 数字 | 作用值 |
| `operation` | `add` / `multiply` / `subtract` / `divide` | 运算方式，**大小写不敏感** |
| `chance` | bool | 是否按概率生效 |

`scope` 和 `operation` 也可以用常量：

```js
api.modifier('duration', api.modifierScope().RECIPE, 0.8, api.modifierOperation().MULTIPLY, false)
```

### 布尔修饰符

只有一个合法组合：

```js
api.modifier('parallelized', 'recipe', true)
```

传别的 `target`/`scope` 组合会抛异常。

## 2. 让物品能装卸修饰符

```js
event.registerModifierItem('minecraft:diamond', 'my_pack:speed')
```

玩家拿钻石右键机器的修饰符槽就能装上，再右键取下。

> ⚠️ 两个参数都是 **ID 字符串**，不是 `Item.of(...)`。

## 3. 结构里留出修饰符槽

```js
.set('M', event.modifierSlot('my_pack:speed'))
// 或者带"不装时显示什么方块"
.set('M', event.modifierSlot('my_pack:speed', api.block('minecraft:iron_block')))
```

## 4. 机器要允许修饰符

```js
event.createMachine('my_pack:smelter')
  .allowModifiers()      // ← 少了这个，修饰符槽不生效
  .register()
```

---

## 完整例子：三级线圈 + 可插拔加速卡

```js
MMCREvents.startup(event => {
  const api = event.getAPI()

  // ---- 机器 ----
  event.createMachine('my_pack:arc_furnace')
    .displayNameKey('machine.my_pack.arc_furnace')
    .appearance('minecraft:iron_block')
    .allowModifiers()
    .allowParallelism()
    .maxParallelAmount(4)
    .register()

  // ---- 等级类型 ----
  event.createLevelType('my_pack:electrode')
    .displayNameKey('level.my_pack.electrode')
    .register()

  // ---- 三个等级 ----
  event.createLevel('my_pack:electrode_graphite')
    .type('my_pack:electrode')
    .state('minecraft:coal_block')
    .priority(0)
    .register()

  event.createLevel('my_pack:electrode_copper')
    .type('my_pack:electrode')
    .state('minecraft:copper_block')
    .priority(10)
    .modifier(api.modifierDefinition([
      api.modifier('duration', 'recipe', 0.85, 'multiply', false)
    ]))
    .register()

  event.createLevel('my_pack:electrode_tungsten')
    .type('my_pack:electrode')
    .state('minecraft:netherite_block')
    .priority(20)
    .modifier(api.modifierDefinition([
      api.modifier('duration', 'recipe', 0.6, 'multiply', false),
      api.modifier('parallelized', 'recipe', true)
    ]))
    .register()

  // ---- 修饰符 ----
  event.registerModifier('my_pack:overclock', api.modifierDefinition([
    api.modifier('duration', 'recipe', 0.5, 'multiply', false),
    api.modifier('energy_input', 'machine', 2.0, 'multiply', false)
  ]))
  event.registerModifierItem('minecraft:redstone_block', 'my_pack:overclock')

  // ---- 结构 ----
  event.createStructure('my_pack:arc_furnace')
    .pattern('BBB')
    .pattern('BLE')     // L = 电极等级槽，E = 修饰符槽
    .pattern('BCB')     // C = 控制器
    .set('B', api.block('minecraft:iron_block'))
    .set('C', api.block('minecraft:iron_block'))
    .set('L', event.levelSlot('my_pack:electrode'))
    .set('E', event.modifierSlot('my_pack:overclock'))
    .controller('C')
    .build()
})
```

---

## 常见疑问

**问：等级和修饰符有什么区别？**
等级改结构（拆了重建），修饰符塞物品（随时拿走）。等级更适合"科技树"，修饰符更适合"超频"。

**问：修饰符数值叠加吗？**
多个修饰符按各自 `operation` 依次作用。具体顺序由 MMCR 决定，别做太复杂的叠加假设。

**问：`priority` 到底影响什么？**
当多个等级都能匹配同一个方块时（比如两个等级都用铁块），`priority` 大的赢。
不同方块时不冲突。

**问：修饰符装了没反应？**
先检查机器有没有 `.allowModifiers()`，再确认结构那一格是 `modifierSlot(...)` 而不是普通方块。
