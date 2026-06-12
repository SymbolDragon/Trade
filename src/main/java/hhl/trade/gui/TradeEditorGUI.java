package hhl.trade.gui;

import hhl.trade.Trade;
import hhl.trade.manager.ConfigManager;
import hhl.trade.model.ItemStackData;
import hhl.trade.model.TradeRecipe;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

/**
 * 交易编辑器GUI
 */
public class TradeEditorGUI implements Listener {
    private final Trade plugin;
    private final ConfigManager configManager;
    
    // 玩家 -> 正在编辑的配方
    private Map<UUID, TradeRecipe> editingRecipes;
    // 玩家 -> 编辑模式（true=编辑输入，false=编辑输出）
    private Map<UUID, Boolean> editModes;
    // 玩家 -> 是否正在刷新界面（防止重复处理关闭事件）
    private Set<UUID> refreshingPlayers;
    
    private static final int INVENTORY_SIZE = 54;
    private static final String EDITOR_TITLE = "§6交易编辑器";
    
    public TradeEditorGUI(Trade plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.editingRecipes = new HashMap<>();
        this.editModes = new HashMap<>();
        this.refreshingPlayers = new HashSet<>();
        
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }
    
    /**
     * 打开编辑器（新建配方）
     */
    public void openNewEditor(Player player) {
        String recipeId = configManager.generateRecipeId();
        TradeRecipe recipe = new TradeRecipe(recipeId);
        
        editingRecipes.put(player.getUniqueId(), recipe);
        editModes.put(player.getUniqueId(), true); // 默认编辑输入侧
        
        openEditor(player);
    }
    
    /**
     * 打开编辑器（编辑现有配方）
     */
    public void openEditEditor(Player player, String recipeId) {
        TradeRecipe recipe = configManager.getRecipe(recipeId);
        if (recipe == null) {
            player.sendMessage("§c配方不存在！");
            return;
        }
        
        // 创建副本进行编辑
        TradeRecipe editCopy = cloneRecipe(recipe);
        editingRecipes.put(player.getUniqueId(), editCopy);
        editModes.put(player.getUniqueId(), true);
        
        openEditor(player);
    }
    
