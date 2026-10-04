package com.example.attributearch.attribute;

import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public final class AttributePinyin {
    private static final Map<String, String> FULL = new HashMap<>();
    private static final Map<String, String> INIT = new HashMap<>();

    static {
        put("max_health", "zuidashengmingzhi", "zdsdmz");
        put("movement_speed", "yidongsudu", "ydsd");
        put("attack_damage", "gongjishanghai", "gjsh");
        put("attack_speed", "gongjisudu", "gjsd");
        put("attack_knockback", "jitui", "jt");
        put("knockback_resistance", "jituitangxing", "jttx");
        put("armor", "hujia", "hj");
        put("armor_toughness", "hujiaarenxing", "hjrx");
        put("luck", "xingyun", "xy");
        put("flying_speed", "feixingsudu", "fxsd");
        put("follow_range", "gensuifanwei", "gsfw");
        put("spawn_reinforcements", "jiangshizengyuan", "jszy");
        put("zombie_spawn_reinforcements", "jiangshizengyuan", "jszy");
        put("horse.jump_strength", "tiaoyuelidu", "tyld");
        put("jump_strength", "tiaoyuelidu", "tyld");
        put("block_break_speed", "fangkuaipohuasudu", "fkphsd");
        put("block_interaction_range", "fangkuaijiaohuajuli", "fkjhjl");
        put("entity_interaction_range", "shitijiaohuajuli", "stjhjl");
        put("safe_fall_distance", "anquanshuailuogaidu", "aqslgd");
        put("fall_damage_multiplier", "shuailushanghaibeishu", "slshbs");
        put("scale", "suofang", "sf");
        put("step_height", "taibuagaodu", "tbgd");
        put("gravity", "zhongli", "zl");
        put("burning_time", "ranshaochixu", "rscx");
        put("explosion_knockback_resistance", "baozhajituitangxing", "bzjttx");
        put("water_movement_efficiency", "shuizhongyidongxiaolv", "szydxl");
        put("oxygen_bonus", "yangqiajiazhong", "yqjz");
        put("movement_efficiency", "yidongxiaolv", "ydxl");
        put("bow_amplification", "gongzengfu", "gzf");
        put("gun_amplification", "qiangxiezengfu", "qxzf");
        put("witch_amplification", "wufazengfu", "wfzf");
        put("iron_amplification", "ironzengfu", "irzf");
    }

    private AttributePinyin() {}

    private static void put(String path, String pinyin, String initials) {
        FULL.put(path, pinyin);
        FULL.put("minecraft:" + path, pinyin);
        FULL.put("generic." + path, pinyin);
        FULL.put("minecraft:generic." + path, pinyin);
        FULL.put("attributearch:" + path, pinyin);
        INIT.put(path, initials);
        INIT.put("minecraft:" + path, initials);
        INIT.put("generic." + path, initials);
        INIT.put("minecraft:generic." + path, initials);
        INIT.put("attributearch:" + path, initials);
    }

    public static String pinyinOf(ResourceLocation id) {
        if (id == null) {
            return "";
        }
        String p = id.getPath();
        if (FULL.containsKey(p)) {
            return FULL.get(p);
        }
        int dot = p.indexOf('.');
        String bare = dot >= 0 ? p.substring(dot + 1) : p;
        return FULL.getOrDefault(bare, FULL.getOrDefault(id.toString(), ""));
    }

    public static String initialsOf(ResourceLocation id) {
        if (id == null) {
            return "";
        }
        String p = id.getPath();
        if (INIT.containsKey(p)) {
            return INIT.get(p);
        }
        int dot = p.indexOf('.');
        String bare = dot >= 0 ? p.substring(dot + 1) : p;
        return INIT.getOrDefault(bare, INIT.getOrDefault(id.toString(), ""));
    }

    public static boolean matches(ResourceLocation id, String displayName, String queryLower) {
        if (queryLower.isEmpty()) {
            return true;
        }
        if (displayName != null && displayName.toLowerCase(Locale.ROOT).contains(queryLower)) {
            return true;
        }
        if (id != null && id.toString().toLowerCase(Locale.ROOT).contains(queryLower)) {
            return true;
        }
        String py = pinyinOf(id);
        if (!py.isEmpty() && py.contains(queryLower)) {
            return true;
        }
        String init = initialsOf(id);
        return !init.isEmpty() && init.contains(queryLower);
    }
}
