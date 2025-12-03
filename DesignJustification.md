# GymClass Booking System – System Design Document

## 1. Introduction

This document describes the architecture and design decisions of the **GymClass Booking System**, a JavaFX + SQLite application that allows:

- **Members** to browse and book group classes and track their fitness progress.
- **Trainers** to manage attendance and award experience points (XP).
- **Admins** to manage users, classes and schedules.

The document focuses on:

- How we structured our classes and packages.
- Why we chose our abstractions (services, repositories, helpers).
- How we modelled the database schema.
- How the application connects to and queries the database.
- The main design challenges we faced and how we solved them.

---

## 2. High-Level Architecture

We follow a **layered architecture**:

- **UI layer (presentation)**  
  JavaFX FXML screens + controllers  
  (`com.gym.ui.controllers`, `/views/*.fxml`)

- **Service layer (business logic)**  
  Application use cases and rules  
  (`com.gym.service`)

- **Domain layer (core entities)**  
  Plain Java classes that model users, classes, bookings, schedules and progress  
  (`com.gym.domain`)

- **Persistence layer**  
  Repository interfaces + SQLite implementations + `DatabaseManager`  
  (`com.gym.repository`, `com.gym.repository.sqlite`)

- **Infrastructure / utilities**  
  App bootstrap, navigation, session management, demo seed data  
  (`com.gym.AppMain`, `com.gym.AppConfig`, `com.gym.utils.*`)

### Main flow (entry point)

1. **`AppMain`** is the JavaFX `Application` entry point.
2. In `start(Stage)` it calls `AppConfig.init()`:
    - creates a `SqliteDatabaseManager` and initializes tables,
    - constructs repositories and services,
    - seeds default users and example classes/schedules.
3. `SceneManager.setPrimaryStage(stage)` registers the main JavaFX stage.
4. `SceneManager.switchTo("/views/login.fxml", "Gym Class Booking - Login")` loads the first screen.
5. From that point, controllers use:
    - `AppConfig` to access services,
    - `SceneManager` to navigate between views,
    - `SessionManager` to know which `User` is logged in.

---

## 3. UML Class Diagram (main classes)

Below is a textual UML-style view of the main classes and relationships (simplified):

### Domain layer
```text
com.gym.domain
-------------------------------
User
  - userId : int
  - username : String
  - password : String
  - email : String
  - role : String   // "ADMIN", "TRAINER", "MEMBER"
  - createdAt : String

GymClass
  - classId : int
  - className : String
  - instructorName : String
  - description : String
  - capacity : int
  - durationMinutes : int
  - classType : String  // "HIIT", "YOGA", ...

ClassSchedule
  - scheduleId : int
  - classId : int
  - scheduledDate : LocalDate
  - startTime : LocalTime
  - endTime : LocalTime
  - availableSpots : int
  -----------------------
  * belongs to one GymClass

Booking
  - bookingId : int
  - userId : int
  - scheduleId : int
  - bookingDate : LocalDateTime
  - status : String  // "CONFIRMED", "CANCELLED", "ATTENDED"
  -----------------------
  * links one User to one ClassSchedule

FitnessProgress
  - progressId : int
  - userId : int
  - category : String // CARDIO, STRENGTH, LEGS, ...
  - totalPoints : int
  - lastUpdated : LocalDate
```

### Service layer (interfaces + implementations)
```text
AuthService
  + login(...)
  + register(...)
  + logout()
  + getCurrentUser()

AuthServiceImpl
  - userRepository : UserRepository

ClassService
  + createClass(...)
  + getAllClasses()
  + createSchedule(...)
  + getAvailableSchedules()
  ...

ClassServiceImpl
  - classRepository : ClassRepository

BookingService
  + bookClass(...)
  + cancelBooking(...)
  + markAttended(bookingId)
  + getUserBookings(...)
  ...

BookingServiceImpl
  - bookingRepository : BookingRepository
  - classRepository   : ClassRepository
  - progressService   : ProgressService

ProgressService
  + initializeUserProgress(userId)
  + awardPointsForClass(userId, classType)
  + getAllUserProgress(userId)
  + getPointsForClassType(classType)

ProgressServiceImpl
  - progressRepository : ProgressRepository
  - pointSystem : Map<String, Map<String,Integer>>
```

