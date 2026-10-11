# Users API

A small RESTful API to manage users, built with Spring Boot, MongoDB and JWT authentication.

## Tech stack

- Java 25
- Spring Boot (Web, Data MongoDB, Security, OAuth2 Resource Server)
- MongoDB
- JWT (HS256), validated by Spring Security
- JUnit 5, Mockito, MockMvc

## Prerequisites

- JDK 25
- Maven 3.9+
- MongoDB running on `localhost:27017`

Start MongoDB with Docker:

```bash
docker run -d --name mongo -p 27017:27017 mongo:latest
```

## Configuration

Create `src/main/resources/application.properties`:

```properties
server.port=8080
spring.data.mongodb.uri=mongodb://localhost:27017/usersdb
app.jwt.secret=${JWT_SECRET}
```

Generate a secret. It must be Base64 and at least 32 bytes:

```bash
openssl rand -base64 32
```

Export it before running the app:

```bash
export JWT_SECRET="paste-the-generated-value-here"
```

Never commit a real secret to the repository.

## Run

```bash
mvn spring-boot:run
```

Or run the `Main` class from IntelliJ. The API is served on `http://localhost:8080`.

## Tests

```bash
mvn test
```

The web tests need `app.jwt.secret` to resolve. Set `JWT_SECRET` in your environment, or add a test properties file at `src/test/resources/application.properties` with a test-only Base64 value.

## API

### Public endpoints

| Method | Path             | Description                       |
|--------|------------------|-----------------------------------|
| POST   | `/auth/register` | Create an account                 |
| POST   | `/auth/login`    | Get a JWT token (valid for 1 hour) |

### Protected endpoints

All require the header `Authorization: Bearer <token>`.

| Method | Path          | Description        |
|--------|---------------|--------------------|
| GET    | `/users`      | List all users     |
| GET    | `/users/{id}` | Get one user       |
| PUT    | `/users/{id}` | Update name, email |
| DELETE | `/users/{id}` | Delete a user      |

Missing, invalid or expired tokens return `401`.

### Examples

Register:

```bash
curl -X POST http://localhost:8080/auth/register \
  -H "Content-Type: application/json" \
  -d '{"nom":"Momo","email":"momo@test.com","password":"secret123"}'
```

Login:

```bash
curl -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"momo@test.com","password":"secret123"}'
```

Response:

```json
{ "token": "eyJhbGciOi..." }
```

List users:

```bash
curl http://localhost:8080/users \
  -H "Authorization: Bearer <token>"
```

## Project structure

```
src/main/java/users/demo/
├── Main.java              Application entry point
├── User.java              Mongo document
├── UserRepository.java    Data access
├── UserController.java    /users endpoints
├── AuthController.java    /auth endpoints
└── SecurityConfig.java    Security rules and JWT beans
src/test/java/users/demo/
├── UserControllerTest.java      Unit tests (Mockito)
├── UserControllerWebTest.java   Web tests for /users
└── AuthControllerWebTest.java   Web tests for /auth
```

## Security notes

- Passwords are hashed with BCrypt and never returned in responses.
- Sessions are stateless and CSRF is disabled, because tokens travel in a header.
- There are no refresh tokens and no revocation. A token works until it expires.

## Known limitations

- `GET /users/{id}` returns `200` with an empty body for an unknown id. It should return `404`.
- `PUT /users/{id}` throws a `RuntimeException` for an unknown id, which gives `500`. It should return `404`.
- No input validation yet.
- Every authenticated user can read, edit and delete every other user. There are no roles.
