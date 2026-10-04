# DeepBlue Rescue

## Descripción

DeepBlue Rescue es un proyecto de persistencia para la gestión de casos de rescate y rehabilitación de fauna marina.

El laboratorio implementa la capa de persistencia utilizando Java 21, Spring Boot 4, Spring Data JPA, Hibernate, PostgreSQL, Flyway y Testcontainers.

El proyecto se enfoca exclusivamente en persistencia. No incluye controladores REST, servicios, DTOs, seguridad ni frontend.

---

## Modelo de datos

El sistema está compuesto por las siguientes entidades:

- `RescueCenter`
- `RescueCase`
- `Animal`
- `MedicalRecord`
- `Specialist`
- `Expertise`
- `Treatment`

Además, se utilizan los enumerados:

- `RescueStatus`
- `AnimalSex`
- `TreatmentType`

---

## Relaciones

### RescueCenter - RescueCase

Un centro de rescate puede tener múltiples casos de rescate.

```text
RescueCenter 1 ─── N RescueCase