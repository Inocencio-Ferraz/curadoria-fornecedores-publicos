package br.edu.ifpb.curadoria.validation;

import br.edu.ifpb.curadoria.transform.DadosNormalizer;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class CnpjValidator implements ConstraintValidator<Cnpj, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) {
            return true;
        }

        String cnpj = DadosNormalizer.documento(value);

        if (!cnpj.matches("[A-Z0-9]{12}[0-9]{2}")
                || cnpj.chars().distinct().count() == 1) {
            return false;
        }

        return calcularDigito(cnpj.substring(0, 12),
                new int[]{5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2}) == cnpj.charAt(12) - '0'
                && calcularDigito(cnpj.substring(0, 13),
                new int[]{6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2}) == cnpj.charAt(13) - '0';
    }

    private int calcularDigito(String valor, int[] pesos) {
        int soma = 0;

        for (int i = 0; i < pesos.length; i++) {
            char caractere = valor.charAt(i);
            int numero = Character.isDigit(caractere)
                    ? caractere - '0'
                    : caractere - 'A' + 17;

            soma += numero * pesos[i];
        }

        int resto = soma % 11;
        return resto < 2 ? 0 : 11 - resto;
    }
}