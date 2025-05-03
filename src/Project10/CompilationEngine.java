package Project10;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class CompilationEngine {
    private FileWriter fileWriter;
    private JackTokenizer jackTokenizer;
    private boolean bFirstRoutine;

    public CompilationEngine(File inFile, File outFile) {
        try {
            fileWriter = new FileWriter(outFile);
            jackTokenizer = new JackTokenizer(inFile);
        } catch (IOException e) {
            e.printStackTrace(System.out);
        }
        bFirstRoutine = true;
    }

    public void compileClass() {
        jackTokenizer.advance();

        try {
            fileWriter.write("<class>\n");
            fileWriter.write("<keyword> class </keyword>\n");
            jackTokenizer.advance();
            fileWriter.write("<identifier> " + jackTokenizer.identifier() + " </identifier>\n");
            jackTokenizer.advance();
            fileWriter.write("<symbol> { </symbol>\n");
            compileClassVarDec();
            compileSubroutine();
            fileWriter.write("<symbol> } </symbol>\n");
            fileWriter.write("</class>\n");
            fileWriter.close();
        } catch (IOException e) {
            e.printStackTrace(System.out);
        }
    }

    public void compileClassVarDec() {
        jackTokenizer.advance();

        try {
            while (jackTokenizer.keyWord() == TokenKeyword.STATIC
                    || jackTokenizer.keyWord() == TokenKeyword.FIELD) {
                fileWriter.write("<classVarDec>\n");
                // field or static
                fileWriter.write("<keyword> " + jackTokenizer.keyWord().lowercase + " </keyword>\n");

                jackTokenizer.advance();
                // variable type (class identifier or primitive keyword)
                if (jackTokenizer.tokenType() == TokenType.IDENTIFIER) {
                    fileWriter.write("<identifier> " + jackTokenizer.identifier() + " </identifier>\n");
                } else {
                    fileWriter.write("<keyword> " + jackTokenizer.keyWord().lowercase + " </keyword>\n");
                }

                jackTokenizer.advance();
                // variable name(s)
                fileWriter.write("<identifier> " + jackTokenizer.identifier() + " </identifier>\n");
                jackTokenizer.advance();
                if (jackTokenizer.symbol() == ',') {
                    fileWriter.write("<symbol> , </symbol>\n");
                    jackTokenizer.advance();
                    fileWriter.write(("<identifier> " + jackTokenizer.identifier() + " </identifier>\n"));
                    jackTokenizer.advance();
                }

                // semicolon
                fileWriter.write("<symbol> ; </symbol>\n");
                jackTokenizer.advance();
                fileWriter.write("</classVarDec>\n");
            }

            // if reach a subroutine, go back in the arraylist to accommodate for advance in the next call
            if (jackTokenizer.keyWord() == TokenKeyword.FUNCTION
                    || jackTokenizer.keyWord() == TokenKeyword.METHOD
                    || jackTokenizer.keyWord() == TokenKeyword.CONSTRUCTOR) {
                jackTokenizer.decrementTokenIndex();
            }
        } catch (IOException e) {
            e.printStackTrace(System.out);
        }
    }

    public void compileSubroutine() {
        boolean hasSubroutines = false;

        jackTokenizer.advance();
        try {
            // once reach the end, return  - no more subroutines - base case for the recursive call
            if (jackTokenizer.tokenType() == TokenType.SYMBOL && jackTokenizer.symbol() == '}') {
                return;
            }
            // subroutine dec tag
            if ((bFirstRoutine) && (jackTokenizer.keyWord() == TokenKeyword.FUNCTION
                    || jackTokenizer.keyWord() == TokenKeyword.METHOD
                    || jackTokenizer.keyWord() == TokenKeyword.CONSTRUCTOR)) {
                bFirstRoutine = false;
                fileWriter.write("<subroutineDec>\n");
                hasSubroutines = true;
            }
            // function ,e
            if (jackTokenizer.keyWord() == TokenKeyword.FUNCTION
                    || jackTokenizer.keyWord() == TokenKeyword.METHOD
                    || jackTokenizer.keyWord() == TokenKeyword.CONSTRUCTOR) {
                hasSubroutines = true;
                fileWriter.write("<keyword> " + jackTokenizer.keyWord().lowercase + " </keyword>\n");
                jackTokenizer.advance();
            }
            // if there is an identifier in the subroutine statement position 2 e.g. function Square getX()
            if (jackTokenizer.tokenType() == TokenType.IDENTIFIER) {
                fileWriter.write("<identifier> " + jackTokenizer.identifier() + " </identifier>\n");
                jackTokenizer.advance();
            }
            // if keyword instead for subroutine statement position 2 e.g. function int getX()
            else if (jackTokenizer.tokenType() == TokenType.KEYWORD) {
                fileWriter.write("<keyword> " + jackTokenizer.keyWord().lowercase + " </keyword>\n");
                jackTokenizer.advance();
            }

            // name of the subroutine
            if (jackTokenizer.tokenType() == TokenType.IDENTIFIER) {
                fileWriter.write("<identifier> " + jackTokenizer.identifier() + " </identifier>\n");
                jackTokenizer.advance();
            }

            // get parameters, or lack there of
            if (jackTokenizer.symbol() == '(') {
                fileWriter.write("<symbol> ( </symbol>\n");
                fileWriter.write("<parameterList>\n");

                compileParameterList();
                fileWriter.write("</parameterList>\n");
                fileWriter.write("<symbol> ) </symbol>\n");
            }
            jackTokenizer.advance();

            // start subroutine body
            if (jackTokenizer.symbol() == '{') {
                fileWriter.write("<subroutineBody>\n");
                fileWriter.write("<symbol> { </symbol>\n");
                jackTokenizer.advance();
            }

            // get all var declarations in the subroutine
            while (jackTokenizer.tokenType() == TokenType.KEYWORD
                    && jackTokenizer.keyWord() == TokenKeyword.VAR) {
                fileWriter.write("<varDec>\n ");
                jackTokenizer.decrementTokenIndex();
                compileVarDec();
                fileWriter.write(" </varDec>\n");
            }
            fileWriter.write("<statements>\n");
            compileStatements();
            fileWriter.write("</statements>\n");
            fileWriter.write("<symbol> " + jackTokenizer.symbol() + " </symbol>\n");
            if (hasSubroutines) {
                fileWriter.write("</subroutineBody>\n");
                fileWriter.write("</subroutineDec>\n");
                bFirstRoutine = true;
            }

            // recursive call
            compileSubroutine();
        } catch (IOException e) {
            e.printStackTrace(System.out);
        }
    }

    public void compileParameterList() {
        jackTokenizer.advance();

        try {
            // until reach the end - )
            while (!(jackTokenizer.tokenType() == TokenType.SYMBOL && jackTokenizer.symbol() == ')')) {
                if (jackTokenizer.tokenType() == TokenType.IDENTIFIER) {
                    fileWriter.write("<identifier> " + jackTokenizer.identifier() + " </identifier>\n");
                    jackTokenizer.advance();
                } else if (jackTokenizer.tokenType() == TokenType.KEYWORD) {
                    fileWriter.write("<keyword> " + jackTokenizer.keyWord().lowercase + " </keyword>\n");
                    jackTokenizer.advance();
                }
                // commas separate the list, if there are multiple
                else if (jackTokenizer.tokenType() == TokenType.SYMBOL && jackTokenizer.symbol() == ',') {
                    fileWriter.write("<symbol> , </symbol>\n");
                    jackTokenizer.advance();
                }
            }
        } catch (IOException e) {
            e.printStackTrace(System.out);
        }
    }

    public void compileVarDec() {
        jackTokenizer.advance();
        try {
            if (jackTokenizer.tokenType() == TokenType.KEYWORD && jackTokenizer.keyWord() == TokenKeyword.VAR) {
                fileWriter.write("<keyword> var </keyword>\n");
                jackTokenizer.advance();
            }
            // type of var, if identifier, e.g. Square or Array
            if (jackTokenizer.tokenType() == TokenType.IDENTIFIER) {
                fileWriter.write("<identifier> " + jackTokenizer.identifier() + " </identifier>\n");
                jackTokenizer.advance();
            }
            // type of var, if keyword, e.g. int or boolean
            else if (jackTokenizer.tokenType() == TokenType.KEYWORD) {
                fileWriter.write("<keyword> " + jackTokenizer.keyWord().lowercase + " </keyword>\n");
                jackTokenizer.advance();
            }
            // name of var
            if (jackTokenizer.tokenType() == TokenType.IDENTIFIER) {
                fileWriter.write("<identifier> " + jackTokenizer.identifier() + " </identifier>\n");
                jackTokenizer.advance();
            }
            // if there are mutliple in 1 line
            if ((jackTokenizer.tokenType() == TokenType.SYMBOL) && (jackTokenizer.symbol() == ',')) {
                fileWriter.write("<symbol> , </symbol>\n");
                jackTokenizer.advance();
                fileWriter.write(("<identifier> " + jackTokenizer.identifier() + " </identifier>\n"));
                jackTokenizer.advance();
            }
            // end of var line
            if ((jackTokenizer.tokenType() == TokenType.SYMBOL) && (jackTokenizer.symbol() == ';')) {
                fileWriter.write("<symbol> ; </symbol>\n");
                jackTokenizer.advance();

            }
        } catch (IOException e) {
            e.printStackTrace(System.out);
        }
    }

    public void compileStatements() {
        try {
            if (jackTokenizer.tokenType() == TokenType.SYMBOL && jackTokenizer.symbol() == '}') {
                return;
            } else if (jackTokenizer.keyWord() == TokenKeyword.DO && (jackTokenizer.tokenType() == TokenType.KEYWORD)) {
                fileWriter.write("<doStatement>\n ");
                compileDo();
                fileWriter.write((" </doStatement>\n"));

            } else if (jackTokenizer.keyWord() == TokenKeyword.LET && (jackTokenizer.tokenType() == TokenType.KEYWORD)) {
                fileWriter.write("<letStatement>\n ");
                compileLet();
                fileWriter.write((" </letStatement>\n"));
            } else if (jackTokenizer.keyWord() == TokenKeyword.IF && (jackTokenizer.tokenType() == TokenType.KEYWORD)) {
                fileWriter.write("<ifStatement>\n ");
                compileIf();
                fileWriter.write((" </ifStatement>\n"));
            } else if (jackTokenizer.keyWord() == TokenKeyword.WHILE && (jackTokenizer.tokenType() == TokenType.KEYWORD)) {
                fileWriter.write("<whileStatement>\n ");
                compileWhile();
                fileWriter.write((" </whileStatement>\n"));
            } else if (jackTokenizer.keyWord() == TokenKeyword.RETURN && (jackTokenizer.tokenType() == TokenType.KEYWORD)) {
                fileWriter.write("<returnStatement>\n ");
                compileReturn();
                fileWriter.write((" </returnStatement>\n"));
            }
            jackTokenizer.advance();
            compileStatements();
        } catch (IOException e) {
            e.printStackTrace(System.out);
        }
    }

    public void compileDo() {
        try {
            if (jackTokenizer.keyWord() == TokenKeyword.DO) {
                fileWriter.write("<keyword> do </keyword>\n");
            }
            // function call
            compileCall();
            // semi colon
            jackTokenizer.advance();
            fileWriter.write("<symbol> " + jackTokenizer.symbol() + " </symbol>\n");


        } catch (IOException e) {
            e.printStackTrace();

        }
    }

    public void compileCall() {
        jackTokenizer.advance();
        try {
            // first part
            fileWriter.write("<identifier> " + jackTokenizer.identifier() + " </identifier>\n");
            jackTokenizer.advance();
            // if . - then is something like Screen.erase()
            if ((jackTokenizer.tokenType() == TokenType.SYMBOL) && (jackTokenizer.symbol() == '.')) {
                fileWriter.write("<symbol> " + jackTokenizer.symbol() + " </symbol>\n");
                jackTokenizer.advance();
                fileWriter.write("<identifier> " + jackTokenizer.identifier() + " </identifier>\n");
                jackTokenizer.advance();
                fileWriter.write("<symbol> " + jackTokenizer.symbol() + " </symbol>\n");
                // parameters in the parentheses
                fileWriter.write("<expressionList>\n");
                compileExpressionList();
                fileWriter.write("</expressionList>\n");
                jackTokenizer.advance();
                fileWriter.write("<symbol> " + jackTokenizer.symbol() + " </symbol>\n");
            }
            // if ( then is something like erase()
            else if ((jackTokenizer.tokenType() == TokenType.SYMBOL) && (jackTokenizer.symbol() == '(')) {
                fileWriter.write("<symbol> " + jackTokenizer.symbol() + " </symbol>\n");
                fileWriter.write("<expressionList>\n");
                compileExpressionList();
                fileWriter.write("</expressionList>\n");
                // parentheses )
                jackTokenizer.advance();
                fileWriter.write("<symbol> " + jackTokenizer.symbol() + " </symbol>\n");
            }
        } catch (IOException e) {
            e.printStackTrace(System.out);
        }
    }

    public void compileLet() {
        try {
            fileWriter.write("<keyword>" + jackTokenizer.keyWord().lowercase + " </keyword>\n");
            jackTokenizer.advance();
            fileWriter.write("<identifier> " + jackTokenizer.identifier() + " </identifier>\n");
            jackTokenizer.advance();

            if ((jackTokenizer.tokenType() == TokenType.SYMBOL) && (jackTokenizer.symbol() == '[')) {
                // there is an expression -- because we have x[5] for example
                fileWriter.write("<symbol> " + jackTokenizer.symbol() + " </symbol>\n");
                compileExpression();
                jackTokenizer.advance();
                if (jackTokenizer.tokenType() == TokenType.SYMBOL && jackTokenizer.symbol() == ']') {
                    fileWriter.write("<symbol> " + jackTokenizer.symbol() + " </symbol>\n");
                }
                // only advance if there is an expression
                jackTokenizer.advance();
            }

            // = sign
            fileWriter.write("<symbol> " + jackTokenizer.symbol() + " </symbol>\n");

            compileExpression();
            // semi colon
            jackTokenizer.advance();
            fileWriter.write("<symbol> " + jackTokenizer.symbol() + " </symbol>\n");
            jackTokenizer.decrementTokenIndex();
            jackTokenizer.advance();
        } catch (IOException e) {
            e.printStackTrace(System.out);
        }
    }

    public void compileWhile() {
        try {
            // while
            fileWriter.write("<keyword> " + jackTokenizer.keyWord().lowercase + " </keyword>\n");
            jackTokenizer.advance();
            // (
            fileWriter.write("<symbol> " + jackTokenizer.symbol() + " </symbol>\n");
            // compile inside of () - expression
            compileExpression();
            // )
            jackTokenizer.advance();
            fileWriter.write("<symbol> " + jackTokenizer.symbol() + " </symbol>\n");
            jackTokenizer.advance();
            // {
            fileWriter.write("<symbol> " + jackTokenizer.symbol() + " </symbol>\n");
            // inside of while statement
            fileWriter.write("<statements>\n");
            compileStatements();
            fileWriter.write("</statements>\n");
            // }
            fileWriter.write("<symbol> " + jackTokenizer.symbol() + " </symbol>\n");

        } catch (IOException e) {
            e.printStackTrace(System.out);
        }
    }

    public void compileReturn() {
        try {
            fileWriter.write("<keyword> return </keyword>\n");
            jackTokenizer.advance();
            if (!((jackTokenizer.tokenType() == TokenType.SYMBOL && jackTokenizer.symbol() == ';'))) {
                jackTokenizer.decrementTokenIndex();
                compileExpression();
                jackTokenizer.advance();
            }
            if (jackTokenizer.tokenType() == TokenType.SYMBOL && jackTokenizer.symbol() == ';') {
                fileWriter.write("<symbol> ; </symbol>\n");
            }
        } catch (IOException e) {
            e.printStackTrace(System.out);
        }
    }

    public void compileIf() {
        try {
            fileWriter.write("<keyword> if </keyword>\n");
            jackTokenizer.advance();
            fileWriter.write("<symbol> ( </symbol>\n");
            // expression within if () condition
            compileExpression();
            fileWriter.write("<symbol> ) </symbol>\n");
            jackTokenizer.advance();
            fileWriter.write("<symbol> { </symbol>\n");
            jackTokenizer.advance();
            fileWriter.write("<statements>\n");
            // compile statements within if clause { }
            compileStatements();
            fileWriter.write("</statements>\n");
            fileWriter.write("<symbol> } </symbol>\n");
            jackTokenizer.advance();
            // if there is an else clause of the if statement
            if (jackTokenizer.tokenType() == TokenType.KEYWORD && jackTokenizer.keyWord() == TokenKeyword.ELSE) {
                fileWriter.write("<keyword> else </keyword>\n");
                jackTokenizer.advance();
                fileWriter.write("<symbol> { </symbol>\n");
                jackTokenizer.advance();
                fileWriter.write("<statements>\n");
                // compile statements within else clause
                compileStatements();
                fileWriter.write("</statements>\n");
                fileWriter.write("<symbol> } </symbol>\n");
            } else {
                // keep placeholder correct
                jackTokenizer.decrementTokenIndex();
            }
        } catch (IOException e) {
            e.printStackTrace(System.out);
        }
    }

    public void compileExpression() {
        try {
            fileWriter.write("<expression>\n");
            compileTerm();
            while (true) {
                jackTokenizer.advance();
                if (jackTokenizer.tokenType() == TokenType.SYMBOL && jackTokenizer.isOperation()) {
                    // < > & = have different xml code
                    if (jackTokenizer.symbol() == '<') {
                        fileWriter.write("<symbol> &lt; </symbol>\n");
                    } else if (jackTokenizer.symbol() == '>') {
                        fileWriter.write("<symbol> &gt; </symbol>\n");
                    } else if (jackTokenizer.symbol() == '&') {
                        fileWriter.write("<symbol> &amp; </symbol>\n");
                    } else {
                        fileWriter.write("<symbol> " + jackTokenizer.symbol() + " </symbol>\n");
                    }
                    compileTerm();
                } else {
                    jackTokenizer.decrementTokenIndex();
                    break;
                }
            }
            fileWriter.write("</expression>\n");
        } catch (IOException e) {
            e.printStackTrace(System.out);
        }
    }

    public void compileTerm() {
        try {
            fileWriter.write("<term>\n");
            jackTokenizer.advance();
            if (jackTokenizer.tokenType() == TokenType.IDENTIFIER) {
                String prevIdentifier = jackTokenizer.identifier();
                jackTokenizer.advance();
                // for [] terms
                if (jackTokenizer.tokenType() == TokenType.SYMBOL && jackTokenizer.symbol() == '[') {
                    fileWriter.write("<identifier> " + prevIdentifier + " </identifier>\n");
                    fileWriter.write("<symbol> [ </symbol>\n");
                    compileExpression();
                    jackTokenizer.advance();
                    fileWriter.write("<symbol> ] </symbol>\n");
                }
                // for ( or . - subroutine calls
                else if (jackTokenizer.tokenType() == TokenType.SYMBOL && (jackTokenizer.symbol() == '(' || jackTokenizer.symbol() == '.')) {
                    jackTokenizer.decrementTokenIndex();
                    jackTokenizer.decrementTokenIndex();
                    compileCall();

                } else {
                    fileWriter.write("<identifier> " + prevIdentifier + " </identifier>\n");
                    jackTokenizer.decrementTokenIndex();
                }
            } else {
                // integer
                if (jackTokenizer.tokenType() == TokenType.INT_CONST) {
                    fileWriter.write("<integerConstant> " + jackTokenizer.intVal() + " </integerConstant>\n");

                }
                // strings
                else if (jackTokenizer.tokenType() == TokenType.STRING_CONST) {
                    fileWriter.write("<stringConstant> " + jackTokenizer.stringVal() + " </stringConstant>\n");
                }
                // this true null or false
                else if (jackTokenizer.tokenType() == TokenType.KEYWORD
                        && (jackTokenizer.keyWord() == TokenKeyword.THIS || jackTokenizer.keyWord() == TokenKeyword.NULL
                        || jackTokenizer.keyWord() == TokenKeyword.FALSE
                        || jackTokenizer.keyWord() == TokenKeyword.TRUE)) {
                    fileWriter.write("<keyword> " + jackTokenizer.keyWord().lowercase + " </keyword>\n");
                }
                // parenthetical separation
                else if (jackTokenizer.tokenType() == TokenType.SYMBOL && jackTokenizer.symbol() == '(') {
                    fileWriter.write("<symbol> " + jackTokenizer.symbol() + " </symbol>\n");
                    compileExpression();
                    jackTokenizer.advance();
                    fileWriter.write("<symbol> " + jackTokenizer.symbol() + " </symbol>\n");
                }
                // unary operators
                else if (jackTokenizer.tokenType() == TokenType.SYMBOL && (jackTokenizer.symbol() == '-' || jackTokenizer.symbol() == '~')) {
                    fileWriter.write("<symbol> " + jackTokenizer.symbol() + " </symbol>\n");
                    // recursive call
                    compileTerm();
                }
            }
            fileWriter.write("</term>\n");

        } catch (IOException e) {
            e.printStackTrace(System.out);
        }
    }

    public void compileExpressionList() {
        jackTokenizer.advance();
        // end of list
        if (jackTokenizer.tokenType() == TokenType.SYMBOL && jackTokenizer.symbol() == ')') {
            jackTokenizer.decrementTokenIndex();
        } else {
            jackTokenizer.decrementTokenIndex();
            compileExpression();
        }
        while (true) {
            jackTokenizer.advance();
            if (jackTokenizer.tokenType() == TokenType.SYMBOL && jackTokenizer.symbol() == ',') {
                try {
                    fileWriter.write("<symbol> , </symbol>\n");
                } catch (IOException e) {
                    e.printStackTrace(System.out);
                }
                compileExpression();
            } else {
                jackTokenizer.decrementTokenIndex();
                break;
            }
        }
    }
}
