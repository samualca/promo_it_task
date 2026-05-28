COMPOSE ?= docker-compose

.PHONY: up down logs test package clean

up:
	cp -n .env.example .env || true
	$(COMPOSE) up --build

down:
	$(COMPOSE) down

logs:
	$(COMPOSE) logs -f app

test:
	mvn test

package:
	mvn -DskipTests package

clean:
	$(COMPOSE) down -v
	mvn clean
