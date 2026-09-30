# Parking Coto – Sistema de gestión de parqueo privado (Java · POO)

EIF400 · Paradigmas de Programación · Proyecto 2

Aplicación de consola que gestiona el ingreso, permanencia, cobro y salida de vehículos en un parqueo privado con cobro por hora y tarifa máxima diaria.

## Requisitos

- JDK 21
- Maven 3.8 o superior (solo para las pruebas JUnit y para ejecutar con Maven)

## Ejecución

### Con Maven

```bash
mvn -q compile exec:java
```

Ejecuta `ParkingCotoJavaPOO`, que lanza `ParkingDemo`: una demostración por consola que imprime, para cada escenario, el resultado esperado, el obtenido y si coinciden ([OK] / [FAIL]).

### Sin Maven

```bash
# Linux / macOS
javac -d out $(find src/main/java -name "*.java")
java -cp out com.mycompany.parking.coto.java.poo.ParkingCotoJavaPOO

# Windows (PowerShell)
javac -d out (Get-ChildItem -Recurse src/main/java -Filter *.java).FullName
java -cp out com.mycompany.parking.coto.java.poo.ParkingCotoJavaPOO
```

### Pruebas automatizadas (JUnit 5)

```bash
mvn test
```

Las pruebas están en `src/test/java/service/ParkingSystemTest.java` y cubren los 15 casos obligatorios del enunciado, además de casos límite de la política de precios (9 h vs 10 h, 24 h, 25 h, 48 h) y reglas extra (salida sin ticket, pago de ticket activo, pago doble).

## Estructura

```
src/main/java
├── com/mycompany/parking/coto/java/poo/ParkingCotoJavaPOO.java   punto de entrada
├── demo/ParkingDemo.java                                          demostración por consola
├── enums/        PaymentType, SpaceStatus, SpaceType, TicketStatus, VehicleType
├── exception/    ParkingException (base) y sus derivadas por regla de negocio
├── model/        Vehicle (abstracta), Car, Motorcycle, CargoVehicle,
│                 ParkingSpace, ParkingTicket, Payment
├── pricing/      PricingPolicy (interfaz), HourlyPricingPolicy
└── service/      ParkingLot (coordina colecciones y operaciones)
docs/parking-coto.puml                                             diagrama UML (PlantUML)
```

## Decisiones de diseño (resumen)

- **Polimorfismo en el cobro:** `Vehicle.calculateFee()` es `final` y delega en `getPricingPolicy()`, que cada subclase redefine. Nadie pregunta por el tipo concreto del vehículo.
- **Tarifa máxima diaria:** vive únicamente en `HourlyPricingPolicy`. Cambiar el umbral (10 h) o el tope no toca `Vehicle`, `ParkingTicket` ni `ParkingLot`. Una regla nueva (tarifa nocturna, fin de semana) es una nueva implementación de `PricingPolicy`.
- **Cada objeto protege sus reglas:** `ParkingSpace` valida ocupación y compatibilidad; `ParkingTicket` controla su ciclo ACTIVO → CERRADO → PAGADO; `ParkingLot` solo coordina y aplica reglas globales (un ticket activo por vehículo, salida solo con ticket activo).
- **Encapsulamiento:** atributos privados, sin setters de estado; los cambios ocurren mediante métodos con intención (`occupy`, `release`, `close`, `pay`).

## Regla de la tarifa máxima diaria

- Toda fracción de hora se cobra como hora completa (mínimo 1 hora).
- Cada período completo de 24 horas se cobra al máximo diario.
- Las horas restantes se cobran por hora, salvo que lleguen a 10 horas o más: entonces se cobra el máximo diario.

| Tipo | Tarifa/hora | Máximo diario |
|---|---|---|
| Motocicleta | ₡500 | ₡4 000 |
| Automóvil | ₡900 | ₡7 000 |
| Vehículo de carga | ₡1 500 | ₡11 000 |

> Nota: como el tope solo aplica desde 10 h, un automóvil que permanece 9 h paga ₡8 100 y uno que permanece 10 h paga ₡7 000. Es una consecuencia directa del enunciado.

## Integrantes

- Anddy Prendas
- Mathew Ramirez
- Alensao
