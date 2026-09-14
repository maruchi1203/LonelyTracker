package com.lonelytracker.backend.user.service;

/**
 * 이번 호출이 향할 곳. 사용자가 고른 자격 증명이거나 서버 설정이다
 */
public record AiTarget(String baseUrl, String model, String apiKey) {
}
