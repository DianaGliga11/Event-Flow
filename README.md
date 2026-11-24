# EventFlow — Distributed Event Ticketing System

## 1. Project Overview

**EventFlow** is a distributed event ticketing system developed as part of a Distributed Systems and Concurrency course project.

The application is designed to manage events, customers, ticket orders, payments, seats, and tickets using two independent relational database management systems: PostgreSQL and MySQL.

The main objective is to develop a multi-tier application that demonstrates distributed transaction management, concurrency control, transaction recovery, and consistency across multiple databases.

The project is currently in its initial development stage. The database schemas and the basic database connectivity have been configured successfully. The distributed transaction management layer and the application use cases are planned for subsequent development.

## 2. Technologies Used

- **Java 21** — primary programming language.
- **Spring Boot 4.1.1** — application framework.
- **Gradle** — build and dependency management.
- **Spring Web MVC** — web application and HTTP request handling.
- **Thymeleaf** — server-side HTML rendering.
- **JDBC** — direct database access and SQL execution.
- **PostgreSQL 18.6** — event, seat, and ticket management.
- **MySQL** — customer, order, and payment management.
- **IntelliJ IDEA** — development environment.

Spring Data JPA is not currently used. Direct JDBC access is intended to provide explicit control over SQL statements and transaction boundaries, which is important for implementing the required application-level distributed transaction protocol.

## 3. System Architecture

EventFlow follows a multi-tier architecture.

### 3.1. Presentation Layer

The presentation layer will provide the interface through which users can browse events, select seats, purchase tickets, and view their orders.

Spring Web MVC and Thymeleaf are included in the project dependencies for this purpose.

### 3.2. Application Layer

The application layer will contain the business logic for event discovery, ticket reservations, order processing, and payment management.

A custom transaction management component is planned to coordinate operations across PostgreSQL and MySQL.

### 3.3. Data Layer

The data layer consists of two separate relational databases:

- **PostgreSQL:** stores event information, available seats, and issued tickets.
- **MySQL:** stores customer information, ticket orders, and payments.

Since the databases run independently, foreign key constraints cannot directly enforce relationships between tables located on different database servers. Cross-database consistency will therefore need to be managed by the application.

## 4. Database Design

Six tables have been created successfully across the two databases.

### 4.1. PostgreSQL — `eventflow_postgres`

#### `events`

Stores the events available in the system.

Main attributes:

- `event_id` — primary key generated automatically.
- `name` — event name.
- `location` — event location.
- `event_date` — scheduled date and time.
- `status` — event status, restricted to `ACTIVE`, `CANCELLED`, or `COMPLETED`.

#### `seats`

Stores the seats available for each event.

Main attributes:

- `seat_id` — primary key generated automatically.
- `event_id` — foreign key referencing `events`.
- `seat_number` — seat identifier.
- `price` — ticket price.
- `status` — availability status, restricted to `AVAILABLE` or `SOLD`.

A unique constraint on `(event_id, seat_number)` prevents duplicate seat identifiers for the same event.

#### `tickets`

Stores the tickets issued for reservations.

Main attributes:

- `ticket_id` — primary key generated automatically.
- `event_id` — foreign key referencing `events`.
- `seat_id` — foreign key referencing `seats`.
- `order_id` — identifier of the corresponding order stored in MySQL.
- `status` — ticket status, restricted to `ACTIVE` or `CANCELLED`.
- `created_at` — ticket creation timestamp.

A unique constraint on `seat_id` prevents multiple ticket records from being created for the same seat.

The `order_id` column is a logical cross-database reference. It does not have a conventional foreign key because the corresponding order is stored in MySQL.

### 4.2. MySQL — `eventflow_mysql`

#### `customers`

Stores customer information.

Main attributes:

- `customer_id` — automatically generated primary key.
- `name` — customer name.
- `email` — unique customer email address.
- `created_at` — customer creation timestamp.

The unique constraint on `email` prevents duplicate customer email addresses.

#### `ticket_orders`

Stores ticket purchase orders.

Main attributes:

- `order_id` — automatically generated primary key.
- `customer_id` — foreign key referencing `customers`.
- `total_amount` — total order amount.
- `status` — order status, restricted to `PENDING`, `CONFIRMED`, or `CANCELLED`.
- `created_at` — order creation timestamp.

The foreign key ensures that each order references an existing customer.

#### `payments`

Stores payment records associated with orders.

Main attributes:

- `payment_id` — automatically generated primary key.
- `order_id` — foreign key referencing `ticket_orders`.
- `amount` — payment amount.
- `status` — payment status, restricted to `PENDING`, `COMPLETED`, `FAILED`, or `REFUNDED`.
- `created_at` — payment creation timestamp.

