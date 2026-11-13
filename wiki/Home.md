# MMCR NekoJS Bridge

给 [Modular Machinery: Community Refoxed](https://github.com/Nibelungorum/ModularMachinery-Community-Refoxed)（下称 **MMCR**）补一个 **NekoJS** 联动附属。

MMCR 官方同时提供 **Java API** 和 **KubeJS 集成** 两种扩展方式。本附属补上第三种：**用 NekoJS 脚本定义机器、结构、配方、等级、修饰符、JEI 信息**，全程不需要 KubeJS，也不需要 Rhino。

```js
// nekojs/startup_scripts/my_machine.js
MMCREvents.startup(event => {
  event.createMachine('my_mod:smelter')
    .recipePool('my_mod:smelter')
    .displayNameKey('machine.my_mod.smelter')
    .register()
})
```

- 支持 **Minecraft 1.21.1**（NeoForge 21.1 / Java 21）与 **26.1.2**（NeoForge / Java 25）两条版本线，共用一份共享源码。
- 只依赖 `cn.howxu.mmcr.publicapi`，不碰 `cn.howxu.mmcr.internal.*`，MMCR 打补丁不会打断本附属。
- API 表面与 MMCR 官方的 KubeJS 集成**同名同语义**，迁移脚本基本就是改目录和事件名。

## 与 KubeJS 版：三处必须知道的差异

这是本 wiki 与 [MMCR 官方 wiki](https://github.com/Nibelungorum/mmcr-wiki) 差别最大的地方，先看清楚再写脚本。

| | MMCR 官方 KubeJS 版 | 本附属（NekoJS） |
|---|---|---|
| 脚本目录 | `kubejs/startup_scripts/`、`kubejs/server_scripts/` | `nekojs/startup_scripts/`、`nekojs/server_scripts/` |
| **结构写在哪** | `server_scripts`，`/reload` 可热重载 | **只能写 `startup_scripts`**，改完必须重启 |
| **静态配方写在哪** | 数据配方放 `server_scripts`，可 `/reload` | **`startup_scripts`（需重启）或数据包 JSON（可 reload）** |
| 客户端 JEI 文本 | `MMCREvents.client` | `MMCREvents.client`（放在 `client_scripts`） |
| 额外生成的文件 | `kubejs/` | `nekojs/`，另有 `jsconfig.json` / `.neko_probe/` 供 IDE 补全 |

**为什么结构不能放 `server_scripts`？** MMCR 的机器定义窗口开在 `mmcr` 自己的 `FMLConstructModEvent` 上，而 NeoForge 是「每个 mod 构造完就立刻给它发这个事件」——`mmcr` 排在 `nekojs` 前面，所以窗口关闭时 NekoJS 的 `STARTUP` 脚本还没跑。bridge 会在脚本跑完后把 MMCR 的启动阶段退回重放一次（见 [生命周期与时机](Getting-Started)）补上这一步。而 `FMLCommonSetupEvent` 里结构注册表一冻结，那之后没有任何窗口能再塞结构进去。KubeJS 之所以能热重载结构，是因为 MMCR 专门给 KubeJS 写了一套内容重载事务，而它对 NekoJS 没有这个照顾。

细节见 [生命周期与时机](Getting-Started)。

## 内容怎么分派

```
nekojs/startup_scripts/    机器定义 + 结构 + 等级 + 等级类型 + 修饰符 + 静态配方 + JEI workstation + 控制屏文本
nekojs/server_scripts/     数据配方（ServerEvents.recipes）；MMCREvents.server 只做只读/拼数据
nekojs/client_scripts/     MMCREvents.client → JEI 配方说明
```

## 五分钟上手

```js
// nekojs/startup_scripts/smelter.js —— 机器 + 结构
MMCREvents.startup(event => {
  const api = event.getAPI()

  event.createMachine('my_mod:smelter').recipePool('my_mod:smelter').register()

  event.createStructure('my_mod:smelter')
    .pattern('BHB', 'HCH', 'BHB')
    .set('B', api.block('minecraft:iron_block'))
    .set('H', api.anyOf(
      api.block('minecraft:iron_block'),
      api.itemInput(), api.itemOutput(), api.energyInput()
    ))
    .controller('C')
    .build()
})
```

```js
// nekojs/server_scripts/smelter_recipes.js —— 配方（可 /nekojs reload）
ServerEvents.recipes(event => {
  event.recipes.mmcr.machine_recipe('my_mod:smelter', 200, [
    { type: 'minecraft:item', io: 'input',  item: 'minecraft:iron_ore', count: 1 },
    { type: 'minecraft:item', io: 'output', stack: { id: 'minecraft:iron_ingot', count: 1 } },
    { type: 'neoforge:energy', io: 'input', fe_per_tick: 10 }
  ]).id('my_mod:smelter_iron')
})
```

重启游戏 → `/mmcr build my_mod:smelter` → 摆好接口就能用。完整流程见 [快速开始](Quick-Start)。

## 文档地图

**入门**

- [安装](Installation) —— 依赖、版本对应、放哪
- [生命周期与时机](Getting-Started) —— 三个注册窗口，以及为什么 NekoJS 只有两个可用
- [快速开始](Quick-Start) —— 从零到一台能跑的机器
- [导出结构](Exporting-Structures) —— 用 `/mmcr export` 生成 JS 结构代码

**API 参考**（签名级，含参数、返回、抛出）

- [总览](API-Reference)
- [事件与顶层绑定](API-Events) —— `MMCREvents` / `MMCR` / `MMCRValues`
- [机器](Machines) · [结构](Structures) · [配方](Recipes) · [等级与修饰符](Levels-and-Modifiers) · [行为回调](Behaviors) · [JEI 信息](JEI)

**其它**

- [全部示例](Examples) —— 按 MMCR 官方示例机器逐个对照
- [从 KubeJS 迁移](KubeJS-Migration)
- [排错](Troubleshooting) · [路线图](Roadmap)

## 许可

本附属为 MMCR / NekoJS 的第三方联动，新增代码按 **MIT** 发布。
MMCR 归 howxu 与贡献者所有（GPL-3.0），NekoJS 归其作者所有（LGPL-3.0）。
