# WebCraft Game Server

브라우저에서 접속하는 마인크래프트 스타일 멀티플레이어 게임 **WebCraft**의 백엔드 서버입니다. 플레이어/월드 관리, 실시간 채팅, WebSocket 기반 게임 플레이(이동, 접속자 목록, 핑) 기능을 제공합니다.

## 기술 스택

- **Spring Boot 4.1** / **Java 21**
- **MySQL** — 플레이어, 월드, 채팅 메시지 등 영속 데이터
- **Redis** — 접속 상태(presence), 최근 채팅 캐시, 채팅 전송 속도 제한
- **Spring WebSocket** — 실시간 게임 통신
- **JPA(Hibernate)** — 인덱스, 낙관적/비관적 락을 포함한 데이터 접근
- **JUnit5 + Mockito, Testcontainers, H2** — 테스트

## 실행 방법

### 1. 사전 준비

- JDK 21
- Docker (MySQL, Redis 로컬 구동용)

### 2. MySQL / Redis 실행

```bash
docker run -d --name webcraft-mysql -p 3306:3306 \
  -e MYSQL_DATABASE=webcraft \
  -e MYSQL_ROOT_PASSWORD=<원하는 비밀번호> \
  mysql:8.0

docker run -d --name webcraft-redis -p 6379:6379 redis:7.4-alpine
```

### 3. 환경 변수 설정

프로젝트 루트에 `.env` 파일을 만들고 DB 계정 정보를 입력합니다. (`.env`는 `.gitignore`에 포함되어 저장소에 올라가지 않습니다.)

```
DB_USERNAME=root
DB_PASSWORD=<위에서 설정한 비밀번호>
```

`spring-dotenv` 라이브러리가 애플리케이션 시작 시 `.env`를 자동으로 읽어 `application.properties`의 `${DB_USERNAME}`, `${DB_PASSWORD}`에 주입합니다.

### 4. 애플리케이션 실행

```bash
./gradlew bootRun
```

기본적으로 `8080` 포트에서 서버가 뜹니다.

## 프로젝트 구조

```
src/main/java/com/gameexpert/
├── player/       # 플레이어 등록 (REST)
├── world/        # 월드 생성/조회/삭제 (REST)
├── chat/         # 채팅 저장, 조회, 이력, 캐시, 속도 제한
├── presence/     # Redis 기반 접속 상태 관리
├── trial/        # 낙관적 락(@Version)을 사용하는 월드 내 이벤트 상태
├── ws/           # WebSocket 핸드셰이크, 세션 관리, 메시지 라우팅/핸들러
└── common/       # 공통 예외 처리
```

## REST API

| Method | Path | 설명 |
|---|---|---|
| POST | `/players` | 닉네임으로 플레이어 등록 |
| GET | `/worlds` | 루트 월드 목록 조회 |
| POST | `/worlds` | 새 월드 생성 |
| DELETE | `/worlds/{id}` | 월드 삭제 |
| DELETE | `/worlds/{id}/if-matches` | 생성 시점 정보가 일치할 때만 월드 삭제 |
| GET | `/worlds/{worldId}/chats` | 최근 채팅 N개 조회 |
| GET | `/worlds/{worldId}/chats/history` | 커서 기반 채팅 이력 페이지 조회 |

## WebSocket 프로토콜

### 연결

```
ws://localhost:8080/ws/worlds/{worldId}?nickname={nickname}
```

- 핸드셰이크 시 닉네임(등록된 플레이어)과 월드 존재 여부를 검증합니다.
- 실패 시 커스텀 종료 코드로 연결이 닫힙니다: `4000`(닉네임/플레이어 없음), `4001`(월드 없음), `4002`(이미 접속 중인 닉네임).

### 메시지 타입

연결 이후 `{"type": "..."}` 형식의 JSON 메시지를 주고받습니다.

| type | 설명 |
|---|---|
| `move` | 플레이어 이동 요청 |
| `chat` | 같은 월드 참여자에게 채팅 전송 (속도 제한: 플레이어당 10초에 5회) |
| `ping` | 접속 상태 갱신(heartbeat) 및 `pong` 응답 |
| `onlineUsers` | 현재 월드의 접속자 닉네임 목록과 인원수 조회 |

## 구현 개요

Lv 1부터 단계적으로 기능을 쌓아 올린 프로젝트로, 주요 구현 내용은 다음과 같습니다.

- **인프라**: Docker로 MySQL/Redis 구동, `.env` + `spring-dotenv`로 자격 증명 분리
- **플레이어/월드**: Bean Validation, 서비스-컨트롤러 책임 분리(불변 `record`로 도메인 사실 전달)
- **채팅 저장/조회**: 최신순 조회 후 시간순으로 뒤집는 방식, 커서 기반 페이지네이션(`beforeCreatedAt`, `beforeId`)
- **WebSocket 인증**: 핸드셰이크에서 닉네임/월드를 조회해 세션 속성에 저장, 디버거로 직접 검증
- **세션/접속 관리**: `ConcurrentHashMap` 기반 세션 레지스트리, Redis ZSET으로 접속 상태(presence) 추적
- **메시지 라우팅**: `type` 필드 기준으로 핸들러에 위임하는 라우터 구조
- **동시성 제어**: `@Version`을 이용한 낙관적 락(같은 트랜잭션 내 다른 변경도 함께 롤백됨을 테스트로 검증), 비관적 락(`SELECT ... FOR UPDATE`)
- **캐시**: 최근 채팅 조회 결과를 Redis에 5초 TTL로 캐싱
- **속도 제한**: Redis Lua 스크립트로 "조회-증가-만료 설정"을 원자적으로 실행해 동시 요청 상황에서도 정확히 5회로 제한

## 테스트

```bash
./gradlew test
```

Redis/MySQL이 필요한 테스트는 Testcontainers로 격리된 컨테이너를 직접 띄워 실행되므로, 로컬에 Docker가 실행 중이어야 합니다.
