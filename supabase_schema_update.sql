-- 1. USERS Table (Core identity and authentication)
-- user_id is the primary key and links to Firebase Auth UID
CREATE TABLE IF NOT EXISTS users (
    id UUID REFERENCES auth.users(id) PRIMARY KEY,
    role VARCHAR(15) NOT NULL CHECK (role IN ('parent', 'institution')),
    account_name VARCHAR(100) NOT NULL,
    avatar_url VARCHAR(255),
    email VARCHAR(100) UNIQUE,
    name VARCHAR(100) NOT NULL,
    phone_number VARCHAR(20) NOT NULL UNIQUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- 2. INSTITUTIONS Table (Organization profile)
CREATE TABLE IF NOT EXISTS institutions (
    institution_id SERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    avatar_url VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- 3. STUDENTS Table (Child/Student records and Parent linkage)
-- is_associated flag is critical for de-duplication/claim logic
CREATE TABLE IF NOT EXISTS students (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    institution_id INT REFERENCES institutions(institution_id), -- FK: Nullable if parent-added
    parent_email VARCHAR(100), -- Institution-provided data for matching
    parent_phone VARCHAR(20),   -- Institution-provided data for matching
    birth_date DATE,
    associated_parent_id VARCHAR(50) REFERENCES users(user_id), -- FK: Links to the parent's user_id
    is_associated BOOLEAN NOT NULL, -- FALSE=Pending Claim, TRUE=Linked/Claimed
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- 4. COURSES Table (Reusable class templates)
-- invitation_code enforces class-level access control
CREATE TABLE IF NOT EXISTS courses (
    course_id SERIAL PRIMARY KEY,
    institution_id INT REFERENCES institutions(institution_id), -- FK: Nullable if self-managed
    invitation_code VARCHAR(10) UNIQUE, -- Used for parent association
    is_self_managed BOOLEAN NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- 5. ENROLLMENTS Table (Student-Course relationship, holds personalized pricing)
CREATE TABLE IF NOT EXISTS enrollments (
    enrollment_id SERIAL PRIMARY KEY,
    student_id INT NOT NULL REFERENCES students(student_id),
    course_id INT NOT NULL REFERENCES courses(course_id),
    institution_id INT REFERENCES institutions(institution_id), -- FK: Redundant, but useful for quick querying
    price_per_hour DECIMAL(10, 2) NOT NULL, -- Personalized pricing (Edge Case #1)
    total_hours_purchased INT NOT NULL,
    remaining_hours INT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- 6. PAYMENT_EVENTS Table (Invoice/Bill records with full scheduling status)
-- Tracks the full financial lifecycle (invoiced, due, paid)
CREATE TABLE IF NOT EXISTS payment_events (
    event_id SERIAL PRIMARY KEY,
    student_id INT NOT NULL REFERENCES students(student_id),
    enrollment_id INT REFERENCES enrollments(enrollment_id),
    institution_id INT REFERENCES institutions(institution_id), -- Nullable if self-managed
    amount_due DECIMAL(10, 2) NOT NULL,
    due_date DATE NOT NULL, -- The scheduled deadline
    invoiced_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(), -- When the bill was generated
    paid_at TIMESTAMP WITH TIME ZONE, -- When the payment was received
    status VARCHAR(20) NOT NULL CHECK (status IN ('pending', 'paid', 'overdue', 'cancelled')),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- 7. SCHEDULES Table (Class time and recurrence rules for calendar population)
CREATE TABLE IF NOT EXISTS schedules (
    schedule_id SERIAL PRIMARY KEY,
    course_id INT NOT NULL REFERENCES courses(course_id),
    start_time TIME NOT NULL,
    duration_minutes INT NOT NULL,
    recurrence_rule VARCHAR(50) NOT NULL, -- e.g., 'weekly', 'once'
    created_at TIMESTAMP WITH