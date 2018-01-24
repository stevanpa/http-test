package org.akka;

import java.util.Map;

import org.akka.domain.Pet;

import akka.http.javadsl.model.StatusCodes;
import static akka.http.javadsl.server.Directives.*;
import akka.http.javadsl.server.Route;

public class HttpPetStoreController {
	
	private Map<Integer, Pet> dataStore;
	
	public HttpPetStoreController(Map<Integer, Pet> dataStore) {
		this.dataStore = dataStore;
	}
	
	public Route deletePet(int petId) {
		dataStore.remove(petId);
		return complete(StatusCodes.OK);
	}

}
