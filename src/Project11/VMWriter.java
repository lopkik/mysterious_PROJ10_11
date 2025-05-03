package Project11;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class VMWriter {
    private FileWriter fileWriter;

    public VMWriter (File outFile) {
        try {
            fileWriter = new FileWriter(outFile);
        } catch (IOException e) {
            e.printStackTrace(System.out);
        }
    }

    public void writePush(Segment segment, int index) {
        try {
            fileWriter.write("push " + segment.label + " " + index + "\n");
        } catch (IOException e) {
            e.printStackTrace(System.out);
        }
    }

    public void writePush(IdentifierKind kind, int index) {
        Segment segment = null;
        if (kind == IdentifierKind.VAR) {
            segment = Segment.LOCAL;
        } else if (kind == IdentifierKind.FIELD) {
            segment = Segment.THIS;
        } else if (kind == IdentifierKind.ARG) {
            segment = Segment.ARG;
        }

        try {
            assert segment != null;
            fileWriter.write("push " + segment.label + " " + index + "\n");
        } catch (IOException e) {
            e.printStackTrace(System.out);
        }
    }

    public void writePop(Segment segment, int index) {
        try {
            fileWriter.write("pop " + segment.label + " " + index + "\n");
        } catch (IOException e) {
            e.printStackTrace(System.out);
        }
    }

    public void writePop(IdentifierKind kind, int index) {
        Segment segment = null;
        if (kind == IdentifierKind.VAR) {
            segment = Segment.LOCAL;
        } else if (kind == IdentifierKind.FIELD) {
            segment = Segment.THIS;
        } else if (kind == IdentifierKind.ARG) {
            segment = Segment.ARG;
        }

        try {
            assert segment != null;
            fileWriter.write("pop " + segment.label + " " + index + "\n");
        } catch (IOException e) {
            e.printStackTrace(System.out);
        }
    }

    public void writeArithmetic(Command command) {
        try {
            fileWriter.write(command.label + "\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void writeLabel(String label) {
        try {
            fileWriter.write("label " + label + "\n");
        } catch (IOException e) {
            e.printStackTrace(System.out);
        }
    }

    public void writeGoto(String label) {
        try {
            fileWriter.write("goto " + label + "\n");
        } catch (IOException e) {
            e.printStackTrace(System.out);
        }
    }

    public void writeIf(String label) {
        try {
            fileWriter.write("if-goto " + label + "\n");
        } catch (IOException e) {
            e.printStackTrace(System.out);
        }
    }

    public void writeCall(String name, int nArgs) {
        try {
            fileWriter.write("call " + name + " " + nArgs + "\n");
        } catch (IOException e) {
            e.printStackTrace(System.out);
        }
    }

    public void writeFunction(String name, int nLocals) {
        try {
            fileWriter.write("function " + name + " " + nLocals + "\n");
        } catch (IOException e) {
            e.printStackTrace(System.out);
        }
    }

    public void writeReturn() {
        try {
            fileWriter.write("return\n");
        } catch (IOException e) {
            e.printStackTrace(System.out);
        }
    }

    public void close() {
        try {
            fileWriter.close();
        } catch (IOException e) {
            e.printStackTrace(System.out);
        }
    }
}
