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
import java.math.BigDecimal;
import org.springframework.dao.DataIntegrityViolationException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;
import java.time.LocalDate;
import java.time.LocalDateTime;
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

    @Test
    void shouldPersistAnimalWithMedicalRecord() {
        RescueCenter center = new RescueCenter(
                "DB-CAR",
                "DeepBlue Caribbean Center",
                "Santa Marta"
        );
        RescueCenter savedCenter = rescueCenterRepository.save(center);

        RescueCase rescueCase = new RescueCase(
                "RES-2026-002",
                LocalDate.of(2026, 1, 20),
                "Taganga",
                RescueStatus.ADMITTED
        );

        savedCenter.addCase(rescueCase);
        rescueCaseRepository.save(rescueCase);

        Animal animal = new Animal();
        animal.setAnimalCode("AN-2026-002");
        animal.setCommonName("Green Sea Turtle");
        animal.setScientificName("Chelonia mydas");
        animal.setSex(AnimalSex.FEMALE);

        rescueCase.assignAnimal(animal);

        MedicalRecord medicalRecord = new MedicalRecord();
        medicalRecord.setInitialWeight(new BigDecimal("28.40"));
        medicalRecord.setInitialCondition("STABLE");
        medicalRecord.setInjuries("Left front flipper injury");

        animal.assignMedicalRecord(medicalRecord);

        Animal savedAnimal = animalRepository.save(animal);

        assertThat(savedAnimal.getId()).isNotNull();
        assertThat(savedAnimal.getMedicalRecord()).isNotNull();
        assertThat(savedAnimal.getMedicalRecord().getId()).isNotNull();

        assertThat(savedAnimal.getMedicalRecord().getInitialWeight())
                .isEqualByComparingTo("28.40");

        assertThat(savedAnimal.getMedicalRecord().getInitialCondition())
                .isEqualTo("STABLE");

        assertThat(savedAnimal.getMedicalRecord().getInjuries())
                .isEqualTo("Left front flipper injury");

        assertThat(savedAnimal.getMedicalRecord().getAnimal())
                .isSameAs(savedAnimal);
    }

    @Test
    void shouldFindRescueCasesByStatus() {
        RescueCenter center = new RescueCenter(
                "DB-CAR",
                "DeepBlue Caribbean Center",
                "Santa Marta"
        );
        RescueCenter savedCenter = rescueCenterRepository.save(center);

        RescueCase case1 = new RescueCase(
                "RES-001",
                LocalDate.of(2026, 2, 1),
                "Playa Blanca",
                RescueStatus.IN_REHABILITATION
        );

        RescueCase case2 = new RescueCase(
                "RES-002",
                LocalDate.of(2026, 2, 2),
                "Taganga",
                RescueStatus.READY_FOR_RELEASE
        );

        RescueCase case3 = new RescueCase(
                "RES-003",
                LocalDate.of(2026, 2, 3),
                "Bahía Concha",
                RescueStatus.IN_REHABILITATION
        );

        savedCenter.addCase(case1);
        savedCenter.addCase(case2);
        savedCenter.addCase(case3);

        rescueCaseRepository.save(case1);
        rescueCaseRepository.save(case2);
        rescueCaseRepository.save(case3);

        List<RescueCase> results =
                rescueCaseRepository.findByStatusOrderByRescueDateAsc(
                        RescueStatus.IN_REHABILITATION
                );

        assertThat(results).hasSize(2);

        assertThat(results)
                .extracting(RescueCase::getCaseCode)
                .containsExactly("RES-001", "RES-003");
    }

    @Test
    void shouldFindAnimalsByRescueCenterCode() {
        RescueCenter caribbean = new RescueCenter(
                "DB-CAR",
                "DeepBlue Caribbean Center",
                "Santa Marta"
        );

        RescueCenter pacific = new RescueCenter(
                "DB-PAC",
                "DeepBlue Pacific Center",
                "Buenaventura"
        );

        rescueCenterRepository.save(caribbean);
        rescueCenterRepository.save(pacific);

        RescueCase carCase = new RescueCase(
                "RES-CAR-001",
                LocalDate.of(2026, 3, 1),
                "Playa Blanca",
                RescueStatus.ADMITTED
        );

        RescueCase pacCase = new RescueCase(
                "RES-PAC-001",
                LocalDate.of(2026, 3, 2),
                "Bahía Málaga",
                RescueStatus.ADMITTED
        );

        caribbean.addCase(carCase);
        pacific.addCase(pacCase);

        rescueCaseRepository.save(carCase);
        rescueCaseRepository.save(pacCase);

        Animal carAnimal = new Animal();
        carAnimal.setAnimalCode("AN-CAR-001");
        carAnimal.setCommonName("Green Sea Turtle");
        carAnimal.setScientificName("Chelonia mydas");
        carAnimal.setSex(AnimalSex.FEMALE);
        carCase.assignAnimal(carAnimal);

        Animal pacAnimal = new Animal();
        pacAnimal.setAnimalCode("AN-PAC-001");
        pacAnimal.setCommonName("Hawksbill Turtle");
        pacAnimal.setScientificName("Eretmochelys imbricata");
        pacAnimal.setSex(AnimalSex.FEMALE);
        pacCase.assignAnimal(pacAnimal);

        animalRepository.save(carAnimal);
        animalRepository.save(pacAnimal);

        List<Animal> results =
                animalRepository.findByRescueCaseRescueCenterCode("DB-CAR");

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getAnimalCode())
                .isEqualTo("AN-CAR-001");
    }

    @Test
    void shouldFindActiveSpecialistsByExpertise() {
        Expertise trauma = expertiseRepository
                .findByNameIgnoreCase("Trauma")
                .orElseThrow();

        Expertise rehabilitation = expertiseRepository
                .findByNameIgnoreCase("Rehabilitation")
                .orElseThrow();

        Expertise marineMammals = expertiseRepository
                .findByNameIgnoreCase("Marine Mammals")
                .orElseThrow();

        Expertise marineBirds = expertiseRepository
                .findByNameIgnoreCase("Marine Birds")
                .orElseThrow();

        Specialist elena = new Specialist();
        elena.setProfessionalCode("SPEC-001");
        elena.setFirstName("Elena");
        elena.setLastName("Vargas");
        elena.setEmail("elena@deepblue.org");
        elena.setActive(true);
        elena.addExpertise(trauma);
        elena.addExpertise(rehabilitation);

        Specialist mateo = new Specialist();
        mateo.setProfessionalCode("SPEC-002");
        mateo.setFirstName("Mateo");
        mateo.setLastName("Gómez");
        mateo.setEmail("mateo@deepblue.org");
        mateo.setActive(true);
        mateo.addExpertise(marineMammals);
        mateo.addExpertise(rehabilitation);

        Specialist sofia = new Specialist();
        sofia.setProfessionalCode("SPEC-003");
        sofia.setFirstName("Sofia");
        sofia.setLastName("Ramírez");
        sofia.setEmail("sofia@deepblue.org");
        sofia.setActive(true);
        sofia.addExpertise(marineBirds);
        sofia.addExpertise(trauma);

        specialistRepository.save(elena);
        specialistRepository.save(mateo);
        specialistRepository.save(sofia);

        List<Specialist> results =
                specialistRepository.findActiveByExpertise("Trauma");

        assertThat(results)
                .extracting(Specialist::getFirstName)
                .containsExactlyInAnyOrder("Elena", "Sofia");
    }

    @Test
    void shouldPersistTreatmentsForAnimal() {
        RescueCenter center = new RescueCenter(
                "DB-CAR",
                "DeepBlue Caribbean Center",
                "Santa Marta"
        );
        RescueCenter savedCenter = rescueCenterRepository.save(center);

        RescueCase rescueCase = new RescueCase(
                "RES-TREAT-001",
                LocalDate.of(2026, 4, 1),
                "Playa Blanca",
                RescueStatus.IN_REHABILITATION
        );
        savedCenter.addCase(rescueCase);
        rescueCaseRepository.save(rescueCase);

        Animal animal = new Animal();
        animal.setAnimalCode("AN-TREAT-001");
        animal.setCommonName("Green Sea Turtle");
        animal.setScientificName("Chelonia mydas");
        animal.setSex(AnimalSex.FEMALE);
        rescueCase.assignAnimal(animal);
        Animal savedAnimal = animalRepository.save(animal);

        Specialist elena = new Specialist();
        elena.setProfessionalCode("SPEC-TREAT-001");
        elena.setFirstName("Elena");
        elena.setLastName("Vargas");
        elena.setEmail("elena.treat@deepblue.org");
        elena.setActive(true);

        Specialist mateo = new Specialist();
        mateo.setProfessionalCode("SPEC-TREAT-002");
        mateo.setFirstName("Mateo");
        mateo.setLastName("Gomez");
        mateo.setEmail("mateo.treat@deepblue.org");
        mateo.setActive(true);

        specialistRepository.save(elena);
        specialistRepository.save(mateo);

        Treatment treatment1 = new Treatment();
        treatment1.setAnimal(savedAnimal);
        treatment1.setSpecialist(elena);
        treatment1.setPerformedAt(LocalDateTime.of(2026, 4, 1, 10, 0));
        treatment1.setType(TreatmentType.WOUND_CARE);
        treatment1.setDescription("Cleaning of left front flipper wound");

        Treatment treatment2 = new Treatment();
        treatment2.setAnimal(savedAnimal);
        treatment2.setSpecialist(elena);
        treatment2.setPerformedAt(LocalDateTime.of(2026, 4, 2, 10, 0));
        treatment2.setType(TreatmentType.HYDRATION);
        treatment2.setDescription("Subcutaneous fluid therapy");

        Treatment treatment3 = new Treatment();
        treatment3.setAnimal(savedAnimal);
        treatment3.setSpecialist(mateo);
        treatment3.setPerformedAt(LocalDateTime.of(2026, 4, 3, 10, 0));
        treatment3.setType(TreatmentType.OBSERVATION);
        treatment3.setDescription("General observation");

        Treatment saved1 = treatmentRepository.save(treatment1);
        Treatment saved2 = treatmentRepository.save(treatment2);
        Treatment saved3 = treatmentRepository.save(treatment3);

        assertThat(saved1.getId()).isNotNull();
        assertThat(saved2.getId()).isNotNull();
        assertThat(saved3.getId()).isNotNull();
    }

    @Test
    void shouldFindTreatmentsByAnimalChronologically() {
        RescueCenter center = new RescueCenter(
                "DB-CAR",
                "DeepBlue Caribbean Center",
                "Santa Marta"
        );
        RescueCenter savedCenter = rescueCenterRepository.save(center);

        RescueCase rescueCase = new RescueCase(
                "RES-CHRON-001",
                LocalDate.of(2026, 5, 1),
                "Playa Blanca",
                RescueStatus.IN_REHABILITATION
        );
        savedCenter.addCase(rescueCase);
        rescueCaseRepository.save(rescueCase);

        Animal animal = new Animal();
        animal.setAnimalCode("AN-CHRON-001");
        animal.setCommonName("Green Sea Turtle");
        animal.setScientificName("Chelonia mydas");
        animal.setSex(AnimalSex.FEMALE);
        rescueCase.assignAnimal(animal);
        Animal savedAnimal = animalRepository.save(animal);

        Specialist specialist = new Specialist();
        specialist.setProfessionalCode("SPEC-CHRON-001");
        specialist.setFirstName("Elena");
        specialist.setLastName("Vargas");
        specialist.setEmail("elena.chron@deepblue.org");
        specialist.setActive(true);
        Specialist savedSpecialist = specialistRepository.save(specialist);

        Treatment treatment1 = new Treatment();
        treatment1.setAnimal(savedAnimal);
        treatment1.setSpecialist(savedSpecialist);
        treatment1.setPerformedAt(LocalDateTime.of(2026, 5, 3, 10, 0));
        treatment1.setType(TreatmentType.HYDRATION);

        Treatment treatment2 = new Treatment();
        treatment2.setAnimal(savedAnimal);
        treatment2.setSpecialist(savedSpecialist);
        treatment2.setPerformedAt(LocalDateTime.of(2026, 5, 1, 10, 0));
        treatment2.setType(TreatmentType.WOUND_CARE);

        Treatment treatment3 = new Treatment();
        treatment3.setAnimal(savedAnimal);
        treatment3.setSpecialist(savedSpecialist);
        treatment3.setPerformedAt(LocalDateTime.of(2026, 5, 2, 10, 0));
        treatment3.setType(TreatmentType.OBSERVATION);

        treatmentRepository.save(treatment1);
        treatmentRepository.save(treatment2);
        treatmentRepository.save(treatment3);

        List<Treatment> results =
                treatmentRepository.findByAnimalIdOrderByPerformedAtAsc(
                        savedAnimal.getId()
                );

        assertThat(results).hasSize(3);

        assertThat(results)
                .extracting(Treatment::getType)
                .containsExactly(
                        TreatmentType.WOUND_CARE,
                        TreatmentType.OBSERVATION,
                        TreatmentType.HYDRATION
                );
    }

    @Test
    void shouldFindTreatmentsBetweenDates() {
        RescueCenter center = new RescueCenter(
                "DB-CAR",
                "DeepBlue Caribbean Center",
                "Santa Marta"
        );
        RescueCenter savedCenter = rescueCenterRepository.save(center);

        RescueCase rescueCase = new RescueCase(
                "RES-INTERVAL-001",
                LocalDate.of(2026, 8, 1),
                "Playa Blanca",
                RescueStatus.IN_REHABILITATION
        );
        savedCenter.addCase(rescueCase);
        rescueCaseRepository.save(rescueCase);

        Animal animal = new Animal();
        animal.setAnimalCode("AN-INTERVAL-001");
        animal.setCommonName("Green Sea Turtle");
        animal.setScientificName("Chelonia mydas");
        animal.setSex(AnimalSex.FEMALE);
        rescueCase.assignAnimal(animal);
        Animal savedAnimal = animalRepository.save(animal);

        Specialist specialist = new Specialist();
        specialist.setProfessionalCode("SPEC-INTERVAL-001");
        specialist.setFirstName("Elena");
        specialist.setLastName("Vargas");
        specialist.setEmail("elena.interval@deepblue.org");
        specialist.setActive(true);
        Specialist savedSpecialist = specialistRepository.save(specialist);

        Treatment treatment1 = new Treatment();
        treatment1.setAnimal(savedAnimal);
        treatment1.setSpecialist(savedSpecialist);
        treatment1.setPerformedAt(
                LocalDateTime.of(2026, 8, 1, 10, 0)
        );
        treatment1.setType(TreatmentType.WOUND_CARE);

        Treatment treatment2 = new Treatment();
        treatment2.setAnimal(savedAnimal);
        treatment2.setSpecialist(savedSpecialist);
        treatment2.setPerformedAt(
                LocalDateTime.of(2026, 8, 10, 10, 0)
        );
        treatment2.setType(TreatmentType.HYDRATION);

        Treatment treatment3 = new Treatment();
        treatment3.setAnimal(savedAnimal);
        treatment3.setSpecialist(savedSpecialist);
        treatment3.setPerformedAt(
                LocalDateTime.of(2026, 8, 20, 10, 0)
        );
        treatment3.setType(TreatmentType.OBSERVATION);

        treatmentRepository.save(treatment1);
        treatmentRepository.save(treatment2);
        treatmentRepository.save(treatment3);

        List<Treatment> results =
                treatmentRepository.findTreatmentsBetween(
                        LocalDateTime.of(2026, 8, 5, 0, 0),
                        LocalDateTime.of(2026, 8, 15, 23, 59)
                );

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getPerformedAt())
                .isEqualTo(LocalDateTime.of(2026, 8, 10, 10, 0));
        assertThat(results.get(0).getType())
                .isEqualTo(TreatmentType.HYDRATION);
    }

    @Test
    void shouldRejectDuplicateAnimalCode() {
        RescueCenter center = new RescueCenter(
                "DB-CAR",
                "DeepBlue Caribbean Center",
                "Santa Marta"
        );
        RescueCenter savedCenter = rescueCenterRepository.save(center);

        RescueCase rescueCase = new RescueCase(
                "RES-UNIQUE-001",
                LocalDate.of(2026, 6, 1),
                "Playa Blanca",
                RescueStatus.ADMITTED
        );
        savedCenter.addCase(rescueCase);
        RescueCase savedCase = rescueCaseRepository.save(rescueCase);

        Animal firstAnimal = new Animal();
        firstAnimal.setAnimalCode("AN-100");
        firstAnimal.setCommonName("Green Sea Turtle");
        firstAnimal.setScientificName("Chelonia mydas");
        firstAnimal.setSex(AnimalSex.FEMALE);
        savedCase.assignAnimal(firstAnimal);

        animalRepository.saveAndFlush(firstAnimal);

        Animal secondAnimal = new Animal();
        secondAnimal.setAnimalCode("AN-100");
        secondAnimal.setCommonName("Hawksbill Turtle");
        secondAnimal.setScientificName("Eretmochelys imbricata");
        secondAnimal.setSex(AnimalSex.FEMALE);

        assertThatThrownBy(() -> animalRepository.saveAndFlush(secondAnimal))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void shouldRejectInvalidForeignKey() {
        assertThatThrownBy(() ->
                jdbcTemplate.update(
                        """
                                INSERT INTO animals (
                                    animal_code,
                                    common_name,
                                    scientific_name,
                                    sex,
                                    rescue_case_id
                                )
                                VALUES (?, ?, ?, ?, ?)
                                """,
                        "AN-FK-001",
                        "Green Sea Turtle",
                        "Chelonia mydas",
                        "FEMALE",
                        999999L
                )
        ).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void shouldPersistIntegratorScenario() {
        RescueCenter center = rescueCenterRepository.save(
                new RescueCenter("DB-CAR", "DeepBlue Caribbean", "Santa Marta")
        );

        RescueCase rescueCase = new RescueCase();
        rescueCase.setCaseCode("RES-2026-100");
        rescueCase.setRescueDate(LocalDate.of(2026, 8, 18));
        rescueCase.setRescueLocation("Bahía Concha");
        rescueCase.setStatus(RescueStatus.IN_REHABILITATION);
        rescueCase.setRescueCenter(center);

        Animal animal = new Animal();
        animal.setAnimalCode("AN-2026-100");
        animal.setCommonName("Green Sea Turtle");
        animal.setScientificName("Chelonia mydas");
        animal.setSex(AnimalSex.FEMALE);

        MedicalRecord medicalRecord = new MedicalRecord();
        medicalRecord.setInitialWeight(new BigDecimal("27.80"));
        medicalRecord.setInitialCondition("STABLE");
        medicalRecord.setInjuries("Injury caused by fishing net");
        medicalRecord.setObservations("Possible plastic ingestion");

        Specialist specialist = new Specialist();
        specialist.setProfessionalCode("SPEC-001");
        specialist.setFirstName("Elena");
        specialist.setLastName("Vargas");
        specialist.setEmail("elena@deepblue.org");
        specialist.setActive(true);

        Expertise marineReptiles = expertiseRepository.findAll().stream()
                .filter(e -> e.getName().equals("Marine Reptiles"))
                .findFirst()
                .orElseThrow();

        Expertise trauma = expertiseRepository.findAll().stream()
                .filter(e -> e.getName().equals("Trauma"))
                .findFirst()
                .orElseThrow();

        Expertise rehabilitation = expertiseRepository.findAll().stream()
                .filter(e -> e.getName().equals("Rehabilitation"))
                .findFirst()
                .orElseThrow();

        specialist.addExpertise(marineReptiles);
        specialist.addExpertise(trauma);
        specialist.addExpertise(rehabilitation);

        rescueCase.assignAnimal(animal);
        animal.assignMedicalRecord(medicalRecord);

        rescueCaseRepository.save(rescueCase);
        specialistRepository.save(specialist);

        Treatment treatment1 = new Treatment();
        treatment1.setAnimal(animal);
        treatment1.setSpecialist(specialist);
        treatment1.setPerformedAt(LocalDateTime.of(2026, 8, 19, 10, 0));
        treatment1.setType(TreatmentType.WOUND_CARE);
        treatment1.setDescription("Cleaning of left front flipper");

        Treatment treatment2 = new Treatment();
        treatment2.setAnimal(animal);
        treatment2.setSpecialist(specialist);
        treatment2.setPerformedAt(LocalDateTime.of(2026, 8, 20, 10, 0));
        treatment2.setType(TreatmentType.HYDRATION);
        treatment2.setDescription("Subcutaneous fluid therapy");

        treatmentRepository.saveAll(List.of(treatment1, treatment2));

        // Consulta 1: ¿Existe el caso RES-2026-100?
        assertThat(rescueCenterRepository.findByCode("DB-CAR")).isPresent();
        assertThat(rescueCaseRepository.findByCaseCode("RES-2026-100")).isPresent();

        // Consulta 2: Casos IN_REHABILITATION
        List<RescueCase> rehabilitationCases =
                rescueCaseRepository.findByStatusOrderByRescueDateAsc(
                        RescueStatus.IN_REHABILITATION);

        assertThat(rehabilitationCases).isNotEmpty();

        // Consulta 3: Animales pertenecientes al centro DB-CAR
        List<Animal> centerAnimals =
                animalRepository.findByRescueCaseRescueCenterCode("DB-CAR");

        assertThat(centerAnimals).isNotEmpty();

        // Consulta 4: Animales cuyo nombre común contiene "turtle"
        List<Animal> turtles =
                animalRepository.findByCommonNameContainingIgnoreCase("turtle");

        assertThat(turtles).isNotEmpty();

        // Consulta 5: Especialistas activos con expertise Trauma
        List<Specialist> traumaSpecialists =
                specialistRepository.findActiveByExpertise("Trauma");

        assertThat(traumaSpecialists).isNotEmpty();

        // Consulta 6: Todos los tratamientos de AN-2026-100 en orden cronológico
        List<Treatment> animalTreatments =
                treatmentRepository.findByAnimalIdOrderByPerformedAtAsc(animal.getId());

        assertThat(animalTreatments)
                .hasSize(2)
                .extracting(Treatment::getPerformedAt)
                .containsExactly(
                        LocalDateTime.of(2026, 8, 19, 10, 0),
                        LocalDateTime.of(2026, 8, 20, 10, 0)
                );

        // Consulta 7: Tratamientos realizados por especialistas con expertise Rehabilitation
        List<Treatment> rehabilitationTreatments =
                treatmentRepository.findBySpecialistExpertise("Rehabilitation");

        assertThat(rehabilitationTreatments).hasSize(2);

        // Consulta 8: Tratamientos entre dos fechas
        List<Treatment> treatmentsBetween =
                treatmentRepository.findTreatmentsBetween(
                        LocalDateTime.of(2026, 8, 19, 0, 0),
                        LocalDateTime.of(2026, 8, 20, 23, 59)
                );

        assertThat(treatmentsBetween).hasSize(2);

        // Reto: animales en rehabilitación con tratamiento de especialista Trauma
        List<Animal> traumaRehabilitationAnimals =
                animalRepository.findAnimalsWithTreatmentBySpecialistExpertise(
                        RescueStatus.IN_REHABILITATION,
                        "Trauma"
                );

        assertThat(traumaRehabilitationAnimals)
                .extracting(Animal::getAnimalCode)
                .contains("AN-2026-100");
    }
}