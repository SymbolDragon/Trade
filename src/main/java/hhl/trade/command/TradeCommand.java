package hhl.trade.command;

import hhl.trade.Trade;
import hhl.trade.gui.TradeEditorGUI;
import hhl.trade.gui.TradeGUI;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * 交易命令处理器
 */
public class TradeCommand implements CommandExecutor, TabCompleter {
    private final Trade plugin;
    private final TradeGUI tradeGUI;
    private final TradeEditorGUI editorGUI;
    
    public TradeCommand(Trade plugin, TradeGUI tradeGUI, TradeEditorGUI editorGUI) {
        this.plugin = plugin;
        this.tradeGUI = tradeGUI;
        this.editorGUI = editorGUI;
    }
    
    @Override
    public boolean onCommand(org.bukkit.command.CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§c此命令只能由玩家执行！");
            return true;
        }
        
        Player player = (Player) sender;
        
        if (args.length == 0) {
            // /trade - 打开交易列表
            tradeGUI.openTradeList(player);
            return true;
        }
        
        String subCommand = args[0].toLowerCase();
        
        switch (subCommand) {
            case "open":
            case "o":
                // /trade open <recipeId> - 打开指定配方交易
                if (args.length < 2) {
                    player.sendMessage("§c用法: /trade open <配方ID>");
                    return true;
                }
                String recipeId = args[1];
                tradeGUI.openTrade(player, recipeId);
                break;
                
            case "edit":
            case "e":
                // /trade edit [recipeId] - 编辑配方
                if (!player.hasPermission("trade.admin")) {
                    player.sendMessage("§c你没有权限执行此命令！");
                    return true;
                }
                
                if (args.length >= 2) {
                    editorGUI.openEditEditor(player, args[1]);
                } else {
                    editorGUI.openNewEditor(player);
                }
                break;
                
            case "list":
            case "l":
                // /trade list - 列出所有配方
                if (!player.hasPermission("trade.admin")) {
                    player.sendMessage("§c你没有权限执行此命令！");
                    return true;
                }
                
                listRecipes(player);
                break;
                
            case "reload":
            case "r":
                // /trade reload - 重新加载配置
                if (!player.hasPermission("trade.admin")) {
                    player.sendMessage("§c你没有权限执行此命令！");
                    return true;
                }
                
                reloadConfig(player);
                break;
                
            case "help":
            case "h":
                // /trade help - 显示帮助
                showHelp(player);
                break;
                
            default:
                player.sendMessage("§c未知子命令！使用 /trade help 查看帮助");
                break;
        }
        
        return true;
    }
    
    /**
     * 列出所有配方
     */
    private void listRecipes(Player player) {
        java.util.Map<String, hhl.trade.model.TradeRecipe> recipes = plugin.getConfigManager().getAllRecipes();
        
        if (recipes.isEmpty()) {
            player.sendMessage("§c当前没有配方！");
            return;
        }
        
        player.sendMessage("§6==== 交易配方列表 ====");
        for (java.util.Map.Entry<String, hhl.trade.model.TradeRecipe> entry : recipes.entrySet()) {
            player.sendMessage("§e- " + entry.getKey() + " §7(输入: " + 
                             entry.getValue().getInputItems().size() + 
                             ", 输出: " + 
                             entry.getValue().getOutputItems().size() + ")");
        }
        player.sendMessage("§6========================");
    }
    
    /**
     * 重新加载配置
     */
    private void reloadConfig(Player player) {
        plugin.getConfigManager().loadAllRecipes();
        player.sendMessage("§a配置已重新加载！");
    }
    
    /**
     * 显示帮助信息
     */
    private void showHelp(Player player) {
        player.sendMessage("§6==== 交易系统帮助 ====");
        player.sendMessage("§e/trade §7- 打开交易列表");
        player.sendMessage("§e/trade open <配方ID> §7- 打开指定配方");
        
        if (player.hasPermission("trade.admin")) {
            player.sendMessage("§e/trade edit [配方ID] §7- 编辑/新建配方");
            player.sendMessage("§e/trade list §7- 列出所有配方");
            player.sendMessage("§e/trade reload §7- 重新加载配置");
        }
        
        player.sendMessage("§e/trade help §7- 显示此帮助");
        player.sendMessage("§6========================");
    }
    
    @Override
    public List<String> onTabComplete(org.bukkit.command.CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            String[] subCommands = {"open", "edit", "list", "reload", "help"};
            for (String subCmd : subCommands) {
                if (subCmd.startsWith(args[0].toLowerCase())) {
                    completions.add(subCmd);
                }
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("open")) {
            // 配方ID自动补全
            for (String recipeId : plugin.getConfigManager().getAllRecipes().keySet()) {
                if (recipeId.toLowerCase().startsWith(args[1].toLowerCase())) {
                    completions.add(recipeId);
                }
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("edit")) {
            // 管理员可以编辑所有配方
            if (sender.hasPermission("trade.admin")) {
                for (String recipeId : plugin.getConfigManager().getAllRecipes().keySet()) {
                    if (recipeId.toLowerCase().startsWith(args[1].toLowerCase())) {
                        completions.add(recipeId);
                    }
                }
            }
        }
        
        return completions;
    }
}
