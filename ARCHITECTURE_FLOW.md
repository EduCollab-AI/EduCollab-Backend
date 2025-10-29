# 🏗️ Architecture Flow Documentation

## ✅ Your Flow is Correctly Set Up!

**Flow:** Flutter App → Railway Backend API → Supabase Database

## 📊 Complete Flow Diagram

```
┌─────────────────┐
│   Flutter App   │
│  (User Action)  │
└────────┬────────┘
         │
         │ HTTP POST Request
         │ /api/v1/auth/register
         ▼
┌─────────────────────────────────────┐
│      Railway Backend API            │
│  educollab-backend-production...    │
└────────┬────────────────────────────┘
         │
         │ 1. Receives Request
         │ 2. Calls Supabase Auth API
         │ 3. Saves to Database
         │
         ▼
┌─────────────────────────────────────┐
│      Supabase Database              │
│  - auth.users (via Supabase API)   │
│  - public.users (via JPA)          │
└─────────────────────────────────────┘
```

## 🔍 Code Flow Breakdown

### Step 1: Flutter App (User Action)

**File:** `lib/services/auth_service.dart`

**Location:** Lines 23-49

```dart
Future<ApiResponse<AuthResponse>> register(RegisterRequest request) async {
  // Makes HTTP POST request to Railway backend
  final response = await _httpClient.post(
    ApiConfig.register,  // http://localhost:8080/api/v1/auth/register (local)
                         // OR https://educollab-backend-production.../api/v1/auth/register (production)
    data: request.toJson(),
  );
  // ... processes response
}
```

**API Config:** `lib/config/api_config.dart`
- Line 4: `environment = 'production'` or `'local'`
- Lines 7-8: Base URLs for local and production
- Line 11: Returns appropriate URL based on environment

---

### Step 2: Railway Backend API (Receives Request)

**File:** `backend/src/main/java/com/educollab/controller/AuthController.java`

**Location:** Lines 22-26

```java
@PostMapping("/register")
public Map<String, Object> register(@RequestBody Map<String, Object> request) {
    System.out.println("Register endpoint accessed with data: " + request);
    return authService.register(request);  // Delegates to service layer
}
```

**REST Endpoint:** `/api/v1/auth/register`
- Receives JSON payload from Flutter app
- Passes request to `AuthService` for processing

---

### Step 3: Backend Service (Business Logic)

**File:** `backend/src/main/java/com/educollab/service/AuthService.java`

**Location:** Lines 23-167

**What it does:**

1. **Calls Supabase Auth API** (Lines 38-44):
   ```java
   WebClient webClient = supabaseConfig.supabaseWebClient();
   Map<String, Object> authResponse = webClient.post()
       .uri("/auth/v1/signup")
       .bodyValue(authRequest)
       .retrieve()
       .bodyToMono(Map.class)
       .block();
   ```
   - Creates user in Supabase Auth (`auth.users` table)

2. **Saves to Database** (Lines 55-70):
   ```java
   User user = userRepository.findById(UUID.fromString(userId)).orElse(null);
   
   if (user == null) {
       user = new User();
       user.setId(UUID.fromString(userId));
       user.setEmail((String) request.get("email"));
       // ... set other fields
       userRepository.save(user);  // ✅ Saves to Supabase public.users table
   }
   ```

---

### Step 4: Database Operations

**File:** `backend/src/main/java/com/educollab/repository/UserRepository.java`

**Location:** Lines 10-13

```java
@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
}
```

**What it does:**
- `JpaRepository` provides `save()`, `findById()`, `delete()`, etc.
- Automatically translates to SQL queries
- Connects to Supabase PostgreSQL database via `DATABASE_URL`

**File:** `backend/src/main/java/com/educollab/model/User.java`

**Location:** Lines 7-37

```java
@Entity
@Table(name = "users")
public class User {
    @Id
    @Column(name = "id")
    private UUID id;
    
    @Column(name = "email", unique = true, nullable = false)
    private String email;
    // ... other fields
}
```

**What it does:**
- Maps Java object to `public.users` table in Supabase
- JPA annotations define table structure

---

## 🔄 Complete Example: Registration Flow

### 1. User fills registration form in Flutter app

**Trigger:** User clicks "Register" button