### Persistence layer
```text
DatabaseManager (interface)
  + initializeDatabase()
  + getConnection() : Connection

SqliteDatabaseManager implements DatabaseManager
  - url : String
  - createUsersTable()
  - createClassesTable()
  - ...

UserRepository
ClassRepository
BookingRepository
ProgressRepository
   // standard CRUD + custom queries

SqliteUserRepository       implements UserRepository
SqliteClassRepository      implements ClassRepository
SqliteBookingRepository    implements BookingRepository
SqliteProgressRepository   implements ProgressRepository
```

### Utilities
```text
SceneManager
  - primaryStage : Stage
  + setPrimaryStage(...)
  + switchTo(fxmlPath, title)

SessionManager
  - currentUser : User
  + setCurrentUser(User)
  + getCurrentUser()
  + clear()
```

### UI Controllers

Each FXML view has a controller under `com.gym.ui.controllers`  
(e.g. `LoginController`, `MemberDashboardController`, `TrainerAttendanceController`, `ProgressController`, …)

Controllers are thin: they read/write UI controls and delegate actions to services.

---

## 4. Database Schema Diagram (SQLite)

We use a relational schema with four main tables.

### 4.1 Tables

#### users

| Column     | Type    | Constraints                                   |
|------------|---------|-----------------------------------------------|
| user_id    | INTEGER | PRIMARY KEY AUTOINCREMENT                     |
| username   | TEXT    | UNIQUE, NOT NULL                              |
| password   | TEXT    | NOT NULL                                      |
| email      | TEXT    | UNIQUE, NOT NULL                              |
| role       | TEXT    | NOT NULL (ADMIN, TRAINER, MEMBER)             |
| created_at | TEXT    | DEFAULT CURRENT_TIMESTAMP                     |

#### classes

| Column           | Type    | Constraints                                   |
|------------------|---------|-----------------------------------------------|
| class_id         | INTEGER | PRIMARY KEY AUTOINCREMENT                     |
| class_name       | TEXT    | NOT NULL                                      |
| instructor_name  | TEXT    | NOT NULL                                      |
| description      | TEXT    |                                               |
| capacity         | INTEGER | NOT NULL                                      |
| duration_minutes | INTEGER | NOT NULL                                      |
| class_type       | TEXT    | NOT NULL (e.g. HIIT, YOGA, CARDIO)            |
| created_at       | TEXT    | DEFAULT CURRENT_TIMESTAMP                     |

#### class_schedule

| Column          | Type    | Constraints                                   |
|-----------------|---------|-----------------------------------------------|
| schedule_id     | INTEGER | PRIMARY KEY AUTOINCREMENT                     |
| class_id        | INTEGER | NOT NULL → references classes(class_id)       |
| scheduled_date  | TEXT    | NOT NULL (ISO yyyy-MM-dd)                     |
| start_time      | TEXT    | NOT NULL (HH:mm)                              |
| end_time        | TEXT    | NOT NULL                                      |
| available_spots | INTEGER | NOT NULL                                      |

**Relationship:** many schedules belong to one GymClass.

#### bookings

| Column       | Type    | Constraints                                          |
|--------------|---------|------------------------------------------------------|
| booking_id   | INTEGER | PRIMARY KEY AUTOINCREMENT                            |
| user_id      | INTEGER | NOT NULL → references users(user_id)                 |
| schedule_id  | INTEGER | NOT NULL → references class_schedule(schedule_id)    |
| booking_date | TEXT    | DEFAULT CURRENT_TIMESTAMP                            |
| status       | TEXT    | DEFAULT 'CONFIRMED' (CONFIRMED, CANCELLED, ATTENDED) |

**Relationship:** a booking links one User and one ClassSchedule.

#### fitness_progress

| Column       | Type    | Constraints                                          |
|--------------|---------|------------------------------------------------------|
| progress_id  | INTEGER | PRIMARY KEY AUTOINCREMENT                            |
| user_id      | INTEGER | NOT NULL → references users(user_id)                 |
| category     | TEXT    | NOT NULL (CARDIO, STRENGTH, LEGS, ARMS, etc.)        |
| total_points | INTEGER | DEFAULT 0                                            |
| last_updated | TEXT    | NOT NULL (ISO date)                                  |

**Additional constraint:** UNIQUE(user_id, category) – one row per user+category.

### 4.2 E-R Overview

- **User – Booking:** 1-to-many (a user can have many bookings).
- **GymClass – ClassSchedule:** 1-to-many (a class can have many scheduled sessions).
- **ClassSchedule – Booking:** 1-to-many (a scheduled session can have many members).
- **User – FitnessProgress:** 1-to-many, but with unique per category.

This schema matches the domain design where:

