# 📚 Supabase API Reference Guide

## How Supabase APIs Work

Supabase provides two types of APIs:

1. **REST API** - For database operations (PostgreSQL)
2. **Auth API** - For authentication operations

Both are accessed via HTTP requests to your Supabase project URL.

---

## 🔍 Finding Supabase API Endpoints

### Method 1: Supabase Dashboard (Recommended)

1. **Go to Supabase Dashboard**
   - https://supabase.com/dashboard
   - Select your project

2. **Open API Documentation**
   - Click **Settings** (gear icon) → **API**
   - Scroll down to **REST API** section
   - You'll see your Project URL: `https://xxxxx.supabase.co`

3. **Check API Reference**
   - Supabase provides auto-generated API docs
   - Go to: `https://supabase.com/docs/reference`

### Method 2: Supabase REST API Structure

All Supabase APIs follow this pattern:

```
https://{PROJECT_REF}.supabase.co/{SERVICE}/{VERSION}/{ENDPOINT}
```

**Examples:**
- Auth API: `https://xxxxx.supabase.co/auth/v1/signup`
- Database API: `https://xxxxx.supabase.co/rest/v1/users`

---

## 🔐 Supabase Auth API Endpoints

**Base URL:** `https://{PROJECT_REF}.supabase.co/auth/v1`

### Common Auth Endpoints:

| Endpoint | Method | Purpose | Example |
|----------|--------|---------|---------|
| `/auth/v1/signup` | POST | Register new user | Sign up with email/password |
| `/auth/v1/token?grant_type=password` | POST | Login user | Sign in with email/password |
| `/auth/v1/user` | GET | Get current user | Get authenticated user info |
| `/auth/v1/logout` | POST | Logout user | Sign out current user |
| `/auth/v1/recover` | POST | Password reset | Send password reset email |
| `/auth/v1/verify` | POST | Verify email | Verify user email |

### 📖 Official Documentation:
- **Auth API Docs:** https://supabase.com/docs/reference/api/auth-sign-up
- **JavaScript Client:** https://supabase.com/docs/reference/javascript/auth-signup
- **REST API Reference:** https://supabase.com/docs/reference/api/introduction

### 🔍 How to Find Auth Endpoints:

1. **Go to Supabase Docs:** https://supabase.com/docs/reference/api
2. **Click "Auth" section** → See all Auth endpoints
3. **Or use client libraries** (JavaScript, Python, etc.) which wrap these endpoints

---

## 🗄️ Supabase REST API (Database) Endpoints

**Base URL:** `https://{PROJECT_REF}.supabase.co/rest/v1`

### Database Endpoints Pattern:

```
GET    /rest/v1/{table_name}           - List all rows
GET    /rest/v1/{table_name}?id=eq.{id} - Get specific row
POST   /rest/v1/{table_name}           - Insert new row
PATCH  /rest/v1/{table_name}?id=eq.{id} - Update row
DELETE /rest/v1/{table_name}?id=eq.{id} - Delete row
```

### Examples:

| Table | Endpoint | Purpose |
|-------|----------|---------|
| `users` | `/rest/v1/users` | CRUD operations on users table |
| `courses` | `/rest/v1/courses` | CRUD operations on courses table |
| `payments` | `/rest/v1/payments` | CRUD operations on payments table |

### 📖 Official Documentation:
- https://supabase.com/docs/reference/javascript/select
- https://supabase.com/docs/reference/javascript/insert

---

## 🔑 Understanding "auth.users" vs "public.users"

### Important Distinction:

**`auth.users`** (Supabase Auth Table)
- **Managed by Supabase Auth API**
- Contains: `id`, `email`, `encrypted_password`, `created_at`
- **NOT directly accessible via REST API**
- Must use Auth API endpoints: `/auth/v1/signup`, `/auth/v1/token`

**`public.users`** (Your Custom Table)
- **Managed by Supabase REST API or JPA**
- Contains: `id`, `email`, `first_name`, `last_name`, `role`, `phone`, etc.
- **Accessible via REST API**: `/rest/v1/users`
- Can also use JPA/PostgreSQL directly

---

## 💻 Code Examples

### Example 1: Register User (Auth API)

**What we're doing:** Creating a user in `auth.users` table

**Code Location:** `backend/src/main/java/com/educollab/service/AuthService.java` (Line 39-44)

```java
WebClient webClient = supabaseConfig.supabaseWebClient();
Map<String, Object> authResponse = webClient.post()
    .uri("/auth/v1/signup")  // ← Supabase Auth API endpoint
    .bodyValue(authRequest)
    .retrieve()
    .bodyToMono(Map.class)
    .block();
```

**Full URL:** `https://xxxxx.supabase.co/auth/v1/signup`

**How I knew this:**
- Supabase Auth API documentation
- Standard endpoint for user registration
- Listed in Supabase docs: https://supabase.com/docs/reference/javascript/auth-signup

---

### Example 2: Save User Profile (REST API via JPA)

**What we're doing:** Saving user profile to `public.users` table

**Code Location:** `backend/src/main/java/com/educollab/service/AuthService.java` (Line 70)

```java
userRepository.save(user);  // ← Uses JPA to save to database
```

**What happens behind the scenes:**
- JPA generates SQL: `INSERT INTO public.users (...) VALUES (...)`
- Executes against Supabase PostgreSQL database
- Uses `DATABASE_URL` connection string

