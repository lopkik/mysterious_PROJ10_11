package Project10;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

public class CompilationEngine {
    private PrintWriter printWriter;
    private JackTokenizer tokenizer;

    public CompilationEngine(File inFile, File outFile) {
        try {
            printWriter = new PrintWriter(outFile);
            tokenizer = new JackTokenizer(inFile);
        } catch (IOException e) {
            e.printStackTrace(System.out);
        }
    }

    public void compileClass() {
        tokenizer.advance();

        if (tokenizer.tokenType() != TokenType.KEYWORD || tokenizer.keyWord() != TokenKeyword.CLASS) {
            return;
        }

        printWriter.println("<class>");
        printWriter.println("<keyword> class </keyword>");

        tokenizer.advance();
        String firstIdentifier = tokenizer.identifier();

        tokenizer.advance();
        if (tokenizer.tokenType() == TokenType.SYMBOL && tokenizer.symbol() == '.') {
            tokenizer.advance();
            firstIdentifier = firstIdentifier.concat("." + tokenizer.identifier());
        } else {
            tokenizer.decrementTokenIndex();
        }
        printWriter.println("<identifier> " + firstIdentifier + " </identifier>");

        processExpectedSymbol('{');
        compileClassVarDec();
        compileSubroutine();
        processExpectedSymbol('}');

        printWriter.println("</class>");
        printWriter.close();
    }

    public void compileClassVarDec() {
        tokenizer.advance();

        // empty class
        if (tokenizer.tokenType() == TokenType.SYMBOL && tokenizer.symbol() == '}') {
            tokenizer.decrementTokenIndex();
            return;
        }

        // if subroutine, then exit
        if (tokenizer.keyWord() == TokenKeyword.FUNCTION
                || tokenizer.keyWord() == TokenKeyword.METHOD
                || tokenizer.keyWord() == TokenKeyword.CONSTRUCTOR) {
            tokenizer.decrementTokenIndex();
            return;
        }

        printWriter.println("<classVarDec>");
        printWriter.println("<keyword> " + tokenizer.getCurrentToken() + " </keyword>");
        compileType();

        do {
            tokenizer.advance();
            printWriter.println("<identifier> " + tokenizer.identifier() + " </identifier>");

            // get next symbol (either ',' or ';')
            tokenizer.advance();
            if (tokenizer.symbol() == ',') {
                printWriter.println("<symbol> , </symbol>");
            } else {
                printWriter.println("<symbol> ; </symbol>");
                break;
            }
        } while (true);

        printWriter.println("</classVarDec>");

        compileClassVarDec();
    }

    public void compileSubroutine() {
        tokenizer.advance();

        // end of subroutine
        if (tokenizer.tokenType() == TokenType.SYMBOL && tokenizer.symbol() == '}') {
            tokenizer.decrementTokenIndex();
            return;
        }

        printWriter.println("<subroutineDec>");
        printWriter.println("<keyword> " + tokenizer.getCurrentToken() + " </keyword>");

        tokenizer.advance();
        if (tokenizer.tokenType() == TokenType.KEYWORD && tokenizer.keyWord() == TokenKeyword.VOID) {
            printWriter.println("<keyword> void </keyword>");
        } else {
            tokenizer.decrementTokenIndex();
            compileType();
        }

        tokenizer.advance();
        printWriter.println("<identifier> " + tokenizer.identifier() + " </identifier>");

        // parameterList
        processExpectedSymbol('(');
        printWriter.println("<parameterList>");
        compileParameterList();
        printWriter.println("</parameterList>");
        processExpectedSymbol(')');

        // subroutineBody
        printWriter.println("<subroutineBody>");
        processExpectedSymbol('{');
        compileVarDec();
        printWriter.println("<statements>");
        compileStatements();
        printWriter.println("</statements>");
        processExpectedSymbol('}');
        printWriter.println("</subroutineBody>");

        printWriter.println("</subroutineDec>");
        compileSubroutine();
    }

    public void compileParameterList() {
        tokenizer.advance();

        if (tokenizer.tokenType() == TokenType.SYMBOL && tokenizer.symbol() == ')') {
            tokenizer.decrementTokenIndex();
            return;
        }

        tokenizer.decrementTokenIndex();
        do {
            compileType();

            tokenizer.advance();
            printWriter.println("<identifier> " + tokenizer.identifier() + " </identifier>");

            tokenizer.advance();
            if (tokenizer.symbol() == ',') {
                printWriter.println("<symbol>,</symbol>");
            } else {
                tokenizer.decrementTokenIndex();
                break;
            }
        } while (true);
    }

