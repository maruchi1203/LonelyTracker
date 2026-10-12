plugins {
	java
	id("org.springframework.boot") version "4.1.1"
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
	testImplementation("org.springframework.boot:spring-boot-testcontainers")
	testImplementation("org.testcontainers:testcontainers-junit-jupiter")
	testImplementation("org.testcontainers:testcontainers-postgresql")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
	testAnnotationProcessor("org.projectlombok:lombok")
}

// 소스에 한글 주석·메시지가 있다. 지정하지 않으면 javac가 플랫폼 기본 인코딩을 씀
tasks.withType<JavaCompile> {
	options.encoding = "UTF-8"
}

// ── 프론트를 jar 에 담는다 ───────────────────────────────────────────────
// bootRun 과 test 는 건드리지 않는다. 개발 중에는 5173 의 vite 가 프론트를 맡는다

val frontendDir = layout.projectDirectory.dir("../frontend")
val frontendDist = frontendDir.dir("dist")
val npm = if (System.getProperty("os.name").startsWith("Windows")) "npm.cmd" else "npm"

val installFrontend by tasks.registering(Exec::class) {
	workingDir = frontendDir.asFile
	// ci 가 아닌 까닭: ci 는 node_modules 를 먼저 지운다. Windows 에서 vite 나 편집기가
	// 네이티브 바이너리를 잡고 있으면 그 삭제가 EPERM 으로 멈추고 트리가 반만 남는다
	commandLine(npm, "install")

	// 잠금 파일이 그대로면 다시 받지 않는다
	inputs.file(frontendDir.file("package.json"))
	inputs.file(frontendDir.file("package-lock.json"))
	outputs.dir(frontendDir.dir("node_modules"))
}

val buildFrontend by tasks.registering(Exec::class) {
	dependsOn(installFrontend)
	workingDir = frontendDir.asFile
	commandLine(npm, "run", "build")

	inputs.dir(frontendDir.dir("src"))
	inputs.file(frontendDir.file("index.html"))
	inputs.file(frontendDir.file("vite.config.ts"))
	outputs.dir(frontendDist)
}

tasks.bootJar {
	dependsOn(buildFrontend)
	from(frontendDist) {
		into("BOOT-INF/classes/static")
	}
}

tasks.withType<Test> {
	useJUnitPlatform()
	// 어떤 테스트가 돌았는지 콘솔에 보이게 함
	testLogging {
		events("passed", "failed", "skipped")
	}
}
