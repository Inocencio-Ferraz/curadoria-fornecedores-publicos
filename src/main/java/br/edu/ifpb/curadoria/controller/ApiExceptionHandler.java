package br.edu.ifpb.curadoria.controller;

import br.edu.ifpb.curadoria.service.*;
import jakarta.validation.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import java.util.*;

@RestControllerAdvice
public class ApiExceptionHandler {
    public record ErroApi(String erro, List<String> detalhes) { }
    @ExceptionHandler(RecursoNaoEncontradoException.class)
    ResponseEntity<ErroApi> naoEncontrado(RuntimeException e) { return ResponseEntity.status(404).body(new ErroApi(e.getMessage(), List.of())); }
    @ExceptionHandler(ConflitoException.class)
    ResponseEntity<ErroApi> conflito(RuntimeException e) { return ResponseEntity.status(409).body(new ErroApi(e.getMessage(), List.of())); }
    @ExceptionHandler(ReferenciaInvalidaException.class)
    ResponseEntity<ErroApi> referenciaInvalida(RuntimeException e) { return ResponseEntity.status(422).body(new ErroApi(e.getMessage(), List.of())); }
    @ExceptionHandler(ParametroInvalidoException.class)
    ResponseEntity<ErroApi> parametroInvalido(RuntimeException e) { return ResponseEntity.badRequest().body(new ErroApi("Requisição inválida", List.of(e.getMessage()))); }
    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ErroApi> corpoIlegivel(HttpMessageNotReadableException e) { return ResponseEntity.badRequest().body(new ErroApi("Requisição inválida", List.of("Corpo da requisição ausente ou malformado"))); }
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ResponseEntity<ErroApi> tipoInvalido(MethodArgumentTypeMismatchException e) { return ResponseEntity.badRequest().body(new ErroApi("Requisição inválida", List.of("Parâmetro inválido: " + e.getName()))); }
    @ExceptionHandler({MethodArgumentNotValidException.class, ConstraintViolationException.class, IllegalArgumentException.class})
    ResponseEntity<ErroApi> invalido(Exception e) {
        List<String> detalhes;
        if (e instanceof MethodArgumentNotValidException m) {
            detalhes = new ArrayList<>(m.getBindingResult().getFieldErrors().stream().map(x -> x.getField() + ": " + x.getDefaultMessage()).toList());
            detalhes.addAll(m.getBindingResult().getGlobalErrors().stream().map(x -> x.getDefaultMessage()).toList());
        } else if (e instanceof ConstraintViolationException) {
            detalhes = List.of(e.getMessage() == null ? "Entrada inválida" : e.getMessage());
        } else {
            // IllegalArgumentException genérico: não expõe a mensagem interna.
            detalhes = List.of("Entrada inválida");
        }
        return ResponseEntity.badRequest().body(new ErroApi("Requisição inválida", detalhes));
    }
    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ErroApi> integridade(DataIntegrityViolationException e) { return ResponseEntity.status(409).body(new ErroApi("Conflito com a integridade dos dados", List.of())); }
}