- **GymClass** describes the template of a class.
- **ClassSchedule** is a specific occurrence in time.
- **Booking** captures who attends which occurrence.
- **FitnessProgress** tracks aggregated XP per category per user.

---

## 5. Detailed Design Justification

### 5.1 Why this class structure? (Domain modelling decisions)

#### Separate GymClass, ClassSchedule and Booking

We intentionally split these responsibilities:

**GymClass**
- Describes what the class is: name, description, instructor, capacity, type.
- It is independent of time.

**ClassSchedule**
- Describes when a specific instance happens: date, start/end time, available spots.

*Why not embed schedule inside GymClass?*

Because a single class (e.g. "Extreme Cardio") can be offered multiple times per week. Modeling schedules as a separate entity makes it easy to:

- list future occurrences,
- update/cancel a single session,
- enforce available spots per session.

**Booking**
- Links a User to one ClassSchedule, plus the status (CONFIRMED, CANCELLED, ATTENDED).

*Why not just add a "participants list" to ClassSchedule?*

- We want per-user status (cancelled vs attended).
- We need to store booking date and run queries like "all bookings for this user".
- A join table (bookings) is the natural relational representation.

This separation also matches how the UI works:

- Admin manages classes and schedules.
- Members interact mainly with bookings.
- Trainers manage attendance on bookings.

#### FitnessProgress per category

We model progress as:

- One row per (userId, category), where category ∈ {CARDIO, STRENGTH, FLEXIBILITY, ENDURANCE, LEGS, ARMS, CORE}.
- Each row stores totalPoints and lastUpdated.

The progress screen aggregates internal categories into the 4 big areas shown in the UI:

- **Upper body** = STRENGTH + CORE + FLEXIBILITY
- **Lower body** = LEGS
- **Arms** = ARMS
- **Cardio** = CARDIO + ENDURANCE

We did it this way to:

- Keep the DB schema flexible (adding a new category is just another string).
- Make the UI aggregation logic explicit in the service/controller, not hard-coded in a schema.

#### Why User is not abstract / no Admin extends User

Instead of modelling:
```java
abstract class User { ... }
class Admin extends User { ... }
class Trainer extends User { ... }
class Member extends User { ... }
```

We use a single `User` class with a `role` field.

**Justification:**

- Behavior differences are mostly in the UI, not in the core domain.
- Admin sees admin dashboard, trainer sees trainer dashboard, etc.
- There was no behaviour that required polymorphism at the domain level (no `user.canBook()` overriding, etc.).
- A single table (users) keeps the DB simple; no JOINs between multiple user tables.
- It is easy to extend with new roles by just adding a constant and adjusting dashboards.
- If later we needed role-specific logic, we could introduce patterns like Role objects or strategy, without forcing inheritance now.

### 5.2 Why this service layer?

We introduced service interfaces (`AuthService`, `ClassService`, `BookingService`, `ProgressService`) to:

- Concentrate business rules in one place.
- Keep controllers thin and testable.
- Isolate the persistence layer (services depend on repositories, not on SQLite).

**Examples:**

**AuthServiceImpl**
- Validates username/password/email/role when registering.
- Delegates to UserRepository for existence checks and saving.
- Keeps currentUser when login is successful.

**ClassServiceImpl**
- Validates business constraints for classes and schedules:
    - no schedules in the past,
    - end time must be after start time,
    - available spots must be positive and ≤ class capacity,
    - only show schedules in the next 14 days, etc.
- Provides search operations (searchClasses) and "available schedules" for the member dashboard.

**BookingServiceImpl**
- Ensures a user cannot double-book the same schedule (hasUserBooked).
- Validates capacity and updates availableSpots when booking / cancelling.
- Encapsulates the rule: XP is awarded only once per booking, in `markAttended(bookingId)`.

**ProgressServiceImpl**
- Encodes the XP system: for each class type, which internal categories get points and how many.
- Ensures one progress row per (user, category) and updates it atomically.

This separation makes it easy to reason about where the rules live:  
**UI → Service → Repository → DB.**

### 5.3 Data structures and algorithms

We mainly use:

**List\<T\>** (e.g. `List<GymClass>`, `List<Booking>`, `List<ClassSchedule>`)
- When retrieving collections from repositories and filtering them in services or controllers.

**Java Streams** with `filter`, `map`, `collect` for concise operations, for example:
- `searchClasses` by name/instructor/type.
- `getAvailableSchedules` (filter by date range and available spots).
- `getSchedulesByDate` (filter all schedules by a specific day).
- Building the "recently completed classes" list from bookings.

