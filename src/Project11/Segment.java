package Project11;

public enum Segment {
    CONST("constant"),
    ARG("argument"),
    LOCAL("local"),
    STATIC("static"),
    THIS("this"),
    THAT("that"),
    POINTER("pointer"),
    TEMP("temp");

    public final String label;

    Segment(String label) {
        this.label = label;
    }
}
