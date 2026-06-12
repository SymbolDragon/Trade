# Trade - Minecraft 交易插件

一个功能强大的 Minecraft 1.21.x Paper 服务器交易插件，支持自定义物品兑换、NBT 数据完整保留、在线编辑等功能。

## ✨ 核心特性

### 1. **完整的 NBT 数据支持**
- ✅ 完美保留 MythicMobs、MMOItems、ItemsAdder 等插件的自定义物品
- ✅ Base64 编码存储完整的 NBT 数据
- ✅ 交易时严格验证 NBT 数据，防止作弊
- ✅ 向后兼容普通物品（无 NBT）

### 2. **智能物品验证系统**
- ✅ 精确对比材质、数量、NBT、附魔、Lore 等所有属性
- ✅ 防止用普通物品替代特殊物品
- ✅ 支持多个相同物品的累加计算
- ✅ 高效的物品匹配算法

### 3. **自定义界面和提示**
- ✅ 每个配方可以自定义交易界面标题
- ✅ 每个配方可以自定义交易成功提示
- ✅ 支持 Minecraft 颜色代码
- ✅ 默认值自动应用，减少配置负担

### 4. **灵活的刷新系统**
- ✅ 无限制（NONE）
- ✅ 每天 0 点刷新（DAILY）
- ✅ 每周一 0 点刷新（WEEKLY）
- ✅ 每月 1 号 0 点刷新（MONTHLY）
- ✅ 自定义间隔（CUSTOM）

### 5. **在线编辑器 GUI**
- ✅ 直观的图形界面编辑配方
- ✅ 实时预览输入输出物品
- ✅ 一键切换编辑侧
- ✅ 可视化刷新类型设置
- ✅ 无需手动编辑配置文件

### 6. **可靠的数据管理**
- ✅ YAML 格式存储，易于手动修改
- ✅ LinkedHashMap 保持配方顺序
- ✅ 安全的 Reload 机制
- ✅ 自动备份和错误恢复

### 7. **兑换次数限制系统** ✨ 新增
- ✅ 每个配方可设置最大兑换次数
- ✅ 支持随时间刷新重置次数（每日/每周/每月）
- ✅ 也支持永久累计不重置（限量物品）
- ✅ 实时显示剩余次数和总次数
- ✅ 次数用完时自动禁用交易按钮

### 8. **智能编辑器增强** ✨ 新增
- ✅ 第50格：设置最大兑换次数（左键+1，右键-1）
- ✅ 第51格：切换次数重置方式（随刷新/永久）
- ✅ 防止关闭时按钮物品返回背包
- ✅ 右侧物品槽位正确保存（20个槽位）

## 📋 命令说明

### 玩家命令
- `/trade` 或 `/t` - 打开交易列表
- `/trade open <配方ID>` - 打开指定配方的交易界面
- `/trade help` - 显示帮助信息

### 管理员命令（需要 `trade.admin` 权限）
- `/trade edit [配方ID]` - 编辑现有配方或创建新配方
- `/trade list` - 列出所有配方
- `/trade reload` - 重新加载所有配置

### 命令别名
- `/t` 
- `/exchange`

## 🔐 权限说明

|     权限      |           描述          |        默认       |
|---------------|-------------------------|-------------------|
|  `trade.use`  |      允许使用交易命令    | 所有玩家 (true)   |
| `trade.admin` | 管理员权限，可以编辑配方 | OP (op)           |

## 🎮 GUI编辑器使用说明

### 打开编辑器
```
/trade edit          # 创建新配方
/trade edit <ID>     # 编辑现有配方
```

### 编辑步骤
1. **放置输入物品**：
   - 点击“切换侧”按钮切换到“左侧(输入)”模式
   - 从背包中将物品放入左侧区域（最多20个槽位）
   
2. **放置输出物品**：
   - 点击“切换侧”按钮切换到“右侧(输出)”模式
   - 从背包中将物品放入右侧区域（最多22个槽位）
   
3. **设置刷新类型**：
   - 右键点击“刷新设置”按钮循环切换：
     - 无限制 → 每天0点 → 每周一0点 → 每月1号0点 → 自定义间隔
   
4. **设置自定义标题和提示** ✨ 新增：
   - 点击第48格按钮设置交易界面标题
   - 点击第52格按钮设置交易成功提示
   - 右键重置为默认值
   
