# 설치와 실행

> **목표는 `java -jar lonelytracker.jar` 한 줄입니다.** PostgreSQL 을 내장하고 프론트를
> jar 안에 넣어 JDK 하나만 있으면 돌아가게 만드는 중입니다. 아래는 그 전까지의 방법입니다.

앱이 무엇인지는 [README](README.md) 에 있습니다.

---

## 준비물

| | 버전 |
| --- | --- |
| JDK | 21 |
| Node.js | 20 이상 |
| PostgreSQL | 17 |

## 1. DB 를 만든다

```sql
CREATE USER lonelytracker WITH PASSWORD '아무거나';
CREATE DATABASE lonelytracker OWNER lonelytracker;
```

테이블은 만들지 않습니다. **Flyway 가 첫 실행 때 전부 세웁니다.** 반대로 이미 테이블이 있는
DB 를 주면 기동이 실패합니다. 조용히 건너뛰다가 스키마가 어긋나는 것보다 낫기 때문입니다.

## 2. `backend/.env` 를 적는다

```properties
DB_PASSWORD=위에서-정한-비밀번호
LONELYTRACKER_ENCRYPTION_KEY=아무-긴-문자열
```

`DB_PASSWORD` 만 필수입니다. 기본값이 없어 비어 있으면 서버가 뜨지 않습니다.

`LONELYTRACKER_ENCRYPTION_KEY` 는 설정 화면에서 API 키를 저장할 때 씁니다. 없어도 앱은
뜨지만 그 기능만 막힙니다. **한 번 정하면 바꾸지 않습니다.** 바꾸면 저장해 둔 키를 읽지 못합니다.

## 3. 띄운다

창 두 개가 필요합니다.

```bash
cd backend && ./gradlew bootRun
```

```bash
cd frontend && npm install && npm run dev
```

`http://localhost:5173` 을 엽니다. 프론트가 `/api` 를 8080 으로 넘겨주므로 CORS 설정은 없습니다.

## 원격 DB 를 쓰려면 (Supabase · Neon 등)

PostgreSQL 이면 그대로 돌아갑니다. `.env` 에 주소만 더 적습니다.

```properties
DB_URL=jdbc:postgresql://db.xxxx.supabase.co:5432/postgres
DB_USERNAME=postgres
DB_PASSWORD=...
```

> **포트를 확인하세요.** Supabase 가 기본으로 보여주는 주소는 `6543` 번 풀러(pgbouncer)입니다.
> 그쪽으로는 Flyway 마이그레이션이 깨집니다. **`5432` 직결 주소를 써야 합니다.**

---

## AI 설정

자연어 입력을 쓰려면 AI 제공자의 API 키가 필요합니다. 편한 쪽을 쓰면 됩니다.

**1. `.env` — clone 해서 바로 쓸 때**

위에서 만든 `backend/.env` 에 세 줄을 더합니다. **따옴표를 붙이지 않습니다.** properties 형식으로 읽혀 따옴표가 값에 들어갑니다.

```properties
AI_BASE_URL=https://generativelanguage.googleapis.com/v1beta/openai
AI_MODEL=gemini-2.5-flash
AI_API_KEY=발급받은-키
```

**2. 설정 화면 — 제공자를 오가며 쓸 때**

제공자마다 키를 따로 저장하고 전환합니다. 화면에서 고른 것이 `.env` 보다 먼저입니다.
키를 DB 에 암호화해 저장하므로 `LONELYTRACKER_ENCRYPTION_KEY` 가 있어야 합니다.

| 제공자 | `AI_BASE_URL`                                             | `AI_MODEL` 예               |
| ------ | --------------------------------------------------------- | --------------------------- |
| OpenAI | `https://api.openai.com/v1`                               | `gpt-5.6-luna`              |
| Gemini | `https://generativelanguage.googleapis.com/v1beta/openai` | `gemini-2.5-flash`          |
| Claude | `https://api.anthropic.com/v1`                            | `claude-haiku-4-5-20251001` |

OpenAI 호환 주소면 목록에 없는 제공자도 쓸 수 있습니다. Claude 는 주소를 보고 네이티브 규약으로 자동 전환됩니다.

제공자마다 **이번 달 토큰 한도**를 설정 화면에서 정할 수 있습니다. 비워 두면 한도가 없습니다.
한도를 넘겼을 때의 동작은 [README](README.md#ai-가-하는-일) 에 있습니다.

---
