# Banking Transaction System

A simple banking transaction management system that demonstrates clean architecture and SOLID principles.

## Project Structure
```
src/
├── main/
│   └── java/
│       └── com/
│           └── hsbc/
│               └── banking/
│                   └── transaction/
│                       ├── controller/
│                       ├── service/
│                       ├── repository/
│                       ├── model/
│                       └── dto/
```

## Design Principles

### Interface Injection & Dependency Inversion

This project demonstrates interface injection, a form of dependency injection that emphasizes programming to interfaces:

#### Key Components

```java
// Repository interface
@Repository
public interface TransactionRepository {
    Transaction save(Transaction transaction);
    Optional<Transaction> findById(Long id);
    List<Transaction> findAll();
}

// Implementation
public class InMemoryTransactionRepository implements TransactionRepository {
    // Implementation details...
}

// Service using interface injection
@Service
public class TransactionService {
    private final TransactionRepository repository;

    public TransactionService(TransactionRepository repository) {
        this.repository = repository;
    }
}
```

#### Benefits of Interface Injection

1. **Decoupling**
   - Components are loosely coupled through interfaces
   - Implementation details are hidden from dependent components

2. **Flexibility**
   - Easy to switch implementations without changing client code
   - Facilitates the addition of cross-cutting concerns (like caching or logging)

3. **Testability**
   - Makes unit testing easier through mock implementations
   - Allows for different implementations in different environments

4. **Maintainability**
   - Clear separation of concerns
   - Changes to implementations don't affect the rest of the system

