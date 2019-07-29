package com.nobutnk.redpen.maven.plugin;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import cc.redpen.RedPen;
import cc.redpen.RedPenException;
import cc.redpen.formatter.Formatter;
import cc.redpen.model.Document;
import cc.redpen.parser.DocumentParser;
import cc.redpen.util.FormatterUtils;
import cc.redpen.validator.ValidationError;

/**
 * Sample
 */
@Mojo(name = "redpen", threadSafe = true, defaultPhase = LifecyclePhase.TEST)
public class RedpenMojo extends AbstractMojo {

    private static final String DEFAULT_CONFIG_NAME = "redpen-conf";

    /**
     * Location of the file.
     */
    @Parameter(property = "project.build.directory", required = true)
    private File outputDirectory;

    @Parameter(property = "redpen.input.format", required = true, defaultValue = "plain")
    private String inputFormat;

    @Parameter(property = "redpen.config.name", required = true)
    private String configFileName;

    @Parameter(property = "redpen.config.language", required = false, defaultValue = "en")
    private String language;

    @Parameter(property = "redpen.config.limit", required = true, defaultValue = "1")
    private int limit;

    @Parameter(property = "redpen.config.resultFormat", required = true, defaultValue = "plain")
    private String resultFormat;

    @Parameter(property = "redpen.config.inputFile", required = true)
    private String inputFile;
    
    @Parameter(property = "redpen.config.resultFile", required = true, defaultValue = "redpen-result.txt")
    private String resultFileName;

    @Override
    public void execute() throws MojoExecutionException {
        getLog().info("redpen plugin start!");
        getLog().info("project.build.directory is " + outputDirectory.getAbsolutePath());

        String inputSentence = null;

        File configFile = resolveConfigLocation(configFileName);
        if (configFile == null) {
            File pwd = new File(".");
            getLog().error("Configuration file is not found." + pwd.getAbsolutePath());
            return;
        }

        // set language
        if (language.equals("ja")) {
            Locale.setDefault(new Locale("ja", "JA"));
        } else {
            Locale.setDefault(new Locale("en", "EN"));
        }

        RedPen redPen;
        try {
            redPen = new RedPen(configFile);
        } catch (RedPenException e) {
            getLog().error("Failed to parse input files: " + e);
            return;
        }
        
        List<String> inputFileList = new ArrayList<>();
        search(inputFile, getExtension(inputFormat), inputFileList);
        getLog().info("inputFiles = " + inputFileList);
        String[] inputFileNames = inputFileList.toArray(new String[inputFileList.size()]);

        List<Document> documents;
        try {
            documents = getDocuments(inputFormat, inputSentence, inputFileNames, redPen);
        } catch (RedPenException e) {
            getLog().error(e);
            return;
        }
        Map<Document, List<ValidationError>> documentListMap = redPen.validate(documents);

        Formatter formatter = FormatterUtils.getFormatterByName(resultFormat);
        if (formatter == null) {
            getLog().error("Unsupported format: " + resultFormat + " - please use xml, plain, plain2, json or json2");
            return;
        }
        String result = formatter.format(documentListMap);
        System.out.println(result);
        try {
            output(result, outputDirectory, resultFileName);
        } catch (IOException e) {
            getLog().error(e);
            return;
        }

        long errorCount = documentListMap.values().stream().mapToLong(List::size).sum();

        if (errorCount > limit) {
            getLog().error(String.format("The number of errors \"%d\" is larger than specified (limit is \"%d\").",
                    errorCount, limit));
        }
    }

    private static List<Document> getDocuments(
            String inputFormat, String inputSentence,
            String[] inputFileNames,
            RedPen redPen) throws RedPenException {
        List<Document> documents = new ArrayList<>();
        DocumentParser parser = DocumentParser.of(inputFormat);
        if (inputSentence == null) {
            documents.addAll(redPen.parse(parser, extractInputFiles(inputFileNames)));
        } else {
            documents.add(redPen.parse(parser, inputSentence));
        }
        return documents;
    }

    private static File[] extractInputFiles(String[] inputFileNames) {
        File[] inputFiles = new File[inputFileNames.length];
        for (int i = 0; i < inputFileNames.length; i++) {
            inputFiles[i] = new File(inputFileNames[i]);
        }
        return inputFiles;
    }

    static File resolveConfigLocation(String configFileName) {
        List<String> pathCandidates = new ArrayList<>();
        if (configFileName != null) {
            pathCandidates.add(configFileName);
        }
        pathCandidates.add(DEFAULT_CONFIG_NAME + ".xml");
        pathCandidates.add(DEFAULT_CONFIG_NAME + "-" + Locale.getDefault().getLanguage() + ".xml");
        String redpenHome = System.getenv("REDPEN_HOME");
        if (redpenHome != null) {
            pathCandidates.add(redpenHome + File.separator + DEFAULT_CONFIG_NAME + ".xml");
            pathCandidates.add(redpenHome + File.separator + DEFAULT_CONFIG_NAME + "-"
                    + Locale.getDefault().getLanguage() + ".xml");
            pathCandidates.add(redpenHome + File.separator + "conf" + File.separator + DEFAULT_CONFIG_NAME + ".xml");
            pathCandidates.add(redpenHome + File.separator + "conf" + File.separator + DEFAULT_CONFIG_NAME + "-"
                    + Locale.getDefault().getLanguage() + ".xml");
        }
        File resolved = resolve(pathCandidates);
        if (resolved != null && resolved.exists() && resolved.isFile()) {
            // getLog().info(String.format("Configuration file: %s",
            // resolved.getAbsolutePath()));
        } else {
            resolved = null;
        }
        return resolved;
    }

    static File resolve(List<String> pathCandidates) {
        File resolved;
        for (String pathCandidate : pathCandidates) {
            resolved = new File(pathCandidate);
            if (resolved.exists() && resolved.isFile()) {
                return resolved;
            }
        }
        return null;
    }
    
    static String getExtension(String inputFormat) {
        if ("asciidoc".equals(inputFormat)) {
            return ".adoc";
        } else if ("markdown".equals(inputFormat)) {
            return ".md";
        } else if ("plain".equals(inputFormat)) {
            return ".txt";
        } else if ("properties".equals(inputFormat)) {
            return ".properties";
        }
        
        return ".txt";
    }

    static void search(String path, String extension, List<String> fileList) {
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
    
    static void output(String result, File directory, String fileName) throws IOException {
        PrintWriter writer = null;
        try {
            // ディレクトリが存在するか確認、存在しない場合は作成
            if (!directory.exists()) {
                System.out.println("ディレクトリなし");
                directory.mkdir();
            } else {
                System.out.println("ディレクトリあり");
            }

            // ファイルが存在するかの確認、存在しない場合は作成
            File resultFile = new File(directory.getAbsolutePath(), fileName);
            if (!resultFile.exists()) {
                System.out.println("ファイルなし");
                resultFile.createNewFile();
            } else {
                System.out.println("ファイルあり");
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
    
    void setInputFormat(String inputFormat) {
        this.inputFormat = inputFormat;
    }
    void setConfigFileName(String configFileName) {
        this.configFileName = configFileName;
    }
    void setLanguage(String language) {
        this.language = language;
    }
    void setLimit(int limit) {
        this.limit = limit;
    }
    void setResultFormat(String resultFormat) {
        this.resultFormat = resultFormat;
    }
    void setInputFile(String inputFile) {
        this.inputFile = inputFile;
    }
    
    void setOutputDirectory(File outputDirectory) {
        this.outputDirectory = outputDirectory;
    }
    
    void setResultFileName(String resultFileName) {
        this.resultFileName = resultFileName;
    }
}
