package hhl.trade.manager;

import hhl.trade.Trade;
import hhl.trade.model.ItemStackData;
import hhl.trade.model.TradeRecipe;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * 配置文件管理器
 */
public class ConfigManager {
    private final Trade plugin;
    private final File recipesFolder;
    private final Map<String, TradeRecipe> recipeCache;
    
    public ConfigManager(Trade plugin) {
        this.plugin = plugin;
        this.recipesFolder = new File(plugin.getDataFolder(), "recipes");
        this.recipeCache = new java.util.LinkedHashMap<>();  // 使用 LinkedHashMap 保持插入顺序
        
        // 创建配方文件夹
        if (!recipesFolder.exists()) {
            recipesFolder.mkdirs();
        }
    }
    
    /**
     * 加载所有配方
     */
    public void loadAllRecipes() {
        recipeCache.clear();
        
        File[] files = recipesFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null || files.length == 0) {
            return;
        }
        
        for (File file : files) {
            try {
                String recipeId = file.getName().replace(".yml", "");
                TradeRecipe recipe = loadRecipe(file);
                if (recipe != null) {
                    recipeCache.put(recipeId, recipe);
                }
            } catch (Exception e) {
                plugin.getLogger().warning("加载配方文件失败: " + file.getName());
                e.printStackTrace();
            }
        }
        
        plugin.getLogger().info("已加载 " + recipeCache.size() + " 个交易配方");
    }
    
    /**
     * 从文件加载单个配方
     */
    public TradeRecipe loadRecipe(File file) {
        if (!file.exists()) {
            return null;
        }
        
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        String recipeId = file.getName().replace(".yml", "");
        
        return TradeRecipe.fromConfig(recipeId, config);
    }
    
    /**
     * 保存配方到文件
     */
    public void saveRecipe(TradeRecipe recipe) {
        if (recipe == null) {
            return;
        }
        
        File file = new File(recipesFolder, recipe.getId() + ".yml");
        FileConfiguration config = new YamlConfiguration();
        
        recipe.toConfig(config);
        
        try {
            config.save(file);
            recipeCache.put(recipe.getId(), recipe);
        } catch (IOException e) {
            plugin.getLogger().severe("保存配方失败: " + recipe.getId());
            e.printStackTrace();
        }
    }
    
    /**
     * 删除配方文件
     */
    public boolean deleteRecipe(String recipeId) {
        File file = new File(recipesFolder, recipeId + ".yml");
        if (file.exists()) {
            recipeCache.remove(recipeId);
            return file.delete();
        }
        return false;
    }
    
    /**
     * 获取所有配方
     */
    public Map<String, TradeRecipe> getAllRecipes() {
        return Collections.unmodifiableMap(recipeCache);
    }
    
    /**
     * 根据ID获取配方
     */
    public TradeRecipe getRecipe(String recipeId) {
        return recipeCache.get(recipeId);
    }
    
    /**
     * 生成新的配方ID
     */
    public String generateRecipeId() {
        int id = 1;
        while (recipeCache.containsKey("trade_" + id)) {
            id++;
        }
        return "trade_" + id;
    }
    
    /**
     * 检查配方ID是否存在
     */
    public boolean existsRecipe(String recipeId) {
        return recipeCache.containsKey(recipeId);
    }
}
