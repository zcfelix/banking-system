# Banking Transaction System 🏦

![Java](https://img.shields.io/badge/Java-21-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.3-brightgreen)
![Build](https://img.shields.io/badge/build-passing-brightgreen)

## Table of Contents 📑

1. [Introduction](#1-introduction-)
2. [Features](#2-features-)
3. [API Documentation](#3-api-documentation-)
   - [How to Access Swagger UI](#31-how-to-access-swagger-ui)
   - [Available APIs](#32-available-apis)
4. [Getting Started](#4-getting-started-)
   - [Prerequisites](#41-prerequisites)
   - [Running the Application](#42-running-the-application)
     - [Local Development](#421-local-development)
     - [Using Docker](#422-using-docker)
   - [Running Tests](#43-running-tests)
     - [Unit Tests](#431-unit-tests)
     - [Integration Tests](#432-integration-tests)
     - [Performance Tests](#433-performance-tests)
5. [Architecture and Design](#5-architecture-and-design-%EF%B8%8F)
   - [System Architecture](#51-system-architecture)
   - [Project Structure](#52-project-structure)
   - [Key Components](#53-key-components)
     - [Transaction Model](#531-transaction-model)
     - [In-Memory Transaction Repository](#532-in-memory-transaction-repository)
6. [Key Design Considerations](#6-key-design-considerations-)
   - [Concurrency Control](#61-concurrency-control)
   - [Data Consistency](#62-data-consistency)
   - [Performance Optimization](#63-performance-optimization)
   - [Interface Injection & Dependency Inversion](#64-interface-injection--dependency-inversion)
7. [Future Enhancements](#7-future-enhancements-)
8. [External Dependencies](#8-external-dependencies-)
   - [Core Dependencies](#81-core-dependencies)
   - [Testing Dependencies](#82-testing-dependencies)
   - [Documentation](#83-documentation)
   - [Caching](#84-caching)
   - [Performance Testing](#85-performance-testing)
   
---

## 1. Introduction 📝
A high-performance banking transaction management system built with `Spring Boot`. This system provides RESTful APIs for managing financial transactions with features like concurrent update handling, audit logging, and in-memory data storage with proper locking mechanisms.

## 2. Features ✨
- 🔄 RESTful APIs for transaction management
- 🔒 Concurrent transaction updates with optimistic locking
- 📝 Audit logging for sensitive operations
- 💾 In-memory data storage with thread-safe implementation
- 📚 Swagger UI for API documentation and testing
- ✅ Comprehensive test coverage (unit tests and integration tests)
- 🚀 Performance testing support with K6

---

## 3. API Documentation 📚

### 3.1 How to Access Swagger UI
1. Start the application
2. Visit: `http://localhost:8080/swagger-ui.html`

### 3.2 Available APIs
- **GET** `/transactions`
  - List all transactions with pagination support
  - Query parameters: `page`, `size`
  - Returns: `PageResponse<TransactionResponse>`

- **GET** `/transactions/{id}`
  - Get a specific transaction by ID
  - Returns: `TransactionResponse`

- **POST** `/transactions`
  - Create a new transaction
  - Request body: Transaction details
  - Returns: Created transaction with ID

- **PUT** `/transactions/{id}`
  - Update an existing transaction
  - Implements optimistic locking for concurrent updates
  - Request body: Updated transaction details
  - Returns: Updated transaction

- **DELETE** `/transactions/{id}`
  - Delete a transaction
  - Creates audit log entry
  - Returns: `204 No Content`

---

## 4. Getting Started 🚀

### 4.1 Prerequisites
- `JDK 21`
- `Maven 3.9+`
- `Docker` (optional)
- `K6` (for performance testing)

### 4.2 Running the Application

#### 4.2.1 Local Development
```bash
# Build the project
mvn clean package

# Run the application
java -jar target/transaction-0.0.1-SNAPSHOT.jar
```

#### 4.2.2 Using Docker
```bash
# Build Docker image
docker build -t banking-transaction .

# Run container
docker run -p 8080:80 banking-transaction
```

### 4.3 Running Tests

#### 4.3.1 Unit Tests
```bash
mvn test
```

#### 4.3.2 Integration Tests
```bash
mvn verify
```

#### 4.3.3 Performance Tests
```bash
# Install k6 (MacOS)
brew install k6

# Run performance test
k6 run -e PORT=8080 load-test.js

# Quick test with custom parameters
k6 run -e PORT=8080 --vus 10 --duration 30s load-test.js
```

---

## 5. Architecture and Design 🏗️

### 5.1 System Architecture
The system follows a layered architecture:
- `Controller Layer`: REST API endpoints and DTOs
- `Service Layer`: Business logic and transaction management
- `Repository Layer`: Data access and storage
- `Model Layer`: Domain objects

### 5.2 Project Structure
```
src/
├── main/
│   └── java/
│       └── com/
│           └── hsbc/
│               └── banking/
│                   └── transaction/
│                       ├── config/      # Configuration classes
│                       ├── controller/  # REST controllers
│                       ├── exception/   # Exception handlers
│                       ├── service/     # Business logic
│                       ├── repository/  # Data access
│                       ├── model/       # Domain models
│                       └── dto/         # Data transfer objects
```

### 5.3 Key Components

#### 5.3.1 Transaction Model
Core fields of the `Transaction` class:
- `id`: Unique identifier
- `orderId`: External order Identifier
- `accountId`: Account ID
- `amount`: Transaction amount (`BigDecimal`)
- `type`: Transaction type (`CREDIT`, `DEBIT`)
- `category`: Transaction category
- `description`: Transaction description
- `createdAt`: Creation timestamp
- `updatedAt`: Last update timestamp
- `version`: Used for optimistic locking

#### 5.3.2 In-Memory Transaction Repository
- Uses `ConcurrentSkipListMap` for thread-safe operations
- Implements optimistic locking for concurrent updates
- Maintains transaction history and audit logs
- Provides atomic operations for data consistency

---

## 6. Key Design Considerations 🔍

### 6.1 Concurrency Control
- Optimistic locking using `version` based conflict detection
- Retry mechanism with exponential backoff

### 6.2 Data Consistency
- Atomic operations in repository layer
- Validation at multiple levels (DTO, Service)

### 6.3 Performance Optimization
- In-memory data storage for fast access
- Efficient data structures (`ConcurrentSkipListMap`)
- Pagination for large data sets

### 6.4 Interface Injection & Dependency Inversion

```java
@Repository
public interface TransactionRepository {
    Transaction save(Transaction transaction);
    Optional<Transaction> findById(Long id);
    List<Transaction> findAll();
}

@Service
public class TransactionService {
    private final TransactionRepository repository;

    public TransactionService(TransactionRepository repository) {
        this.repository = repository;
    }
}
```

---

## 7. Future Enhancements 🔮
- Add `status` and `currencyType` fields in `Transaction` model
- Persistent storage support (JPA, Hibernate)
- Enhanced security features (JWT, OAuth)
- Metrics dashboard (Prometheus, Grafana)

## 8. External Dependencies 📚

### 8.1 Core Dependencies
- `spring-boot-starter-web` (v3.2.3)
  - Purpose: Provides core Spring MVC functionality
  - Features: RESTful API support, embedded Tomcat server

- `spring-boot-starter-validation` (v3.2.3)
  - Purpose: Bean validation support
  - Features: Input validation, constraint annotations

- `spring-boot-starter-cache` (v3.2.3)
  - Purpose: Caching support for improved performance
  - Features: Cache abstraction, Caffeine integration

### 8.2 Testing Dependencies
- `spring-boot-starter-test` (v3.2.3)
  - Purpose: Testing framework integration
  - Features: JUnit 5, Mockito, AssertJ

### 8.3 Documentation
- `springdoc-openapi-starter-webmvc-ui` (v2.3.0)
  - Purpose: API documentation
  - Features: OpenAPI 3.0, Swagger UI

### 8.4 Caching
- `caffeine` (Latest)
  - Purpose: High-performance caching library
  - Features: In-memory caching, thread-safe operations

### 8.5 Performance Testing
- `k6` (Latest)
  - Purpose: Load and performance testing
  - Features: Scripted tests, metrics collection


