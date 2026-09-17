# 更新文档（Attribute Arch / attributearch）

| 项 | 值 |
|---|---|
| 模组 | **attributearch** |
| Minecraft | **1.21.1** |
| 加载器 | **NeoForge 21.1.200** |
| 当前版本 | **1.1.0**（开发中） |
| 构建产物 | `build/libs/attributearch-NeoForge-1.21.1-1.1.0.jar` |
| 构建命令 | `gradlew build` |
| 设计文档 | `DESIGN-1.21.1.md` |

> **开发阶段说明**：当前统一使用 **1.1.0** 作为开发版本号，后续功能与修复均合入此版本，不单独拆分小版本发布。

---

## 版本历史

### v1.1.0（开发中）— 附魔强化祭坛

**日期**：2026-09-17

**变更摘要**

| 类型 | 内容 |
|------|------|
| 新方块 | `attributearch:enchanting_altar`「附魔强化祭坛」 |
| 定位 | 与属性强化祭坛同族：改**物品附魔**，非玩家属性 |
| 固定规则 | 书架 **0**；修理免费 100% 耐久；花费 **0 级**；允许宝藏/诅咒/铁砧专属；禁止不可发现附魔；**不增加**铁砧惩罚 |
| 网络 | C2S `attributearch:confirm_enchant`（附魔 ID + 等级表，服务端校验） |
| 合成 | 下界合金锭 · 附魔台 · 下界合金锭 / 紫水晶块 · **属性强化祭坛** · 紫水晶块 / 黑曜石 · 书架 · 黑曜石 |
| 创造栏 | 改名为「强化祭坛」，两方块并列 |
| 资源 | blockstate / 模型 / 三面贴图 / 战利品表 / zh_cn & en_us |
| GUI | 按 Enchanting Infuser **展示元素**：**220×185**；搜索 (67,6)；附魔列表 (30,18) 160×70、行高 18；`+`/`-`、Shift+`+` 满级、Shift+`-` 清零；附魔 (7,44)、修理 (7,66)；装备栏+背包+副手。**无**清除/一键拉满/书架/额外花费文案 |
| 附魔写入 | 对齐 Infuser `setNewEnchantments`：`transmuteCopy` 书互转 + `setEnchantments`；**选择表保留 0 级以支持减级/移除**；冲突检测忽略已清零项；markedDirty 才可附魔 |
| 盔甲槽 | `canEquip` + 头/胸/腿/靴空槽图标 + 副手盾图标；`onEquipItem` 回调 |
| GUI 交互 | 对齐 Infuser：行悬停提示（等级范围/描述/冲突/当前等级）；附魔按钮差异预览（绿新增/黄变更/灰保留/红移除）；修理按钮耐久提示；宝藏金/诅咒紫高亮；搜索 resize 保留 + 热键栏数字键；滚动条拖拽；确认附魔音效 |
| 修复/优化 | 冲突检测与服务端同源（含物品已有附魔）；失败 ActionBar 提示；音效在祭坛位置；换物/确认后清选择；附魔包上限 256；创造模式不扣经验；`select_attribute` 校验；列表/Tooltip 脏标记；删客户端死代码 |
| 指令规则 | `/attributearch add\|set\|min` **不受**配置 `maxLevel`/黑名单/白名单限制；Tab 补全同样不按配置过滤（与 GUI 不同） |
| 放置 | **对齐 Infuser**：改 `BaseEntityBlock`（无朝向）；blockstate 单变体；碰撞箱 12px；`sidedSuccess` 开 GUI；不可寻路。弃用 `HorizontalDirectionalBlock`（FACING 未注册曾导致放置异常）。贴面放置仍需潜行（Shift） |

**新增 / 重写源码**

```
enchant/EnchantingAltarHelper.java      # 过滤、冲突、固定花费、computeResult/Clear
block/EnchantingAltarBlock.java
blockentity/EnchantingAltarBlockEntity.java
menu/EnchantingAltarMenu.java
client/EnchantingAltarScreen.java
network/ConfirmEnchantPayload.java
textures/gui/enchanting_altar.png
```

**注册更新**

- `ModBlocks` / `ModItems` / `ModBlockEntities` / `ModMenus` / `ModCreativeTabs` / `ModNetwork` / `AttributeArchClient`

**自测清单**

- [ ] 创造栏取出两祭坛，对空地右键可正常放置
- [ ] 对着另一祭坛侧面：不潜行打开对方 GUI；潜行后可贴面放置
- [ ] 放置后方块朝向玩家，顶/底/侧贴图正确
- [ ] 无书架即可打开附魔 GUI 并附到满级；UI 无书架数量
- [ ] 可对书附魔生成附魔书；宝藏/诅咒可上；冲突互斥
- [ ] 花费 0；可免费满耐久修理；可改已有附魔、可移除
- [ ] 悬停列表行：显示附魔描述/等级范围；冲突附魔置灰并列出冲突
- [ ] 悬停附魔按钮：显示附魔变更预览（绿/黄/灰/红）
- [ ] 悬停修理按钮：显示耐久与免费提示
- [ ] 宝藏附魔金色、诅咒紫色显示
- [ ] 确认附魔播放附魔台音效（祭坛位置）
- [ ] 物品已有互斥附魔时，新选附魔置灰且确认失败有提示
- [ ] 换物品后附魔选择自动清空
- [ ] 创造模式属性强化不扣经验
- [ ] 不增加铁砧惩罚；与 attribute_altar 并存不冲突
- [ ] `gradlew build` 通过

---

### v1.0.0 — 属性强化祭坛基线

**变更摘要**

| 类型 | 内容 |
|------|------|
| 核心 | 方块 `attribute_altar`：右键 GUI，搜索属性、强化/降级 |
| 数据 | Attachment `enhance_levels`；三乘区 Modifier（加法 / 乘法 / 独立乘法） |
| 指令 | `/attributearch add|set|min` + 白名单 `wenxingtools_whitelist.json` |
| 网络 | `select_attribute`（C2S）、`sync_levels`（S2C） |
| 配置 | 最高等级、每级加成、经验消耗、属性黑名单等（TOML） |
| 合成 | 铁/钻石/铁 + 金/书/金 + 石×3 |

详见 `DESIGN-1.21.1.md`。

---

## 安装说明（给玩家）

1. 安装 **Minecraft 1.21.1** 与 **NeoForge 21.1.x**（推荐 21.1.200+）。
2. 将 `attributearch-NeoForge-1.21.1-1.1.0.jar` 放入 `mods` 文件夹。
3. 启动游戏后，在「强化祭坛」创造页签中可找到：
   - 属性强化祭坛
   - 附魔强化祭坛

### 附魔强化祭坛合成

```
下界合金锭  附魔台      下界合金锭
紫水晶块    属性强化祭坛  紫水晶块
黑曜石      书架        黑曜石
```

### 属性强化祭坛合成

```
铁锭  钻石  铁锭
金锭  书    金锭
石头  石头  石头
```

---

## 构建与开发

```powershell
# 编译
.\gradlew.bat compileJava

# 完整构建（产出 jar）
.\gradlew.bat build

# 客户端试运行（可选）
.\gradlew.bat runClient
```

构建产物路径：`build/libs/attributearch-NeoForge-1.21.1-1.1.0.jar`

---

## 参考

- 设计全文：`DESIGN-1.21.1.md`
- GUI 布局借鉴源：`其他/1.21.1/`（Enchanting Infuser，界面布局、UV 与交互提示参考；规则仍为本模组固定书架 0 / 花费 0）
