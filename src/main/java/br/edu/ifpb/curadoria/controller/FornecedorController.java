package br.edu.ifpb.curadoria.controller;

import br.edu.ifpb.curadoria.dto.*;
import br.edu.ifpb.curadoria.service.FornecedorService;
import br.edu.ifpb.curadoria.service.ParametroInvalidoException;
import br.edu.ifpb.curadoria.validation.GrupoAtualizacao;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.*;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/fornecedores")
public class FornecedorController {

    static final int TAMANHO_MAXIMO = 100;
    private static final Set<String> CAMPOS_ORDENAVEIS =
            Set.of("id", "razaoSocial", "ativo", "habilitadoLicitar", "cnpj", "cpf");

    private final FornecedorService service;

    public FornecedorController(FornecedorService service) {
        this.service = service;
    }


    @GetMapping
    public PaginaResponse<FornecedorResponse> listar(
            @RequestParam(required = false) String busca,
            @RequestParam(required = false) Boolean ativo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) List<String> sort) {
        Page<FornecedorResponse> resultado =
                service.listar(busca, ativo, paginacao(page, size, sort));

        return new PaginaResponse<>(
                resultado.getContent(),
                resultado.getNumber(),
                resultado.getSize(),
                resultado.getTotalElements(),
                resultado.getTotalPages());
    }

    @GetMapping("/{id}")
    public FornecedorResponse buscar(@PathVariable Long id) {
        return service.buscar(id);
    }

    @GetMapping("/cnpj/{cnpj}")
    public FornecedorResponse buscarCnpj(@PathVariable String cnpj) {
        return service.buscarPorCnpj(cnpj);
    }

    @GetMapping("/cpf/{cpf}")
    public FornecedorResponse buscarCpf(@PathVariable String cpf) {
        return service.buscarPorCpf(cpf);
    }

    @PostMapping
    public ResponseEntity<FornecedorResponse> criar(
            @Validated @RequestBody FornecedorRequest request) {
        FornecedorResponse criado = service.criar(request);

        return ResponseEntity
                .created(URI.create("/api/fornecedores/" + criado.id()))
                .body(criado);
    }

    @PutMapping("/{id}")
    public FornecedorResponse atualizar(
            @PathVariable Long id,
            @Validated(GrupoAtualizacao.class) @RequestBody FornecedorRequest request) {
        return service.atualizar(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable Long id) {
        service.excluir(id);
        return ResponseEntity.noContent().build();
    }



    static Pageable paginacao(int page, int size, List<String> sort) {
        if (page < 0) {
            throw new ParametroInvalidoException("page deve ser maior ou igual a zero");
        }
        if (size < 1) {
            throw new ParametroInvalidoException("size deve ser maior que zero");
        }

        return PageRequest.of(page, Math.min(size, TAMANHO_MAXIMO), ordenacao(sort));
    }


    private static Sort ordenacao(List<String> sort) {
        List<Sort.Order> ordens = new ArrayList<>();

        if (sort != null) {
            for (String token : sort) {
                String valor = token == null ? "" : token.trim();

                if (valor.equalsIgnoreCase("asc") || valor.equalsIgnoreCase("desc")) {
                    if (ordens.isEmpty()) {
                        throw new ParametroInvalidoException("sort: direção sem campo");
                    }
                    Sort.Order anterior = ordens.remove(ordens.size() - 1);
                    ordens.add(new Sort.Order(
                            Sort.Direction.fromString(valor), anterior.getProperty()));
                } else if (!valor.isEmpty()) {
                    if (!CAMPOS_ORDENAVEIS.contains(valor)) {
                        throw new ParametroInvalidoException(
                                "sort: campo não permitido (use um de " + CAMPOS_ORDENAVEIS + ")");
                    }
                    ordens.add(Sort.Order.asc(valor));
                }
            }
        }

        if (ordens.isEmpty()) {
            ordens.add(Sort.Order.asc("razaoSocial"));
        }

        if (ordens.stream().noneMatch(o -> o.getProperty().equals("id"))) {
            ordens.add(Sort.Order.asc("id"));
        }

        return Sort.by(ordens);
    }
}
