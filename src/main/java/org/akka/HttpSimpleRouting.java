package org.akka;

import java.io.IOException;
import java.util.concurrent.CompletionStage;

import akka.NotUsed;
import akka.actor.ActorSystem;
import akka.http.javadsl.ConnectHttp;
import akka.http.javadsl.Http;
import akka.http.javadsl.ServerBinding;
import akka.http.javadsl.model.ContentTypes;
import akka.http.javadsl.model.HttpEntities;
import akka.http.javadsl.model.HttpRequest;
import akka.http.javadsl.model.HttpResponse;
import akka.http.javadsl.server.AllDirectives;
import akka.http.javadsl.server.Route;
import akka.stream.ActorMaterializer;
import akka.stream.javadsl.Flow;

//mvn exec:java -Dexec.mainClass="org.akka.HttpSimpleRouting" // -Dexec.args="arg0 arg1"
public class HttpSimpleRouting extends AllDirectives {

	public static void main(String[] args) throws IOException {
		
		ActorSystem system = ActorSystem.create("HttpServer");
		
		final HttpSimpleRouting app = new HttpSimpleRouting();
		
		final Http http = Http.get(system);
		final ActorMaterializer materializer = ActorMaterializer.create(system);
		
		final Flow<HttpRequest, HttpResponse, NotUsed> routeFlow = app.createRoute().flow(system, materializer);
		final CompletionStage<ServerBinding> binding = http.bindAndHandle(routeFlow, ConnectHttp.toHost("localhost", 8080), materializer);
		
		System.out.println("Type RETURN to exit");
	    System.in.read();
	    
	    binding
	    	.thenCompose(ServerBinding::unbind)
	    	.thenAccept(unbound -> system.terminate());
	}
	
	public Route createRoute() {
		
		Route helloRoute = parameterOptional("name", optName -> {
			String name = optName.orElse("Mister X");
			return complete("Hello " + name + "!");
		});
		
		// Only handle GET requests
		return get(() -> route(
			pathSingleSlash(() ->
				// return a constant string with a certain content type
				// http://localhost:8080
				complete(HttpEntities.create(ContentTypes.TEXT_HTML_UTF8, "<html><body>Hello world!</body></html>"))
			),
			path("ping", () ->
				// return a simple text/plain response
				// http://localhost:8080/ping
				complete("PONG!")
			),
			path("hello", () ->
				// use the route defined above
				// http://localhost:8080/hello?name=Sexy%20Beast
				helloRoute
			)
		));
	}
}
