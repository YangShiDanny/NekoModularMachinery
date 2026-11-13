# 机器

> `cn.howxu.mmcr.nekojs.builder.MachineBuilderJS`
>
> 由 `event.createMachine(id)` 返回，链式配置，最后 `.register()` 提交。

机器定义是 MMCR 收集的**第一个**东西（`FMLConstructModEvent`，通过 ServiceLoader）。它决定：控制器长什么样、行为怎么跑、能并行多少、能装什么修饰符。

**只能在 `startup_scripts` 里注册。** 改完必须重启游戏。

## 最小可用

```js
MMCREvents.startup(event => {
  event.createMachine('my_mod:smelter').register()
})
```

只写了这一句也会得到一个可放置的控制器方块（`mmcr:smelter_controller`），只是没有外观配置、没有结构、没有配方。

## 1. 身份与配方池

### `recipePool(...ids)`

声明这台机器读哪些配方池。

```js
.recipePool('my_mod:smelter')                    // 一个池
.recipePool('my_mod:tier1', 'my_mod:tier2')      // 多个池
```

- **会覆盖**默认池，不是追加。
- 省略时默认用「与机器 ID 同名的池」。
- 多个机器可以绑定同一个池——这是做机器分级（同一台机器不同等级读同一批配方）的常见手法。

### `displayNameKey(key)`

设置本地化键。惯例 `machine.<命名空间>.<名字>`。

```js
.displayNameKey('machine.my_mod.smelter')
```

对应的 lang 键：

```json
{ "machine.my_mod.smelter": "铁熔炉" }
```

## 2. 外观

### `appearance(blockId)`

机器成型前的「基础方块」外观。给一个明确方块 ID 时，控制器在被放置但未成型时表现为该方块。

```js
.appearance('minecraft:iron_block')
```

### 控制器纹理

纹理 ID 是资源路径，不含 `textures/` 前缀和 `.png` 后缀：`'my_mod:block/controller_front'` 对应 `assets/my_mod/textures/block/controller_front.png`。

| 方法 | 说明 |
|---|---|
| `controllerTextures(front, side, top, bottom)` | 六面分别指定（四参） |
| `controllerTextures(front, otherFaces)` | 正面单独，其余五面统一（两参） |
| `controllerFrontTexture(id)` | 只改正面 |
| `controllerSideTexture(id)` | 只改侧面 |
| `controllerTopTexture(id)` | 只改顶面 |
| `controllerBottomTexture(id)` | 只改底面 |

```js
.controllerTextures('my_mod:block/smelter_front', 'my_mod:block/smelter_side')
```

## 3. 其他贴图

| 方法 | 用途 |
|---|---|
| `controllerBaseTexture(id)` | 控制器底图（叠在面上的基础层） |
| `formedPortBaseTexture(id)` | 成型后接口位置的底图 |
| `controllerIdleOverlayTexture(id)` | 空闲时的叠加层（overlay） |
| `controllerActiveOverlayTexture(id)` | 运行时的叠加层 |

Idle/Active 两个 overlay 叠在控制器正面，用来做「亮灯」效果：空闲时半透明灰，运行时换成高亮版本。

## 4. 音效

| 方法 | 说明 |
|---|---|
| `runningSound(id)` | 运行中的循环音效，如 `'minecraft:block.furnace.fire_crackle'` |
| `finishSound(id)` | 完成一次配方时的音效 |

## 5. 控制器朝向

| 方法 | 效果 |
|---|---|
| `allowVerticalFacing()` | 允许控制器朝上/朝下 |
| `requireVerticalFacing()` | 强制控制器只能朝上/朝下 |
| `fullyRotationallySymmetric()` | 四个水平方向完全等价（结构旋转不变） |

三个都不调时，控制器只有四个水平朝向，且结构按朝向旋转匹配。

## 6. 修饰符 / 并行 / 工厂

| 方法 | 说明 |
|---|---|
| `allowModifiers()` | 允许结构里插修饰符槽 |
| `allowMultithreading()` | 允许多线程处理配方 |
| `allowParallelism()` | 允许并行（一次处理多份） |
| `maxParallelAmount(n)` | 并行上限，默认 **1** |
| `maxParallelism(n)` | 上面方法的别名 |
| `factoryThreads(n)` | 启用工厂控制器并设定线程数 |

注意 `allowParallelism()` 只是**开开关**，实际能并行多少由 `maxParallelAmount` 和配方/修饰符共同决定。只调 `allowParallelism()` 而不调 `maxParallelAmount` 时，上限仍是 1。

```js
event.createMachine('my_mod:smelter')
  .allowParallelism()
  .maxParallelAmount(16)
  .allowMultithreading()
  .register()
```

## 7. 网络接口

### `networkInterface(maxCount, maxConnections)`

在控制器上内建网络接口（用于自动 IO）。

```js
.networkInterface(4, 16)   // 最多 4 个接口，每个最多 16 条连接
```

不调这个方法时不创建内建接口。

## 8. 行为回调

这是让机器「不只是跑配方」的地方。见 [行为回调](Behaviors) 了解 Context 提供的全部方法。

```js
event.createMachine('my_mod:smelter')
  .tickBehavior(hooks => {
    hooks.serverTick(ctx => {
      // 每个服务端 tick 都跑；ctx 是 TickContext
      if (ctx.isDue(20) && ctx.smartInterfaceValue('temperature').orElse(0) > 100) {
        ctx.screenText().line('过热！')
      }
    })
  })
  .recipeBehavior(hooks => {
    hooks.idleStart(ctx => { /* 从空闲转入运行 */ })
    hooks.beforeStart(ctx => { /* 可以改耗时、输出、取消 */ })
    hooks.recipeTick(ctx => { /* 每 tick */ })
    hooks.beforeFinish(ctx => { /* 可以丢弃产出、取消 */ })
    hooks.idleEnd(ctx => { /* 回到空闲 */ })
  })
  .register()
```

## 9. `register()`

提交声明。返回 `this`，所以可以链在最后。

```js
.register()
```

调用后：

- 机器进入 `DeclarationStore`
- `STARTUP` 脚本跑完后，bridge 重开 MMCR 的定义窗口，通过 ServiceLoader 把整批声明交给 MMCR
  （见 [生命周期与时机](Getting-Started)）
- 控制器方块在注册阶段生成，ID 固定为 `mmcr:<机器名>_controller`

**不要**重复注册同一个 ID，MMCR 会报重复。

## 完整例子

```js
MMCREvents.startup(event => {
  event.createMachine('my_mod:smelter')
    .recipePool('my_mod:smelter')
    .displayNameKey('machine.my_mod.smelter')
    .appearance('minecraft:iron_block')
    .controllerTextures('my_mod:block/smelter_front', 'my_mod:block/smelter_side')
    .controllerIdleOverlayTexture('my_mod:block/smelter_idle')
    .controllerActiveOverlayTexture('my_mod:block/smelter_active')
    .runningSound('minecraft:block.furnace.fire_crackle')
    .finishSound('minecraft:entity.player.levelup')
    .allowModifiers()
    .allowParallelism()
    .maxParallelAmount(8)
    .register()
})
```

## 下一步

- [结构](Structures) —— 给机器一个形状
- [行为回调](Behaviors) —— 自定义 tick 与配方生命周期
