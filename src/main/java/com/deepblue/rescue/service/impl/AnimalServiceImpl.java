package com.deepblue.rescue.service.impl;

import com.deepblue.rescue.domain.Animal;
import com.deepblue.rescue.domain.RescueStatus;
import com.deepblue.rescue.dto.response.AnimalResponse;
import com.deepblue.rescue.exception.ResourceNotFoundException;
import com.deepblue.rescue.repository.AnimalRepository;
import com.deepblue.rescue.service.AnimalService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AnimalServiceImpl implements AnimalService {

    private final AnimalRepository animalRepository;

    public AnimalServiceImpl(AnimalRepository animalRepository) {
        this.animalRepository = animalRepository;
    }

    @Override
    public AnimalResponse findByCode(String animalCode) {
        Animal animal = animalRepository.findByAnimalCode(animalCode)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Animal not found with code: " + animalCode
                ));

        return toResponse(animal);
    }

    @Override
    public List<AnimalResponse> findAnimalsInRehabilitation() {
        return animalRepository
                .findByRescueCaseStatus(RescueStatus.IN_REHABILITATION)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public boolean canReceiveTreatment(String animalCode) {
        throw new UnsupportedOperationException(
                "Treatment eligibility rule has not been defined yet"
        );
    }

    private AnimalResponse toResponse(Animal animal) {
        return new AnimalResponse(
                animal.getId(),
                animal.getAnimalCode(),
                animal.getCommonName(),
                animal.getScientificName(),
                animal.getSex(),
                animal.getRescueCase().getStatus()
        );
    }
}
