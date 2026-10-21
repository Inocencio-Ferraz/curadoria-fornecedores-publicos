package br.edu.ifpb.curadoria.validation;

import br.edu.ifpb.curadoria.transform.DadosNormalizer;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class CpfValidator implements ConstraintValidator<Cpf, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) {
            return true;
        }

        String cpf = DadosNormalizer.documento(value);

        if (!cpf.matches("[0-9]{11}") || cpf.chars().distinct().count() == 1) {
            return false;
        }

        int primeiro = calcularDigito(cpf.substring(0, 9), 10);
        int segundo = calcularDigito(cpf.substring(0, 9) + primeiro, 11);

        return primeiro == cpf.charAt(9) - '0'
                && segundo == cpf.charAt(10) - '0';
    }

    private int calcularDigito(String valor, int pesoInicial) {
        int soma = 0;

        for (int i = 0; i < valor.length(); i++) {
            soma += (valor.charAt(i) - '0') * (pesoInicial - i);
        }

        int resto = (soma * 10) % 11;
        return resto == 10 ? 0 : resto;
    }
}