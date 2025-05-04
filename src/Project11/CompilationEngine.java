package Project11;

import java.io.File;

public class CompilationEngine {
    private final JackTokenizer tokenizer;
    private final SymbolTable symboltable;
    private final VMWriter vmWriter;
    private String currClass = "";
    private String currSubroutine = "";

    private int labelIndex;
    
    public CompilationEngine(File inFile, File outFile) {
        tokenizer = new JackTokenizer(inFile);
        symboltable = new SymbolTable();
        vmWriter = new VMWriter(outFile);
        labelIndex = 0;
    }

    public void compileClass() {
        tokenizer.advance(); // class
        tokenizer.advance(); // className
        currClass = tokenizer.identifier();

        tokenizer.advance();
        if (tokenizer.tokenType() == TokenType.SYMBOL && tokenizer.symbol() == '.') {
            tokenizer.advance();
            currClass = currClass.concat("." + tokenizer.identifier());
        } else {
            tokenizer.decrementTokenIndex();
        }

        processExpectedSymbol('{');
        compileClassVarDec();
        compileSubroutine();
        processExpectedSymbol('}');

        vmWriter.close();
    }

    public void compileClassVarDec() {
        tokenizer.advance();

        // end of class
        if (tokenizer.tokenType() == TokenType.SYMBOL && tokenizer.symbol() == '}') {
            tokenizer.decrementTokenIndex();
            return;
        }

        // no classVarDec
        if (tokenizer.keyWord() == TokenKeyword.CONSTRUCTOR
                || tokenizer.keyWord() == TokenKeyword.FUNCTION
                || tokenizer.keyWord() == TokenKeyword.METHOD) {
            tokenizer.decrementTokenIndex();
            return;
        }

        while (tokenizer.keyWord() == TokenKeyword.STATIC
                || tokenizer.keyWord() == TokenKeyword.FIELD) {
            IdentifierKind kind = tokenizer.keyWord() == TokenKeyword.STATIC
                    ? IdentifierKind.STATIC : IdentifierKind.FIELD;

            String type = compileType();

            tokenizer.advance();
            symboltable.define(tokenizer.identifier(), type, kind);

            tokenizer.advance();
            while (tokenizer.symbol() == ',') {
                tokenizer.advance();
                symboltable.define(tokenizer.identifier(), type, kind);
                tokenizer.advance();
            }
            tokenizer.decrementTokenIndex();
            processExpectedSymbol(';');
        }

        compileClassVarDec();
    }

    public void compileSubroutine() {
        tokenizer.advance();

        if (tokenizer.tokenType() == TokenType.SYMBOL && tokenizer.symbol() == '}') {
            tokenizer.decrementTokenIndex();
            return;
        }

        TokenKeyword keyword = tokenizer.keyWord();
        symboltable.startSubroutine();
        if (tokenizer.keyWord() == TokenKeyword.METHOD) {
            symboltable.define("this", currClass, IdentifierKind.ARG);
        }

        tokenizer.advance();
        String type = "";
        if (tokenizer.tokenType() == TokenType.KEYWORD && tokenizer.keyWord() == TokenKeyword.VOID) {
            type = "void";
        } else  {
            tokenizer.decrementTokenIndex();
            type = compileType();
        }

        tokenizer.advance();
        currSubroutine = tokenizer.identifier();

        processExpectedSymbol('(');
        compileParameterList();
        processExpectedSymbol(')');

        // subroutine body
        processExpectedSymbol('{');
        compileVarDec();
        vmWriter.writeFunction(currClass + "." + currSubroutine, symboltable.varCount(IdentifierKind.VAR));
        if (keyword == TokenKeyword.METHOD) {
            vmWriter.writePush(Segment.ARG, 0);
            vmWriter.writePop(Segment.POINTER, 0);
        } else if (keyword == TokenKeyword.CONSTRUCTOR) {
            vmWriter.writePush(Segment.CONST, symboltable.varCount(IdentifierKind.FIELD));
            vmWriter.writeCall("Memory.alloc", 1);
            vmWriter.writePop(Segment.POINTER, 0);
        }
        compileStatements();
        processExpectedSymbol('}');

        compileSubroutine();
    }

