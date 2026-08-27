# Multigeneys Resource Booking System - Test Cases

The test suite in `ResourceBookingIntegrationTests` covers the assignment's authentication, RBAC, CRUD, ownership, validation, filtering, pagination, sorting, and booking-overlap requirements.

| # | Test case | Expected result |
|---|---|---|
| 1 | Login with valid ADMIN credentials | 200 + JWT + ADMIN role |
| 2 | Login with invalid password | 401 |
| 3 | Register a new user | 201 + JWT + USER role |
| 4 | Register duplicate username | 400 |
| 5 | BCrypt password storage | Password is encoded, not stored as plain text |
| 6 | Access resources without JWT | 401 |
| 7 | USER reads active resources | 200 |
| 8 | USER creates resource | 403 |
| 9 | ADMIN creates resource | 201 |
| 10 | ADMIN updates resource | 200 |
| 11 | ADMIN deletes resource | 204 |
| 12 | USER creates reservation | 201 |
| 13 | Reservation starts with PENDING status | PENDING |
| 14 | Reservation price is calculated from hourly rate | Correct decimal price |
| 15 | User identity is taken from authenticated JWT | Reservation belongs to logged-in user |
| 16 | End time before/equal to start time | 400 |
| 17 | Reservation uses non-existing resource | 404 |
| 18 | Reservation uses inactive resource | 409 |
| 19 | Overlapping PENDING/CONFIRMED reservation | 409 |
| 20 | Booking exactly when previous booking ends | 201 |
| 21 | USER views own reservations | 200 + own records only |
| 22 | USER views another user's reservation | 403 |
| 23 | ADMIN views all reservations | 200 + all records |
| 24 | USER updates own reservation | 200 |
| 25 | USER updates another user's reservation | 403 |
| 26 | USER cancels own reservation | 204 and status becomes CANCELLED |
| 27 | ADMIN changes reservation status | 200 |
| 28 | ADMIN updates reservation/user/resource | 200 |
| 29 | ADMIN deletes reservation | 204 |
| 30 | Filter by status | Matching records only |
| 31 | Filter by minimum price | Matching records only |
| 32 | Filter by maximum price | Matching records only |
| 33 | Filter with minPrice > maxPrice | 400 |
| 34 | Pagination with page/size | Correct page metadata/results |
| 35 | Invalid page or size | 400 |
| 36 | Sort by price ascending/descending | Correct order |
| 37 | Invalid sort field | 400 |
| 38 | Invalid sort direction | 400 |
| 39 | Invalid reservation status value | 400 |
| 40 | Malformed JSON request | 400 |
| 41 | USER accesses ADMIN reservation endpoints | 403 |
| 42 | Expired/invalid JWT | 401 |
| 43 | JWT is stateless | No server session required |
| 44 | Resource not found | 404 |
| 45 | Reservation not found | 404 |

## Run

From the project directory:

```bash
mvn clean test
```

The automated tests use an H2 in-memory database under the `test` profile, while the normal application remains configured for MySQL.
