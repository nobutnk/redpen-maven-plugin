package com.nobutnk.redpen.maven.plugin;

import java.io.File;
import java.io.IOException;
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
import cc.redpen.util.FormatterUtils;
import cc.redpen.validator.ValidationError;

/**
 * Sample
 */
@Mojo(name = "redpen", threadSafe = true, defaultPhase = LifecyclePhase.TEST)
public class RedpenMojo extends AbstractMojo {
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
    
    private RedpenHelper redpenHelper = new RedpenHelper();
    
    private FileHelper fileHelper = new FileHelper();

    @Override
    public void execute() throws MojoExecutionException {
        getLog().info("redpen plugin start!");
        getLog().info("project.build.directory is " + outputDirectory.getAbsolutePath());

        String inputSentence = null;

        File configFile = redpenHelper.resolveConfigLocation(configFileName);
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
        fileHelper.search(inputFile, redpenHelper.getExtension(inputFormat), inputFileList);
        getLog().info("inputFiles = " + inputFileList);
        String[] inputFileNames = inputFileList.toArray(new String[inputFileList.size()]);

        List<Document> documents;
        try {
            documents = redpenHelper.getDocuments(inputFormat, inputSentence, inputFileNames, redPen);
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
            fileHelper.output(result, outputDirectory, resultFileName);
        } catch (IOException e) {
            getLog().error(e);
            return;
        }

        long errorCount = documentListMap.values().stream().mapToLong(List::size).sum();

        if (errorCount > limit) {
            String message = String.format("The number of errors \"%d\" is larger than specified (limit is \"%d\").",
                    errorCount, limit);
            getLog().error(message);
            throw new MojoExecutionException(message);
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
