package org.karton.smashegg;

import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

/** A message entry is either a plain MiniMessage string or a section with text/output. */
final class Messages {
    private static final Set<String> FIELDS = Set.of("text", "output");

    private Messages() {}

    /**
     * @param translation the language-file value, used for {@code text} when the override only
     *                    changes another field, so {@code output: actionbar} alone stays valid
     */
    static MessageSpec parse(Object value, Object translation, String path, Consumer<String> warning) {
        if (value instanceof String text) return spec(text, MessageOutput.CHAT, path);
        Map<String, Object> section = ConfigNodes.section(value, path);
        SectionFields.check(section, path, FIELDS, warning);
        String text = section.containsKey("text")
                ? ConfigNodes.string(section.get("text"), path + ".text")
                : translatedText(translation, path);
        MessageOutput output = MessageOutput.CHAT;
        if (section.containsKey("output")) {
            output = MessageOutput.parse(ConfigNodes.string(section.get("output"), path + ".output"),
                    path + ".output");
        }
        return spec(text, output, path);
    }

    private static String translatedText(Object translation, String path) {
        if (translation instanceof String text) return text;
        throw ConfigNodes.invalid(path + ".text", "must be a string");
    }

    /** Parsed once at load time so a broken template is reported before it is ever shown. */
    private static MessageSpec spec(String text, MessageOutput output, String path) {
        try {
            ColorUtil.parse(text);
        } catch (IllegalArgumentException e) {
            throw ConfigNodes.invalid(path, e.getMessage());
        }
        return new MessageSpec(text, output);
    }
}
