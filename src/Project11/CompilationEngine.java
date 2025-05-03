package Project11;

import java.io.File;

public class CompilationEngine {
    private final JackTokenizer jackTokenizer;
    private final SymbolTable symboltable;
    private final VMWriter vmWriter;
    private String strClassName = "";
    private String strSubRoutineName = "";

    private int nLabelIndex;
    
    public CompilationEngine(File inFile, File outFile) {
        jackTokenizer = new JackTokenizer(inFile);
        symboltable = new SymbolTable();
        vmWriter = new VMWriter(outFile);
        nLabelIndex = 0;
    }

    public void compileClass() {
        jackTokenizer.advance();
        jackTokenizer.advance();

        strClassName = jackTokenizer.identifier();
        jackTokenizer.advance();
        compileClassVarDec();
        compileSubroutine();

        vmWriter.close();
    }

    public void compileClassVarDec() {
        jackTokenizer.advance();

        while (jackTokenizer.keyWord() == TokenKeyword.STATIC
                || jackTokenizer.keyWord() == TokenKeyword.FIELD) {
            IdentifierKind kind = jackTokenizer.keyWord() == TokenKeyword.STATIC
                    ? IdentifierKind.STATIC : IdentifierKind.FIELD;

            jackTokenizer.advance();
            String type = jackTokenizer.tokenType() == TokenType.IDENTIFIER
                    ? jackTokenizer.identifier() : jackTokenizer.keyWord().lowercase;

            jackTokenizer.advance();
            symboltable.define(jackTokenizer.identifier(), type, kind);
            jackTokenizer.advance();

            while (jackTokenizer.symbol() == ',') {
                jackTokenizer.advance();
                symboltable.define(jackTokenizer.identifier(), type, kind);
                jackTokenizer.advance();
            }
            jackTokenizer.advance();
        }
    }

    public void compileSubroutine() {
        jackTokenizer.advance();

        if (jackTokenizer.tokenType() == TokenType.SYMBOL && jackTokenizer.symbol() == '}') {
            return;
        }

        TokenKeyword keyword = null;
        if (jackTokenizer.keyWord() == TokenKeyword.FUNCTION
                || jackTokenizer.keyWord() == TokenKeyword.METHOD
                || jackTokenizer.keyWord() == TokenKeyword.CONSTRUCTOR) {
            keyword = jackTokenizer.keyWord();
            symboltable.startSubroutine();
            if (jackTokenizer.keyWord() == TokenKeyword.METHOD) {
                symboltable.define("this", strClassName, IdentifierKind.ARG);
            }
            jackTokenizer.advance();
        }

        String type = "";
        if (jackTokenizer.tokenType() == TokenType.KEYWORD && (
                jackTokenizer.keyWord() == TokenKeyword.VOID
                || jackTokenizer.keyWord() == TokenKeyword.INT
                || jackTokenizer.keyWord() == TokenKeyword.BOOLEAN
                || jackTokenizer.keyWord() == TokenKeyword.CHAR
                )) {
            type = jackTokenizer.keyWord().lowercase;
            jackTokenizer.advance();
        } else  {
            type = jackTokenizer.identifier();
            jackTokenizer.advance();
        }

        if (jackTokenizer.tokenType() == TokenType.IDENTIFIER) {
            strSubRoutineName = jackTokenizer.identifier();
            jackTokenizer.advance();
        }

        if (jackTokenizer.tokenType() == TokenType.SYMBOL && jackTokenizer.symbol() == '(') {
            compileParameterList();
        }
        jackTokenizer.advance();

        if (jackTokenizer.tokenType() == TokenType.SYMBOL && jackTokenizer.symbol() == '{') {
            jackTokenizer.advance();
        }

        while (jackTokenizer.tokenType() == TokenType.KEYWORD && jackTokenizer.keyWord() == TokenKeyword.VAR) {
            jackTokenizer.decrementTokenIndex();
            compileVarDec();
        }

        String function = "";
        if (!strClassName.isEmpty() && !strSubRoutineName.isEmpty()) {
            function = strClassName + "." + strSubRoutineName;
        }
        vmWriter.writeFunction(function, symboltable.varCount(IdentifierKind.VAR));

        if (keyword == TokenKeyword.METHOD) {
            vmWriter.writePush(Segment.ARG, 0);
            vmWriter.writePop(Segment.POINTER, 0);
        } else if (keyword == TokenKeyword.CONSTRUCTOR) {
            vmWriter.writePush(Segment.CONST, symboltable.varCount(IdentifierKind.FIELD));
            vmWriter.writeCall("Memory.alloc", 1);
            vmWriter.writePop(Segment.POINTER, 0);
        }

        compileStatements();

        compileSubroutine();
    }

