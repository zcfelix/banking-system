# Banking Transaction System 🏦

![Java](https://img.shields.io/badge/Java-21-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.3-brightgreen)
![Build](https://img.shields.io/badge/build-passing-brightgreen)

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




