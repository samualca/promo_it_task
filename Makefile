.PHONY: up down logs test package clean

up:
	cp -n .env.example .env || true
	docker compose up --build

down:
	docker compose down

logs:
	docker compose logs -f app

test:
	mvn test

package:
	mvn -DskipTests package

clean:
	docker compose down -v
	mvn clean
