# Configuration Guide

## Overview

The Shedlr Auth Service uses a profile-based configuration architecture. 
Sensitive information is NEVER stored in code or YAML files. All secrets must be provided via environment variables.

## Configuration Profiles

- **default**: Base configuration in `application.yaml`.
- **dev**: Development settings in `application-dev.yml` (Verbose logging, SQL formatting).
- **prod**: Production settings in `application-prod.yml` (Strict security, optimized logging).

## Local Setup

1. **Environment Variables**:
   - Copy `.env.example` to `.env`.
   - Update the values in `.env` for your local environment.
   - Note: The application does NOT automatically load the `.env` file. You must ensure the variables in `.env` are exported to your environment (e.g., using `source .env` or IDE plugins like "EnvFile").

2. **Infrastructure**:
   - Ensure PostgreSQL is running and the database specified in `DB_URL` exists.
   - Ensure Redis is running on `REDIS_HOST` and `REDIS_PORT`.

3. **Running the App**:
   - Run the application with `-Dspring.profiles.active=dev`.
   - In IntelliJ IDEA, use the "EnvFile" plugin or manually set Environment Variables in the Run Configuration.

## Production Configuration

In production, secrets should be managed via your platform's native secret management (e.g., Kubernetes Secrets, AWS Secrets Manager, GitHub Secrets).

1. Set `SPRING_PROFILES_ACTIVE=prod`.
2. Provide all required variables as environment variables (the `.env` file is NOT recommended for production).
3. Ensure the environment variables match those listed in [Environment Variables Guide](./ENVIRONMENT_VARIABLES.md).
