# 安装

## 1. 你需要什么

| 组件 | 必需 | 说明 |
|---|---|---|
| **Modular Machinery: Community Refoxed** | ✅ | 提供机器框架与 `publicapi` |
| **NekoJS** | ✅ | 脚本引擎（GraalJS） |
| **GraalMC** | ✅ | **NekoJS 的前置**，独立模组，NekoJS 自己的 jar 里不含 Graal 运行时 |
| **本附属**（`mmcr_nekojs`） | ✅ | 桥接层 |
| NeoForge | ✅ | 版本必须与 MC 版本匹配 |
| JEI | 可选 | 装了才有配方页与结构预览 |

GraalMC 是最容易被漏掉的一个。NekoJS 构建时**故意把 Graal 排除在自己的 jar 外**（它作为独立模组发布），少了它游戏会起不来。

## 2. 版本对应

本附属有两条独立的版本线，**不能混用**：

| | Minecraft **1.21.1** | Minecraft **26.1.2** |
|---|---|---|
| Java | 21 | 25 |
| NeoForge | `21.1.238` 及以上 | `26.1.2.99` 及以上 |
| MMCR | `1.21.1-0.1.7` | `26.1.2-0.1.7` |
| NekoJS | `1.1.0-preview3` | `1.1.0-preview3` |
| GraalMC | `25.1.3.7` | `25.1.3.7` |
| 附属 jar | `mmcr_nekojs-1.21.1-<version>.jar` | `mmcr_nekojs-26.1.2-<version>.jar` |

附件文件名里的 `1.21.1` / `26.1.2` 就是 MC 版本，装错版本会在启动时报 `Missing or unsupported mandatory dependencies`。

> NeoForge 的版本号比 MC 多一段：26.1.2 的 NeoForge 是 `26.1.2.99` 这种四段式，`26.1.2` 本身不是有效版本号。

## 3. 放哪

全部丢进同一个 `mods/` 目录：

```
.minecraft/
├── mods/
│   ├── mmcr-neoforge-mc26.1.2-0.1.7.jar
│   ├── nekojs-neoforge-26.1.2-1.1.0-preview3.jar
│   ├── graalmc-neoforge-25.1.3.7.jar
│   └── mmcr_nekojs-26.1.2-0.1.1.jar        ← 本附属
└── nekojs/                                  ← 首次启动后自动生成
    ├── startup_scripts/
    ├── server_scripts/
    └── client_scripts/
```

`nekojs/` 目录由 NekoJS 在第一次启动时创建。**如果它没被创建，说明 NekoJS 本身没加载成功**，先解决那个再谈本附属。

## 4. 确认装好了

启动一次游戏，在日志里搜 `MMCR <-> NekoJS bridge ready`：

```
[main/INFO] [mmcr_nekojs/]: MMCR <-> NekoJS bridge ready (KubeJS-free)
```

再搜 `startup declarations`，它会在启动脚本跑完后打印你声明了多少东西：

```
[main/INFO] [mmcr_nekojs/]: MMCR/NekoJS startup declarations: 2 machine(s) [my_mod:smelter, my_mod:cracker], 2 structure(s), 1 level(s), 0 modifier(s)
```

这两行都在，就说明桥接层通了。机器数量是 0 也不一定是错——可能你还没写脚本。

## 5. 装完先做一件事：确认没有 KubeJS

本附属与 KubeJS **不冲突也不共用**，但如果你同时装着 KubeJS，MMCR 会走它自己的 KubeJS 集成，同时本附属也会注册一遍同名内容。同一个机器 ID 注册两次会让 MMCR 直接报重复。

要么只用一个，要么保证两边的 ID 完全不重叠。

## 6. 本地构建（可选）

如果你想自己编译：

```bash
./gradlew :mc1211:build     # 1.21.1
./gradlew :mc2612:build     # 26.1.2
# 产物：versions/<mc>/build/libs/*.jar
```

依赖从 CurseMaven（NekoJS / GraalMC）与 Modrinth Maven（MMCR）拉取，不需要手动放 jar。
仓库里没有 gradle wrapper 的二进制，第一次用之前跑一次 `gradle wrapper --gradle-version 9.8.2`。

## 下一步

- [生命周期与时机](Getting-Started) —— 先搞清楚什么能写在哪
- [快速开始](Quick-Start) —— 写第一台机器
