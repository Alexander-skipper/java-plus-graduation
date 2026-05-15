## Архитектура

**Микросервисы (core):**

- `event-service` — управление мероприятиями.
- `request-service` — заявки на участие.
- `user-service` — пользователи.
- `location-service` — локации.

**Инфраструктура (infra):**

- API Gateway — порт 8080
- Config Server — порт 8888
- Discovery Server (Eureka) — порт 8761

**Статистика (stats):**

- `stats-server` — сбор статистики.
- `stats-client` — клиент для отправки хитов.

**Взаимодействие:** OpenFeign + Eureka + ConfigServer