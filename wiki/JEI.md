# JEI 集成

没装 JEI 时这些调用会被安全忽略，不影响游戏启动。

---

## 让机器出现在 JEI 里

JEI 需要知道"哪种物品代表这台机器"，以及"这台机器能做什么配方"。

```js
MMCREvents.startup(event => {
  // 配方池对应的工作站物品（池里的配方会显示在这个物品下）
  event.addRecipePoolWorkstation('my_pack:smelter', 'minecraft:blast_furnace')

  // 机器对应哪个配方类型
  event.addMachineWorkstation('my_pack:smelter', 'minecraft:smelting')
})
```

| 方法 | 作用 |
|---|---|
| `addRecipePoolWorkstation(poolId, itemId)` | 「这个物品能执行这个池的配方」 |
| `addMachineWorkstation(machineId, recipeTypeId)` | 「这台机器用这个配方类型」 |

两个参数都是 **ID 字符串**。别名 `addMachineWorkStation`（大写 S）也能用。

> 一般你只需要 `addRecipePoolWorkstation`。它决定 JEI 在哪个物品页面下显示你的配方。

---

## 给配方加说明文字

写在 `client_scripts/`：

```js
MMCREvents.client(event => {
  // 池级说明：这个池里所有配方都会显示
  event.addRecipePoolInfo('my_pack:smelter', 'jei.my_pack.smelter.info', 200)

  // 单配方说明
  event.addRecipeInfo('my_pack:iron_smelting', 'jei.my_pack.iron_smelting.info')
})
```

| 方法 | 作用 |
|---|---|
| `addRecipePoolInfo(poolId, key, ...args)` | 池级 |
| `addRecipeInfo(recipeId, key, ...args)` | 单配方级 |

`...args` 会作为格式化参数塞进 `Component.translatable(key, args...)`，所以语言文件里可以写 `%s`：

```json
{
  "jei.my_pack.smelter.info": "工作时长：%s tick",
  "jei.my_pack.iron_smelting.info": "需要线圈等级：铜"
}
```

改完 `/nekojs reload client` 生效。

---

## 完整示例

```js
// startup_scripts/jei.js —— 工作站注册必须在这
MMCREvents.startup(event => {
  event.addRecipePoolWorkstation('my_pack:smelter', 'my_pack:smelter_controller')
  event.addRecipePoolWorkstation('my_pack:smelter', 'minecraft:furnace')
})

// client_scripts/jei_info.js —— 说明文字
MMCREvents.client(event => {
  event.addRecipePoolInfo('my_pack:smelter', 'jei.my_pack.smelter.info', 100)
  event.addRecipeInfo('my_pack:iron_smelting', 'jei.my_pack.iron_smelting.special')
})
```

`assets/my_pack/lang/zh_cn.json`：

```json
{
  "jei.my_pack.smelter.info": "基础加工时间 %s tick",
  "jei.my_pack.iron_smelting.special": "需要铜级线圈"
}
```

---

## 常见问题

**问：机器在 JEI 里搜不到。**
控制器物品本身要有语言条目（`machine.<ns>.<path>`），否则显示成原始 key 很难搜。
另外确认 `addRecipePoolWorkstation` 的池名和机器 `recipePool()` 一致。

**问：配方显示了但描述是空白。**
`addRecipeInfo` 的 ID 要等于配方的 `.id(...)`，不是池名。

**问：装了 JEI 但客户端崩溃。**
检查是不是在 `startup_scripts` 里调了 `MMCREvents.client` —— 那个事件只在客户端加载阶段触发，
而且必须有监听者才会 post。把客户端相关的放 `client_scripts`。
