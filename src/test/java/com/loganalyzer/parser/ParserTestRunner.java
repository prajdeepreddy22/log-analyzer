package com.loganalyzer.parser;

import com.loganalyzer.service.HashKeyService;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class ParserTestRunner {

    public static void main(String[] args) throws Exception {

        String logs = """
                2024-01-15 10:23:45 ERROR [UserService] User not found
                
                2024-01-15 10:23:45.123 ERROR 12345 --- [main] com.app.UserService : NullPointerException
                \tat com.app.UserService.getUser(UserService.java:45)
                \tat com.app.Controller.handle(Controller.java:23)
                Caused by: java.lang.NullPointerException: User is null
                \tat com.app.UserRepository.find(UserRepository.java:12)
                
                Some random unstructured log line
                """;

        // ✅ FIX: correct package
        HashKeyService hashKeyService = new HashKeyService();

        LogParserService parser = new LogParserService(hashKeyService);

        List<ParsedLogEntry> result = parser.parse(
                new ByteArrayInputStream(logs.getBytes(StandardCharsets.UTF_8))
        );

        for (ParsedLogEntry entry : result) {
            System.out.println("--------------------------------------------------");
            System.out.println("Sequence      : " + entry.getLogSequence());
            System.out.println("Timestamp     : " + entry.getTimestamp());
            System.out.println("Level         : " + entry.getLevel());
            System.out.println("Service       : " + entry.getServiceName());
            System.out.println("HasStackTrace : " + entry.isHasStackTrace());
            System.out.println("HashKey       : " + entry.getHashKey());
            System.out.println("Message       : \n" + entry.getMessage());
        }
    }
}