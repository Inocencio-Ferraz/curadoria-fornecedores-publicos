package br.edu.ifpb.curadoria.controller;

import br.edu.ifpb.curadoria.service.*;
import jakarta.validation.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestControllerAdvice
public class ApiExceptionHandler {
    public record ErroApi(String erro, List<String> detalhes) { }
    @ExceptionHandler(RecursoNaoEncontradoException.class)
    ResponseEntity<ErroApi> naoEncontrado(RuntimeException e) { return ResponseEntity.status(404).body(new ErroApi(e.getMessage(), List.of())); }
    @ExceptionHandler(ConflitoException.class)
    ResponseEntity<ErroApi> conflito(RuntimeException e) { return ResponseEntity.status(409).body(new ErroApi(e.getMessage(), List.of())); }
    @ExceptionHandler({MethodArgumentNotValidException.class, ConstraintViolationException.class, IllegalArgumentException.class})
    ResponseEntity<ErroApi> invalido(Exception e) {
        List<String> detalhes = e instanceof MethodArgumentNotValidException m
                ? m.getBindingResult().getFieldErrors().stream().map(x -> x.getField() + ": " + x.getDefaultMessage()).toList()
                : List.of(e.getMessage() == null ? "Entrada inválida" : e.getMessage());
        return ResponseEntity.badRequest().body(new ErroApi("Requisição inválida", detalhes));
    }
    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ErroApi> integridade(DataIntegrityViolationException e) { return ResponseEntity.status(409).body(new ErroApi("Conflito com a integridade dos dados", List.of())); }
}
