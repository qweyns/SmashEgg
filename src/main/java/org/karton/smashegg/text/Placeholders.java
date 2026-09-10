package org.karton.smashegg.text;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Substitutes <code>{token}</code> placeholders in a message template. Unknown tokens are left
 * untouched so a typo stays visible instead of silently blanking part of the message.
 */
public final class Placeholders {
    private static final Pattern TOKEN = Pattern.compile("\\{([a-z0-9_]+)}");

    private Placeholders() {}

    public static String apply(String template, Map<String, String> values) {
        if (template.isEmpty() || values.isEmpty()) return template;
        Matcher matcher = TOKEN.matcher(template);
        StringBuilder result = new StringBuilder();
        while (matcher.find()) {
            String value = values.get(matcher.group(1));
            matcher.appendReplacement(result, Matcher.quoteReplacement(value == null ? matcher.group() : value));
        }
        matcher.appendTail(result);
        return result.toString();
    }
}
