package org.karton.smashegg;

/**
 * A message template plus the channel it is shown on. The text stays unparsed so that
 * per-event placeholders can be substituted at send time; an empty text disables the message.
 */
record MessageSpec(String text, MessageOutput output) {
    MessageSpec {
        text = text == null ? "" : text;
        output = output == null ? MessageOutput.CHAT : output;
    }

    static MessageSpec disabled() {
        return new MessageSpec("", MessageOutput.NONE);
    }

    boolean enabled() {
        return output != MessageOutput.NONE && !text.isEmpty();
    }
}
