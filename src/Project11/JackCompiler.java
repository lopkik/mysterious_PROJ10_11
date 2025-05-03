package Project11;

import java.io.File;
import java.util.ArrayList;

public class JackCompiler {
    public static void main (String[] args) {
        File jackFileDir = new File(args[0]);

        ArrayList<File> files = new ArrayList<>();
        if (jackFileDir.isFile() && args[0].endsWith(".jack")) {
            files.add(jackFileDir);
        } else if (jackFileDir.isDirectory()) {
            File[] jackDirFiles = jackFileDir.listFiles();
            if (jackDirFiles != null) {
                for (File file : jackDirFiles) {
                    if (file.getName().endsWith(".jack")) {
                        files.add(file);
                    }
                }
            }
        }


        for (File file : files) {
            String originalFileName = file.getName();
            String fileOutName = originalFileName.substring(0, originalFileName.length() - 5) + ".vm";
            File fileOut = new File(fileOutName);

            System.out.print("Writing to ");
            System.out.println(fileOutName);
            CompilationEngine compilationEngine = new CompilationEngine(file, fileOut);
            compilationEngine.compileClass();
        }
    }
}