**Map<String, Map<String,Integer>> pointSystem** in `ProgressServiceImpl`:
```java
Map<String, Map<String, Integer>> pointSystem;
// e.g. pointSystem.get("HIIT") = {CARDIO=10, STRENGTH=10, ENDURANCE=10, LEGS=10}
```

Using a nested map for the XP rules allows:

- Very fast lookup of how much XP each class type gives to each category.
- Easy tuning of values (e.g. nerfing from 60/70/80 points down to 10).
- Centralized configuration instead of spreading "magic numbers" all over the code.

The progress aggregation uses simple integer arithmetic:

- 100 XP = 1 level.
- Level = totalPoints / 100.
- Progress bar value = (totalPoints % 100) / 100.0.

This keeps the algorithms simple and readable, which is important for a teaching project.

### 5.4 Database access and decoupling

#### Repository Pattern

We implemented a dedicated repository interface per aggregate:

- `UserRepository`
- `ClassRepository`
- `BookingRepository`
- `ProgressRepository`

Each has a concrete `Sqlite*Repository` implementation using JDBC.

**Reasons:**

**Separation of concerns:**
- Services deal with domain objects and business rules.
- Repositories deal with SQL, connections, and mapping rows to objects.

**Testability:**
- In unit tests, we can provide fake/in-memory implementations of `UserRepository` etc.

**Extensibility:**
- If the storage layer changes (e.g. another DB), we implement a new `XxxRepository` without touching UI or services.

**Example mapping helper** (in `SqliteUserRepository`):
```java
private User extractUserFromResultSet(ResultSet rs) throws SQLException {
    return new User(
            rs.getInt("user_id"),
            rs.getString("username"),
            rs.getString("password"),
            rs.getString("email"),
            rs.getString("role"),
            rs.getString("created_at")
    );
}
```

We use similar helper methods for `Booking`, `GymClass`, `ClassSchedule`, and `FitnessProgress`.

#### DatabaseManager abstraction

Originally, `SqliteDatabaseManager` was a static utility. We refactored it to implement a `DatabaseManager` interface:
```java
public interface DatabaseManager {
    void initializeDatabase();
    Connection getConnection() throws SQLException;
}
```

`SqliteDatabaseManager` now:

- Implements `DatabaseManager`.
- Provides `getConnection()` and internal `create*Table()` methods.
- Is instantiated once in `AppConfig`, then passed to each `Sqlite*Repository`.

**Why this change?**

- It reduces coupling: repositories depend on the `DatabaseManager` abstraction, not on static methods of a concrete class.
- It becomes easier to:
    - swap SQLite for another DB,
    - or inject a test DB.

### 5.5 Navigation and session management

We introduced two helpers to decouple navigation and login state from controllers:

#### SceneManager

- Holds a static reference to the primary `Stage`.
- Provides `switchTo(fxmlPath, title)`:
    - loads the FXML,
    - sets the new scene on the stage,
    - updates the window title.

**Why?**

Without `SceneManager`, every controller would need to know how to set up scenes and would duplicate code to load FXML and find the primary stage.

With it, controllers simply call:
```java
SceneManager.switchTo("/views/member-dashboard.fxml", "Member Dashboard");
```

#### SessionManager

- Stores the currently authenticated `User`.
- Used by controllers like `ProgressController` to get the logged-in user ID.

We chose a `SessionManager` separate from `AuthServiceImpl` to keep:

- `AuthService` focused purely on login/registration logic.
- `SessionManager` as a simple shared store accessible from anywhere in the UI layer.

### 5.6 Validation strategy

We systematically validate input in the service layer, not in the repositories:

**AuthServiceImpl** validates usernames, passwords, emails and roles.

**ClassServiceImpl** validates:
- non-empty names,
- positive capacity and duration,
- future dates,
- consistent start/end times,
- available spots ≤ capacity.

**BookingServiceImpl** validates:
- schedule existence,
- available spots,
- that the user has not already booked the same schedule,
- that a user can only cancel their own bookings.

Repositories assume they are given valid domain objects; their responsibility is persistence, not business rules.

### 5.7 Main design challenge and how we solved it

**Challenge:** Designing a realistic XP progression system and attendance logic without weird edge cases.

We initially had these problems:

1. **XP per class was too high**
    - A single class could grant 60–80 XP in multiple categories, and each level needed only 100 XP.
    - Result: after a couple of classes, members were already level 10+ — the progress screen felt broken.

2. **"Recently completed classes" list didn't match reality**
    - It showed past bookings even if the trainer never actually marked attendance.

