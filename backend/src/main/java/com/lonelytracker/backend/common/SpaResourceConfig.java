package com.lonelytracker.backend.common;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

import java.io.IOException;

/**
 * jar 안에 담은 프론트를 내준다.
 * <p>
 * 없는 경로는 index.html 로 돌린다. 주소창에 /habits 를 직접 치면 서버에 그런 파일이
 * 없고, 화면 경로를 아는 것은 React Router 뿐이다.
 */
@Configuration
public class SpaResourceConfig implements WebMvcConfigurer {

    private static final String INDEX = "/static/index.html";

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/")
                .resourceChain(true)
                .addResolver(new PathResourceResolver() {

                    @Override
                    protected Resource getResource(String path, Resource location)
                            throws IOException {
                        // API 는 없으면 없다고 해야 한다. index.html 을 주면 404 가 200 이 된다
                        if (path.startsWith("api/")) {
                            return null;
                        }

                        Resource asked = location.createRelative(path);

                        if (asked.exists() && asked.isReadable()) {
                            return asked;
                        }

                        // 프론트를 빼고 띄우면 index.html 이 없다. 있는 척하면 404 대신 500 이 난다
                        Resource index = new ClassPathResource(INDEX);

                        return index.exists() ? index : null;
                    }
                });
    }
}
