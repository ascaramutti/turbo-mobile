package com.turbo.mobile.util;

import java.util.HashMap;
import java.util.Map;

public class FormErrors {
    private final Map<String, String> errors = new HashMap<>();

    public void add(String field, String message) {
        errors.put(field, message);
    }

    public boolean hasErrors() {
        return !errors.isEmpty();
    }

    public boolean has(String field) {
        return errors.containsKey(field);
    }

    public String get(String field) {
        return errors.get(field);
    }
}
