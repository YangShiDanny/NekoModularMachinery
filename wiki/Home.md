# MMCR NekoJS Bridge

给 [Modular Machinery: Community Refoxed](https://modrinth.com/mod/modular-machinery-community-refoxed)
加一层 [NekoJS](https://www.curseforge.com/minecraft/mc-mods/nekojs) 脚本接口，
让你**不用写 Java**就能定义多方块机器、结构、配方、等级和修饰符。

MMCR 官方只支持 KubeJS。这个附属把同一套 API 搬到 NekoJS 上——**方法名、参数、生命周期都一样**，
你写过的 KubeJS 脚本基本改个事件名就能跑。

---

## 支持版本

| 文件 | Minecraft | NeoForge | Java |
|---|---|---|---|
| `mmcr_nekojs-1.21.1-*.jar` | 1.21.1 | 21.1.238+ | 21 |
| `mmcr_nekojs-26.1.2-*.jar` | 26.1.2 | 26.1.2.99+ | 25 |

**必装**：MMCR 0.1.7+、NekoJS 1.1.0+、GraalJS 25.1.3.7+。
**不要装**：KubeJS、Rhino、NekuJS——会和 NekoJS 抢脚本引擎。

---

## 30 秒上手

`nekojs/startup_scripts/machine.js`：

```js
MMCREvents.startup(event => {
  const api = event.getAPI()

  // 1) 定义机器
  event.createMachine('my_pack:smelter')
    .appearance('minecraft:iron_block')
    .recipePool('my_pack:smelter')
    .register()

  // 2) 定义结构（3x3x1 外壳 + 中间控制器）
  event.createStructure('my_pack:smelter')
    .pattern('BBB', 'B#B', 'BBB')
    .set('B', api.block('minecraft:iron_block'))
    .set('#', api.air())
    .controller('B')          // 用外壳方块当控制器
    .build()
})
```

`nekojs/server_scripts/recipes.js`：

```js
const api = MMCR.getAPI()

ServerEvents.recipes(event => {
  event.recipes.mmcr.machine_recipe('my_pack:smelter', 100, [
    api.itemInputRequirement('minecraft:iron_ore', 1),
    api.energyRequirement('input', 20),
    api.itemOutputRequirement('minecraft:iron_ingot', 1, 1.0)
  ]).id('my_pack:iron_smelting')
})
```

重启游戏，机器就能用了。配方改完 `/nekojs reload` 就行，不用重启。

---

## 文档导航

| 页面 | 内容 |
|---|---|
| [安装](Installation) | 依赖、脚本目录、版本选择 |
| [快速开始](Quick-Start) | 从零做一个能跑的机器 |
| [机器](Machines) | `createMachine` 全部方法、外观、并行、行为钩子 |
| [结构](Structures) | `pattern` 怎么写、符号、端口要求、等级槽 |
| [配方](Recipes) | 三种写法、需求项、字段表 |
| [等级与修饰符](Levels-and-Modifiers) | 等级类型、等级、修饰符、修饰符物品 |
| [JEI 集成](JEI) | 工作站、配方说明 |
| [从 KubeJS 迁移](KubeJS-Migration) | 逐项对照，改哪几行 |
| [常见错误](Troubleshooting) | 机器不出现在 JEI / 结构不成型 / 配方被吞 |
| [路线图](Roadmap) | 还差哪些 KubeJS 高级 API |

完整方法签名见仓库里的 [`docs/API.md`](https://github.com/YangShiDanny/NekoModularMachinery/blob/main/docs/API.md)。

---

## 一个必须先知道的事

**结构、机器、等级、修饰符全部写在 `startup_scripts/`，不是 `server_scripts/`。**

KubeJS 版可以在 `server` 阶段声明结构，是因为 MMCR 专门为 KubeJS 写了一段延迟收集逻辑。
NekoJS 没有这个待遇，结构表在 `FMLCommonSetupEvent` 就冻结了，而 SERVER 脚本要到服务器启动才加载。

NekoJS 的 STARTUP 脚本在 `NekoJSMod` 构造函数里就跑完了，比 MMCR 收机器定义还早，所以走这条路是安全的。

只有**数据驱动配方**（`ServerEvents.recipes`）可以放 `server_scripts`，且支持热重载。

仓库是 [YangShiDanny/NekoModularMachinery](https://github.com/YangShiDanny/NekoModularMachinery)，
MIT 协议，问题欢迎开 issue。
