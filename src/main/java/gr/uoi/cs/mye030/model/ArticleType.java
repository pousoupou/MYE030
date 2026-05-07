package gr.uoi.cs.mye030.model;

public enum ArticleType {
    JOURNAL('J'),
    CONFERENCE('C');

    private final char type;

    ArticleType(char type) {
        this.type = type;
    }

    public char code() {
        return type;
    }

    public static ArticleType fromType(char type) {
        for (ArticleType t : values()) {
            if (t.type == type) return t;
        }
        throw new IllegalArgumentException("Unknown article type code: " + type);
    }
}
