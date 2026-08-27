# Secure Resource Booking System

A RESTful Resource Booking API built with:

- Java 17+
- Spring Boot 3.5
- Spring Security
- JWT authentication
- BCrypt password hashing
- Spring Data JPA / Hibernate
- MySQL / PostgreSQL / H2
- Maven

## 1. Features

### Authentication & RBAC
- `POST /auth/login` and `POST /api/auth/login`
- `POST /auth/register` and `POST /api/auth/register`
- JWT bearer-token authentication (Stateless)
- BCrypt password hashing
- Seed users created automatically:
  - **ADMIN**: `admin` / `Admin@123`
  - **USER**: `user` / `User@123`

### Roles & Permissions

**ADMIN**
- Full CRUD on resources (`POST`, `GET`, `PUT`, `DELETE` `/resources`)
- Full CRUD on reservations (`POST`, `GET`, `PUT`, `DELETE` `/admin/reservations`)
- View all reservations across all users
- Change reservation status (`PENDING`, `CONFIRMED`, `CANCELLED`)
- Assign/reassign reservations to any user

**USER**
- Read-only access to active resources (`GET /resources`)
- Create reservations for self (`POST /reservations`)
- View only own reservations (`GET /reservations`)
- Update only own reservations (`PUT /reservations/{id}`)
- Cancel only own reservations (`DELETE /reservations/{id}`)
- User identity is extracted directly from authenticated JWT (preventing user impersonation)

### Reservation Features
- Statuses: `PENDING`, `CONFIRMED`, `CANCELLED`
- Decimal price stored and calculated automatically (`price = hours * pricePerHour`)
- Validation: Start time in future, end time after start time, resource must be active
- Overlap prevention: Rejects overlapping bookings for `PENDING` and `CONFIRMED` statuses (`409 CONFLICT`)
- Filtering by `status`, `minPrice`, and `maxPrice`
- Pagination via `page` and `size` parameters
- Sorting via `sortBy` (`id`, `price`, `startTime`, `endTime`, `status`) and `direction` (`asc`, `desc`)

## 2. Prerequisites
- JDK 17 or higher
- Maven 3.9+
- MySQL 8+ or PostgreSQL 14+ (or in-memory H2)

## 3. Database Configuration

You can run the application with **MySQL**, **PostgreSQL**, or **H2**.

### Option A: MySQL (Default)
```sql
CREATE DATABASE resource_booking_db;
```
Environment variables:
```bash
export DB_URL="jdbc:mysql://localhost:3306/resource_booking_db?createDatabaseIfNotExist=true&serverTimezone=Asia/Kolkata"
export DB_USERNAME="root"
export DB_PASSWORD="your_password"
```

### Option B: PostgreSQL
```sql
CREATE DATABASE resource_booking_db;
```
Environment variables:
```bash
export DB_URL="jdbc:postgresql://localhost:5432/resource_booking_db"
export DB_USERNAME="postgres"
export DB_PASSWORD="your_password"
```

### Option C: In-Memory H2 (Zero Setup)
```bash
export DB_URL="jdbc:h2:mem:resource_booking_db"
export DB_USERNAME="sa"
export DB_PASSWORD=""
```

## 4. Run & Test

### Run Application:
```bash
mvn spring-boot:run
```
Base URL: `http://localhost:8080`

### Run Test Suite (100% Pass Rate):
```bash
mvn clean test
```

## 5. API Documentation
- **Swagger UI**: `http://localhost:8080/swagger-ui/index.html`
- **OpenAPI JSON**: `http://localhost:8080/v3/api-docs`
- **Postman Collection**: [`postman/Resource-Booking-API.postman_collection.json`](postman/Resource-Booking-API.postman_collection.json)

## 6. Seed Users

| Role | Username | Password |
|---|---|---|
| ADMIN | admin | Admin@123 |
| USER | user | User@123 |

Change these credentials before using the project outside testing.

## 6. Login

### Admin

```http
POST /api/auth/login
Content-Type: application/json

{
  "username": "admin",
  "password": "Admin@123"
}
```

### User

```http
POST /api/auth/login
Content-Type: application/json

{
  "username": "user",
  "password": "User@123"
}
```

Copy the returned JWT and send it with:

```text
Authorization: Bearer <TOKEN>
```

## 7. API endpoints

### Authentication

```text
POST /api/auth/register
POST /api/auth/login
```

