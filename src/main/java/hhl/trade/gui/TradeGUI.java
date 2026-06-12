package hhl.trade.gui;

import hhl.trade.Trade;
import hhl.trade.manager.ConfigManager;
import hhl.trade.manager.RefreshManager;
import hhl.trade.model.ItemStackData;
import hhl.trade.model.TradeRecipe;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

/**
 * 玩家交易GUI
 */
public class TradeGUI implements Listener {
    private final Trade plugin;
    private final ConfigManager configManager;
    private final RefreshManager refreshManager;
    
    // 玩家 -> 当前查看的配方ID
    private Map<UUID, String> viewingRecipes;
    
    private static final int INVENTORY_SIZE = 54;
    
    public TradeGUI(Trade plugin, ConfigManager configManager, RefreshManager refreshManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.refreshManager = refreshManager;
        this.viewingRecipes = new HashMap<>();
        
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }
    
    /**
     * 打开交易列表
     */
    public void openTradeList(Player player) {
        Map<String, TradeRecipe> recipes = configManager.getAllRecipes();
        
        if (recipes.isEmpty()) {
            player.sendMessage("§c当前没有可用的交易配方！");
            return;
        }
        
        Inventory inventory = Bukkit.createInventory(null, INVENTORY_SIZE, "§6交易系统 - 选择配方");
        
        int slot = 0;
        for (Map.Entry<String, TradeRecipe> entry : recipes.entrySet()) {
            if (slot >= INVENTORY_SIZE - 9) {
                break;
            }
            
            TradeRecipe recipe = entry.getValue();
            ItemStack displayItem = createRecipeDisplayItem(recipe);
            inventory.setItem(slot, displayItem);
            slot++;
        }
        
        player.openInventory(inventory);
    }
    
    /**
     * 打开具体交易界面
     */
    public void openTrade(Player player, String recipeId) {
        TradeRecipe recipe = configManager.getRecipe(recipeId);
        if (recipe == null) {
            player.sendMessage("§c配方不存在！");
            return;
        }
        
        viewingRecipes.put(player.getUniqueId(), recipeId);
        
        //Inventory inventory = Bukkit.createInventory(null, INVENTORY_SIZE, recipe.getTradeTitle() + " - " + recipeId);
        Inventory inventory = Bukkit.createInventory(null, INVENTORY_SIZE, recipe.getTradeTitle());
        // 设置分界线
        setupBorder(inventory);
        
        // 显示输入物品
        fillItems(inventory, recipe.getInputItems(), true);
        
        // 显示输出物品
        fillItems(inventory, recipe.getOutputItems(), false);
        
        // 确认交易按钮
        inventory.setItem(49, createTradeButton(player, recipe));
        
        player.openInventory(inventory);
    }
    
    /**
     * 设置边界
     */
    private void setupBorder(Inventory inventory) {
        int[] borderSlots = {4, 13, 22, 31, 40, 49};
        ItemStack borderItem = createBorderItem();
        for (int slot : borderSlots) {
            if (slot != 49) { // 跳过确认按钮位置
                inventory.setItem(slot, borderItem);
            }
        }
    }
    
    /**
     * 填充物品
     */
    private void fillItems(Inventory inventory, List<ItemStackData> items, boolean isInput) {
        for (int i = 0; i < items.size(); i++) {
            int slot;
            if (isInput) {
                slot = getLeftSlot(i);
            } else {
                slot = getRightSlot(i);
            }
            
            if (slot >= 0 && slot < INVENTORY_SIZE) {
                ItemStackData itemData = items.get(i);
                ItemStack item = itemData.toItemStack();
                if (item != null) {
                    inventory.setItem(slot, item);
                }
            }
        }
    }
    
    /**
     * 获取左侧槽位
     */
    private int getLeftSlot(int index) {
        int[] leftSlots = {
            0, 1, 2, 3,
            9, 10, 11, 12,
            18, 19, 20, 21,
            27, 28, 29, 30,
            36, 37, 38, 39,
            45, 46, 47, 48
        };
        return index < leftSlots.length ? leftSlots[index] : -1;
    }
    
    /**
     * 获取右侧槽位
     */
    private int getRightSlot(int index) {
        int[] rightSlots = {
            5, 6, 7, 8,
            14, 15, 16, 17,
            23, 24, 25, 26,
            32, 33, 34, 35,
            41, 42, 43, 44,
            50, 51, 52
        };
        return index < rightSlots.length ? rightSlots[index] : -1;
    }
    
