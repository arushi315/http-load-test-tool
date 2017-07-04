package com.http.load.tool.inputdata;

import org.apache.commons.lang.StringUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;

/**
 * Created by manish kumar.
 */
@Configuration
public class CsvDataFileReader {

    @Bean
    public List<Map<String, String>> createDevices() throws IOException {
        List<List<String>> csvData = readRecords();
        List<String> parametersNames = csvData.stream().findFirst().get();
        List<Map<String, String>> ss = csvData.stream()
                .skip(1)
                .map(attributes -> {
                    Map<String, String> parameters = new HashMap<>();
                    int index = 0;
                    for (String name : parametersNames) {
                        parameters.put(name.trim(), attributes.get(index));
                        index++;
                    }
                    return parameters;
                })
                .collect(Collectors.toList());
        return ss;
    }

    private List<List<String>> readRecords() throws IOException {
        List<List<String>> data;
        System.out.println("Reading \"input-data-file.csv\" system variable to determine if data file is provided.");
        String externalFilePath = System.getProperty("activeSyncDataFile");
        if (StringUtils.isNotEmpty(externalFilePath)) {
            data = readFromExternalDataFile(externalFilePath);
        } else {
            // Most cases we won't execute this flow. This is for people to be able to define a default behavior.
            System.out.println("No external data file provided, reading the default file \"input-data-file.csv\"");
            data = getDataFromDefaultFile("input-data-file.csv");
        }
        return data;
    }

    private List<List<String>> readFromExternalDataFile(final String externalFilePath) throws IOException {
        List<List<String>> data;
        System.out.println("External data file provided. Path = " + externalFilePath);
        data = Files.readAllLines(Paths.get(externalFilePath))
                .stream()
                .map(line -> asList(line.split(",")))
                .collect(Collectors.toList());
        return data;
    }

    private List<List<String>> getDataFromDefaultFile(final String fileName) throws IOException {
        System.out.println("No external data file provided, using the default one :: " + fileName);
        try (BufferedReader buffer = new BufferedReader(new InputStreamReader(
                this.getClass().getClassLoader().getResourceAsStream(fileName)))) {
            return buffer
                    .lines()
                    .map(line -> asList(line.split(",")))
                    .collect(Collectors.toList());
        }
    }
}