
package br.edu.ifpb.curadoria.service;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
public class ComprasGovService {

    // API atual de Dados Abertos do Compras.gov.br
    static final String URL_FORNECEDOR =
            "https://dadosabertos.compras.gov.br/modulo-fornecedor/1_consultarFornecedor"
                    + "?pagina=1&tamanhoPagina=10&cnpj=";

    private final RestTemplate restTemplate = new RestTemplate();


    @SuppressWarnings("unchecked")
    public Map<String, Object> consultarFornecedorPorCnpj(String cnpj) {

        try {
            String url = URL_FORNECEDOR + cnpj + "&ativo=true";

            Map<String, Object> corpo =
                    restTemplate.getForObject(url, Map.class);

            if (corpo != null && corpo.get("resultado") instanceof List<?> lista
                    && !lista.isEmpty()
                    && lista.get(0) instanceof Map<?, ?> primeiro) {

                return (Map<String, Object>) primeiro;
            }

            return Collections.emptyMap();

        } catch (Exception e) {
            throw new ParametroInvalidoException(
                    "Erro ao comunicar com a API do Compras.gov: "
                            + e.getMessage()
            );
        }
    }
}