    /**
     * 创建分界线物品
     */
    private ItemStack createBorderItem() {
        ItemStack item = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§7§l━━━━━━━━━━━━");
            item.setItemMeta(meta);
        }
        return item;
    }
    
    /**
     * 创建配方显示物品
     */
    private ItemStack createRecipeDisplayItem(TradeRecipe recipe) {
        Material material = Material.CHEST;
        if (!recipe.getInputItems().isEmpty()) {
            material = recipe.getInputItems().get(0).getMaterial();
        }
        
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§e§l" + recipe.getId());
            
            List<String> lore = new ArrayList<>();
            lore.add("§7输入物品: §f" + recipe.getInputItems().size());
            lore.add("§7输出物品: §f" + recipe.getOutputItems().size());
            lore.add("");
            
            switch (recipe.getRefreshType()) {
                case DAILY:
                    lore.add("§b刷新: §e每天0点");
                    break;
                case WEEKLY:
                    lore.add("§b刷新: §e每周一0点");
                    break;
                case MONTHLY:
                    lore.add("§b刷新: §e每月1号0点");
                    break;
                case CUSTOM:
                    lore.add("§b刷新: §e每" + recipe.getRefreshInterval() + "秒");
                    break;
                default:
                    lore.add("§b刷新: §a无限制");
                    break;
            }
            
            // 显示次数限制
            if (recipe.getMaxTradeCount() > 0) {
                lore.add("§d次数限制: §e" + recipe.getMaxTradeCount() + "次");
                if (recipe.isCountResetsWithRefresh()) {
                    lore.add("§7（随刷新重置）");
                } else {
                    lore.add("§7（永久累计）");
                }
            }
            
            lore.add("");
            lore.add("§7点击查看详情");
            
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }
    
    /**
     * 创建交易按钮
     */
    private ItemStack createTradeButton(Player player, TradeRecipe recipe) {
        Material material = Material.LIME_CONCRETE;
        String status = "§a§l点击交易";
        
        // 检查冷却
        if (!refreshManager.canTrade(player.getUniqueId(), recipe)) {
            material = Material.RED_CONCRETE;
            long remainingSeconds = refreshManager.getRemainingCooldownSeconds(player.getUniqueId(), recipe);
            status = "§c§l冷却中: " + formatTime(remainingSeconds);
        }
        
        // 检查次数限制
        int maxCount = recipe.getMaxTradeCount();
        if (maxCount > 0) {
            int currentCount = refreshManager.getTradeCount(player.getUniqueId(), recipe.getId());
            int remainingCount = maxCount - currentCount;
            if (remainingCount <= 0) {
                material = Material.GRAY_CONCRETE;
                status = "§7§l次数已用完";
            } else {
                status = "§a§l点击交易（剩余" + remainingCount + "次）";
            }
        }
        
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(status);
            
            List<String> lore = new ArrayList<>();
            lore.add("§7输入: §f" + recipe.getInputItems().size() + " 个物品");
            lore.add("§7输出: §f" + recipe.getOutputItems().size() + " 个物品");
            lore.add("");
            
            // 显示次数信息
            if (maxCount > 0) {
                int currentCount = refreshManager.getTradeCount(player.getUniqueId(), recipe.getId());
                lore.add("§d兑换次数: §e" + currentCount + "/" + maxCount);
                if (recipe.isCountResetsWithRefresh()) {
                    lore.add("§7（随刷新重置）");
                } else {
                    lore.add("§7（永久累计）");
                }
                lore.add("");
            }
            
            if (recipe.getRefreshType() != TradeRecipe.RefreshType.NONE) {
                lore.add("§c注意: 此交易有冷却时间");
            }
            
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }
    
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        // 检查是否是交易界面（动态标题）
        String title = event.getView().getTitle();
        boolean isTradeGUI = false;
        
        // 检查是否是配方列表界面
        if (title.contains("选择配方")) {
            isTradeGUI = true;
        }
        
        // 检查是否是具体交易界面（遍历所有配方的标题）
        if (!isTradeGUI) {
            for (TradeRecipe recipe : configManager.getAllRecipes().values()) {
                if (title.equals(recipe.getTradeTitle()) || title.startsWith(recipe.getTradeTitle() + " - ")) {
                    isTradeGUI = true;
                    break;
                }
            }
        }
        
        // 如果不是交易 GUI，直接返回
        if (!isTradeGUI) {
            return;
        }
        
        // 确认是交易 GUI 后，取消事件（防止玩家拿走物品）
        event.setCancelled(true);
        
        Player player = (Player) event.getWhoClicked();
        int slot = event.getRawSlot();
        
        // 配方列表界面
        if (title.contains("选择配方")) {
            if (slot >= 0 && slot < INVENTORY_SIZE - 9) {
                ItemStack clickedItem = event.getCurrentItem();
                if (clickedItem != null && clickedItem.getType() != Material.AIR) {
                    // 通过槽位索引获取对应的配方
                    Map<String, TradeRecipe> recipes = configManager.getAllRecipes();
                    int index = 0;
                    String recipeId = null;
                    
                    for (String id : recipes.keySet()) {
                        if (index == slot) {
                            recipeId = id;
                            break;
                        }
                        index++;
                    }
                    
                    if (recipeId != null) {
                        openTrade(player, recipeId);
                    } else {
                        player.sendMessage("§c配方不存在！");
                    }
                }
            }
            return;
        }
        
        // 交易界面
        String recipeId = viewingRecipes.get(player.getUniqueId());
        if (recipeId == null) {
            player.closeInventory();
            return;
        }
        
        // 确认交易按钮
        if (slot == 49) {
            executeTrade(player, recipeId);
        }
    }
    
    /**
     * 执行交易
     */
    private void executeTrade(Player player, String recipeId) {
        TradeRecipe recipe = configManager.getRecipe(recipeId);
        if (recipe == null) {
            player.sendMessage("§c配方不存在！");
            return;
        }
        
        // 检查冷却时间
        if (!refreshManager.canTrade(player.getUniqueId(), recipe)) {
            long remainingSeconds = refreshManager.getRemainingCooldownSeconds(player.getUniqueId(), recipe);
            player.sendMessage("§c此交易还在冷却中！剩余时间: §e" + formatTime(remainingSeconds));
            return;
        }
        
        // 检查兑换次数限制
        int maxCount = recipe.getMaxTradeCount();
        if (maxCount > 0) {
            int currentCount = refreshManager.getTradeCount(player.getUniqueId(), recipe.getId());
            int remainingCount = maxCount - currentCount;
            if (remainingCount <= 0) {
                player.sendMessage("§c此配方的兑换次数已用完！（" + currentCount + "/" + maxCount + "）");
                return;
            }
        }
        
        // 检查玩家是否有足够的输入物品
        if (!hasRequiredItems(player, recipe.getInputItems())) {
            player.sendMessage("§c你没有足够的物品进行交易！");
            return;
        }
        
        // 移除输入物品
        removeItems(player, recipe.getInputItems());
        
        // 给予输出物品
        giveItems(player, recipe.getOutputItems());
        
        // 记录交易时间
        refreshManager.recordTrade(player.getUniqueId(), recipeId);
        
        player.sendMessage(recipe.getTradeSuccessMessage());
        
        // 重新打开交易界面
        openTrade(player, recipeId);
    }
    
    /**
     * 检查玩家是否有足够的物品（包含 NBT 检查）
     */
    private boolean hasRequiredItems(Player player, List<ItemStackData> requiredItems) {
        plugin.getLogger().info("[DEBUG] 开始检查物品，需要 " + requiredItems.size() + " 种物品");
        
        // 创建玩家背包中所有物品的副本
        List<ItemStack> playerItems = new ArrayList<>();
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() != Material.AIR) {
                playerItems.add(item.clone());
            }
        }
        
        plugin.getLogger().info("[DEBUG] 玩家背包中有 " + playerItems.size() + " 个非空物品槽");
        
        // 对每个需要的物品，尝试在玩家背包中查找匹配的物品
        for (int i = 0; i < requiredItems.size(); i++) {
            ItemStackData requiredData = requiredItems.get(i);
            ItemStack requiredItem = requiredData.toItemStack();
            if (requiredItem == null) {
                plugin.getLogger().info("[DEBUG] 物品#" + i + " 转换为 null，跳过");
                continue;
            }
            
            plugin.getLogger().info("[DEBUG] 检查物品#" + i + ": " + requiredItem.getType() + " x" + requiredItem.getAmount());
            plugin.getLogger().info("[DEBUG] 需要的 NBT: " + (requiredData.getNbtData() != null ? "有(" + requiredData.getNbtData().length() + "字符)" : "无"));
            
            int remainingAmount = requiredItem.getAmount();
            int matchedCount = 0;
            
            // 遍历玩家背包，查找匹配的物品
            Iterator<ItemStack> iterator = playerItems.iterator();
            while (iterator.hasNext() && remainingAmount > 0) {
                ItemStack playerItem = iterator.next();
                
                // 检查材质是否相同
                if (playerItem.getType() != requiredItem.getType()) {
                    continue;
                }
                
                // 检查 NBT 数据是否匹配
                boolean match = isItemMatch(playerItem, requiredItem);
                if (!match) {
                    plugin.getLogger().info("[DEBUG]   - 物品不匹配 (材质: " + playerItem.getType() + ", 数量: " + playerItem.getAmount() + ")");
                    continue;
                }
                
                plugin.getLogger().info("[DEBUG]   - 找到匹配物品: " + playerItem.getType() + " x" + playerItem.getAmount());
                
                // 物品匹配，扣除数量
                int playerAmount = playerItem.getAmount();
                matchedCount += playerAmount;
                if (playerAmount <= remainingAmount) {
                    remainingAmount -= playerAmount;
                    iterator.remove(); // 移除已使用的物品
                } else {
                    playerItem.setAmount(playerAmount - remainingAmount);
                    remainingAmount = 0;
                }
            }
            
            plugin.getLogger().info("[DEBUG] 物品#" + i + " 匹配结果: 需要" + requiredItem.getAmount() + ", 找到" + matchedCount + ", 剩余需求" + remainingAmount);
            
            // 如果还有剩余需求，说明物品不足
            if (remainingAmount > 0) {
                plugin.getLogger().warning("[DEBUG] 物品不足！需要 " + requiredItem.getAmount() + " 个 " + requiredItem.getType() + "，但只找到 " + matchedCount + " 个");
                return false;
            }
        }
        
        plugin.getLogger().info("[DEBUG] 所有物品检查通过！");
        return true;
    }
    
    /**
     * 检查两个物品是否匹配（包括 NBT 数据）
     */
    private boolean isItemMatch(ItemStack playerItem, ItemStack requiredItem) {
        // 材质必须相同
        if (playerItem.getType() != requiredItem.getType()) {
            plugin.getLogger().info("[DEBUG]     isItemMatch: 材质不匹配 (玩家: " + playerItem.getType() + ", 需要: " + requiredItem.getType() + ")");
            return false;
        }
        
        // 转换为 ItemStackData 进行完整对比
        ItemStackData playerData = new ItemStackData(playerItem);
        ItemStackData requiredData = new ItemStackData(requiredItem);
        
        // 对比 NBT 数据（Base64 编码）
        String playerNbt = playerData.getNbtData();
        String requiredNbt = requiredData.getNbtData();
        
        plugin.getLogger().info("[DEBUG]     isItemMatch: 材质相同=" + playerItem.getType());
        plugin.getLogger().info("[DEBUG]     isItemMatch: 玩家NBT=" + (playerNbt != null ? "有(" + playerNbt.length() + "字符)" : "无"));
        plugin.getLogger().info("[DEBUG]     isItemMatch: 需要NBT=" + (requiredNbt != null ? "有(" + requiredNbt.length() + "字符)" : "无"));
        
        // 如果要求的物品有 NBT 数据，需要更智能的对比
        if (requiredNbt != null && !requiredNbt.isEmpty()) {
            // 反序列化两个物品进行实际对比
            ItemStack requiredDeserialized = requiredData.toItemStack();
            ItemStack playerDeserialized = playerData.toItemStack();
            
            if (requiredDeserialized == null || playerDeserialized == null) {
                plugin.getLogger().warning("[DEBUG]     isItemMatch: 反序列化失败");
                return false;
            }
            
            // 对比关键属性
            boolean match = compareItemsStrictly(requiredDeserialized, playerDeserialized);
            plugin.getLogger().info("[DEBUG]     isItemMatch: NBT严格对比结果=" + match);
            return match;
        }
        
        // 如果要求的物品没有 NBT 数据，则玩家的物品可以是任何状态
        // （有 NBT 或没有 NBT 都可以，只要材质相同）
        plugin.getLogger().info("[DEBUG]     isItemMatch: 无需NBT检查，返回true");
        return true;
    }
    
    /**
     * 严格对比两个物品的所有属性
     */
    private boolean compareItemsStrictly(ItemStack item1, ItemStack item2) {
        // 材质必须相同
        if (item1.getType() != item2.getType()) {
            return false;
        }
        
        ItemMeta meta1 = item1.getItemMeta();
        ItemMeta meta2 = item2.getItemMeta();
        
        // 如果都没有 Meta，认为相同
        if (meta1 == null && meta2 == null) {
            return true;
        }
        
        // 如果一个有 Meta 一个没有，不同
        if (meta1 == null || meta2 == null) {
            return false;
        }
        
        // 对比显示名称
        boolean nameMatch = Objects.equals(meta1.getDisplayName(), meta2.getDisplayName());
        if (!nameMatch) {
            plugin.getLogger().info("[DEBUG]       - 名称不匹配");
            return false;
        }
        
        // 对比 Lore
        boolean loreMatch = Objects.equals(meta1.getLore(), meta2.getLore());
        if (!loreMatch) {
            plugin.getLogger().info("[DEBUG]       - Lore不匹配");
            return false;
        }
        
        // 对比附魔
        boolean enchantsMatch = meta1.getEnchants().equals(meta2.getEnchants());
        if (!enchantsMatch) {
            plugin.getLogger().info("[DEBUG]       - 附魔不匹配");
            return false;
        }
        
        // 对比不可破坏
        if (meta1.isUnbreakable() != meta2.isUnbreakable()) {
            plugin.getLogger().info("[DEBUG]       - 不可破坏属性不匹配");
            return false;
        }
        
        // 对比亚自定义模型数据
        if (meta1.hasCustomModelData() != meta2.hasCustomModelData()) {
            plugin.getLogger().info("[DEBUG]       - 自定义模型数据存在性不匹配");
            return false;
        }
        
        if (meta1.hasCustomModelData() && meta2.hasCustomModelData()) {
            if (meta1.getCustomModelData() != meta2.getCustomModelData()) {
                plugin.getLogger().info("[DEBUG]       - 自定义模型数据值不匹配");
                return false;
            }
        }
        
        // 尝试对比 PersistentDataContainer（MythicMobs 等插件使用）
        try {
            org.bukkit.persistence.PersistentDataContainer pdc1 = meta1.getPersistentDataContainer();
            org.bukkit.persistence.PersistentDataContainer pdc2 = meta2.getPersistentDataContainer();
            
            // 对比 PDC 的 keys
            Set<org.bukkit.NamespacedKey> keys1 = pdc1.getKeys();
            Set<org.bukkit.NamespacedKey> keys2 = pdc2.getKeys();
            
            if (!keys1.equals(keys2)) {
                plugin.getLogger().info("[DEBUG]       - PDC keys不匹配");
                return false;
            }
            
            // 对比每个 key 的值
            for (org.bukkit.NamespacedKey key : keys1) {
                // 简单对比：如果都有这个 key，认为匹配
                // 更严格的对比需要检查值的类型和内容
            }
        } catch (Exception e) {
            plugin.getLogger().warning("[DEBUG]       - PDC对比失败: " + e.getMessage());
        }
        
        plugin.getLogger().info("[DEBUG]       - 所有属性匹配成功");
        return true;
    }
    
    /**
     * 移除物品（包含 NBT 检查）
     */
    private void removeItems(Player player, List<ItemStackData> itemsToRemove) {
        // 获取玩家背包内容
        ItemStack[] contents = player.getInventory().getContents();
        
        // 对每个需要移除的物品
        for (ItemStackData itemData : itemsToRemove) {
            ItemStack requiredItem = itemData.toItemStack();
            if (requiredItem == null) continue;
            
            int remainingAmount = requiredItem.getAmount();
            
            // 遍历玩家背包
            for (int i = 0; i < contents.length && remainingAmount > 0; i++) {
                ItemStack item = contents[i];
                if (item == null || item.getType() == Material.AIR) {
                    continue;
                }
                
                // 检查材质是否相同
                if (item.getType() != requiredItem.getType()) {
                    continue;
                }
                
                // 检查 NBT 数据是否匹配
                if (!isItemMatch(item, requiredItem)) {
                    continue;
                }
                
                // 物品匹配，移除数量
                int itemAmount = item.getAmount();
                if (itemAmount <= remainingAmount) {
                    contents[i] = null; // 完全移除
                    remainingAmount -= itemAmount;
                } else {
                    item.setAmount(itemAmount - remainingAmount);
                    remainingAmount = 0;
                }
            }
        }
        
        // 更新玩家背包
        player.getInventory().setContents(contents);
    }
    
    /**
     * 给予物品
     */
    private void giveItems(Player player, List<ItemStackData> itemsToGive) {
        for (ItemStackData itemData : itemsToGive) {
            ItemStack item = itemData.toItemStack();
            if (item != null) {
                player.getInventory().addItem(item);
            }
        }
    }
    
    /**
     * 格式化时间显示
     */
    private String formatTime(long seconds) {
        if (seconds < 60) {
            return seconds + "秒";
        } else if (seconds < 3600) {
            return (seconds / 60) + "分钟";
        } else {
            return (seconds / 3600) + "小时";
        }
    }
}
