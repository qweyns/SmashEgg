package org.karton.smashegg.config;

import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Localisable words substituted into {@code {hand}}, {@code {mode}}, {@code {spawner}},
 * {@code {creative}}, {@code {filter}} and the "not set" marker for {@code /info}.
 */
public record PlaceholderWords(String handMain, String handOff, String modeSpawner, String modeGround,
                               String spawnerOn, String spawnerOff, String creativeYes, String creativeNo,
                               String filterBlacklist, String filterWhitelist, String unset) {

    public static final PlaceholderWords FALLBACK = new PlaceholderWords(
            "main", "off", "spawner", "ground", "on", "off", "yes", "no", "blacklist", "whitelist", "-");

    private static final Set<String> ROOT_KEYS = Set.of("hand", "mode", "spawner", "creative", "filter", "unset");

    public static PlaceholderWords load(Object node, String path, Consumer<String> warning) {
        Map<String, Object> root = ConfigNodes.section(node, path);
        if (root.isEmpty()) return FALLBACK;
        SectionFields.check(root, path, ROOT_KEYS, warning);
        Pair hand = pair(root.get("hand"), path + ".hand", "main", "off", FALLBACK.handMain, FALLBACK.handOff, warning);
        Pair mode = pair(root.get("mode"), path + ".mode", "spawner", "ground",
                FALLBACK.modeSpawner, FALLBACK.modeGround, warning);
        Pair spawner = pair(root.get("spawner"), path + ".spawner", "on", "off",
                FALLBACK.spawnerOn, FALLBACK.spawnerOff, warning);
        Pair creative = pair(root.get("creative"), path + ".creative", "yes", "no",
                FALLBACK.creativeYes, FALLBACK.creativeNo, warning);
        Pair filter = pair(root.get("filter"), path + ".filter", "blacklist", "whitelist",
                FALLBACK.filterBlacklist, FALLBACK.filterWhitelist, warning);
        return new PlaceholderWords(hand.a(), hand.b(), mode.a(), mode.b(), spawner.a(), spawner.b(),
                creative.a(), creative.b(), filter.a(), filter.b(),
                word(root.get("unset"), path + ".unset", FALLBACK.unset()));
    }

    public String hand(boolean offHand) {
        return offHand ? handOff : handMain;
    }

    public String mode(boolean spawner) {
        return spawner ? modeSpawner : modeGround;
    }

    public String spawner(boolean enabled) {
        return enabled ? spawnerOn : spawnerOff;
    }

    public String creative(boolean affected) {
        return affected ? creativeYes : creativeNo;
    }

    public String filter(FilterMode mode) {
        return mode == FilterMode.WHITELIST ? filterWhitelist : filterBlacklist;
    }

    private static Pair pair(Object node, String path, String firstKey, String secondKey,
                             String firstFallback, String secondFallback, Consumer<String> warning) {
        if (node == null) return new Pair(firstFallback, secondFallback);
        Map<String, Object> section = ConfigNodes.section(node, path);
        SectionFields.check(section, path, Set.of(firstKey, secondKey), warning);
        return new Pair(
                word(section.get(firstKey), path + "." + firstKey, firstFallback),
                word(section.get(secondKey), path + "." + secondKey, secondFallback));
    }

    private static String word(Object value, String path, String fallback) {
        if (value == null) return fallback;
        String text = ConfigNodes.string(value, path);
        return text.isEmpty() ? fallback : text;
    }

    private record Pair(String a, String b) {}
}
