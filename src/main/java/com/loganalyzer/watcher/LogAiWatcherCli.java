package com.loganalyzer.watcher;

public class LogAiWatcherCli {

    public static void main(String[] args) throws Exception {
        if (args.length == 0 || hasHelpArg(args)) {
            printUsage();
            return;
        }

        WatcherConfig config = WatcherConfig.fromArgs(args);

        new LogFileWatcher(
                config,
                new LogTailReader(),
                new LiveIngestionClient()
        ).run();
    }

    private static boolean hasHelpArg(String[] args) {
        for (String arg : args) {
            if ("--help".equals(arg) || "-h".equals(arg)) {
                return true;
            }
        }
        return false;
    }

    private static void printUsage() {
        System.out.println("""
                LogAI watcher

                Required:
                  --file <path>           Log file to follow
                  --backend-url <url>     Backend URL, for example http://localhost:8080
                  --token <api-token>     Ingestion API token
                  --source-id <id>        Log source id created in LogAI

                Optional:
                  --state-file <path>     Defaults to .aeip-watcher-state.json
                  --batch-size <number>   Defaults to 100, max 500
                  --poll-ms <number>      Defaults to 500
                """);
    }
}