**Code:** `lib/screens/auth/parent_register_form.dart`
```dart
// User enters email, password, name, etc.
// Calls: AuthService().register(registerRequest)
```

### 2. Flutter app sends HTTP request

**Request:**
```http
POST https://educollab-backend-production.up.railway.app/api/v1/auth/register
Content-Type: application/json

{
  "email": "user@example.com",
  "password": "password123",
  "firstName": "John",
  "lastName": "Doe",
  "role": "parent",
  "phone": "1234567890"
}
```

**Code:** `lib/services/auth_service.dart` → `_httpClient.post()`

### 3. Railway backend receives request

**Endpoint:** `AuthController.register()`

**Code:** `backend/src/main/java/com/educollab/controller/AuthController.java`

### 4. Backend processes request

**Service:** `AuthService.register()`

**Actions:**
- Calls Supabase Auth API → Creates user in `auth.users`
- Saves user profile to `public.users` table via JPA
- Returns success response with user data and tokens

**Code:** `backend/src/main/java/com/educollab/service/AuthService.java`

### 5. Database changes

**Supabase Tables Updated:**
- `auth.users` → User authentication data (via Supabase API)
- `public.users` → User profile data (via JPA/PostgreSQL)

**Code:** `backend/src/main/java/com/educollab/repository/UserRepository.java` → `save()`

### 6. Response sent back to Flutter

**Response:**
```json
{
  "success": true,
  "message": "Registration successful",
  "data": {
    "user": {
      "id": "uuid-here",
      "email": "user@example.com",
      "firstName": "John",
      "lastName": "Doe",
      "role": "parent"
    },
    "accessToken": "jwt-token-here",
    "refreshToken": "refresh-token-here",
    "expiresIn": 3600
  }
}
```

**Code:** `lib/services/auth_service.dart` → Processes response and updates app state

---

## 📁 Key Files Reference

### Flutter App (Client Side)
- **`lib/config/api_config.dart`** - API endpoints configuration
- **`lib/services/auth_service.dart`** - Makes HTTP requests to backend
- **`lib/services/http_client.dart`** - HTTP client wrapper (Dio)

### Backend API (Railway)
- **`backend/src/main/java/com/educollab/controller/AuthController.java`** - REST endpoints
- **`backend/src/main/java/com/educollab/service/AuthService.java`** - Business logic
- **`backend/src/main/java/com/educollab/repository/UserRepository.java`** - Database operations
- **`backend/src/main/java/com/educollab/model/User.java`** - Database entity model
- **`backend/src/main/java/com/educollab/config/SupabaseConfig.java`** - Supabase API client

### Database (Supabase)
- **`supabase_schema_simplified.sql`** - Table definitions
- **`supabase_rls_policies.sql`** - Security policies
- **`supabase_user_trigger.sql`** - Auto-create user profiles

---

## ✅ Verification Checklist

Your flow is correctly set up if:

- ✅ Flutter app sends requests to Railway backend (`ApiConfig.baseUrl`)
- ✅ Backend receives requests at `/api/v1/auth/register`
- ✅ Backend calls Supabase Auth API
- ✅ Backend saves data to Supabase PostgreSQL via JPA
- ✅ Response returns to Flutter app
- ✅ Database tables exist in Supabase
- ✅ RLS policies allow backend operations

---

## 🚀 How to Add New Features

To add a new feature (e.g., creating a course):

1. **Create Model:** `backend/src/main/java/com/educollab/model/Course.java`
2. **Create Repository:** `backend/src/main/java/com/educollab/repository/CourseRepository.java`
3. **Create Service:** `backend/src/main/java/com/educollab/service/CourseService.java`
4. **Create Controller:** `backend/src/main/java/com/educollab/controller/CourseController.java`
5. **Add Flutter API Call:** `lib/services/course_service.dart`
6. **Call from UI:** `lib/screens/...`

Follow the same pattern as `AuthService` and `AuthController`!

---

## 🔍 Testing the Flow

### Test Registration:
```bash
# Test backend directly
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"test123","firstName":"Test","lastName":"User","role":"parent","phone":"1234567890"}'
```

### Verify Database:
1. Go to Supabase Dashboard
2. Open **Table Editor** → **users**
3. Should see new user record!

---

Your architecture is **correctly set up** and follows best practices! 🎉

