package Project11;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JackTokenizer {
    private static final ArrayList<String> keywords;
    private static final String operations;
    private final ArrayList<String> parsedTokens;
    private String currentToken;
    private int currentTokenIndex;

    private static String keyWordRegex;
    private static String symbolRegex;
    private static String intRegex;
    private static String strRegex;
    private static String idRegex;

    static {
        keywords = new ArrayList<>();
        keywords.add("class");
        keywords.add("constructor");
        keywords.add("function");
        keywords.add("method");
        keywords.add("field");
        keywords.add("static");
        keywords.add("var");
        keywords.add("int");
        keywords.add("char");
        keywords.add("boolean");
        keywords.add("void");
        keywords.add("true");
        keywords.add("false");
        keywords.add("null");
        keywords.add("this");
        keywords.add("do");
        keywords.add("if");
        keywords.add("else");
        keywords.add("while");
        keywords.add("return");
        keywords.add("let");
        operations = "+-*/&|<>=";
    }

    public JackTokenizer(File inFile) {
        Scanner scanner;
        try {
            scanner = new Scanner(inFile);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }

        String sanitizedJackCode = "";
        while (scanner.hasNextLine()) {
            String jackFileLine = scanner.nextLine();
            jackFileLine = removeComments(jackFileLine).trim();

            while (jackFileLine.isEmpty() && scanner.hasNextLine()) {
                jackFileLine = scanner.nextLine();
                jackFileLine = removeComments(jackFileLine).trim();
            }
            sanitizedJackCode = sanitizedJackCode.concat(jackFileLine);
        }

        parsedTokens = new ArrayList<>();
        currentTokenIndex = 0;

        keyWordRegex = "";
        for (String keyword : keywords) {
            keyWordRegex = keyWordRegex.concat(keyword + "|");
        }
        symbolRegex = "[\\&\\*\\+\\(\\)\\.\\/\\,\\-\\]\\;\\~\\}\\|\\{\\>\\=\\[\\<]";
        intRegex = "[0-9]+";
        strRegex = "\"[^\"\n]*\"";
        idRegex = "[\\w_]+";
        Pattern tokenPatterns = Pattern.compile(idRegex + "|" + keyWordRegex + symbolRegex + "|" + intRegex + "|" + strRegex);

        Matcher m = tokenPatterns.matcher(sanitizedJackCode);
        while (m.find()) {
            parsedTokens.add(m.group());
        }
    }

    public boolean hasMoreTokens() {
        return currentTokenIndex < parsedTokens.size();
    }

    public String getCurrentToken() {
        return currentToken;
    }

    public void advance() {
        if (!this.hasMoreTokens()) return;

        currentToken = parsedTokens.get(currentTokenIndex);
        System.out.println("Token index " + currentTokenIndex + ": " + currentToken);
        currentTokenIndex++;
    }

    public TokenType tokenType() {
        if (currentToken.matches(keyWordRegex)) {
            return TokenType.KEYWORD;
        } else if (currentToken.matches(symbolRegex)) {
            return TokenType.SYMBOL;
        } else if (currentToken.matches(intRegex)) {
            return TokenType.INT_CONST;
        } else if (currentToken.matches(strRegex)) {
            return TokenType.STRING_CONST;
        } else if (currentToken.matches(idRegex)) {
            return TokenType.IDENTIFIER;
        }

        return null;
    }

    public TokenKeyword keyWord() {
        if (this.tokenType() != TokenType.KEYWORD) return null;

        return switch (currentToken) {
            case "class" -> TokenKeyword.CLASS;
            case "constructor" -> TokenKeyword.CONSTRUCTOR;
            case "function" -> TokenKeyword.FUNCTION;
            case "method" -> TokenKeyword.METHOD;
            case "field" -> TokenKeyword.FIELD;
            case "static" -> TokenKeyword.STATIC;
            case "var" -> TokenKeyword.VAR;
            case "int" -> TokenKeyword.INT;
            case "char" -> TokenKeyword.CHAR;
            case "boolean" -> TokenKeyword.BOOLEAN;
            case "void" -> TokenKeyword.VOID;
            case "true" -> TokenKeyword.TRUE;
            case "false" -> TokenKeyword.FALSE;
            case "null" -> TokenKeyword.NULL;
            case "this" -> TokenKeyword.THIS;
            case "do" -> TokenKeyword.DO;
            case "if" -> TokenKeyword.IF;
            case "else" -> TokenKeyword.ELSE;
            case "while" -> TokenKeyword.WHILE;
            case "return" -> TokenKeyword.RETURN;
            case "let" -> TokenKeyword.LET;
            default -> null;
        };
    }

    public Character symbol() {
        if (this.tokenType() != TokenType.SYMBOL) return null;

        return currentToken.charAt(0);
    }

    public String identifier() {
        if (this.tokenType() != TokenType.IDENTIFIER) return null;

        return currentToken;
    }

    public Integer intVal() {
        if (this.tokenType() != TokenType.INT_CONST) return null;

        return Integer.parseInt(currentToken);
    }

    public String stringVal() {
        if (this.tokenType() != TokenType.STRING_CONST) return null;

        return currentToken.substring(1, currentToken.length() - 1);
    }

    public boolean isOperation() {
        if (this.tokenType() != TokenType.SYMBOL) return false;

        return operations.contains(currentToken);
    }

    public void decrementTokenIndex() {
        if (currentTokenIndex > 0) {
            currentTokenIndex--;
        }
    }

    private String removeComments(String line) {
        int commentIndex;
        line = line.trim();
        if (line.startsWith("*")) {
            commentIndex = line.indexOf("*");
        } else if (line.contains("/*")) {
            commentIndex = line.indexOf("/*");
        } else if (line.contains("*/")) {
            commentIndex = line.indexOf("*/");
            return line.substring(commentIndex + 2).trim();
        } else {
            commentIndex = line.indexOf("//");
        }

        return commentIndex == -1 ? line : line.substring(0, commentIndex).trim();
    }
}
