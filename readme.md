# API Documentation

This document outlines the API endpoints for the Predictor Backend service. 

## Authentication

Most endpoints require a valid JWT. To obtain one, use the Google Auth endpoint.

- **Header:** `Authorization: Bearer <your_jwt_token>`

## Auth API

### Google Login
Exchanges a Google ID Token for an application access and refresh token.

- **URL:** `/v1/auth/google`
- **Method:** `POST`
- **Request Body:** 
  ```json
  {
    "tokenId": "string"
  }
  ```
- **Response Structure (TokenResponse):**
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
- **Request Body:** 
  ```json
  {
    "refreshToken": "string"
  }
  ```

### Get Current User Profile
Retrieves the profile of the currently authenticated user.

- **URL:** `/v1/auth/me`
- **Method:** `GET`
- **Response Structure (AuthUserResponse):**
  ```json
  {
    "userId": "string",
    "email": "string",
    "displayName": "string",
    "pictureUrl": "string",
    "balance": 1000.00
  }
  ```

## Market API

### Create Market
Creates a new prediction market.

- **URL:** `/v1/markets`
- **Method:** `POST`
- **Request Body:** JSON

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
- **Query Parameters:**
    - `status` (Optional): Filter markets by status (`OPEN`, `RESOLVED`).
- **Response Body:** Array of Market objects

**Response Object:**
```json
{
  "marketId": "string",
  "marketName": "string",
  "marketDescription": "string",
  "status": "OPEN", 
  "resolvedOutcome": "YES" // or null
}
```

### Get Market by ID
Retrieves details of a specific market.

- **URL:** `/v1/markets/{marketId}`
- **Method:** `GET`

### Resolve Market
Resolves a market with a specific outcome (YES or NO).

- **URL:** `/v1/markets/{marketId}/resolve`
- **Method:** `POST`
- **Request Body:** 
  ```json
  {
    "outcomeId": "YES"
  }
  ```

## Trade API

### Buy Shares
Executes a trade to buy shares in a market. Requires authentication.

- **URL:** `/v1/markets/{marketId}/trades`
- **Method:** `POST`
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
  "tradeId": 123,
  "sharesBought": 15.4,
  "cost": 10.0,
  "outcome": "YES"
}
```

## User API

### Get All Users
Retrieves a list of all registered users (IDs only).

- **URL:** `/v1/users`
- **Method:** `GET`

## Error Response Format

All error responses (4xx and 5xx) follow a consistent structure.

**Response Structure (ErrorResponse):**
```json
{
  "timestamp": "2026-03-15T18:44:36",
  "status": 401,
  "error": "Unauthorized",
  "message": "Invalid Google token"
}
```

## Testing

To run the automated tests for the application (including Auth integration tests), use the following Maven command:

```bash
mvn test
```

Or, to run a specific test class:

```bash
mvn test -Dtest=AuthIntegrationTest
```

