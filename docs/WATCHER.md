# LogAI Watcher

The watcher is a small local Java CLI that follows a log file and sends new lines to:

```text
POST /api/ingest/stream
```

It uses an ingestion API token, not a user JWT.

## Run Locally

Start the backend stack first:

```bash
docker compose up -d --build
```

Create an API token and log source from the app, then run:

```bash
mvn -DskipTests package
java -cp target/classes com.loganalyzer.watcher.LogAiWatcherCli \
  --file sample-logs/live-app.log \
  --backend-url http://localhost:8080 \
  --token <ingestion-api-token> \
  --source-id <log-source-id>
```

Optional flags:

```text
--state-file <path>    Defaults to .aeip-watcher-state.json
--batch-size <number>  Defaults to 100, max 500
--poll-ms <number>     Defaults to 500
```

The watcher starts from the end of the file the first time it runs. After a successful send, it stores the byte offset in `.aeip-watcher-state.json` and resumes from there on restart.

If ingestion fails, the offset is not advanced, so the same lines are retried later.
