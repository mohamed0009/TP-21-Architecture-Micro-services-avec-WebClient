package com.example.car.services;

import com.example.car.entities.Client;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
public class ClientService {

    private final WebClient.Builder webClientBuilder;

    public ClientService(WebClient.Builder webClientBuilder) {
        this.webClientBuilder = webClientBuilder;
    }

    public Client findClientById(Long id) {
        return webClientBuilder.build()
                .get()
                .uri("http://SERVICE-CLIENT/api/clients/" + id)
                .retrieve()
                .bodyToMono(Client.class)
                .block();
    }
}
