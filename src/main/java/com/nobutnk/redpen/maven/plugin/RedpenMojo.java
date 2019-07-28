package com.nobutnk.redpen.maven.plugin;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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

    @Override
    public void execute() throws MojoExecutionException {
        getLog().info("redpen plugin start!");
        getLog().debug("project.build.directory is " + outputDirectory.getAbsolutePath());

        String inputSentence = null;

        File configFile = resolveConfigLocation(configFileName);
        if (configFile == null) {
            getLog().error("Configuration file is not found.");
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
        search(inputFile, "adoc", inputFileList);
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
}
