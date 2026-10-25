package dev.jbaby.wicket.testkit.locator;

import java.util.regex.Pattern;
import org.apache.wicket.util.string.Strings;

/// How descriptions and failure messages put types, values and texts into words.
///
/// **Internal.** Public only so that both packages of the testkit word their messages alike; not
/// part of the API, and it may change in any release.
public final class Words {

    /// Any run of whitespace, non-breaking spaces included.
    private static final Pattern WHITESPACE = Pattern.compile("\\s+", Pattern.UNICODE_CHARACTER_CLASS);

    private static final Pattern TAG = Pattern.compile("<[^>]*>");

    private Words() {
    }

    /// The noun with `a` in front, or `an` before a vowel letter.
    ///
    /// @param noun the noun
    /// @return the noun with its article
    public static String withArticle(String noun) {
        return ("AEIOUaeiou".indexOf(noun.charAt(0)) >= 0 ? "an " : "a ") + noun;
    }

    /// The class itself, or its nearest named superclass if it is anonymous.
    ///
    /// @param type the class
    /// @return the named class
    public static Class<?> namedClass(Class<?> type) {
        var named = type;
        while (named.isAnonymousClass()) {
            named = named.getSuperclass();
        }
        return named;
    }

    /// The simple name of the class, or of its nearest named superclass if it is anonymous.
    ///
    /// @param type the class
    /// @return the name
    public static String className(Class<?> type) {
        return namedClass(type).getSimpleName();
    }

    /// A string in double quotes, any other value as `String.valueOf` shows it, or by its class if
    /// its `toString` throws.
    ///
    /// @param value the value
    /// @return the value in words
    public static String value(Object value) {
        if (value instanceof String) {
            return "\"" + value + "\"";
        }
        try {
            return String.valueOf(value);
        } catch (RuntimeException e) {
            return withArticle(className(value.getClass())) + " whose toString throws";
        }
    }

    /// The text without whitespace at either end, and every run of whitespace as one space, as a
    /// browser shows it.
    ///
    /// @param text the text
    /// @return the normalized text
    public static String normalized(String text) {
        return WHITESPACE.matcher(text).replaceAll(" ").strip();
    }

    /// The text of markup: without its tags, and with its character entities decoded.
    ///
    /// @param markup the markup, such as `Terms &amp; <b>conditions</b>`
    /// @return the text, such as `Terms & conditions`
    public static String textOf(String markup) {
        return Strings.unescapeMarkup(TAG.matcher(markup).replaceAll("")).toString();
    }
}
