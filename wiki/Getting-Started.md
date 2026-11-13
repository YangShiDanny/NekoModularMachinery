# 生命周期与时机

MMCR 不是「随时可以注册」的 API。它在启动流程里只开三个窗口，每个窗口收一类东西，收完就冻结。**写错窗口的声明不会报错，只会被丢掉**——这是 MMCR 生态里最常见的踩坑方式。

本页把这三个窗口、NekoJS 的执行位置、以及由此产生的硬约束一次讲清。

## 1. 三个注册窗口

| # | 收什么 | 什么时候 | 通过什么 |
|---|---|---|---|
| 1 | 机器定义（控制器、行为、外观、配方池） | `FMLConstructModEvent` | `ServiceLoader` 调 `MachineDefinitionProvider` |
| 2 | 结构、等级、等级类型、修饰符 | `FMLCommonSetupEvent` | `NeoForge.EVENT_BUS` 上的 `RegisterMachineStructuresEvent`，post 完立刻 `freeze` + `commit` |
| 3 | 静态配方（程序化构建的） | `ModifyDefaultComponentsEvent` | `RegisterMachineRecipesEvent` |

三个窗口都在**主菜单出现之前**。它们不是「游戏内事件」，是 mod 加载期事件。

数据驱动的 `mmcr:machine_recipe` JSON 不受这个限制——它是标准配方类型，跟着数据包走，`/reload` 就能换。

## 2. NekoJS 站在哪一格

关键事实一：**NekoJS 在自己的 mod 构造器里就同步跑完了所有 `STARTUP` 脚本**。

关键事实二（容易搞反）：**`FMLConstructModEvent` 不是"所有 mod 都构造完之后统一发一次"**。NeoForge 的 `ModLoader.lambda$constructMods$0` 对每个 mod 做的是：

```java
container.constructMod();                              // 这个 mod 的构造器
container.acceptEvent(new FMLConstructModEvent(...));   // 紧接着就给它发事件
```

也就是说，每个 mod 的 `FMLConstructModEvent` 在**它自己构造完的那一瞬间**就发了，跑在构造它的同一个并行 worker 里。而 mod 构造顺序按加载顺序，`mmcr` 排在 `nekojs` 前面：

```
modloading-worker-0:  mmcr.构造器 → mmcr 的 FMLConstructModEvent   ← 窗口 1 在这里关闭（此时还没机器）
                      mmcr_nekojs.构造器
                      nekojs.构造器 → STARTUP 脚本执行完毕          ← 声明此时才出现在 bridge 里
FMLCommonSetupEvent                                                 ← 窗口 2：结构/等级/修饰符
ModifyDefaultComponentsEvent                                        ← 窗口 3：静态配方
服务器启动 → SERVER 脚本加载 → 世界可进
```

按老假设（"事件在全部构造完之后发"）写出来的桥接，窗口 1 永远收不到声明，然后窗口 2 会拿着一个不存在的机器去注册结构，MMCR 直接抛 `Unknown machine definition` 把游戏打崩。

**本附属怎么处理**：`STARTUP` 脚本一跑完（NekoJS 的 `afterScriptsLoaded`），bridge 立刻调
`StartupContentRegistration.resetForTesting()` 把 MMCR 的启动阶段退回 `NOT_STARTED`，再跑一次
`registerProductionForModStartup(mmcr 的 mod 总线)`。这时离窗口 2 还早、什么都没 commit，所以这次重放就是真正生效的那一次收集，`ServiceLoader` 拿到的正是填满的声明。日志里能看到：

```
MMCR/NekoJS submitted N machine definition(s) through a reopened window: [...]
```

想关掉这层兼容：JVM 参数 `-Dmmcrnekojs.disableStartupReplay=true`（关掉后脚本机器不会存在，只会在窗口 2 里被跳过并报错）。

所以 `MMCREvents.startup` 里的声明，**三个窗口都能用**。

