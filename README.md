# POPLOT Backend

팝업스토어 방문 예약과 현장 체크인을 지원하는 POPLOT의 백엔드 서버입니다.

Java와 Spring Boot를 기반으로 예약, 대기열, QR 체크인 기능을 구현하며,
고경합 예약 환경에서의 동시성 제어와 대용량 트래픽 처리를 주요 기술 과제로 다룹니다.

## 기술 스택

- Java 21
- Spring Boot 4.1, Spring MVC
- Spring Data JPA, Flyway
- Spring Security, JWT
- PostgreSQL 17, H2
- SpringDoc OpenAPI, Swagger UI
- Docker, Docker Compose, GitHub Actions

## 프로젝트 구조

도메인별 Layered Architecture를 기본으로 사용합니다. Redis, 메시지 큐, 외부 API처럼 구현 교체나 장애 격리가 필요한 경계에만 Port/Adapter를 선택적으로 적용합니다.

```text
src/main/java/com/poplot
├── feature
│   ├── reservation
│   │   ├── controller
│   │   ├── dto
│   │   │   ├── request
│   │   │   └── response
│   │   ├── entity
│   │   ├── repository
│   │   ├── service
│   │   ├── exception
│   │   ├── port
│   │   │   ├── InventoryPort.java
│   │   │   └── NotificationPort.java
│   │   └── adapter
│   │       ├── DbInventoryAdapter.java
│   │       ├── RedisInventoryAdapter.java
│   │       └── SqsNotificationAdapter.java
│   ├── timeslot
│   ├── waitlist
│   ├── event
│   └── user
└── global
    ├── common
    ├── config
    ├── exception
    ├── logging
    └── security
```

각 feature는 다음 책임을 가집니다.

- `controller`: REST API 진입점
- `dto`: API 요청과 응답 모델
- `entity`: JPA 엔티티와 핵심 상태
- `repository`: Spring Data JPA 저장소
- `service`: 유스케이스와 트랜잭션 처리
- `exception`: feature별 오류 코드와 예외
- `port`: 서비스가 외부 구현에 요구하는 비즈니스 관점의 인터페이스
- `adapter`: Redis, SQS, 외부 API 등 Port의 기술별 구현

의존성 방향은 다음을 원칙으로 합니다.

```text
Controller → Service → Repository
                     → Port ← Adapter
```

단순한 JPA CRUD에는 Repository를 직접 사용합니다. 구현이 실제로 여러 개이거나, 교체 가능성·장애 격리·테스트 대역이 중요한 지점에만 Port/Adapter를 추가하여 불필요한 계층과 보일러플레이트를 피합니다.

`example` feature는 기본 Layered Architecture를 보여주는 실행 가능한 예시입니다.

```text
POST /api/v1/examples
GET  /api/v1/examples/{id}
```

## 공통 기능

- 일관된 API 성공·실패 응답
- `@RestControllerAdvice` 기반 전역 예외 처리
- JPA 생성·수정 시각 감사
- Stateless JWT 인증과 Spring Security 설정
- 요청별 `X-Request-Id` 추적 로깅
- Flyway 데이터베이스 마이그레이션
- Spring Boot Actuator 상태 확인
- Swagger UI 및 OpenAPI 문서

API 응답 형식은 다음과 같습니다.

```json
{
  "isSuccess": true,
  "code": "OK",
  "message": "요청에 성공했습니다.",
  "data": {}
}
```

## 시작하기

### 요구 사항

- JDK 21
- Docker 및 Docker Compose

### 환경 변수

`.env.example`을 `.env`로 복사한 뒤 값을 설정합니다.

```bash
cp .env.example .env
```

Windows PowerShell:

```powershell
Copy-Item .env.example .env
```

```dotenv
DB_HOST=localhost
DB_PORT=5432
DB_NAME=poplot_db
DB_USERNAME=postgres
DB_PASSWORD=postgres

# HMAC 키로 사용할 충분히 긴 임의 문자열
JWT_SECRET=
JWT_ACCESS_TOKEN_EXPIRATION=3600000
JWT_REFRESH_TOKEN_EXPIRATION=1209600000

# JWT와 다른 최소 32바이트 이상의 임의 문자열
QR_TICKET_SECRET=
```

`.env`는 Git에 포함하지 않습니다. `JWT_SECRET`과 `QR_TICKET_SECRET`에는 서로 다른 최소 32바이트 이상의 임의 값을 사용합니다.

QR 비밀키를 교체하면 기존 키로 발급한 모든 QR 티켓은 즉시 무효화됩니다. 모든 애플리케이션 인스턴스에 새 값을 동시에 배포한 뒤 예약 소유자가 티켓을 다시 조회해 재발급받도록 안내해야 합니다. 롤백할 때는 애플리케이션과 `QR_TICKET_SECRET`을 함께 이전 값으로 되돌려야 하며, 서로 다른 키를 사용하는 인스턴스를 동시에 운영하지 않습니다. 현재 `STAFF`와 `ADMIN`은 모든 이벤트를 체크인할 수 있으며, 매장별 권한은 Staff-Store 소속 모델이 추가된 후 제한해야 합니다.

### 로컬 실행

PostgreSQL을 실행합니다.

```bash
docker compose -f docker-compose.local.yml up -d
```

애플리케이션을 실행합니다.

```bash
./gradlew bootRun
```

Windows PowerShell:

```powershell
.\gradlew.bat bootRun
```

로컬 서버의 기본 주소는 `http://localhost:8080`입니다.

PostgreSQL을 종료하려면 다음 명령을 사용합니다.

```bash
docker compose -f docker-compose.local.yml down
```

`down -v`는 로컬 DB 데이터까지 삭제하므로 주의해야 합니다.

## 환경별 프로필

| 프로필 | 데이터베이스 | Flyway | JPA DDL |
| --- | --- | --- | --- |
| `local` | PostgreSQL | 활성화 | `validate` |
| `test` | H2 | 비활성화 | `create-drop` |
| `prod` | PostgreSQL | 활성화 | `validate` |

기본 프로필은 `local`입니다. 운영 환경에서는 `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`를 설정해야 합니다.

## 테스트와 빌드

```bash
./gradlew clean test
./gradlew clean build
```

Windows PowerShell:

```powershell
.\gradlew.bat clean test
.\gradlew.bat clean build
```

Pull Request와 `main`, `develop` 브랜치 Push 시 GitHub Actions에서 전체 빌드와 테스트를 실행합니다.

## Docker 이미지

```bash
docker build -t poplot-backend .
```

Dockerfile은 JDK 21 빌드 이미지와 JRE 21 실행 이미지를 사용하는 멀티 스테이지 빌드로 구성되어 있습니다.

## API 문서와 상태 확인

애플리케이션 실행 후 다음 경로에서 확인할 수 있습니다.

| 항목 | URL |
| --- | --- |
| Swagger UI | `http://localhost:8080/swagger-ui.html` |
| OpenAPI | `http://localhost:8080/v3/api-docs` |
| Health Check | `http://localhost:8080/actuator/health` |

## 개발 규칙

커밋, 브랜치, Issue 및 Pull Request 규칙은 [CONVENTION.md](.github/CONVENTION.md)를 참고합니다.

Issue를 생성하면 제목의 Type에 맞는 `<type>/#<issue-number>-<description>` 브랜치가 default branch에서 자동 생성됩니다. 한글 Issue 제목을 영문 slug로 변환하기 위해 Repository Secret에 `GROQ_API_KEY`를 설정해야 합니다.
