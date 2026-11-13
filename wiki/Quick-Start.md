# 快速开始

从零做一个能用的机器：**铁熔炉**——3×3×1 的外壳，中间放控制器，消耗铁矿和能量产出铁锭。

---

## 1. 建目录

```
.minecraft/nekojs/
├── startup_scripts/
│   └── smelter.js
├── server_scripts/
│   └── smelter_recipes.js
└── assets/
    └── my_pack/lang/zh_cn.json
```

---

## 2. 定义机器和结构

`startup_scripts/smelter.js`：

```js
MMCREvents.startup(event => {
  const api = event.getAPI()

  // ---- 机器 ----
  event.createMachine('my_pack:smelter')
    .displayNameKey('machine.my_pack.smelter')
    .appearance('minecraft:iron_block')   // 外壳方块
    .recipePool('my_pack:smelter')        // 这个机器读哪个配方池
    .register()

  // ---- 结构 ----
  // 一次 pattern() = 一个 z 层（按调用顺序从 z 小到大）
  // 每个字符串 = 一层里的一行；第一个字符串是 y 最小（最底层）
  // 每个字符 = 一格，从左（x 小）到右
  event.createStructure('my_pack:smelter')
    .pattern('BBB')     // z = 0 层（3×1 的一个横排）
    .pattern('BCB')     // z = 1 层，中间是控制器
    .pattern('BBB')     // z = 2 层
    .set('B', api.block('minecraft:iron_block'))
    .set('C', api.block('minecraft:iron_block'))   // 占位，会被替换成控制器方块
    .controller('C')
    .build()
})
```

三个必须先搞明白的点：

- **一次 `pattern()` 是一个水平层（z）**，不是一个竖直面。想要 3×3 的地板，就调三次，
  每次传一行 3 个字符；不是写三次行。
- **字符串长度必须一致**。混进 4 个字符的行，整个结构会错位。
- **空格 `' '` 表示"这一格不检查"**，不是"必须是空气"。真要要求空气就 `.set('#', api.air())`。

---

## 3. 写配方

`server_scripts/smelter_recipes.js`：

```js
const api = MMCR.getAPI()

ServerEvents.recipes(event => {
  event.recipes.mmcr.machine_recipe('my_pack:smelter', 100, [
    api.itemInputRequirement('minecraft:iron_ore', 1),
    api.energyRequirement('input', 20),
    api.itemOutputRequirement('minecraft:iron_ingot', 1, 1.0)
  ]).id('my_pack:iron_smelting')
})
```

参数依次是：**配方池 ID**、**加工时间（tick，20 = 1 秒）**、**需求数组**。

配方改完 `/nekojs reload` 就生效，不用重启。

---

## 4. 加个显示名（可选）

`assets/my_pack/lang/zh_cn.json`：

```json
{
  "machine.my_pack.smelter": "铁熔炉"
}
```

`assets/my_pack/lang/en_us.json`：

```json
{
  "machine.my_pack.smelter": "Iron Smelter"
}
```

不写也能跑，界面上会显示原始的 key。

---

## 5. 重启，验证

启动日志里应该出现：

```
[mmcr_nekojs] MMCR/NekoJS submitted 1 machine definition(s): [my_pack:smelter]
[mmcr_nekojs] MMCR/NekoJS submitted 1 structure(s), 0 level(s), 0 modifier(s)
```

然后：

1. 拿控制器物品（JEI 里搜"铁熔炉"，或者自己做个配方给它）
2. 按 `pattern()` 在地上铺 3×3 的铁块（共 9 格），控制器会占中间那格
3. 右键控制器成型
4. 在结构表面装上物品/能量端口

如果不成型，看[常见错误](Troubleshooting)。

---

## 6. 加一个等级（可选）

让机器认"线圈等级"：放铁块是一级，放钻石块是更高级。

```js
MMCREvents.startup(event => {
  const api = event.getAPI()

  event.createLevelType('my_pack:coil')
    .displayNameKey('level.my_pack.coil')
    .register()

  event.createLevel('my_pack:coil_basic')
    .type('my_pack:coil')
    .state('minecraft:iron_block')
    .priority(0)
    .register()

  event.createLevel('my_pack:coil_elite')
    .type('my_pack:coil')
    .state('minecraft:diamond_block')
    .priority(10)
    .modifier(api.modifierDefinition([
      api.modifier('duration', 'recipe', 0.7, 'multiply', false)   // 快 30%
    ]))
    .register()

  // 结构里用 L 引用这个等级类型
  event.createStructure('my_pack:smelter')
    .pattern('BBB')
    .pattern('CLC')                            // C = 控制器占位，L = 等级槽
    .pattern('BBB')
    .set('B', api.block('minecraft:iron_block'))
    .set('C', api.block('minecraft:iron_block'))
    .set('L', event.levelSlot('my_pack:coil'))   // ← 等级槽
    .controller('C')
    .build()
})
```

`priority` 大的优先匹配。装配饰符和等级细节见[等级与修饰符](Levels-and-Modifiers)。

---

## 下一步

| 想做的事 | 看这页 |
|---|---|
| 改外观、纹理、音效、并行数 | [机器](Machines) |
| 复杂结构、端口数量/等级要求 | [结构](Structures) |
| 概率产出、流体、能量、修饰符 | [配方](Recipes) |
| 从 KubeJS 脚本搬过来 | [从 KubeJS 迁移](KubeJS-Migration) |
