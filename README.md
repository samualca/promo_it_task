# Promo IT OTP Service

Backend-сервис для защиты операций одноразовыми OTP-кодами. Реализованы регистрация, JWT-аутентификация, роли `ADMIN` и `USER`, настройка параметров OTP, генерация и валидация кодов, PostgreSQL/JDBC и доставка через file, email, SMPP и Telegram.

## Быстрый запуск

```bash
make up
```

Команда создаст `.env` из `.env.example`, соберет Docker-образ приложения и поднимет:

- `app` на `http://localhost:8080`
- PostgreSQL 17 на `localhost:15432`
- Mailpit SMTP на `localhost:1025`
- Mailpit UI на `http://localhost:8025`
- SMPPsim на `localhost:2775`
- SMPPsim UI на `http://localhost:8088`

Telegram использует внешний API и настраивается через `TELEGRAM_BOT_TOKEN` и `TELEGRAM_CHAT_ID` в `.env`.

По умолчанию Makefile использует `docker-compose`. Если в вашей системе установлен Compose как plugin-команда, можно запускать так:

```bash
make up COMPOSE="docker compose"
```

## API

Все ответы имеют вид:

```json
{"success":true,"data":{},"error":null}
```

### Регистрация

```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H 'Content-Type: application/json' \
  -d '{"login":"admin","password":"secret123","role":"ADMIN"}'
```

Второй администратор запрещен. Для обычного пользователя:

```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H 'Content-Type: application/json' \
  -d '{"login":"user","password":"secret123","role":"USER"}'
```

### Логин

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"login":"user","password":"secret123"}'
```

Ответ содержит `token`. Дальше передавайте его в заголовке:

```bash
Authorization: Bearer <token>
```

### Admin API

Получить конфигурацию:

```bash
curl http://localhost:8080/api/admin/config -H "Authorization: Bearer <admin-token>"
```

Изменить конфигурацию:

```bash
curl -X PUT http://localhost:8080/api/admin/config \
  -H "Authorization: Bearer <admin-token>" \
  -H 'Content-Type: application/json' \
  -d '{"codeLength":6,"ttlSeconds":300}'
```

Список пользователей без администраторов:

```bash
curl http://localhost:8080/api/admin/users -H "Authorization: Bearer <admin-token>"
```

Удалить пользователя и его OTP-коды:

```bash
curl -X DELETE http://localhost:8080/api/admin/users/2 -H "Authorization: Bearer <admin-token>"
```

### User API

Сгенерировать OTP:

```bash
curl -X POST http://localhost:8080/api/otp/generate \
  -H "Authorization: Bearer <user-token>" \
  -H 'Content-Type: application/json' \
  -d '{"operationId":"payment-100","channel":"FILE","destination":"local"}'
```

Каналы: `FILE`, `EMAIL`, `SMS`, `TELEGRAM`.

Для `FILE` код пишется в `otp-codes.txt` внутри рабочей директории контейнера. Для `EMAIL` письмо можно посмотреть в Mailpit UI. Для `SMS` используется SMPPsim из Docker Compose. Для `TELEGRAM` нужны реальные token/chat id.

Пример генерации кода через SMPPsim:

```bash
curl -X POST http://localhost:8080/api/otp/generate \
  -H "Authorization: Bearer <user-token>" \
  -H 'Content-Type: application/json' \
  -d '{"operationId":"payment-sms-100","channel":"SMS","destination":"79001234567"}'
```

Валидировать OTP:

```bash
curl -X POST http://localhost:8080/api/otp/validate \
  -H "Authorization: Bearer <user-token>" \
  -H 'Content-Type: application/json' \
  -d '{"operationId":"payment-100","code":"123456"}'
```

## Локальная сборка

```bash
make package
mvn test
```

## Остановить окружение

```bash
make down
```

Удалить volume PostgreSQL:

```bash
make clean
```
