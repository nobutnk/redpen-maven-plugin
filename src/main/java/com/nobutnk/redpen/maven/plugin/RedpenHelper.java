package com.nobutnk.redpen.maven.plugin;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import cc.redpen.RedPen;
import cc.redpen.RedPenException;
import cc.redpen.model.Document;
import cc.redpen.parser.DocumentParser;

public class RedpenHelper {

    private static final String DEFAULT_CONFIG_NAME = "redpen-conf";
    
    private static final Map<String, String> extensionsMap = new HashMap<>();
    
    static {
        extensionsMap.put("asciidoc", ".adoc");
        extensionsMap.put("markdown", ".md");
        extensionsMap.put("plain", ".txt");
        extensionsMap.put("properties", ".properties");
        extensionsMap.put("latex", ".tex");
        extensionsMap.put("rest", ".rst");
        extensionsMap.put("review", ".re");
        extensionsMap.put("wiki", ".textile");
    }
    
    public List<Document> getDocuments(
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

    private File[] extractInputFiles(String[] inputFileNames) {
        File[] inputFiles = new File[inputFileNames.length];
        for (int i = 0; i < inputFileNames.length; i++) {
            inputFiles[i] = new File(inputFileNames[i]);
        }
        return inputFiles;
    }
    
    public File resolveConfigLocation(String configFileName) {
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
        if (resolved == null ||
                !resolved.exists() ||
                !resolved.isFile()) {
            resolved = null;
        }
        return resolved;
    }

    File resolve(List<String> pathCandidates) {
        File resolved;
        for (String pathCandidate : pathCandidates) {
            resolved = new File(pathCandidate);
            if (resolved.exists() && resolved.isFile()) {
                return resolved;
            }
        }
        return null;
    }
    
    public String getExtension(String inputFormat) {
        String extention = extensionsMap.get(inputFormat);
        if (extention != null) {
            return extention;
        } else {
            return ".txt";
        }
    }
}
