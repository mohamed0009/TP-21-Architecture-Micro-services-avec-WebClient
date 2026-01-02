package com.example.car.web;

import com.example.car.entities.Car;
import com.example.car.entities.Client;
import com.example.car.repositories.CarRepository;
import com.example.car.services.ClientService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cars")
public class CarController {

    private final CarRepository carRepository;
    private final ClientService clientService;

    public CarController(CarRepository carRepository, ClientService clientService) {
        this.carRepository = carRepository;
        this.clientService = clientService;
    }

    @GetMapping
    public List<Car> findAll() {
        List<Car> cars = carRepository.findAll();
        // Enrichir chaque voiture avec les données du client via WebClient
        cars.forEach(car -> {
            if (car.getClientId() != null) {
                Client client = clientService.findClientById(car.getClientId());
                car.setClient(client);
            }
        });
        return cars;
    }

    @GetMapping("/{id}")
    public Car findById(@PathVariable Long id) {
        Car car = carRepository.findById(id).orElse(null);
        if (car != null && car.getClientId() != null) {
            Client client = clientService.findClientById(car.getClientId());
            car.setClient(client);
        }
        return car;
    }

    @PostMapping
    public Car save(@RequestBody Car car) {
        return carRepository.save(car);
    }
}
