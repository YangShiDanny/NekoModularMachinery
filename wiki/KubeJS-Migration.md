# 从 KubeJS 迁移

MMCR 的 KubeJS 集成和这个附属**共用同一套 MMCR Public API**，所以脚本长相几乎一样。
大多数脚本只需要改三处。

---

## 三处必改

### 1. 结构从 `server_scripts` 挪到 `startup_scripts`

```js
// ❌ KubeJS：这样能work
// server_scripts/structure.js
MMCREvents.server(event => {
  event.createStructure('my_pack:smelter').pattern(...).build()
})
```

```js
// ✅ NekoJS：必须放 startup_scripts
// startup_scripts/structure.js
MMCREvents.startup(event => {
  event.createStructure('my_pack:smelter').pattern(...).build()
})
```

**为什么**：MMCR 专门为 KubeJS 写了一段延迟结构收集（`isLoaded("kubejs")` 分支），
NekoJS 没有这个待遇。`FMLCommonSetupEvent` 一到结构表就冻结，而 SERVER 脚本那时还没加载。

同理，`event.createRecipe(...)` 也要挪到 `startup`。

### 2. 方块/物品注册换成 NekoJS 的 API

```js
// ❌ KubeJS
StartupEvents.registry('block', event => {
  event.create('my_pack:casing')
})
```

```js
// ✅ NekoJS
RegistryEvents.register(event => {
  event.block('my_pack:casing', block => {
    // 可选的方块属性配置
  })
})
```

### 3. `Item.of(...)` → ID 字符串

```js
// ❌ KubeJS 老写法
event.registerModifierItem(Item.of('minecraft:diamond'), 'my_pack:speed')
event.addRecipePoolWorkstation('my_pack:pool', Item.of('minecraft:furnace'))
```

```js
// ✅ NekoJS
event.registerModifierItem('minecraft:diamond', 'my_pack:speed')
event.addRecipePoolWorkstation('my_pack:pool', 'minecraft:furnace')
```

MMCR 已经把这两个接口迁移成"启动期只存 ID"，所以传字符串才是正确姿势。

---

## 不用改的

好消息：下面这些**一模一样**。

```js
// 事件名、事件对象、getAPI()
MMCREvents.startup(event => { const api = event.getAPI() })

// 机器
event.createMachine('my_pack:smelter')
  .displayNameKey('machine.my_pack.smelter')
  .appearance('minecraft:iron_block')
  .recipePool('my_pack:smelter')
  .allowModifiers()
  .register()

// 结构（只是要挪目录）
event.createStructure('my_pack:smelter')
  .pattern('BBB')
  .pattern('BCB')
  .pattern('BBB')
  .set('B', api.block('minecraft:iron_block'))
  .controller('C')
  .build()

// 等级
event.createLevelType('my_pack:coil').register()
event.createLevel('my_pack:coil_basic').type('my_pack:coil').state('minecraft:iron_block').register()

// 修饰符
event.registerModifier('my_pack:speed', api.modifierDefinition([
  api.modifier('duration', 'recipe', 0.8, 'multiply', false)
]))

// 配方（server_scripts，不用挪）
ServerEvents.recipes(event => {
  event.custom({ type:'mmcr:machine_recipe', recipe_pool:'...', tick_time:100, requirements:[...] }).id('...')
})
```

---

## 逐项对照表

| 项目 | KubeJS | NekoJS（本附属） | 要改吗 |
|---|---|---|---|
| 事件组 | `MMCREvents.startup/server/client` | 同 | ❌ |
| 事件 ID | `mmcr.startup` / `mmcr.server` / `mmcr.client` | 同 | ❌ |
| `event.getAPI()` | ✅ | ✅ | ❌ |
| 全局 `MMCR.getAPI()` | ✅ | ✅ | ❌ |
| `MMCRValues.INT_MAX` 等 | ✅ | ✅ | ❌ |
| `createMachine` | ✅ | ✅ | ❌ |
| `createStructure` | ✅ | ✅ | ⚠️ 必须挪到 startup |
| `createLevelType` / `createLevel` | ✅ | ✅ | ❌ |
| `createRecipe` | ✅ | ✅ | ⚠️ 必须挪到 startup |
| `registerModifier` | ✅ | ✅ | ❌ |
| `registerModifierItem` | 接受 ItemStack | 只接受 ID 字符串 | ✅ |
| `addRecipePoolWorkstation` | 接受 ItemStack | 只接受 ID 字符串 | ✅ |
| `addMachineWorkstation` | ✅ | ✅ | ❌ |
| `registerControllerScreenText` | ✅ | ✅ | ❌ |
| 配方 schema `mmcr:machine_recipe` | ✅ 由 MMCR 提供 | ✅ 由本附属提供 | ❌ |
| 方块注册 | `StartupEvents.registry` | `RegistryEvents.register` | ✅ |
| `readableNumberBigInt` | ✅ | ❌ | 用 `readableNumberExact` |
| 化学品/魔法快捷工厂 | ✅ | ❌ | 手写 JSON |
| 网络/智能接口运行时 | ✅ | ❌ | 见[路线图](Roadmap) |

---

## 迁移检查清单

- [ ] 所有 `event.createStructure(...)` 从 `server_scripts` 挪到 `startup_scripts`
- [ ] 所有 `event.createRecipe(...)` 同上
- [ ] `Item.of('x')` 全部改成 `'x'`
- [ ] `StartupEvents.registry('block'|'item', ...)` 改成 `RegistryEvents.register(...)`
- [ ] 配方保持 `server_scripts` + `ServerEvents.recipes`，不用动
- [ ] 移除 KubeJS / Rhino / NekuJS 的 jar
- [ ] 重启游戏，在日志里确认 `submitted N machine definition(s)`

---

## 一个完整的前后对比

**KubeJS 版（`server_scripts/machine.js`）**

```js
MMCREvents.server(event => {
  const api = event.getAPI()

  event.createStructure('my_pack:smelter')
    .pattern('BBB').pattern('BCB').pattern('BBB')
    .set('B', api.block('minecraft:iron_block'))
    .controller('C')
    .build()
})

MMCREvents.startup(event => {
  event.createMachine('my_pack:smelter')
    .appearance('minecraft:iron_block')
    .register()
})
```

**NekoJS 版（`startup_scripts/machine.js`）**

```js
MMCREvents.startup(event => {
  const api = event.getAPI()

  event.createMachine('my_pack:smelter')
    .appearance('minecraft:iron_block')
    .register()

  // ← 挪进来了
  event.createStructure('my_pack:smelter')
    .pattern('BBB').pattern('BCB').pattern('BBB')
    .set('B', api.block('minecraft:iron_block'))
    .controller('C')
    .build()
})
```

配方不动。就这么点改动。
