package com.Luxurycars.carstore.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PageResponseDTO<T> {

    private List<T> content;       // the actual items on this page
    private int page;              // current page index (0-based)
    private int size;              // page size
    private long totalElements;    // total items in DB
    private int totalPages;        // total number of pages
    private boolean first;         // is this the first page?
    private boolean last;          // is this the last page?
    private boolean empty;         // is content empty?

    // ─── FACTORY METHOD: build from Spring's Page<> ───
    public static <T> PageResponseDTO<T> from(org.springframework.data.domain.Page<T> page) {
        return PageResponseDTO.<T>builder()
                .content(page.getContent())
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .first(page.isFirst())
                .last(page.isLast())
                .empty(page.isEmpty())
                .build();
    }
}