    public void compileParameterList() {
        tokenizer.advance();

        if (tokenizer.tokenType() == TokenType.SYMBOL && tokenizer.symbol() == ')') {
            tokenizer.decrementTokenIndex();
            return;
        }

        String type;
        tokenizer.decrementTokenIndex();
        do {
            type = compileType();

            tokenizer.advance();
            symboltable.define(tokenizer.identifier(), type, IdentifierKind.ARG);

            tokenizer.advance();
            if (tokenizer.symbol() == ')') {
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

        String type = compileType();

        do {
            tokenizer.advance(); // get identifier token
            symboltable.define(tokenizer.identifier(), type, IdentifierKind.VAR);

            tokenizer.advance();
        } while (tokenizer.symbol() != ';');

        compileVarDec();
    }

    public void compileStatements() {
        tokenizer.advance();

        if (tokenizer.tokenType() == TokenType.SYMBOL && tokenizer.symbol() == '}') {
            tokenizer.decrementTokenIndex();
            return;
        } else if (tokenizer.tokenType() == TokenType.KEYWORD && tokenizer.keyWord() == TokenKeyword.DO) {
            compileDo();
        } else if (tokenizer.tokenType() == TokenType.KEYWORD && tokenizer.keyWord() == TokenKeyword.LET) {
            compileLet();
        } else if (tokenizer.tokenType() == TokenType.KEYWORD && tokenizer.keyWord() == TokenKeyword.IF) {
            compileIf();
        } else if (tokenizer.tokenType() == TokenType.KEYWORD && tokenizer.keyWord() == TokenKeyword.WHILE) {
            compileWhile();
        } else if (tokenizer.tokenType() == TokenType.KEYWORD && tokenizer.keyWord() == TokenKeyword.RETURN) {
            compileReturn();
        }

        compileStatements();
    }

    public void compileDo() {
        compileCall();
        processExpectedSymbol(';');
        vmWriter.writePop(Segment.TEMP, 0);
    }

    public void compileCall() {
        tokenizer.advance();

        String name = tokenizer.identifier();
        int nArgs = 0;

        tokenizer.advance();
        if (tokenizer.tokenType() == TokenType.SYMBOL && tokenizer.symbol() == '(') {
            vmWriter.writePush(Segment.POINTER, 0);
            nArgs = compileExpressionList() + 1;
            processExpectedSymbol(')');
            vmWriter.writeCall(currClass + "." + name, nArgs);
        } else if (tokenizer.tokenType() == TokenType.SYMBOL && tokenizer.symbol() == '.') {
            String objName = name;

            tokenizer.advance();
            name = tokenizer.identifier();
            String type = symboltable.typeOf(objName);

            if (type.isEmpty()) {
                name = objName + "." + name;
            } else {
                nArgs = 1;
                vmWriter.writePush(symboltable.kindOf(objName), symboltable.indexOf(objName));
                name = symboltable.typeOf(objName) + "." + name;
            }

            processExpectedSymbol('(');
            nArgs += compileExpressionList();
            processExpectedSymbol(')');
            vmWriter.writeCall(name, nArgs);
        }
    }

    public void compileLet() {
        tokenizer.advance();
        String name = tokenizer.identifier();
        System.out.println("CompileLet: " + name);

        boolean isExpression = false;
        tokenizer.advance();
        if (tokenizer.tokenType() == TokenType.SYMBOL && tokenizer.symbol() == '[') {
            isExpression = true;
            vmWriter.writePush(symboltable.kindOf(name), symboltable.indexOf(name));
            compileExpression();
            processExpectedSymbol(']');
            vmWriter.writeArithmetic(Command.ADD);
//            tokenizer.advance();
        }

        if (isExpression) tokenizer.advance();

        compileExpression();
        processExpectedSymbol(';');

        if (isExpression) {
            vmWriter.writePop(Segment.TEMP, 0);
            vmWriter.writePop(Segment.POINTER, 1);
            vmWriter.writePush(Segment.TEMP, 0);
            vmWriter.writePop(Segment.THAT, 0);
        } else {
            System.out.println(name + " " + symboltable.kindOf(name).name());
            vmWriter.writePop(symboltable.kindOf(name), symboltable.indexOf(name));
        }
    }

    public void compileWhile() {
        String breakLabel = "LABEL_" + labelIndex++;
        String whileLabel = "LABEL_" + labelIndex++;
        vmWriter.writeLabel(whileLabel);

        // while condition
        processExpectedSymbol('(');
        compileExpression();
        processExpectedSymbol(')');

        // go to breakLabel when while condition is false
        vmWriter.writeArithmetic(Command.NOT);
        vmWriter.writeIf(breakLabel);

        // while body
        processExpectedSymbol('{');
        compileStatements();
        processExpectedSymbol('}');


        vmWriter.writeGoto(whileLabel);
        vmWriter.writeLabel(breakLabel);
    }

    public void compileReturn() {
        tokenizer.advance();

        if (tokenizer.tokenType() == TokenType.SYMBOL && tokenizer.symbol() == ';') {
            vmWriter.writePush(Segment.CONST, 0);
        } else {
            tokenizer.decrementTokenIndex();
            compileExpression();
            processExpectedSymbol(';');
        }

        vmWriter.writeReturn();
    }

    public void compileIf() {
        String elseLabel = "LABEL_" + labelIndex++;
        String endIfLabel = "LABEL_" + labelIndex++;

        // if condition
        processExpectedSymbol('(');
        compileExpression();
        processExpectedSymbol(')');

        // go to elseLabel when if condition is false
        vmWriter.writeArithmetic(Command.NOT);
        vmWriter.writeIf(elseLabel);

        // if body
        processExpectedSymbol('{');
        compileStatements();
        processExpectedSymbol('}');

        // after if body, go to the endLabel
        vmWriter.writeGoto(endIfLabel);
        vmWriter.writeLabel(elseLabel);

        tokenizer.advance();
        if (tokenizer.tokenType() == TokenType.KEYWORD && tokenizer.keyWord() == TokenKeyword.ELSE) {
            // else body
            processExpectedSymbol('{');
            compileStatements();
            processExpectedSymbol('}');
        } else {
            tokenizer.decrementTokenIndex();
        }
        vmWriter.writeLabel(endIfLabel);
    }

    public void compileExpression() {
        compileTerm();

        while (true) {
            tokenizer.advance();
            if (tokenizer.tokenType() == TokenType.SYMBOL && tokenizer.isOperation()) {
                if (tokenizer.symbol() == '<') {
                    compileTerm();
                    vmWriter.writeArithmetic(Command.LT);
                } else if (tokenizer.symbol() == '>') {
                    compileTerm();
                    vmWriter.writeArithmetic(Command.GT);
                } else if (tokenizer.symbol() == '&') {
                    compileTerm();
                    vmWriter.writeArithmetic(Command.AND);
                } else if (tokenizer.symbol() == '+') {
                    compileTerm();
                    vmWriter.writeArithmetic(Command.ADD);
                } else if (tokenizer.symbol() == '-') {
                    compileTerm();
                    vmWriter.writeArithmetic(Command.SUB);
                } else if (tokenizer.symbol() == '*') {
                    compileTerm();
                    vmWriter.writeCall("Math.multiply", 2);
                } else if (tokenizer.symbol() == '/') {
                    compileTerm();
                    vmWriter.writeCall("Math.divide", 2);
                } else if (tokenizer.symbol() == '=') {
                    compileTerm();
                    vmWriter.writeArithmetic(Command.EQ);
                } else if (tokenizer.symbol() == '|') {
                    compileTerm();
                    vmWriter.writeArithmetic(Command.OR);
                }
            } else {
                tokenizer.decrementTokenIndex();
                break;
            }
        }
    }

    public void compileTerm() {
        tokenizer.advance();

        if (tokenizer.tokenType() == TokenType.IDENTIFIER) {
            String prevIdentifier = tokenizer.identifier();
            tokenizer.advance();
            // for [] terms
            if (tokenizer.tokenType() == TokenType.SYMBOL && tokenizer.symbol() == '[') {
                // push the array start
                vmWriter.writePush(symboltable.kindOf(prevIdentifier), symboltable.indexOf(prevIdentifier));
                compileExpression();
                processExpectedSymbol(']');

                vmWriter.writeArithmetic(Command.ADD);
                vmWriter.writePop(Segment.POINTER, 1);
                vmWriter.writePush(Segment.THAT, 0);
            } else if (tokenizer.tokenType() == TokenType.SYMBOL && (tokenizer.symbol() == '(' || tokenizer.symbol() == '.')) {
                tokenizer.decrementTokenIndex();
                tokenizer.decrementTokenIndex();
                compileCall();
            } else {
                tokenizer.decrementTokenIndex();

                vmWriter.writePush(symboltable.kindOf(prevIdentifier), symboltable.indexOf(prevIdentifier));
            }
        } else {
            // integer
            if (tokenizer.tokenType() == TokenType.INT_CONST) {
                vmWriter.writePush(Segment.CONST, tokenizer.intVal());
            } else if (tokenizer.tokenType() == TokenType.STRING_CONST) {
                String tokenStringVal = tokenizer.stringVal();

                vmWriter.writePush(Segment.CONST, tokenStringVal.length());
                vmWriter.writeCall("String.new", 1);

                for (int i = 0; i < tokenStringVal.length(); i++) {
                    vmWriter.writePush(Segment.CONST, tokenStringVal.charAt(i));
                    vmWriter.writeCall("String.appendChar", 2);
                }
            } else if (tokenizer.tokenType() == TokenType.KEYWORD && tokenizer.keyWord() == TokenKeyword.THIS) {
                vmWriter.writePush(Segment.POINTER, 0);
            }
            // false and null - 0
            else if (tokenizer.tokenType() == TokenType.KEYWORD && (tokenizer.keyWord() == TokenKeyword.NULL || tokenizer.keyWord() == TokenKeyword.FALSE)) {
                vmWriter.writePush(Segment.CONST, 0);
            }
            // true - not 0
            else if (tokenizer.tokenType() == TokenType.KEYWORD && tokenizer.keyWord() == TokenKeyword.TRUE) {
                vmWriter.writePush(Segment.CONST, 0);
                vmWriter.writeArithmetic(Command.NOT);
            }
            // parenthetical separation
            else if (tokenizer.tokenType() == TokenType.SYMBOL && tokenizer.symbol() == '(') {
                compileExpression();
                processExpectedSymbol(')');
            }
            // unary operators
            else if (tokenizer.tokenType() == TokenType.SYMBOL && (tokenizer.symbol() == '-' || tokenizer.symbol() == '~')) {
                char symbol = tokenizer.symbol();
                // recursive call
                compileTerm();
                if (symbol == '-') {
                    vmWriter.writeArithmetic(Command.NEG);
                } else if (symbol == '~') {
                    vmWriter.writeArithmetic(Command.NOT);
                }
            }
        }
    }

    public int compileExpressionList() {
        int nArguments = 0;

        tokenizer.advance();
        if (tokenizer.tokenType() == TokenType.SYMBOL && tokenizer.symbol() == ')') {
            tokenizer.decrementTokenIndex();
            return nArguments;
        } else {
            nArguments = 1;
            tokenizer.decrementTokenIndex();
            compileExpression();
            do {
                tokenizer.advance();
                if (tokenizer.tokenType() == TokenType.SYMBOL && tokenizer.symbol() == ',') {
                    compileExpression();
                    nArguments++;
                } else {
                    tokenizer.decrementTokenIndex();
                    break;
                }
            } while (true);
        }
        return nArguments;
    }

    public void processExpectedSymbol(char symbol) {
        tokenizer.advance();
        if (tokenizer.tokenType() != TokenType.SYMBOL || tokenizer.symbol() != symbol) {
            throw new Error("Could not find expected symbol: " + symbol);
        }
    }

    public String compileType() {
        tokenizer.advance();

        if (tokenizer.tokenType() == TokenType.KEYWORD
                && (tokenizer.keyWord() == TokenKeyword.INT
                || tokenizer.keyWord() == TokenKeyword.CHAR
                || tokenizer.keyWord() == TokenKeyword.BOOLEAN)) {
            return tokenizer.getCurrentToken();
        }

        if (tokenizer.tokenType() == TokenType.IDENTIFIER) {
            return tokenizer.identifier();
        }

        throw new Error("Invalid type used" + tokenizer.getCurrentToken());
    }
}
