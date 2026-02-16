# API Documentation

This document outlines the API endpoints for the Predictor Backend service.

## Market API

### Create Market
Creates a new prediction market.

- **URL:** `/v1/markets`
- **Method:** `POST`
- **Request Body:** JSON

| Field | Type | Description | Required | Constraints |
|---|---|---|---|---|
| `name` | String | Name of the market | Yes | Cannot be empty |
| `description` | String | Description of the market | No | |
| `liquidity` | Double | Initial liquidity | No | Default: 50.0, Must be > 0 |

**Example Request:**
```json
{
  "name": "Will it rain tomorrow?",
  "description": "Prediction market for rain in NYC on 2026-02-17",
  "liquidity": 100.0
}
```

### Get All Markets
Retrieves a list of all markets, optionally filtered by status.

- **URL:** `/v1/markets`
- **Method:** `GET`
- **Query Parameters:**
    - `status` (Optional): Filter markets by status (e.g., OPEN, CLOSED).
- **Response Body:** Array of Market objects

**Response Structure (GetAllMarket):**
```json
[
  {
    "marketId": "string",
    "marketName": "string",
    "marketDescription": "string",
    "status": "OPEN", // or CLOSED, RESOLVED
    "resolvedOutcome": "YES" // or NO, null if not resolved
  }
]
```

### Get Market by ID
Retrieves details of a specific market.

- **URL:** `/v1/markets/{marketId}`
- **Method:** `GET`
- **Path Parameters:**
    - `marketId`: ID of the market to retrieve.
- **Response Body:** Market object (same as above)

### Resolve Market
Resolves a market with a specific outcome (YES or NO).

- **URL:** `/v1/markets/{marketId}/resolve`
- **Method:** `POST`
- **Path Parameters:**
    - `marketId`: ID of the market to resolve.
- **Request Body:** JSON

| Field | Type | Description | Required | Constraints |
|---|---|---|---|---|
| `outcomeId` | String | The winning outcome ("YES" or "NO") | Yes | Cannot be empty |

**Example Request:**
```json
{
  "outcomeId": "YES"
}
```

## Trade API

### Buy Shares
Executes a trade to buy shares in a market.

- **URL:** `/v1/markets/{marketId}/trades`
- **Method:** `POST`
- **Path Parameters:**
    - `marketId`: ID of the market to trade in.
- **Headers:**
    - `userId` (Required): The ID of the user acting.
- **Request Body:** JSON

| Field | Type | Description | Required | Constraints |
|---|---|---|---|---|
| `outcome` | String | Outcome to buy ("YES" or "NO") | Yes | Case-insensitive |
| `amount` | Double | Investment amount (cost) | Yes | Must be > 0 |

**Example Request:**
```json
{
  "outcome": "YES",
  "amount": 10.0
}
```

## User API

### Create User
Registers a new user.

- **URL:** `/v1/users`
- **Method:** `POST`
- **Request Body:** JSON

| Field | Type | Description | Required | Constraints |
|---|---|---|---|---|
| `userId` | String | Unique user identifier | Yes | Cannot be empty |
| `email` | String | User email address | Yes | Valid email format |
| `password` | String | User password | Yes | Min 8 characters |

**Example Request:**
```json
{
  "userId": "jdoe",
  "email": "jdoe@example.com",
  "password": "securepassword123"
}
```

### Get All Users
Retrieves a list of all registered users (currently returns only IDs).

- **URL:** `/v1/users`
- **Method:** `GET`
- **Response Body:** Array of User objects.

**Response Structure:**
```json
[
  {
    "userId": "string"
  }
]
```
