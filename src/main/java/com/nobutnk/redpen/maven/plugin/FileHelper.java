package com.nobutnk.redpen.maven.plugin;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.List;

public class FileHelper {

    public void search(String path, String extension, List<String> fileList) {
        File dir = new File(path);
        File files[] = dir.listFiles();
        for (int i = 0; i < files.length; i++) {
            String fileName = files[i].getName();
            if (files[i].isDirectory()) { // ディレクトリなら再帰を行う
                search(path + "/" + fileName, extension, fileList);
            } else {
                if (fileName.endsWith(extension)) { // file_nameの最後尾(拡張子)が指定のものならば出力
                    fileList.add(path + "/" + fileName);
                }
            }
        }
    }
    
    public void output(String result, File directory, String fileName) throws IOException {
        PrintWriter writer = null;
        try {
            // ディレクトリが存在するか確認、存在しない場合は作成
            if (!directory.exists()) {
                directory.mkdir();
            }

            // ファイルが存在するかの確認、存在しない場合は作成
            File resultFile = new File(directory.getAbsolutePath(), fileName);
            if (!resultFile.exists()) {
                resultFile.createNewFile();
            }

            // ファイルに追記書き込み
            writer = new PrintWriter
                    (new BufferedWriter(new OutputStreamWriter
                    (new FileOutputStream(resultFile),"utf-8")));
            writer.write(result);
        } finally {
            if (writer != null) {
                writer.close();
            }
        }
    }

}
