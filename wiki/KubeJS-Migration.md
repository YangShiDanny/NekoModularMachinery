# 从 KubeJS 迁移

如果你已经有一份 MMCR 的 KubeJS 脚本，本页告诉你哪些能直接搬、哪些必须改、哪些搬不过来。

## 一句话总结

**机器定义的 API 完全一样，结构要换目录，配方换写法。** 剩下的是三件搬不过来的事，见第 4 节。

## 1. 目录对照

| KubeJS | NekoJS | 说明 |
|---|---|---|
| `kubejs/startup_scripts/` | `nekojs/startup_scripts/` | 机器、等级、修饰符、**结构** |
| `kubejs/server_scripts/` | `nekojs/server_scripts/` | 只有配方 |
| `kubejs/client_scripts/` | `nekojs/client_scripts/` | JEI 说明 |
| `kubejs/assets/<ns>/lang/` | `nekojs/assets/<ns>/lang/` | 翻译 |
| `kubejs/data/<ns>/` | `nekojs/data/<ns>/` | 数据包内容 |

NekoJS 还会在每个脚本目录生成 `jsconfig.json` 和 `.neko_probe/`（IDE 补全用），这些不用管。

## 2. 代码对照

### 可以原样保留

```js
MMCREvents.startup(event => { ... })
MMCREvents.server(event => { ... })
MMCREvents.client(event => { ... })

event.createMachine('ns:name')
  .recipePool(...)
  .displayNameKey(...)
  .appearance(...)
  .controllerTextures(...)
  .allowModifiers()
  .allowParallelism()
  .maxParallelAmount(...)
  .tickBehavior(...)
  .recipeBehavior(...)
  .register()

event.createStructure('ns:name')
  .pattern(...)
  .set(...)
  .controller(...)
  .portRequirements(...)
  .build()

event.createLevelType(...) / event.createLevel(...)
event.registerModifier(...) / event.registerModifierItem(...)
event.levelSlot(...) / event.modifierSlot(...)
event.addRecipePoolWorkstation(...) / event.addMachineWorkstation(...)
```

### 方法名要改

| KubeJS（`KubeJSApi`） | NekoJS（`MMCRApiJS`） |
|---|---|
| `api.anyOfItemInput()` | `api.itemInput()` |
| `api.anyOfItemOutput()` | `api.itemOutput()` |
| `api.anyOfFluidInput()` | `api.fluidInput()` |
| `api.anyOfFluidOutput()` | `api.fluidOutput()` |
| `api.anyOfEnergyInput()` | `api.energyInput()` |
| `api.anyOfEnergyOutput()` | `api.energyOutput()` |
| `api.anyOfHeatInput()` | `api.heatInput()` |
| `api.anyOfHeatOutput()` | `api.heatOutput()` |

规律：去掉 `anyOf` 前缀、首字母小写。其余 `block` / `state` / `tag` / `air` / `any` / `anyOf` / `id` **一样**。

### 结构必须换目录

这是最容易漏的一条：

```js
// KubeJS：server_scripts 里也能用，还能 /reload
MMCREvents.server(event => {
  event.createStructure('my_mod:x').pattern(...).build()
})

// NekoJS：必须挪到 startup_scripts，而且用 MMCREvents.startup
MMCREvents.startup(event => {
  event.createStructure('my_mod:x').pattern(...).build()
})
```

挪过去之后，改结构要**重启游戏**，不能 `/reload` 了。

### 配方

KubeJS 的 `ServerEvents.recipes` 和 NekoJS **同名**，所以这段基本不用改：

```js
ServerEvents.recipes(event => {
  event.custom({
    type: 'mmcr:machine_recipe',
    recipe_pool: 'my_mod:smelter',
    tick_time: 200,
    requirements: [ /* 一字不改 */ ]
  }).id('my_mod:smelter_iron')
})
```

本附属还注册了 schema，所以可以写成更短的位置参数形式：

```js
event.recipes.mmcr.machine_recipe('my_mod:smelter', 200, [ /* requirements */ ])
```

## 3. 逐条迁移清单

1. 新建 `nekojs/startup_scripts/`、`server_scripts/`、`client_scripts/`
2. 把所有 `kubejs/startup_scripts/*.js` 复制过去，替换 `anyOf*` 方法名
3. 把 `kubejs/server_scripts/` 里的**结构**脚本移到 `startup_scripts/`，把 `MMCREvents.server` 改成 `MMCREvents.startup`
4. `server_scripts/` 里只留 `ServerEvents.recipes(...)`
5. `client_scripts/` 里的 `MMCREvents.client` 原样保留
6. 翻译文件搬到 `nekojs/assets/<ns>/lang/`
7. 重启游戏，看日志的 `startup declarations` 数量对不对
8. `/mmcr build <机器ID>` 逐个验证结构

## 4. 三件搬不过来的事

| | KubeJS 能做 | NekoJS 不能 | 替代方案 |
|---|---|---|---|
| **结构热重载** | 改结构 `/reload` 立即生效 | 结构在启动期冻结 | 用 `/mmcr build` 快速验证，改完重启 |
| **配方程序化构建** | `MachineRecipeBuilderJS` 在 server 阶段构建 | 配方窗口在服务端脚本之前关闭 | 用数据 JSON（`ServerEvents.recipes`），反而**能** reload |
| **KubeJS 专属 reload 事务** | MMCR 为 KubeJS 单独实现了内容重载 | 对 NekoJS 无此照顾 | 无 |

第 4 行意味着：任何依赖「运行时重新注册 MMCR 内容」的脚本，迁移后都不成立。如果你的包严重依赖这个特性（比如做游戏内编辑器），建议继续用 KubeJS。

## 5. 混用的情况

同一个整合包里**同时**装 KubeJS 和本附属是可行的，但：

- MMCR 会同时启用 KubeJS 集成和本附属
- 两边注册的机器/结构 ID **不能重复**，否则 MMCR 报重复
- 建议分工：KubeJS 做需要热重载的部分，NekoJS 做需要 GraalJS/TypeScript/模块化的部分

如果只是想跑现成的 KubeJS 包，**不要装本附属**——没必要。

## 6. 一个完整的迁移例子

**迁移前**（KubeJS，两个文件）：

```js
// kubejs/startup_scripts/smelter.js
MMCREvents.startup(event => {
  event.createMachine('my_mod:smelter').recipePool('my_mod:smelter').register()
})
```

```js
// kubejs/server_scripts/smelter_structure.js
MMCREvents.server(event => {
  const api = event.getAPI()
  event.createStructure('my_mod:smelter')
    .pattern('BBB', 'B#B', 'BBB')
    .set('B', api.block('minecraft:iron_block'))
    .set('#', api.air())
    .controller('B')
    .build()
})
```

**迁移后**（NekoJS，一个文件）：

```js
// nekojs/startup_scripts/smelter.js
MMCREvents.startup(event => {
  const api = event.getAPI()

  event.createMachine('my_mod:smelter').recipePool('my_mod:smelter').register()

  event.createStructure('my_mod:smelter')
    .pattern('BBB', 'BCB', 'BBB')      // 注意：控制器改成独立的 C
    .set('B', api.block('minecraft:iron_block'))
    .controller('C')
    .build()
})
```

顺便修掉了原脚本的两个问题：控制器符号和外壳符号混用、以及 `set('#', air)` 但结构里没有实际留空。

## 下一步

- [生命周期](Getting-Started) —— 理解为什么结构必须挪位置
- [全部示例](Examples) —— 按官方示例逐个看
