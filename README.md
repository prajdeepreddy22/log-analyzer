# Log Analyzer Backend

Spring Boot backend for a portfolio log-analysis application with JWT
authentication, file ingestion, AI-assisted analysis, SSE chat, persistent
daily usage limits, and MySQL storage.

## Requirements

- Java 21
- MySQL 8
- Maven Wrapper or Maven
- OpenAI API key for AI features

## Local Setup

1. Create the MySQL database:

   ```sql
   CREATE DATABASE log_analyzer;
   ```

2. Set the environment variables listed in `.env.example`.

3. Run the application:

   ```powershell
   .\mvnw.cmd spring-boot:run
   ```

The local API base URL is `http://localhost:8080/api`.

## Required Production Variables

```text
DB_URL
DB_USERNAME
DB_PASSWORD
JWT_SECRET
OPENAI_API_KEY
CORS_ALLOWED_ORIGINS
```

Recommended production variables:

```text
PORT
STORAGE_BASE_PATH
APP_LOG_LEVEL
SQL_LOG_LEVEL
ACTUATOR_HEALTH_DETAILS
```

Never commit real values. Configure them in the hosting provider's secret or
environment-variable settings.

## Build and Test

```powershell
.\mvnw.cmd clean test
.\mvnw.cmd clean package -DskipTests
```

Run the packaged application:

```powershell
java -jar target\log-analyzer-0.0.1-SNAPSHOT.jar
```

## Deployment Checks

- Use Java 21.
- Set `CORS_ALLOWED_ORIGINS` to the deployed frontend origin.
- Verify `GET /api/actuator/health` returns a healthy response.
- Verify registration, login, upload, analysis, profile, and SSE chat flows.
- Use HTTPS URLs provided by the hosting platform.
- Confirm Flyway migrations complete during startup.

## Portfolio Deployment Notes

The default storage implementation writes uploads to the local filesystem.
This is suitable for a single-instance portfolio demo only when the hosting
provider offers persistent storage. On ephemeral filesystems, uploaded files
can disappear after a restart or redeployment.

The minute rate limit is maintained in application memory. The daily AI usage
limit is stored in MySQL. Run one backend instance for the portfolio demo.

Keep the AI allowance low and configure an OpenAI project budget to limit
unexpected usage.

## Railway Portfolio Deployment

Railway can deploy this repository directly using Railpack. The included
`railway.toml` configures the actuator health check and restart policy.

1. Create a Railway project and add its MySQL template.
2. Add this GitHub repository as a service.
3. Configure these backend service variables:

   ```text
   DB_URL=jdbc:mysql://${{MySQL.MYSQLHOST}}:${{MySQL.MYSQLPORT}}/${{MySQL.MYSQLDATABASE}}?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
   DB_USERNAME=${{MySQL.MYSQLUSER}}
   DB_PASSWORD=${{MySQL.MYSQLPASSWORD}}
   JWT_SECRET=<generate-a-random-secret-with-at-least-32-characters>
   OPENAI_API_KEY=<your-restricted-openai-project-key>
   CORS_ALLOWED_ORIGINS=<set-this-after-the-frontend-is-deployed>
   STORAGE_BASE_PATH=/tmp/log-analyzer-uploads
   APP_LOG_LEVEL=INFO
   SQL_LOG_LEVEL=WARN
   ACTUATOR_HEALTH_DETAILS=never
   ```

4. Generate a Railway domain for the backend service.
5. Verify:

   ```text
   https://<backend-domain>/api/actuator/health
   ```

Railway deployment filesystems are ephemeral. Using `/tmp` makes that
limitation explicit: uploaded source files can disappear after restart, while
ingested logs and analysis records remain in MySQL. This is acceptable for the
portfolio demo because files are processed immediately.
