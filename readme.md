# Wallet API

A Spring Boot REST API for managing cryptocurrency portfolios with price tracking and simulation capabilities.

## Tech Stack

- **Framework**: Spring Boot 3.5.6
- **Language**: Java 21
- **Database**: PostgreSQL
- **ORM**: Spring Data JPA with Hibernate
- **Security**: Spring Security with JWT
- **Documentation**: SpringDoc OpenAPI
- **External API**: CoinCap API for cryptocurrency prices
- **Build Tool**: Maven
- **Database Migration**: Liquibase
- **HTTP Client**: OpenFeign
- **Testing**: JUnit 5

## Architecture

### Core Entities
- **User**: Represents application users with email-based authentication
- **Asset**: Cryptocurrency assets with real-time'ish USD pricing
- **UserAsset**: Entity linking users to their owned assets with quantities

### Key Components
- **Controllers**: REST endpoints for authentication, wallet management, and simulation
- **Services**: Business logic for user management, asset tracking, and portfolio operations
- **Security**: JWT-based authentication with custom filters and configurations
- **Price Integration**: CoinCap API adapter for real-time cryptocurrency price fetching
- **Scheduled Tasks**: Background price updates with configurable threading

## API Endpoints

### Authentication (`/auth`)
- `POST /auth/signup` - Register a new user
  - **Example request:**
    ```json
    {
      "email": "user@example.com",
      "password": "mypassword"
    }
    ``` 
- `POST /auth/login` - Authenticate user and get JWT token
  - **Example request:**
    ```json
    {
      "email": "user@example.com",
      "password": "mypassword"
    }
    ``` 

### Wallet Management (`/wallet`)
- `GET /wallet/info` - Get wallet information with current balances
  - **Example response:**
    ```json
    {
      "id": "00000000-0000-0000-0000-000000000000",
      "original": {
        "total": 200000,
        "assets": [
          {
            "symbol": "BTC",
            "quantity": 2,
            "price": 100000,
            "value": 200000
          }
        ]
      },
      "current": {
        "total": 400000,
        "assets": [
          {
            "symbol": "BTC",
            "quantity": 2,
            "price": 200000,
            "value": 400000,
            "timestamp": "2025-10-20T20:44:46.150766Z"
          }
        ]
      }
    } 
    ```
- `POST /wallet/asset` - Add an asset to the wallet
  - **Example request:**
    ```json
    {
      "symbol": "BTC",
      "price": "50000",
      "quantity": "2"
    }
    ```
- `PUT /wallet/asset` - Update asset quantity in the wallet
  - **Example request:**
      ```json
      {
        "symbol": "BTC",
        "price": "50000",
        "quantity": "2"
      }
      ```

### Portfolio Simulation (`/simulation`)
- `POST /simulation` - Simulate portfolio performance over time
  - **Example request:**
    ```json
    {
      "timestamp": "1723327200",
      "assets": [
        {
          "symbol": "BTC",
          "quantity": 1.5,
          "value": 50000
        },
        {
          "symbol": "ETH",
          "quantity": 4.25,
          "value": 1500
        }
      ]
    } 
    ```
  - **Example response:**
    ```json
    {
      "timestamp": "2024-08-10T22:00:00Z",
      "total": 102849.95834051541,
      "bestAsset": "ETH",
      "bestPerformance": 641.5977,
      "worstAsset": "BTC",
      "worstPerformance": 83.452,
      "assets": [
        {
          "symbol": "BTC",
          "quantity": 1.5,
          "price": 61150.66196935916,
          "value": 91725.99295403872
        },
        {
          "symbol": "ETH",
          "quantity": 4.25,
          "price": 2617.403620347455,
          "value": 11123.965386476684
        }
      ]
    } 
    ```

## Getting Started

### Prerequisites
- Java 21
- Docker and Docker Compose
- Maven

### Setup

1. **Clone the repository**
   ```bash
   git clone <repository-url>
   cd wallet
   ```

2. **Configure CoinCap API token**

   Set your CoinCap API token in `application.properties`:
   ```properties
   app.price.coincap.token=your_token_here
   ```
   
   OR run the application with `-Dapp.price.coincap.token=your_token_here`

3. **Run the application**
   ```bash
   ./mvnw spring-boot:run
   ```

The application will automatically start PostgreSQL using Docker Compose integration (spring-boot-docker-compose starter) and be available at `http://localhost:8080`

## API Documentation

The API is available at `http://localhost:8080/swagger-ui/index.html` when the application is running.

There's also a [bruno](https://www.usebruno.com/) collection at `/doc/bruno/Wallet`, after running the `/auth/signup`
and `/auth/login` endpoints it will store the JWT token in the `Authorization` header for all subsequent requests.

## Testing

There are unit tests for the `AssetService` and `UserService` classes with 100% code coverage
for each.

You can also run unit tests with:

```bash
./mvnw test
```

## Database Schema

The application uses Liquibase for database versioning. Schema changes are managed through changesets in `src/main/resources/db/changelog/changesets/`:

- `001-create-asset-table.yaml` - Asset table for cryptocurrency data
- `002-create-user-table.yaml` - User table for authentication
- `003-create-user-asset-table.yaml` - User-asset relationship table

## Security

- JWT-based authentication with configurable expiration
- Passwords are hashed using BCrypt
- Stateless session management

## Price Updates

The application automatically fetches cryptocurrency prices from CoinCap API:
- Configurable update interval (default: 60 seconds)
- Multithreaded price fetching (default: three simultaneous requests with Virtual Threads)
- Automatic asset discovery and price synchronization

## Additional Notes

### Authentication System
The application uses a JWT-based authentication mechanism. For production systems, integration with enterprise 
identity providers like Keycloak would be recommended since it's production ready and battle-tested.

### Current and Original Wallet Data on the Info Endpoint
The `/wallet/info` endpoint returns both the `original` and `current` wallet state.

### Virtual Thread-Based Price Fetching Strategy with Semaphores
Price updates are done inside virtual threads (default: three concurrent operations) to fetch individual 
asset prices concurrently. 
The semaphores are used to ensure that no more than three requests are sent to the CoinCap API at a time.
The CoinCap API supports batch requests for up to 100 assets, so this can be improved in the future.
