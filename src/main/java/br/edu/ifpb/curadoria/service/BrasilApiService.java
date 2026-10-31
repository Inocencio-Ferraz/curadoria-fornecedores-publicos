package br.edu.ifpb.curadoria.service;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.util.Map;

@Service
public class BrasilApiService {

    private final RestTemplate restTemplate = new RestTemplate();


    public Map<String, Object> consultarCnpj(String cnpj) {
        String url = "https://brasilapi.com.br/api/cnpj/v1/" + cnpj;
        try {
            return restTemplate.getForObject(url, Map.class);
        } catch (Exception e) {
            throw new ParametroInvalidoException("Erro ao consultar o CNPJ na BrasilAPI: " + e.getMessage());
        }
    }


    public Map<String, Object> consultarCep(String cep) {
        String url = "https://brasilapi.com.br/api/cep/v1/" + cep;
        try {
            return restTemplate.getForObject(url, Map.class);
        } catch (Exception e) {
            throw new ParametroInvalidoException("Erro ao consultar o CEP na BrasilAPI: " + e.getMessage());
        }
    }
}
