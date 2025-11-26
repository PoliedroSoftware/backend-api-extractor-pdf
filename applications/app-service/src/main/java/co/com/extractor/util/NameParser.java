package co.com.extractor.util;

import co.com.extractor.domain.model.FullName;

public class NameParser {

    /**
     * Si el FullName ya tiene campos estructurados los devuelve tal cual.
     * Si no, intenta parsear `display` o `middleNames` en lastName, secondLastName y firstName.
     */
    public static FullName parse(FullName fn) {
        if (fn == null) return null;

        // Si ya tiene estructura, devolver
        boolean hasStructured = (fn.getFirstName() != null && !fn.getFirstName().trim().isEmpty()) ||
                (fn.getLastName() != null && !fn.getLastName().trim().isEmpty()) ||
                (fn.getSecondLastName() != null && !fn.getSecondLastName().trim().isEmpty());
        if (hasStructured) return fn;

        String source = fn.getDisplay();
        if (source == null || source.trim().isEmpty()) {
            source = fn.getMiddleNames();
        }
        if (source == null) return fn;

        String normalized = source.trim().replaceAll("\\s+", " ");
        // eliminar prefijos numéricos y sufijos ruidosos
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
        // >=3 -> apellido1 apellido2 nombres...
        fn.setLastName(parts[0]);
        fn.setSecondLastName(parts[1]);
        // Primer nombre = primer token de los nombres; middleNames = resto (si existe)
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
