package mods.Hileb.curseforge;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.ConfigManager;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.event.entity.player.AnvilRepairEvent;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLLoadCompleteEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import org.apache.commons.lang3.RandomUtils;

import java.util.*;
import java.util.stream.Collectors;

@Mod(modid = Tags.MOD_ID, name = Tags.MOD_NAME, version = Tags.VERSION, acceptableRemoteVersions = "*")
@Mod.EventBusSubscriber
@Config(modid = Tags.MOD_ID)
public class CurseForge {

    @Config.Ignore
    public static List<Enchantment> CURES_ENCHANTMENTS = Lists.newArrayList();

    @Mod.EventHandler
    public void buildEnchantments(FMLLoadCompleteEvent event) {
        CURES_ENCHANTMENTS = ForgeRegistries.ENCHANTMENTS.getValuesCollection().stream().filter(CurseForge::isCursedEnchantment).collect(Collectors.toList());
    }

    public static boolean isCursedEnchantment(Enchantment ench) {
        return ench.isCurse() || "ivrench".equals(ench.getRegistryName().getNamespace());
    }

    public static Enchantment getCursedEnchantment(ItemStack stack) {
        int totalWeight = 0;
        for (Enchantment enchant : CURES_ENCHANTMENTS) {
            if (enchant.canApply(stack) && !isEnchantmentBlocked(enchant)) {
                totalWeight += enchant.getRarity().getWeight();
            }
        }

        if (totalWeight <= 0) return null;

        int randomValue = RandomUtils.nextInt(0, totalWeight);
        int cumulativeWeight = 0;

        for (Enchantment enchant : CURES_ENCHANTMENTS) {
            if (enchant.canApply(stack)) {
                cumulativeWeight += enchant.getRarity().getWeight();
                if (randomValue < cumulativeWeight) {
                    return enchant;
                }
            }
        }

        return null;
    }

    @SubscribeEvent
    public static void onForge(AnvilRepairEvent event) {
        if ((!onlyEnchant) || event.getIngredientInput().isItemEnchanted()) {
            double chance = curseForgeChance;
            ItemStack result = event.getItemResult();
            if (addChance && result.hasTagCompound() && result.getTagCompound().hasKey(Tags.MOD_ID, Constants.NBT.TAG_COMPOUND) && result.getSubCompound(Tags.MOD_ID).hasKey("extra_chance", Constants.NBT.TAG_DOUBLE)) {
                chance += result.getSubCompound(Tags.MOD_ID).getDouble("extra_chance");
            }
            if (RandomUtils.nextDouble() < chance) {
                Enchantment enchantment = getCursedEnchantment(event.getItemResult());
                if (enchantment != null) {
                    if (event.getItemResult().isItemEnchanted()) {
                        int level = RandomUtils.nextInt(enchantment.getMinLevel(), 1 + enchantment.getMaxLevel());
                        Map<Enchantment, Integer> map = EnchantmentHelper.getEnchantments(event.getItemResult());
                        if (map.containsKey(enchantment)) {
                            if (map.get(enchantment) < level) {
                                map.put(enchantment, level);
                                EnchantmentHelper.setEnchantments(map, event.getItemResult());
                            }
                        } else {
                            event.getItemResult().addEnchantment(enchantment, level);
                        }
                    } else {
                        event.getItemResult().addEnchantment(enchantment, RandomUtils.nextInt(enchantment.getMinLevel(), 1 + enchantment.getMaxLevel()));
                    }
                }
            }
            if (addChance) {
                NBTTagCompound tagCompound = event.getItemResult().getOrCreateSubCompound(Tags.MOD_ID);
                if (tagCompound.hasKey("extra_chance", Constants.NBT.TAG_DOUBLE)) {
                    tagCompound.setDouble("extra_chance", tagCompound.getDouble("extra_chance") + extraChance);
                } else tagCompound.setDouble("extra_chance", extraChance);
            }
        }
    }

    @SubscribeEvent
    public static void onConfigChanged(ConfigChangedEvent event) {
        if (Tags.MOD_ID.equals(event.getModID())) {
            ConfigManager.sync(Tags.MOD_ID, Config.Type.INSTANCE);
        }
    }

    @Config.Comment("the chance of applying curse enchantment when enchant item thought anvil (Or see onlyEnchant)")
    @Config.RangeDouble(min = 0, max = 1)
    @Config.SlidingOption
    public static double curseForgeChance = 0.1f;

    @Config.Comment("Only try to applying curse when enchant, or false for every anvil using, include repair and rename")
    public static boolean onlyEnchant = true;

    @Config.Comment("Add chance for single item when we are about to try to curse it")
    public static boolean addChance = false;

    @Config.Comment("the chance to add for single time")
    @Config.RangeDouble(min = 0, max = 1)
    @Config.SlidingOption
    public static double extraChance = 0.1;

    @Config.Comment("the register name for the enchantments to be blocked, if you do not want them be added when using anvil")
    public static String[] blockedEnchantments = new String[0];

    @Config.Ignore
    @SuppressWarnings("all")
    public static long blockedEnchantments_hash = blockedEnchantments.hashCode();

    @Config.Ignore
    public static Set<String> blockedEnchantments_set = new HashSet<>();

    @SuppressWarnings("all")
    public static boolean isEnchantmentBlocked(Enchantment enchantment) {
        if (blockedEnchantments.hashCode() != blockedEnchantments_hash) {
            blockedEnchantments_set = Sets.newHashSet(Arrays.stream(blockedEnchantments).iterator());
        }
        return blockedEnchantments_set.contains(enchantment.getRegistryName().toString());
    }
}

