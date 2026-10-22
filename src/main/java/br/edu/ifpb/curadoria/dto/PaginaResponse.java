package br.edu.ifpb.curadoria.dto;

import java.util.List;

public record PaginaResponse<T>(List<T> content, int number, int size, long totalElements, int totalPages) { }
