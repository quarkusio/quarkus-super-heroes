package io.quarkus.sample.superheroes.location.grpc

import io.grpc.CallOptions
import io.grpc.Channel
import io.grpc.ClientCall
import io.grpc.ClientInterceptor
import io.grpc.MethodDescriptor
import java.util.concurrent.TimeUnit
import jakarta.enterprise.context.ApplicationScoped
import io.quarkus.grpc.GlobalInterceptor

@GlobalInterceptor
@ApplicationScoped
class DeadlineInterceptor : ClientInterceptor {
	override fun <ReqT : Any?, RespT : Any?> interceptCall(method: MethodDescriptor<ReqT, RespT>?, callOptions: CallOptions?, next: Channel?): ClientCall<ReqT, RespT> =
		next?.newCall(method, callOptions?.withDeadlineAfter(10, TimeUnit.SECONDS))!!
}