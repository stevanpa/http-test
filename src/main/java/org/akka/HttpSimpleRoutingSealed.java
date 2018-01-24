package org.akka;

import java.io.IOException;
import java.util.concurrent.CompletionStage;

import akka.NotUsed;
import akka.actor.ActorSystem;
import akka.http.javadsl.ConnectHttp;
import akka.http.javadsl.Http;
import akka.http.javadsl.ServerBinding;
import akka.http.javadsl.model.HttpRequest;
import akka.http.javadsl.model.HttpResponse;
import akka.http.javadsl.model.headers.RawHeader;
import akka.http.javadsl.server.AllDirectives;
import akka.http.javadsl.server.Route;
import akka.stream.ActorMaterializer;
import akka.stream.javadsl.Flow;

//mvn exec:java -Dexec.mainClass="org.akka.HttpSimpleRoutingSealed" // -Dexec.args="arg0 arg1"
public class HttpSimpleRoutingSealed extends AllDirectives {

	public static void main(String[] args) throws IOException {
		
		final HttpSimpleRoutingSealed app = new HttpSimpleRoutingSealed();
		app.runServer();
	}

	public void runServer() throws IOException {
		
		ActorSystem system = ActorSystem.create("HttpServer");
		final ActorMaterializer materializer = ActorMaterializer.create(system);
		
		Route sealedRoute = get(() ->
			pathSingleSlash(() ->
				complete("Captain on the bridge!")
			)
		).seal(system,  materializer);
		
		Route route = respondWithHeader(
			RawHeader.create("special-header", "you always have this even in 404"),
			() -> sealedRoute
		);
		
		
		final Http http = Http.get(system);
		final Flow<HttpRequest, HttpResponse, NotUsed> routeFlow = route.flow(system, materializer);
		final CompletionStage<ServerBinding> binding = http.bindAndHandle(routeFlow, ConnectHttp.toHost("localhost", 8080), materializer);
		System.out.println("Type RETURN to exit");
	    System.in.read();
	    
	    binding
	    	.thenCompose(ServerBinding::unbind)
	    	.thenAccept(unbound -> system.terminate());
	}
}
