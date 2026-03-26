# User Service

Backend service for user management in BiteTogether application.

## Features

- User registration and authentication via Firebase
- User profile management
- Avatar upload to Firebase Storage
- Friend management
- Notification settings
- JWT authentication

## Prerequisites

- Java 21+
- Maven 3.6+
- PostgreSQL 13+
- Firebase Project with Admin SDK
- Kafka (for messaging)

## Setup Instructions

### 1. Firebase Configuration

You need to set up Firebase Admin SDK credentials:

1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Select your project
3. Go to **Project Settings** → **Service Accounts**
4. Click **Generate New Private Key**
5. Save the downloaded JSON file as `firebase-service-account.json`
6. Place it in `src/main/resources/config/` directory

**Important:** This file contains sensitive credentials and should never be committed to version control.

For detailed Firebase setup instructions, see [Firebase Setup Guide](docs/FIREBASE_SETUP.md).

### 2. Database Configuration

Update `src/main/resources/application-database.yml` with your database credentials:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/user_db
    username: your_username
    password: your_password
```

### 3. Build the Project

```bash
./mvnw clean install
```

### 4. Run the Application

```bash
./mvnw spring-boot:run
```

Or run the JAR file:

```bash
java -jar target/user-service-0.0.1-SNAPSHOT.jar
```

## Environment Variables

You can override configuration using environment variables:

| Variable | Description | Default |
|----------|-------------|---------|
| `FIREBASE_CREDENTIALS_PATH` | Path to Firebase service account JSON | `config/firebase-service-account.json` |
| `FIREBASE_STORAGE_BUCKET` | Firebase Storage bucket name | `bitetogether-aplus.appspot.com` |
| `DB_URL` | Database connection URL | - |
| `DB_USERNAME` | Database username | - |
| `DB_PASSWORD` | Database password | - |

## API Documentation

Once the application is running, access the API documentation at:

- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`

## Project Structure

```
user-service/
├── src/
│   ├── main/
│   │   ├── java/com/bitetogether/user/
│   │   │   ├── configuration/       # Spring configurations
│   │   │   │   └── firebase/        # Firebase configuration
│   │   │   ├── controller/          # REST controllers
│   │   │   ├── service/             # Business logic
│   │   │   ├── repository/          # Data access layer
│   │   │   ├── model/               # Entity models
│   │   │   ├── dto/                 # Data transfer objects
│   │   │   └── exception/           # Custom exceptions
│   │   └── resources/
│   │       ├── application.yml      # Main configuration
│   │       ├── application-firebase.yml
│   │       ├── application-database.yml
│   │       └── config/              # Credentials (gitignored)
│   └── test/                        # Unit and integration tests
├── docs/                            # Documentation
│   └── FIREBASE_SETUP.md           # Firebase setup guide
└── pom.xml                         # Maven dependencies
```

## Security

- Firebase Admin SDK credentials are protected via `.gitignore`
- JWT tokens for API authentication
- Password encryption using BCrypt
- Input validation and sanitization

## Development

### Running Tests

```bash
./mvnw test
```

### Building Docker Image

```bash
docker build -t user-service:latest .
```

### Running with Docker Compose

```bash
docker-compose up -d
```

## Contributing

1. Create a feature branch
2. Make your changes
3. Run tests and ensure they pass
4. Submit a pull request

## License

Proprietary - BiteTogether Team
