package br.edu.ifpb.curadoria.transform;

import java.text.Normalizer;
import java.util.Locale;

public final class DadosNormalizer {
    private DadosNormalizer() {}
    public static String texto(String value) {
        if (value == null) return null;
        String normalized = value.trim().replaceAll("\\s+", " ");
        return normalized.isEmpty() ? null : normalized;
    }
    public static String documento(String value) {
        if (value == null) return null;
        return value.replaceAll("[^A-Za-z0-9]", "").toUpperCase(Locale.ROOT);
    }
    public static String uf(String value) {
        String normalized = texto(value);
        return normalized == null ? null : normalized.toUpperCase(Locale.ROOT);
    }

    public static String municipio(String value) {
        String normalized = texto(value);
        if (normalized == null) return null;
        String semAcento = Normalizer.normalize(normalized, Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
        return semAcento.toUpperCase(Locale.ROOT);
    }

    public static String cep(String value) {
        if (value == null) return null;
        String digitos = value.replaceAll("\\D", "");
        return digitos.isEmpty() ? null : digitos;
    }
}
