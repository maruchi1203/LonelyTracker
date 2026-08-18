# 일정 CRUD API 설계 (v1)

작성일: 2026-08-18 (로드맵 1일차)
기준 문서: `자기개발/프로젝트-AI일정관리앱-기획.md`

## 1. 도메인 모델 — Schedule

| 필드 | 타입 | 제약 | 설명 |
|---|---|---|---|
| `id` | Long | PK, auto | 식별자 |
| `title` | String(200) | not null | 일정 제목 |
| `description` | TEXT | nullable | 상세 메모. **마크다운 원문**을 그대로 저장하고 렌더링은 화면이 담당 |
| `startAt` | LocalDateTime | not null | 시작 시각 |
| `endAt` | LocalDateTime | nullable | 종료 시각 (없으면 시점 일정) |
| `allDay` | boolean | not null, default false | 종일 일정 여부 |
| `status` | enum | not null, default `PLANNED` | `PLANNED` / `DONE` / `SKIPPED` |
| `category` | String(100) | nullable | 계층 경로. `\`로 하위 구분 (예: `능력\개발\SpringBoot`) |
| `createdAt` | LocalDateTime | not null | 생성 시각 (`@CreatedDate`) |
| `updatedAt` | LocalDateTime | not null | 수정 시각 (`@LastModifiedDate`) |

**설계 의도**
- `status`는 4순위 기능인 "주간 수행률 리포트"의 계산 근거. 1일차부터 넣어두면 나중에 마이그레이션이 필요 없음.
- `category`는 자유 문자열이며 `\`로 계층을 표현한다. 별도 테이블 없이 문자열 경로로 둔 이유는, v1 규모에서 조인 없이 prefix 조회만으로 "하위 포함 필터"가 해결되기 때문.
- 일정의 성격 구분(수동 입력 / AI 파싱 등)은 별도 필드를 두지 않고 `category`로 처리한다.

**인덱스**: `startAt` (기간 조회가 가장 흔한 쿼리), `status`, `category` (prefix LIKE는 앞이 고정이라 인덱스를 탄다).

## 2. 엔드포인트

베이스 경로: `/api/schedules`

| 메서드 | 경로 | 설명 | 성공 응답 |
|---|---|---|---|
| `GET` | `/api/schedules?from=&to=&status=&category=` | 기간·상태·카테고리로 목록 조회 | 200 + 배열 |
| `GET` | `/api/schedules/{id}` | 단건 조회 | 200 |
| `POST` | `/api/schedules` | 생성 | 201 + `Location` 헤더 |
| `PUT` | `/api/schedules/{id}` | 전체 수정 | 200 |
| `PATCH` | `/api/schedules/{id}/status` | 상태만 변경 (완료 체크용) | 200 |
| `DELETE` | `/api/schedules/{id}` | 삭제 | 204 |

`from` / `to`는 ISO-8601 (`2026-08-18T00:00:00`). 생략 시 전체 조회.
`category`는 **하위 카테고리까지 포함**해 조회한다. `능력`으로 조회하면 `능력`, `능력\개발`은 걸리고 `능력강화`는 걸리지 않는다.
`PATCH .../status`를 따로 둔 이유: 목록에서 체크박스 하나 누르는 동작에 전체 객체를 왕복시키지 않기 위함.

## 3. 요청 / 응답 형태

**생성 요청** `POST /api/schedules`
```json
{
  "title": "회의",
  "description": null,
  "startAt": "2026-08-19T15:00:00",
  "endAt": "2026-08-19T16:00:00",
  "allDay": false,
  "category": "능력"
}
```

**응답**
```json
{
  "id": 1,
  "title": "회의",
  "startAt": "2026-08-19T15:00:00",
  "endAt": "2026-08-19T16:00:00",
  "allDay": false,
  "status": "PLANNED",
  "category": "능력",
  "source": "MANUAL",
  "createdAt": "2026-08-18T14:40:00",
  "updatedAt": "2026-08-18T14:40:00"
}
```

**에러 응답** (공통, `@RestControllerAdvice`)
```json
{
  "timestamp": "2026-08-18T14:40:00",
  "status": 400,
  "error": "Bad Request",
  "message": "title은 필수입니다",
  "path": "/api/schedules"
}
```

| 상황 | 코드 |
|---|---|
| 검증 실패 (`@Valid`) | 400 |
| 해당 id 없음 | 404 |
| `endAt` < `startAt` | 400 |

## 4. 패키지 구조

```
com.lonelytracker.backend
├─ schedule
│  ├─ Schedule.java            // 엔티티
│  ├─ ScheduleStatus.java      // enum
│  ├─ ScheduleSource.java      // enum
│  ├─ ScheduleRepository.java  // JpaRepository + JpaSpecificationExecutor
│  ├─ ScheduleSpecs.java       // 동적 조회 조건
│  ├─ ScheduleService.java
│  ├─ ScheduleController.java
│  └─ dto/
│     ├─ ScheduleCreateRequest.java
│     ├─ ScheduleUpdateRequest.java
│     ├─ ScheduleStatusRequest.java
│     └─ ScheduleResponse.java
└─ common
   ├─ GlobalExceptionHandler.java
   ├─ NotFoundException.java
   └─ JpaConfig.java           // @EnableJpaAuditing
