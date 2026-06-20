# Environment Variables Reference

This document lists all environment variables used by the Shedlr Auth Service.

## Required Variables

These must be set for the application to start successfully.

| Variable | Description | Example |
| :--- | :--- | :--- |
| `SPRING_PROFILES_ACTIVE` | Active profile (dev, prod) | `dev` |
| `DB_URL` | JDBC URL for PostgreSQL | `jdbc:postgresql://db:5432/auth` |
| `DB_USERNAME` | Database username | `postgres` |
| `DB_PASSWORD` | Database password | `secret` |
| `JWT_SECRET_KEY` | HMAC SHA-256 Secret (256-bit min) | `replace_with_secure_key` |
| `MAIL_USERNAME` | SMTP login email | `your_email@example.com` |
| `MAIL_PASSWORD` | SMTP app password | `your_app_password` |
| `MAIL_FROM` | Outgoing 'From' address | `support@example.com` |
| `FRONTEND_BASE_URL` | Base URL of the Angular app | `http://localhost:4200` |

## Optional / Defaulted Variables

| Variable | Description | Default |
| :--- | :--- | :--- |
| `REDIS_HOST` | Redis server host | `localhost` |
| `REDIS_PORT` | Redis server port | `6379` |
| `ALLOWED_ORIGINS` | CORS Allowed Origins (CSV) | `http://localhost:4200` |
| `JWT_ACCESS_TOKEN_EXPIRATION` | Access token TTL (ms) | `3600000` (1h) |
| `JWT_REFRESH_TOKEN_EXPIRATION` | Refresh token TTL (ms) | `604800000` (7d) |
| `LOCKOUT_MAX_ATTEMPTS` | Failed login threshold | `5` |
| `LOCKOUT_DURATION` | Lockout length (minutes) | `15` |
| `ZIPKIN_ENDPOINT` | Zipkin server URL | `http://localhost:9411/api/v2/spans` |

## Rate Limiting Variables

| Variable | Description | Default |
| :--- | :--- | :--- |
| `RATE_LIMIT_LOGIN_CAPACITY` | Login tokens | `5` |
| `RATE_LIMIT_LOGIN_PERIOD` | Login refill period | `1m` |
| `RATE_LIMIT_SIGNUP_CAPACITY` | Signup tokens | `3` |
| `RATE_LIMIT_SIGNUP_PERIOD` | Signup refill period | `1h` |