    public void compileParameterList() {
        jackTokenizer.advance();

        String type = "";
        String name = "";
        String prev;
        boolean hasParam = false;

        while (!(jackTokenizer.tokenType() == TokenType.SYMBOL && jackTokenizer.symbol() == ')')) {
            if (jackTokenizer.tokenType() == TokenType.KEYWORD) {
                hasParam = true;
                type = jackTokenizer.keyWord().lowercase;
            } else if (jackTokenizer.tokenType() == TokenType.IDENTIFIER) {
                type = jackTokenizer.identifier();
            }

            jackTokenizer.advance();
            if (jackTokenizer.tokenType() == TokenType.IDENTIFIER) {
                name = jackTokenizer.identifier();
            }

            jackTokenizer.advance();
            if (jackTokenizer.tokenType() == TokenType.SYMBOL && jackTokenizer.symbol() == ',') {
                symboltable.define(name, type, IdentifierKind.ARG);
                jackTokenizer.advance();
            }
        }

        if (hasParam) {
            symboltable.define(name, type, IdentifierKind.ARG);
        }
    }

    public void compileVarDec() {
        jackTokenizer.advance();

        String type = "";
        String name = "";
        if (jackTokenizer.tokenType() == TokenType.KEYWORD && jackTokenizer.keyWord() == TokenKeyword.VAR) {
            jackTokenizer.advance();
        }

        // type of var, if identifier, e.g. Square or Array
        if (jackTokenizer.tokenType() == TokenType.IDENTIFIER) {
            type = jackTokenizer.identifier();
        } else if (jackTokenizer.tokenType() == TokenType.KEYWORD) {
            type = jackTokenizer.keyWord().lowercase;
        }
        jackTokenizer.advance();

        // name of var
        if (jackTokenizer.tokenType() == TokenType.IDENTIFIER) {
            name = jackTokenizer.identifier();
            jackTokenizer.advance();
        }
        symboltable.define(name, type, IdentifierKind.VAR);

        // if there are multiple in 1 line
        while (jackTokenizer.tokenType() == TokenType.SYMBOL && jackTokenizer.symbol() == ',') {
            jackTokenizer.advance();
            name = jackTokenizer.identifier();
            symboltable.define(name, type, IdentifierKind.VAR);

            jackTokenizer.advance();
        }

        // end of var line
        if ((jackTokenizer.tokenType() == TokenType.SYMBOL) && (jackTokenizer.symbol() == ';')) {
            jackTokenizer.advance();
        }
    }

    public void compileStatements() {
        if (jackTokenizer.tokenType() == TokenType.SYMBOL && jackTokenizer.symbol() == '}') {
            return;
        } else if (jackTokenizer.tokenType() == TokenType.KEYWORD && jackTokenizer.keyWord() == TokenKeyword.DO) {
            compileDo();
        } else if (jackTokenizer.tokenType() == TokenType.KEYWORD && jackTokenizer.keyWord() == TokenKeyword.LET) {
            compileLet();
        } else if (jackTokenizer.tokenType() == TokenType.KEYWORD && jackTokenizer.keyWord() == TokenKeyword.IF) {
            compileIf();
        } else if (jackTokenizer.tokenType() == TokenType.KEYWORD && jackTokenizer.keyWord() == TokenKeyword.WHILE) {
            compileWhile();
        } else if (jackTokenizer.tokenType() == TokenType.KEYWORD && jackTokenizer.keyWord() == TokenKeyword.RETURN) {
            compileReturn();
        }
        jackTokenizer.advance();
        compileStatements();
    }

