package utils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class WRFile {
    private  static  FileWriter fwriter ;
    private  static  boolean rewrite = false;

    public void saveContent(String content,String filePath)
    {
        try {
            File file = new File(filePath);

            //目录不存在 则创建
            if (!file.getParentFile().exists()) {
                boolean mkdir = file.getParentFile().mkdirs();
                if (!mkdir) {
                    throw new RuntimeException("创建目标文件所在目录失败！");
                }
            }
            if(rewrite) // 表示覆盖之前的内容
            {
                fwriter = new FileWriter(filePath);
                rewrite = true;

            }
            else
            {
                fwriter = new FileWriter(filePath,true);
            }
            fwriter.write(content+"\r\n");//换行
        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            try {
                fwriter.flush();
                fwriter.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

}
