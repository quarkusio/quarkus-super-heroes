package io.quarkus.sample.superheroes.statistics.endpoint;

import java.net.URI;
import java.util.Deque;
import java.util.concurrent.LinkedBlockingDeque;

import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
class TeamStatsWebSocketTests {
	private static final Deque<String> MESSAGES = new LinkedBlockingDeque<>();

	@TestHTTPResource("/stats/team")
	URI uri;

}
