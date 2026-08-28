package com.lonelytracker.backend.ai;

import com.lonelytracker.backend.common.AppProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;

/** OpenAI 호출용 {@link RestClient} 배선 */
@Configuration
public class OpenAiClientConfig {

    /** 연결·읽기 타임아웃과 기본 주소를 건 RestClient 를 만든다. */
    @Bean
    public RestClient openAiRestClient(AppProperties properties) {
        AppProperties.AiSetting setting = properties.ai();
        RestClient.Builder builder = RestClient.builder();

        // 연결 — 서버에 붙는 데까지 / 읽기 — 응답을 다 받는 데까지
        // JdkClientHttpRequestFactory 에는 연결 쪽 setter 가 없다
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
