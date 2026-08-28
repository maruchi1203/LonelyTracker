package com.lonelytracker.backend.ai;

import com.lonelytracker.backend.common.AppProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;

/**
 * OpenAI 호출용 {@link RestClient} 배선.
 * <p>
 * 파서에서 떼어낸 이유는 <b>타임아웃 배관과 파싱 로직이 다른 관심사</b>이기 때문이다.
 * 파서가 직접 만들면 테스트에서 가짜 서버를 끼울 수 없다 —
 * {@code MockRestServiceServer} 가 심어둔 request factory 를 생성자가 덮어써 버린다.
 */
@Configuration
public class OpenAiClientConfig {

    /**
     * 빌더를 주입받지 않고 직접 만든다.
     * <p>
     * Boot 4 는 자동설정이 모듈로 쪼개져 있어 {@code RestClient.Builder} 빈이 기본으로 없다.
     * 모듈을 더할 수도 있지만, <b>외부 API 클라이언트를 앱 전역 Jackson 설정과 떼어두는 편이
     * 안전하다</b> — {@code default-property-inclusion: non_null} 같은 설정이 요청 본문에
     * 영향을 주면 곤란하다.
     */
    @Bean
    public RestClient openAiRestClient(AppProperties properties) {
        AppProperties.AiSetting setting = properties.ai();
        RestClient.Builder builder = RestClient.builder();

        // 두 타임아웃이 막는 것이 다르다.
        //   연결 — 서버에 붙는 데까지. 상대가 아예 죽어 있으면 여기서 걸린다
        //   읽기 — 응답을 다 받는 데까지. 붙긴 했는데 답이 안 오면 여기서 걸린다
        // JdkClientHttpRequestFactory 는 연결 쪽 setter 가 없어 HttpClient 를 직접 만든다.
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(setting.connectTimeout())
                .build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
        factory.setReadTimeout(setting.readTimeout());

        return builder
                .requestFactory(factory)
                .baseUrl(setting.baseUrl())
                .build();
    }
}
