# Travel Agency Platform - Project Documentation

## Table of Contents
1. [Project Overview](#project-overview)
2. [Tech Stack](#tech-stack)
3. [Architecture](#architecture)
4. [Low-Level Design (LLD)](#low-level-design)
5. [Data Models](#data-models)
6. [Project Flow](#project-flow)
7. [API Endpoints](#api-endpoints)
8. [Security & Authentication](#security--authentication)
9. [Error Handling](#error-handling)
10. [Deployment](#deployment)

---

## Project Overview

The **Travel Agency Platform** is a Spring Boot-based backend service designed to provide a comprehensive travel booking and management system. It enables users to:

- **User Authentication**: Sign up and sign in with secure JWT-based authentication
- **Tour Discovery**: Browse, search, and filter available tours
- **Tour Details**: View detailed information about tours including pricing, meal plans, accommodations
- **Reviews Management**: Read and manage tour reviews from other travelers
- **Account Security**: Account lockout mechanism after failed login attempts

### Key Features:
- ✅ User registration with password strength validation
- ✅ JWT-based stateless authentication
- ✅ Account lockout after 5 failed login attempts
- ✅ Comprehensive tour search with multiple filters
- ✅ Destination autocomplete
- ✅ Paginated tour listings and reviews
- ✅ MongoDB for persistent data storage
- ✅ CORS support for cross-origin requests
- ✅ Docker containerization for easy deployment
- ✅ Kubernetes-ready with Helm charts

---

## Tech Stack

### Backend Framework
- **Spring Boot 3.5.0** - RESTful API framework
- **Spring Security** - Authentication and authorization
- **Spring Data MongoDB** - Database access layer

### Libraries
- **JWT (JJWT 0.12.6)** - JSON Web Token implementation
- **BCrypt** - Password hashing
- **Lombok** - Reduced boilerplate code
- **Jackson** - JSON serialization/deserialization
- **Spring Validation** - Input validation

### Database
- **MongoDB** - NoSQL database for flexible schema

### Infrastructure
- **Java 17** - Programming language
- **Maven** - Build tool
- **Docker** - Containerization
- **Kubernetes & Helm** - Orchestration and deployment

---

## Architecture

### Layered Architecture Pattern

```
┌─────────────────────────────────────────┐
│         Presentation Layer              │
│      (Controllers & DTOs)               │
├─────────────────────────────────────────┤
│         Business Logic Layer            │
│      (Services)                         │
├─────────────────────────────────────────┤
│         Data Access Layer               │
│      (Repositories)                     │
├─────────────────────────────────────────┤
│         Database Layer                  │
│      (MongoDB)                          │
└─────────────────────────────────────────┘
```

### Package Structure

```
com.epam.edp.demo/
├── DemoApplication.java
├── config/
│   ├── CorsConfig.java
│   └── SecurityConfig.java
├── controller/
│   ├── AuthController.java
│   └── TourController.java
├── dto/
│   ├── SignInRequestDTO
│   ├── SignInResponseDTO
│   ├── SignUpRequestDTO
│   ├── SignUpResponseDTO
│   ├── UserResponseDTO
│   ├── TourListResponseDTO
│   ├── TourDetailResponseDTO
│   ├── ReviewListResponseDTO
│   └── DestinationListResponseDTO
├── enums/
│   └── Role.java (CUSTOMER, ADMIN)
├── exception/
│   ├── GlobalExceptionHandler.java
│   ├── AccountLockedException.java
│   ├── EmailAlreadyExistsException.java
│   ├── InvalidCredentialsException.java
│   ├── WeakPasswordException.java
│   ├── UnauthenticatedException.java
│   └── ApiError.java
├── model/
│   ├── User.java
│   ├── Tour.java
│   └── Review.java
├── repository/
│   ├── UserRepository.java
│   ├── TourRepository.java
│   └── ReviewRepository.java
├── security/
│   ├── JwtService.java
│   └── JwtAuthenticationFilter.java
└── service/
    ├── UserService.java
    └── TourService.java
```

---

## Low-Level Design

### 1. Entity Models

#### User Entity
```
┌─────────────────────────────────────┐
│          User (MongoDB)             │
├─────────────────────────────────────┤
│ - id (ObjectId)                     │
│ - firstName (String)                │
│ - lastName (String)                 │
│ - email (String, unique indexed)    │
│ - passwordHash (String)             │
│ - role (Enum: CUSTOMER/ADMIN)       │
│ - failedLoginAttempts (int)         │
│ - lockedUntil (Instant, nullable)   │
│ - createdAt (Instant)               │
│ - updatedAt (Instant)               │
└─────────────────────────────────────┘
```

**Key Features:**
- Email is unique indexed for fast lookup
- Password stored as BCrypt hash (never in plaintext)
- Account locking mechanism tracks failed attempts

#### Tour Entity
```
┌─────────────────────────────────────┐
│          Tour (MongoDB)             │
├─────────────────────────────────────┤
│ - id (ObjectId)                     │
│ - name (String)                     │
│ - destination (String, indexed)     │
│ - summary (String)                  │
│ - imageUrls (List<String>)          │
│ - rating (Double)                   │
│ - reviewCount (Integer)             │
│ - startDates (List<LocalDate>)      │
│ - durations (List<String>)          │
│ - pricePerDuration (Map)            │
│ - mealPlans (List<String>)          │
│ - mealSupplementsPerDay (Map)       │
│ - tourType (String)                 │
│ - hotelName (String)                │
│ - hotelDescription (String)         │
│ - accommodation (String)            │
│ - customDetails (Map)               │
│ - guestQuantity (Nested)            │
│ - totalCapacity (Integer)           │
│ - bookedCount (Integer)             │
│ - freeCancellationDaysBefore (Int)  │
│ - assignedAgentId (String)          │
│ - createdAt (LocalDateTime)         │
│ - updatedAt (LocalDateTime)         │
└─────────────────────────────────────┘
```

#### Review Entity
```
┌─────────────────────────────────────┐
│         Review (MongoDB)            │
├─────────────────────────────────────┤
│ - id (ObjectId)                     │
│ - tourId (String, indexed)          │
│ - userId (String)                   │
│ - userName (String)                 │
│ - userAvatarUrl (String)            │
│ - rate (Double)                     │
│ - comment (String)                  │
│ - reviewDate (LocalDate)            │
│ - createdAt (LocalDateTime)         │
└─────────────────────────────────────┘
```

### 2. Service Layer Design

#### UserService
**Responsibilities:**
- User registration (sign-up)
- User authentication (sign-in)
- Password strength validation
- Account lockout management
- Failed attempt tracking

**Key Methods:**
```
signUp(SignUpRequestDTO) -> User
  ├─ Validate password strength
  ├─ Check for identity containment in password
  ├─ Check email uniqueness
  ├─ Encode password with BCrypt
  └─ Save user to database

signIn(SignInRequestDTO) -> SignInResponseDTO
  ├─ Fetch user by email
  ├─ Check if account is locked
  ├─ Compare password with hash
  ├─ Handle failed attempts
  ├─ Lock account if max attempts exceeded
  ├─ Generate JWT token
  └─ Return user details with token

requireById(userId) -> User
  └─ Fetch user by ID or throw exception
```

#### TourService
**Responsibilities:**
- Tour discovery and filtering
- Destination autocomplete
- Review management
- Tour detail retrieval
- Pagination and sorting

**Key Methods:**
```
searchDestinations(query) -> DestinationListResponseDTO
  ├─ Validate query length (min 3 chars)
  ├─ Use regex for case-insensitive search
  └─ Return distinct destination list

getAvailableTours(...filters...) -> TourListResponseDTO
  ├─ Build MongoDB query with filters
  ├─ Apply sorting
  ├─ Calculate pagination
  ├─ Execute query
  └─ Map results to DTOs

getTourById(id) -> TourDetailResponseDTO
  ├─ Fetch tour by ID
  ├─ Map to detailed DTO
  └─ Return tour information

getReviews(tourId, sortBy, page, pageSize) -> ReviewListResponseDTO
  ├─ Fetch reviews for tour with pagination
  ├─ Apply sorting (TOP_RATED_FIRST/MOST_RECENT_FIRST)
  └─ Return paginated review list
```

### 3. Security Layer

#### JwtService
**JWT Generation:**
- Creates HS256-signed tokens
- Embeds userId, email, firstName
- Configurable TTL (default: 24 hours)
- Instant expiration tracking

**JWT Validation:**
- Verifies signature with secret key
- Parses claims
- Throws InvalidJwtException on failure

#### JwtAuthenticationFilter
- Intercepts requests before reaching controllers
- Extracts JWT from Authorization header
- Validates and parses token
- Sets authentication context
- Allows unauthenticated requests to permitted endpoints

### 4. Controller Layer Design

#### AuthController
```
POST /api/v1/auth/sign-up
  ├─ Request: SignUpRequestDTO
  ├─ Response: SignUpResponseDTO
  └─ Status: 201 Created

POST /api/v1/auth/sign-in
  ├─ Request: SignInRequestDTO
  ├─ Response: SignInResponseDTO (with JWT)
  └─ Status: 200 OK

GET /api/v1/auth/me
  ├─ Requires: Valid JWT
  ├─ Response: UserResponseDTO
  └─ Status: 200 OK
```

#### TourController
```
GET /tours/destinations?destination=query
  ├─ Query: destination search string
  ├─ Response: DestinationListResponseDTO
  └─ Status: 200 OK

GET /tours/available?...filters...
  ├─ Filters: destination, startDate, duration, etc.
  ├─ Response: TourListResponseDTO
  └─ Status: 200 OK

GET /tours/{id}
  ├─ Param: tour ID
  ├─ Response: TourDetailResponseDTO
  └─ Status: 200 OK

GET /tours/{id}/reviews?sortBy=...&page=...&pageSize=...
  ├─ Params: tourId, sortBy, pagination
  ├─ Response: ReviewListResponseDTO
  └─ Status: 200 OK
```

### 5. Data Flow Diagrams

#### Authentication Flow
```
┌──────────────┐
│   Client     │
└────┬─────────┘
     │
     │ 1. POST /api/v1/auth/sign-up
     │    {firstName, lastName, email, password}
     ▼
┌──────────────────────────┐
│   AuthController         │
│   (sign-up)             │
└────┬─────────────────────┘
     │
     │ 2. Call signUp()
     ▼
┌──────────────────────────┐
│   UserService            │
│   - Validate password    │
│   - Check email unique   │
│   - Hash password        │
└────┬─────────────────────┘
     │
     │ 3. Save user
     ▼
┌──────────────────────────┐
│   UserRepository         │
│   MongoDB: users         │
└────┬─────────────────────┘
     │
     │ 4. Return created user
     ▼
┌──────────────┐
│   Client     │
│   201 Created│
└──────────────┘
```

#### Sign-In with JWT Flow
```
┌──────────────┐
│   Client     │
└────┬─────────┘
     │
     │ 1. POST /api/v1/auth/sign-in
     │    {email, password}
     ▼
┌──────────────────────────┐
│   AuthController         │
│   (sign-in)              │
└────┬─────────────────────┘
     │
     │ 2. Call signIn()
     ▼
┌──────────────────────────┐
│   UserService            │
│   - Find user by email   │
│   - Check account locked?│
│   - Compare password     │
│   - Generate JWT         │
└────┬─────────────────────┘
     │
     │ 3. Request JWT from JwtService
     ▼
┌──────────────────────────┐
│   JwtService             │
│   - Create HS256 token   │
│   - Add claims           │
│   - Set expiration       │
└────┬─────────────────────┘
     │
     │ 4. Return JWT + metadata
     ▼
┌──────────────┐
│   Client     │
│   200 OK     │
│   {token, ..}│
└──────────────┘
```

#### Protected Request Flow
```
┌──────────────┐
│   Client     │
│   Header:    │
│   Auth:      │
│   Bearer TOKEN
└────┬─────────┘
     │
     │ Request with JWT
     ▼
┌────────────────────────────┐
│   JwtAuthenticationFilter   │
│   - Extract token          │
│   - Validate signature      │
│   - Parse claims           │
│   - Set SecurityContext    │
└────┬───────────────────────┘
     │
     │ Token valid?
     ├─ YES ──┐
     │        │
     │        ▼
     │   ┌──────────────┐
     │   │ Controller   │
     │   │ (Proceed)    │
     │   └──────────────┘
     │
     └─ NO ──┐
            │
            ▼
       401 Unauthenticated
```

#### Tour Search Flow
```
┌──────────────┐
│   Client     │
└────┬─────────┘
     │
     │ GET /tours/available?destination=Paris&adults=2
     ▼
┌──────────────────────────┐
│   TourController         │
│   getAvailableTours()    │
└────┬─────────────────────┘
     │
     │ 1. Call tourService.getAvailableTours()
     ▼
┌──────────────────────────────┐
│   TourService                │
│   - Build MongoDB Query      │
│   - Apply filters            │
│   - Apply sorting            │
│   - Calculate pagination     │
└────┬───────────────────────────┘
     │
     │ 2. Execute query
     ▼
┌──────────────────────────┐
│   MongoTemplate / Repo   │
│   - Filter tours by      │
│     destination          │
│   - Match capacity       │
│   - Sort by rating       │
│   - Paginate results     │
└────┬─────────────────────┘
     │
     │ 3. Map to DTOs
     ▼
┌──────────────┐
│   Client     │
│   200 OK     │
│   {tours, ..}│
└──────────────┘
```

---

## Data Models

### DTOs (Data Transfer Objects)

#### SignUpRequestDTO
```json
{
  "firstName": "John",
  "lastName": "Doe",
  "email": "john@example.com",
  "password": "SecurePass123!"
}
```

#### SignUpResponseDTO
```json
{
  "ok": true,
  "message": "User created successfully"
}
```

#### SignInRequestDTO
```json
{
  "email": "john@example.com",
  "password": "SecurePass123!"
}
```

#### SignInResponseDTO
```json
{
  "token": "eyJhbGc...",
  "role": "CUSTOMER",
  "userName": "John Doe",
  "email": "john@example.com",
  "expiresAt": "2025-05-05T10:30:00Z"
}
```

#### UserResponseDTO
```json
{
  "id": "507f1f77bcf86cd799439011",
  "firstName": "John",
  "lastName": "Doe",
  "email": "john@example.com",
  "role": "CUSTOMER"
}
```

#### TourListResponseDTO
```json
{
  "tours": [
    {
      "id": "...",
      "name": "Paris Vacation",
      "destination": "Paris",
      "summary": "...",
      "imageUrl": "...",
      "rating": 4.8,
      "reviewCount": 125,
      "startingPrice": "$999"
    }
  ],
  "totalPages": 10,
  "currentPage": 1,
  "pageSize": 6,
  "totalItems": 60
}
```

#### TourDetailResponseDTO
```json
{
  "id": "...",
  "name": "Paris Vacation",
  "destination": "Paris",
  "summary": "...",
  "imageUrls": ["..."],
  "rating": 4.8,
  "reviewCount": 125,
  "startDates": ["2025-06-01", "2025-06-15"],
  "durations": ["5 days", "7 days"],
  "pricePerDuration": {"5 days": "$999", "7 days": "$1299"},
  "mealPlans": ["Breakfast", "Half Board", "Full Board"],
  "accommodation": "4-star hotel",
  "totalCapacity": 100,
  "bookedCount": 85,
  "freeCancellationDaysBefore": 7
}
```

#### ReviewListResponseDTO
```json
{
  "reviews": [
    {
      "id": "...",
      "userName": "Jane Doe",
      "userAvatarUrl": "...",
      "rate": 5.0,
      "comment": "Amazing experience!",
      "reviewDate": "2025-04-20"
    }
  ],
  "totalPages": 5,
  "currentPage": 1,
  "pageSize": 4,
  "totalItems": 18
}
```

#### DestinationListResponseDTO
```json
{
  "destinations": ["Paris", "Punta Cana", "Phuket", "Pune"]
}
```

---

## Project Flow

### 1. User Onboarding Flow

```
New User
   │
   ├─ Fill registration form
   │  (firstName, lastName, email, password)
   │
   ▼
┌─────────────────────────────────┐
│  Validations                    │
├─────────────────────────────────┤
│ ✓ Password ≥ 8 chars            │
│ ✓ Password has uppercase        │
│ ✓ Password not same as email    │
│ ✓ Password not same as firstName│
│ ✓ Email format valid            │
│ ✓ Email not already registered  │
└─────────────────────────────────┘
   │
   ├─ Validations Pass?
   │  ├─ YES ──┐
   │  └─ NO ──┤────► Show error
   │          │
   │          ▼
   ▼      Take Input Again
┌─────────────────────────────────┐
│  Hash password with BCrypt      │
│  (12 rounds)                    │
└─────────────────────────────────┘
   │
   ▼
┌─────────────────────────────────┐
│  Save user to MongoDB           │
│  - id (auto-generated)          │
│  - firstName, lastName          │
│  - email (unique index)         │
│  - passwordHash                 │
│  - role = CUSTOMER              │
│  - failedLoginAttempts = 0      │
│  - createdAt (auto)             │
└─────────────────────────────────┘
   │
   ▼
┌─────────────────────────────────┐
│  201 Created Response           │
│  Show success message           │
└─────────────────────────────────┘
   │
   ▼
User Registered Successfully
```

### 2. User Login Flow

```
User Login
   │
   ├─ Enter email & password
   │
   ▼
┌──────────────────────────────────┐
│  Validations                     │
├──────────────────────────────────┤
│ ✓ Email format valid             │
│ ✓ Password not empty             │
└──────────────────────────────────┘
   │
   ▼
┌──────────────────────────────────┐
│  Query MongoDB for user by email │
└──────────────────────────────────┘
   │
   ├─ User found?
   │
   ├─ NO ──┐
   │       └──► 401 Unauthorized
   │           "Invalid email/password"
   │
   ├─ YES ──┐
   │        ▼
   │   ┌──────────────────────────┐
   │   │ Is account locked?       │
   │   │ (lockedUntil > now)      │
   │   └──────────────────────────┘
   │        │
   │        ├─ YES ──┐
   │        │        └──► 423 Locked
   │        │            "Account locked for X sec"
   │        │
   │        ├─ NO ──┐
   │        │       ▼
   │        │   ┌──────────────────────────┐
   │        │   │ Compare submitted        │
   │        │   │ password with hash       │
   │        │   │ (BCrypt)                 │
   │        │   └──────────────────────────┘
   │        │        │
   │        │        ├─ NO MATCH ──┐
   │        │        │             ▼
   │        │        │        Increment
   │        │        │        failedAttempts
   │        │        │             │
   │        │        │        ┌────────────────┐
   │        │        │        │ Attempts ≥ 5?  │
   │        │        │        └────────────────┘
   │        │        │             │
   │        │        │        ├─ YES ──┐
   │        │        │        │        └──► Lock account
   │        │        │        │            Set lockedUntil
   │        │        │        │            (now + 15 min)
   │        │        │        │
   │        │        │        └─ NO ──► 401 Unauthorized
   │        │        │
   │        │        └─ MATCH ──┐
   │        │                   ▼
   │        │            Reset failedAttempts = 0
   │        │            Clear lockedUntil
   │        │                   │
   │        └────────────────────┴──► Generate JWT
   │                                 (userId,
   │                                  email,
   │                                  firstName,
   │                                  TTL=24h)
   │
   ▼
┌──────────────────────────────────┐
│  200 OK Response with JWT        │
├──────────────────────────────────┤
│ - token (Bearer)                 │
│ - role (CUSTOMER/ADMIN)          │
│ - userName                       │
│ - email                          │
│ - expiresAt                      │
└──────────────────────────────────┘
   │
   ▼
User Logged In Successfully
```

### 3. Tour Discovery Flow

```
User Browses Tours
   │
   ├─ (Optional) Type destination prefix
   │
   ▼
┌──────────────────────────────────┐
│  GET /tours/destinations?        │
│      destination=Paris           │
└──────────────────────────────────┘
   │
   ▼
┌──────────────────────────────────┐
│  Validate query ≥ 3 chars        │
└──────────────────────────────────┘
   │
   ▼
┌──────────────────────────────────┐
│  MongoDB regex search             │
│  db.tours.find({                 │
│    destination: /paris/i         │
│  }).distinct("destination")      │
│  Limit to unique destinations    │
└──────────────────────────────────┘
   │
   ▼
┌──────────────────────────────────┐
│  200 OK with destinations         │
│  ["Paris", "Punta Cana",...]     │
└──────────────────────────────────┘
   │
   ├─ User selects filters
   │  - destination
   │  - startDate
   │  - duration
   │  - adults
   │  - children
   │  - mealPlan
   │  - tourType
   │  - sortBy (RATING_DESC, PRICE_ASC, etc.)
   │  - page, pageSize
   │
   ▼
┌──────────────────────────────────┐
│  GET /tours/available?...        │
└──────────────────────────────────┘
   │
   ▼
┌──────────────────────────────────┐
│  Build MongoDB Query              │
│  - Regex for destination         │
│  - startDates array contains     │
│  - guestQuantity matches         │
│  - Available capacity            │
│  - pricePerDuration has key      │
└──────────────────────────────────┘
   │
   ▼
┌──────────────────────────────────┐
│  Apply Sort & Pagination         │
│  - Skip: (page-1) * pageSize     │
│  - Limit: pageSize               │
│  - Sort by selected field        │
└──────────────────────────────────┘
   │
   ▼
┌──────────────────────────────────┐
│  200 OK with tour list            │
│  - tours (paginated)              │
│  - totalPages                     │
│  - currentPage                    │
│  - totalItems                     │
└──────────────────────────────────┘
   │
   ├─ User selects a tour
   │
   ▼
┌──────────────────────────────────┐
│  GET /tours/{tourId}              │
└──────────────────────────────────┘
   │
   ▼
┌──────────────────────────────────┐
│  Fetch tour details from MongoDB  │
│  by ID                            │
└──────────────────────────────────┘
   │
   ▼
┌──────────────────────────────────┐
│  200 OK with full tour details    │
│  - All tour information           │
│  - Pricing details                │
│  - Hotel info                     │
│  - Capacity & availability        │
└──────────────────────────────────┘
   │
   ├─ View reviews
   │
   ▼
┌──────────────────────────────────┐
│  GET /tours/{tourId}/reviews      │
│      ?sortBy=TOP_RATED_FIRST      │
│      &page=1&pageSize=4           │
└──────────────────────────────────┘
   │
   ▼
┌──────────────────────────────────┐
│  MongoDB query:                   │
│  - Find reviews for tourId        │
│  - Sort by rating DESC/date       │
│  - Paginate results               │
└──────────────────────────────────┘
   │
   ▼
┌──────────────────────────────────┐
│  200 OK with reviews              │
│  - Paginated review list          │
│  - User names & avatars           │
│  - Ratings & comments             │
│  - Review dates                   │
└──────────────────────────────────┘
   │
   ▼
User Reviews Tour Information
```

### 4. Protected Request Flow

```
Authenticated User Makes Request
   │
   ├─ Include JWT in header
   │  Authorization: Bearer <token>
   │
   ▼
┌──────────────────────────────────┐
│  JwtAuthenticationFilter          │
│  (Spring Security Filter Chain)   │
└──────────────────────────────────┘
   │
   ├─ Extract token from header
   │
   ▼
┌──────────────────────────────────┐
│  JwtService.parse(token)          │
│  - Verify HS256 signature         │
│  - Validate expiration            │
│  - Extract claims                 │
└──────────────────────────────────┘
   │
   ├─ Token valid?
   │
   ├─ NO (Invalid/Expired) ──┐
   │                         └──► Log & Skip
   │                              Set null auth
   │
   ├─ YES ──┐
   │        ▼
   │   ┌──────────────────────────┐
   │   │ Create Authentication    │
   │   │ - Principal: userId      │
   │   │ - Authorities: role      │
   │   │ - Credentials: token     │
   │   └──────────────────────────┘
   │        │
   │        ▼
   │   ┌──────────────────────────┐
   │   │ Set in SecurityContext   │
   │   │ (Thread-local holder)    │
   │   └──────────────────────────┘
   │        │
   │        ▼
   │   Request flows to Handler
   │        │
   │        ▼
   │   ┌──────────────────────────┐
   │   │ @GetMapping("/me")       │
   │   │ Get auth from context    │
   │   │ Extract userId           │
   │   │ Fetch user from DB       │
   │   │ Return UserResponseDTO   │
   │   └──────────────────────────┘
   │
   ▼
┌──────────────────────────────────┐
│  200 OK with user profile         │
└──────────────────────────────────┘
```

---

## API Endpoints

### Authentication Endpoints

#### 1. Sign Up
```
POST /api/v1/auth/sign-up
Content-Type: application/json

Request:
{
  "firstName": "John",
  "lastName": "Doe",
  "email": "john@example.com",
  "password": "SecurePass123!"
}

Response (201 Created):
{
  "ok": true,
  "message": "User registered successfully"
}

Error Responses:
- 400: Invalid input (validation errors)
- 409: Email already exists
- 400: Weak password
```

#### 2. Sign In
```
POST /api/v1/auth/sign-in
Content-Type: application/json

Request:
{
  "email": "john@example.com",
  "password": "SecurePass123!"
}

Response (200 OK):
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "role": "CUSTOMER",
  "userName": "John Doe",
  "email": "john@example.com",
  "expiresAt": "2025-05-05T10:30:00Z"
}

Error Responses:
- 401: Invalid credentials
- 423: Account locked (includes retryAfter header)
```

#### 3. Get Current User
```
GET /api/v1/auth/me
Authorization: Bearer <JWT_TOKEN>

Response (200 OK):
{
  "id": "507f1f77bcf86cd799439011",
  "firstName": "John",
  "lastName": "Doe",
  "email": "john@example.com",
  "role": "CUSTOMER"
}

Error Responses:
- 401: Unauthenticated (missing/invalid token)
```

### Tour Endpoints

#### 1. Search Destinations (Autocomplete)
```
GET /tours/destinations?destination=Par
Authorization: (optional)

Response (200 OK):
{
  "destinations": ["Paris", "Punta Cana", "Pattaya"]
}

Error Responses:
- 400: Query less than 3 characters
```

#### 2. Get Available Tours
```
GET /tours/available?destination=Paris&startDate=2025-06-01&adults=2&sortBy=RATING_DESC&page=1&pageSize=6
Authorization: (optional)

Query Parameters:
- destination (optional): Tour destination
- startDate (optional): ISO date (2025-06-01)
- duration (optional): e.g., "5 days"
- adults (optional): Number of adults
- children (optional): Number of children
- mealPlan (optional): e.g., "Half Board"
- tourType (optional): e.g., "Beach"
- sortBy (optional): RATING_DESC, PRICE_ASC, PRICE_DESC, NEWEST_FIRST
- page (optional, default: 1): Page number
- pageSize (optional, default: 6): Items per page

Response (200 OK):
{
  "tours": [
    {
      "id": "...",
      "name": "Paris City Tour",
      "destination": "Paris",
      "summary": "Experience the magic of Paris",
      "imageUrl": "https://...",
      "rating": 4.8,
      "reviewCount": 125,
      "startingPrice": "$999"
    }
  ],
  "totalPages": 10,
  "currentPage": 1,
  "pageSize": 6,
  "totalItems": 60
}
```

#### 3. Get Tour Details
```
GET /tours/{tourId}
Authorization: (optional)

Parameter:
- tourId: MongoDB ObjectId

Response (200 OK):
{
  "id": "507f1f77bcf86cd799439011",
  "name": "Paris City Tour",
  "destination": "Paris",
  "summary": "Experience the magic of Paris",
  "imageUrls": ["https://...", "https://..."],
  "rating": 4.8,
  "reviewCount": 125,
  "startDates": ["2025-06-01", "2025-06-15", "2025-07-01"],
  "durations": ["5 days", "7 days"],
  "pricePerDuration": {
    "5 days": "$999",
    "7 days": "$1299"
  },
  "mealPlans": ["Breakfast", "Half Board", "Full Board"],
  "mealSupplementsPerDay": {
    "Vegetarian": "$15",
    "Vegan": "$20"
  },
  "tourType": "City Tour",
  "hotelName": "Hotel Le Marais",
  "hotelDescription": "4-star luxury hotel in the heart of Paris",
  "accommodation": "Double room with city view",
  "customDetails": {
    "language": "English",
    "groupSize": "Max 30 people"
  },
  "guestQuantity": {
    "adult": 2,
    "child": 1
  },
  "totalCapacity": 100,
  "bookedCount": 85,
  "freeCancellationDaysBefore": 7
}

Error Responses:
- 404: Tour not found
```

#### 4. Get Tour Reviews
```
GET /tours/{tourId}/reviews?sortBy=TOP_RATED_FIRST&page=1&pageSize=4
Authorization: (optional)

Parameters:
- tourId: MongoDB ObjectId
- sortBy (optional, default: TOP_RATED_FIRST): TOP_RATED_FIRST, MOST_RECENT_FIRST
- page (optional, default: 1): Page number
- pageSize (optional, default: 4): Items per page

Response (200 OK):
{
  "reviews": [
    {
      "id": "507f1f77bcf86cd799439012",
      "userName": "Jane Smith",
      "userAvatarUrl": "https://...",
      "rate": 5.0,
      "comment": "Absolutely amazing experience! The tour guide was excellent.",
      "reviewDate": "2025-04-20"
    },
    {
      "id": "507f1f77bcf86cd799439013",
      "userName": "Mike Johnson",
      "userAvatarUrl": "https://...",
      "rate": 4.5,
      "comment": "Great value for money. Would recommend to friends.",
      "reviewDate": "2025-04-15"
    }
  ],
  "totalPages": 5,
  "currentPage": 1,
  "pageSize": 4,
  "totalItems": 18
}

Error Responses:
- 404: Tour not found
```

---

## Security & Authentication

### 1. Password Security

**Password Requirements:**
- Minimum 8 characters
- At least one uppercase letter
- At least one digit
- Cannot contain firstName
- Cannot contain email local part
- Cannot contain special sequences

**Password Storage:**
- Hashed using BCrypt with 12 rounds
- Never stored in plaintext
- Cost factor of 12 provides strong security

### 2. JWT Token Security

**Token Structure:**
```
Header: {
  "alg": "HS256",
  "typ": "JWT"
}

Payload: {
  "sub": "userId",        // Subject (user ID)
  "email": "user@ex.com", // Custom claim
  "firstName": "John",    // Custom claim
  "iat": 1714848600,     // Issued at
  "exp": 1714935000      // Expires at
}

Signature: HMACSHA256(base64(header) + "." + base64(payload), secret)
```

**Token Security:**
- Signed with HS256 algorithm
- Secret key ≥ 32 bytes (256 bits)
- Configurable TTL (default: 24 hours)
- Stateless verification

### 3. Account Lockout Mechanism

**Flow:**
1. User enters wrong password → `failedLoginAttempts++`
2. After 5 failed attempts → Account locked
3. Locked until `createdAt + 15 minutes`
4. Successful login → Reset counter

**Configuration:**
- `app.auth.max-failed-attempts` = 5 (default)
- `app.auth.lockout-minutes` = 15 (default)

### 4. Authentication Flow

**Without Token:**
- Public endpoints accessible
- Example: GET /tours/available

**With Token:**
- Include in Authorization header: `Authorization: Bearer <token>`
- JwtAuthenticationFilter validates
- Claims extracted and stored in SecurityContext
- Controller can access via `SecurityContextHolder.getContext().getAuthentication()`

**Token Validation:**
```java
JwtService.parse(token)
  ├─ Decode Base64
  ├─ Verify signature with secret key
  ├─ Check expiration timestamp
  ├─ Extract claims
  └─ Throw InvalidJwtException if invalid
```

### 5. CORS Configuration

**Allowed:**
- Cross-origin requests enabled
- Credentials can be sent
- All methods allowed (GET, POST, PUT, DELETE, etc.)

### 6. Security Endpoints

**Permitted to All:**
- POST /api/v1/auth/sign-up
- POST /api/v1/auth/sign-in
- GET /actuator/health
- GET /tours/* (tour browsing)

**Requires Authentication:**
- GET /api/v1/auth/me
- Any endpoint matching /api/**

---

## Error Handling

### Global Exception Handler

All exceptions are caught by `GlobalExceptionHandler` and return consistent error responses.

### Error Response Format

```json
{
  "timestamp": "2025-05-04T10:30:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Description of what went wrong",
  "fieldErrors": {
    "fieldName": "Error message for field"
  }
}
```

### Exception Types

#### 1. EmailAlreadyExistsException
```
Status: 409 Conflict
Message: "Email already exists"

Example Response:
{
  "timestamp": "...",
  "status": 409,
  "error": "Conflict",
  "message": "Email already exists",
  "fieldErrors": {
    "email": "An account with this email already exists"
  }
}
```

#### 2. WeakPasswordException
```
Status: 400 Bad Request
Message: Password validation error

Example Response:
{
  "timestamp": "...",
  "status": 400,
  "error": "Bad Request",
  "message": "Password must not contain your first name",
  "fieldErrors": {
    "password": "Password must not contain your first name"
  }
}
```

#### 3. InvalidCredentialsException
```
Status: 401 Unauthorized
Message: "Wrong password or email"

Example Response:
{
  "timestamp": "...",
  "status": 401,
  "error": "Unauthorized",
  "message": "Wrong password or email",
  "fieldErrors": {
    "email": "Invalid email or password",
    "password": "Invalid email or password"
  }
}
```

#### 4. AccountLockedException
```
Status: 423 Locked
Headers: Retry-After: <seconds>
Message: Account is temporarily locked

Example Response:
{
  "timestamp": "...",
  "status": 423,
  "error": "Locked",
  "message": "Account locked. Retry after 900 seconds",
  "retryAfter": 900
}
```

#### 5. UnauthenticatedException
```
Status: 401 Unauthorized
Message: "Missing or invalid token"

Example Response:
{
  "timestamp": "...",
  "status": 401,
  "error": "Unauthenticated",
  "message": "Authentication is required"
}
```

#### 6. Validation Errors
```
Status: 400 Bad Request
Message: "Invalid input provided"

Example Response:
{
  "timestamp": "...",
  "status": 400,
  "error": "Bad Request",
  "message": "Invalid input provided",
  "fieldErrors": {
    "email": "must be a valid email address",
    "firstName": "must not be blank"
  }
}
```

---

## Deployment

### Docker

**File:** `Dockerfile`
```dockerfile
FROM public.ecr.aws/docker/library/openjdk:22-ea-17-slim-bookworm
COPY ./target/*.jar /home/app.jar
CMD ["java","-jar","/home/app.jar"]
```

**Building Image:**
```bash
mvn clean package
docker build -t travel-agency:latest .
docker run -p 8080:8080 travel-agency:latest
```

### Kubernetes with Helm

**Chart Location:** `deploy-templates/`

**Structure:**
```
deploy-templates/
├── Chart.yaml                 # Chart metadata
├── values.yaml                # Configuration values
├── templates/
│   ├── deployment.yaml        # K8s Deployment spec
│   ├── service.yaml           # K8s Service definition
│   ├── ingress.yaml           # Ingress route
│   ├── hpa.yaml               # Horizontal Pod Autoscaler
│   ├── serviceaccount.yaml    # Service account
│   ├── _helpers.tpl           # Helm template helpers
│   ├── NOTES.txt              # Deployment notes
│   └── tests/
│       └── test-connection.yaml
```

**Deploying to Kubernetes:**
```bash
helm install travel-agency ./deploy-templates \
  --namespace default \
  --create-namespace
```

**Configuration:**
- Replicas: 2 (default)
- Resource limits: Memory and CPU constraints
- Health checks: Liveness and readiness probes
- Auto-scaling: HPA based on CPU/memory

### Environment Configuration

**Properties File:** `src/main/resources/application.properties`

```properties
# MongoDB Configuration
spring.data.mongodb.uri=mongodb://localhost:27017/travelagency
spring.data.mongodb.database=travelagency

# Server Configuration
server.port=8080

# JWT Configuration
app.jwt.secret=<base64-encoded-secret-min-32-bytes>
app.jwt.ttl-hours=24

# Authentication Configuration
app.auth.max-failed-attempts=5
app.auth.lockout-minutes=15

# Logging
logging.level.org.springframework.data.mongodb=DEBUG
```

### Build & Run

**Build:**
```bash
mvn clean package
```

**Run Locally:**
```bash
mvn spring-boot:run
```

**Run JAR:**
```bash
java -jar target/java-maven-springboot-0.0.1-SNAPSHOT.jar
```

### Health Check

**Endpoint:**
```
GET /actuator/health
```

**Response:**
```json
{
  "status": "UP",
  "components": {
    "db": { "status": "UP" },
    "mongoDb": { "status": "UP" }
  }
}
```

---

## Development Setup

### Prerequisites
- Java 17+
- Maven 3.8+
- MongoDB 5.0+
- Git

### Local Setup

1. **Clone Repository:**
```bash
git clone <repository-url>
cd travel-agency
```

2. **Install Dependencies:**
```bash
mvn clean install
```

3. **Configure MongoDB:**
```bash
# Start MongoDB locally
mongod
```

4. **Set Environment Variables:**
```bash
export APP_JWT_SECRET="base64-encoded-secret-key"
export SPRING_DATA_MONGODB_URI="mongodb://localhost:27017/travelagency"
```

5. **Run Application:**
```bash
mvn spring-boot:run
```

6. **Test API:**
```bash
curl -X POST http://localhost:8080/api/v1/auth/sign-up \
  -H "Content-Type: application/json" \
  -d '{
    "firstName": "John",
    "lastName": "Doe",
    "email": "john@example.com",
    "password": "SecurePass123!"
  }'
```

---

## Summary

This Travel Agency Platform provides:

✅ **Robust Authentication**: JWT-based stateless authentication with account lockout
✅ **Comprehensive Tour Management**: Search, filter, and browse tours with detailed information
✅ **Secure Data Storage**: MongoDB with proper indexing and relationships
✅ **Production-Ready**: Docker & Kubernetes deployment, health checks, logging
✅ **Scalable Architecture**: Layered design with clear separation of concerns
✅ **Error Handling**: Centralized exception handling with meaningful error messages
✅ **API Documentation**: Well-structured RESTful endpoints with clear contracts

The system is designed to scale horizontally, handle concurrent requests efficiently, and provide a secure, user-friendly experience for travelers and tour administrators.
