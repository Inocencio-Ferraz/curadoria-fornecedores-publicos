package br.edu.ifpb.curadoria.validation;

import br.edu.ifpb.curadoria.transform.DadosNormalizer;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class CnpjValidator implements ConstraintValidator<Cnpj, String> {
    @Override public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) return true; // @NotBlank trata obrigatoriedade
        String cnpj = DadosNormalizer.documento(value);
        if (!cnpj.matches("[A-Z0-9]{12}[0-9]{2}") || cnpj.chars().distinct().count() == 1) return false;
        return digit(cnpj.substring(0, 12), new int[]{5,4,3,2,9,8,7,6,5,4,3,2}) == cnpj.charAt(12) - '0'
                && digit(cnpj.substring(0, 13), new int[]{6,5,4,3,2,9,8,7,6,5,4,3,2}) == cnpj.charAt(13) - '0';
    }
    private int digit(String value, int[] weights) {
        int sum = 0;
        for (int i = 0; i < weights.length; i++) sum += (value.charAt(i) - '0') * weights[i];
        int remainder = sum % 11;
        return remainder < 2 ? 0 : 11 - remainder;
    }
}
