# LogAI - Incident Intelligence Backend

![Java](https://img.shields.io/badge/Java-21-orange)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.3-green)
![Angular](https://img.shields.io/badge/Angular-18-red)
![MySQL](https://img.shields.io/badge/MySQL-8-blue)
![Docker](https://img.shields.io/badge/Docker-Enabled-blue)
![AWS](https://img.shields.io/badge/AWS-EC2-orange)

> A production-grade AI-powered incident investigation platform that combines log ingestion, incident detection, root cause analysis, and conversational troubleshooting using OpenAI GPT-4o mini.

- **Live Demo:** http://35.154.51.190
- **Swagger UI:** http://35.154.51.190/api/swagger-ui/index.html
- **Frontend Repo:** [https://github.com/prajdeepreddy22/log-analyzer-ui](https://github.com/prajdeepreddy22/log-analyzer-ui)
- **Author:** Rajdeep Reddy - [GitHub](https://github.com/prajdeepreddy22)

---

## What Is LogAI?

LogAI is a full-stack AI-powered platform that helps developers and support engineers upload log files, inspect parsed log events, detect incidents, and get intelligent root cause analysis through a conversational assistant.

---

## Highlights

- Full-stack AI-powered log analysis platform
- Spring Boot 3 + Java 21 backend
- Angular 18 frontend
- OpenAI GPT-4o mini integration
- JWT authentication and authorization
- Real-time SSE streaming chat
- Flyway database migrations
- Dockerized deployment
- AWS EC2 hosted
- Swagger/OpenAPI documentation

Key capabilities:

- Upload `.log` and `.txt` files and parse them into structured, searchable log entries
- AI-powered incident analysis with severity scoring, root cause detection, fix suggestions, and confidence scoring
- Real-time SSE streaming chat with an AI assistant that understands uploaded log context
- JWT-secured REST API with per-user rate limiting
- Flyway-managed MySQL schema with Hibernate validation
- Operational endpoints through Spring Actuator and Micrometer

---

## Tech Stack

| Layer | Technology |
| --- | --- |
| Language | Java 21 |
| Framework | Spring Boot 3.3 |
| Database | MySQL 8 |
| Migrations | Flyway |
| AI | OpenAI GPT-4o mini |
| Auth | JWT, stateless Spring Security |
| Streaming | Server-Sent Events |
| Observability | Micrometer + Spring Actuator |
| Build | Maven |
| Container | Docker |
| Cloud | AWS EC2 t3.micro, ap-south-1 |
| Reverse Proxy | Nginx |

---

## Architecture

```text
Browser
  |
  v
Nginx (port 80)
  |
  |-- /* -------------> Angular SPA static files
  |
  `-- /api/* ---------> Spring Boot backend (port 8080)
                            |
                    +-------+--------+
                    |                |
                  MySQL          OpenAI API
                (Docker)       (GPT-4o mini)
```

The deployed portfolio version runs on a single AWS EC2 instance with Docker containers for MySQL and the Spring Boot backend. Nginx serves the Angular frontend and proxies `/api/*` traffic to the backend.

---

## Features

### Log Management

- Upload `.log` and `.txt` files up to 10 MB
- Parse logs into structured entries with level, timestamp, service name, host, environment, source, and message
- Search logs by keyword, level, service name, and time range
- Server-side pagination and sorting

### AI Analysis

- Async AI analysis per upload
- Standardized root cause taxonomy
- Severity score and confidence score for each analysis
- Incident grouping by upload and root cause
- Dedicated AI worker flow so long AI calls do not block normal HTTP request handling

### Streaming Chat

- Context-aware AI assistant for uploaded logs
- Server-Sent Events streaming for real-time answer chunks
- JWT support through query parameter for SSE because browser `EventSource` cannot send custom headers

### Security And Rate Limiting

- Stateless JWT authentication
- User ownership checks for uploads, logs, analysis, chat, incidents, and profile data
- Per-user minute and daily AI usage limits
- Environment-based CORS configuration
- Public Swagger UI for API exploration

---

## Project Metrics

- 20+ REST API endpoints
- JWT-secured API architecture
- AI-powered incident analysis
- Real-time streaming responses using SSE
- Dockerized deployment on AWS
- Flyway-managed database migrations
- Production-style environment configuration

---

## Request Flow

1. User uploads a log file
2. Backend parses raw logs into structured entries
3. Parsed logs are stored in MySQL
4. AI analysis is triggered asynchronously
5. GPT generates root cause analysis and recommendations
6. Results are persisted and grouped into incidents
7. User can continue investigation through AI chat

---

## API Endpoints

All backend endpoints are served under the `/api` context path.

| Method | Path | Description |
| --- | --- | --- |
| POST | `/api/auth/register` | Register a new user |
| POST | `/api/auth/login` | Login and receive JWT |
| GET | `/api/auth/me` | Get current user profile |
| PATCH | `/api/auth/profile` | Update display name |
| POST | `/api/upload` | Upload a log file |
| GET | `/api/uploads` | List uploads |
| GET | `/api/uploads/{uploadId}` | Get upload status/details |
| DELETE | `/api/uploads/{uploadId}` | Delete an upload |
| GET | `/api/logs/{uploadId}` | Get parsed logs |
| POST | `/api/logs/search/{uploadId}` | Search parsed logs |
| GET | `/api/logs/{uploadId}/stats` | Get log statistics |
| POST | `/api/analysis/{uploadId}` | Trigger AI analysis |
| GET | `/api/analysis/{uploadId}` | Get analysis result |
| GET | `/api/analysis/{uploadId}/status` | Get analysis status |
| GET | `/api/analysis/history` | Get analysis history |
| POST | `/api/chat` | Non-streaming chat |
| GET | `/api/chat/stream` | SSE streaming chat |
| GET | `/api/rate-limit/status` | Get current usage limits |
| GET | `/api/incidents` | List grouped incidents |
| GET | `/api/actuator/health` | Health check |

Interactive API docs:

```text
http://35.154.51.190/api/swagger-ui/index.html
```

---

## Running Locally

### Prerequisites

- Java 21
- Maven 3.9+ or Maven Wrapper
- Docker
- OpenAI API key

### Option 1: Docker Compose

Create a local `.env` file from `.env.example`, then run:

```powershell
docker compose up -d --build
```

Verify:

```text
http://localhost:8080/api/actuator/health
```

### Option 2: Local MySQL + Maven

Create the database:

```sql
CREATE DATABASE log_analyzer;
```

Set environment variables:

```powershell
$env:DB_URL="jdbc:mysql://localhost:3306/log_analyzer?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"
$env:DB_USERNAME="root"
$env:DB_PASSWORD="your-db-password"
$env:JWT_SECRET="your-minimum-32-character-secret-key"
$env:JWT_EXPIRATION="86400000"
$env:OPENAI_API_KEY="your-openai-api-key"
$env:CORS_ALLOWED_ORIGINS="http://localhost:4200"
$env:STORAGE_BASE_PATH="uploads"
```

Run:

```powershell
.\mvnw.cmd spring-boot:run
```

Useful local URLs:

```text
Health:     http://localhost:8080/api/actuator/health
Swagger UI: http://localhost:8080/api/swagger-ui/index.html
```

---

## Running With Docker

Build the image:

```powershell
docker build -t logai-backend .
```

Create a Docker network:

```powershell
docker network create logai-net
```

Start MySQL:

```powershell
docker run -d `
  --name logai-mysql `
  --network logai-net `
  -e MYSQL_ROOT_PASSWORD=root `
  -e MYSQL_DATABASE=log_analyzer `
  mysql:8.0
```

Start backend:

```powershell
docker run -d `
  --name logai-backend `
  --network logai-net `
  -p 8080:8080 `
  -e DB_URL="jdbc:mysql://logai-mysql:3306/log_analyzer?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC" `
  -e DB_USERNAME=root `
  -e DB_PASSWORD=root `
  -e JWT_SECRET="your-minimum-32-character-secret-key" `
  -e JWT_EXPIRATION=86400000 `
  -e OPENAI_API_KEY="your-openai-api-key" `
  -e CORS_ALLOWED_ORIGINS="http://localhost:4200" `
  -e STORAGE_BASE_PATH="/app/uploads" `
  logai-backend
```

---

## Database Migrations

Flyway manages all schema migrations automatically on startup.

Migration files are stored in:

```text
src/main/resources/db/migration/
```

The application uses Hibernate `validate` mode, so schema changes are controlled by Flyway instead of automatic Hibernate DDL.

---

## Environment Variables

| Variable | Description | Required |
| --- | --- | --- |
| `DB_URL` | MySQL JDBC URL | Yes |
| `DB_USERNAME` | Database username | Yes |
| `DB_PASSWORD` | Database password | Yes |
| `JWT_SECRET` | 32+ character secret for JWT signing | Yes |
| `JWT_EXPIRATION` | JWT expiry in milliseconds | Yes |
| `OPENAI_API_KEY` | OpenAI API key | Yes |
| `CORS_ALLOWED_ORIGINS` | Allowed frontend origins | Yes |
| `STORAGE_BASE_PATH` | Local upload storage path | Yes |
| `FLYWAY_BASELINE_ON_MIGRATE` | Flyway baseline toggle | No |
| `ACTUATOR_HEALTH_DETAILS` | Health detail visibility | No |
| `APP_LOG_LEVEL` | Application log level | No |
| `SQL_LOG_LEVEL` | Hibernate SQL log level | No |

See `.env.example` for a complete template.

---

## Project Structure

```text
src/main/java/com/loganalyzer/
|-- config/          # Security, CORS, async, OpenAPI, startup validation
|-- controller/      # REST and SSE controllers
|-- service/         # Business logic, AI pipeline, rate limits, incidents
|-- repository/      # Spring Data JPA repositories
|-- model/           # JPA entities
|-- dto/             # Request and response DTOs
|-- security/        # JWT and user details services
|-- parser/          # Log parsing
`-- exception/       # Global exception handling

src/main/resources/
|-- db/migration/    # Flyway SQL migrations
`-- application.yml  # Environment-driven app configuration
```

---

## Key Design Decisions

**SSE + JWT:** Browser `EventSource` cannot send custom headers, so the streaming chat endpoint accepts the JWT as a URL query parameter. Normal REST endpoints use the `Authorization: Bearer <token>` header.

**Async AI Processing:** Analysis requests are queued and handled asynchronously so uploads and polling remain responsive while OpenAI calls run in the background.

**Flyway Over Hibernate DDL:** Hibernate is configured with `ddl-auto=validate`. Flyway is the source of truth for schema evolution.

**Centralized AI Persistence:** Analysis persistence is centralized so normalized root causes, severity scores, confidence scores, and incident grouping remain consistent.

**Single EC2 Deployment:** The live portfolio deployment uses one EC2 instance with Docker and Nginx to keep infrastructure simple and low cost.

**Swagger Security:** Swagger UI paths (`/swagger-ui/**`, `/swagger-ui.html`, `/v3/api-docs/**`) are explicitly whitelisted in Spring Security.

---

## Deployment

Current portfolio deployment:

```text
AWS EC2 t3.micro (Amazon Linux 2023, ap-south-1)
|-- Docker: logai-mysql (MySQL 8)
|-- Docker: logai-backend (Spring Boot)
|-- Nginx: reverse proxy + static frontend
`-- systemd: container auto-start on reboot
```

Deployment notes:

- The app is served through Nginx on port 80
- Backend runs on port 8080 behind `/api/*`
- Database name is `log_analyzer`
- Upload storage uses the local filesystem for the portfolio deployment
- For multi-instance production, uploaded files should move to S3 or EFS

---

## Cloud Architecture

- AWS EC2 t3.micro
- Docker containers
- Nginx reverse proxy
- MySQL 8 database
- OpenAI API integration
- Systemd auto-restart
- Flyway database migration management
- Spring Actuator + Micrometer metrics endpoints

---

## Build And Test

Run tests:

```powershell
.\mvnw.cmd test
```

Build package:

```powershell
.\mvnw.cmd package -DskipTests
```

Build Docker image:

```powershell
docker build -t logai-backend .
```

---

## Security Notes

- No real secrets are committed to this repository
- Runtime secrets are supplied through environment variables
- JWT authentication is stateless
- Users can access only their own uploads, logs, analyses, chat context, and profile data
- RDS/production-style deployments should never expose MySQL publicly
- OpenAI API keys should be restricted and protected with usage budgets

---

## Screenshots

### Dashboard

![Dashboard](docs/screenshots/dashboard.png)

### Uploads

![Uploads](docs/screenshots/uploads.png)

### AI Analysis

![AI Analysis](docs/screenshots/analysis.png)

### AI Chat

![AI Chat](docs/screenshots/chatbot.png)

### Rate Limits

![Rate Limits](docs/screenshots/ratelimits.png)

---

## Future Enhancements

- Redis caching layer
- Elasticsearch log indexing
- S3-based file storage
- Prometheus and Grafana monitoring dashboards
- Kubernetes deployment
- Multi-tenant support
- Role-based access control

---

Built by Rajdeep Reddy as a portfolio project demonstrating production-grade Spring Boot development, GenAI integration, Dockerized deployment, and AWS hosting.
