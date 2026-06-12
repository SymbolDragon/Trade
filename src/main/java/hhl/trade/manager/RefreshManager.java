package hhl.trade.manager;

import hhl.trade.Trade;
import hhl.trade.model.TradeRecipe;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.time.*;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 时间刷新管理器，管理玩家交易冷却时间
 */
public class RefreshManager {
    private final Trade plugin;
    private final File cooldownFile;
    private FileConfiguration cooldownConfig;
    
    // 玩家ID -> 配方ID -> 上次交易时间戳
    private Map<UUID, Map<String, Long>> playerCooldowns;
    
    // 玩家ID -> 配方ID -> 已兑换次数
    private Map<UUID, Map<String, Integer>> playerTradeCounts;
    
    public RefreshManager(Trade plugin) {
        this.plugin = plugin;
        this.cooldownFile = new File(plugin.getDataFolder(), "cooldowns.yml");
        this.playerCooldowns = new HashMap<>();
        this.playerTradeCounts = new HashMap<>();
        
        loadCooldowns();
    }
    
    /**
     * 加载冷却时间数据
     */
    private void loadCooldowns() {
        if (!cooldownFile.exists()) {
            try {
                cooldownFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("创建冷却文件失败");
                e.printStackTrace();
            }
        }
        
        cooldownConfig = YamlConfiguration.loadConfiguration(cooldownFile);
        
        // 从文件加载到内存
        for (String uuidStr : cooldownConfig.getKeys(false)) {
            UUID uuid = UUID.fromString(uuidStr);
            Map<String, Long> recipeCooldowns = new HashMap<>();
            
            ConfigurationSection section = cooldownConfig.getConfigurationSection(uuidStr);
            if (section != null) {
                for (String recipeId : section.getKeys(false)) {
                    // 加载时间戳
                    if (recipeId.endsWith("_time")) {
                        String realRecipeId = recipeId.replace("_time", "");
                        recipeCooldowns.put(realRecipeId, section.getLong(recipeId));
                    }
                    // 加载次数
                    else if (recipeId.endsWith("_count")) {
                        String realRecipeId = recipeId.replace("_count", "");
                        playerTradeCounts.computeIfAbsent(uuid, k -> new HashMap<>())
                                        .put(realRecipeId, section.getInt(recipeId));
                    }
                }
            }
            
            playerCooldowns.put(uuid, recipeCooldowns);
        }
    }
    
    /**
     * 保存冷却时间数据
     */
    public void saveCooldowns() {
        cooldownConfig = new YamlConfiguration();
        
        for (Map.Entry<UUID, Map<String, Long>> entry : playerCooldowns.entrySet()) {
            UUID uuid = entry.getKey();
            String uuidStr = uuid.toString();
            
            // 保存时间戳
            for (Map.Entry<String, Long> cooldownEntry : entry.getValue().entrySet()) {
                cooldownConfig.set(uuidStr + "." + cooldownEntry.getKey() + "_time", cooldownEntry.getValue());
            }
            
            // 保存次数
            Map<String, Integer> counts = playerTradeCounts.get(uuid);
            if (counts != null) {
                for (Map.Entry<String, Integer> countEntry : counts.entrySet()) {
                    cooldownConfig.set(uuidStr + "." + countEntry.getKey() + "_count", countEntry.getValue());
                }
            }
        }
        
        try {
            cooldownConfig.save(cooldownFile);
        } catch (IOException e) {
            plugin.getLogger().severe("保存冷却数据失败");
            e.printStackTrace();
        }
    }
    
    /**
     * 检查是否可以交易
     */
    public boolean canTrade(UUID playerId, TradeRecipe recipe) {
        // 检查时间限制
        if (recipe.getRefreshType() != TradeRecipe.RefreshType.NONE) {
            Map<String, Long> playerRecipes = playerCooldowns.get(playerId);
            if (playerRecipes != null && playerRecipes.containsKey(recipe.getId())) {
                long lastTradeTime = playerRecipes.get(recipe.getId());
                long now = System.currentTimeMillis();
                
                if (getNextRefreshTime(recipe, lastTradeTime) > now) {
                    return false; // 还在冷却中
                }
                
                // 如果已刷新，重置次数
                if (recipe.isCountResetsWithRefresh()) {
                    resetTradeCount(playerId, recipe.getId());
                }
            }
        }
        
        // 检查次数限制
        if (recipe.getMaxTradeCount() > 0) {
            int currentCount = getTradeCount(playerId, recipe.getId());
            if (currentCount >= recipe.getMaxTradeCount()) {
                return false; // 次数已用完
            }
        }
        
        return true;
    }
    
