# Convention

## 1. Commit Convention

### 1.1. Commit Message Format

```text
<type>: <description> (#<issue-number>)
````

* `description`은 변경 내용을 간결하게 작성합니다.
* 문장 끝에 마침표는 사용하지 않습니다.
* 하나의 커밋에는 가능한 한 하나의 작업 단위를 담습니다.
* 관련 Issue가 있는 경우 Commit Message 뒤에 Issue 번호를 작성합니다.
* Issue와 직접 관련되지 않은 단순 작업은 Issue 번호를 생략할 수 있습니다.

### 1.2. Commit Type

| Type       | Description                  |
| ---------- | ---------------------------- |
| `feat`     | 새로운 기능 추가                    |
| `fix`      | 버그 수정                        |
| `refactor` | 기능 변경 없이 코드 구조 개선            |
| `chore`    | 빌드, 설정, 의존성, 개발 환경 등의 변경     |
| `docs`     | 문서 작성 및 수정                   |
| `test`     | 테스트 코드 추가 및 수정               |
| `ci`       | CI/CD 설정 및 워크플로 변경           |
| `perf`     | 성능 개선                        |
| `style`    | 코드 동작에 영향을 주지 않는 포맷 및 스타일 수정 |
| `infra`    | 서버, 네트워크, 클라우드 및 배포 인프라 변경   |

### 1.3. Examples

```text
feat: add JWT authentication filter (#12)
fix: handle invalid request body (#18)
refactor: simplify validation error mapping (#24)
chore: configure local MySQL Docker Compose (#31)
docs: update project setup guide (#35)
test: add JWT provider tests (#40)
ci: add GitHub Actions workflow
perf: optimize database query (#67)
infra: configure production deployment environment (#108)
```

---

## 2. Branch Convention

### 2.1. Branch Name Format

```text
<type>/#<issue-number>-<description>
```

* `description`은 영문 소문자와 `kebab-case`를 사용합니다.
* 작업과 관련된 Issue가 있다면 Issue 번호를 포함합니다.

### 2.2. Branch Type

| Type       | Description |
| ---------- | ----------- |
| `feature`  | 새로운 기능 개발   |
| `fix`      | 버그 수정       |
| `refactor` | 리팩토링        |
| `chore`    | 설정 및 기타 작업  |
| `docs`     | 문서 작업       |
| `test`     | 테스트 작업      |
| `ci`       | CI/CD 작업    |
| `perf`     | 성능 개선       |
| `infra`    | 인프라 관련 작업   |

### 2.3. Examples

```text
feature/#12-jwt-authentication
fix/#18-validation-error-response
refactor/#24-security-config
chore/#31-docker-compose
docs/#35-readme-update
test/#38-jwt-provider-test
ci/#40-github-actions
perf/#45-query-optimization
infra/#50-aws-deployment
```

Issue를 생성하면 제목의 Type과 설명을 기반으로 브랜치가 자동 생성됩니다. 한글 설명은 영문 `kebab-case` slug로 변환되며, 브랜치는 저장소의 default branch를 기준으로 생성됩니다.

---

## 3. Issue Convention

### 3.1. Issue Title Format

```text
<emoji> [TYPE] <description>
```

### 3.2. Issue Type

| Emoji | Type       | Description                |
| ----- | ---------- | -------------------------- |
| ✨     | `FEAT`     | 새로운 기능                     |
| 🐛    | `FIX`      | 버그 수정                      |
| ♻️    | `REFACTOR` | 코드 구조 개선                   |
| 🧹    | `CHORE`    | 설정, 환경 구성 및 기타 작업          |
| 📝    | `DOCS`     | 문서 작업                      |
| ✅     | `TEST`     | 테스트 관련 작업                  |
| 👷    | `CI`       | CI/CD 관련 작업                |
| ⚡     | `PERF`     | 성능 개선                      |
| ☁️    | `INFRA`    | 서버, 네트워크, 클라우드 및 배포 인프라 작업 |

### 3.3. Examples

```text
✨ [FEAT] JWT 인증 구현
🐛 [FIX] 잘못된 요청 본문 예외 처리
♻️ [REFACTOR] Security 설정 구조 개선
🧹 [CHORE] 로컬 Docker Compose 구성
📝 [DOCS] README 작성
✅ [TEST] JWT Provider 테스트 추가
👷 [CI] GitHub Actions CI 구성
⚡ [PERF] 사용자 조회 쿼리 최적화
☁️ [INFRA] AWS 배포 환경 구성
```

---

## 4. Pull Request Convention

### 4.1. PR Title Format

```text
<emoji> [TYPE] <description>
```

* PR Type과 Emoji는 Issue Convention을 따릅니다.
* 가능하면 연결된 Issue 제목과 동일한 Type을 사용합니다.

### 4.2. Issue Closing

PR Merge 시 관련 Issue를 자동으로 닫으려면 다음 형식을 사용합니다.

```text
Closes #12
```

여러 Issue를 함께 닫는 경우 각각 작성합니다.

```text
Closes #12
Closes #15
```

Issue를 닫지 않고 단순히 참조하는 경우 다음과 같이 작성합니다.

```text
Related to #12
```

---

## 5. General Rules

* 하나의 Issue는 가능한 한 하나의 명확한 작업 단위를 다룹니다.
* 하나의 Branch는 하나의 Issue를 기준으로 작업하는 것을 권장합니다.
* 하나의 PR에는 관련성이 높은 변경 사항만 포함합니다.
* 기능 구현과 무관한 대규모 포맷 변경은 별도 PR로 분리합니다.
* Merge 전 테스트 및 기존 기능 영향 여부를 확인합니다.