5. **设置兑换次数限制** ⭐ 新增：
   - 点击第50格按钮设置最大兑换次数
     - 左键点击：增加1次（从0开始时设为10）
     - Shift+左键：大幅增加10次
     - 右键点击：减少1次
     - Shift+右键：设置为无限制（0）
   - 点击第51格按钮切换次数重置方式
     - CLOCK（时钟）= 随刷新重置
     - BARRIER（屏障）= 永久累计
   
6. **保存配方**：
   - 点击“确认保存”按钮（第45格）
   - 配方自动保存到 `plugins/Trade/recipes/<ID>.yml`

### 注意事项
- ⚠️ 输入和输出区域都至少需要一个物品
- ⚠️ 中间的分界线不可放置物品
- ⚠️ 关闭编辑器时，GUI中的物品会返回背包
- ⚠️ 切换编辑侧时，请先放置好当前侧的物品
- ✅ 支持拖动、Shift+点击等所有物品操作
- ✅ 完全保留 NBT、附魔、Lore 等所有属性

## 📁 配置文件结构

### 配方文件位置
```
plugins/Trade/recipes/<配方ID>.yml
```

### 配方文件格式（完整版）
```yaml
# 配方ID（必须与文件名一致，不含 .yml）
id: mythic_weapon_trade

# 自定义交易界面标题（可选，默认 "§6交易系统"）
trade_title: "§e§l武器商店"

# 自定义交易成功提示（可选，默认 "§a交易成功！"）
trade_success_message: "§6购买成功！获得传说武器"

# 输入物品（左侧）
input:
  '0':
    material: GOLD_INGOT
    amount: 64
    # 完整的 NBT 数据（Base64 编码，自动生成）
    nbt_data: "H4sIAAAAAAAA..."
    
# 输出物品（右侧）
output:
  '0':
    material: DIAMOND_SWORD
    amount: 1
    display_name: "§6传说之剑"
    lore:
      - "§7攻击力: +100"
      - "§e稀有度: 传说"
    enchantments:
      sharpness: 5
      unbreaking: 3
    unbreakable: true
    custom_model_data: "1001"
    # NBT 数据会自动保存
    nbt_data: "H4sIAAAAAAAA..."

# 刷新设置
refresh:
  type: DAILY  # NONE, DAILY, WEEKLY, MONTHLY, CUSTOM
  interval: 3600  # 仅在 type=CUSTOM 时使用，单位：秒
```

### 字段说明

#### 必填字段
- `id`: 配方ID（必须与文件名一致）
- `input`: 输入物品列表
- `output`: 输出物品列表
- `refresh.type`: 刷新类型

#### 可选字段
- `trade_title`: 交易界面标题（默认 "§6交易系统"）
- `trade_success_message`: 交易成功提示（默认 "§a交易成功！"）
- `refresh.interval`: 自定义刷新间隔（仅 CUSTOM 类型需要）
- `max_trade_count`: 最大兑换次数（0表示无限制，默认0）⭐ 新增
- `count_resets_with_refresh`: 次数是否随刷新重置（默认true）⭐ 新增

#### 物品属性
- `material`: 材质（必填）
- `amount`: 数量（必填）
- `nbt_data`: 完整 NBT 数据（Base64，自动保存）
- `display_name`: 显示名称（可选）
- `lore`: 描述列表（可选）
- `enchantments`: 附魔列表（可选）
- `unbreakable`: 是否不可破坏（可选）
- `custom_model_data`: 自定义模型数据（可选）

### 刷新类型说明

| 类型 | 描述 | 示例 |
|------|------|------|
| `NONE` | 无限制，可以随时交易 | 永久商店 |
| `DAILY` | 每天 0 点刷新 | 每日兑换 |
| `WEEKLY` | 每周一 0 点刷新 | 每周奖励 |
| `MONTHLY` | 每月 1 号 0 点刷新 | 月度礼包 |
| `CUSTOM` | 自定义间隔（秒） | 每小时刷新 |

## 💾 数据存储

### 冷却时间数据
```
plugins/Trade/cooldowns.yml
```

格式：
```yaml
<玩家UUID>:
  <配方ID>: <上次交易时间戳>
```

### 重要说明
- ⚠️ **不要手动修改文件名**：文件名必须与内部的 `id` 字段一致
- ✅ **使用 LinkedHashMap**：保持配方的插入顺序
- ✅ **安全的 Reload**：reload 后配方不会丢失
- ✅ **NBT 自动保存**：所有物品的 NBT 数据自动序列化

