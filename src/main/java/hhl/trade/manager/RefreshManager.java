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
    
    public RefreshManager(Trade plugin) {
        this.plugin = plugin;
        this.cooldownFile = new File(plugin.getDataFolder(), "cooldowns.yml");
        this.playerCooldowns = new HashMap<>();
        
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
                    recipeCooldowns.put(recipeId, section.getLong(recipeId));
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
            String uuidStr = entry.getKey().toString();
            for (Map.Entry<String, Long> cooldownEntry : entry.getValue().entrySet()) {
                cooldownConfig.set(uuidStr + "." + cooldownEntry.getKey(), cooldownEntry.getValue());
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
        if (recipe.getRefreshType() == TradeRecipe.RefreshType.NONE) {
            return true;
        }
        
        Map<String, Long> playerRecipes = playerCooldowns.get(playerId);
        if (playerRecipes == null || !playerRecipes.containsKey(recipe.getId())) {
            return true;
        }
        
        long lastTradeTime = playerRecipes.get(recipe.getId());
        long now = System.currentTimeMillis();
        
        return getNextRefreshTime(recipe, lastTradeTime) <= now;
    }
    
    /**
     * 记录交易时间
     */
    public void recordTrade(UUID playerId, String recipeId) {
        playerCooldowns.computeIfAbsent(playerId, k -> new HashMap<>())
                      .put(recipeId, System.currentTimeMillis());
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
