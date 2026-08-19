package com.lonelytracker.backend.user;

import com.lonelytracker.backend.user.dto.UserCreateRequest;
import com.lonelytracker.backend.user.dto.UserResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final CurrentUserProvider currentUserProvider;

    /** 현재 로그인한 사용자. 인증이 붙기 전까지는 항상 기본 사용자를 반환한다. */
    @GetMapping("/me")
    public UserResponse me() {
        return UserResponse.from(currentUserProvider.get());
    }

    /** 사용자 단건 조회. 없으면 404. */
    @GetMapping("/{id}")
    public UserResponse findById(@PathVariable Long id) {
        return userService.findById(id);
    }

    /** 사용자 생성. 추천 카테고리(육체·정신·능력·취미)가 함께 만들어진다. */
    @PostMapping
    public ResponseEntity<UserResponse> create(@Valid @RequestBody UserCreateRequest request) {
        UserResponse created = userService.create(request.username(), request.displayName());
        return ResponseEntity
                .created(URI.create("/api/users/" + created.id()))
                .body(created);
    }
}
