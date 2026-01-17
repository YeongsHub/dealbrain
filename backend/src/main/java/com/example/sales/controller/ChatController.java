package com.example.sales.controller;

import com.example.sales.model.dto.ChatQueryRequest;
import com.example.sales.model.dto.ChatQueryResponse;
import com.example.sales.model.entity.User;
import com.example.sales.service.RagService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/chat")
@RequiredArgsConstructor
public class ChatController {

    private final RagService ragService;

    @PostMapping("/query")
    public ResponseEntity<ChatQueryResponse> query(
            @Valid @RequestBody ChatQueryRequest request,
            @AuthenticationPrincipal User user
    ) {
        ChatQueryResponse response = ragService.query(request, user);
        return ResponseEntity.ok(response);
    }
}
