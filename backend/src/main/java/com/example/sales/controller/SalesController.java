package com.example.sales.controller;

import com.example.sales.model.dto.AnalyzeResponse;
import com.example.sales.model.entity.User;
import com.example.sales.service.SalesService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/sales")
@RequiredArgsConstructor
public class SalesController {

    private final SalesService salesService;

    @PostMapping("/analyze")
    public ResponseEntity<AnalyzeResponse> analyze(
            @RequestParam("files") List<MultipartFile> files,
            @AuthenticationPrincipal User user
    ) {
        AnalyzeResponse response = salesService.analyze(files, user);
        return ResponseEntity.ok(response);
    }
}
