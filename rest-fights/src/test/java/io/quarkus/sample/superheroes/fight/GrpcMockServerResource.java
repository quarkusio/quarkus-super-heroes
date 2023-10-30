package io.quarkus.sample.superheroes.fight;

import java.util.Map;

import org.grpcmock.GrpcMock;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager.TestInjector.AnnotatedAndMatchesType;

public class GrpcMockServerResource implements QuarkusTestResourceLifecycleManager {
	private final GrpcMock server = GrpcMock.grpcMock().build();

	@Override
	public Map<String, String> start() {
		this.server.start();
		GrpcMock.configureFor(this.server);

		return Map.of(
				"quarkus.grpc.clients.locations.host", "localhost",
        "quarkus.grpc.clients.locations.port", String.valueOf(this.server.getPort()),
		    "quarkus.grpc.clients.locations.test-port", String.valueOf(this.server.getPort())
	    );
	}

	@Override
	public void stop() {
		this.server.stop();
	}

	@Override
  public void inject(TestInjector testInjector) {
		testInjector.injectIntoFields(
      this.server,
      new AnnotatedAndMatchesType(InjectGrpcMock.class, GrpcMock.class)
    );
	}
}
