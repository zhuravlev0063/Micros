# Микросервисное веб-приложение для поиска авиабилетов

Проект реализует ТЗ: Java, Spring Boot, Spring Data JDBC, Spring Security, Kafka,
PostgreSQL, Docker, JUnit, Testcontainers, TypeScript, React и Tailwind CSS.

## Состав

- `api-gateway` - единая точка входа, маршрутизация запросов и проверка JWT.
- `auth-service` - регистрация, вход, access/refresh токены, пользователи в PostgreSQL.
- `search-service` - поиск авиабилетов через контракт поставщиков, история поисков в PostgreSQL, события Kafka.
- `frontend` - React/TypeScript/Tailwind интерфейс.

## Запуск

```powershell
docker compose up --build
```

После запуска:

- Frontend: http://localhost:5173
- API Gateway: http://localhost:8080
- Auth Service: http://localhost:8081
- Search Service: http://localhost:8082
- PostgreSQL: localhost:5432, база `flights`, пользователь `flights`, пароль `flights`
- Kafka: localhost:29092

## Проверка API

```powershell
$body = @{ email = "user@example.com"; password = "Password123!"; firstName = "Ivan"; lastName = "Ivanov" } | ConvertTo-Json
Invoke-RestMethod http://localhost:8080/api/auth/register -Method Post -ContentType "application/json" -Body $body

$login = @{ email = "user@example.com"; password = "Password123!" } | ConvertTo-Json
$tokens = Invoke-RestMethod http://localhost:8080/api/auth/login -Method Post -ContentType "application/json" -Body $login

Invoke-RestMethod "http://localhost:8080/api/search/flights?origin=MOW&destination=AER&departureDate=2026-06-01&passengers=1" -Headers @{ Authorization = "Bearer $($tokens.accessToken)" }
```

## Тесты

```powershell
mvn test
```

Если Docker Desktop запущен и доступен из терминала, тесты `auth-service` и
`search-service` поднимут временный PostgreSQL через Testcontainers. Если Docker
недоступен, эти интеграционные тесты будут корректно пропущены, а остальные
JUnit-тесты продолжат выполняться.

## Что реализовано по безопасности и отказоустойчивости

- `Auth Service` выдает access и refresh токены.
- `POST /api/auth/logout` отзывает refresh-токен.
- JWT содержит роль `USER`.
- `API Gateway` проверяет JWT и наличие роли `USER` для `/api/search/**`.
- `Search Service` обращается к поставщикам через контракт `FlightProvider`.
- Для поставщиков добавлены retry и timeout; ошибка одного поставщика не
  останавливает поиск.
- Сервисы публикуют и читают Kafka-события для демонстрации асинхронного обмена.
