package com.example.booking;

import com.example.booking.entity.Reservation;
import com.example.booking.entity.ReservationStatus;
import com.example.booking.entity.Resource;
import com.example.booking.entity.Role;
import com.example.booking.entity.User;
import com.example.booking.repository.ReservationRepository;
import com.example.booking.repository.ResourceRepository;
import com.example.booking.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ResourceBookingIntegrationTests {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private ResourceRepository resourceRepository;
    @Autowired private ReservationRepository reservationRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private String adminToken;
    private String userToken;
    private String secondUserToken;
    private Long resourceId;
    private Long secondResourceId;
    private Long inactiveResourceId;
    private Long firstUserId;
    private Long secondUserId;

    @BeforeEach
    void setUp() throws Exception {
        reservationRepository.deleteAll();
        resourceRepository.deleteAll();
        userRepository.deleteAll();

        User adminUser = userRepository.save(new User("admin", passwordEncoder.encode("Admin@123"), Role.ADMIN));
        User user1 = userRepository.save(new User("user", passwordEncoder.encode("User@123"), Role.USER));
        User user2 = userRepository.save(new User("user2", passwordEncoder.encode("User2@123"), Role.USER));

        firstUserId = user1.getId();
        secondUserId = user2.getId();

        Resource room = resourceRepository.save(new Resource(
                "Training Room", "ROOM", "Room for technical training", new BigDecimal("800.00"), true));
        Resource car = resourceRepository.save(new Resource(
                "Company Car", "VEHICLE", "Sedan for travel", new BigDecimal("1200.00"), true));
        Resource projector = resourceRepository.save(new Resource(
                "Projector Inactive", "EQUIPMENT", "Maintenance", new BigDecimal("300.00"), false));

        resourceId = room.getId();
        secondResourceId = car.getId();
        inactiveResourceId = projector.getId();

        adminToken = login("admin", "Admin@123");
        userToken = login("user", "User@123");
        secondUserToken = login("user2", "User2@123");
    }

    private String login(String username, String password) throws Exception {
        String response = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token", not(isEmptyOrNullString())))
                .andReturn().getResponse().getContentAsString();
        return com.jayway.jsonpath.JsonPath.read(response, "$.token");
    }

    private String future(int days, int hour) {
        return LocalDateTime.now().plusDays(days).withHour(hour).withMinute(0).withSecond(0).withNano(0).toString();
    }

    // 1. Login with valid ADMIN credentials -> 200 + JWT + ADMIN role
    @Test
    @DisplayName("01: Login with valid ADMIN credentials")
    void test01_loginWithValidAdminCredentials() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"Admin@123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token", not(isEmptyOrNullString())))
                .andExpect(jsonPath("$.role").value("ADMIN"))
                .andExpect(jsonPath("$.username").value("admin"));
    }

    // 2. Login with invalid password -> 401
    @Test
    @DisplayName("02: Login with invalid password")
    void test02_loginWithInvalidPassword() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"WrongPassword@123\"}"))
                .andExpect(status().isUnauthorized());
    }

    // 3. Register a new user -> 201 + JWT + USER role
    @Test
    @DisplayName("03: Register a new user")
    void test03_registerNewUser() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"john_doe\",\"password\":\"Secure@123\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token", not(isEmptyOrNullString())))
                .andExpect(jsonPath("$.role").value("USER"))
                .andExpect(jsonPath("$.username").value("john_doe"));
    }

    // 4. Register duplicate username -> 400
    @Test
    @DisplayName("04: Register duplicate username")
    void test04_registerDuplicateUsername() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"NewAdminPass@123\"}"))
                .andExpect(status().isBadRequest());
    }

    // 5. BCrypt password storage -> Password is encoded, not plain text
    @Test
    @DisplayName("05: BCrypt password storage")
    void test05_bcryptPasswordStorage() {
        User user = userRepository.findByUsername("admin").orElseThrow();
        assertNotEquals("Admin@123", user.getPassword());
        assertTrue(user.getPassword().startsWith("$2a$") || user.getPassword().startsWith("$2b$"));
        assertTrue(passwordEncoder.matches("Admin@123", user.getPassword()));
    }

    // 6. Access resources without JWT -> 401
    @Test
    @DisplayName("06: Access resources without JWT")
    void test06_accessResourcesWithoutJwt() throws Exception {
        mockMvc.perform(get("/api/resources"))
                .andExpect(status().isUnauthorized());
    }

    // 7. USER reads active resources -> 200
    @Test
    @DisplayName("07: USER reads active resources")
    void test07_userReadsActiveResources() throws Exception {
        mockMvc.perform(get("/api/resources")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].active").value(true))
                .andExpect(jsonPath("$[1].active").value(true));
    }

    // 8. USER creates resource -> 403
    @Test
    @DisplayName("08: USER creates resource")
    void test08_userCreatesResourceForbidden() throws Exception {
        mockMvc.perform(post("/api/resources")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"User Room\",\"type\":\"ROOM\",\"pricePerHour\":500.0}"))
                .andExpect(status().isForbidden());
    }

    // 9. ADMIN creates resource -> 201
    @Test
    @DisplayName("09: ADMIN creates resource")
    void test09_adminCreatesResource() throws Exception {
        mockMvc.perform(post("/api/resources")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Board Room\",\"type\":\"ROOM\",\"description\":\"Executive Boardroom\",\"pricePerHour\":1500.00,\"active\":true}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.name").value("Board Room"))
                .andExpect(jsonPath("$.active").value(true));
    }

    // 10. ADMIN updates resource -> 200
    @Test
    @DisplayName("10: ADMIN updates resource")
    void test10_adminUpdatesResource() throws Exception {
        mockMvc.perform(put("/api/resources/" + resourceId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Updated Training Room\",\"type\":\"ROOM\",\"description\":\"Upgraded\",\"pricePerHour\":950.00,\"active\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Training Room"))
                .andExpect(jsonPath("$.pricePerHour").value(950.00));
    }

    // 11. ADMIN deletes resource -> 204
    @Test
    @DisplayName("11: ADMIN deletes resource")
    void test11_adminDeletesResource() throws Exception {
        mockMvc.perform(delete("/api/resources/" + resourceId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/resources/" + resourceId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound());
    }

    // 12. USER creates reservation -> 201
    @Test
    @DisplayName("12: USER creates reservation")
    void test12_userCreatesReservation() throws Exception {
        String start = future(2, 10);
        String end = future(2, 12);

        mockMvc.perform(post("/api/reservations")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"resourceId\":" + resourceId + ",\"startTime\":\"" + start + "\",\"endTime\":\"" + end + "\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()));
    }

    // 13. Reservation starts with PENDING status -> PENDING
    @Test
    @DisplayName("13: Reservation starts with PENDING status")
    void test13_reservationStartsWithPendingStatus() throws Exception {
        String start = future(2, 14);
        String end = future(2, 16);

        mockMvc.perform(post("/api/reservations")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"resourceId\":" + resourceId + ",\"startTime\":\"" + start + "\",\"endTime\":\"" + end + "\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    // 14. Reservation price is calculated from hourly rate -> Correct decimal price
    @Test
    @DisplayName("14: Reservation price calculated from hourly rate")
    void test14_reservationPriceCalculatedFromHourlyRate() throws Exception {
        // 3 hours * 800.00 = 2400.00
        String start = future(3, 10);
        String end = future(3, 13);

        mockMvc.perform(post("/api/reservations")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"resourceId\":" + resourceId + ",\"startTime\":\"" + start + "\",\"endTime\":\"" + end + "\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.price").value(2400.0));
    }

    // 15. User identity is taken from authenticated JWT -> Reservation belongs to logged-in user
    @Test
    @DisplayName("15: User identity taken from authenticated JWT")
    void test15_userIdentityTakenFromAuthenticatedJwt() throws Exception {
        String start = future(4, 10);
        String end = future(4, 12);

        mockMvc.perform(post("/api/reservations")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"resourceId\":" + resourceId + ",\"startTime\":\"" + start + "\",\"endTime\":\"" + end + "\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("user"));
    }

    // 16. End time before/equal to start time -> 400
    @Test
    @DisplayName("16: End time before/equal to start time")
    void test16_endTimeBeforeOrEqualStartTime() throws Exception {
        String start = future(5, 12);
        String end = future(5, 10);

        mockMvc.perform(post("/api/reservations")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"resourceId\":" + resourceId + ",\"startTime\":\"" + start + "\",\"endTime\":\"" + end + "\"}"))
                .andExpect(status().isBadRequest());

        // Equal start and end
        mockMvc.perform(post("/api/reservations")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"resourceId\":" + resourceId + ",\"startTime\":\"" + start + "\",\"endTime\":\"" + start + "\"}"))
                .andExpect(status().isBadRequest());
    }

    // 17. Reservation uses non-existing resource -> 404
    @Test
    @DisplayName("17: Reservation uses non-existing resource")
    void test17_reservationUsesNonExistingResource() throws Exception {
        String start = future(6, 10);
        String end = future(6, 12);

        mockMvc.perform(post("/api/reservations")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"resourceId\":99999,\"startTime\":\"" + start + "\",\"endTime\":\"" + end + "\"}"))
                .andExpect(status().isNotFound());
    }

    // 18. Reservation uses inactive resource -> 409
    @Test
    @DisplayName("18: Reservation uses inactive resource")
    void test18_reservationUsesInactiveResource() throws Exception {
        String start = future(7, 10);
        String end = future(7, 12);

        mockMvc.perform(post("/api/reservations")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"resourceId\":" + inactiveResourceId + ",\"startTime\":\"" + start + "\",\"endTime\":\"" + end + "\"}"))
                .andExpect(status().isConflict());
    }

    // 19. Overlapping PENDING/CONFIRMED reservation -> 409
    @Test
    @DisplayName("19: Overlapping PENDING/CONFIRMED reservation")
    void test19_overlappingPendingConfirmedReservation() throws Exception {
        String start = future(8, 10);
        String end = future(8, 12);
        String overlapStart = future(8, 11);
        String overlapEnd = future(8, 13);

        mockMvc.perform(post("/api/reservations")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"resourceId\":" + resourceId + ",\"startTime\":\"" + start + "\",\"endTime\":\"" + end + "\"}"))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/reservations")
                        .header("Authorization", "Bearer " + secondUserToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"resourceId\":" + resourceId + ",\"startTime\":\"" + overlapStart + "\",\"endTime\":\"" + overlapEnd + "\"}"))
                .andExpect(status().isConflict());
    }

    // 20. Booking exactly when previous booking ends -> 201
    @Test
    @DisplayName("20: Booking exactly when previous booking ends")
    void test20_bookingExactlyWhenPreviousBookingEnds() throws Exception {
        String start = future(9, 10);
        String end = future(9, 12);
        String nextStart = future(9, 12);
        String nextEnd = future(9, 14);

        mockMvc.perform(post("/api/reservations")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"resourceId\":" + resourceId + ",\"startTime\":\"" + start + "\",\"endTime\":\"" + end + "\"}"))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/reservations")
                        .header("Authorization", "Bearer " + secondUserToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"resourceId\":" + resourceId + ",\"startTime\":\"" + nextStart + "\",\"endTime\":\"" + nextEnd + "\"}"))
                .andExpect(status().isCreated());
    }

    // 21. USER views own reservations -> 200 + own records only
    @Test
    @DisplayName("21: USER views own reservations")
    void test21_userViewsOwnReservations() throws Exception {
        createReservation(userToken, resourceId, future(10, 8), future(10, 10));
        createReservation(secondUserToken, resourceId, future(10, 11), future(10, 13));

        mockMvc.perform(get("/api/reservations")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].username").value("user"));
    }

    // 22. USER views another user's reservation -> 403
    @Test
    @DisplayName("22: USER views another user's reservation")
    void test22_userViewsAnotherUsersReservation() throws Exception {
        String res = mockMvc.perform(post("/api/reservations")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"resourceId\":" + resourceId + ",\"startTime\":\"" + future(11, 10) + "\",\"endTime\":\"" + future(11, 12) + "\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        int id = com.jayway.jsonpath.JsonPath.read(res, "$.id");

        mockMvc.perform(get("/api/reservations/" + id)
                        .header("Authorization", "Bearer " + secondUserToken))
                .andExpect(status().isForbidden());
    }

    // 23. ADMIN views all reservations -> 200 + all records
    @Test
    @DisplayName("23: ADMIN views all reservations")
    void test23_adminViewsAllReservations() throws Exception {
        createReservation(userToken, resourceId, future(12, 8), future(12, 10));
        createReservation(secondUserToken, secondResourceId, future(12, 11), future(12, 13));

        mockMvc.perform(get("/api/admin/reservations")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)));
    }

    // 24. USER updates own reservation (same resource extension / no self-conflict) -> 200
    @Test
    @DisplayName("24: USER updates own reservation without self-conflict")
    void test24_userUpdatesOwnReservation() throws Exception {
        String res = mockMvc.perform(post("/api/reservations")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"resourceId\":" + resourceId + ",\"startTime\":\"" + future(13, 10) + "\",\"endTime\":\"" + future(13, 12) + "\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        int id = com.jayway.jsonpath.JsonPath.read(res, "$.id");

        // Extending end time on same resource (10 to 14) must not trigger self-conflict
        mockMvc.perform(put("/api/reservations/" + id)
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"resourceId\":" + resourceId + ",\"startTime\":\"" + future(13, 10) + "\",\"endTime\":\"" + future(13, 14) + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.resourceId").value(resourceId));
    }

    // 25. USER updates another user's reservation -> 403
    @Test
    @DisplayName("25: USER updates another user's reservation")
    void test25_userUpdatesAnotherUsersReservation() throws Exception {
        String res = mockMvc.perform(post("/api/reservations")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"resourceId\":" + resourceId + ",\"startTime\":\"" + future(14, 10) + "\",\"endTime\":\"" + future(14, 12) + "\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        int id = com.jayway.jsonpath.JsonPath.read(res, "$.id");

        mockMvc.perform(put("/api/reservations/" + id)
                        .header("Authorization", "Bearer " + secondUserToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"resourceId\":" + secondResourceId + ",\"startTime\":\"" + future(14, 14) + "\",\"endTime\":\"" + future(14, 16) + "\"}"))
                .andExpect(status().isForbidden());
    }

    // 26. USER cancels own reservation -> 204 and status becomes CANCELLED
    @Test
    @DisplayName("26: USER cancels own reservation")
    void test26_userCancelsOwnReservation() throws Exception {
        String res = mockMvc.perform(post("/api/reservations")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"resourceId\":" + resourceId + ",\"startTime\":\"" + future(15, 10) + "\",\"endTime\":\"" + future(15, 12) + "\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        int id = com.jayway.jsonpath.JsonPath.read(res, "$.id");

        mockMvc.perform(delete("/api/reservations/" + id)
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isNoContent());

        Reservation reservation = reservationRepository.findById((long) id).orElseThrow();
        assertEquals(ReservationStatus.CANCELLED, reservation.getStatus());
    }

    // 27. ADMIN changes reservation status -> 200
    @Test
    @DisplayName("27: ADMIN changes reservation status")
    void test27_adminChangesReservationStatus() throws Exception {
        String res = mockMvc.perform(post("/api/reservations")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"resourceId\":" + resourceId + ",\"startTime\":\"" + future(16, 10) + "\",\"endTime\":\"" + future(16, 12) + "\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        int id = com.jayway.jsonpath.JsonPath.read(res, "$.id");

        mockMvc.perform(put("/api/admin/reservations/" + id + "/status")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"CONFIRMED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"));
    }

    // 28. ADMIN updates reservation/user/resource -> 200
    @Test
    @DisplayName("28: ADMIN updates reservation/user/resource")
    void test28_adminUpdatesReservationUserResource() throws Exception {
        String res = mockMvc.perform(post("/api/reservations")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"resourceId\":" + resourceId + ",\"startTime\":\"" + future(17, 10) + "\",\"endTime\":\"" + future(17, 12) + "\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        int id = com.jayway.jsonpath.JsonPath.read(res, "$.id");

        mockMvc.perform(put("/api/admin/reservations/" + id)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":" + secondUserId + ",\"resourceId\":" + secondResourceId + ",\"startTime\":\"" + future(17, 13) + "\",\"endTime\":\"" + future(17, 15) + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("user2"))
                .andExpect(jsonPath("$.resourceId").value(secondResourceId));
    }

    // 29. ADMIN deletes reservation -> 204
    @Test
    @DisplayName("29: ADMIN deletes reservation")
    void test29_adminDeletesReservation() throws Exception {
        String res = mockMvc.perform(post("/api/reservations")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"resourceId\":" + resourceId + ",\"startTime\":\"" + future(18, 10) + "\",\"endTime\":\"" + future(18, 12) + "\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        int id = com.jayway.jsonpath.JsonPath.read(res, "$.id");

        mockMvc.perform(delete("/api/admin/reservations/" + id)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());

        assertFalse(reservationRepository.existsById((long) id));
    }

    // 30. Filter by status -> Matching records only
    @Test
    @DisplayName("30: Filter by status")
    void test30_filterByStatus() throws Exception {
        String res = mockMvc.perform(post("/api/reservations")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"resourceId\":" + resourceId + ",\"startTime\":\"" + future(19, 10) + "\",\"endTime\":\"" + future(19, 12) + "\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        int id = com.jayway.jsonpath.JsonPath.read(res, "$.id");

        createReservation(userToken, secondResourceId, future(19, 13), future(19, 15));

        // Confirm one
        mockMvc.perform(put("/api/admin/reservations/" + id + "/status")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"CONFIRMED\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/reservations")
                        .header("Authorization", "Bearer " + userToken)
                        .param("status", "CONFIRMED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].status").value("CONFIRMED"));
    }

    // 31. Filter by minimum price -> Matching records only
    @Test
    @DisplayName("31: Filter by minimum price")
    void test31_filterByMinPrice() throws Exception {
        createReservation(userToken, resourceId, future(20, 8), future(20, 9)); // 800
        createReservation(userToken, secondResourceId, future(20, 10), future(20, 12)); // 2400

        mockMvc.perform(get("/api/reservations")
                        .header("Authorization", "Bearer " + userToken)
                        .param("minPrice", "1000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].price").value(2400.0));
    }

    // 32. Filter by maximum price -> Matching records only
    @Test
    @DisplayName("32: Filter by maximum price")
    void test32_filterByMaxPrice() throws Exception {
        createReservation(userToken, resourceId, future(21, 8), future(21, 9)); // 800
        createReservation(userToken, secondResourceId, future(21, 10), future(21, 12)); // 2400

        mockMvc.perform(get("/api/reservations")
                        .header("Authorization", "Bearer " + userToken)
                        .param("maxPrice", "1000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].price").value(800.0));
    }

    // 33. Filter with minPrice > maxPrice -> 400
    @Test
    @DisplayName("33: Filter with minPrice > maxPrice")
    void test33_filterWithMinPriceGreaterThanMaxPrice() throws Exception {
        mockMvc.perform(get("/api/reservations")
                        .header("Authorization", "Bearer " + userToken)
                        .param("minPrice", "2000")
                        .param("maxPrice", "1000"))
                .andExpect(status().isBadRequest());
    }

    // 34. Pagination with page/size -> Correct page metadata/results
    @Test
    @DisplayName("34: Pagination with page/size")
    void test34_paginationWithPageSize() throws Exception {
        createReservation(userToken, resourceId, future(22, 8), future(22, 9));
        createReservation(userToken, secondResourceId, future(22, 10), future(22, 11));

        mockMvc.perform(get("/api/reservations")
                        .header("Authorization", "Bearer " + userToken)
                        .param("page", "0")
                        .param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(1))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content", hasSize(1)));
    }

    // 35. Invalid page or size -> 400
    @Test
    @DisplayName("35: Invalid page or size")
    void test35_invalidPageOrSize() throws Exception {
        mockMvc.perform(get("/api/reservations")
                        .header("Authorization", "Bearer " + userToken)
                        .param("page", "-1"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/reservations")
                        .header("Authorization", "Bearer " + userToken)
                        .param("size", "0"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/reservations")
                        .header("Authorization", "Bearer " + userToken)
                        .param("size", "101"))
                .andExpect(status().isBadRequest());
    }

    // 36. Sort by price ascending/descending -> Correct order
    @Test
    @DisplayName("36: Sort by price ascending/descending")
    void test36_sortByPriceAscendingDescending() throws Exception {
        createReservation(userToken, resourceId, future(23, 8), future(23, 9)); // 800
        createReservation(userToken, secondResourceId, future(23, 10), future(23, 12)); // 2400

        mockMvc.perform(get("/api/reservations")
                        .header("Authorization", "Bearer " + userToken)
                        .param("sortBy", "price")
                        .param("direction", "asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].price").value(800.0))
                .andExpect(jsonPath("$.content[1].price").value(2400.0));

        mockMvc.perform(get("/api/reservations")
                        .header("Authorization", "Bearer " + userToken)
                        .param("sortBy", "price")
                        .param("direction", "desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].price").value(2400.0))
                .andExpect(jsonPath("$.content[1].price").value(800.0));
    }

    // 37. Invalid sort field -> 400
    @Test
    @DisplayName("37: Invalid sort field")
    void test37_invalidSortField() throws Exception {
        mockMvc.perform(get("/api/reservations")
                        .header("Authorization", "Bearer " + userToken)
                        .param("sortBy", "invalidColumn"))
                .andExpect(status().isBadRequest());
    }

    // 38. Invalid sort direction -> 400
    @Test
    @DisplayName("38: Invalid sort direction")
    void test38_invalidSortDirection() throws Exception {
        mockMvc.perform(get("/api/reservations")
                        .header("Authorization", "Bearer " + userToken)
                        .param("direction", "sideways"))
                .andExpect(status().isBadRequest());
    }

    // 39. Invalid reservation status value -> 400
    @Test
    @DisplayName("39: Invalid reservation status value")
    void test39_invalidReservationStatusValue() throws Exception {
        mockMvc.perform(get("/api/reservations")
                        .header("Authorization", "Bearer " + userToken)
                        .param("status", "UNKNOWN_STATUS"))
                .andExpect(status().isBadRequest());
    }

    // 40. Malformed JSON request -> 400
    @Test
    @DisplayName("40: Malformed JSON request")
    void test40_malformedJsonRequest() throws Exception {
        mockMvc.perform(post("/api/reservations")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"resourceId\": 1, \"startTime\": broken_json"))
                .andExpect(status().isBadRequest());
    }

    // 41. USER accesses ADMIN reservation endpoints -> 403
    @Test
    @DisplayName("41: USER accesses ADMIN reservation endpoints")
    void test41_userAccessesAdminReservationEndpoints() throws Exception {
        mockMvc.perform(get("/api/admin/reservations")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/admin/reservations")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":" + firstUserId + ",\"resourceId\":" + resourceId + ",\"startTime\":\"" + future(24, 10) + "\",\"endTime\":\"" + future(24, 12) + "\"}"))
                .andExpect(status().isForbidden());
    }

    // 42. Expired/invalid JWT -> 401
    @Test
    @DisplayName("42: Expired/invalid JWT")
    void test42_expiredOrInvalidJwt() throws Exception {
        mockMvc.perform(get("/api/resources")
                        .header("Authorization", "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.invalid.token"))
                .andExpect(status().isUnauthorized());
    }

    // 43. JWT is stateless -> No server session required
    @Test
    @DisplayName("43: JWT is stateless")
    void test43_jwtIsStateless() throws Exception {
        // Request with JWT succeeds
        mockMvc.perform(get("/api/resources")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk());

        // Subsequent request without token fails (proves statelessness, no cookie session)
        mockMvc.perform(get("/api/resources"))
                .andExpect(status().isUnauthorized());
    }

    // 44. Resource not found -> 404
    @Test
    @DisplayName("44: Resource not found")
    void test44_resourceNotFound() throws Exception {
        mockMvc.perform(get("/api/resources/99999")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isNotFound());
    }

    // 45. Reservation not found -> 404
    @Test
    @DisplayName("45: Reservation not found")
    void test45_reservationNotFound() throws Exception {
        mockMvc.perform(get("/api/reservations/99999")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/admin/reservations/99999")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound());
    }

    private void createReservation(String token, Long resource, String start, String end) throws Exception {
        mockMvc.perform(post("/api/reservations")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"resourceId\":" + resource + ",\"startTime\":\"" + start + "\",\"endTime\":\"" + end + "\"}"))
                .andExpect(status().isCreated());
    }
}

