package hhl.trade.model;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;

/**
 * 物品数据模型，用于序列化/反序列化ItemStack
 * 支持完整 NBT 数据保存（包括 MythicMobs 等插件的自定义数据）
 */
public class ItemStackData {
    private Material material;
    private int amount;
    private String displayName;
    private List<String> lore;
    private Map<String, Integer> enchantments;
    private boolean unbreakable;
    private String customModelData;
    
    // 完整的 NBT 数据（Base64 编码）
    private String nbtData;
    
    public ItemStackData() {
        this.material = Material.AIR;
        this.amount = 1;
        this.lore = new ArrayList<>();
        this.enchantments = new HashMap<>();
        this.unbreakable = false;
    }
    
    public ItemStackData(ItemStack itemStack) {
        // 先初始化所有字段
        this.material = Material.AIR;
        this.amount = 0;
        this.lore = new ArrayList<>();
        this.enchantments = new HashMap<>();
        this.unbreakable = false;
        
        if (itemStack == null || itemStack.getType() == Material.AIR) {
            return;
        }
        
        this.material = itemStack.getType();
        this.amount = itemStack.getAmount();
        
        // 保存完整的 NBT 数据（优先）
        this.nbtData = serializeItemStack(itemStack);
        
        // 同时也保存基本属性（用于兼容和调试）
        ItemMeta meta = itemStack.getItemMeta();
        if (meta != null) {
            // 显示名称
            if (meta.hasDisplayName()) {
                this.displayName = meta.getDisplayName();
            }
            
            // Lore
            if (meta.hasLore()) {
                this.lore = meta.getLore();
            }
            
            // 附魔
            if (meta.hasEnchants()) {
                for (Map.Entry<Enchantment, Integer> entry : meta.getEnchants().entrySet()) {
                    this.enchantments.put(entry.getKey().getKey().toString(), entry.getValue());
                }
            }
            
            // 不可破坏
            this.unbreakable = meta.isUnbreakable();
            
            // 自定义模型数据
            if (meta.hasCustomModelData()) {
                this.customModelData = String.valueOf(meta.getCustomModelData());
            }
        }
    }
    
    /**
     * 从配置段加载物品数据
     */
    public static ItemStackData fromConfig(ConfigurationSection config) {
        ItemStackData data = new ItemStackData();
        
        String materialName = config.getString("material", "AIR");
        try {
            data.material = Material.valueOf(materialName.toUpperCase());
        } catch (IllegalArgumentException e) {
            data.material = Material.AIR;
        }
        
        data.amount = config.getInt("amount", 1);
        
        // 优先加载 NBT 数据
        data.nbtData = config.getString("nbt_data");
        
        // 也加载基本属性（用于兼容）
        data.displayName = config.getString("display_name");
        data.lore = config.getStringList("lore");
        data.unbreakable = config.getBoolean("unbreakable", false);
        data.customModelData = config.getString("custom_model_data");
        
        // 加载附魔
        ConfigurationSection enchantsSection = config.getConfigurationSection("enchantments");
        if (enchantsSection != null) {
            for (String key : enchantsSection.getKeys(false)) {
                data.enchantments.put(key, enchantsSection.getInt(key));
            }
        }
        
        return data;
    }
    
    /**
     * 保存到配置段
     */
    public void toConfig(ConfigurationSection config) {
        config.set("material", material.name());
        config.set("amount", amount);
        
        // 保存完整的 NBT 数据（优先）
        if (nbtData != null && !nbtData.isEmpty()) {
            config.set("nbt_data", nbtData);
        }
        
        // 也保存基本属性（用于兼容和调试）
        if (displayName != null && !displayName.isEmpty()) {
            config.set("display_name", displayName);
        }
        
        if (lore != null && !lore.isEmpty()) {
            config.set("lore", lore);
        }
        
        if (!enchantments.isEmpty()) {
            ConfigurationSection enchantsSection = config.createSection("enchantments");
            for (Map.Entry<String, Integer> entry : enchantments.entrySet()) {
                enchantsSection.set(entry.getKey(), entry.getValue());
            }
        }
        
        if (unbreakable) {
            config.set("unbreakable", true);
        }
        
        if (customModelData != null && !customModelData.isEmpty()) {
            config.set("custom_model_data", customModelData);
        }
    }
    
