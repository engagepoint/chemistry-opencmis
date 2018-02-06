package org.apache.chemistry.opencmis.workbench;

import org.apache.commons.io.IOUtils;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class PredefinedParameters {
    private static final int PARAMETER_ENTRY_COUNT_INDEX = 2;
    private static final int EMPTY_VALUE_PARAMETER_INDEX = 1;
    private static final String DELIMETER = "=";

    private final Map<String, String> parameters;

    public PredefinedParameters() {
        this.parameters = new LinkedHashMap<String, String>();
    }

    public PredefinedParameters(Map<String, String> parameters) {
        this.parameters = parameters;
    }

    public String put(String key, String value) {
        return parameters.put(key, value);
    }

    public String get(String key) {
        return parameters.get(key);
    }

    public Set<Map.Entry<String, String>> propertiesSet() {
        return parameters.entrySet();
    }

    public void store(String path) throws FileNotFoundException {
        PrintWriter printWriter = null;
        try {
            printWriter = new PrintWriter(path);
            for (Map.Entry<String, String> stringStringEntry : parameters.entrySet()) {
                printWriter.println(stringStringEntry);
            }
            printWriter.flush();
        }  finally {
            IOUtils.closeQuietly(printWriter);
        }
    }

    public void load(String path) throws IOException {
        List<String> lines = Files.readAllLines(Paths.get(path), Charset.defaultCharset());
        for (String line : lines) {
            if (line.contains(DELIMETER)) {
                String[] strings = line.split(DELIMETER);
                if (strings.length > 0 && strings.length <= PARAMETER_ENTRY_COUNT_INDEX) {
                    parameters.put(strings[0],
                            strings.length == EMPTY_VALUE_PARAMETER_INDEX ? "" : strings[1]);
                }
            }
        }
    }
}
