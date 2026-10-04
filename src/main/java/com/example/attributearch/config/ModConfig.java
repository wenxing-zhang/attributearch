package com.example.attributearch.config;

import java.util.List;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class ModConfig {
    private ModConfig() {
    }

    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.IntValue MAX_LEVEL = BUILDER
            .comment("每个属性的最大强化等级",
                    "Maximum enhancement level per attribute")
            .defineInRange("maxLevel", 5, 1, 100);

    public static final ModConfigSpec.DoubleValue BASE_ADDITION_PER_LEVEL = BUILDER
            .comment("每级加法加成",
                    "Addition amount applied per enhancement level")
            .defineInRange("baseAdditionPerLevel", 20.0D, 0.0D, 1_000_000.0D);

    public static final ModConfigSpec.DoubleValue BASE_MULTIPLIER_PER_LEVEL = BUILDER
            .comment("乘法乘区 (ADD_MULTIPLIED_BASE) 每级加成",
                    "ADD_MULTIPLIED_BASE amount applied per enhancement level")
            .defineInRange("baseMultiplierPerLevel", 0.2D, 0.0D, 1000.0D);

    public static final ModConfigSpec.DoubleValue BASE_INDEPENDENT_MULTIPLIER_PER_LEVEL = BUILDER
            .comment("独立乘法乘区 (ADD_MULTIPLIED_TOTAL) 每级加成",
                    "ADD_MULTIPLIED_TOTAL amount applied per enhancement level")
            .defineInRange("baseIndependentMultiplierPerLevel", 0.2D, 0.0D, 1000.0D);

    public static final ModConfigSpec.IntValue XP_COST_PER_LEVEL = BUILDER
            .comment("每级所需经验等级（目标等级 * 此值，最少 1）",
                    "Experience levels required per target level (target * this value, min 1)")
            .defineInRange("xpCostPerLevel", 10, 1, 10000);

    public static final ModConfigSpec.BooleanValue USE_ALL_PLAYER_ATTRIBUTES = BUILDER
            .comment("列出玩家身上全部属性，含其它模组属性",
                    "List every attribute attached to the player, including modded ones")
            .define("useAllPlayerAttributes", true);

    public static final ModConfigSpec.ConfigValue<List<? extends String>> BLACKLISTED_ATTRIBUTES = BUILDER
            .comment("GUI 中永不出现的属性 ID",
                    "Attribute ids that never appear in the GUI")
            .defineListAllowEmpty("blacklistedAttributes", List.of(), ModConfig::isResourceId);

    public static final ModConfigSpec.ConfigValue<List<? extends String>> ENABLED_ATTRIBUTES = BUILDER
            .comment("仅当 useAllPlayerAttributes=false 时使用",
                    "Only used when useAllPlayerAttributes=false")
            .defineListAllowEmpty("enabledAttributes", List.of(), ModConfig::isResourceId);

    private static boolean isResourceId(Object o) {
        return o instanceof String s && net.minecraft.resources.ResourceLocation.tryParse(s) != null;
    }

    public static final ModConfigSpec.BooleanValue KEEP_ON_DEATH = BUILDER
            .comment("死亡后保留强化等级",
                    "Keep enhancement levels after death")
            .define("keepOnDeath", true);

    public static final ModConfigSpec.IntValue WENXING_TOTEM_COOLDOWN_TICKS = BUILDER
            .comment("文星图腾触发后冷却 tick（默认 20 = 1 秒）",
                    "Wenxing Totem cooldown ticks after trigger (default 20 = 1 second)")
            .defineInRange("wenxingTotemCooldownTicks", 20, 0, 12000);

    public static final ModConfigSpec.IntValue WENXING_SHIELD_DURATION_TICKS = BUILDER
            .comment("文星护盾持续 tick（默认 600 = 30 秒）",
                    "Wenxing Shield duration ticks (default 600 = 30 seconds)")
            .defineInRange("wenxingShieldDurationTicks", 600, 1, 12000);

    public static final ModConfigSpec.BooleanValue WENXING_CLEAR_NEGATIVE_EFFECTS = BUILDER
            .comment("图腾触发时清除全部负面效果",
                    "Clear all negative effects when the totem triggers")
            .define("wenxingClearNegativeEffects", true);

    public static final ModConfigSpec SPEC = BUILDER.build();
}