**Alternative (Direct REST API):**
```java
// You could also do this directly:
webClient.post()
    .uri("/rest/v1/users")  // ← Supabase REST API endpoint
    .bodyValue(userData)
    .retrieve()
    .bodyToMono(Map.class)
    .block();
```

---

## 📖 Where to Find API Endpoints

### 1. Supabase Official Documentation

**Auth API:**
- https://supabase.com/docs/reference/javascript/auth-signup
- https://supabase.com/docs/reference/javascript/auth-signinwithpassword
- https://supabase.com/docs/reference/javascript/auth-logout

**REST API:**
- https://supabase.com/docs/reference/javascript/select
- https://supabase.com/docs/reference/javascript/insert
- https://supabase.com/docs/reference/javascript/update

### 2. Your Supabase Dashboard

1. Go to **Settings** → **API**
2. See **Project URL** and **API Keys**
3. Scroll to **REST API** section for examples

### 3. API Reference Browser

- Go to: https://supabase.com/docs/reference
- Search for what you need (e.g., "signup", "insert", "select")

---

## 🎯 How We Determined Our Endpoints

### For Registration:

1. **Need:** Register user with email/password
2. **Where to look:**
   - Supabase Docs: https://supabase.com/docs/reference/api → Auth section
   - Found: **POST** `/auth/v1/signup` endpoint
3. **Reference:** 
   - REST API: https://supabase.com/docs/reference/api/auth-sign-up
   - JS Client: https://supabase.com/docs/reference/javascript/auth-signup
4. **Implemented:** In `AuthService.java` line 40

**Important:** We use **Auth API** (`/auth/v1/*`) for authentication operations, NOT the REST API (`/rest/v1/*`). The `auth.users` table is managed by Supabase Auth, not directly accessible via REST API.

### For Login:

1. **Need:** Login user with email/password
2. **Search:** "Supabase login API"
3. **Found:** `/auth/v1/token?grant_type=password`
4. **Reference:** https://supabase.com/docs/reference/javascript/auth-signinwithpassword
5. **Implemented:** In `AuthService.java` line ~100 (login method)

### For Database Operations:

1. **Need:** Save user profile to database
2. **Options:**
   - Use JPA (easier, type-safe) ✅ We chose this
   - Use REST API directly (`/rest/v1/users`)
3. **Chose:** JPA because it's cleaner and handles SQL automatically
4. **Implementation:** `UserRepository.save()` in `AuthService.java` line 70

---

## 🔧 Common Supabase API Patterns

### Pattern 1: Auth Operations (Must use Auth API)

```java
// Register
POST /auth/v1/signup

// Login
POST /auth/v1/token?grant_type=password

// Get Current User
GET /auth/v1/user
  Header: Authorization: Bearer {access_token}

// Logout
POST /auth/v1/logout
  Header: Authorization: Bearer {access_token}
```

### Pattern 2: Database Operations (Use REST API or JPA)

**Via REST API:**
```java
// Get all users
GET /rest/v1/users

// Get user by ID
GET /rest/v1/users?id=eq.{uuid}

// Insert user
POST /rest/v1/users
  Body: { "email": "...", "first_name": "..." }

// Update user
PATCH /rest/v1/users?id=eq.{uuid}
  Body: { "first_name": "..." }

// Delete user
DELETE /rest/v1/users?id=eq.{uuid}
```

**Via JPA (What we're using):**
```java
// Get all users
userRepository.findAll()

// Get user by ID
userRepository.findById(uuid)

// Insert/Update user
userRepository.save(user)

// Delete user
userRepository.deleteById(uuid)
```

---

## 📝 Quick Reference Table

| Operation | Auth API | REST API | JPA |
|-----------|----------|----------|-----|
| Register User | `/auth/v1/signup` | ❌ | ❌ |
| Login User | `/auth/v1/token` | ❌ | ❌ |
| Get User Profile | `/auth/v1/user` | `/rest/v1/users` | `findById()` |
| Create Course | ❌ | `/rest/v1/courses` | `save()` |
| List Courses | ❌ | `/rest/v1/courses` | `findAll()` |
| Update Course | ❌ | `/rest/v1/courses` | `save()` |
| Delete Course | ❌ | `/rest/v1/courses` | `deleteById()` |

---

## 🎓 Key Takeaways

1. **Auth Operations** → Always use `/auth/v1/*` endpoints
2. **Database Operations** → Use `/rest/v1/{table}` OR JPA (we use JPA)
3. **Table `auth.users`** → Managed by Auth API only
4. **Table `public.users`** → Managed by REST API or JPA
5. **Find Endpoints** → Check Supabase documentation or dashboard

---

## 🔗 Useful Links

- **Supabase Auth API:** https://supabase.com/docs/reference/javascript/auth-signup
- **Supabase REST API:** https://supabase.com/docs/reference/javascript/select
- **Supabase PostgREST:** https://postgrest.org/en/stable/api.html (Advanced)
- **Your Dashboard:** https://supabase.com/dashboard → Settings → API

---

**TL;DR:** 
- Auth endpoints: `/auth/v1/*` (found in Supabase docs)
- Database endpoints: `/rest/v1/{table}` OR use JPA (what we chose)
- Check Supabase docs: https://supabase.com/docs/reference

