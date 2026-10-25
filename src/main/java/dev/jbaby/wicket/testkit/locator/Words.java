package dev.jbaby.wicket.testkit.locator;

/// How descriptions and failure messages put types and values into words.
final class Words {

    private Words() {
    }

    /// The noun with `a` in front, or `an` before a vowel letter.
    static String withArticle(String noun) {
        return ("AEIOUaeiou".indexOf(noun.charAt(0)) >= 0 ? "an " : "a ") + noun;
    }

    /// The simple name of the class, or of its nearest named superclass if it is anonymous.
    static String className(Class<?> type) {
        var named = type;
        while (named.isAnonymousClass()) {
            named = named.getSuperclass();
        }
        return named.getSimpleName();
    }

    /// A string in double quotes, any other value as `String.valueOf` shows it, or by its class if
    /// its `toString` throws.
    static String value(Object value) {
        if (value instanceof String) {
            return "\"" + value + "\"";
        }
        try {
            return String.valueOf(value);
        } catch (RuntimeException e) {
            return withArticle(className(value.getClass())) + " whose toString throws";
        }
    }
}
