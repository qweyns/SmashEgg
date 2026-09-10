package org.karton.smashegg.effect;

/**
 * A message template plus the channel it is shown on. The text stays unparsed so that
 * per-event placeholders can be substituted at send time; an empty text disables the message.
 */
public record MessageSpec(String text, MessageOutput output) {
    public MessageSpec {
        text = text == null ? "" : text;
        output = output == null ? MessageOutput.CHAT : output;
    }

    public static MessageSpec disabled() {
        return new MessageSpec("", MessageOutput.NONE);
    }

    public boolean enabled() {
        return output != MessageOutput.NONE && !text.isEmpty();
    }
}
