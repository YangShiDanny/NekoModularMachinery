# MMCR NekoJS Bridge（`mmcr_nekojs`）

[![Build & Release](https://github.com/YangShiDanny/NekoModularMachinery/actions/workflows/build.yml/badge.svg)](https://github.com/YangShiDanny/NekoModularMachinery/actions/workflows/build.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)
[![Minecraft](https://img.shields.io/badge/Minecraft-1.21.1%20%7C%2026.1.2-blue)](https://github.com/YangShiDanny/NekoModularMachinery/releases)

给 **Modular Machinery: Community Refoxed (MMCR)** 补一个 **NekoJS** 联动附属：
在 NekoJS 脚本里用与官方 KubeJS 集成**同名、同语义**的 API 定义机器、结构、配方、等级、修饰符、JEI 信息。
全程 **不依赖 KubeJS / Rhino / NekuJS**，直接走 MMCR 的 `publicapi`。

- 版本：**Minecraft 1.21.1 (NeoForge 21.1 / Java 21)** 与 **26.1.2 (NeoForge / Java 25)**，两者同时构建。
- 依据：NekoJS 官方 Wiki（已用 GitHub 镜像克隆到 `../nekojs-wiki/`）+ MMCR 源码 `neo/1.21.1`、`neo/26.1.2` 分支与 MMCR Wiki。
- 下载：见 [Releases](https://github.com/YangShiDanny/NekoModularMachinery/releases)。

## 文档

| 文档 | 内容 |
|---|---|
| [**Wiki**](https://github.com/YangShiDanny/NekoModularMachinery/wiki) | 教程：安装、快速开始、机器、结构、配方、迁移、排错 |
| [`docs/API.md`](docs/API.md) | 参考手册：全部方法签名、参数、返回值 |
| [`examples/`](examples/) | 可直接跑的示例：太空电梯（主机 + 模块），Python 脚本 + lang |
| [`wiki/`](wiki/) | Wiki 源码（CI 自动同步到 GitHub Wiki） |

---

## 1. 为什么不能直接复用 MMCR 的 KubeJS 集成

MMCR 的 KubeJS 联动层在 `cn.howxu.mmcr.compat.kubejs`（22 个类），它编译期依赖
`dev.latvian.mods.kubejs.*`（`KubeJSPlugin`、`EventGroup`、`RecipeSchema` …）。没装 KubeJS 时这些类不存在，
jar 里整个包都加载不了；NekuJS 兼容插件又只支持 1.21.1。因此本附属**另写一层桥**：

```
NekoJS 脚本  ── MMCREvents.startup / server / client ──>  Java 插件(builder)
                                                            │
                                      MMCR publicapi (Machines / Structures / Recipes)
                                                            │
                                      RegisterMachineDefinitionsEvent (ServiceLoader)
                                      RegisterMachineStructuresEvent / RegisterMachineRecipesEvent
                                      RegisterJeiWorkstationsEvent / RegisterJeiRecipeInformationEvent
```

配方另有一条**数据驱动**路径：`ServerEvents.recipes` + `event.custom({type:'mmcr:machine_recipe', ...})`，
由 `scriptpack/` 提供 recipe schema 后也可写成 `event.recipes.mmcr.machine_recipe(pool, ticks, requirements)`。

## 2. 版本矩阵

| | 1.21.1 | 26.1.2 |
|---|---|---|
| Gradle 子项目 | `:mc1211` → `versions/1.21.1` | `:mc2612` → `versions/26.1.2` |
| Java | 21 | 25 |
| 资源 ID 类 | `ResourceLocation` | `Identifier` |
| 端口族数 | 10（含 stress/source/mana/air） | 6（物品/流体/能量/化学品/放射性化学品/热） |
| NekoJS | NeoForge 1.21.1 版 | NeoForge 26.1.2 版 |
| MMCR | `1.21.1-0.1.7` | `26.1.2-0.1.7` |
| 差异处理 | `version/Ids.java` + `version/VersionSupportImpl.java` | 同左 |

两个分支的 `publicapi` 主体一致（Draft/Registrar 签名逐字相同，已用脚本比对过），
差异集中在两处，都由 `common` 之外的 `version/` 包隔离：

- **`Ids.java`**：`ResourceLocation` vs `Identifier`
- **`VersionSupportImpl.java`**：26.1.2 缺 4 个端口族，`PortTierLimits.Builder` 也没有对应方法；
  `RecipeDraft` 同样没有 stress/source/mana/air 的 I/O。共享代码统一走 `VersionSupport` 接口，
  不支持的族记一条警告后跳过。

`common/src/main/java` 是同一份源码，共享代码里**不出现** `ResourceLocation` / `Identifier` 字面量
（用 `var` 推断），这是同时兼容两个 MC 版本的关键。

## 3. 目录

```
mmcr-nekojs/
├── common/src/main/java/cn/howxu/mmcr/nekojs/
│   ├── MMCRNekoJS.java            @Mod 入口（必须存在，否则 NeoForge 不扫描事件订阅者）
│   ├── MMCRNekoJSPlugin.java      @RegisterNekoJSPlugin 入口、生命周期
│   ├── MMCRNekoEvents.java        MMCREvents 事件组 (startup/server/client)
│   ├── MMCRBinding.java           全局 MMCR 绑定
│   ├── MMCRApiJS.java             KubeJSApi 对等门面
│   ├── MMCRValuesJS.java          INT_MAX/INT_MIN/...
│   ├── api/                       常量对象、LevelSlot/ModifierUse 引用
│   ├── builder/                   Machine/Structure/Level/Recipe builder
│   ├── event/                     Startup/Server/Client 事件对象
│   ├── bridge/                    DeclarationStore + MMCR 注册器 + ServiceLoader
│   ├── recipe/                    mmcr:machine_recipe 的 schema、namespace handler
│   └── version/VersionSupport     版本差异接口（实现放在各 versions/ 下）
├── versions/1.21.1/, versions/26.1.2/
│   ├── build.gradle.kts          ModDevGradle + 依赖坐标引用
│   ├── gradle.properties         neo_version / nekojsCoordinate / mmcrCoordinate
│   ├── src/main/java/.../version/ Ids.java + VersionSupportImpl.java
│   └── src/main/resources/META-INF/  neoforge.mods.toml + services/
├── .github/workflows/
│   ├── build.yml                 矩阵编译 + 自动打 tag 发 Release
│   └── wiki.yml                  把 wiki/ 同步到 GitHub Wiki
├── scriptpack/packs/mmcr_nekojs/         NekoJS 版本化脚本包（recipe schema，可选）
├── examples/                             太空电梯示例（.py）: startup/server/client + lang
├── wiki/                                 Wiki 源码（教程）
├── docs/API.md                           参考手册（全部方法签名）
└── LICENSE                               MIT
```

## 4. 构建

### 4.1 依赖从哪来

两个上游都**没有发布 Maven 制品**，但都能通过公共 Maven 代理拉到，所以仓库里**不需要提交任何 jar**：

| 依赖 | 来源 | 坐标 |
|---|---|---|
| NekoJS | CurseForge only → [CurseMaven](https://cursemaven.com) | `curse.maven:nekojs-1518701:<fileId>` |
| **GraalMC** | CurseMaven | `curse.maven:graal-1504336:8762962` |
| MMCR | Modrinth → Modrinth Maven | `maven.modrinth:modular-machinery-community-refoxed:<version>` |
| NeoForge / MC | `maven.neoforged.net`，由 [ModDevGradle](https://github.com/neoforged/ModDevGradle) 处理 | — |

> **为什么还要 GraalMC**：NekoJS 的 `EventBusJS` 实现了 `graal.graalvm.polyglot.proxy.ProxyExecutable`，
> 但 NekoJS 打包时**故意把 Graal 排除在外**（它作为独立模组发布）。所以编译期必须把 GraalMC 也放上
> classpath，否则 javac 会报 `cannot access ProxyExecutable`。这两个 jar 都只是 `compileOnly`，
> 不会进产物。

坐标写在 `versions/<mc>/gradle.properties` 里，改版本只动这一处：

```properties
neo_version=21.1.238
nekojsCoordinate=curse.maven:nekojs-1518701:9048720
graalCoordinate=curse.maven:graal-1504336:8762962
mmcrCoordinate=maven.modrinth:modular-machinery-community-refoxed:1.21.1-0.1.7
```

升级 NekoJS 时把 `fileId` 换成 [CurseForge files 页](https://www.curseforge.com/minecraft/mc-mods/nekojs/files)
上对应版本的数字 ID 即可（页面 URL 末尾那个数）。

### 4.2 本地编译

```bash
# 需要 JDK 21（Gradle 守护进程用）；JDK 25 由 toolchain 自动下载
gradle :mc1211:build     # → versions/1.21.1/build/libs/mmcr_nekojs-1.21.1-<version>.jar
gradle :mc2612:build     # → versions/26.1.2/build/libs/mmcr_nekojs-26.1.2-<version>.jar
```

仓库**不提交 wrapper**。第一次用请生成一次：

```bash
gradle wrapper --gradle-version 9.8.2
```

之后就能用 `./gradlew`。`common/src/main/java` 会被两个子项目各自编译一遍，
依赖是 `compileOnly`，NekoJS / MMCR 的类不会被 shaded 进产物。

### 4.3 离线 / 内网构建

如果 CI 或内网拉不到 CurseMaven 与 Modrinth，把 jar 丢进 `versions/<mc>/libs/`
并在该版本的 `build.gradle.kts` 里追加：

```kotlin
dependencies { compileOnly(fileTree("libs") { include("*.jar") }) }
```

`libs/` 已在 `.gitignore` 里，不会被误提交。

### 4.4 CI 与自动发布

`.github/workflows/build.yml` 两条流水线合一：

| 触发 | 行为 |
|---|---|
| PR / 推任意分支 | 矩阵编译 1.21.1 与 26.1.2，上传 artifact，**不发布** |
| 推到 `main` / `master` | 编译通过后，若 `v<mod_version>` 这个 tag 还不存在，**自动打 tag 并发布 Release** |
| 推 `v*` tag | 直接发布该 tag 的 Release |
| 手动 `workflow_dispatch` | 可勾 `force_release` 强制重发已有 tag |

发版流程就是改一行版本号再 push：

```properties
# gradle.properties
mod_version=0.1.1        # ← 改这里
```

然后 `git commit && git push`，CI 会打出 `mmcr_nekojs-1.21.1-0.1.1.jar` 和
`mmcr_nekojs-26.1.2-0.1.1.jar`，创建 `v0.1.1` tag 和 Release。
版本号没变就只编译不发版，不会刷屏。

CI 里还有一道产物校验：解包 jar 确认 `META-INF/neoforge.mods.toml` 和
`META-INF/services/cn.howxu.mmcr.publicapi.registration.MachineDefinitionProvider` 都在，
缺任何一个直接失败——这两个文件缺了游戏里会静默不加载。


## 5. 安装到游戏

`mods/` 里放：对应版本的 **MMCR + NekoJS + Graal(≥25.1.3.7) + `mmcr_nekojs-*.jar`**。
**不要**再放 KubeJS / Rhino / NekuJS。

脚本包二选一：

- 把 `scriptpack/packs/mmcr_nekojs/` 整个复制到 `nekojs/packs/`（作为 `GLOBAL:mmcr_nekojs` 版本化包）；或
- 只要 recipe schema：把 `scriptpack/packs/mmcr_nekojs/data/mmcr/nekojs/recipe_types/machine_recipe.json`
  复制到 `nekojs/data/mmcr/nekojs/recipe_types/machine_recipe.json`。

把 `examples/` 下四个 `.py` 分别放进 `nekojs/startup_scripts/`、`server_scripts/`、`client_scripts/`，
`assets/` 放进 `nekojs/assets/`，首次启动后按需要执行 `/nekojs probe` 刷新补全。

> 这些示例是 MMCR 官方 `A_Module_Machine.js`（太空电梯）的 **Python 版**：NekoJS 内置 Python
> 转译器，`.py` 和 `.js` 一样丢进脚本目录就会被加载，全局绑定完全相同。
> 注意 `assets/mmcr/lang/*.json` 是**控制器方块名**——控制器统一注册在 `mmcr` 命名空间下，
> 写在自己命名空间里是不会生效的（见 [排错](wiki/Troubleshooting.md)）。

## 6. 脚本示例（与 KubeJS 版对照）

**关键规则：机器 / 结构 / 等级 / 修饰符全部写在 `startup_scripts`。** 原因见第 7 节。

```js
// startup_scripts —— 改了必须重启
MMCREvents.startup(event => {
  const api = event.getAPI()

  // 1. 机器
  event.createMachine('my_pack:blast_furnace')
    .recipePool('my_pack:blast_furnace')
    .displayNameKey('machine.my_pack.blast_furnace')
    .appearance('my_pack:machine_casing')
    .controllerTextures('my_pack:block/controller_front', 'my_pack:block/controller_side')
    .register()

  // 2. 结构（也在 startup）
  event.createStructure('my_pack:blast_furnace')
    .pattern('BBB', 'BPB', 'BBB')
    .pattern('BBB', 'PFP', 'BBB')
    .set('B', api.block('my_pack:machine_casing'))
    .set('P', api.itemInput())
    .set('F', api.fluidInput())
    .controller('C')
    .build()
})

// server_scripts —— /nekojs reload 即可
ServerEvents.recipes(event => {
  event.recipes.mmcr.machine_recipe('my_pack:blast_furnace', 200, [
    MMCR.getAPI().itemInputRequirement('minecraft:iron_ingot', 2)
  ]).id('my_pack:iron')
})
```

完整对照见 `docs/API.md`，可运行示例见 `examples/`，完整文档见 [`wiki/`](wiki/Home.md)。

## 7. 已知限制（务必阅读）

1. **所有形状声明必须在 `startup_scripts`。** 这不是本附属的偏好，而是 MMCR 的时序决定的：

   | 阶段 | MMCR 做什么 | NekoJS 脚本状态 |
   |---|---|---|
   | `mmcr` 构造函数 → `mmcr` 的 `FMLConstructModEvent` | `registerProductionForModStartup` → `ServiceLoader` 收机器定义 → post `RegisterMachineDefinitionsEvent` | **脚本还没跑**（NekoJS 还没被构造） |
   | `nekojs` 构造函数 | — | **`STARTUP` 脚本在这里执行完毕** |
   | `FMLCommonSetupEvent` | `completeProductionForModStartup` → post `RegisterMachineStructuresEvent`，然后 `freeze` + `commitStructures` | — |
   | `ModifyDefaultComponentsEvent` | `completeProductionRecipesAfterComponentsBound` → post `RegisterMachineRecipesEvent` | — |
   | 服务器启动 | — | `SERVER` 脚本此时才加载（**太晚**） |

   **关键一点：`FMLConstructModEvent` 不是"所有 mod 构造完之后统一发一次"，而是每个 mod 构造完
   立刻单独发给自己**（`ModLoader.lambda$constructMods$0` → `container.constructMod()` 紧接着
   `acceptEvent(new FMLConstructModEvent(...))`，跑在同一个并行 worker 里）。而 mod 构造顺序按加载
   顺序，`mmcr` 排在 `nekojs` 前面——所以 MMCR 定义窗口关闭时，NekoJS 连构造函数都还没进。
   MMCR 也只对 **KubeJS** 延迟结构收集
   （`completeProductionForModStartup` 里写死了
   `ModList.get().isLoaded("kubejs") && !Plugin.startupScriptsLoaded()`），对本附属没有这种照顾。

   **本附属的处理**：`STARTUP` 脚本跑完、`afterScriptsLoaded` 回来的那一刻，bridge 调用
   `StartupContentRegistration.resetForTesting()` 把 MMCR 的启动阶段退回 `NOT_STARTED`，
   再跑一次 `registerProductionForModStartup(mmcr 的 mod 总线)`。此时离
   `FMLCommonSetupEvent` 还早，没有任何内容被 commit，所以这次重放就是"第一次真正生效"的收集——
   `ServiceLoader` 拿到的就是我们填满的 `DeclarationStore`。日志会打印
   `submitted N machine definition(s) through a reopened window`。
   （想关掉这个兼容层：`-Dmmcrnekojs.disableStartupReplay=true`。）

   因此 `ServerEventJS` **只暴露 `getAPI()`**——不提供
   `createStructure` / `createLevel` / `createRecipe`，让错误在写脚本时就暴露，
   而不是运行时静默丢弃。

   万一 `resetForTesting()` / `registerProductionForModStartup(...)` 在某个 MMCR 版本里改签名，
   bridge 只打一条 `could not replay the definition window` 错误，不会崩游戏；同样地，任何
   MMCR 拒绝的 structure/level/modifier 只会被跳过并以 `skipped N declaration(s)` 汇总报告，
   不会再让 `FMLCommonSetupEvent` 抛异常把启动打断。

2. **没有程序化配方 builder。** `RecipeDraft` 只接受真实的 `Item` / `Ingredient` 对象，
   而 MMCR 的配方窗口（`ModifyDefaultComponentsEvent`）在服务端脚本加载之前就关了，
   STARTUP 阶段物品注册表又还没填充完——两头都接不上。
   配方统一走 `mmcr:machine_recipe` 数据 JSON（`event.recipes.mmcr.machine_recipe(...)`
   或 `event.custom({...})`），它是标准配方类型，走 `RecipeManager`，
   反而**能** `/nekojs reload` 热重载。

3. **两个 MC 版本的端口族不一样。** 1.21.1 有 10 族（含 Create 应力、Ars 源、Botania 魔力、
   PneumaticCraft 空气）；26.1.2 只剩 6 族（物品/流体/能量/化学品/放射性化学品/热）。
   26.1.2 的 `RecipeDraft` 也没有 stress/source/mana/air 的 I/O。
   本附属用 `version/VersionSupport` 隔离这层差异：在 26.1.2 上引用不存在的族会给出警告并跳过。

4. **行为回调。** `tickBehavior(cb)` / `recipeBehavior(cb)` 依赖 GraalJS 把 JS 函数转成
   Java `Consumer`。若运行时出现类型转换错误，请改成在 Java 侧扩展 `MachineDraft`；
   常见被动机器可用 `event.custom` 配方 + 端口实现，不必用 tick。

5. **未覆盖的 KubeJS 高级 API**（见 `docs/API.md`）：网络请求 / `DataValue`、
   智能接口运行时值、Mekanism 化学品快捷工厂、`SmartInterfaces` 类型注册、
   控制器渲染器扩展。这些都可通过在 `common/` 加 Java 方法或直接手写 JSON 补齐。

6. **构建验证状态。** `common/` + 两个版本的 `version/` 已用**真实发布 jar**
   （NekoJS `1.1.0-preview3`、MMCR `0.1.7`、GraalMC `25.1.3.7`）做 `javac` 编译，
   **1.21.1 与 26.1.2 均 0 错误**。26.1.2 的 jar 是 Java 25 字节码，必须用 JDK 25 编译。
   Gradle 打包链路（ModDevGradle + CurseMaven + Modrinth）由 CI 验证。

7. `neoforge.mods.toml` 的版本范围是保守值，按你实际使用的 NeoForge / MMCR 版本调整。

## 8. 参考资料

- NekoJS Wiki：`../nekojs-wiki/`（镜像 `https://ghfast.top/https://github.com/NekoJS-Dev/NekoJS.wiki.git`）
  重点页：`plugin-development_cn.md`、`event-extensions_cn.md`、`recipe-system_cn.md`、`script-packs_cn.md`、`platform-compatibility_cn.md`
- MMCR Wiki：`https://mmcr-wiki.howxu.cn/zh-cn/API/KubeJS.html`（KubeJS API 全量）、`JavaAPI.html`（publicapi）
- MMCR 源码：`Nibelungorum/ModularMachinery-Community-Refoxed` 分支 `neo/1.21.1` / `neo/26.1.2`
- NekoJS 源码：`NekoJS-Dev/NekoJS`（插件 API 包名核实）

## 许可

本附属新增的代码按 **MIT** 发布（见 `LICENSE`）。

它是一个运行时链接 MMCR 与 NekoJS 的第三方联动：本仓库不包含二者任何源码，只在
`compileOnly` 下引用其 API，因此 MIT 与上游许可（MMCR 的 GPL-3.0、NekoJS 的 LGPL-3.0）不冲突。
分发时请自行遵守上游许可。