    public void compileVarDec() {
        tokenizer.advance();

        if (tokenizer.tokenType() != TokenType.KEYWORD || tokenizer.keyWord() != TokenKeyword.VAR) {
            tokenizer.decrementTokenIndex();
            return;
        }

        printWriter.println("<varDec>");
        printWriter.println("<keyword> var </keyword>");
        compileType();

        do {
            tokenizer.advance();
            printWriter.println("<identifier> " + tokenizer.identifier() + " </identifier>");

            tokenizer.advance();
            if (tokenizer.symbol() == ',') {
                printWriter.println("<symbol> , </symbol>");
            } else {
                printWriter.println("<symbol> ; </symbol>");
                break;
            }
        } while (true);

        printWriter.println("</varDec>");
        compileVarDec();
    }

    public void compileStatements() {
        tokenizer.advance();

        if (tokenizer.tokenType() == TokenType.SYMBOL && tokenizer.symbol() == '}') {
            tokenizer.decrementTokenIndex();
            return;
        }

        if (tokenizer.tokenType() == TokenType.KEYWORD) {
            switch (tokenizer.keyWord()) {
                case LET: compileLet();break;
                case IF: compileIf();break;
                case WHILE: compileWhile();break;
                case DO: compileDo();break;
                case RETURN: compileReturn();break;
            }
        }

        compileStatements();
    }

    public void compileDo() {
        printWriter.println("<doStatement>");
        printWriter.println("<keyword> do </keyword>");
        compileSubroutineCall();
        processExpectedSymbol(';');
        printWriter.println("</doStatement>");
    }

    public void compileLet() {
        printWriter.println("<letStatement>");
        printWriter.println("<keyword> let </keyword>");

        tokenizer.advance();
        printWriter.println("<identifier> " + tokenizer.identifier() + " </identifier>");

        tokenizer.advance();
        boolean expExist = false;
        if (tokenizer.symbol() == '[') {
            expExist = true;
            printWriter.println("<symbol> [ </symbol>");
            compileExpression();
            processExpectedSymbol(']');
        }

        if (expExist) tokenizer.advance();

        printWriter.println("<symbol> = </symbol>");
        compileExpression();
        processExpectedSymbol(';');

        printWriter.println("</letStatement>");
    }

    public void compileWhile() {
        printWriter.println("<whileStatement>");

        printWriter.println("<keyword> while </keyword>");
        processExpectedSymbol('(');
        compileExpression();
        processExpectedSymbol(')');

        processExpectedSymbol('{');
        printWriter.println("<statements>");
        compileStatements();
        printWriter.println("</statements>");
        processExpectedSymbol('}');

        printWriter.println("</whileStatement>");
    }

    public void compileReturn() {
        printWriter.println("<returnStatement>");
        printWriter.println("<keyword> return </keyword>");

        tokenizer.advance();
        if (tokenizer.tokenType() == TokenType.SYMBOL && tokenizer.symbol() == ';') {
            printWriter.println("<symbol> ; </symbol>");
            printWriter.println("</returnStatement>");
            return;
        }

        tokenizer.decrementTokenIndex();
        compileExpression();
        processExpectedSymbol(';');
        printWriter.println("</returnStatement>");
    }

    public void compileIf() {
        printWriter.println("<ifStatement>");
        printWriter.println("<keyword> if </keyword>");
        processExpectedSymbol('(');
        compileExpression();
        processExpectedSymbol(')');

        processExpectedSymbol('{');
        printWriter.println("<statements>");
        compileStatements();
        printWriter.println("</statements>");
        processExpectedSymbol('}');

        tokenizer.advance();
        if (tokenizer.tokenType() == TokenType.KEYWORD && tokenizer.keyWord() == TokenKeyword.ELSE) {
            printWriter.println("<keyword> else </keyword>");
            processExpectedSymbol('{');
            printWriter.println("<statements>");
            compileStatements();
            printWriter.println("</statements>");
            processExpectedSymbol('}');
        } else {
            tokenizer.decrementTokenIndex();
        }

        printWriter.println("</ifStatement>");
    }

    public void compileExpression() {
        printWriter.println("<expression>");
        compileTerm();

        do {
            tokenizer.advance();
            if (tokenizer.tokenType() == TokenType.SYMBOL && tokenizer.isOperation()) {
                if (tokenizer.symbol() == '<') {
                    printWriter.write("<symbol> &lt; </symbol>\n");
                } else if (tokenizer.symbol() == '>') {
                    printWriter.write("<symbol> &gt; </symbol>\n");
                } else if (tokenizer.symbol() == '&') {
                    printWriter.write("<symbol> &amp; </symbol>\n");
                } else {
                    printWriter.write("<symbol> " + tokenizer.symbol() + " </symbol>\n");
                }
                compileTerm();
            } else {
                tokenizer.decrementTokenIndex();
                break;
            }
        } while (true);

        printWriter.println("</expression>");
    }

