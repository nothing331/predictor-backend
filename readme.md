# API Documentation

## Run

Note: You must have PostgreSQL and Redis running locally (e.g., via `docker compose up -d db redis`) before starting the Spring Boot application using Maven.

```bash
mvn spring-boot:run
```

### Run The Full Dev Stack With Docker

If you want one command for backend dependencies during frontend work, run:

```bash
docker compose up --build
```

That starts:

- the Spring Boot backend on `http://localhost:8080`
- PostgreSQL on `localhost:5433`
- Redis on `localhost:6379`

The compose setup defaults to the `demo` Spring profile so you can use demo auth
without wiring Google OAuth during local frontend work.

To stop everything:

```bash
docker compose down
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
| `GET /v1/markets/{id}/me` | **Required** |
| `POST /v1/markets` | **Required (ADMIN)** |
| `POST /v1/markets/{id}/resolve` | **Required (ADMIN)** |
| `POST /v1/markets/{id}/trades` | **Required** |
| `GET /v1/users` | **Required** |
| `GET /v1/users/me/summary` | **Required** |
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
    "expiresInSeconds": 900
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
    "name": "string",
    "pictureUrl": "string",
    "balance": 1000.00,
    "role": "USER"
  }
  ```

---

## Market API

### Create Market
Creates a new prediction market. Only accessible to users with the ADMIN role.

- **URL:** `/v1/markets`
- **Method:** `POST`
- **Auth Required:** Yes (ADMIN)
- **Request Body:**

| Field | Type | Description | Required | Constraints |
|---|---|---|---|---|
| `name` | String | Name of the market | Yes | Unique, Cannot be empty |
| `description` | String | Description of the market | No | |
| `liquidity` | Double | Initial liquidity | No | Default: 50.0, Must be > 0 |
| `category` | String | Category of the market | No | Default: "General" |
| `yesLabel` | String | Custom label for YES outcome | No | Default: "Yes" |
| `noLabel` | String | Custom label for NO outcome | No | Default: "No" |

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
    "status": "OPEN", 
    "resolvedOutcome": null,
    "category": "string",
    "outcomes": [
      {
        "outcomeId": "YES",
        "label": "string",
        "probability": 0.5
      },
      {
        "outcomeId": "NO",
        "label": "string",
        "probability": 0.5
      }
    ],
    "totalValue": 0.0
  }
]
```

### Get Market by ID
Retrieves details of a specific market.

- **URL:** `/v1/markets/{marketId}`
- **Method:** `GET`
- **Auth Required:** No

### Get Current User Position for a Market
Returns the authenticated user's investment summary and trade history for a specific market.

- **URL:** `/v1/markets/{marketId}/me`
- **Method:** `GET`
- **Auth Required:** Yes

**Response Structure:**
```json
{
  "userId": "string",
  "marketId": "string",
  "marketName": "string",
  "marketStatus": "OPEN",
  "resolvedOutcome": null,
  "currentYesChance": 0.58,
  "currentNoChance": 0.42,
  "yesSharesHeld": 3.5,
  "noSharesHeld": 1.0,
  "totalInvested": 12.50,
  "totalYesInvested": 8.00,
  "totalNoInvested": 4.50,
  "firstTradeAt": "2026-03-27T10:00:00Z",
  "lastTradeAt": "2026-03-27T12:00:00Z",
  "projectedPayoutIfYes": 3.5,
  "projectedPayoutIfNo": 1.0,
  "realizedPayout": null,
  "realizedNetPnl": null,
  "tradeCount": 2,
  "trades": [
    {
      "tradeId": "string",
      "outcome": "NO",
      "sharesBought": 1.0,
      "cost": 4.50,
      "tradedAt": "2026-03-27T12:00:00Z"
    }
  ]
}
```

Notes:

- if the user has never traded that market, this still returns `200` with zeroed holdings and `trades: []`
- projected payout fields are outcome-based settlement payouts, not current cash-out estimates
- when the market is resolved, `realizedPayout` and `realizedNetPnl` are populated
- trade history is returned newest first

### Resolve Market
Resolves a market with a specific outcome (YES or NO). Only accessible to users with the ADMIN role.

- **URL:** `/v1/markets/{marketId}/resolve`
- **Method:** `POST`
- **Auth Required:** Yes (ADMIN)
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
    "userId": "string"
  }
]
```

### Get Current User Account Summary
Returns the authenticated user's available balance and summaries for the 3 most recently traded unique markets.

- **URL:** `/v1/users/me/summary`
- **Method:** `GET`
- **Auth Required:** Yes

**Response Structure:**
```json
{
  "userId": "string",
  "availableBalance": 1000.00,
  "recentMarkets": [
    {
      "marketId": "string",
      "marketName": "string",
      "marketStatus": "OPEN",
      "lastTradedAt": "2026-03-27T12:00:00Z",
      "resolvedOutcome": null,
      "userYesShares": 3.5,
      "userNoShares": 1.0,
      "currentYesChance": 0.58,
      "currentNoChance": 0.42,
      "projectedPayoutIfYes": 3.5,
      "projectedPayoutIfNo": 1.0
    }
  ]
}
```

Notes:

- `recentMarkets` contains at most 3 unique markets
- markets are ordered by the user's most recent trade time, newest first
- projected payouts are settlement-style payouts, not current cash-out estimates
- `availableBalance` is cash balance only

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
