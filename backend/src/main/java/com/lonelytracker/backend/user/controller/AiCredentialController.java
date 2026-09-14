package com.lonelytracker.backend.user.controller;

import com.lonelytracker.backend.user.dto.AiCredentialListResponse;
import com.lonelytracker.backend.user.dto.AiCredentialRequest;
import com.lonelytracker.backend.user.dto.AiCredentialResponse;
import com.lonelytracker.backend.user.service.AiCredentialService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** AI 제공자별 자격 증명. 키 원본은 어떤 응답에도 싣지 않는다 */
@RestController
@RequestMapping("/api/users/me/ai-credentials")
@RequiredArgsConstructor
public class AiCredentialController {

    private final AiCredentialService credentialService;

    @GetMapping
    public AiCredentialListResponse list() {
        return credentialService.list();
    }

    /** 주소가 같으면 고치고 없으면 만든다. 저장한 것을 바로 쓴다 */
    @PutMapping
    public AiCredentialResponse save(@Valid @RequestBody AiCredentialRequest request) {
        return credentialService.save(request);
    }

    /** 이 자격 증명으로 부르게 한다 */
    @PutMapping("/{id}/active")
    public AiCredentialResponse activate(@PathVariable Long id) {
        return credentialService.activate(id);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        credentialService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