The foreign key ensures that each payment references an existing order.

## 5. Database Connectivity

Dedicated JDBC connection classes have been created in the package:

`com.eventflow.eventflow.database`

- `PostgresConnection.java` — establishes connections to PostgreSQL.
- `MySqlConnection.java` — establishes connections to MySQL.
- `DatabaseConnectionTest.java` — tests connectivity to both databases when the application starts.

The connection classes obtain their configuration from `application.properties` using Spring's `@Value` annotation.

The configuration uses separate connection URLs, usernames, and password environment variables for each database. Passwords are not intended to be hardcoded in the source code.

Example configuration:

```properties
spring.application.name=Event Flow

# PostgreSQL
eventflow.datasource.postgres.url=jdbc:postgresql://localhost:5432/eventflow_postgres
eventflow.datasource.postgres.username=postgres
eventflow.datasource.postgres.password=${POSTGRES_PASSWORD}

# MySQL
eventflow.datasource.mysql.url=jdbc:mysql://localhost:3306/eventflow_mysql
eventflow.datasource.mysql.username=root
eventflow.datasource.mysql.password=${MYSQL_PASSWORD}
```

The connection test executes a simple query against each database to verify connectivity and retrieve the current database name.

**Current status:** successful connections to both `eventflow_postgres` and `eventflow_mysql` have been established.

## 6. Application Configuration

The main application class is:

`com.eventflow.eventflow.EventFlowApplication`

The project uses Java 21 and Gradle. Spring Boot's automatic single-datasource configuration is excluded because the application manages two separate databases through custom JDBC connection classes.

This setup avoids relying on a single automatically configured datasource and provides a foundation for explicit control over database operations.

## 7. Planned Features

The following use cases are planned for implementation:

1. Browse and search for events.
2. View event details and available seats.
3. Register and manage customers.
4. Create ticket orders.
5. Reserve seats and issue tickets.
6. Process and record payments.
7. View order and ticket status.
8. Cancel orders and tickets.

These features are planned and should not be considered fully implemented at the current stage.

## 8. Planned Distributed Transaction Management

A central project requirement is the implementation of a manual application-level distributed transaction involving both databases.

For example, a ticket purchase may require the following operations:

1. Create an order in MySQL.
2. Create a payment record in MySQL.
3. Mark the selected seat as sold in PostgreSQL.
4. Create a ticket record in PostgreSQL.
5. Confirm the order after all required operations succeed.

The final implementation must coordinate these operations so that failures do not leave the databases in an inconsistent state.

The planned transaction management component will include:

- Explicit transaction coordination across both databases.
- A concurrency control mechanism, such as Strict Two-Phase Locking.
- An undo log for reversing operations after failure.
- Commit and rollback handling.
- Deadlock detection and resolution.
- Recovery mechanisms for interrupted transactions.

These components have not yet been implemented. The final design will depend on the transaction protocol selected for the course requirements.

The implementation will avoid relying on `@Transactional`, JPA transaction management, or an automatic distributed transaction manager for the core distributed transaction logic.

## 9. Current Project Status

| Component | Status |
|---|---|
| Java 21 and Gradle setup | Configured |
| Spring Boot application | Starts successfully |
| PostgreSQL database | Created |
| MySQL database | Created |
| Six database tables | Created |
| JDBC connection classes | Implemented |
| Connectivity checks | Implemented |
| Connection to PostgreSQL | Verified |
| Connection to MySQL | Verified |
| Event search functionality | Planned |
| Ticket purchasing workflow | Planned |
| Manual distributed transaction manager | Planned |
| Concurrency control and deadlock handling | Planned |
| Automated tests | To be developed |

## 10. Development Setup

To run the project locally:

1. Install Java 21.
2. Install and start PostgreSQL 18.6 and MySQL.
3. Create the `eventflow_postgres` and `eventflow_mysql` databases.
4. Execute the SQL scripts that define the six tables.
5. Configure the database credentials through the `POSTGRES_PASSWORD` and `MYSQL_PASSWORD` environment variables.
6. Open the project in IntelliJ IDEA and allow Gradle to synchronize the dependencies.
7. Run `EventFlowApplication`.

The database connection test runs during application startup and prints the connection status of both databases.

## 11. Conclusion

EventFlow currently has a working Spring Boot foundation, two independently configured relational databases, and a six-table database schema.

The next development stage is to implement the application use cases using JDBC, followed by the custom distributed transaction manager and its concurrency control and recovery mechanisms.

The project aims to demonstrate how consistency can be maintained across independent databases when a business operation spans multiple transactional resources.
