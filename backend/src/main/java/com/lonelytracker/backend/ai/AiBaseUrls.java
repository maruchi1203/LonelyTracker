package com.lonelytracker.backend.ai;

import java.net.URI;
import java.util.Locale;

/**
 * AI 제공자 주소를 다루는 유틸
 */
public final class AiBaseUrls {

    private AiBaseUrls() {
    }

    /**
     * 같은 제공자가 두 줄로 갈리지 않도록 주소를 한 모양으로 맞춘다
     *
     * @throws IllegalArgumentException 주소로 쓸 수 없을 때
     */
    public static String normalize(String raw) {
        String url = (raw == null) ? "" : raw.strip();

        // 1. 쪼개기. 따옴표나 가운데 공백이 있으면 여기서 걸린다
        URI uri;
        try {
            uri = URI.create(url);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("주소 형식이 올바르지 않습니다");
        }

        // 2. 거절
        String scheme = (uri.getScheme() == null) ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
        if (!scheme.equals("http") && !scheme.equals("https")) {
            throw new IllegalArgumentException("http 또는 https 로 시작하는 주소를 넣어 주세요");
        }
        if (uri.getHost() == null) {
            // 적긴 했는데 호스트로 읽히지 않은 것. 밑줄 같은 문자가 원인이다
            if (uri.getRawAuthority() != null) {
                throw new IllegalArgumentException(
                        "호스트 이름에 밑줄(_) 같은 문자는 쓸 수 없습니다. 하이픈(-)으로 바꾸거나 IP 주소를 넣어 주세요");
            }
            throw new IllegalArgumentException("주소에 호스트가 없습니다");
        }
        if (uri.getRawUserInfo() != null) {
            throw new IllegalArgumentException("주소에 계정 정보를 넣을 수 없습니다");
        }
        if (uri.getRawQuery() != null || uri.getRawFragment() != null) {
            throw new IllegalArgumentException("주소에 ? 나 # 를 붙일 수 없습니다");
        }

        // 3. 맞추기 — 호스트만 소문자로
        String host = uri.getHost().toLowerCase(Locale.ROOT);

        // 기본 포트면 버리고, 아니면 그대로 붙인다
        int port = uri.getPort();
        boolean defaultPort = port == -1
                || (scheme.equals("https") && port == 443)
                || (scheme.equals("http") && port == 80);
        String portPart = defaultPort ? "" : ":" + port;

        // 경로는 대소문자 그대로. 끝의 / 만 전부 뗀다
        String path = (uri.getRawPath() == null) ? "" : uri.getRawPath();
        while (path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }

        // 4. 조립
        return scheme + "://" + host + portPart + path;
    }
}
