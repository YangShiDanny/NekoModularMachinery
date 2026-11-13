# 安装

## 你需要什么

| 组件 | 版本 | 从哪下 |
|---|---|---|
| Minecraft | 1.21.1 **或** 26.1.2 | — |
| NeoForge | 21.1.238+ / 26.1.2.99+ | [neoforged.net](https://neoforged.net) |
| Modular Machinery: Community Refoxed | 0.1.7+ | [Modrinth](https://modrinth.com/mod/modular-machinery-community-refoxed) |
| NekoJS | 1.1.0-preview3+ | [CurseForge](https://www.curseforge.com/minecraft/mc-mods/nekojs) |
| GraalJS | 25.1.3.7+ | NekoJS 的页面会写明对应版本 |
| **本附属** | 对应版本 | [Releases](https://github.com/YangShiDanny/NekoModularMachinery/releases) |

### 别装这些

**KubeJS / Rhino / NekuJS** —— 它们和 NekoJS 是同一类东西（脚本引擎），同时装会冲突。
这个附属的价值就是让你在没有 KubeJS 的情况下用 MMCR。

### 选哪个 jar

Release 里有两个文件，按你的 Minecraft 版本选**一个**：

```
mmcr_nekojs-1.21.1-0.1.0.jar   ← MC 1.21.1
mmcr_nekojs-26.1.2-0.1.0.jar   ← MC 26.1.2
```

名字里的版本号必须和你的 MC 版本对上。放错版本游戏会直接拒绝加载（`mods.toml` 里有
`versionRange` 检查）。

---

## 安装步骤

1. 把上表所有 jar 丢进 `mods/` 文件夹。
2. 启动一次游戏，让它生成目录结构。
3. 退出游戏，你的 `nekojs/` 目录现在长这样：

```
.minecraft/
└── nekojs/
    ├── startup_scripts/     ← 机器、结构、等级、修饰符
    ├── server_scripts/      ← 配方
    ├── client_scripts/      ← JEI 说明
    └── assets/
```

4. 把脚本写进去，重启游戏。

**如果 `nekojs/` 目录没出现**，说明 NekoJS 没加载成功——先单独排查 NekoJS，别急着装本附属。

---

## 脚本放哪：一张表说清

| 你要做的事 | 放在 | 改完要重启吗 |
|---|---|---|
| 定义机器 | `startup_scripts/` | ✅ 要 |
| 定义结构 | `startup_scripts/` | ✅ 要 |
| 定义等级类型 / 等级 | `startup_scripts/` | ✅ 要 |
| 定义修饰符 | `startup_scripts/` | ✅ 要 |
| 注册机器外壳方块 / 物品 | `startup_scripts/` | ✅ 要 |
| 写配方 | `server_scripts/` | ❌ `/nekojs reload` |
| JEI 配方说明 | `client_scripts/` | ❌ `/nekojs reload client` |

为什么结构也必须在 startup：见[首页那段说明](Home#一个必须先知道的事)。
简单讲，MMCR 在 NeoForge 的 `FMLCommonSetupEvent` 就把结构表冻住了，而 `server_scripts`
要到服务器启动之后才加载——晚了整整一个阶段。

---

## 验证装好了

进游戏后看日志，应该能找到：

```
[mmcr_nekojs] MMCR/NekoJS submitted N machine definition(s): [...]
[mmcr_nekojs] MMCR/NekoJS submitted N structure(s), N level(s), N modifier(s)
```

没有这两行说明脚本没跑或者没声明任何东西。

也可以直接开 JEI，搜你定义的机器——它在"机器"分类里应该能看到。

---

## 服务端 / 客户端都要装吗

**两边都要。** 本附属在双端都有逻辑：

- 服务端：机器定义、结构、配方注册
- 客户端：控制器外观、JEI 工作台与配方说明

只装一边会连不上或者控制器显示异常。
