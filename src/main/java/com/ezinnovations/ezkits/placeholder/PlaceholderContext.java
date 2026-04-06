package com.ezinnovations.ezkits.placeholder;

import java.util.HashMap;
import java.util.Map;

public class PlaceholderContext {

    private final Map<String, String> placeholders = new HashMap<>();

    public PlaceholderContext with(String placeholder, String value) {
        placeholders.put(placeholder, value);
        return this;
    }

    public String apply(String input) {
        String result = input;
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            result = result.replace(entry.getKey(), entry.getValue());
        }
        return result;
    }
}