    /**
     * 打开编辑器界面
     */
    private void openEditor(Player player) {
        // 检查玩家是否还在编辑状态
        UUID playerId = player.getUniqueId();
        if (!editingRecipes.containsKey(playerId)) {
            return;
        }
        
        // 标记为正在刷新，防止关闭事件处理
        refreshingPlayers.add(playerId);
        
        Inventory inventory = Bukkit.createInventory(null, INVENTORY_SIZE, EDITOR_TITLE);
        TradeRecipe recipe = editingRecipes.get(playerId);
        boolean isInputMode = editModes.get(playerId);
        
        // 设置分界线和按钮
        setupBorder(inventory, isInputMode, recipe);
        
        // 填充输入物品
        fillItems(inventory, recipe.getInputItems(), true);
        
        // 填充输出物品
        fillItems(inventory, recipe.getOutputItems(), false);
        
        // 直接打开，不延迟（延迟可能导致状态不同步）
        player.openInventory(inventory);
        
        // 延迟移除刷新标记，确保 inventory 已打开
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            refreshingPlayers.remove(playerId);
        }, 2L);
    }
    
    /**
     * 设置边界和按钮
     */
    private void setupBorder(Inventory inventory, boolean isInputMode, TradeRecipe recipe) {
        // 中间分界线（第4列，索引4,13,22,31,40,49）
        int[] borderSlots = {4, 13, 22, 31, 40, 49};
        ItemStack borderItem = createBorderItem();
        for (int slot : borderSlots) {
            inventory.setItem(slot, borderItem);
        }
        
        // 确认按钮（第45格）
        inventory.setItem(45, createConfirmButton());
        
        // 切换侧按钮（第46格）
        inventory.setItem(46, createSwitchSideButton(isInputMode));
        
        // 刷新类型按钮（第47格）
        inventory.setItem(47, createRefreshTypeButton());
        
        // 删除配方按钮（第53格）
        inventory.setItem(53, createDeleteButton());
        
        // 设置界面标题按钮（第48格）
        inventory.setItem(48, createSetTitleButton(recipe));
        
        // 设置成功提示按钮（第52格）
        inventory.setItem(52, createSetSuccessMessageButton(recipe));
        
        // 设置最大兑换次数按钮（第50格）
        inventory.setItem(50, createSetMaxTradeCountButton(recipe));
        
        // 设置次数重置方式按钮（第51格）
        inventory.setItem(51, createSetResetModeButton(recipe));
    }
    
    /**
     * 填充物品到对应区域
     */
    private void fillItems(Inventory inventory, List<ItemStackData> items, boolean isInput) {
        int maxItems = isInput ? 20 : 22; // 左侧20个槽位，右侧22个槽位
        
        for (int i = 0; i < items.size() && i < maxItems; i++) {
            int slot;
            if (isInput) {
                slot = getLeftSlot(i);
            } else {
                slot = getRightSlot(i);
            }
            
            if (slot >= 0 && slot < INVENTORY_SIZE) {
                ItemStackData itemData = items.get(i);
                if (itemData != null) {
                    ItemStack item = itemData.toItemStack();
                    if (item != null) {
                        inventory.setItem(slot, item);
                    }
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
            36, 37, 38, 39
            // 注意：45, 46, 47, 48 是按钮区域，不作为物品槽
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
            41, 42, 43, 44
            // 注意：50, 51, 52, 53 是按钮区域，不作为物品槽
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
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            item.setItemMeta(meta);
        }
        return item;
    }
    
    /**
     * 创建确认按钮
     */
    private ItemStack createConfirmButton() {
        ItemStack item = new ItemStack(Material.LIME_CONCRETE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§a§l确认保存");
            List<String> lore = Arrays.asList(
                "§7点击保存当前交易配方",
                "",
                "§e左侧 §8→ §e右侧"
            );
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }
    
    /**
     * 创建切换侧按钮
     */
    private ItemStack createSwitchSideButton(boolean isInputMode) {
        ItemStack item = new ItemStack(isInputMode ? Material.RED_CONCRETE : Material.BLUE_CONCRETE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§b§l切换编辑侧");
            List<String> lore = Arrays.asList(
                "§7当前: " + (isInputMode ? "§e左侧(输入)" : "§a右侧(输出)"),
                "",
                "§7点击切换编辑区域"
            );
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }
    
    /**
     * 创建刷新类型按钮
     */
    private ItemStack createRefreshTypeButton() {
        if (editingRecipes.isEmpty()) {
            return new ItemStack(Material.CLOCK);
        }
        
        TradeRecipe recipe = editingRecipes.values().iterator().next();
        Material material;
        String typeName;
        
        switch (recipe.getRefreshType()) {
            case DAILY:
                material = Material.CLOCK;
                typeName = "§e每天0点刷新";
                break;
            case WEEKLY:
                material = Material.COMPASS;
                typeName = "§b每周一0点刷新";
                break;
            case MONTHLY:
                material = Material.CLOCK;
                typeName = "§d每月1号0点刷新";
                break;
            case CUSTOM:
                material = Material.REDSTONE;
                typeName = "§c自定义间隔: " + recipe.getRefreshInterval() + "秒";
                break;
            default:
                material = Material.BARRIER;
                typeName = "§7无限制";
                break;
        }
        
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§6§l刷新设置");
            List<String> lore = Arrays.asList(
                "§7当前: " + typeName,
                "",
                "§7右键循环切换类型",
                "§7左键设置自定义间隔"
            );
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }
    
    /**
     * 创建删除按钮
     */
    private ItemStack createDeleteButton() {
        ItemStack item = new ItemStack(Material.BARRIER);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§c§l删除配方");
            List<String> lore = Arrays.asList(
                "§7点击删除此配方",
                "",
                "§4§l警告: 不可撤销!"
            );
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }
    
    /**
     * 创建设置界面标题按钮
     */
    private ItemStack createSetTitleButton(TradeRecipe recipe) {
        ItemStack item = new ItemStack(Material.NAME_TAG);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§d§l设置界面标题");
            List<String> lore = Arrays.asList(
                "§7当前: §e" + recipe.getTradeTitle(),
                "",
                "§7左键设置自定义标题",
                "§7右键重置为默认值"
            );
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }
    
    /**
     * 创建设置成功提示按钮
     */
    private ItemStack createSetSuccessMessageButton(TradeRecipe recipe) {
        ItemStack item = new ItemStack(Material.WRITABLE_BOOK);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§b§l设置交易成功提示");
            List<String> lore = Arrays.asList(
                "§7当前: §e" + recipe.getTradeSuccessMessage(),
                "",
                "§7左键设置自定义提示",
                "§7右键重置为默认值"
            );
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }
    
    /**
     * 创建设置最大兑换次数按钮
     */
    private ItemStack createSetMaxTradeCountButton(TradeRecipe recipe) {
        Material material = recipe.getMaxTradeCount() > 0 ? Material.PAPER : Material.BOOK;
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§d§l设置兑换次数限制");
            List<String> lore;
            if (recipe.getMaxTradeCount() > 0) {
                lore = Arrays.asList(
                    "§7当前: §e" + recipe.getMaxTradeCount() + "次",
                    "",
                    "§7左键增加次数 (+1)",
                    "§7Shift+左键大幅增加 (+10)",
                    "§7右键减少次数 (-1)",
                    "§7Shift+右键设置为无限制"
                );
            } else {
                lore = Arrays.asList(
                    "§7当前: §a无限制",
                    "",
                    "§7左键设置次数 (默认为10)",
                    "§7右键保持无限制"
                );
            }
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }
    
    /**
     * 创建设置次数重置方式按钮
     */
    private ItemStack createSetResetModeButton(TradeRecipe recipe) {
        Material material = recipe.isCountResetsWithRefresh() ? Material.CLOCK : Material.BARRIER;
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§d§l次数重置方式");
            List<String> lore;
            if (recipe.isCountResetsWithRefresh()) {
                lore = Arrays.asList(
                    "§7当前: §e随刷新重置",
                    "",
                    "§7每次时间刷新时，次数会重置",
                    "§7例如：每日配方每天0点重置次数",
                    "",
                    "§7左键切换为永久累计"
                );
            } else {
                lore = Arrays.asList(
                    "§7当前: §e永久累计",
                    "",
                    "§7次数不会随时间刷新重置",
                    "§7直到达到最大次数为止",
                    "",
                    "§7左键切换为随刷新重置"
                );
            }
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }
    
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().equals(EDITOR_TITLE)) {
            return;
        }
        
        Player player = (Player) event.getWhoClicked();
        UUID playerId = player.getUniqueId();
        
        if (!editingRecipes.containsKey(playerId)) {
            player.closeInventory();
            return;
        }
        
        // 使用 getSlot() 而不是 getRawSlot()
        // getSlot() 返回相对于当前打开的 inventory 的槽位
        int slot = event.getSlot();
        Inventory clickedInventory = event.getClickedInventory();
        
        // 如果点击的是玩家背包，允许操作但不取消事件
        if (clickedInventory == player.getInventory()) {
            // 不取消事件，允许玩家从背包拿取物品
            // 但需要延迟更新数据，因为物品可能刚被拿起
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                TradeRecipe recipe = editingRecipes.get(playerId);
                if (recipe != null) {
                    boolean currentMode = editModes.get(playerId);
                    updateRecipeFromInventory(player, recipe, currentMode);
                }
            }, 1L);
            return;
        }
        
        // 如果点击的不是顶部 inventory，取消
        if (clickedInventory != player.getOpenInventory().getTopInventory()) {
            event.setCancelled(true);
            return;
        }
        
        TradeRecipe recipe = editingRecipes.get(playerId);
        boolean isInputMode = editModes.get(playerId);
        
        // 调试日志
        plugin.getLogger().info("点击槽位: " + slot + ", 模式: " + (isInputMode ? "输入" : "输出"));
        
        // 确认按钮
        if (slot == 45) {
            event.setCancelled(true);
            saveRecipe(player, recipe);
            return;
        }
        
        // 切换侧按钮
        if (slot == 46) {
            event.setCancelled(true);
            
            // 先更新当前数据
            updateRecipeFromInventory(player, recipe, isInputMode);
            
            // 切换模式
            editModes.put(playerId, !isInputMode);
            
            // 重新打开编辑器
            openEditor(player);
            return;
        }
        
        // 刷新类型按钮
        if (slot == 47) {
            event.setCancelled(true);
            
            // 先更新当前数据
            updateRecipeFromInventory(player, recipe, isInputMode);
            
            if (event.isRightClick()) {
                cycleRefreshType(recipe);
            } else {
                player.sendMessage("§e自定义间隔功能待实现，请使用右键循环切换");
            }
            
            // 重新打开编辑器
            openEditor(player);
            return;
        }
        
        // 删除按钮
        if (slot == 53) {
            event.setCancelled(true);
            deleteRecipe(player, recipe.getId());
            return;
        }
        
        // 设置界面标题按钮
        if (slot == 48) {
            event.setCancelled(true);
            
            // 先更新当前数据
            updateRecipeFromInventory(player, recipe, isInputMode);
            
            if (event.isRightClick()) {
                // 右键重置为默认值
                recipe.setTradeTitle("§6交易系统");
                player.sendMessage("§a界面标题已重置为默认值");
            } else {
                // 左键提示玩家输入新标题
                player.closeInventory();
                player.sendMessage("§e请在聊天栏中输入新的界面标题（支持颜色代码，如 §6）");
                player.sendMessage("§7提示: 输入 'cancel' 取消操作");
                
                // TODO: 这里需要异步等待玩家输入，暂时使用默认逻辑
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    openEditor(player);
                }, 20L); // 1秒后重新打开
                return;
            }
            
            // 重新打开编辑器
            openEditor(player);
            return;
        }
        
        // 设置成功提示按钮
        if (slot == 52) {
            event.setCancelled(true);
            
            // 先更新当前数据
            updateRecipeFromInventory(player, recipe, isInputMode);
            
            if (event.isRightClick()) {
                // 右键重置为默认值
                recipe.setTradeSuccessMessage("§a交易成功！");
                player.sendMessage("§a交易成功提示已重置为默认值");
            } else {
                // 左键提示玩家输入新提示
                player.closeInventory();
                player.sendMessage("§e请在聊天栏中输入新的交易成功提示（支持颜色代码）");
                player.sendMessage("§7提示: 输入 'cancel' 取消操作");
                
                // TODO: 这里需要异步等待玩家输入，暂时使用默认逻辑
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    openEditor(player);
                }, 20L); // 1秒后重新打开
                return;
            }
            
            // 重新打开编辑器
            openEditor(player);
            return;
        }
        
        // 设置最大兑换次数按钮
        if (slot == 50) {
            event.setCancelled(true);
            
            // 先更新当前数据
            updateRecipeFromInventory(player, recipe, isInputMode);
            
            if (event.isShiftClick() && event.isRightClick()) {
                // Shift+右键：设置为无限制
                recipe.setMaxTradeCount(0);
                player.sendMessage("§a兑换次数限制已设置为无限制");
            } else if (event.isShiftClick() && event.isLeftClick()) {
                // Shift+左键：大幅增加 (+10)
                int current = recipe.getMaxTradeCount();
                recipe.setMaxTradeCount(current + 10);
                player.sendMessage("§a兑换次数增加10次，现在是 " + recipe.getMaxTradeCount() + " 次");
            } else if (event.isRightClick()) {
                // 右键：减少次数 (-1)
                int current = recipe.getMaxTradeCount();
                if (current > 0) {
                    recipe.setMaxTradeCount(current - 1);
                    player.sendMessage("§a兑换次数减少1次，现在是 " + recipe.getMaxTradeCount() + " 次");
                } else {
                    player.sendMessage("§c已经是无限制状态");
                }
            } else {
                // 左键：增加次数 (+1) 或设置为10
                int current = recipe.getMaxTradeCount();
                if (current == 0) {
                    recipe.setMaxTradeCount(10); // 默认设置为10
                    player.sendMessage("§a兑换次数限制已设置为 10 次");
                } else {
                    recipe.setMaxTradeCount(current + 1);
                    player.sendMessage("§a兑换次数增加1次，现在是 " + recipe.getMaxTradeCount() + " 次");
                }
            }
            
            // 重新打开编辑器
            openEditor(player);
            return;
        }
        
        // 设置次数重置方式按钮
        if (slot == 51) {
            event.setCancelled(true);
            
            // 先更新当前数据
            updateRecipeFromInventory(player, recipe, isInputMode);
            
            // 切换重置方式
            boolean currentMode = recipe.isCountResetsWithRefresh();
            recipe.setCountResetsWithRefresh(!currentMode);
            
            if (!currentMode) {
                player.sendMessage("§a次数重置方式已改为：随刷新重置");
            } else {
                player.sendMessage("§a次数重置方式已改为：永久累计");
            }
            
            // 重新打开编辑器
            openEditor(player);
            return;
        }
        
        // 检查是否是分界线
        int[] borderSlots = {4, 13, 22, 31, 40, 49};
        for (int borderSlot : borderSlots) {
            if (slot == borderSlot) {
                event.setCancelled(true);
                return;
            }
        }
        
        // 物品放置区域 - 允许玩家操作
        if (isValidItemSlot(slot, isInputMode)) {
            plugin.getLogger().info("允许在槽位 " + slot + " 放置物品");
            // 不取消事件，允许玩家放置/拿取物品
            // 延迟更新数据
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                updateRecipeFromInventory(player, recipe, isInputMode);
            }, 1L);
        } else {
            // 其他区域禁止操作
            plugin.getLogger().info("禁止在槽位 " + slot + " 操作");
            event.setCancelled(true);
        }
    }
    
    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!event.getView().getTitle().equals(EDITOR_TITLE)) {
            return;
        }
        
        Player player = (Player) event.getWhoClicked();
        UUID playerId = player.getUniqueId();
        
        if (!editingRecipes.containsKey(playerId)) {
            player.closeInventory();
            return;
        }
        
        TradeRecipe recipe = editingRecipes.get(playerId);
        boolean isInputMode = editModes.get(playerId);
        
        // 检查所有被拖动的槽位
        for (int rawSlot : event.getRawSlots()) {
            // 转换为相对于 inventory 的槽位
            Inventory inventory = event.getInventory();
            int slot = rawSlot;
            
            // 如果是玩家背包的槽位，跳过（允许拖动）
            if (rawSlot >= inventory.getSize()) {
                continue;
            }
            
            // 如果是按钮或分界线，取消整个拖动
            if (slot == 45 || slot == 46 || slot == 47 || slot == 48 || slot == 50 || slot == 51 || slot == 52 || slot == 53) {
                event.setCancelled(true);
                return;
            }
            
            int[] borderSlots = {4, 13, 22, 31, 40, 49};
            for (int borderSlot : borderSlots) {
                if (slot == borderSlot) {
                    event.setCancelled(true);
                    return;
                }
            }
            
            // 如果不是有效的物品槽，取消
            if (!isValidItemSlot(slot, isInputMode)) {
                event.setCancelled(true);
                return;
            }
        }
        
        // 允许拖动，延迟更新数据
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            updateRecipeFromInventory(player, recipe, isInputMode);
        }, 1L);
    }
    
    /**
     * 检查是否是有效的物品槽位
     */
    private boolean isValidItemSlot(int slot, boolean isInputMode) {
        if (isInputMode) {
            for (int i = 0; i < 24; i++) {
                if (getLeftSlot(i) == slot) {
                    return true;
                }
            }
        } else {
            for (int i = 0; i < 20; i++) {
                if (getRightSlot(i) == slot) {
                    return true;
                }
            }
        }
        return false;
    }
    
    /**
     * 从库存更新配方数据
     */
    private void updateRecipeFromInventory(Player player, TradeRecipe recipe, boolean isInputMode) {
        Inventory inventory = player.getOpenInventory().getTopInventory();
        List<ItemStackData> targetList = isInputMode ? recipe.getInputItems() : recipe.getOutputItems();
        
        // 清空现有数据
        targetList.clear();
        
        // 重新读取所有槽位的物品
        if (isInputMode) {
            for (int i = 0; i < 24; i++) {
                int slot = getLeftSlot(i);
                if (slot >= 0) {
                    ItemStack item = inventory.getItem(slot);
                    if (item != null && item.getType() != Material.AIR) {
                        targetList.add(new ItemStackData(item));
                    }
                }
            }
        } else {
            for (int i = 0; i < 20; i++) {
                int slot = getRightSlot(i);
                if (slot >= 0) {
                    ItemStack item = inventory.getItem(slot);
                    if (item != null && item.getType() != Material.AIR) {
                        targetList.add(new ItemStackData(item));
                    }
                }
            }
        }
    }
    
    /**
     * 循环切换刷新类型
     */
    private void cycleRefreshType(TradeRecipe recipe) {
        TradeRecipe.RefreshType[] types = TradeRecipe.RefreshType.values();
        int currentIndex = recipe.getRefreshType().ordinal();
        int nextIndex = (currentIndex + 1) % types.length;
        recipe.setRefreshType(types[nextIndex]);
        
        if (types[nextIndex] == TradeRecipe.RefreshType.CUSTOM) {
            recipe.setRefreshInterval(3600); // 默认1小时
        }
    }
    
    /**
     * 保存配方
     */
    private void saveRecipe(Player player, TradeRecipe recipe) {
        if (recipe.getInputItems().isEmpty() || recipe.getOutputItems().isEmpty()) {
            player.sendMessage("§c输入和输出物品都不能为空！");
            return;
        }
        
        UUID playerId = player.getUniqueId();
        
        // 标记为正在保存，防止关闭事件处理
        refreshingPlayers.add(playerId);
        
        configManager.saveRecipe(recipe);
        player.sendMessage("§a配方已保存: §e" + recipe.getId());
        
        // 清空 GUI 中的物品（避免关闭时返回）
        Inventory inv = player.getOpenInventory().getTopInventory();
        inv.clear();
        
        player.closeInventory();
        
        editingRecipes.remove(playerId);
        editModes.remove(playerId);
        
        // 延迟移除刷新标记
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            refreshingPlayers.remove(playerId);
        }, 2L);
    }
    
    /**
     * 删除配方
     */
    private void deleteRecipe(Player player, String recipeId) {
        UUID playerId = player.getUniqueId();
        
        // 标记为正在删除，防止关闭事件处理
        refreshingPlayers.add(playerId);
        
        if (configManager.deleteRecipe(recipeId)) {
            player.sendMessage("§a配方已删除: §e" + recipeId);
            
            // 清空 GUI
            Inventory inv = player.getOpenInventory().getTopInventory();
            inv.clear();
            
            player.closeInventory();
            
            editingRecipes.remove(playerId);
            editModes.remove(playerId);
            
            // 延迟移除刷新标记
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                refreshingPlayers.remove(playerId);
            }, 2L);
        } else {
            player.sendMessage("§c删除失败！");
        }
    }
    
    /**
     * 克隆配方
     */
    private TradeRecipe cloneRecipe(TradeRecipe original) {
        TradeRecipe clone = new TradeRecipe(original.getId());
        clone.setRefreshType(original.getRefreshType());
        clone.setRefreshInterval(original.getRefreshInterval());
        clone.setTradeTitle(original.getTradeTitle());
        clone.setTradeSuccessMessage(original.getTradeSuccessMessage());
        
        for (ItemStackData itemData : original.getInputItems()) {
            clone.getInputItems().add(new ItemStackData(itemData.toItemStack()));
        }
        
        for (ItemStackData itemData : original.getOutputItems()) {
            clone.getOutputItems().add(new ItemStackData(itemData.toItemStack()));
        }
        
        return clone;
    }
    
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!event.getView().getTitle().equals(EDITOR_TITLE)) {
            return;
        }
        
        Player player = (Player) event.getPlayer();
        UUID playerId = player.getUniqueId();
        
        // 如果正在刷新界面，不处理关闭事件（物品已经在新的 inventory 中）
        if (refreshingPlayers.contains(playerId)) {
            plugin.getLogger().info("玩家 " + player.getName() + " 正在刷新界面，跳过关闭事件");
            return;
        }
        
        // 在清理之前，先将 GUI 中的物品返回给玩家
        Inventory topInventory = event.getInventory();
        
        // 收集所有**有效物品槽位**的物品（排除按钮和分界线）
        List<ItemStack> itemsToReturn = new ArrayList<>();
        
        // 定义需要排除的槽位（按钮和分界线）
        Set<Integer> excludedSlots = new HashSet<>();
        // 按钮槽位
        excludedSlots.add(45); // 确认
        excludedSlots.add(46); // 切换侧
        excludedSlots.add(47); // 刷新类型
        excludedSlots.add(48); // 设置标题
        excludedSlots.add(50); // 设置次数
        excludedSlots.add(51); // 设置重置方式
        excludedSlots.add(52); // 设置提示
        excludedSlots.add(53); // 删除
        // 分界线槽位
        excludedSlots.add(4);
        excludedSlots.add(13);
        excludedSlots.add(22);
        excludedSlots.add(31);
        excludedSlots.add(40);
        excludedSlots.add(49);
        
        // 遍历整个 inventory，只收集有效物品槽位的物品
        for (int slot = 0; slot < topInventory.getSize(); slot++) {
            // 跳过排除的槽位
            if (excludedSlots.contains(slot)) {
                continue;
            }
            
            ItemStack item = topInventory.getItem(slot);
            if (item != null && item.getType() != Material.AIR) {
                itemsToReturn.add(item);
            }
        }
        
        // 将物品返回给玩家
        for (ItemStack item : itemsToReturn) {
            player.getInventory().addItem(item);
        }
        
        // 通知玩家
        if (!itemsToReturn.isEmpty()) {
            player.sendMessage("§e编辑器已关闭，" + itemsToReturn.size() + " 个物品已返回背包");
        }
        
        // 清理编辑状态
        editingRecipes.remove(playerId);
        editModes.remove(playerId);
    }
}

