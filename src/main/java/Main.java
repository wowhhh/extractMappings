import sun.rmi.runtime.Log;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

public class Main {

    private static ArrayList<String> fileName  = new ArrayList<>();
    private static  ExtractMappings extractor = new ExtractMappings();
    public static void main(String[] args) throws IOException {
//        datainit(23);
        for(int v=23;v<=29;v++) {//API 23->29
            datainit(v);
            fileName.clear();
            getAllFileName(config.sourcePath, fileName);

            for (int i = 0; i < fileName.size(); i++) {
                if (fileName.get(i).endsWith(".java")) {
                    File file = new File(fileName.get(i));
                    extractor.inspectJavaFile(new File(fileName.get(i)));
                }
            }
        }
//        annotations.inspectJavaFile(new File("res\\test.java"));
//                annotations.inspectJavaFile(new File("D:\\Android\\AndroidPlatform\\source\\sources-25_r01\\src\\android\\location\\LocationManager.java"));
//        annotations.inspectJavaFile(new File("C:/Users/wyb/AppData/Local/Android/Sdk/sources/android-25/android/telephony/TelephonyManager.java"));
        return;
    }

    private static void datainit(int version) {

        config.sourceVersion = "sources-"+version+"_r01";
        config.sourcePath ="D:\\Android\\AndroidPlatform\\source\\"+ config.sourceVersion;
        config.pathToJar ="C:\\Users\\wyb\\AppData\\Local\\Android\\Sdk\\platforms\\android-"+version+"\\android.jar";
    }

    public static void getAllFileName(String path, ArrayList<String> listFileName){
        File file = new File(path);
        File [] files = file.listFiles();
        String [] names = file.list();

        for(File a:files){
            if(a.isDirectory()){
                getAllFileName(a.getAbsolutePath()+"\\",listFileName);
            }
            else if(a.isFile()) //为文件
            {
                listFileName.add(a.toString());
            }
        }
    }
}
