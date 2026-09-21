package com.deepblue.rescue;

import com.deepblue.rescue.domain.*;
import com.deepblue.rescue.repository.AnimalRepository;
import com.deepblue.rescue.repository.ExpertiseRepository;
import com.deepblue.rescue.repository.RescueCaseRepository;
import com.deepblue.rescue.repository.RescueCenterRepository;
import com.deepblue.rescue.repository.SpecialistRepository;
import com.deepblue.rescue.repository.TreatmentRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import com.deepblue.rescue.domain.Animal;
import com.deepblue.rescue.domain.AnimalSex;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;

@Testcontainers
@SpringBootTest
@Transactional
class PersistenceIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer postgres =
            new PostgreSQLContainer("postgres:18-alpine")
                    .withDatabaseName("deepblue_test")
                    .withUsername("deepblue")
                    .withPassword("deepblue");

    @Autowired
    RescueCenterRepository rescueCenterRepository;

    @Autowired
    RescueCaseRepository rescueCaseRepository;

    @Autowired
    AnimalRepository animalRepository;

    @Autowired
    SpecialistRepository specialistRepository;

    @Autowired
    ExpertiseRepository expertiseRepository;

    @Autowired
    TreatmentRepository treatmentRepository;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Test
    void shouldHaveFlywayMigrationsApplied() {

        var versions = jdbcTemplate.queryForList(
                """
                
                        SELECT version 
                FROM flyway_schema_history
                WHERE version IN ('1', '2')
                ORDER BY version
                """,
                String.class
        );

        System.out.println("Migraciones encontradas: " + versions);

        assertThat(versions).contains("1", "2");
    }
    @Test
    void shouldPersistAndFindRescueCenter() {
        RescueCenter center = new RescueCenter(
                "DB-CAR",
                "DeepBlue Caribbean Center",
                "Santa Marta"
        );

        RescueCenter saved = rescueCenterRepository.save(center);

        assertThat(saved.getId()).isNotNull();

        var found = rescueCenterRepository.findById(saved.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getCode()).isEqualTo("DB-CAR");
        assertThat(found.get().getName())
                .isEqualTo("DeepBlue Caribbean Center");
        assertThat(found.get().getCity())
                .isEqualTo("Santa Marta");

        assertThat(rescueCenterRepository.existsById(saved.getId()))
                .isTrue();

        assertThat(rescueCenterRepository.count())
                .isEqualTo(1l);
    }
    @Test
    void shouldPersistRescueCasesBelongingToRescueCenter() {
        RescueCenter center = new RescueCenter(
                "DB-CAR",
                "DeepBlue Caribbean Center",
                "Santa Marta"
        );

        RescueCenter savedCenter = rescueCenterRepository.save(center);

        RescueCase case1 = new RescueCase(
                "CASE-001",
                LocalDate.of(2026, 1, 15),
                "Playa Blanca",
                RescueStatus.ADMITTED
        );

        RescueCase case2 = new RescueCase(
                "CASE-002",
                LocalDate.of(2026, 1, 20),
                "Taganga",
                RescueStatus.UNDER_EVALUATION
        );

        savedCenter.addCase(case1);
        savedCenter.addCase(case2);

        rescueCaseRepository.save(case1);
        rescueCaseRepository.save(case2);

        List<RescueCase> cases =
                rescueCaseRepository.findByRescueCenterCode("DB-CAR");

        assertThat(cases).hasSize(2);

        assertThat(cases)
                .extracting(RescueCase::getCaseCode)
                .containsExactlyInAnyOrder("CASE-001", "CASE-002");

        assertThat(case1.getRescueCenter())
                .isSameAs(savedCenter);

        assertThat(case2.getRescueCenter())
                .isSameAs(savedCenter);
    }
    @Test
    void shouldPersistAnimalLinkedToRescueCase() {
        RescueCenter center = new RescueCenter(
                "DB-CAR",
                "DeepBlue Caribbean Center",
                "Santa Marta"
        );
        RescueCenter savedCenter = rescueCenterRepository.save(center);

        RescueCase rescueCase = new RescueCase(
                "RES-2026-001",
                LocalDate.of(2026, 1, 25),
                "Playa Blanca",
                RescueStatus.ADMITTED
        );

        savedCenter.addCase(rescueCase);
        rescueCaseRepository.save(rescueCase);

        Animal animal = new Animal();
        animal.setAnimalCode("AN-2026-001");
        animal.setCommonName("Green Sea Turtle");
        animal.setScientificName("Chelonia mydas");
        animal.setSex(AnimalSex.FEMALE);
        rescueCase.assignAnimal(animal);

        Animal savedAnimal = animalRepository.save(animal);

        assertThat(savedAnimal.getId()).isNotNull();

        assertThat(savedAnimal.getRescueCase()).isNotNull();
        assertThat(savedAnimal.getRescueCase().getCaseCode())
                .isEqualTo("RES-2026-001");

        assertThat(rescueCase.getAnimal()).isSameAs(savedAnimal);
    }
    @Test
    void specialistShouldHaveTwoExpertiseAreas() {

        Expertise trauma = expertiseRepository
                .findByNameIgnoreCase("Trauma")
                .orElseThrow();

        Expertise rehabilitation = expertiseRepository
                .findByNameIgnoreCase("Rehabilitation")
                .orElseThrow();

        Specialist specialist = new Specialist();
        specialist.setProfessionalCode("SP-2026-001");
        specialist.setFirstName("Elena");
        specialist.setLastName("Vargas");
        specialist.setEmail("elena.vargas@deepblue.com");
        specialist.setActive(true);

        specialist.addExpertise(trauma);
        specialist.addExpertise(rehabilitation);

        Specialist savedSpecialist =
                specialistRepository.save(specialist);

        assertThat(savedSpecialist.getId()).isNotNull();

        assertThat(savedSpecialist.getExpertiseAreas())
                .hasSize(2);
    }
}