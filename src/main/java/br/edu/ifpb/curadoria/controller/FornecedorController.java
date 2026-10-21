package br.edu.ifpb.curadoria.controller;

import br.edu.ifpb.curadoria.dto.*;
import br.edu.ifpb.curadoria.service.FornecedorService;
import jakarta.validation.Valid;
import org.springframework.data.domain.*;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.net.URI;

@RestController
@RequestMapping("/api/fornecedores")
public class FornecedorController {

    private final FornecedorService service;

    public FornecedorController(FornecedorService service) {
        this.service = service;
    }

    @GetMapping
    public Page<FornecedorResponse> listar(
            @RequestParam(required = false) String busca,
            @RequestParam(required = false) Boolean ativo,
            @PageableDefault(size = 20, sort = "razaoSocial")
            Pageable pageable) {
        Pageable seguro = PageRequest.of(
                pageable.getPageNumber(),
                Math.min(pageable.getPageSize(), 100),
                pageable.getSort());

        return service.listar(busca, ativo, seguro);
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
            @Valid @RequestBody FornecedorRequest request) {
        FornecedorResponse criado = service.criar(request);

        return ResponseEntity
                .created(URI.create("/api/fornecedores/" + criado.id()))
                .body(criado);
    }

    @PutMapping("/{id}")
    public FornecedorResponse atualizar(
            @PathVariable Long id,
            @Valid @RequestBody FornecedorRequest request) {
        return service.atualizar(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable Long id) {
        service.excluir(id);
        return ResponseEntity.noContent().build();
    }
}