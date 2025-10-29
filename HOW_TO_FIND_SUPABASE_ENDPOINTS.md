# 🔍 How to Find Supabase Endpoints for Courses & Payments

## Quick Answer: Two Options

When working with database tables (courses, payments, etc.), you have **two options**:

### Option 1: Use JPA (Recommended - What We're Using)
- **No need to find Supabase endpoints!**
- JPA automatically generates SQL queries
- Type-safe, cleaner code
- Example: `courseRepository.save(course)`

### Option 2: Use Supabase REST API Directly
- **Endpoint pattern:** `/rest/v1/{table_name}`
- Example: `/rest/v1/courses`, `/rest/v1/payments`
- More control, but requires manual HTTP calls

---

## 🎯 Step-by-Step Guide: Adding Course/Payment Features

### Step 1: Check Your Database Schema

**Your tables are defined in:** `supabase_schema_simplified.sql`

**Courses table** (Line 30-49):
```sql
CREATE TABLE IF NOT EXISTS public.courses (
  id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
  name TEXT NOT NULL,
  code TEXT UNIQUE,
  ...
)
```

**Payments table** (Line 68-82):
```sql
CREATE TABLE IF NOT EXISTS public.payments (
  id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
  parent_id UUID REFERENCES public.users(id),
  ...
)
```

---

## 🛠️ Option 1: Using JPA (Recommended)

### This is What We Use - No Supabase Endpoints Needed!

**Why JPA?**
- Automatically generates SQL
- Type-safe
- Handles relationships automatically
- No need to know Supabase endpoints

### Example: Adding Course Feature

#### 1. Create Model
**File:** `backend/src/main/java/com/educollab/model/Course.java`

```java
@Entity
@Table(name = "courses")
public class Course {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;
    
    @Column(name = "name", nullable = false)
    private String name;
    
    @Column(name = "code", unique = true)
    private String code;
    
    @Column(name = "description")
    private String description;
    
    @Column(name = "price")
    private BigDecimal price;
    
    // ... other fields
    
    // Getters and setters
}
```

#### 2. Create Repository
**File:** `backend/src/main/java/com/educollab/repository/CourseRepository.java`

```java
@Repository
public interface CourseRepository extends JpaRepository<Course, UUID> {
    List<Course> findByTeacherId(UUID teacherId);
    Optional<Course> findByCode(String code);
}
```

#### 3. Create Service
**File:** `backend/src/main/java/com/educollab/service/CourseService.java`

```java
@Service
public class CourseService {
    
    @Autowired
    private CourseRepository courseRepository;
    
    public Course createCourse(Course course) {
        // JPA automatically saves to Supabase database!
        return courseRepository.save(course);
    }
    
    public Course updateCourse(UUID id, Course updatedCourse) {
        Course course = courseRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Course not found"));
        
        // Update fields
        course.setName(updatedCourse.getName());
        course.setPrice(updatedCourse.getPrice());
        // ... update other fields
        
        // JPA automatically updates the database!
        return courseRepository.save(course);
    }
    
    public List<Course> getAllCourses() {
        // JPA automatically queries Supabase!
        return courseRepository.findAll();
    }
}
```

#### 4. Create Controller
**File:** `backend/src/main/java/com/educollab/controller/CourseController.java`

```java
@RestController
@RequestMapping("/api/v1/courses")
@CrossOrigin(origins = "*")
public class CourseController {
    
    @Autowired
    private CourseService courseService;
    
    @PostMapping
    public Course createCourse(@RequestBody Course course) {
        return courseService.createCourse(course);
    }
    
    @PutMapping("/{id}")
    public Course updateCourse(@PathVariable UUID id, @RequestBody Course course) {
        return courseService.updateCourse(id, course);
    }
    
    @GetMapping
    public List<Course> getAllCourses() {
        return courseService.getAllCourses();
    }
}
```

**That's it!** JPA handles all database operations automatically. No Supabase endpoints needed!

---

## 🌐 Option 2: Using Supabase REST API Directly

### If You Want to Call Supabase Directly

**Endpoint Pattern:**
```
https://{PROJECT_REF}.supabase.co/rest/v1/{table_name}
```

### Course Endpoints:

| Operation | Method | Endpoint | Example |
|-----------|--------|----------|---------|
| List all courses | GET | `/rest/v1/courses` | Get all courses |
| Get course by ID | GET | `/rest/v1/courses?id=eq.{id}` | Get specific course |
| Create course | POST | `/rest/v1/courses` | Create new course |
| Update course | PATCH | `/rest/v1/courses?id=eq.{id}` | Update course |
| Delete course | DELETE | `/rest/v1/courses?id=eq.{id}` | Delete course |

### Payment Endpoints:

| Operation | Method | Endpoint | Example |
|-----------|--------|----------|---------|
| List all payments | GET | `/rest/v1/payments` | Get all payments |
| Get payment by ID | GET | `/rest/v1/payments?id=eq.{id}` | Get specific payment |
| Create payment | POST | `/rest/v1/payments` | Create new payment |
| Update payment | PATCH | `/rest/v1/payments?id=eq.{id}` | Update payment |
| Delete payment | DELETE | `/rest/v1/payments?id=eq.{id}` | Delete payment |

