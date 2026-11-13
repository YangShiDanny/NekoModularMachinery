# 机器

```js
MMCREvents.startup(event => {
  event.createMachine('my_pack:smelter')
    // ...链式调用...
    .register()
})
```

所有方法都返回 builder 本身，可以随意链式。**最后必须调 `.register()`**，否则什么都不会发生。

---

## 基础

| 方法 | 说明 |
|---|---|
| `displayNameKey(key)` | 语言键。不写默认 `machine.<命名空间>.<路径>` |
| `recipePool(...ids)` | 这个机器读哪些配方池。**默认只有一个：机器自己的 ID** |
| `appearance(blockId)` | 外壳方块。结构里除控制器和端口外的格子就是它 |
| `register()` | 提交 |

`recipePool` 值得多说一句：默认池就是机器 ID，所以 `createMachine('my_pack:smelter')` 会自动读
`my_pack:smelter` 池，不用显式写。想复用别的池才需要调。

---

## 外观

| 方法 | 说明 |
|---|---|
| `controllerTextures(front, side, top, bottom)` | 控制器四面对应纹理 |
| `controllerTextures(front, other)` | 正面单独，其余三面相同 |
| `controllerFrontTexture(id)` | 只改正面 |
| `controllerSideTexture(id)` | 只改侧面 |
| `controllerTopTexture(id)` | 只改顶面 |
| `controllerBottomTexture(id)` | 只改底面 |
| `controllerBaseTexture(id)` | 控制器底座 |
| `formedPortBaseTexture(id)` | 成型后端口底座 |
| `controllerIdleOverlayTexture(id)` | 空闲时叠加层 |
| `controllerActiveOverlayTexture(id)` | 运行时叠加层 |
| `runningSound(id)` / `finishSound(id)` | 运行中 / 完成音效 |

纹理参数是**资源路径**，不是方块 ID。比如 `controllerFrontTexture('my_pack:block/smelter_front')`
对应文件 `assets/my_pack/textures/block/smelter_front.png`。

```js
event.createMachine('my_pack:smelter')
  .appearance('my_pack:casing')
  .controllerTextures(
    'my_pack:block/smelter_front',    // 正面
    'my_pack:block/smelter_side',     // 其余三面
    'my_pack:block/smelter_side',
    'my_pack:block/smelter_side'
  )
  .controllerIdleOverlayTexture('my_pack:block/smelter_idle')
  .controllerActiveOverlayTexture('my_pack:block/smelter_active')
  .runningSound('minecraft:block.furnace.fire_crackle')
  .finishSound('minecraft:entity.player.levelup')
  .register()
```

---

## 朝向

| 方法 | 说明 |
|---|---|
| `allowVerticalFacing()` | 允许控制器上下朝向 |
| `requireVerticalFacing()` | 强制上下朝向（只能上下） |
| `fullyRotationallySymmetric()` | 四向完全对称，摆的时候不用管方向 |

---

## 并行与工厂

| 方法 | 说明 |
|---|---|
| `allowModifiers()` | 允许装**修饰符**（`registerModifier` 定义的那些） |
| `allowMultithreading()` | 允许多线程 |
| `allowParallelism()` | 允许**并行**（同时处理多份配方） |
| `maxParallelAmount(n)` | 并行上限 |
| `maxParallelism(n)` | 同上，别名 |
| `factoryThreads(n)` | 开启工厂模式并设定线程数，`n > 0` 才生效 |
| `networkInterface(maxCount, maxConnections)` | 网络接口上限，`maxCount >= 0` 才发 |

**并行**（同一台机器同时跑多份相同配方）和**多线程**（工厂内多个线程跑不同配方）是两回事，
别混。想要工厂模式就调 `factoryThreads()`。

```js
event.createMachine('my_pack:mass_smelter')
  .appearance('my_pack:casing')
  .allowParallelism()
  .maxParallelAmount(16)
  .allowMultithreading()
  .factoryThreads(4)
  .allowModifiers()
  .register()
```

---

## 行为钩子

想在机器运行的不同时刻做点什么（发消息、改数据、放粒子）时用这两个。

### `tickBehavior(hooks => ...)`

```js
.tickBehavior(hooks => {
  hooks.serverTick(ctx => {
    // 每 tick 调一次
    // ctx.pos / ctx.level / ctx.machine ...
  })
})
```

### `recipeBehavior(hooks => ...)`

| 钩子 | 什么时候 |
|---|---|
| `hooks.idleStart(cb)` | 从"有活干"变成"没活干" |
| `hooks.idleEnd(cb)` | 从"没活干"变成"有活干" |
| `hooks.beforeStart(cb)` | 一份配方开始加工前 |
| `hooks.recipeTick(cb)` | 加工中每 tick |
| `hooks.beforeFinish(cb)` | 一份配方完成前 |
| `hooks.preServerTick(cb)` | 每 tick 最前面 |
| `hooks.postServerTick(cb)` | 每 tick 最后面 |

```js
.recipeBehavior(hooks => {
  hooks.beforeFinish(ctx => {
    console.info('配方完成 @ ' + ctx.pos)
  })
  hooks.idleStart(ctx => {
    console.info('没活干了')
  })
})
```

> ⚠️ 这两个钩子依赖 NekoJS 把 JS 函数转成 Java `Consumer`。如果你的 NekoJS 版本不支持
> 宿主 `@FunctionalInterface` 桥接，调用时会抛类型错误。遇到这种情况改用 Java 侧扩展。

---

## 完整示例

```js
MMCREvents.startup(event => {
  const api = event.getAPI()

  event.createMachine('my_pack:blast_furnace')
    .displayNameKey('machine.my_pack.blast_furnace')
    .appearance('my_pack:machine_casing')
    .recipePool('my_pack:blast_furnace', 'my_pack:shared_smelting')
    .controllerTextures('my_pack:block/bf_front', 'my_pack:block/bf_side')
    .controllerActiveOverlayTexture('my_pack:block/bf_active')
    .runningSound('minecraft:block.blastfurnace.fire_crackle')
    .allowVerticalFacing()
    .allowModifiers()
    .allowParallelism()
    .maxParallelAmount(8)
    .factoryThreads(2)
    .recipeBehavior(hooks => {
      hooks.beforeFinish(ctx => console.info('烧完了 @ ' + ctx.pos))
    })
    .register()
})
```

---

## 常见疑问

**问：控制器方块怎么来的？**
MMCR 会给每个 `createMachine` 自动生成一个控制器方块，ID 就是你的机器 ID。
本附属不单独提供"设置控制器方块"的方法。

**问：机器为什么不显示？**
检查 `appearance()` 指向的方块是否真实存在。用了自定义方块就得在
`startup_scripts` 里先用 `RegistryEvents` 注册它。

**问：改了 `maxParallelAmount` 没效果？**
必须先调 `allowParallelism()`。上限方法只是设值，开关在那个。

**问：`factoryThreads` 和 `maxParallelAmount` 能一起用吗？**
可以，但语义不同，见上面"并行与工厂"。
