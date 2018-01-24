package org.akka;

import java.util.concurrent.CompletionStage;

import akka.actor.ActorSystem;
import akka.http.javadsl.ConnectHttp;
import akka.http.javadsl.Http;
import akka.http.javadsl.IncomingConnection;
import akka.http.javadsl.ServerBinding;
import akka.http.javadsl.server.AllDirectives;
import akka.stream.ActorMaterializer;
import akka.stream.javadsl.Source;
import akka.stream.javadsl.Sink;

//mvn exec:java -Dexec.mainClass="org.akka.HttpBase" // -Dexec.args="arg0 arg1"
public class HttpBase extends AllDirectives {

	public static void main(String[] args) throws Exception {
		
		ActorSystem system = ActorSystem.create("HttpServer");
		ActorMaterializer materializer = ActorMaterializer.create(system);
		
		Http http = Http.get(system);
		Source<IncomingConnection, CompletionStage<ServerBinding>> serverSource = http.bind(ConnectHttp.toHost("localhost", 8080), materializer);
		
		CompletionStage<ServerBinding> serverBindingFuture = serverSource.to(Sink.foreach(connection -> {
			System.out.println("Accepted new connection from " + connection.remoteAddress());
		      // ... and then actually handle the connection
		}))
		.run(materializer);
		
		
		//--- Start up Info
		System.out.println("Server online at http://localhost:8080/\nPress RETURN to stop...");
	    System.in.read(); // let it run until user presses return
	    
	  //--- Termination stage
	    serverBindingFuture
        	.thenCompose(ServerBinding::unbind) // trigger unbinding from the port
        	.thenAccept(unbound -> system.terminate()); // and shutdown when done
	}
}