    public void compileDo() {
        compileCall();
        // semi colon
        jackTokenizer.advance();
        vmWriter.writePop(Segment.TEMP, 0);
    }

    public void compileCall() {
        jackTokenizer.advance();

        String firstIdentifier = jackTokenizer.identifier();
        int nArguments = 0;
        jackTokenizer.advance();
        // if . - then is something like Screen.erase()
        if (jackTokenizer.tokenType() == TokenType.SYMBOL && jackTokenizer.symbol() == '.') {
            jackTokenizer.advance();
            String secondIdentifier = jackTokenizer.identifier();
            String callName;
            String strType = symboltable.typeOf(firstIdentifier);
            if (strType.isEmpty()) {
                callName = firstIdentifier + "." + secondIdentifier;
            } else {
                nArguments = 1;
                vmWriter.writePush(symboltable.kindOf(firstIdentifier), symboltable.indexOf(firstIdentifier));
                callName = symboltable.typeOf(firstIdentifier) + "." + firstIdentifier;
            }

            // parameters in the parentheses
            nArguments += compileExpressionList();
            jackTokenizer.decrementTokenIndex();
            jackTokenizer.advance();
            vmWriter.writeCall(callName, nArguments);
        }

        // if ( then is something like erase()
        else if (jackTokenizer.tokenType() == TokenType.SYMBOL && jackTokenizer.symbol() == '(') {
            vmWriter.writePush(Segment.POINTER, 0);

            nArguments = compileExpressionList() + 1;
            // parentheses )
            jackTokenizer.advance();
            vmWriter.writeCall(strClassName + "." + firstIdentifier, nArguments);


        }
    }

    public void compileLet() {
        jackTokenizer.advance();
        String strVariableName = jackTokenizer.identifier();
        jackTokenizer.advance();
        boolean bArray = false;
        if (jackTokenizer.tokenType() == TokenType.SYMBOL && jackTokenizer.symbol() == '[') {
            // there is an expression (array) -- because we have x[5] for example
            bArray = true;
            compileExpression();
            vmWriter.writePush(symboltable.kindOf(strVariableName), symboltable.indexOf(strVariableName));
            jackTokenizer.advance();
            // add array start to number in array
            vmWriter.writeArithmetic(Command.ADD);
            // only advance if there is an expression
            jackTokenizer.advance();
        }

        // = sign

        compileExpression();
        // semi colon
        jackTokenizer.advance();
        if (bArray) {
            // pop into temp value and into pointer to hold for that
            vmWriter.writePop(Segment.TEMP, 0);
            vmWriter.writePop(Segment.POINTER, 1);
            // put the value into that
            vmWriter.writePush(Segment.TEMP, 0);
            vmWriter.writePop(Segment.THAT, 0);
        } else {
            // pop directly
            System.out.print("let variable: ");
            System.out.println(strVariableName);
            System.out.println(symboltable.kindOf(strVariableName));
            vmWriter.writePop(symboltable.kindOf(strVariableName), symboltable.indexOf(strVariableName));
        }
    }

