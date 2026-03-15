# API Documentation

## Run

```bash
mvn spring-boot:run
```

### Enable Demo Mode
To enable the username and password authentication flow (Demo Mode), run the application with the `demo` profile:

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=demo
```

This document outlines the API endpoints for the Predictor Backend service.

## Authentication

Most endpoints require a valid JWT. To obtain one, use the Google Auth endpoint.

- **Header:** `Authorization: Bearer <your_jwt_token>`

| Endpoint | Authentication |
|---|---|
| `POST /v1/auth/google` | Public |
| `POST /v1/auth/demo/register` | Public (Demo Mode Only) |
| `POST /v1/auth/demo/login` | Public (Demo Mode Only) |
| `POST /v1/auth/refresh` | Public |
| `POST /v1/auth/logout` | Public |
| `GET /v1/auth/me` | **Required** |
| `GET /v1/markets` | Public |
| `GET /v1/markets/{id}` | Public |
| `POST /v1/markets` | **Required** |
| `POST /v1/markets/{id}/resolve` | **Required** |
| `POST /v1/markets/{id}/trades` | **Required** |
| `GET /v1/users` | **Required** |
| `GET /v1/stream/events` | Public / Optional |

---

## Auth API

### Google Login
Exchanges a Google ID Token for an application access and refresh token.

- **URL:** `/v1/auth/google`
- **Method:** `POST`
- **Auth Required:** No
- **Request Body:** 
  ```json
  {
    "tokenId": "string"
  }
  ```
- **Response Structure:**
  ```json
  {
    "accessToken": "string",
    "refreshToken": "string",
    "expiresIn": 900
  }
  ```

### Refresh Token
Obtain a new access token using a refresh token.

- **URL:** `/v1/auth/refresh`
- **Method:** `POST`
- **Auth Required:** No
- **Request Body:** 
  ```json
  {
    "refreshToken": "string"
  }
  ```

### Logout
Revokes the provided refresh token.

- **URL:** `/v1/auth/logout`
- **Method:** `POST`
- **Auth Required:** No (Token passed in body)
- **Request Body:** 
  ```json
  {
    "refreshToken": "string"
  }
  ```

---

## Demo Auth API (Demo Mode Only)

### Demo Register
Registers a new user with a username and password. Only active in `demo` profile.

- **URL:** `/v1/auth/demo/register`
- **Method:** `POST`
- **Auth Required:** No
- **Request Body:**
  ```json
  {
    "username": "string",
    "password": "string",
    "email": "string"
  }
  ```

### Demo Login
Authenticates a user with a username and password. Only active in `demo` profile.

- **URL:** `/v1/auth/demo/login`
- **Method:** `POST`
- **Auth Required:** No
- **Request Body:**
  ```json
  {
    "username": "string",
    "password": "string"
  }
  ```

### Get Current User Profile
Retrieves the profile of the currently authenticated user.

- **URL:** `/v1/auth/me`
- **Method:** `GET`
- **Auth Required:** Yes
- **Response Structure:**
  ```json
  {
    "userId": "string",
    "email": "string",
    "displayName": "string",
    "pictureUrl": "string",
    "balance": 1000.00
  }
  ```

---

## Market API

### Create Market
Creates a new prediction market.

- **URL:** `/v1/markets`
- **Method:** `POST`
- **Auth Required:** Yes
- **Request Body:**

| Field | Type | Description | Required | Constraints |
|---|---|---|---|---|
| `name` | String | Name of the market | Yes | Unique, Cannot be empty |
| `description` | String | Description of the market | No | |
| `liquidity` | Double | Initial liquidity | No | Default: 50.0, Must be > 0 |

**Example Response:**
```json
{
  "status": "success",
  "message": "Market created successfully.",
  "marketId": "uuid-string"
}
```

### Get All Markets
Retrieves a list of all markets, optionally filtered by status.

- **URL:** `/v1/markets`
- **Method:** `GET`
- **Auth Required:** No
- **Query Parameters:**
    - `status` (Optional): Filter markets by status (`OPEN`, `RESOLVED`).

**Response Object (Array):**
```json
[
  {
    "marketId": "string",
    "marketName": "string",
    "marketDescription": "string",
    "status": "OPEN", 
    "resolvedOutcome": null,
    "qYes": 0.0,
    "qNo": 0.0
  }
]
```

### Get Market by ID
Retrieves details of a specific market.

- **URL:** `/v1/markets/{marketId}`
- **Method:** `GET`
- **Auth Required:** No

### Resolve Market
Resolves a market with a specific outcome (YES or NO).

- **URL:** `/v1/markets/{marketId}/resolve`
- **Method:** `POST`
- **Auth Required:** Yes
- **Request Body:** 
  ```json
  {
    "outcomeId": "YES"
  }
  ```

---

## Trade API

### Buy Shares
Executes a trade to buy shares in a market.

- **URL:** `/v1/markets/{marketId}/trades`
- **Method:** `POST`
- **Auth Required:** Yes
- **Request Body:** 

| Field | Type | Description | Required | Constraints |
|---|---|---|---|---|
| `outcome` | String | Outcome to buy ("YES" or "NO") | Yes | Case-insensitive |
| `amount` | Double | Investment amount (cost) | Yes | Must be > 0 |

**Example Response:**
```json
{
  "status": "success",
  "message": "Trade executed successfully.",
  "tradeId": "string",
  "sharesBought": 15.4,
  "cost": 10.0,
  "outcome": "YES"
}
```

---

## User API

### Get All Users
Retrieves a list of all registered users.

- **URL:** `/v1/users`
- **Method:** `GET`
- **Auth Required:** Yes

**Response Object (Array):**
```json
[
  {
    "userId": "string",
    "userName": "string",
    "email": "string"
  }
]
```

---

## SSE Stream (Events)

### Real-time Event Stream
Provides a Server-Sent Events (SSE) stream for real-time updates on market changes and trades.

- **URL:** `/v1/stream/events`
- **Method:** `GET`
- **Auth Required:** Optional (Uses token for rate limiting if provided)
- **Headers:** 
  - `Accept: text/event-stream`
- **Query Parameters:**
  - `marketId` (Optional): Filter events for a specific market.

---

## Error Response Format

All error responses (4xx and 5xx) follow a consistent structure.

**Response Structure:**
```json
{
  "timestamp": "2026-03-15T18:44:36",
  "status": 401,
  "error": "Unauthorized",
  "message": "Invalid access token"
}
```

## Testing

To run the automated tests:

```bash
mvn test
```

To run a specific test class:

```bash
mvn test -Dtest=AuthIntegrationTest
```

To run demo authentication tests:

```bash
mvn test -Dtest=DemoAuthIntegrationTest
```