### Resources

```text
GET    /api/resources
GET    /api/resources/{id}

ADMIN:
POST   /api/resources
PUT    /api/resources/{id}
DELETE /api/resources/{id}
```

By default, `GET /api/resources` returns active resources.

To include inactive resources:

```text
GET /api/resources?activeOnly=false
```

### User reservations

```text
POST   /api/reservations
GET    /api/reservations
GET    /api/reservations/{id}
PUT    /api/reservations/{id}
DELETE /api/reservations/{id}
```

### Admin reservations

```text
POST   /api/admin/reservations
GET    /api/admin/reservations
PUT    /api/admin/reservations/{id}
PUT    /api/admin/reservations/{id}/status
DELETE /api/admin/reservations/{id}
```

## 8. Create resource as ADMIN

```http
POST /api/resources
Authorization: Bearer <ADMIN_TOKEN>
Content-Type: application/json

{
  "name": "Training Room",
  "type": "ROOM",
  "description": "Room for technical training",
  "pricePerHour": 800.00,
  "active": true
}
```

## 9. Create reservation as USER

```http
POST /api/reservations
Authorization: Bearer <USER_TOKEN>
Content-Type: application/json

{
  "resourceId": 1,
  "startTime": "2026-09-10T10:00:00",
  "endTime": "2026-09-10T12:00:00"
}
```

The server calculates:

```text
price = hours × resource.pricePerHour
```

The user identity comes from the JWT, not from the request body. This prevents a user from creating a reservation on behalf of another user.

## 10. Filtering

Example:

```text
GET /api/reservations?status=PENDING&minPrice=500&maxPrice=2000
```

Admin:

```text
GET /api/admin/reservations?status=CONFIRMED&minPrice=500&maxPrice=5000
```

## 11. Pagination and sorting

Example:

```text
GET /api/reservations?page=0&size=5&sortBy=price&direction=desc
```

Allowed `sortBy` values:

```text
id
price
startTime
endTime
status
```

## 12. Error responses

Typical HTTP status codes:

- `200 OK` - successful read/update
- `201 CREATED` - successful creation
- `204 NO CONTENT` - successful delete
- `400 BAD REQUEST` - validation error
- `401 UNAUTHORIZED` - missing/invalid credentials
- `403 FORBIDDEN` - insufficient role/ownership
- `404 NOT FOUND` - resource/reservation not found
- `409 CONFLICT` - overlapping reservation/business conflict
- `500 INTERNAL SERVER ERROR` - unexpected server error

## 13. Project structure

```text
resource-booking-system/
├── pom.xml
├── README.md
└── src/
    ├── main/
    │   ├── java/com/example/booking/
    │   │   ├── ResourceBookingApplication.java
    │   │   ├── config/
    │   │   ├── controller/
    │   │   ├── dto/
    │   │   ├── entity/
    │   │   ├── exception/
    │   │   ├── repository/
    │   │   ├── security/
    │   │   └── service/
    │   └── resources/
    │       └── application.properties
    └── test/
        └── java/com/example/booking/
```

## 14. Testing with Postman

Recommended test sequence:

1. Login as `admin`.
2. Copy admin JWT.
3. Create a resource.
4. Login as `user`.
5. Copy user JWT.
6. Get resources.
7. Create a reservation.
8. Get user reservations.
9. Try accessing another user's reservation with the user token.
10. Login as admin and get all reservations.
11. Change a reservation from `PENDING` to `CONFIRMED`.
12. Test status/price filters.
13. Test page/size/sorting.
14. Try an overlapping reservation and verify `409 CONFLICT`.
15. Try admin-only endpoints using a USER token and verify `403 FORBIDDEN`.

## 15. Security notes

For production:

- Replace the default JWT secret.
- Do not keep seed passwords.
- Use environment variables or a secrets manager.
- Enable HTTPS.
- Add refresh-token handling if long-lived sessions are required.
- Add database-level constraints/locking for high-concurrency booking scenarios.


## 16. Admin reservation example

```http
POST /api/admin/reservations
Authorization: Bearer <ADMIN_TOKEN>
Content-Type: application/json

{
  "userId": 2,
  "resourceId": 1,
  "startTime": "2026-09-15T10:00:00",
  "endTime": "2026-09-15T12:00:00"
}
```

A USER cannot change another user's reservation or cancel it. Ownership is checked from the authenticated JWT username.