## 🛠️ 构建和部署

### 环境要求
- Java 8 或更高版本
- Maven 3.6+
- Minecraft 1.21.3 Paper 服务器

### 安装到服务器
1. 将生成的 jar 文件复制到服务器的 `plugins` 目录
2. 重启服务器或执行 `/reload` 命令
3. 插件会自动创建必要的文件夹和示例配方

### 验证安装
```
1. 启动服务器
2. 查看控制台是否有 "Trade插件已启用！" 消息
3. 执行 /trade 命令测试
4. 检查 plugins/Trade 目录是否生成
```

## 📝 示例配方

### 示例1：普通物品兑换
```yaml
# 文件名：stone_to_iron.yml
id: stone_to_iron
input:
  '0':
    material: STONE
    amount: 64
output:
  '0':
    material: IRON_INGOT
    amount: 1
refresh:
  type: NONE
```

### 示例2：MythicMobs 武器商店
```yaml
# 文件名：mythic_shop.yml
id: mythic_shop
trade_title: "§e§l传说武器店"
trade_success_message: "§6恭喜获得传说武器！"

input:
  '0':
    material: GOLD_INGOT
    amount: 64
output:
  '0':
    material: DIAMOND_SWORD
    amount: 1
    nbt_data: "H4sIAAAAAAAA..."  # MythicMobs 武器的 NBT
refresh:
  type: DAILY
```

### 示例3：每日兑换
```yaml
# 文件名：daily_exchange.yml
id: daily_exchange
trade_title: "§b§l每日兑换"
trade_success_message: "§e今日兑换完成！明天再来吧~"

input:
  '0':
    material: EMERALD
    amount: 10
output:
  '0':
    material: DIAMOND
    amount: 1
refresh:
  type: DAILY
```

### 示例4：每日限兑商店 ⭐ 新增
```yaml
# 文件名：daily_limit_shop.yml
id: daily_limit_shop
trade_title: "§e§l每日限量商店"

input:
  '0':
    material: GOLD_INGOT
    amount: 10
output:
  '0':
    material: DIAMOND
    amount: 1
refresh:
  type: DAILY
max_trade_count: 5                    # 每天最多兑换5次
count_resets_with_refresh: true       # 每天0点重置次数
```

### 示例5：限量稀有物品 ⭐ 新增
```yaml
# 文件名：rare_item_limited.yml
id: rare_item_limited
trade_title: "§d§l限量商店"

input:
  '0':
    material: EMERALD
    amount: 64
output:
  '0':
    material: NETHER_STAR
    amount: 1
refresh:
  type: NONE
max_trade_count: 3                    # 总共只能兑换3次
count_resets_with_refresh: false      # 不重置，永久累计
```


## ❓ 常见问题

### Q1: 如何添加新的交易配方？
**A**: 有两种方法：
1. **推荐**：使用 `/trade edit` 命令打开 GUI 编辑器
2. **高级**：手动在 `plugins/Trade/recipes/` 目录下创建 yml 文件

### Q2: 如何修改已有配方？
**A**: 
- 使用 `/trade edit <配方ID>` 命令打开编辑器
- 或直接编辑对应的 yml 文件，然后执行 `/trade reload`

### Q3: 为什么 reload 后配方失效了？
**A**: 
- ✅ 已修复！现在 reload 完全正常
- 确保文件名和内部 `id` 字段一致
- 检查控制台是否有加载错误

### Q4: 如何防止玩家用普通物品替代特殊物品？
**A**: 
- ✅ 已实现完整的 NBT 验证！
- 系统会自动对比完整的 NBT 数据
- MythicMobs、MMOItems 等物品无法被替代

### Q5: 玩家的交易记录保存在哪里？
**A**: 保存在 `plugins/Trade/cooldowns.yml` 文件中

### Q6: 如何重置某个玩家的冷却时间？
**A**: 
1. 删除 `cooldowns.yml` 中对应对玩家和配方的记录
2. 或删除整个文件（重置所有玩家）
3. 或等待冷却时间自然过期

### Q7: 支持哪些 Minecraft 版本？
**A**: 目前仅支持 1.21.x 版本（基于 Paper 核心）

### Q8: 可以自定义交易界面的标题吗？
**A**: 
- ✅ 可以！每个配方都可以有自己的标题
- 在配方文件中添加 `trade_title` 字段
- 或在 GUI 编辑器中点击第48格按钮设置

