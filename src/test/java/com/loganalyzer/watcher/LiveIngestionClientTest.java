package com.loganalyzer.watcher;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LiveIngestionClientTest {

    @Test
    void resolvesIngestionEndpointWithOrWithoutApiContextPath() {

        LiveIngestionClient client = new LiveIngestionClient();

        assertThat(client.resolveIngestionUri("http://localhost:8080").toString())
                .isEqualTo("http://localhost:8080/api/ingest/stream");

        assertThat(client.resolveIngestionUri("http://localhost:8080/api").toString())
                .isEqualTo("http://localhost:8080/api/ingest/stream");
    }
}
