package org.akka;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import org.akka.domain.Pet;

import akka.NotUsed;
import akka.actor.ActorSystem;
import akka.http.javadsl.ConnectHttp;
import akka.http.javadsl.Http;
import akka.http.javadsl.ServerBinding;
import akka.http.javadsl.marshallers.jackson.Jackson;
import akka.http.javadsl.model.HttpRequest;
import akka.http.javadsl.model.HttpResponse;
import akka.http.javadsl.model.StatusCodes;
import static akka.http.javadsl.server.Directives.*;
import akka.http.javadsl.server.Route;
import akka.stream.ActorMaterializer;
import akka.stream.javadsl.Flow;

import static akka.http.javadsl.unmarshalling.StringUnmarshallers.INTEGER;

// mvn exec:java -Dexec.mainClass="org.akka.HttpPetStore" // -Dexec.args="arg0 arg1"
public class HttpPetStore {
	
	private static Route putPetHandler(Map<Integer, Pet> pets, Pet thePet) {
		pets.put(thePet.getId(), thePet);
		return complete(StatusCodes.OK, thePet, Jackson.<Pet>marshaller());
	}
	
	private static Route alternativeFuturePutPetHandler(Map<Integer, Pet> pets, Pet thePet) {
		pets.put(thePet.getId(), thePet);
		CompletableFuture<Pet> futurePet = CompletableFuture.supplyAsync(() -> thePet);
		return completeOKWithFuture(futurePet, Jackson.<Pet>marshaller());
	}
	
	public static Route appRoute(final Map<Integer, Pet> pets) {
		HttpPetStoreController controller = new HttpPetStoreController(pets);
		
		// Defined as Function in order to refer to [pets], but this could also be an ordinary method.
		Function<Integer, Route> existingPet = petId -> {
			Pet pet = pets.get(petId);
			return (pet == null) ? reject() : complete(StatusCodes.OK, pet, Jackson.<Pet>marshaller());
		};
		
		return route(
			path("", () ->
				getFromResource("web/index.html")
			),
			pathPrefix("pet", () ->
				path(INTEGER, petId -> route(
					// demonstrate different ways of handling requests:
					// 1. Function
					get(() -> existingPet.apply(petId)),
					// 2. Method
					put(() -> entity(Jackson.unmarshaller(Pet.class), thePet ->
						putPetHandler(pets, thePet)
					)),
					// 2.1. using a method, and internally handling a Future value
					path("alternate", () ->
						put(() -> entity(Jackson.unmarshaller(Pet.class), thePet ->
							alternativeFuturePutPetHandler(pets, thePet)
						))
					),
					// 3. Method of Controller instance
					delete(() -> controller.deletePet(petId))
				))
			)
		);
	}
	
	public static void main(String[] args) throws IOException {
		
		Map<Integer, Pet> pets = new ConcurrentHashMap<>();
		Pet dog = new Pet(0, "dog");
		Pet cat = new Pet(1, "cat");
		pets.put(0, dog);
		pets.put(1, cat);
		
		final ActorSystem system = ActorSystem.create("HttpServer");
		final ActorMaterializer materializer = ActorMaterializer.create(system);
		final Http http = Http.get(system);
		final Flow<HttpRequest, HttpResponse, NotUsed> routeFlow = appRoute(pets).flow(system, materializer);
		final CompletionStage<ServerBinding> binding = http.bindAndHandle(routeFlow, ConnectHttp.toHost("localhost", 8080), materializer);
		
		System.out.println("Type RETURN to exit");
	    System.in.read();
	    
	    binding
	    	.thenCompose(ServerBinding::unbind)
	    	.thenAccept(unbound -> system.terminate());
	}
}
