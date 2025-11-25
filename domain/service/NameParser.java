package co.com.extractor.domain.service;

import co.com.extractor.domain.model.FullName;

public class NameParser {

    /**
     * Si el FullName ya tiene campos estructurados los devuelve tal cual.
     * Si no, intenta parsear `display` o `middleNames` en lastName, secondLastName y firstName.
     */
    public static FullName parse(FullName fn) {
        if (fn == null) return null;

        boolean hasStructured = (fn.getFirstName() != null && !fn.getFirstName().isBlank()) ||
                (fn.getLastName() != null && !fn.getLastName().isBlank()) ||
                (fn.getSecondLastName() != null && !fn.getSecondLastName().isBlank());
        if (hasStructured) return fn;

        String source = fn.getDisplay();
        if (source == null || source.isBlank()) {
            source = fn.getMiddleNames();
        }
        if (source == null) return fn;

        String normalized = source.trim().replaceAll("\\s+", " ");
        normalized = normalized.replaceAll("^(?:\\d{1,3}\\.|\\d{1,3}\\))\\s*", "").trim();
        normalized = normalized.replaceAll("\\s+\\d{1,3}\\.?\\s*([A-Za-zÀ-ÿ].*)?$", "").trim();
        normalized = normalized.replaceAll("[.,;:_-]+$", "").trim();

        String[] parts = normalized.split(" ");
        if (parts.length == 1) {
            fn.setFirstName(parts[0]);
            fn.setMiddleNames(null);
            fn.setDisplay(normalized);
            return fn;
        }
        if (parts.length == 2) {
            fn.setLastName(parts[0]);
            fn.setFirstName(parts[1]);
            fn.setMiddleNames(null);
            fn.setDisplay(normalized);
            return fn;
        }
        fn.setLastName(parts[0]);
        fn.setSecondLastName(parts[1]);
        String first = parts[2];
        StringBuilder middle = new StringBuilder();
        for (int i = 3; i < parts.length; i++) {
            if (middle.length() > 0) middle.append(' ');
            middle.append(parts[i]);
        }
        fn.setFirstName(first);
        fn.setMiddleNames(middle.length() > 0 ? middle.toString() : null);
        fn.setDisplay(normalized);
        return fn;
    }
}

