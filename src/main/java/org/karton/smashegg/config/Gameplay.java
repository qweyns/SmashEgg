package org.karton.smashegg.config;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import org.karton.smashegg.util.EggTypes;

/**
 * Optional player-facing mechanics. Every nested feature is off unless {@code enabled: true},
 * so a 4.0 config keeps the old click-and-roll behaviour.
 */
public record Gameplay(Preview preview, AllIn allIn, Map<String, Catalyst> catalysts,
                       Pity pity, Grace grace, List<Luck> luck, CriticalFail criticalFail,
                       SpawnerRisk spawnerRisk, ChangeLimit changeLimit, Consolation consolation,
                       CooldownDisplay cooldownDisplay, Announce announce) {

    public static final Gameplay OFF = new Gameplay(
            Preview.OFF, AllIn.OFF, Map.of(), Pity.OFF, Grace.OFF, List.of(), CriticalFail.OFF,
            SpawnerRisk.OFF, ChangeLimit.OFF, Consolation.OFF, CooldownDisplay.OFF, Announce.OFF);

    private static final Set<String> ROOT = Set.of("preview", "all-in", "catalysts", "pity", "grace",
            "luck", "critical-fail", "spawner-risk", "change-limit", "consolation",
            "cooldown-display", "announce");

    public Gameplay {
        catalysts = Map.copyOf(catalysts);
        luck = List.copyOf(luck);
    }

    public static Gameplay load(Object node, String path, Consumer<String> warning) {
        Map<String, Object> root = ConfigNodes.section(node, path);
        if (root.isEmpty()) return OFF;
        SectionFields.check(root, path, ROOT, warning);
        return new Gameplay(
                Preview.load(root.get("preview"), path + ".preview", warning),
                AllIn.load(root.get("all-in"), path + ".all-in", warning),
                catalysts(root.get("catalysts"), path + ".catalysts", warning),
                Pity.load(root.get("pity"), path + ".pity", warning),
                Grace.load(root.get("grace"), path + ".grace", warning),
                luck(root.get("luck"), path + ".luck", warning),
                CriticalFail.load(root.get("critical-fail"), path + ".critical-fail", warning),
                SpawnerRisk.load(root.get("spawner-risk"), path + ".spawner-risk", warning),
                ChangeLimit.load(root.get("change-limit"), path + ".change-limit", warning),
                Consolation.load(root.get("consolation"), path + ".consolation", warning),
                CooldownDisplay.load(root.get("cooldown-display"), path + ".cooldown-display", warning),
                Announce.load(root.get("announce"), path + ".announce", warning));
    }

    public boolean catalystsEnabled() {
        return !catalysts.isEmpty();
    }

    public Luck luckFor(java.util.function.Predicate<String> hasPermission) {
        for (Luck tier : luck) {
            if (hasPermission.test(tier.permission())) return tier;
        }
        return Luck.NONE;
    }

    private static Map<String, Catalyst> catalysts(Object node, String path, Consumer<String> warning) {
        Map<String, Object> section = ConfigNodes.section(node, path);
        Map<String, Catalyst> result = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : section.entrySet()) {
            String name = entry.getKey().trim().toUpperCase(Locale.ROOT);
            if (!name.matches("[A-Z][A-Z0-9_]*")) {
                warning.accept(path + "." + entry.getKey() + ": invalid material name; skipped.");
                continue;
            }
            result.put(name, Catalyst.load(entry.getValue(), path + "." + entry.getKey(), warning));
        }
        return result;
    }

    private static List<Luck> luck(Object node, String path, Consumer<String> warning) {
        if (node == null) return List.of();
        if (!(node instanceof List<?> list)) throw ConfigNodes.invalid(path, "must be a list of luck tiers");
        List<Luck> result = new ArrayList<>();
        int index = 0;
        for (Object entry : list) {
            result.add(Luck.load(entry, path + "[" + index++ + "]", warning));
        }
        return result;
    }

    public record Preview(boolean enabled, int intervalTicks, int reach) {
        public static final Preview OFF = new Preview(false, 10, 5);
        private static final Set<String> FIELDS = Set.of("enabled", "interval-ticks", "reach");

        static Preview load(Object node, String path, Consumer<String> warning) {
            Map<String, Object> section = ConfigNodes.section(node, path);
            if (section.isEmpty()) return OFF;
            SectionFields.check(section, path, FIELDS, warning);
            return new Preview(
                    flag(section.get("enabled"), path + ".enabled"),
                    ConfigNodes.integer(section.getOrDefault("interval-ticks", 10), path + ".interval-ticks", 1, 200),
                    ConfigNodes.integer(section.getOrDefault("reach", 5), path + ".reach", 1, 16));
        }
    }

    public record AllIn(boolean enabled, boolean sneak, int extra, String mode, int breakDelta, int groundDelta,
                        String pay) {
        public static final AllIn OFF = new AllIn(false, true, 2, "guarantee", -100, 100, "success");
        private static final Set<String> FIELDS = Set.of("enabled", "sneak", "extra", "mode",
                "break-delta", "ground-delta", "pay");

        static AllIn load(Object node, String path, Consumer<String> warning) {
            Map<String, Object> section = ConfigNodes.section(node, path);
            if (section.isEmpty()) return OFF;
            SectionFields.check(section, path, FIELDS, warning);
            String mode = word(section.get("mode"), path + ".mode", "guarantee", Set.of("guarantee", "reduce"));
            String pay = word(section.get("pay"), path + ".pay", "success", Set.of("success", "always"));
            return new AllIn(
                    flag(section.get("enabled"), path + ".enabled"),
                    section.get("sneak") == null || ConfigNodes.bool(section.get("sneak"), path + ".sneak"),
                    ConfigNodes.integer(section.getOrDefault("extra", 2), path + ".extra", 0, 64),
                    mode,
                    delta(section.get("break-delta"), path + ".break-delta", -100),
                    delta(section.get("ground-delta"), path + ".ground-delta", 100),
                    pay);
        }

        public boolean guarantee() {
            return "guarantee".equals(mode);
        }

        public boolean payAlways() {
            return "always".equals(pay);
        }
    }

    public record Catalyst(boolean consume, int breakDelta, int groundDelta) {
        private static final Set<String> FIELDS = Set.of("consume", "break-delta", "ground-delta");

        static Catalyst load(Object node, String path, Consumer<String> warning) {
            Map<String, Object> section = ConfigNodes.section(node, path);
            SectionFields.check(section, path, FIELDS, warning);
            return new Catalyst(
                    section.get("consume") == null || ConfigNodes.bool(section.get("consume"), path + ".consume"),
                    delta(section.get("break-delta"), path + ".break-delta", 0),
                    delta(section.get("ground-delta"), path + ".ground-delta", 0));
        }
    }

    public record Pity(boolean enabled, int after, String scope) {
        public static final Pity OFF = new Pity(false, 5, "player");
        private static final Set<String> FIELDS = Set.of("enabled", "after", "scope");

        static Pity load(Object node, String path, Consumer<String> warning) {
            Map<String, Object> section = ConfigNodes.section(node, path);
            if (section.isEmpty()) return OFF;
            SectionFields.check(section, path, FIELDS, warning);
            return new Pity(
                    flag(section.get("enabled"), path + ".enabled"),
                    ConfigNodes.integer(section.getOrDefault("after", 5), path + ".after", 1, 1000),
                    word(section.get("scope"), path + ".scope", "player", Set.of("player", "entity")));
        }

        public boolean perEntity() {
            return "entity".equals(scope);
        }
    }

    public record Grace(boolean enabled, int first) {
        public static final Grace OFF = new Grace(false, 10);
        private static final Set<String> FIELDS = Set.of("enabled", "first");

        static Grace load(Object node, String path, Consumer<String> warning) {
            Map<String, Object> section = ConfigNodes.section(node, path);
            if (section.isEmpty()) return OFF;
            SectionFields.check(section, path, FIELDS, warning);
            return new Grace(
                    flag(section.get("enabled"), path + ".enabled"),
                    ConfigNodes.integer(section.getOrDefault("first", 10), path + ".first", 1, 10000));
        }
    }

    public record Luck(String permission, int breakDelta, int groundDelta) {
        public static final Luck NONE = new Luck("", 0, 0);
        private static final Set<String> FIELDS = Set.of("permission", "break-delta", "ground-delta");

        static Luck load(Object node, String path, Consumer<String> warning) {
            Map<String, Object> section = ConfigNodes.section(node, path);
            SectionFields.check(section, path, FIELDS, warning);
            String permission = ConfigNodes.string(section.get("permission"), path + ".permission").trim();
            if (permission.isEmpty()) throw ConfigNodes.invalid(path + ".permission", "must not be empty");
            return new Luck(permission,
                    delta(section.get("break-delta"), path + ".break-delta", 0),
                    delta(section.get("ground-delta"), path + ".ground-delta", 0));
        }
    }

    public record CriticalFail(boolean enabled, int chance, double knockback, String potion,
                               int potionTicks, int potionAmplifier) {
        public static final CriticalFail OFF = new CriticalFail(false, 5, 0.0, "", 60, 0);
        private static final Set<String> FIELDS = Set.of("enabled", "chance", "knockback", "potion",
                "potion-ticks", "potion-amplifier");

        static CriticalFail load(Object node, String path, Consumer<String> warning) {
            Map<String, Object> section = ConfigNodes.section(node, path);
            if (section.isEmpty()) return OFF;
            SectionFields.check(section, path, FIELDS, warning);
            String potion = section.get("potion") == null ? ""
                    : ConfigNodes.string(section.get("potion"), path + ".potion").trim();
            return new CriticalFail(
                    flag(section.get("enabled"), path + ".enabled"),
                    ConfigNodes.integer(section.getOrDefault("chance", 5), path + ".chance", 0, 100),
                    ConfigNodes.decimal(section.getOrDefault("knockback", 0.0), path + ".knockback", 0.0, 5.0),
                    potion,
                    ConfigNodes.integer(section.getOrDefault("potion-ticks", 60), path + ".potion-ticks", 1, 72000),
                    ConfigNodes.integer(section.getOrDefault("potion-amplifier", 0), path + ".potion-amplifier", 0, 10));
        }
    }

    public record SpawnerRisk(boolean enabled, int chance, String action, String resetTo) {
        public static final SpawnerRisk OFF = new SpawnerRisk(false, 0, "none", "");
        private static final Set<String> FIELDS = Set.of("enabled", "chance", "action", "reset-to");

        static SpawnerRisk load(Object node, String path, Consumer<String> warning) {
            Map<String, Object> section = ConfigNodes.section(node, path);
            if (section.isEmpty()) return OFF;
            SectionFields.check(section, path, FIELDS, warning);
            String resetTo = section.get("reset-to") == null ? ""
                    : EggTypes.normalize(ConfigNodes.string(section.get("reset-to"), path + ".reset-to"));
            if (!resetTo.isEmpty() && !resetTo.matches("[A-Z][A-Z0-9_]*")) {
                throw ConfigNodes.invalid(path + ".reset-to", "invalid entity name: " + resetTo);
            }
            return new SpawnerRisk(
                    flag(section.get("enabled"), path + ".enabled"),
                    ConfigNodes.integer(section.getOrDefault("chance", 0), path + ".chance", 0, 100),
                    word(section.get("action"), path + ".action", "none", Set.of("none", "lock", "reset")),
                    resetTo);
        }
    }

    public record ChangeLimit(boolean enabled, int max) {
        public static final ChangeLimit OFF = new ChangeLimit(false, 3);
        private static final Set<String> FIELDS = Set.of("enabled", "max");

        static ChangeLimit load(Object node, String path, Consumer<String> warning) {
            Map<String, Object> section = ConfigNodes.section(node, path);
            if (section.isEmpty()) return OFF;
            SectionFields.check(section, path, FIELDS, warning);
            return new ChangeLimit(
                    flag(section.get("enabled"), path + ".enabled"),
                    ConfigNodes.integer(section.getOrDefault("max", 3), path + ".max", 1, 1000));
        }
    }

    public record Consolation(boolean enabled, String material, int amount, int chance) {
        public static final Consolation OFF = new Consolation(false, "EGG", 1, 100);
        private static final Set<String> FIELDS = Set.of("enabled", "material", "amount", "chance");

        static Consolation load(Object node, String path, Consumer<String> warning) {
            Map<String, Object> section = ConfigNodes.section(node, path);
            if (section.isEmpty()) return OFF;
            SectionFields.check(section, path, FIELDS, warning);
            String material = section.get("material") == null ? "EGG"
                    : ConfigNodes.string(section.get("material"), path + ".material").trim().toUpperCase(Locale.ROOT);
            if (!material.matches("[A-Z][A-Z0-9_]*")) {
                throw ConfigNodes.invalid(path + ".material", "invalid material: " + material);
            }
            return new Consolation(
                    flag(section.get("enabled"), path + ".enabled"),
                    material,
                    ConfigNodes.integer(section.getOrDefault("amount", 1), path + ".amount", 1, 64),
                    ConfigNodes.integer(section.getOrDefault("chance", 100), path + ".chance", 0, 100));
        }
    }

    public record CooldownDisplay(boolean enabled) {
        public static final CooldownDisplay OFF = new CooldownDisplay(false);
        private static final Set<String> FIELDS = Set.of("enabled");

        static CooldownDisplay load(Object node, String path, Consumer<String> warning) {
            Map<String, Object> section = ConfigNodes.section(node, path);
            if (section.isEmpty()) return OFF;
            SectionFields.check(section, path, FIELDS, warning);
            return new CooldownDisplay(flag(section.get("enabled"), path + ".enabled"));
        }
    }

    public record Announce(boolean enabled, int radius, Set<String> entities, boolean includeSelf) {
        public static final Announce OFF = new Announce(false, 32, Set.of(), false);
        private static final Set<String> FIELDS = Set.of("enabled", "radius", "entities", "include-self");

        public Announce {
            entities = Set.copyOf(entities);
        }

        static Announce load(Object node, String path, Consumer<String> warning) {
            Map<String, Object> section = ConfigNodes.section(node, path);
            if (section.isEmpty()) return OFF;
            SectionFields.check(section, path, FIELDS, warning);
            Set<String> entities = new java.util.LinkedHashSet<>();
            Object listNode = section.get("entities");
            if (listNode != null) {
                if (!(listNode instanceof List<?> list)) {
                    throw ConfigNodes.invalid(path + ".entities", "must be a list of entity names");
                }
                for (Object entry : list) {
                    if (!(entry instanceof String text)) {
                        throw ConfigNodes.invalid(path + ".entities", "must contain only strings");
                    }
                    entities.add(EggTypes.normalize(text));
                }
            }
            return new Announce(
                    flag(section.get("enabled"), path + ".enabled"),
                    ConfigNodes.integer(section.getOrDefault("radius", 32), path + ".radius", 1, 256),
                    entities,
                    flag(section.get("include-self"), path + ".include-self"));
        }
    }

    static boolean flag(Object value, String path) {
        return value != null && ConfigNodes.bool(value, path);
    }

    static int delta(Object value, String path, int fallback) {
        if (value == null) return fallback;
        return ConfigNodes.integer(value, path, -100, 100);
    }

    static String word(Object value, String path, String fallback, Set<String> allowed) {
        if (value == null) return fallback;
        String text = ConfigNodes.string(value, path).trim().toLowerCase(Locale.ROOT);
        if (!allowed.contains(text)) {
            throw ConfigNodes.invalid(path, "must be one of " + String.join(", ", allowed) + " (found: " + value + ")");
        }
        return text;
    }

    public static int clamp(int percent) {
        return Math.max(0, Math.min(100, percent));
    }
}