    public void compileTerm() {
        printWriter.println("<term>");

        tokenizer.advance();
        if (tokenizer.tokenType() == TokenType.IDENTIFIER) {
            String prevIdentifier = tokenizer.identifier();

            tokenizer.advance();
            if (tokenizer.tokenType() == TokenType.SYMBOL && tokenizer.symbol() == '[') {
                printWriter.println("<identifier> " + prevIdentifier + " </identifier>");
                printWriter.println("<symbol> [ </symbol>");
                compileExpression();
                processExpectedSymbol(']');
            } else if (tokenizer.tokenType() == TokenType.SYMBOL && (tokenizer.symbol() == '(' || tokenizer.symbol() == '.')) {
                tokenizer.decrementTokenIndex();
                tokenizer.decrementTokenIndex();
                compileSubroutineCall();
            } else {
                printWriter.println("<identifier> " + prevIdentifier + " </identifier>");
                tokenizer.decrementTokenIndex();
            }
        } else {
            if (tokenizer.tokenType() == TokenType.INT_CONST) {
                printWriter.println("<integerConstant> " + tokenizer.intVal() + " </integerConstant>");

            } else if (tokenizer.tokenType() == TokenType.STRING_CONST) {
                printWriter.println("<stringConstant> " + tokenizer.stringVal() + " </stringConstant>");
            } else if (tokenizer.tokenType() == TokenType.KEYWORD
                    && (tokenizer.keyWord() == TokenKeyword.THIS
                    || tokenizer.keyWord() == TokenKeyword.NULL
                    || tokenizer.keyWord() == TokenKeyword.FALSE
                    || tokenizer.keyWord() == TokenKeyword.TRUE)) {
                printWriter.println("<keyword> " + tokenizer.getCurrentToken() + " </keyword>");
            } else if (tokenizer.tokenType() == TokenType.SYMBOL && tokenizer.symbol() == '(') {
                printWriter.println("<symbol> ( </symbol>");
                compileExpression();
                processExpectedSymbol(')');
            } else if (tokenizer.tokenType() == TokenType.SYMBOL && (tokenizer.symbol() == '-' || tokenizer.symbol() == '~')) {
                printWriter.println("<symbol> " + tokenizer.symbol() + " </symbol>");
                compileTerm();
            }
        }

        printWriter.println("</term>");
    }

    public void compileExpressionList() {
        tokenizer.advance();

        if (tokenizer.tokenType() == TokenType.SYMBOL && tokenizer.symbol() == ')') {
            tokenizer.decrementTokenIndex();
        } else {
            tokenizer.decrementTokenIndex();
            compileExpression();

            do {
                tokenizer.advance();
                if (tokenizer.tokenType() == TokenType.SYMBOL && tokenizer.symbol() == ',') {
                    printWriter.println("<symbol> , </symbol>");
                    compileExpression();
                } else {
                    tokenizer.decrementTokenIndex();
                    break;
                }
            } while (true);
        }
    }

    public void compileType() {
        tokenizer.advance();

        boolean isValidType = false;

        if (tokenizer.tokenType() == TokenType.KEYWORD
                && (tokenizer.keyWord() == TokenKeyword.INT
                || tokenizer.keyWord() == TokenKeyword.CHAR
                || tokenizer.keyWord() == TokenKeyword.BOOLEAN)) {
            printWriter.println("<keyword> " + tokenizer.getCurrentToken() + " </keyword>");
            isValidType = true;
        }

        if (tokenizer.tokenType() == TokenType.IDENTIFIER) {
            printWriter.println("<identifier> " + tokenizer.identifier() + " </identifier>");
            isValidType = true;
        }

        if (!isValidType) throw new Error("Invalid type used" + tokenizer.getCurrentToken());
    }

    public void compileSubroutineCall() {
        tokenizer.advance();

        printWriter.println("<identifier> " + tokenizer.identifier() + " </identifier>");

        tokenizer.advance();
        if (tokenizer.tokenType() == TokenType.SYMBOL && tokenizer.symbol() == '(') {
            printWriter.println("<symbol> ( </symbol>");
            printWriter.println("<expressionList>");
            compileExpressionList();
            printWriter.println("</expressionList>");
            processExpectedSymbol(')');
        } else if (tokenizer.tokenType() == TokenType.SYMBOL && tokenizer.symbol() == '.') {
            printWriter.println("<symbol> . </symbol>");

            tokenizer.advance();
            printWriter.println("<identifier> " + tokenizer.identifier() + " </identifier>");

            processExpectedSymbol('(');
            printWriter.println("<expressionList>");
            compileExpressionList();
            printWriter.println("</expressionList>");
            processExpectedSymbol(')');
        }
    }

    public void processExpectedSymbol(char symbol) {
        tokenizer.advance();
        if (tokenizer.tokenType() == TokenType.SYMBOL && tokenizer.symbol() == symbol) {
            printWriter.println("<symbol> " + symbol + " </symbol>");
        } else {
            throw new Error("Could not find expected symbol: " + symbol);
        }
    }
}
