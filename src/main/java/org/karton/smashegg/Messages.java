package org.karton.smashegg;

import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

/** A message entry is either a plain MiniMessage string or a section with text/output. */
final class Messages {
    private static final Set<String> FIELDS = Set.of("text", "output");

    private Messages() {}

    static MessageSpec parse(Object value, String path, Consumer<String> warning) {
        if (value instanceof String text) return spec(text, MessageOutput.CHAT, path);
        Map<String, Object> section = ConfigNodes.section(value, path);
        SectionFields.check(section, path, FIELDS, warning);
        String text = ConfigNodes.string(section.get("text"), path + ".text");
        MessageOutput output = MessageOutput.CHAT;
        if (section.containsKey("output")) {
            try {
                output = MessageOutput.parse(ConfigNodes.string(section.get("output"), path + ".output"));
            } catch (IllegalArgumentException e) {
                throw ConfigNodes.invalid(path + ".output", e.getMessage());
            }
        }
        return spec(text, output, path);
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