3. **XP could be awarded multiple times for the same class**
    - Each time the trainer opened the attendance screen and hit "Save", XP was added again for the same booking.

**Solution:**

We redesigned the XP system around a simple rule:

- Every class type awards **10 XP** per affected category, and levels require **100 XP**.

Example mappings:

- **STRENGTH** → +10 to STRENGTH, ARMS, LEGS, CORE
- **YOGA** → +10 to FLEXIBILITY, CORE, STRENGTH
- **CARDIO** → +10 to CARDIO, ENDURANCE, LEGS
- **HIIT** → +10 to CARDIO, STRENGTH, ENDURANCE, LEGS

This logic is centralized in `ProgressServiceImpl.pointSystem`.

We introduced a clear status model for bookings: `"CONFIRMED"`, `"CANCELLED"`, `"ATTENDED"` and helper methods `isConfirmed()`, `isCancelled()`, `isAttended()`.

We moved the "award XP exactly once" rule into `BookingServiceImpl.markAttended(int bookingId)`:

- It loads the booking.
- If already `ATTENDED` → does nothing.
- Otherwise:
    - sets status to `ATTENDED` and updates the booking,
    - finds the related `ClassSchedule` and `GymClass`,
    - calls `progressService.awardPointsForClass(userId, classType)`.

The trainer UI (`TrainerAttendanceController`) now:

- Just calls `bookingService.markAttended(row.getBookingId())`.
- Shows informative messages:
    - "Attendance saved and points awarded…" for new awards.
    - "Attendance and points were already saved…" if the user was already attended.
- Disables the attendance checkbox for bookings that were already attended when the screen loaded (read-only tick).

The "Recently completed classes" table in `ProgressController` now:

- Filters bookings by `isAttended()` and past end time.
- Shows the real XP value per class, computed from `ProgressService.getPointsForClassType`.

This combination fixed:

- Unrealistic XP growth.
- Past bookings appearing as "completed" without attendance.
- Multiple XP awards for the same class.

### 5.8 Extensibility and limitations

#### Extensibility

- **New roles:** add another constant in `User.role` and adapt dashboards/permissions.
- **New progress categories:** insert a new category in the XP map and adjust UI aggregation.
- **New class types:** extend `pointSystem` in `ProgressServiceImpl` and optionally provide new filters in UI.
- **Alternative storage:** implement repositories for another DB and a new `DatabaseManager`.

#### Limitations (known trade-offs)

- Roles are modelled as a String field rather than an enum or hierarchy; more robust role systems would need extra structure.
- Some filtering is done in memory (`findAllSchedules().stream().filter(...)`), which is fine for a small teaching project but not ideal for very large datasets.
- Error reporting is mainly via `System.out` / `System.err` logs; user-friendly feedback is partially implemented in controllers.

---

## 6. Mapping to Assignment Questions

### "Why did you structure your classes this way (e.g., why use inheritance or an interface here)?"

- We used **composition** and separate entities (`GymClass`, `ClassSchedule`, `Booking`, `FitnessProgress`) to model different responsibilities (class template, occurrences, links, aggregated stats).
- We avoided unnecessary inheritance for User roles; a single `User` with a `role` string is enough given that role differences are mostly UI-level.
- We used **interfaces** for services and repositories (`AuthService`, `BookingService`, `UserRepository`, etc.) to decouple business logic from persistence and to allow alternative implementations.

### "Why did you choose your specific data structures for a task?"

- `List<T>` is used for collections from repositories; it maps naturally to SQL result sets.
- `Map<String,Map<String,Integer>>` is used to represent class-type-to-XP mappings in a concise, configurable way.
- Simple arithmetic is used for XP → level and progress bar calculations to keep logic understandable.

### "How does your application connect to and query the database?"

- `AppConfig` constructs a `SqliteDatabaseManager`, which:
    - creates the tables if they don't exist,
    - exposes `getConnection()` for repositories.
- Each `Sqlite*Repository` uses JDBC (`Connection`, `PreparedStatement`, `ResultSet`) to execute queries and map rows to domain objects.
- The service layer calls the repositories; the UI only talks to services.

### "What was the most significant design challenge you faced and how did you solve it?"

**The XP and attendance system:**  
Designing a realistic level progression and ensuring XP is awarded exactly once per class attended, while keeping the UI consistent.

We solved it by:

- Defining a simple, consistent XP rule,
- Encoding it centrally in `ProgressServiceImpl`,
- Making `BookingServiceImpl.markAttended` responsible for XP awarding and idempotence,
- Syncing the trainer and member UIs with the booking status.