    /**
     * 记录交易时间
     */
    public void recordTrade(UUID playerId, String recipeId) {
        playerCooldowns.computeIfAbsent(playerId, k -> new HashMap<>())
                      .put(recipeId, System.currentTimeMillis());
        
        // 增加兑换次数
        Map<String, Integer> counts = playerTradeCounts.computeIfAbsent(playerId, k -> new HashMap<>());
        counts.merge(recipeId, 1, Integer::sum);
    }
    
    /**
     * 获取当前兑换次数
     */
    public int getTradeCount(UUID playerId, String recipeId) {
        Map<String, Integer> counts = playerTradeCounts.get(playerId);
        if (counts == null) {
            return 0;
        }
        return counts.getOrDefault(recipeId, 0);
    }
    
    /**
     * 重置兑换次数
     */
    public void resetTradeCount(UUID playerId, String recipeId) {
        Map<String, Integer> counts = playerTradeCounts.get(playerId);
        if (counts != null) {
            counts.remove(recipeId);
        }
    }
    
    /**
     * 获取剩余兑换次数
     */
    public int getRemainingTradeCount(UUID playerId, TradeRecipe recipe) {
        if (recipe.getMaxTradeCount() <= 0) {
            return -1; // 无限制
        }
        
        int currentCount = getTradeCount(playerId, recipe.getId());
        return Math.max(0, recipe.getMaxTradeCount() - currentCount);
    }
    
    /**
     * 获取下次可交易时间
     */
    public long getNextTradeTime(UUID playerId, TradeRecipe recipe) {
        Map<String, Long> playerRecipes = playerCooldowns.get(playerId);
        if (playerRecipes == null || !playerRecipes.containsKey(recipe.getId())) {
            return 0;
        }
        
        long lastTradeTime = playerRecipes.get(recipe.getId());
        return getNextRefreshTime(recipe, lastTradeTime);
    }
    
    /**
     * 计算下次刷新时间
     */
    private long getNextRefreshTime(TradeRecipe recipe, long lastTradeTime) {
        switch (recipe.getRefreshType()) {
            case DAILY:
                return getNextDailyRefresh(lastTradeTime);
            case WEEKLY:
                return getNexWeeklyRefresh(lastTradeTime);
            case MONTHLY:
                return getNextMonthlyRefresh(lastTradeTime);
            case CUSTOM:
                return lastTradeTime + (recipe.getRefreshInterval() * 1000);
            default:
                return 0;
        }
    }
    
    /**
     * 获取每天0点的刷新时间
     */
    private long getNextDailyRefresh(long lastTradeTime) {
        LocalDateTime lastDateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(lastTradeTime), ZoneId.systemDefault());
        LocalDateTime nextMidnight = lastDateTime.toLocalDate().plusDays(1).atStartOfDay();
        return nextMidnight.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }
    
    /**
     * 获取每周第一天（周一）0点的刷新时间
     */
    private long getNexWeeklyRefresh(long lastTradeTime) {
        LocalDateTime lastDateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(lastTradeTime), ZoneId.systemDefault());
        DayOfWeek currentDay = lastDateTime.getDayOfWeek();
        int daysUntilMonday = DayOfWeek.MONDAY.getValue() - currentDay.getValue();
        if (daysUntilMonday <= 0) {
            daysUntilMonday += 7;
        }
        LocalDateTime nextMonday = lastDateTime.toLocalDate().plusDays(daysUntilMonday).atStartOfDay();
        return nextMonday.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }
    
    /**
     * 获取每月第一天0点的刷新时间
     */
    private long getNextMonthlyRefresh(long lastTradeTime) {
        LocalDateTime lastDateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(lastTradeTime), ZoneId.systemDefault());
        YearMonth currentMonth = YearMonth.from(lastDateTime);
        LocalDateTime nextFirstDay = currentMonth.plusMonths(1).atDay(1).atStartOfDay();
        return nextFirstDay.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }
    
    /**
     * 获取剩余冷却时间（秒）
     */
    public long getRemainingCooldownSeconds(UUID playerId, TradeRecipe recipe) {
        long nextTradeTime = getNextTradeTime(playerId, recipe);
        if (nextTradeTime == 0) {
            return 0;
        }
        
        long remaining = nextTradeTime - System.currentTimeMillis();
        return Math.max(0, remaining / 1000);
    }
}
