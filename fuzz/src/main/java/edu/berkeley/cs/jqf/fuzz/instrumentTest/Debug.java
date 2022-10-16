package edu.berkeley.cs.jqf.fuzz.instrumentTest;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;

public class Debug {

    public static void prepare(String path) {
        File file = new File(path);
        if (!file.exists()) {
            file.getParentFile().mkdirs();
        }
    }

    public static void write(final String path,final byte[] bytes) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try{
                    prepare(path);
                    Files.write(Paths.get(path), bytes);
                } catch (Throwable t){
                    t.printStackTrace();
                }
            }
        }).start();
    }
}