### Q9: 如何备份配方？
**A**: 
```
# 备份整个 recipes 文件夹
```

### Q10: 性能怎么样？
**A**: 
- 小配方（1-4个物品）：无明显影响
- 大配方（10+个物品）：轻微延迟（可接受）
- 建议：保持配方简洁，避免过多物品

## 技术支持

如遇到问题，请提供以下信息：
1. 服务器版本和核心类型
2. 完整的错误日志
3. 相关的配置文件内容
4. 问题复现步骤

## 📜 更新日志

#### ✨ 新增功能
##### 1. **完整的 NBT 数据支持**
   - Base64 编码存储完整的物品 NBT 数据
   - 完美支持 MythicMobs、MMOItems、ItemsAdder 等插件
   - 交易时自动保留所有物品属性

##### 2. **智能 NBT 验证系统** ⭐ 重要安全修复
   - 精确对比材质、数量、NBT、附魔、Lore
   - 防止用普通物品替代特殊物品
   - 高效的物品匹配算法
   - **修复前**：玩家可以用普通钻石剑兑换需要 MythicMobs 武器的配方 ❌
   - **修复后**：必须完全匹配 NBT 数据才能交易 ✅

##### 3. **自定义界面标题和提示**
   - 每个配方可以自定义交易界面标题
   - 每个配方可以自定义交易成功提示
   - GUI 编辑器中提供快捷设置按钮
   - 支持 Minecraft 颜色代码

##### 4. **兑换次数限制系统** ⭐ 新增核心功能
   - 每个配方可设置最大兑换次数（`max_trade_count`）
   - 支持随时间刷新重置次数（`count_resets_with_refresh`）
   - 两种模式：
     - **随刷新重置**：每日/每周/每月刷新时自动重置次数
     - **永久累计**：次数用完永远不能再兑换（限量物品）
   - 实时显示剩余次数和总次数（如“点击交易（剩余2次）”）
   - 次数用完时自动禁用交易按钮（变为灰色）
   - 在配方列表中显示次数限制信息

##### 5. **改进的物品检查逻辑**
   - 逐个对比完整的 ItemStackData
   - 支持多个相同物品的累加计算
   - 正确的 NBT 匹配验证

#### 🐛 重要修复
##### 1. **Reload 后配方失效问题**
   - 修复 YAML 加载逻辑（getList → getConfigurationSection）
   - 现在 reload 完全正常，配方不会丢失

##### 2. **配方列表点击失败**
   - 修复配方 ID 提取逻辑
   - 改用槽位索引匹配，更可靠

##### 3. **GUI 物品可被拿走**
   - 修复事件检测逻辑
   - 确认后立即可取消事件

##### 4. **手动重命名文件问题**
   - 改进 GUI 检测机制
   - 防止物品被意外取出

##### 5. **物品双倍问题**
   - 添加刷新状态追踪
   - 防止关闭事件重复处理

##### 6. **GUI 闪退问题**
   - 优化 inventory 打开逻辑
   - 关闭时正确返回物品

##### 7. **NullPointerException**
   - 构造函数中先初始化所有字段
   - 避免提前 return 导致 null

##### 8. **命令注册失败**
   - 删除 paper-plugin.yml
   - 只使用 plugin.yml

##### 9. **Material.CALENDAR 不存在**
   - 改为 Material.CLOCK

##### 10. **编辑器关闭时按钮物品返回背包** ⭐ 新增修复
   - 添加排除列表，跳过按钮和分界线槽位
   - 只返回玩家放置的有效物品

##### 11. **右侧物品无法保存** ⭐ 新增修复
   - 修复循环次数与数组长度不一致的问题
   - 右侧物品槽从 22 个调整为 20 个（移除第50、51、52格）
   - 切换左右侧时物品不再重复

#### 🔧 技术改进
1. **数据结构优化**
   - HashMap → LinkedHashMap（保持顺序）
   - 确保配方列表顺序一致

2. **YAML 格式修正**
   - 正确使用 ConfigurationSection
   - Map 结构而非 List

3. **事件处理优化**
   - 区分 GUI 和玩家背包槽位
   - 使用 getSlot() 而非 getRawSlot()

4. **错误处理增强**
   - 添加详细的调试日志
   - 更好的错误提示

---

**祝您使用愉快！** 🎉
