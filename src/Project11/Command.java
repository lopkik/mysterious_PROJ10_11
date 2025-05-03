package Project11;

public enum Command {
    ADD("add"),
    SUB("sub"),
    NEG("neg"),
    EQ("eq"),
    GT("gt"),
    LT("lt"),
    AND("and"),
    OR("or"),
    NOT("not");

    public final String label;

    private Command(String label) {
        this.label = label;
    }
}
