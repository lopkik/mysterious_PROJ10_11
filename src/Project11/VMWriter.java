package Project11;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

public class VMWriter {
    private PrintWriter printWriter;

    public VMWriter (File outFile) {
        try {
            printWriter = new PrintWriter(outFile);
        } catch (IOException e) {
            e.printStackTrace(System.out);
        }
    }

    public void writePush(Segment segment, int index) {
        printWriter.println("push " + segment.label + " " + index);
    }

    public void writePush(IdentifierKind kind, int index) {
        Segment segment = null;
        if (kind == IdentifierKind.VAR) {
            segment = Segment.LOCAL;
        } else if (kind == IdentifierKind.FIELD) {
            segment = Segment.THIS;
        } else if (kind == IdentifierKind.ARG) {
            segment = Segment.ARG;
        } else if (kind == IdentifierKind.STATIC) {
            segment = Segment.STATIC;
        }

        assert segment != null;
        printWriter.println("push " + segment.label + " " + index );
    }

    public void writePop(Segment segment, int index) {
        printWriter.println("pop " + segment.label + " " + index);
    }

    public void writePop(IdentifierKind kind, int index) {
        Segment segment = null;
        if (kind == IdentifierKind.VAR) {
            segment = Segment.LOCAL;
        } else if (kind == IdentifierKind.FIELD) {
            segment = Segment.THIS;
        } else if (kind == IdentifierKind.ARG) {
            segment = Segment.ARG;
        } else if (kind == IdentifierKind.STATIC) {
            segment = Segment.STATIC;
        }

        assert segment != null;
        printWriter.println("pop " + segment.label + " " + index);
    }

    public void writeArithmetic(Command command) {
        printWriter.println(command.label);
    }

    public void writeLabel(String label) {
        printWriter.println("label " + label);
    }

    public void writeGoto(String label) {
        printWriter.println("goto " + label);
    }

    public void writeIf(String label) {
        printWriter.println("if-goto " + label);
    }

    public void writeCall(String name, int nArgs) {
        printWriter.println("call " + name + " " + nArgs);
    }

    public void writeFunction(String name, int nLocals) {
        printWriter.println("function " + name + " " + nLocals);
    }

    public void writeReturn() {
        printWriter.println("return");
    }

    public void close() {
        printWriter.close();
    }
}
