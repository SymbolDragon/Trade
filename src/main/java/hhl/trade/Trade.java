package hhl.trade;

import hhl.trade.command.TradeCommand;
import hhl.trade.gui.TradeEditorGUI;
import hhl.trade.gui.TradeGUI;
import hhl.trade.manager.ConfigManager;
import hhl.trade.manager.RefreshManager;
import org.bukkit.plugin.java.JavaPlugin;

public final class Trade extends JavaPlugin {

    private ConfigManager configManager;
    private RefreshManager refreshManager;
    private TradeGUI tradeGUI;
    private TradeEditorGUI editorGUI;

    @Override
    public void onEnable() {
        // 调试信息
        getLogger().info("插件数据文件夹: " + getDataFolder().getAbsolutePath());
        getLogger().info("配置文件位置: " + new java.io.File(getDataFolder(), "paper-plugin.yml").getAbsolutePath());
        
        // 初始化配置管理器
        configManager = new ConfigManager(this);
        
        // 初始化刷新管理器
        refreshManager = new RefreshManager(this);
        
        // 初始化GUI
        tradeGUI = new TradeGUI(this, configManager, refreshManager);
        editorGUI = new TradeEditorGUI(this, configManager);
        
        // 注册命令
        TradeCommand command = new TradeCommand(this, tradeGUI, editorGUI);
        
        // 获取命令并检查是否为null
        org.bukkit.command.PluginCommand tradeCommand = getCommand("trade");
        if (tradeCommand == null) {
            getLogger().severe("===========================================");
            getLogger().severe("无法注册命令 'trade'！");
            getLogger().severe("可能的原因：");
            getLogger().severe("1. paper-plugin.yml 文件格式错误");
            getLogger().severe("2. YAML 缩进问题（使用了Tab而非空格）");
            getLogger().severe("3. version 字段包含未替换的变量");
            getLogger().severe("4. 文件编码问题");
            getLogger().severe("");
            getLogger().severe("请检查 jar 文件中的 paper-plugin.yml 内容");
            getLogger().severe("===========================================");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        
        tradeCommand.setExecutor(command);
        tradeCommand.setTabCompleter(command);
        getLogger().info("命令 'trade' 已成功注册");
        
        // 加载所有配方
        configManager.loadAllRecipes();
        
        getLogger().info("Trade插件已启用！");
    }

    @Override
    public void onDisable() {
        // 保存冷却数据
        if (refreshManager != null) {
            refreshManager.saveCooldowns();
        }
        
        getLogger().info("Trade插件已禁用！");
    }
    
    // Getters
    public ConfigManager getConfigManager() {
        return configManager;
    }
    
    public RefreshManager getRefreshManager() {
        return refreshManager;
    }
}