而 `SERVER` 脚本加载在最后，**三个窗口全都关完了**。这就是本附属和 KubeJS 版最大的分歧点：

- KubeJS 版把结构放 `server_scripts` 还能用，是因为 MMCR 给 KubeJS 单独写了一套内容重载事务（`KubeJSContentReloadTransaction`），在 reload 时重新走一遍注册。
- 本附属没有这套事务（NekoJS 的脚本模型是「事务式替换 Context」，和 MMCR 的内容注册表不是同一套生命周期），所以**只能用启动窗口**。

## 3. 由此得出的硬规则

| 内容 | 写在 | 改完怎么生效 |
|---|---|---|
| 机器定义 | `startup_scripts` | 重启 |
| 结构 | `startup_scripts` | 重启 |
| 等级 / 等级类型 | `startup_scripts` | 重启 |
| 修饰符 / 修饰符物品 | `startup_scripts` | 重启 |
| 配方池 workstation（JEI） | `startup_scripts` | 重启 |
| 控制屏文本 | `startup_scripts` | 重启 |
| **配方**（`mmcr:machine_recipe` JSON） | `server_scripts` 或数据包 | `/nekojs reload` 或 `/reload` |
| JEI 配方说明 | `client_scripts` | `F3+T` 或重开客户端 |

一句话：**`startup_scripts` 管「注册」，`server_scripts` 只管「配方数据」。**

> 配方为什么反而是唯一能热重载的东西？因为 `mmcr:machine_recipe` 是标准配方类型，走 Minecraft 的 `RecipeManager`，不经过 MMCR 的启动期注册表。这也是本附属不提供程序化配方 builder 的原因——见 [配方](Recipes)。

## 4. `/nekojs reload startup` 能做什么

NekoJS 允许重跑启动脚本，但 Minecraft 的注册表在游戏启动后就冻结了。所以：

- ✅ 脚本里的语法错误、拼写错误可以靠 reload 快速迭代
- ✅ 日志会重新打印一遍声明数量
- ❌ 新增/修改的机器、结构、等级、修饰符、静态配方**不会生效**

想真正看到结构改动，只能重启游戏。这就是为什么建议**先离线把结构调对再进游戏**——见 [导出结构](Exporting-Structures)。

## 5. 命名空间与 ID 的三个层次

新手最容易混的是这三者不是一回事：

| 东西 | 例子 | 谁生成 |
|---|---|---|
| 机器 ID | `my_mod:smelter` | 你，`createMachine` 的参数 |
| 配方池 ID | `my_mod:smelter` | 你，`.recipePool(...)` 声明，可以和机器 ID 不同，也能多个机器共用一个池 |
| 控制器方块 ID | `mmcr:smelter_controller` | **MMCR 自动生成**，固定在 `mmcr` 命名空间下 |

翻译键因此也分两套：

```json
{
  "machine.my_mod.smelter": "铁熔炉",
  "recipe_pool.my_mod.smelter": "铁熔炉",
  "item.mmcr.smelter_controller": "铁熔炉 控制器",
  "block.mmcr.smelter_controller": "铁熔炉 控制器"
}
```

注意控制器那条的命名空间是 `mmcr`，不是你的 mod。

## 6. 想少踩坑，就记住这张顺序图

```
声明机器  →  声明结构（引用机器 ID）  →  声明配方（引用配方池 ID）
```

三者都写在**同一个** `startup_scripts` 脚本里也没问题，顺序也不重要（它们只是往 `DeclarationStore` 里存数据，MMCR 按自己的窗口来取）。但**结构里的 `.controller(...)` 指向的机器必须注册过**，否则成型时找不到控制器。

## 下一步

- [快速开始](Quick-Start) —— 按这个顺序走一遍完整流程
- [结构](Structures) —— 坐标语义、端口、等级槽
- [配方](Recipes) —— 静态配方 vs 数据配方
