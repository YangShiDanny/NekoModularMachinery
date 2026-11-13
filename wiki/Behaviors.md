# 行为回调

配方式机器只管「有输入就产出」。要做出真正有意思的机器，得在 tick 和配方生命周期上插手。本附属把 MMCR 的 `TickHooks` / `RecipeHooks` 原样暴露给脚本：

```js
event.createMachine('my_mod:smelter')
  .tickBehavior(hooks => { hooks.serverTick(ctx => { /* ... */ }) })
  .recipeBehavior(hooks => {
    hooks.beforeStart(ctx => { /* ... */ })
    hooks.recipeTick(ctx => { /* ... */ })
    hooks.beforeFinish(ctx => { /* ... */ })
  })
  .register()
```

回调在 `startup_scripts` 里注册，但**实际执行在服务端 tick**。所以回调里可以读世界状态、算数值，但**不要试图在里面注册新内容**。

## 1. `tickBehavior`

接收一个 `hooks` 参数（MMCR 的 `TickHooks`），目前只有一个方法：

### `hooks.serverTick(callback)`

每个服务端 tick 调用一次（20 次/秒）。`callback` 收到 `TickContext`。

```js
.tickBehavior(hooks => {
  hooks.serverTick(ctx => {
    // 每 20 tick（1 秒）检查一次智能接口的温度
    if (ctx.isDue(20) && ctx.smartInterfaceValue('temperature').orElse(0) > 800) {
      ctx.screenText().line('过热')
    }
  })
})
```

用 `isDue(n)` 而不是自己数 tick——它基于 `gameTime`，更省心且能跨并行线程。

## 2. `recipeBehavior`

`hooks` 是 MMCR 的 `RecipeHooks`，七个回调覆盖配方完整生命周期：

| 回调 | 时机 | 收到的 Context |
|---|---|---|
| `idleStart(ctx)` | 从「有活干」进入「没活干」 | `MachineContext` |
| `idleEnd(ctx)` | 从「没活干」恢复「有活干」 | `MachineContext` |
| `beforeStart(ctx)` | 配方即将开始 | `RecipeStartContext` |
| `recipeTick(ctx)` | 配方运行中每 tick | `RecipeTickContext` |
| `beforeFinish(ctx)` | 配方即将结算 | `RecipeFinishContext` |
| `preServerTick(ctx)` | 机器服务端 tick 之前 | `MachineContext` |
| `postServerTick(ctx)` | 机器服务端 tick 之后 | `MachineContext` |

### 最有用的是 `beforeStart`

它是唯一能**改配方本身**的时机：

```js
.recipeBehavior(hooks => {
  hooks.beforeStart(ctx => {
    // 耗时减半
    ctx.setDuration(ctx.duration() / 2)

    // 或者：条件不满足就取消
    if (ctx.machineContext().upgradeItems().isEmpty()) {
      ctx.cancel()
    }
  })
})
```

`RecipeStartContext` 能做的：

| 方法 | 用途 |
|---|---|
| `duration()` / `setDuration(ticks)` | 读/改总耗时 |
| `requestedParallelism()` / `effectiveParallelism()` | 请求的并行数 / 实际生效的并行数 |
| `requirements()` / `setRequirements(list)` | 读/替换全部需求 |
| `outputs()` / `setOutputs(list)` | 读/替换全部产出 |
| `recipe()` / `recipeId()` | 当前配方 |
| `cancel()` / `cancelled()` | 取消这条配方 |
| `replaceExactItemInputCount(item, expected, replacement)` | 精确改某个输入的消耗量 |
| `machineContext()` | 拿到机器上下文（见下） |

### `MachineContext` —— 机器自身的状态

`idleStart` / `idleEnd` / `preServerTick` / `postServerTick` 直接收到它，其他 Context 通过 `machineContext()` 拿。

| 方法 | 返回 |
|---|---|
| `controllerPos()` | 控制器坐标 |
| `machineId()` | 机器 ID |
| `level()` | `ServerLevel` |
| `gameTime()` | 当前游戏时间 |
| `isDue(period)` | 是否到了指定周期 |
| `screenText()` | 控制器屏幕文本构建器 |
| `ioView()` | IO 快照（当前库存） |
| `dataStorage()` | 数据存储（可读写自定义数据） |
| `upgradeItems()` | 升级总线里的物品列表 |
| `jadeText()` | Jade/TOP 提示文本 |
| `countStructureBlocks(block)` | 结构内某方块的数量 |
| `controller()` | 控制器方块实体视图 |

### `TickContext` —— 每 tick 的额外信息

在 `MachineContext` 基础上多了：

| 方法 | 返回 |
|---|---|
| `factoryThreadCount()` | 工厂线程数 |
| `parallelism()` | 当前并行度 |
| `smartInterfaceValue(name)` | 某个智能接口的值（`Optional`） |
| `smartInterfaceValues()` | 全部智能接口值（`Map`） |
| `ioPlan()` | 本 tick 的 IO 计划，可以往里加东西 |

### `RecipeFinishContext` —— 结算时

```js
hooks.beforeFinish(ctx => {
  // 概率性丢弃产出
  if (Math.random() < 0.1) ctx.discardOutputs()

  // 或者直接取消，等于这次白干
  // ctx.cancel()
})
```

| 方法 | 用途 |
|---|---|
| `outputs()` / `setOutputs(list)` | 改产出 |
| `discardOutputs()` / `outputsDiscarded()` | 全部丢弃 |
| `cancel()` / `cancelled()` | 取消结算 |

### `RecipeTickContext` —— 运行中的每 tick

| 方法 | 返回 |
|---|---|
| `currentTick()` / `totalTick()` | 进度 |
| `parallelism()` | 并行度 |
| `requirements()` / `outputs()` | 当前需求与产出（只读视图） |
| `ioSnapshot()` | IO 快照 |

## 3. 完整例子：需要「插了升级芯片才开工」的熔炉

```js
MMCREvents.startup(event => {
  const api = event.getAPI()

  event.createMachine('my_mod:smelter')
    .recipePool('my_mod:smelter')
    .allowModifiers()
    .allowParallelism()
    .maxParallelAmount(4)
    .tickBehavior(hooks => {
      hooks.serverTick(ctx => {
        if (!ctx.isDue(10)) return
        const parallel = ctx.parallelism()
        ctx.screenText().line(parallel > 1 ? '§a并行 ' + parallel : '§7单线程')
      })
    })
    .recipeBehavior(hooks => {
      hooks.beforeStart(ctx => {
        // 升级总线里没芯片就不开工
        if (ctx.machineContext().upgradeItems().isEmpty()) {
          ctx.cancel()
        }
      })
      hooks.beforeFinish(ctx => {
        // 10% 概率提示额外产出
        if (Math.random() < 0.1) {
          ctx.machineContext().screenText().line('§e额外产出！')
        }
      })
    })
    .register()
})
```

## 4. 限制

回调里**不要**做这些：

| 不要 | 原因 |
|---|---|
| 注册机器/结构/配方 | 注册窗口早就关了，声明会被丢弃 |
| 长时间的同步 IO | 在服务端 tick 里，会卡整个服务器 |
| 依赖客户端状态 | 这些回调只在服务端跑 |

要改配方，用 `beforeStart`；要改产出，用 `beforeFinish`；要读写持久数据，用 `machineContext().dataStorage()`。

## 下一步

- [JEI 信息](JEI) —— 把这些数值展示给玩家看
- [配方](Recipes) —— 配方的数据侧
