# Users API

A small RESTful API to manage users, built with Spring Boot, MongoDB and JWT authentication. It has two roles (`USER` and `ADMIN`) and per-user ownership rules.

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

The app refuses to start if `JWT_SECRET` is missing. Generate a secret. It must be Base64 and at least 32 bytes:

```bash
openssl rand -base64 32
```

Set it before running the app.

Terminal:

```bash
export JWT_SECRET="paste-the-generated-value-here"
```

IntelliJ: create an **Application** run configuration for `users.demo.Main`, then add `JWT_SECRET=<value>` under **Modify options → Environment variables**.

Never commit a real secret to the repository. If one was ever committed, rotate it.

## Run

```bash
mvn spring-boot:run
```

Or run the `Main` class from IntelliJ. The API is served on `http://localhost:8080`.

## Tests

```bash
mvn test
```

The web tests need `app.jwt.secret` to resolve. The simplest way is a test-only file at `src/test/resources/application.properties`:

```properties
app.jwt.secret=dGVzdC1vbmx5LXNlY3JldC1rZXktMzItYnl0ZXMhISE=
```

That value is a fake key for tests. It lets `mvn test` run on any machine and in CI without the real secret.

## Authentication and authorization

### Roles

Every user has a `role`, either `USER` (the default) or `ADMIN`. The role is stored in Mongo and copied into the JWT as the `roles` claim at login. Spring maps it to the authority `ROLE_USER` or `ROLE_ADMIN`.

Registration never accepts a role, so nobody can make themselves an admin. The role is changed only through the admin endpoint below.

### Ownership

The JWT subject is the user's **id**. A user may read, update or delete only their own record. Admins may act on any record.

### Rules

- **Last admin protected.** The last remaining admin cannot be demoted or deleted.
- **No self-demotion.** An admin cannot remove their own admin role.
- **Unique email.** Updating a user to an email already used by someone else returns `409`.
- **Serialized changes.** `RoleService` methods are `synchronized`, so two simultaneous requests cannot both pass the last-admin check. This holds for one running instance only. With several instances, each has its own lock, and a database-level guard would be needed.

### Creating the first admin

Nobody is an admin at first. Set the role by hand in Mongo, then log in again to get a token that carries it:

```bash
docker exec -it mongo mongosh usersdb --eval 'db.Users.updateOne({email:"momo@test.com"},{$set:{role:"ADMIN"}})'
```

After that, admins can promote others through `PUT /users/{id}/role`.

## API

### Public endpoints

| Method | Path             | Description                        |
|--------|------------------|------------------------------------|
| POST   | `/auth/register` | Create an account (role `USER`)    |
| POST   | `/auth/login`    | Get a JWT token (valid for 1 hour) |

### Protected endpoints

All require the header `Authorization: Bearer <token>`.

| Method | Path                | Access           | Description          |
|--------|---------------------|------------------|----------------------|
| GET    | `/users`            | Admin            | List all users       |
| GET    | `/users/{id}`       | Admin or owner   | Get one user         |
| PUT    | `/users/{id}`       | Admin or owner   | Update name and email |
| DELETE | `/users/{id}`       | Admin or owner   | Delete a user        |
| PUT    | `/users/{id}/role`  | Admin            | Set role to `USER` or `ADMIN` |

### Status codes

- `401`: missing, invalid or expired token.
- `403`: valid token, but not an admin or not the owner.
- `404`: user not found (on role changes and updates).
- `409`: conflict. Email already used, self-demotion, or removing the last admin.

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

List users (admin only):

```bash
curl http://localhost:8080/users \
  -H "Authorization: Bearer <token>"
```

Change a role (admin only):

```bash
curl -X PUT http://localhost:8080/users/<id>/role \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{"role":"ADMIN"}'
```

A role change takes effect only after the target user logs in again. Their old token keeps the old role until it expires.

## Project structure

```
src/main/java/users/demo/
├── Main.java              Application entry point
├── User.java              Mongo document (with role)
├── UserRepository.java    Data access
├── UserController.java    /users endpoints
├── AuthController.java    /auth endpoints
├── RoleService.java       Role changes and deletes, with last-admin protection
└── SecurityConfig.java    Security rules, JWT beans, role mapping
src/test/java/users/demo/
├── UserControllerTest.java      Unit tests (Mockito)
├── UserControllerWebTest.java   Web tests for /users: security, ownership, roles
├── AuthControllerWebTest.java   Web tests for /auth
└── RoleServiceTest.java         Unit tests for the role rules
```

## Security notes

- Passwords are hashed with BCrypt and never returned in responses.
- Sessions are stateless and CSRF is disabled, because tokens travel in a header.
- Endpoint rules use `@PreAuthorize`, enabled by `@EnableMethodSecurity`.
- Role checks read the token, not the database. A demoted admin keeps access until the token expires (up to 1 hour).
- There are no refresh tokens and no revocation. A token works until it expires.

## Known limitations

- No input validation yet. Add `spring-boot-starter-validation` and enforce a minimum password length.
- `GET /users/{id}` returns `200` with an empty body for an unknown id. It should return `404`.
- The last-admin lock works for a single instance only.
- No password change flow.