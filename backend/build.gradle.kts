plugins {
	java
	id("org.springframework.boot") version "4.1.0"
	id("io.spring.dependency-management") version "1.1.7"
}

group = "com.lonelytracker"
version = "0.0.1-SNAPSHOT"
description = "AI schedule manager backend"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(21)
	}
}

repositories {
	mavenCentral()
}

dependencies {
	implementation("org.springframework.boot:spring-boot-starter-actuator")
	implementation("org.springframework.boot:spring-boot-starter-data-jpa")
	// Boot 4는 오토컨피그가 모듈로 쪼개져 있어 spring-boot-flyway 가 있어야
	// 마이그레이션이 기동 시 자동 실행된다. flyway-core 만으로는 아무 일도 일어나지 않는다.
	// 또 Flyway 10부터 DB별 지원이 분리돼 postgresql 모듈도 함께 필요하다.
	implementation("org.springframework.boot:spring-boot-flyway")
	implementation("org.flywaydb:flyway-database-postgresql")
	implementation("org.springframework.boot:spring-boot-starter-validation")
	implementation("org.springframework.boot:spring-boot-starter-webmvc")
	compileOnly("org.projectlombok:lombok")
	developmentOnly("org.springframework.boot:spring-boot-devtools")
	runtimeOnly("org.postgresql:postgresql")
	annotationProcessor("org.projectlombok:lombok")
	testImplementation("org.springframework.boot:spring-boot-starter-actuator-test")
	testImplementation("org.springframework.boot:spring-boot-starter-data-jpa-test")
	testImplementation("org.springframework.boot:spring-boot-starter-validation-test")
	testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
	testCompileOnly("org.projectlombok:lombok")
	// 테스트는 실제 PostgreSQL 컨테이너에 붙는다. H2로는 Flyway의 generate_series,
	// LIKE ... ESCAPE 같은 PostgreSQL 전용 문법을 검증할 수 없다.
	// Testcontainers 2.x에서 아티팩트 이름이 바뀌었다(postgresql -> testcontainers-postgresql).
	testImplementation("org.springframework.boot:spring-boot-testcontainers")
	testImplementation("org.testcontainers:testcontainers-junit-jupiter")
	testImplementation("org.testcontainers:testcontainers-postgresql")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
	testAnnotationProcessor("org.projectlombok:lombok")
}

// 소스에 한글 주석·메시지가 있다. 지정하지 않으면 javac가 플랫폼 기본 인코딩을 쓰므로
// 로케일이 다른 환경(CI 등)에서 문자열이 깨진다.
tasks.withType<JavaCompile> {
	options.encoding = "UTF-8"
}

tasks.withType<Test> {
	useJUnitPlatform()
	// 어떤 테스트가 돌았는지 콘솔에 보이게 한다.
	// 테스트가 0개인데 BUILD SUCCESSFUL 이 뜨는 상황을 눈으로 잡기 위함.
	testLogging {
		events("passed", "failed", "skipped")
	}
}
