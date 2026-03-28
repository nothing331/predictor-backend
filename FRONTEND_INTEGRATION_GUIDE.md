# Frontend Integration Guide

This document explains how a frontend application should integrate with the current Predictor backend.

It is written for a frontend engineer who needs to:

- call the REST API correctly
- implement authentication and token refresh
- connect to the SSE stream for live updates
- understand error handling, rate limits, and backend constraints
- know what the backend currently does and does not expose

This guide reflects the current implementation in this repository, not an idealized future API.

## 1. Backend Summary

This backend is a Spring Boot application with:

- JWT-based access tokens
- refresh-token rotation
- Google sign-in
- optional demo username/password auth
- public market read endpoints
- protected mutation endpoints
- SSE for market events
- PostgreSQL, Redis, Flyway, and Spring Security under the hood

The API is centered around binary prediction markets. Every market currently resolves to one of two outcomes:

- `YES`
- `NO`

Important integration fact:

- this backend is stateless for access tokens
- it does not use cookie-based auth
- it returns tokens in JSON bodies
- the frontend is responsible for attaching the access token on protected requests

## 2. Base URL and Versioning

All routes are versioned under `/v1`.

Example:

```text
https://api.example.com/v1/markets
```

Recommended frontend environment variables:

```env
VITE_API_BASE_URL=https://api.example.com
VITE_GOOGLE_CLIENT_ID=your-google-client-id
```

If you use Next.js:

```env
NEXT_PUBLIC_API_BASE_URL=https://api.example.com
NEXT_PUBLIC_GOOGLE_CLIENT_ID=your-google-client-id
```

## 3. Route Matrix

### Public routes

- `POST /v1/auth/google`
- `POST /v1/auth/refresh`
- `POST /v1/auth/logout`
- `POST /v1/auth/demo/register` if demo auth is enabled
- `POST /v1/auth/demo/login` if demo auth is enabled
- `GET /v1/markets`
- `GET /v1/markets/{marketId}`
- `GET /v1/stream/events`

### Protected routes

- `GET /v1/auth/me`
- `GET /v1/markets/{marketId}/me`
- `POST /v1/markets`
- `POST /v1/markets/{marketId}/resolve`
- `POST /v1/markets/{marketId}/trades`
- `GET /v1/users`
- `GET /v1/users/me/summary`
- `POST /v1/users/me/gift-claim`

Important authorization note:

- role-based access control is active (`USER` and `ADMIN` roles exist)
- any authenticated user can trade, but market creation and market resolution require the `ADMIN` role
- the frontend must verify the user's role before showing creation or resolution UI to avoid `403 Forbidden` responses

## 4. Authentication Model

There are two auth modes exposed by the backend:

- Google sign-in
- optional demo auth

### 4.1 Google sign-in flow

The intended browser flow is:

1. Frontend gets a Google ID token from Google Sign-In.
2. Frontend sends that ID token to `POST /v1/auth/google`.
3. Backend verifies the Google token.
4. Backend creates or updates the local user.
5. Backend returns:
   - an app access token
   - an app refresh token
   - access-token lifetime in seconds

Request:

```http
POST /v1/auth/google
Content-Type: application/json
```

```json
{
  "tokenId": "google-id-token-from-client"
}
```

Success response:

```json
{
  "accessToken": "jwt-access-token",
  "refreshToken": "opaque-refresh-token",
  "expiresInSeconds": 900
}
```

Notes:

- `expiresInSeconds` is the actual response field name
- the current default access-token lifetime is 900 seconds, which is 15 minutes
- the refresh token is a backend-issued opaque token, not a JWT

### 4.2 Demo auth flow

Demo auth is only available when `app.demo.auth.enabled=true`.

This is enabled in the `demo` profile and disabled by default in normal development config.

Register request:

```http
POST /v1/auth/demo/register
Content-Type: application/json
```

```json
{
  "username": "demo_user",
  "password": "demo_pass",
  "email": "demo@example.com"
}
```

Login request:

```http
POST /v1/auth/demo/login
Content-Type: application/json
```

```json
{
  "username": "demo_user",
  "password": "demo_pass"
}
```

Both endpoints return the same token shape:

```json
{
  "accessToken": "jwt-access-token",
  "refreshToken": "opaque-refresh-token",
  "expiresInSeconds": 900
}
```

### 4.3 Access token usage

Protected routes require:

```http
Authorization: Bearer <accessToken>
```

Example:

```http
GET /v1/auth/me
Authorization: Bearer eyJ...
```

The backend extracts the authenticated user from the JWT subject claim. The frontend should treat the access token as opaque and should not rely on decoding it for application state.

### 4.4 Refresh flow

When the access token expires or a protected call returns `401`, the frontend should:

1. call `POST /v1/auth/refresh`
2. replace both the access token and refresh token with the new values
3. retry the original request once

Request:

```http
POST /v1/auth/refresh
Content-Type: application/json
```

```json
{
  "refreshToken": "current-refresh-token"
}
```

Success response:

```json
{
  "accessToken": "new-access-token",
  "refreshToken": "new-refresh-token",
  "expiresInSeconds": 900
}
```

Very important:

- refresh tokens are rotated
- after a successful refresh, the old refresh token becomes invalid
- if you keep using the old refresh token, the backend will return `401`

This means your frontend must always overwrite the stored refresh token with the newest one.

### 4.5 Logout behavior

Logout request:

```http
POST /v1/auth/logout
Content-Type: application/json
```

```json
{
  "refreshToken": "current-refresh-token"
}
```

Success response:

- HTTP `204 No Content`

Important nuance:

- logout revokes the refresh token
- logout does not actively invalidate already-issued access tokens
- because access tokens are stateless JWTs, an existing access token may continue working until its expiry time

Frontend implication:

- on logout, clear local auth state immediately
- do not wait for the current access token to expire

### 4.6 Current user endpoint

Request:

```http
GET /v1/auth/me
Authorization: Bearer <accessToken>
```

Success response:

```json
{
  "userId": "google-sub-or-demo-user-id",
  "email": "user@example.com",
  "name": "Display Name",
  "pictureUrl": "https://...",
  "balance": 1000.00,
  "role": "USER",
  "giftAvailable": true,
  "nextGiftAt": null
}
```

Notes:

- the field is `name`, not `displayName`
- `balance` is returned as a JSON number
- `giftAvailable` tells the frontend whether the user can currently claim the 12-hour gift
- `nextGiftAt` is `null` when the gift is already claimable; otherwise it is the ISO-8601 timestamp when the next claim becomes available
- this is the best endpoint to hydrate the authenticated user after login or app reload

### 4.7 Gift claim endpoint

Request:

```http
POST /v1/users/me/gift-claim
Authorization: Bearer <accessToken>
```

Success response when the gift is claimed:

```json
{
  "balance": 1500.00,
  "claimedAmount": 500.00,
  "claimed": true,
  "lastClaimedAt": "2026-03-28T10:00:00Z",
  "nextGiftAt": "2026-03-28T22:00:00Z",
  "giftAvailable": false
}
```

Success response when the user is still on cooldown:

```json
{
  "balance": 1500.00,
  "claimedAmount": 0,
  "claimed": false,
  "lastClaimedAt": "2026-03-28T10:00:00Z",
  "nextGiftAt": "2026-03-28T22:00:00Z",
  "giftAvailable": false
}
```

Frontend notes:

- this endpoint is safe to call from a button click without a request body
- the backend does not throw a cooldown error; instead it returns `claimed: false`
- after a successful claim, immediately update local balance and gift timer from this response
- the gift amount is currently fixed at `500` and the cooldown is currently fixed at `12` hours

### 4.8 Account summary endpoint

Request:

```http
GET /v1/users/me/summary
Authorization: Bearer <accessToken>
```

Success response:

```json
{
  "userId": "user-1",
  "availableBalance": 1000.00,
  "recentMarkets": [
    {
      "marketId": "market-1",
      "marketName": "Will team A win?",
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

- `recentMarkets` contains up to 3 unique markets ordered by the user's latest trade time
- `availableBalance` is cash balance only and does not include unrealized position value
- projected payout fields are settlement payouts, not current sell/cash-out estimates
- use this endpoint for a compact account summary or portfolio card

## 5. Recommended Frontend Auth State

Minimum auth state:

- `accessToken`
- `refreshToken`
- `accessTokenExpiresAt`
- `user` (including `userId`, `email`, `role`, etc.)
- `isAuthenticated`

Recommended behavior:

1. On login success, store tokens and compute `accessTokenExpiresAt`.
2. Call `/v1/auth/me` to hydrate the app user.
3. If the landing experience needs balance and recent market summaries, call `/v1/users/me/summary`.
4. Attach `Authorization: Bearer <accessToken>` to protected requests.
5. On `401`, try one refresh attempt.
6. If refresh fails, clear auth state and redirect to login.

Reasonable token-storage options:

- in-memory only: safest against persistent XSS, but user loses session on full page reload
- localStorage: easiest, but more exposed to XSS
- sessionStorage: similar tradeoff, but not persistent across browser restarts

Because this backend currently returns tokens in JSON rather than setting `HttpOnly` cookies, the frontend must consciously choose a storage strategy. If security hardening becomes a priority later, migrating to cookie-based refresh tokens would be worth considering.

## 6. REST API Details

### 6.1 Create market

Request:

```http
POST /v1/markets
Authorization: Bearer <accessToken>
Content-Type: application/json
```

```json
{
  "name": "Will BTC hit 150k this year?",
  "description": "Binary market for year-end BTC price",
  "liquidity": 50.0,
  "category": "Crypto",
  "yesLabel": "Yes",
  "noLabel": "No"
}
```

Fields:

- `name`: required, non-blank
- `description`: optional
- `liquidity`: optional, must be greater than `0`
- `category`: optional, defaults to "General"
- `yesLabel`: optional, defaults to "Yes"
- `noLabel`: optional, defaults to "No"

Success response:

```json
{
  "status": "success",
  "message": "Market created successfully.",
  "marketId": "Will-BTC-hit-150k-this-year?-20260316-153000"
}
```

Important notes:

- `marketId` is generated from the market name plus a timestamp
- treat `marketId` as an opaque backend identifier
- if a market with the same name already exists, backend returns `409 Conflict`

Conflict response:

```json
{
  "error": "Market with this name already exists."
}
```

### 6.2 Get all markets

Request:

```http
GET /v1/markets
GET /v1/markets?status=OPEN
GET /v1/markets?status=RESOLVED
```

Success response:

```json
[
  {
    "marketId": "market-1",
    "marketName": "Will team A win?",
    "status": "OPEN",
    "resolvedOutcome": null,
    "category": "Sports",
    "outcomes": [
      {
        "outcomeId": "YES",
        "label": "Yes",
        "probability": 0.5
      },
      {
        "outcomeId": "NO",
        "label": "No",
        "probability": 0.5
      }
    ],
    "totalValue": 0.00
  }
]
```

Notes:

- `status` filter only meaningfully supports `OPEN` and `RESOLVED`
- invalid `status` values currently return an empty array rather than a validation error

### 6.3 Get market by ID

Request:

```http
GET /v1/markets/{marketId}
```

Success response:

```json
{
  "marketId": "market-1",
  "marketName": "Will team A win?",
  "status": "OPEN",
  "resolvedOutcome": null,
  "category": "Sports",
  "outcomes": [
    {
      "outcomeId": "YES",
      "label": "Yes",
      "probability": 0.5
    },
    {
      "outcomeId": "NO",
      "label": "No",
      "probability": 0.5
    }
  ],
  "totalValue": 0.00
}
```

If not found:

- HTTP `404`
- response body may be empty

### 6.4 Get market history

Request:

```http
GET /v1/markets/{marketId}/history
GET /v1/markets/{marketId}/history?limit=200
GET /v1/markets/{marketId}/history?from=2026-03-22T10:00:00Z&to=2026-03-22T12:00:00Z
```

Query params:

- `from`: optional inclusive ISO-8601 timestamp
- `to`: optional inclusive ISO-8601 timestamp
- `limit`: optional point limit, defaults to `200`

Success response:

```json
{
  "marketId": "market-1",
  "status": "OPEN",
  "points": [
    {
      "timestamp": "2026-03-22T10:00:00Z",
      "yesProbability": 0.5,
      "noProbability": 0.5,
      "eventType": "INITIAL"
    },
    {
      "timestamp": "2026-03-22T10:05:00Z",
      "yesProbability": 0.62,
      "noProbability": 0.38,
      "eventType": "TRADE",
      "tradeId": "123",
      "outcome": "YES",
      "sharesBought": 10.5,
      "cost": 25.4
    }
  ]
}
```

History semantics:

- points are returned in ascending timestamp order
- the first point is usually an `INITIAL` point at market creation with `0.5 / 0.5`
- `TRADE` points represent post-trade probabilities after replaying market state
- resolved markets may include a final `RESOLUTION` point with `1.0 / 0.0` or `0.0 / 1.0`

Frontend usage:

- use this endpoint as the source of truth for rendering the full graph on page load
- use SSE only to append new live points while the page is connected
- on reconnect, refetch this endpoint because SSE replay is not supported

### 6.5 Get current user's position for a market

Request:

```http
GET /v1/markets/{marketId}/me
Authorization: Bearer <accessToken>
```

Success response:

```json
{
  "userId": "user-1",
  "marketId": "market-1",
  "marketName": "Will team A win?",
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
      "tradeId": "123",
      "outcome": "NO",
      "sharesBought": 1.0,
      "cost": 4.50,
      "tradedAt": "2026-03-27T12:00:00Z"
    }
  ]
}
```

Frontend notes:

- this endpoint is the market-detail source of truth for the current user's exposure in that market
- it returns `200` with zeroed investment fields and `trades: []` if the user has not traded that market yet
- trade rows are returned newest first
- `projectedPayoutIfYes` and `projectedPayoutIfNo` are outcome payouts, not cash-out estimates
- `realizedPayout` and `realizedNetPnl` are only populated after market resolution
- use this endpoint together with `GET /v1/markets/{marketId}` and `GET /v1/markets/{marketId}/history` for a complete market detail page

### 6.6 Resolve market

Request:

```http
POST /v1/markets/{marketId}/resolve
Authorization: Bearer <accessToken>
Content-Type: application/json
```

```json
{
  "outcomeId": "YES"
}
```

Accepted values:

- `YES`
- `NO`

The backend uppercases before enum conversion, so lowercase values also work in practice.

Success response:

```json
{
  "status": "success",
  "message": "Market resolved successfully.",
  "marketId": "market-1",
  "resolvedOutcome": "YES"
}
```

### 6.7 Buy shares

Request:

```http
POST /v1/markets/{marketId}/trades
Authorization: Bearer <accessToken>
Content-Type: application/json
```

```json
{
  "outcome": "YES",
  "amount": 25
}
```

Request semantics:

- `outcome` must be `YES` or `NO`
- input is case-insensitive
- `amount` is the amount of money the user wants to spend
- the backend computes how many shares that amount buys under LMSR pricing

Success response:

```json
{
  "status": "success",
  "message": "Trade executed successfully.",
  "tradeId": "trade-uuid",
  "sharesBought": 12.345,
  "cost": 25,
  "outcome": "YES"
}
```

Frontend notes:

- the request is budget-based, not share-count-based
- `sharesBought` is computed by the backend
- use the returned `cost` and `sharesBought` for confirmation UI

### 6.8 Get users

Request:

```http
GET /v1/users
Authorization: Bearer <accessToken>
```

Success response:

```json
[
  {
    "userId": "user-1"
  }
]
```

Important note:

- this endpoint currently returns only `userId`
- it does not return names, emails, or profile images

### 6.9 Claim 12-hour gift

Request:

```http
POST /v1/users/me/gift-claim
Authorization: Bearer <accessToken>
```

Success response:

```json
{
  "balance": 1500.00,
  "claimedAmount": 500.00,
  "claimed": true,
  "lastClaimedAt": "2026-03-28T10:00:00Z",
  "nextGiftAt": "2026-03-28T22:00:00Z",
  "giftAvailable": false
}
```

Frontend notes:

- call this only for authenticated users
- if `claimed` is `false`, keep the claim button disabled and use `nextGiftAt` for the countdown
- if `claimed` is `true`, refresh local user state from this response or refetch `/v1/auth/me`

### 6.10 Get account summary

Request:

```http
GET /v1/users/me/summary
Authorization: Bearer <accessToken>
```

Success response:

```json
{
  "userId": "user-1",
  "availableBalance": 1000.00,
  "recentMarkets": [
    {
      "marketId": "market-1",
      "marketName": "Will team A win?",
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

Frontend notes:

- this is the backend's current account-summary endpoint
- it is intentionally limited to the 3 most recently traded unique markets
- use `currentYesChance` and `currentNoChance` directly for UI percentages
- resolved markets return settled certainties (`1.0/0.0` or `0.0/1.0`)

## 7. What the Backend Currently Does Not Expose

This part is especially important for frontend planning.

The current API does not expose:

- order history endpoints
- global raw trade list endpoints
- event replay for SSE

Practical impact:

- you can build market lists, auth flows, create/resolve actions, and buy actions
- you can display trade confirmations from mutation responses
- you can react to SSE event notifications
- you can reconstruct current market odds from the REST API responses since probabilities are returned
- you can build a lightweight account summary using `/v1/users/me/summary`
- you can build a 12-hour reward button and countdown using `/v1/auth/me` plus `/v1/users/me/gift-claim`
- you can build a per-market user trade/investment panel using `/v1/markets/{marketId}/me`
- you still cannot build a full cross-market trade ledger or complete portfolio history from public API alone

The backend now does expose a graph-oriented market history endpoint:

- `GET /v1/markets/{marketId}/history`

Important nuance:

- this is probability-history for charting
- it is not a general raw trade-history feed
- if the frontend later needs a cross-market activity feed, a separate trade-list endpoint would still be appropriate

## 8. SSE Integration

### 8.1 SSE endpoint

Request:

```http
GET /v1/stream/events
Accept: text/event-stream
```

Optional filtered request:

```http
GET /v1/stream/events?marketId=market-1
Accept: text/event-stream
```

Behavior:

- connection stays open
- server pushes named events
- clients can subscribe to all markets or one market
- heartbeat comments are sent every 15 seconds

### 8.2 Event names

The backend emits these SSE event names:

- `MarketCreated`
- `TradeExecuted`
- `MarketResolved`

### 8.3 Event payload shape

Every event payload follows this general shape:

```json
{
  "eventId": "uuid",
  "type": "TradeExecuted",
  "occurredAt": "2026-03-16T10:20:30Z",
  "marketId": "market-1",
  "payload": {}
}
```

#### MarketCreated payload

```json
{
  "eventId": "uuid",
  "type": "MarketCreated",
  "occurredAt": "2026-03-16T10:20:30Z",
  "marketId": "market-1",
  "payload": {
    "marketName": "Will team A win?"
  }
}
```

#### TradeExecuted payload

```json
{
  "eventId": "uuid",
  "type": "TradeExecuted",
  "occurredAt": "2026-03-16T10:20:30Z",
  "marketId": "market-1",
  "payload": {
    "tradeId": "trade-uuid",
    "userId": "user-1",
    "outcome": "YES",
    "shareCount": 12.345,
    "cost": 25,
    "yesProbability": 0.62,
    "noProbability": 0.38,
    "qYes": 42.5,
    "qNo": 26.0,
    "status": "OPEN"
  }
}
```

TradeExecuted notes:

- `yesProbability` and `noProbability` are the post-trade probabilities
- these values are intended for live graph updates
- `qYes` and `qNo` are also post-trade values
- `occurredAt` should be used as the x-axis timestamp for the live graph point

#### MarketResolved payload

```json
{
  "eventId": "uuid",
  "type": "MarketResolved",
  "occurredAt": "2026-03-16T10:20:30Z",
  "marketId": "market-1",
  "payload": {
    "outcomeId": "YES"
  }
}
```

### 8.4 Browser implementation notes

For a normal browser frontend, the easiest client is native `EventSource`.

Example:

```ts
const es = new EventSource(`${API_BASE_URL}/v1/stream/events`);
```

If you want market-specific events:

```ts
const es = new EventSource(
  `${API_BASE_URL}/v1/stream/events?marketId=${encodeURIComponent(marketId)}`
);
```

Listen by event name:

```ts
es.addEventListener("TradeExecuted", (event) => {
  const data = JSON.parse((event as MessageEvent).data);
});
```

### 8.5 Important auth nuance with EventSource

Native browser `EventSource` does not let you attach an `Authorization` header.

That matters less here because:

- this SSE endpoint is public
- backend permits anonymous connections
- rate limiting falls back to IP if no authenticated principal is present

Frontend implication:

- if you use native browser `EventSource`, expect the SSE connection to be anonymous
- do not design the current SSE stream as a private user-specific channel

If private authenticated SSE is ever needed later, you would need one of these:

- cookie-based auth
- a fetch-based SSE polyfill that supports headers
- a different transport such as WebSockets

### 8.6 Reconnect and state recovery

This backend explicitly expects the client recovery rule to be:

1. on reconnect, fetch REST snapshots first
2. then reopen the stream

Why this matters:

- the backend does not implement replay from `Last-Event-ID`
- events contain `eventId`, but the server does not use them for resume support
- if the client disconnects, missed events are not replayed automatically

Recommended reconnect strategy:

1. detect `error` or closed connection
2. wait with backoff
3. refetch current REST state
4. reopen the stream

Suggested backoff sequence:

- 1 second
- 2 seconds
- 5 seconds
- 10 seconds
- 15 seconds max

### 8.7 What SSE is good for in this app

Use SSE to:

- refresh market lists when a market is created
- append a live graph point when a trade happens
- refresh a market detail page if other UI elements depend on fields not present in SSE
- switch a market to resolved state when it resolves
- show lightweight real-time notifications

Recommended graph pattern:

1. fetch `GET /v1/markets/{marketId}/history`
2. render the chart from `points`
3. open `GET /v1/stream/events?marketId={marketId}`
4. on `TradeExecuted`, append a new chart point from SSE payload
5. on reconnect, refetch history before reopening SSE

Example:

```ts
const history = await fetch(`${API_BASE_URL}/v1/markets/${marketId}/history`);
const initial = await history.json();

setPoints(initial.points);

const es = new EventSource(
  `${API_BASE_URL}/v1/stream/events?marketId=${encodeURIComponent(marketId)}`
);

es.addEventListener("TradeExecuted", (event) => {
  const data = JSON.parse((event as MessageEvent).data);

  setPoints((prev) => [
    ...prev,
    {
      timestamp: data.occurredAt,
      yesProbability: data.payload.yesProbability,
      noProbability: data.payload.noProbability,
      eventType: "TRADE"
    }
  ]);
});
```

Do not assume SSE alone gives you enough data to derive complete market state. For example, trade events tell you that a trade happened, but they do not include a full market snapshot or current probabilities.

## 9. Error Handling

The backend has two different error-response styles, and the frontend should handle both.

### 9.1 Standard API error shape

Most application errors use:

```json
{
  "timestamp": "2026-03-16T18:44:36",
  "status": 400,
  "error": "Bad Request",
  "message": "Invalid outcome: MAYBE"
}
```

Common cases:

- invalid business action
- invalid Google token
- refresh token expired
- illegal trade input
- rate limiting through the global exception path
- generic internal server errors

### 9.2 Security 401 shape

Missing or invalid access tokens on protected routes return a different shape:

```json
{
  "status": 401,
  "error": "Unauthorized"
}
```

Important frontend note:

- do not assume every error response has `message`
- for `401`, key off the HTTP status first

### 9.3 Validation edge case

The backend currently does not add a custom handler for Spring validation errors like `MethodArgumentNotValidException`.

That means malformed or invalid request bodies may return Spring Boot's default validation response rather than the custom `ErrorResponse` shape.

Frontend implication:

- always treat HTTP status as the primary source of truth
- parse error JSON defensively
- support unknown error shapes

## 10. Rate Limits

Current configured limits:

- trade-related protected mutation routes: `30` requests per `60` seconds
- SSE connection attempts: `20` requests per `60` seconds

Important details:

- the rate limiter uses Redis
- if Redis is unavailable, rate limiting is configured to fail closed
- fail-closed means requests can be rejected with `429` even when the user has not truly exceeded quota

What the frontend should do:

- on `429`, show a user-friendly retry message
- back off before retrying
- avoid aggressive reconnect loops for SSE
- debounce or disable rapid repeat trade submissions

Good trade UX:

- disable the buy button while a trade request is in flight
- prevent double-submission
- surface the server error message when available

## 11. CORS and Deployment Notes

This codebase now includes explicit CORS configuration.

Current behavior:

- allowed origins come from backend config
- local default is `http://localhost:5173`
- allowed methods include `GET`, `POST`, `PUT`, `PATCH`, `DELETE`, and `OPTIONS`
- allowed headers include `Authorization` and `Content-Type`
- credentials are disabled because auth is bearer-token based, not cookie based

For production:

- the deployed frontend origin must be added to the backend allowlist
- this is configured via backend environment/config, not in frontend code

Frontend developer takeaway:

- if the frontend origin changes, the backend CORS allowlist must be updated
- if you see browser CORS failures from a new origin, that is typically a backend deploy/config issue, not a frontend bug

At minimum, protected cross-origin requests will need `Authorization` allowed in CORS headers.

## 12. Recommended Frontend Data Layer Design

A clean frontend architecture would separate:

- `authClient`
- `apiClient`
- `sseClient`
- query/state hooks or stores

### `authClient` responsibilities

- Google sign-in exchange
- demo login/register if used
- token storage
- refresh
- logout
- current-user bootstrap

### `apiClient` responsibilities

- base URL
- JSON parsing
- bearer token injection
- single retry on refresh after `401`
- normalized error handling

### `sseClient` responsibilities

- open stream
- subscribe by event type
- reconnect with backoff
- trigger REST refetch after reconnect

## 13. Suggested TypeScript Shapes

These are good frontend-side types based on the current API.

```ts
export type TokenResponse = {
  accessToken: string;
  refreshToken: string;
  expiresInSeconds: number;
};
```

```ts
export type AuthUser = {
  userId: string;
  email: string | null;
  name: string | null;
  pictureUrl: string | null;
  balance: number;
};
```

```ts
export type MarketSummary = {
  marketId: string;
  marketName: string;
  status: "OPEN" | "RESOLVED";
  resolvedOutcome: "YES" | "NO" | null;
  category: string;
  totalValue: number;
  outcomes: Array<{
    outcomeId: "YES" | "NO";
    label: string;
    probability: number;
  }>;
};
```

```ts
export type MarketHistoryPoint = {
  timestamp: string;
  yesProbability: number;
  noProbability: number;
  eventType: "INITIAL" | "TRADE" | "RESOLUTION";
  tradeId?: string;
  outcome?: "YES" | "NO";
  sharesBought?: number;
  cost?: number;
};
```

```ts
export type MarketHistoryResponse = {
  marketId: string;
  status: "OPEN" | "RESOLVED";
  points: MarketHistoryPoint[];
};
```

```ts
export type TradeResponse = {
  status: string;
  message: string;
  tradeId: string;
  sharesBought: number;
  cost: number;
  outcome: "YES" | "NO";
};
```

```ts
export type DomainEvent =
  | {
      eventId: string;
      type: "MarketCreated";
      occurredAt: string;
      marketId: string;
      payload: { marketName: string };
    }
  | {
      eventId: string;
      type: "TradeExecuted";
      occurredAt: string;
      marketId: string;
      payload: {
        tradeId: string;
        userId: string;
        outcome: "YES" | "NO";
        shareCount: number;
        cost: number;
        yesProbability: number;
        noProbability: number;
        qYes: number;
        qNo: number;
        status: "OPEN" | "RESOLVED";
      };
    }
  | {
      eventId: string;
      type: "MarketResolved";
      occurredAt: string;
      marketId: string;
      payload: { outcomeId: "YES" | "NO" };
    };
```

## 14. Recommended Request Patterns

### App startup

1. Load tokens from storage.
2. If no refresh token exists, show signed-out state.
3. If access token exists, try `/v1/auth/me`.
4. If that fails with `401`, try refresh.
5. If refresh succeeds, retry `/v1/auth/me`.
6. If refresh fails, clear auth state.

### After login

1. Receive token response.
2. Save access token and refresh token.
3. Fetch `/v1/auth/me`.
4. Use `giftAvailable` and `nextGiftAt` from `/v1/auth/me` to initialize the reward UI.
5. Navigate into the app.
6. Optionally open SSE stream after authenticated app bootstrap.

### Gift claim flow

1. Read `giftAvailable` and `nextGiftAt` from `/v1/auth/me`.
2. If `giftAvailable` is `false`, disable the claim button and render a countdown to `nextGiftAt`.
3. If `giftAvailable` is `true`, enable the claim button.
4. On click, call `POST /v1/users/me/gift-claim`.
5. Update displayed balance, button state, and countdown from the response.

### Market list page

1. Fetch `/v1/markets`.
2. Open SSE stream.
3. On `MarketCreated`, refetch the market list.
4. On `MarketResolved`, refetch affected market or market list.
5. On reconnect, refetch then reopen stream.

### Market detail page

1. Fetch `/v1/markets/{marketId}` for card/detail metadata.
2. Fetch `/v1/markets/{marketId}/history` for the initial graph.
3. Open `/v1/stream/events?marketId={marketId}`.
4. On `TradeExecuted`, append a graph point directly from SSE payload.
5. On `MarketResolved`, refetch market detail and history or append a final resolved point based on product needs.
6. On reconnect, refetch history before reopening the stream.

## 15. Known Contract Quirks and Sharp Edges

These are easy places for frontend confusion.

- Token response field is `expiresInSeconds`, not `expiresIn`.
- `/v1/auth/me` returns `name`, not `displayName`.
- `/v1/auth/me` also returns `giftAvailable` and `nextGiftAt`; the reward UI should use those rather than trying to calculate cooldown client-side.
- `/v1/users` currently returns only `userId`.
- `/v1/markets` and `/v1/markets/{id}` return outcome probabilities and total traded value, but they are still summary-oriented responses rather than full chart history.
- `/v1/markets/{marketId}/history` is the graph source of truth.
- `GET /v1/markets/{id}` may return a bare `404` with empty body.
- Protected-route `401` responses do not use the full standard error structure.
- Native browser `EventSource` cannot attach bearer auth headers.
- Refresh tokens rotate and must always be replaced client-side.
- Logout revokes refresh token, not existing access JWTs.
- `POST /v1/users/me/gift-claim` returns `200` even during cooldown; use the `claimed` boolean to distinguish success from no-op.
- Any authenticated user can trade and list users, but only `ADMIN` users can create or resolve markets.

## 16. Backend Changes the Frontend May Soon Need

If the frontend roadmap includes a richer trading product, these are the most likely backend additions worth requesting:

- endpoint for a user's positions and holdings
- endpoint for raw trade history / activity feed
- endpoint for a single user's portfolio summary
- admin/owner authorization around market creation and resolution
- standardized validation error payloads
- optional cookie-based refresh token flow
- SSE replay or snapshot/version support

## 17. Frontend Delivery Checklist

A frontend engineer should confirm all of the following before calling integration complete:

- base API URL is environment-driven
- Google client ID is environment-driven
- access token is attached to protected routes
- token refresh overwrites both tokens
- failed refresh clears auth state
- `/v1/auth/me` is used to hydrate the user
- reward button state is driven by `giftAvailable` and `nextGiftAt`
- reward claim flow handles both `claimed: true` and `claimed: false`
- logout clears local state immediately
- SSE reconnect uses backoff
- REST state is refetched on SSE reconnect
- `429` is handled gracefully
- UI does not assume all errors include `message`
- UI does not assume market responses include pricing or positions
- deployment plan includes backend CORS configuration

## 18. Final Recommendation

If you are building the frontend now, the most reliable approach is:

- use REST as the source of truth
- use SSE as an invalidation and notification channel
- keep auth handling centralized in one client module
- design the UI around the data the backend actually returns today

For the current backend, that means the frontend can confidently implement:

- sign-in
- session persistence with refresh
- current-user bootstrap
- 12-hour gift claim UI
- market list
- market detail summary
- create market
- resolve market
- buy shares
- live event reactions

But if you want a polished trading UI with positions, prices, and portfolio analytics, you should expect at least a small second round of backend API additions.