    public void compileWhile() {
        String secondLabel = "LABEL_" + nLabelIndex++;
        String firstLabel = "LABEL_" + nLabelIndex++;
        vmWriter.writeLabel(firstLabel);
        // while
        jackTokenizer.advance();
        // (
        // compile inside of () - expression
        compileExpression();
        // )
        jackTokenizer.advance();
        // if not condition, go to the next label
        vmWriter.writeArithmetic(Command.NOT);
        vmWriter.writeIf(secondLabel);
        jackTokenizer.advance();
        // {
        // inside of while statement
        compileStatements();
        // }
        // if condition go to first label
        vmWriter.writeGoto(firstLabel);
        // otherwise go to next label
        vmWriter.writeLabel(secondLabel);
    }

    public void compileReturn() {
        jackTokenizer.advance();
        if (!(jackTokenizer.tokenType() == TokenType.SYMBOL && jackTokenizer.symbol() == ';')) {
            jackTokenizer.decrementTokenIndex();
            compileExpression();
        } else if (jackTokenizer.tokenType() == TokenType.SYMBOL && jackTokenizer.symbol() == ';') {
            vmWriter.writePush(Segment.CONST, 0);
        }
        vmWriter.writeReturn();
    }

    public void compileIf() {
        String strLabelElse = "LABEL_" + nLabelIndex++;
        String strLabelEnd = "LABEL_" + nLabelIndex++;
        jackTokenizer.advance();
        // expression within if () condition
        compileExpression();
        jackTokenizer.advance();
        // if not condition got to label else
        vmWriter.writeArithmetic(Command.NOT);
        vmWriter.writeIf(strLabelElse);
        jackTokenizer.advance();
        // compile statements within if clause { }
        compileStatements();
        // after statement finishes, go to the end label
        vmWriter.writeGoto(strLabelEnd);
        vmWriter.writeLabel(strLabelElse);
        jackTokenizer.advance();
        // if there is an else clause of the if statement
        if (jackTokenizer.tokenType() == TokenType.KEYWORD && jackTokenizer.keyWord() == TokenKeyword.ELSE) {
            jackTokenizer.advance();
            jackTokenizer.advance();
            // compile statements within else clause
            compileStatements();
        } else {
            // keep placeholder correct
            jackTokenizer.decrementTokenIndex();
        }
        vmWriter.writeLabel(strLabelEnd);
    }

    public void compileExpression() {
        compileTerm();
        while (true) {
            jackTokenizer.advance();
            if (jackTokenizer.tokenType() == TokenType.SYMBOL && jackTokenizer.isOperation()) {
                // < > & = have different xml code
                if (jackTokenizer.symbol() == '<') {
                    compileTerm();
                    vmWriter.writeArithmetic(Command.LT);
                } else if (jackTokenizer.symbol() == '>') {
                    compileTerm();
                    vmWriter.writeArithmetic(Command.GT);
                } else if (jackTokenizer.symbol() == '&') {
                    compileTerm();
                    vmWriter.writeArithmetic(Command.AND);
                } else if (jackTokenizer.symbol() == '+') {
                    compileTerm();
                    vmWriter.writeArithmetic(Command.ADD);
                } else if (jackTokenizer.symbol() == '-') {
                    compileTerm();
                    vmWriter.writeArithmetic(Command.SUB);
                } else if (jackTokenizer.symbol() == '*') {
                    compileTerm();
                    vmWriter.writeCall("Math.multiply", 2);
                } else if (jackTokenizer.symbol() == '/') {
                    compileTerm();
                    vmWriter.writeCall("Math.divide", 2);

                } else if (jackTokenizer.symbol() == '=') {
                    compileTerm();
                    vmWriter.writeArithmetic(Command.EQ);
                } else if (jackTokenizer.symbol() == '|') {
                    compileTerm();
                    vmWriter.writeArithmetic(Command.OR);
                }
            } else {
                jackTokenizer.decrementTokenIndex();
                break;
            }
        }
    }

