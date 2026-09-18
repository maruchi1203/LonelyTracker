package com.lonelytracker.backend.user.controller;

import com.lonelytracker.backend.user.dto.AiProviderListResponse;
import com.lonelytracker.backend.user.dto.AiProviderRequest;
import com.lonelytracker.backend.user.dto.AiProviderResponse;
import com.lonelytracker.backend.user.service.AiProviderService;
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

/** AI 제공자별 제공자 설정. 키 원본은 어떤 응답에도 싣지 않는다 */
@RestController
@RequestMapping("/api/users/me/ai-providers")
@RequiredArgsConstructor
public class AiProviderController {

    private final AiProviderService providerService;

    @GetMapping
    public AiProviderListResponse list() {
        return providerService.list();
    }

    /** 주소가 같으면 고치고 없으면 만든다. 저장한 것을 바로 쓴다 */
    @PutMapping
    public AiProviderResponse save(@Valid @RequestBody AiProviderRequest request) {
        return providerService.save(request);
    }

    /** 이 제공자 설정으로 부르게 한다 */
    @PutMapping("/{id}/active")
    public AiProviderResponse activate(@PathVariable Long id) {
        return providerService.activate(id);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        providerService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
