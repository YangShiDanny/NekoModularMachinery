# API 参考总览

本节按 MMCR 官方 wiki 的组织方式，把本附属暴露给脚本的 API 分成七块。所有签名都取自实际代码（`cn.howxu.mmcr.nekojs.*`），不是推测。

| 页面 | 覆盖的类 |
|---|---|
| [事件与顶层绑定](API-Events) | `MMCR`、`MMCRValues`、`MMCREvents`、`MMCRApiJS`、`StartupEventJS`、`ServerEventJS`、`ClientInfoEventJS` |
| [机器](Machines) | `MachineBuilderJS` |
| [结构](Structures) | `StructureBuilderJS` |
| [配方](Recipes) | `MachineRecipeHandler`、`MachineRecipeSchema`、requirement JSON |
| [等级与修饰符](Levels-and-Modifiers) | `LevelTypeBuilderJS`、`MachineLevelBuilderJS`、`Modifiers` |
| [行为回调](Behaviors) | `TickHooks`、`RecipeHooks` 及其 Context 类型 |
| [JEI 信息](JEI) | `addRecipePoolInfo`、`addRecipeInfo`、workstation |

## 三条调用线

脚本能碰到的东西只有这么三类，认清它们就不会乱：

```js
// 1. 全局绑定（不需要 event，随时可用）
MMCR.getAPI().block('minecraft:iron_block')

// 2. 事件对象（在 MMCREvents.xxx 回调里）
MMCREvents.startup(event => {
  event.createMachine('my_mod:x')     // 声明类
  event.getAPI()                      // 和 MMCR.getAPI() 同一个东西
})

// 3. builder 链（createXxx 返回，最后 register()/build() 提交）
event.createStructure('my_mod:x').pattern(...).build()
```

## 类型速查

脚本里的参数类型不是 Java 类型照抄，能直接传什么要看这张表。

| API 期望 | 脚本里写什么 |
|---|---|
| 资源 ID（`ResourceLocation`） | `'minecraft:iron_ore'` 这样的字符串；也可以用 `api.id('...')` |
| `BlockCondition` | `api.block('...')`、`api.state('...')`、`api.tag('...')`、`api.air()`、`api.anyOf(...)`，或直接给方块 ID 字符串 |
| `ModifierBundle` | `api.modifierDefinition(...)` 或 `event.registerModifier(...)` |
| 等级槽 | `event.levelSlot('my_mod:voltage')` 的返回值 |
| 修饰符槽 | `event.modifierSlot('my_mod:overclock')` 的返回值 |
| 翻译键 | 普通字符串，如 `'machine.my_mod.smelter'` |
| 端口族名 | 字符串字面量，如 `'item_input_bus'`（完整列表见 [结构](Structures)） |

## 双版本差异

本附属同时支持 MC 1.21.1 与 26.1.2。脚本层面几乎完全一致，唯一的差异在**端口族数量**：

- **1.21.1** 有 10 个端口族（含 `pneumatic` 等联动族）
- **26.1.2** 只有 6 个

在 26.1.2 上用不存在的族名，`portTierRequirements(...)` 会打一条 warn 并跳过，不会崩。

代码层面：`ResourceLocation`（1.21.1）与 `Identifier`（26.1.2）的差异被隔离在版本层，共享代码里没有任何 ID 类型字面量，所以 API 表面看不出区别。

## 下一步

- [事件与顶层绑定](API-Events)
