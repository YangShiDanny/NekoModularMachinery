# JEI 信息

本附属对 JEI 的支持分两部分：

- **配方页本身** —— MMCR 自动生成，不需要你做什么。装了 JEI 就能看到 `mmcr:machine_recipe` 的配方，也能预览结构。
- **附加说明文本** —— 需要你在 `client_scripts` 里写。

## 1. 配方池说明

给某个配方池的整页加一段说明（显示在配方页下方）。

```js
// nekojs/client_scripts/jei_info.js
MMCREvents.client(event => {
  event.addRecipePoolInfo('my_mod:smelter', 'jei.my_mod.smelter.info')
})
```

### `addRecipePoolInfo(poolId, translationKey, ...args)`

| 参数 | 说明 |
|---|---|
| `poolId` | 配方池 ID（不是机器 ID） |
| `translationKey` | 语言文件里的键 |
| `...args` | 可选，作为 `Component.translatable` 的参数传下去 |

lang 文件：

```json
{
  "jei.my_mod.smelter.info": "需要 10 FE/t。装上超频芯片后耗时减半。",
  "jei.my_mod.smelter.info.with_param": "本配方需要 %s 个线圈。"
}
```

带参数：

```js
event.addRecipePoolInfo('my_mod:smelter', 'jei.my_mod.smelter.info.with_param', 3)
```

## 2. 单条配方说明

只给某一具体配方加说明。

```js
MMCREvents.client(event => {
  event.addRecipeInfo('my_mod:smelter_iron', 'jei.my_mod.smelter_iron.info')
})
```

### `addRecipeInfo(recipeId, translationKey, ...args)`

参数同上一节，第一个是**配方 ID**（就是你在 `ServerEvents.recipes` 里 `.id(...)` 设的那串）。

## 3. 工作台图标（可选）

让 JEI 的配方页显示一个「工作台」图标，玩家一眼知道该做什么机器。

```js
MMCREvents.startup(event => {
  event.addRecipePoolWorkstation('my_mod:smelter', 'my_mod:smelter_chip')
})
```

### `addRecipePoolWorkstation(poolId, itemId)`

| 参数 | 说明 |
|---|---|
| `poolId` | 配方池 ID |
| `itemId` | 用作图标的物品 ID |

这个必须写在 `startup_scripts`——它走的是 MMCR 的 JEI workstation 注册事件，在启动期处理。

还有 `addMachineWorkstation(machineId, recipeTypeId)` 用于按机器挂配方类型，用法类似。

## 4. 没装 JEI 会怎样

所有 `MMCREvents.client` 里的调用都会被忽略，日志里可能有一条 warn，但**不会崩**。所以这些脚本可以无条件写在包里。

同样，`addRecipePoolWorkstation` 也只在有 JEI 时生效。

## 5. 什么时候生效

客户端脚本可以 `/nekojs reload client` 热重载（仅单人/集成服务器）。改了 lang 文件用 `F3+T` 重载资源。

注意 JEI 的配方列表本身是从服务端同步过来的，改**配方**要 `/nekojs reload`（服务端）；改**说明文本**只要重载客户端。

## 排查

| 现象 | 原因 |
|---|---|
| JEI 里看不到我的机器配方 | 配方池 ID 写错；或配方脚本报错（`/nekojs error`） |
| 有配方但没有说明文字 | `MMCREvents.client` 写在了 `server_scripts` 里——必须在 `client_scripts` |
| 说明文字显示为原始键名 | lang 文件里没有对应键，或 namespace 不对 |
| JEI 里没有这台机器的分类 | 机器没注册成功，看启动日志的 `startup declarations` 数量 |

## 下一步

- [示例](Examples) —— 完整的机器 + 结构 + 配方 + JEI 组合
- [排错](Troubleshooting)
