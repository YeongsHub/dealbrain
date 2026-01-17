package com.example.sales.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatQueryRequest {

    @NotBlank(message = "Query cannot be blank")
    @Size(max = 2000, message = "Query must not exceed 2000 characters")
    private String query;

    private String dealId;

    private Integer topK;
}
