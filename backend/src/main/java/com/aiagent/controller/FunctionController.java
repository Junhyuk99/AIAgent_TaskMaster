package com.aiagent.controller;

import com.aiagent.dto.function.FunctionRequest;
import com.aiagent.dto.function.FunctionResponse;
import com.aiagent.dto.function.FunctionTestRequest;
import com.aiagent.dto.function.FunctionTestResponse;
import com.aiagent.security.UserPrincipal;
import com.aiagent.service.FunctionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/functions")
@RequiredArgsConstructor
public class FunctionController {

    private final FunctionService functionService;

    @GetMapping
    public ResponseEntity<List<FunctionResponse>> getAllFunctions(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(functionService.getAllFunctions(principal.getId()));
    }

    @GetMapping("/paged")
    public ResponseEntity<Page<FunctionResponse>> getFunctionsPaged(
            @AuthenticationPrincipal UserPrincipal principal,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(functionService.getFunctionsPaged(principal.getId(), pageable));
    }

    @GetMapping("/active")
    public ResponseEntity<List<FunctionResponse>> getActiveFunctions(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(functionService.getActiveFunctions(principal.getId()));
    }

    @GetMapping("/search")
    public ResponseEntity<List<FunctionResponse>> searchFunctions(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam String query) {
        return ResponseEntity.ok(functionService.searchFunctions(principal.getId(), query));
    }

    @GetMapping("/{id}")
    public ResponseEntity<FunctionResponse> getFunction(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(functionService.getFunction(id, principal.getId()));
    }

    @PostMapping
    public ResponseEntity<FunctionResponse> createFunction(
            @Valid @RequestBody FunctionRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        FunctionResponse response = functionService.createFunction(request, principal.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<FunctionResponse> updateFunction(
            @PathVariable Long id,
            @Valid @RequestBody FunctionRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(functionService.updateFunction(id, request, principal.getId()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteFunction(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        functionService.deleteFunction(id, principal.getId());
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/toggle-active")
    public ResponseEntity<FunctionResponse> toggleActive(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(functionService.toggleActive(id, principal.getId()));
    }

    @PostMapping("/{id}/test")
    public ResponseEntity<FunctionTestResponse> testFunction(
            @PathVariable Long id,
            @RequestBody FunctionTestRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(functionService.testFunction(id, principal.getId(), request));
    }
}