    public void compileTerm() {
        jackTokenizer.advance();
        if (jackTokenizer.tokenType() == TokenType.IDENTIFIER) {
            String prevIdentifier = jackTokenizer.identifier();
            jackTokenizer.advance();
            // for [] terms
            if (jackTokenizer.tokenType() == TokenType.SYMBOL && jackTokenizer.symbol() == '[') {
                // push the array start
                vmWriter.writePush(symboltable.kindOf(prevIdentifier), symboltable.indexOf(prevIdentifier));
                compileExpression();
                jackTokenizer.advance();
                // add array number to array start, pop into pointer for that, and push into that
                vmWriter.writeArithmetic(Command.ADD);
                vmWriter.writePop(Segment.POINTER, 1);
                vmWriter.writePush(Segment.THAT, 0);
            }
            // for ( or . - subroutine calls
            else if (jackTokenizer.tokenType() == TokenType.SYMBOL && (jackTokenizer.symbol() == '(' || jackTokenizer.symbol() == '.')) {
                jackTokenizer.decrementTokenIndex();
                jackTokenizer.decrementTokenIndex();
                compileCall();

            } else {
                jackTokenizer.decrementTokenIndex();
                System.out.print("prevIdentifier: ");
                System.out.print(prevIdentifier);
                System.out.println(symboltable.kindOf(prevIdentifier));

                vmWriter.writePush(symboltable.kindOf(prevIdentifier), symboltable.indexOf(prevIdentifier));
            }
        } else {
            // integer
            if (jackTokenizer.tokenType() == TokenType.INT_CONST) {
                vmWriter.writePush(Segment.CONST, jackTokenizer.intVal());

            }
            // strings
            else if (jackTokenizer.tokenType() == TokenType.STRING_CONST) {
                String strToken = jackTokenizer.stringVal();
                vmWriter.writePush(Segment.CONST, strToken.length());
                vmWriter.writeCall("String.new", 1);
                for (int i = 0; i < strToken.length(); i++) {
                    vmWriter.writePush(Segment.CONST, (int) strToken.charAt(i));
                    vmWriter.writeCall("String.appendChar", 2);
                }
            }
            // this - push this pointer
            else if (jackTokenizer.tokenType() == TokenType.KEYWORD && jackTokenizer.keyWord() == TokenKeyword.THIS) {
                vmWriter.writePush(Segment.POINTER, 0);
            }
            // false and null - 0
            else if (jackTokenizer.tokenType() == TokenType.KEYWORD && (jackTokenizer.keyWord() == TokenKeyword.NULL || jackTokenizer.keyWord() == TokenKeyword.FALSE)) {
                vmWriter.writePush(Segment.CONST, 0);

            }
            // true - not 0
            else if (jackTokenizer.tokenType() == TokenType.KEYWORD && jackTokenizer.keyWord() == TokenKeyword.TRUE) {
                vmWriter.writePush(Segment.CONST, 0);
                vmWriter.writeArithmetic(Command.NOT);
            }

            // parenthetical separation
            else if (jackTokenizer.tokenType() == TokenType.SYMBOL && jackTokenizer.symbol() == '(') {
                compileExpression();
                jackTokenizer.advance();
            }
            // unary operators
            else if (jackTokenizer.tokenType() == TokenType.SYMBOL && (jackTokenizer.symbol() == '-' || jackTokenizer.symbol() == '~')) {
                char symbol = jackTokenizer.symbol();
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
        jackTokenizer.advance();
        // end of list
        if (jackTokenizer.tokenType() == TokenType.SYMBOL && jackTokenizer.symbol() == ')') {
            jackTokenizer.decrementTokenIndex();
        } else {
            nArguments = 1;
            jackTokenizer.decrementTokenIndex();
            compileExpression();
        }
        while (true) {
            jackTokenizer.advance();
            if (jackTokenizer.tokenType() == TokenType.SYMBOL && jackTokenizer.symbol() == ',') {
                compileExpression();
                nArguments++;
            } else {
                jackTokenizer.decrementTokenIndex();
                break;
            }
        }
        return nArguments;
    }
}