    /**
     * 转换为 Bukkit ItemStack
     * 优先使用 NBT 数据（保留完整信息），如果没有则使用基本属性
     */
    public ItemStack toItemStack() {
        if (material == Material.AIR || amount <= 0) {
            return null;
        }
            
        // 如果有完整的 NBT 数据，优先使用
        if (nbtData != null && !nbtData.isEmpty()) {
            ItemStack item = deserializeItemStack(nbtData);
            if (item != null) {
                return item;
            }
            // 如果反序列化失败，继续使用基本属性
        }
            
        // 使用基本属性构建物品（兼容旧数据）
        ItemStack item = new ItemStack(material, amount);
        ItemMeta meta = item.getItemMeta();
            
        if (meta != null) {
            // 设置显示名称
            if (displayName != null && !displayName.isEmpty()) {
                meta.setDisplayName(displayName);
            }
                
            // 设置 Lore
            if (lore != null && !lore.isEmpty()) {
                meta.setLore(lore);
            }
                
            // 添加附魔
            for (Map.Entry<String, Integer> entry : enchantments.entrySet()) {
                Enchantment enchantment = Enchantment.getByKey(org.bukkit.NamespacedKey.fromString(entry.getKey()));
                if (enchantment != null) {
                    meta.addEnchant(enchantment, entry.getValue(), true);
                }
            }
                
            // 设置不可破坏
            meta.setUnbreakable(unbreakable);
                
            // 设置自定义模型数据
            if (customModelData != null && !customModelData.isEmpty()) {
                try {
                    meta.setCustomModelData(Integer.parseInt(customModelData));
                } catch (NumberFormatException e) {
                    // 忽略无效的自定义模型数据
                }
            }
                
            item.setItemMeta(meta);
        }
            
        return item;
    }
    
    // Getters and Setters
    public Material getMaterial() { return material; }
    public void setMaterial(Material material) { this.material = material; }
    
    public int getAmount() { return amount; }
    public void setAmount(int amount) { this.amount = amount; }
    
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    
    public List<String> getLore() { return lore; }
    public void setLore(List<String> lore) { this.lore = lore; }
    
    public Map<String, Integer> getEnchantments() { return enchantments; }
    public void setEnchantments(Map<String, Integer> enchantments) { this.enchantments = enchantments; }
    
    public boolean isUnbreakable() { return unbreakable; }
    public void setUnbreakable(boolean unbreakable) { this.unbreakable = unbreakable; }
    
    public String getCustomModelData() { return customModelData; }
    public void setCustomModelData(String customModelData) { this.customModelData = customModelData; }
    
    public String getNbtData() { return nbtData; }
    public void setNbtData(String nbtData) { this.nbtData = nbtData; }
    
    /**
     * 将 ItemStack 序列化为 Base64 字符串（包含完整 NBT 数据）
     */
    private static String serializeItemStack(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) {
            return null;
        }
        
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            BukkitObjectOutputStream dataOutput = new BukkitObjectOutputStream(outputStream);
            
            // 写入物品
            dataOutput.writeObject(item);
            dataOutput.close();
            
            // 转换为 Base64
            return Base64.getEncoder().encodeToString(outputStream.toByteArray());
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * 从 Base64 字符串反序列化为 ItemStack
     */
    private static ItemStack deserializeItemStack(String data) {
        if (data == null || data.isEmpty()) {
            return null;
        }
        
        try {
            byte[] bytes = Base64.getDecoder().decode(data);
            ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes);
            BukkitObjectInputStream dataInput = new BukkitObjectInputStream(inputStream);
            
            // 读取物品
            ItemStack item = (ItemStack) dataInput.readObject();
            dataInput.close();
            
            return item;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
