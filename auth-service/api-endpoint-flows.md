# Shedlr Auth Service - API Endpoint Flows & Verification Guide

This document provides a complete guide on how to use the authentication microservice, the lifecycle of a user account, and how to verify that everything is working correctly in your production-ready environment.

---

## 1. Prerequisites & Setup

Before testing the endpoints, ensure your infrastructure is running:

1.  **Start Infrastructure:** Run the following command in the project root:
    ```bash
    docker-compose up -d
    ```
    *This starts PostgreSQL (Database), Redis (Rate Limiting), and Zipkin (Tracing).*

2.  **Start the Application:** Run the Spring Boot app from your IDE or via terminal:
    ```bash
    ./mvnw spring-boot:run
    ```

3.  **Access Documentation:** Open your browser and go to:
    - **Swagger UI:** [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
    - **Zipkin (Tracing):** [http://localhost:9411](http://localhost:9411)

---

## 2. Authentication Flow (The Happy Path)

### Phase 1: Registration (`/signup`)
- **Endpoint:** `POST /api/v1/auth/signup`
- **Body:** `{ "email": "user@example.com", "password": "Password123!", "firstName": "John", "lastName": "Doe" }`
- **What happens:**
    1.  Account is created with status `PENDING`.
    2.  A unique **Verification Token** is generated.
    3.  An email is "sent" (check application logs to see the token).
- **Verification:** Check the `user_account` table in pgAdmin. Status should be `PENDING`.

### Phase 2: Email Verification (`/verify-email`)
- **Endpoint:** `POST /api/v1/auth/verify-email`
- **Body:** `{ "token": "TOKEN_FROM_LOGS" }`
- **What happens:**
    1.  Token is validated.
    2.  User status changes to `ACTIVE`.
- **Verification:** Attempting to login *before* this step will fail with a `403 Forbidden` or a message saying the account is not verified.

### Phase 3: Login (`/login`)
- **Endpoint:** `POST /api/v1/auth/login`
- **Body:** `{ "email": "user@example.com", "password": "Password123!" }`
- **What happens:**
    1.  Credentials verified using Argon2id.
    2.  **Access Token** (short-lived) and **Refresh Token** (long-lived) are returned.
    3.  A `UserSession` is created in the database.
- **Verification:** You will receive a JSON response with `accessToken` and `refreshToken`.

---

## 3. Account Recovery Flow

### Step 1: Request Reset (`/forgot-password`)
- **Endpoint:** `POST /api/v1/auth/forgot-password`
- **Body:** `{ "email": "user@example.com" }`
- **What happens:** Generates a reset token and logs it (simulating an email).

### Step 2: Reset Password (`/reset-password`)
- **Endpoint:** `POST /api/v1/auth/reset-password`
- **Body:** `{ "token": "TOKEN_FROM_LOGS", "newPassword": "NewSecurePassword123!" }`
- **What happens:** Updates the password and revokes the reset token.

---

## 4. Session & Token Management

### Refresh Token Rotation (`/refresh`)
- **Endpoint:** `POST /api/v1/auth/refresh`
- **Body:** `{ "refreshToken": "YOUR_REFRESH_TOKEN" }`
- **Why?** Allows the app to stay logged in without asking for a password every 15 minutes.
- **Security:** Each time you refresh, the old refresh token is revoked and a new one is issued (Rotation).

### Logout (`/logout`)
- **Endpoint:** `POST /api/v1/auth/logout`
- **Header:** `Authorization: Bearer <accessToken>`
- **What happens:** Revokes **all** active sessions for the user. Very secure for production.

---

## 5. User Profile Management (Protected Endpoints)

*Note: These require the `Authorization: Bearer <accessToken>` header.*

1.  **Get My Profile:** `GET /api/v1/users/me`
    - Returns your personal details, roles, and workspace information.
2.  **Update Profile:** `PUT /api/v1/users/me`
    - Update your name or preferences.

---

## 6. How to Check Everything (Verification Tools)

### A. Rate Limiting (Bucket4j + Redis)
- **Test:** Try to login with the wrong password 6 times rapidly.
- **Expected Result:** On the 6th attempt, you should receive `429 Too Many Requests`.
- **Check Redis:** Run `docker exec -it shedlr-redis redis-cli keys "*"` to see the rate-limit buckets stored in Redis.

### B. Distributed Tracing (Zipkin)
- **Action:** Perform a few API calls.
- **Check:** Go to [http://localhost:9411](http://localhost:9411) and click "Run Query".
- **Insight:** You can see exactly how long each request took and if there were any internal bottlenecks.

### C. Database (PostgreSQL)
- **Tool:** pgAdmin 4 or DBeaver.
- **Tables to watch:**
    - `user_account`: Credential and status info.
    - `user_profile`: Personal details.
    - `user_session`: Active login sessions.
    - `flyway_schema_history`: Track DB versioning.

### D. Logs (Structured JSON)
- **Check:** Look at the console output.
- **Format:** The logs are in JSON format, making them ready for ELK Stack (Elasticsearch, Logstash, Kibana) in production.
- **Verification:** Look for `[DEBUG_LOG]` or token outputs during signup/forgot-password.

---

## 7. Summary Table

| Endpoint | Method | Auth Required | Purpose |
| :--- | :--- | :--- | :--- |
| `/api/v1/auth/signup` | POST | No | Create account |
| `/api/v1/auth/verify-email` | POST | No | Activate account |
| `/api/v1/auth/login` | POST | No | Get JWT tokens |
| `/api/v1/auth/refresh` | POST | No | Renew access token |
| `/api/v1/auth/logout` | POST | **Yes** | Invalidate sessions |
| `/api/v1/auth/forgot-password` | POST | No | Request reset token |
| `/api/v1/auth/reset-password` | POST | No | Set new password |
| `/api/v1/users/me` | GET | **Yes** | View own profile |
| `/api/v1/users/me` | PUT | **Yes** | Update own profile |
