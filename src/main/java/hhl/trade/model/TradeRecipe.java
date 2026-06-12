package hhl.trade.model;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * 交易配方数据模型
 */
public class TradeRecipe {
    private String id; // 配方ID
    private List<ItemStackData> inputItems; // 输入物品（左侧）
    private List<ItemStackData> outputItems; // 输出物品（右侧）
    private RefreshType refreshType; // 刷新类型
    private long refreshInterval; // 刷新间隔（秒，仅用于CUSTOM类型）
    private String tradeTitle; // 交易界面标题（可选，默认为"§6交易系统"）
    private String tradeSuccessMessage; // 交易成功提示（可选，默认为"§a交易成功！"）
    private int maxTradeCount; // 最大兑换次数（0表示无限制）
    private boolean countResetsWithRefresh; // 次数是否随刷新重置
    
    public enum RefreshType {
        DAILY,      // 每天0点刷新
        WEEKLY,     // 每周第一天0点刷新
        MONTHLY,    // 每月第一天0点刷新
        CUSTOM,     // 自定义间隔
        NONE        // 无限制
    }
    
    public TradeRecipe() {
        this.inputItems = new ArrayList<>();
        this.outputItems = new ArrayList<>();
        this.refreshType = RefreshType.NONE;
        this.refreshInterval = 0;
        this.tradeTitle = "§6交易系统"; // 默认标题
        this.tradeSuccessMessage = "§a交易成功！"; // 默认提示
        this.maxTradeCount = 0; // 默认无限制
        this.countResetsWithRefresh = true; // 默认随刷新重置
    }
    
    public TradeRecipe(String id) {
        this();
        this.id = id;
    }
    
    /**
     * 从配置段加载配方
     */
    public static TradeRecipe fromConfig(String id, ConfigurationSection config) {
        TradeRecipe recipe = new TradeRecipe(id);
        
        // 加载输入物品
        if (config.contains("input")) {
            ConfigurationSection inputSection = config.getConfigurationSection("input");
            if (inputSection != null) {
                for (String key : inputSection.getKeys(false)) {
                    ConfigurationSection itemSection = inputSection.getConfigurationSection(key);
                    if (itemSection != null) {
                        recipe.inputItems.add(ItemStackData.fromConfig(itemSection));
                    }
                }
            }
        }
        
        // 加载输出物品
        if (config.contains("output")) {
            ConfigurationSection outputSection = config.getConfigurationSection("output");
            if (outputSection != null) {
                for (String key : outputSection.getKeys(false)) {
                    ConfigurationSection itemSection = outputSection.getConfigurationSection(key);
                    if (itemSection != null) {
                        recipe.outputItems.add(ItemStackData.fromConfig(itemSection));
                    }
                }
            }
        }
        
        // 加载刷新设置
        if (config.contains("refresh.type")) {
            try {
                recipe.refreshType = RefreshType.valueOf(config.getString("refresh.type", "NONE").toUpperCase());
            } catch (IllegalArgumentException e) {
                recipe.refreshType = RefreshType.NONE;
            }
        }
        
        if (config.contains("refresh.interval")) {
            recipe.refreshInterval = config.getLong("refresh.interval", 3600);
        }
        
        // 加载自定义标题和提示（如果存在）
        if (config.contains("trade_title")) {
            recipe.tradeTitle = config.getString("trade_title", "§6交易系统");
        }
        
        if (config.contains("trade_success_message")) {
            recipe.tradeSuccessMessage = config.getString("trade_success_message", "§a交易成功！");
        }
        
        // 加载次数限制设置
        if (config.contains("max_trade_count")) {
            recipe.maxTradeCount = config.getInt("max_trade_count", 0);
        }
        
        if (config.contains("count_resets_with_refresh")) {
            recipe.countResetsWithRefresh = config.getBoolean("count_resets_with_refresh", true);
        }
        
        return recipe;
    }
    
    /**
     * 保存到配置段
     */
    public ConfigurationSection toConfig(ConfigurationSection config) {
        config.set("id", id);
        
        // 保存输入物品
        List<ConfigurationSection> inputSections = new ArrayList<>();
        for (ItemStackData itemData : inputItems) {
            ConfigurationSection section = config.createSection("input." + inputSections.size());
            itemData.toConfig(section);
            inputSections.add(section);
        }
        
        // 保存输出物品
        List<ConfigurationSection> outputSections = new ArrayList<>();
        for (ItemStackData itemData : outputItems) {
            ConfigurationSection section = config.createSection("output." + outputSections.size());
            itemData.toConfig(section);
            outputSections.add(section);
        }
        
        // 保存刷新设置
        config.set("refresh.type", refreshType.name());
        if (refreshType == RefreshType.CUSTOM) {
            config.set("refresh.interval", refreshInterval);
        }
        
        // 保存自定义标题和提示（仅在非默认值时保存）
        if (!tradeTitle.equals("§6交易系统")) {
            config.set("trade_title", tradeTitle);
        }
        
        if (!tradeSuccessMessage.equals("§a交易成功！")) {
            config.set("trade_success_message", tradeSuccessMessage);
        }
        
        // 保存次数限制设置（仅在非默认值时保存）
        if (maxTradeCount > 0) {
            config.set("max_trade_count", maxTradeCount);
            config.set("count_resets_with_refresh", countResetsWithRefresh);
        }
        
        return config;
    }
    
    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public List<ItemStackData> getInputItems() { return inputItems; }
    public void setInputItems(List<ItemStackData> inputItems) { this.inputItems = inputItems; }
    
    public List<ItemStackData> getOutputItems() { return outputItems; }
    public void setOutputItems(List<ItemStackData> outputItems) { this.outputItems = outputItems; }
    
    public RefreshType getRefreshType() { return refreshType; }
    public void setRefreshType(RefreshType refreshType) { this.refreshType = refreshType; }
    
    public long getRefreshInterval() { return refreshInterval; }
    public void setRefreshInterval(long refreshInterval) { this.refreshInterval = refreshInterval; }
    
    public String getTradeTitle() { return tradeTitle; }
    public void setTradeTitle(String tradeTitle) { this.tradeTitle = tradeTitle; }
    
    public String getTradeSuccessMessage() { return tradeSuccessMessage; }
    public void setTradeSuccessMessage(String tradeSuccessMessage) { this.tradeSuccessMessage = tradeSuccessMessage; }
    
    public int getMaxTradeCount() { return maxTradeCount; }
    public void setMaxTradeCount(int maxTradeCount) { this.maxTradeCount = maxTradeCount; }
    
    public boolean isCountResetsWithRefresh() { return countResetsWithRefresh; }
    public void setCountResetsWithRefresh(boolean countResetsWithRefresh) { this.countResetsWithRefresh = countResetsWithRefresh; }
}
