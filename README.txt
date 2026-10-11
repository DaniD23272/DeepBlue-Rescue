DeepBlue Rescue — controladores REST y pruebas MVC

Incluye:
- AnimalController.java
- TreatmentController.java
- RescueCaseControllerTest.java
- AnimalControllerTest.java
- TreatmentControllerTest.java

Copia las carpetas src/main y src/test dentro de la raíz del proyecto, conservando la estructura.
No reemplaza RescueCaseController.java ni GlobalExceptionHandler/DTOs existentes.

DEPENDENCIA PARA TESTS MVC (Spring Boot 4.1.1)
Agrega dentro de <dependencies> en pom.xml:

<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-webmvc-test</artifactId>
    <scope>test</scope>
</dependency>

Después ejecuta desde PowerShell:
  .\mvnw.cmd test

NOTAS DE INTEGRACIÓN:
1. Las pruebas de excepciones asumen que ResourceNotFoundException y BusinessRuleException están en
   com.deepblue.rescue.exception y que tienen constructor con un mensaje String.
2. Se asume que el GlobalExceptionHandler existente está anotado con @RestControllerAdvice y convierte
   ResourceNotFoundException en 404 y BusinessRuleException en 409.
3. Los tests eligen valores existentes de los enums mediante values()[0], para no asumir los nombres.
