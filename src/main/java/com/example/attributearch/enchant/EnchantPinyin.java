package com.example.attributearch.enchant;

import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.enchantment.Enchantment;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public final class EnchantPinyin {
    private static final Map<String, String> BY_PATH = new HashMap<>();
    private static final Map<String, String> BY_PATH_INIT = new HashMap<>();

    static {
        put("efficiency", "xiaolv", "xl");
        put("unbreaking", "naijiu", "nj");
        put("mending", "jingyanxiubu", "jyxb");
        put("fortune", "shiyun", "sy");
        put("silk_touch", "jingzhuncaiji", "jzcj");
        put("sharpness", "fengli", "fl");
        put("smite", "wanglingshasha", "wlss");
        put("bane_of_arthropods", "jiezhishasha", "jzss");
        put("knockback", "jitui", "jt");
        put("fire_aspect", "huoyanfujia", "hyfj");
        put("looting", "qiangduo", "qd");
        put("sweeping_edge", "hengsaozhiren", "hszr");
        put("power", "liliang", "ll");
        put("punch", "chongji", "cj");
        put("flame", "huoshi", "hs");
        put("infinity", "wuxian", "wx");
        put("protection", "baohu", "bh");
        put("fire_protection", "huoyanbaohu", "hybh");
        put("blast_protection", "baozhabaohu", "bzbh");
        put("projectile_protection", "tanshewubaohu", "tswbh");
        put("feather_falling", "shuaihuohuanjiang", "shhj");
        put("respiration", "shuixiahuxi", "sxhx");
        put("aqua_affinity", "shuixiasujue", "sxsj");
        put("thorns", "jingji", "jj");
        put("depth_strider", "shenhaitasuozhe", "shtsz");
        put("frost_walker", "bingshuangxingzhe", "bsxz");
        put("soul_speed", "linghunjiixing", "lhjx");
        put("swift_sneak", "xunjieqianxing", "xjqx");
        put("luck_of_the_sea", "haizhijuangu", "hzjg");
        put("lure", "erdiao", "ed");
        put("loyalty", "zhongcheng", "zc");
        put("impaling", "chuanci", "cc");
        put("riptide", "jiliu", "jl");
        put("channeling", "yinlei", "yl");
        put("multishot", "duochongsheji", "dcsj");
        put("piercing", "chuantou", "ct");
        put("quick_charge", "kuaizusangzhuang", "kzsz");
        put("curse_of_binding", "bangdingzuzhou", "bdzz");
        put("curse_of_vanishing", "xiaoshizuzhou", "xszz");
    }

    private EnchantPinyin() {}

    private static void put(String path, String pinyin, String init) {
        BY_PATH.put(path, pinyin);
        BY_PATH.put("minecraft:" + path, pinyin);
        BY_PATH_INIT.put(pinyin, init);
    }

    public static String pinyinOf(Holder<Enchantment> holder) {
        ResourceLocation id = holder.unwrapKey()
                .map(net.minecraft.resources.ResourceKey::location)
                .orElse(null);
        if (id == null) {
            return "";
        }
        return BY_PATH.getOrDefault(id.getPath(), "");
    }

    public static String initialsOf(Holder<Enchantment> holder) {
        String full = pinyinOf(holder);
        if (full.isEmpty()) {
            return "";
        }
        return BY_PATH_INIT.getOrDefault(full, full.substring(0, 1));
    }

    public static boolean matches(Holder<Enchantment> holder, String display, String id, String queryLower) {
        if (queryLower.isEmpty()) {
            return true;
        }
        if (display.toLowerCase(Locale.ROOT).contains(queryLower)
                || id.toLowerCase(Locale.ROOT).contains(queryLower)) {
            return true;
        }
        String py = pinyinOf(holder);
        if (!py.isEmpty() && py.contains(queryLower)) {
            return true;
        }
        String init = initialsOf(holder);
        return !init.isEmpty() && init.contains(queryLower);
    }
}