```

엔티티를 그대로 반환하지 않고 `ScheduleResponse`로 감싸는 이유: JPA 지연 로딩이 JSON 직렬화 중에 터지는 문제를 원천 차단하고, 나중에 필드가 늘어도 API 계약이 흔들리지 않게 하기 위함.

## 5. 이후 단계에서 붙일 엔드포인트 (지금은 설계만)

| 로드맵 | 엔드포인트 | 설명 |
|---|---|---|
| 4-5일차 | `POST /api/schedules/parse` | 자연어 문장 → Schedule 초안 반환 (저장은 사용자 확인 후) |
| 6일차 | `GET /api/coaching/daily` | 과거 기록 기반 하루 계획 제안 |
| 7일차 | `GET /api/reports/weekly?week=` | 주간 수행률 요약 |

`parse`를 "바로 저장"이 아니라 "초안 반환"으로 둔 이유: AI가 시각을 잘못 잡았을 때 사용자가 고칠 여지를 남기기 위함. 확정 저장은 기존 `POST /api/schedules`를 재사용.

## 5-1. 구현하며 확정한 사항 (2026-08-18)

- **조회는 Specification(동적 쿼리)으로 구현.** 처음엔 JPQL에 `(:from is null or ...)` 패턴을 썼으나, PostgreSQL이 null 비교만 있는 파라미터의 타입을 추론하지 못해 `could not determine data type of parameter` 오류가 났다. 조건이 있을 때만 predicate를 붙이는 방식이 SQL도 더 깔끔하다.
- **요청 DTO의 `allDay`는 `Boolean`(래퍼).** 원시 `boolean`이면 JSON에서 필드를 생략했을 때 Jackson이 실패한다. 선택 필드이므로 래퍼를 쓰고 서비스에서 null을 false로 취급한다.
- **수정/상태변경은 `saveAndFlush`로 flush 후 응답 생성.** `@LastModifiedDate`가 flush 시점에 채워지기 때문에, 그냥 반환하면 응답의 `updatedAt`이 갱신 전 값으로 나간다.
- **`server.error.include-stacktrace: never` + `spring.devtools.add-properties: false`.** devtools가 스택트레이스 노출을 기본값으로 켜두기 때문에 명시적으로 껐다.

## 5-2. 스키마 관리 (2026-08-18 도입)

`ddl-auto: update` 는 컬럼 삭제와 타입 변경을 하지 않아 손으로 SQL을 돌려야 했다.
Flyway로 전환하고 `ddl-auto` 는 `validate` 로 내렸다. 스키마를 고치는 주체는 Flyway 하나뿐이다.

- 마이그레이션 위치: `backend/src/main/resources/db/migration/`
- `V1__init.sql` — 기존 스키마 베이스라인. 이미 테이블이 있는 DB에서는 실행되지 않고 기록만 된다.
- `V2__drop_source_and_widen_columns.sql` — source 제거, description → TEXT, category → varchar(100), 카테고리 인덱스.
- **이미 적용된 파일은 수정 금지.** checksum이 어긋나면 앱이 기동을 거부한다. 변경이 필요하면 새 버전을 추가한다.
- Boot 4에서는 `spring-boot-flyway` 모듈이 있어야 자동 실행된다. `flyway-core` 만으로는 아무 일도 일어나지 않는다.

## 6. 미결 사항

- 사용자 개념(멀티 유저) 도입 여부 — v1은 단일 사용자 전제라 `userId` 없음. 배포를 공개 URL로 할 경우 인증이 필요해짐.
- 반복 일정(매주 월요일 등) 지원 여부 — v1 스코프 밖.
- 페이지네이션 — 기간 조회로 충분하다고 보고 v1에서는 생략.
