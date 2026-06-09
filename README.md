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

## Docker

Build the production image:

```powershell
docker build -t log-analyzer-backend .
```

Run it with environment variables supplied from a local ignored `.env` file:

```powershell
docker run --rm --env-file .env -p 8080:8080 log-analyzer-backend
```

Verify:

```text
http://localhost:8080/api/actuator/health
```

## AWS Portfolio Deployment

The recommended portfolio architecture is:

```text
Frontend hosting -> Elastic Beanstalk Docker backend -> Amazon RDS MySQL
```

Elastic Beanstalk builds the root `Dockerfile`, runs the container on EC2, and
uses the health-check configuration under `.ebextensions`.

1. Create an Amazon RDS MySQL 8 database.
2. Ensure the RDS security group allows MySQL port `3306` only from the
   Elastic Beanstalk instance security group.
3. Create an Elastic Beanstalk web-server environment using the Docker
   platform on Amazon Linux 2023.
4. Deploy this repository with the EB CLI or upload a source bundle containing
   the repository files.
5. Configure these Elastic Beanstalk environment properties:

   ```text
   DB_URL=jdbc:mysql://<rds-endpoint>:3306/log_analyzer?sslMode=REQUIRED&serverTimezone=UTC
   DB_USERNAME=<rds-username>
   DB_PASSWORD=<rds-password>
   JWT_SECRET=<random-secret-with-at-least-32-characters>
   OPENAI_API_KEY=<restricted-openai-project-key>
   CORS_ALLOWED_ORIGINS=<deployed-frontend-https-origin>
   STORAGE_BASE_PATH=/app/uploads
   APP_LOG_LEVEL=INFO
   SQL_LOG_LEVEL=WARN
   ACTUATOR_HEALTH_DETAILS=never
   ```

6. Verify the backend:

   ```text
   http://<elastic-beanstalk-domain>/api/actuator/health
   ```

7. For HTTPS, attach an ACM certificate to the Elastic Beanstalk Application
   Load Balancer and redirect HTTP to HTTPS.

Do not make the RDS database publicly accessible. Do not place production
secrets in the Docker image, `Dockerfile`, source bundle, or GitHub.

The container filesystem is not durable across replacement instances. Uploaded
source files can disappear after redeployment, while ingested logs and analysis
records remain in RDS. This is acceptable for this single-instance portfolio
demo because files are processed immediately. Use Amazon S3 if durable uploaded
files become a requirement.