### Example: Direct REST API Call

```java
@Service
public class CourseService {
    
    @Autowired
    private SupabaseConfig supabaseConfig;
    
    public Map<String, Object> createCourse(Map<String, Object> courseData) {
        WebClient webClient = supabaseConfig.supabaseWebClient();
        
        // POST to /rest/v1/courses
        Map<String, Object> response = webClient.post()
            .uri("/rest/v1/courses")  // ← Supabase REST API endpoint
            .bodyValue(courseData)
            .retrieve()
            .bodyToMono(Map.class)
            .block();
        
        return response;
    }
    
    public Map<String, Object> updateCourse(UUID id, Map<String, Object> courseData) {
        WebClient webClient = supabaseConfig.supabaseWebClient();
        
        // PATCH to /rest/v1/courses?id=eq.{id}
        Map<String, Object> response = webClient.patch()
            .uri("/rest/v1/courses?id=eq." + id)  // ← Supabase REST API endpoint
            .bodyValue(courseData)
            .retrieve()
            .bodyToMono(Map.class)
            .block();
        
        return response;
    }
}
```

---

## 📚 How to Find Supabase REST API Endpoints

### Method 1: Use the Pattern

**For any table in `public` schema:**
```
/rest/v1/{table_name}
```

**Examples:**
- `courses` table → `/rest/v1/courses`
- `payments` table → `/rest/v1/payments`
- `users` table → `/rest/v1/users`
- `children` table → `/rest/v1/children`

### Method 2: Check Supabase Documentation

1. **Go to:** https://supabase.com/docs/reference/javascript/select
2. **See examples** for different operations
3. **Pattern is:** `/rest/v1/{table}`

### Method 3: Check Your Supabase Dashboard

1. **Go to:** Supabase Dashboard → **Settings** → **API**
2. **Scroll to REST API section**
3. **See examples** for your tables

### Method 4: Use PostgREST Documentation

Supabase uses PostgREST under the hood:
- **PostgREST Docs:** https://postgrest.org/en/stable/api.html
- **Pattern:** `/rest/v1/{table}` for all operations

---

## 🎯 Quick Decision Guide

### Use JPA (Recommended) ✅
- ✅ Simpler code
- ✅ Type-safe
- ✅ Automatic SQL generation
- ✅ Easy to test
- ✅ Handles relationships automatically

**When to use:** For most database operations

### Use REST API Directly
- ✅ More control
- ✅ Can use Supabase filters (eq, gt, etc.)
- ✅ Faster for simple queries
- ❌ More manual work
- ❌ Less type-safe

**When to use:** For complex queries or if you need Supabase-specific features

---

## 📝 Real Example: Our User Registration

**What we do:** Use JPA for database operations

**Code:** `backend/src/main/java/com/educollab/service/AuthService.java`

```java
// Line 70: Save user to database using JPA
userRepository.save(user);  // ← No Supabase endpoint needed!
```

**What happens:**
1. JPA generates SQL: `INSERT INTO public.users (...) VALUES (...)`
2. Executes against Supabase PostgreSQL database
3. User is saved to `public.users` table

**Alternative (REST API):**
```java
// Could also do this, but we don't:
webClient.post()
    .uri("/rest/v1/users")  // ← Supabase REST API endpoint
    .bodyValue(userData)
    .retrieve()
    .bodyToMono(Map.class)
    .block();
```

---

## 🚀 Recommended Approach for Courses & Payments

### Follow the Same Pattern as Users:

1. **Create Model** (`Course.java`, `Payment.java`)
2. **Create Repository** (`CourseRepository.java`, `PaymentRepository.java`)
3. **Create Service** (`CourseService.java`, `PaymentService.java`)
4. **Create Controller** (`CourseController.java`, `PaymentController.java`)

**Benefits:**
- ✅ Consistent with existing code
- ✅ No need to find Supabase endpoints
- ✅ JPA handles everything automatically
- ✅ Easy to maintain and test

---

## 🔗 Reference: Common Supabase REST API Patterns

### Query Parameters (if using REST API directly):

```
# Filter by column
GET /rest/v1/courses?teacher_id=eq.{uuid}

# Select specific columns
GET /rest/v1/courses?select=name,price

# Order by
GET /rest/v1/courses?order=created_at.desc

# Limit results
GET /rest/v1/courses?limit=10

# Combine filters
GET /rest/v1/courses?teacher_id=eq.{uuid}&status=eq.active&order=created_at.desc
```

**More examples:** https://postgrest.org/en/stable/api.html#query-parameters

---

## ✅ Summary

**For Courses & Payments:**

1. **Option 1 (Recommended):** Use JPA
   - No Supabase endpoints needed
   - Follow existing User pattern
   - JPA handles database operations

2. **Option 2:** Use Supabase REST API
   - Endpoint: `/rest/v1/{table_name}`
   - Pattern: `/rest/v1/courses`, `/rest/v1/payments`
   - More manual work

**My Recommendation:** Use JPA (Option 1) - it's simpler and consistent with your existing code!

