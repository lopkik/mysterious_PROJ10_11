package Project11;

import java.util.HashMap;

public class SymbolTable {
    private final HashMap<String, Symbol> classTable;
    private final HashMap<String, Symbol> subroutineTable;
    private final HashMap<IdentifierKind, Integer> identifierCount;

    public SymbolTable() {
        classTable = new HashMap<>();
        subroutineTable = new HashMap<>();
        identifierCount = new HashMap<>();
        identifierCount.put(IdentifierKind.STATIC, 0);
        identifierCount.put(IdentifierKind.FIELD, 0);
        identifierCount.put(IdentifierKind.ARG, 0);
        identifierCount.put(IdentifierKind.VAR, 0);
    }

    public void startSubroutine() {
        System.out.println("start subroutine called");
        subroutineTable.clear();
        identifierCount.put(IdentifierKind.ARG, 0);
        identifierCount.put(IdentifierKind.VAR, 0);
    }

    public void define(String name, String type, IdentifierKind kind) {
        if (kind == IdentifierKind.NONE) return;

        int kindCount = identifierCount.get(kind);
        Symbol symbol = new Symbol(type, kind, kindCount);
        identifierCount.put(kind, ++kindCount);

        System.out.print("Defining in symbol table: ");
        System.out.println(name);

        if (kind == IdentifierKind.ARG || kind == IdentifierKind.VAR) {
            subroutineTable.put(name, symbol);
        } else if (kind == IdentifierKind.STATIC || kind == IdentifierKind.FIELD) {
            classTable.put(name, symbol);
        }
    }

    public int varCount(IdentifierKind kind) {
        return identifierCount.get(kind);
    }

    public IdentifierKind kindOf(String name) {
        IdentifierKind kind;
        if (subroutineTable.containsKey(name)) {
            kind = subroutineTable.get(name).kind();
        } else if (classTable.containsKey(name)) {
            kind = classTable.get(name).kind();
        } else {
            kind = IdentifierKind.NONE;
        }
        return kind;
    }

    public String typeOf(String name) {
        String type;
        if (subroutineTable.containsKey(name)) {
            type = subroutineTable.get(name).type();
        } else if (classTable.containsKey(name)) {
            type = classTable.get(name).type();
        } else {
            type = "";
        }
        return type;
    }

    public int indexOf(String name) {
        Symbol symbol = null;
        if (subroutineTable.containsKey(name)) {
            symbol = subroutineTable.get(name);
        } else if (classTable.containsKey(name)) {
            symbol = classTable.get(name);
        }

        return symbol == null ? -1 : symbol.index();
    }
}
