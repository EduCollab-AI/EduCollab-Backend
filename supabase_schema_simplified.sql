-- Supabase Database Schema for EduCollab App - Simplified Version
-- Run this in your Supabase SQL Editor step by step

-- Step 1: Create users table
CREATE TABLE IF NOT EXISTS public.users (
  id UUID REFERENCES auth.users(id) PRIMARY KEY,
  email TEXT UNIQUE NOT NULL,
  first_name TEXT NOT NULL,
  last_name TEXT NOT NULL,
  role TEXT NOT NULL CHECK (role IN ('parent', 'teacher', 'admin', 'school')),
  phone TEXT,
  avatar_url TEXT,
  created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
  updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Step 2: Create children table
CREATE TABLE IF NOT EXISTS public.children (
  id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
  parent_id UUID REFERENCES public.users(id) ON DELETE CASCADE,
  name TEXT NOT NULL,
  age INTEGER,
  grade TEXT,
  school TEXT,
  created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
  updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Step 3: Create courses table
CREATE TABLE IF NOT EXISTS public.courses (
  id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
  name TEXT NOT NULL,
  code TEXT UNIQUE,
  description TEXT,
  teacher_id UUID REFERENCES public.users(id),
  academic_year TEXT,
  semester TEXT,
  credits INTEGER,
  max_students INTEGER,
  price DECIMAL(10,2),
  duration_weeks INTEGER,
  prerequisites TEXT,
  materials_needed TEXT,
  difficulty_level TEXT CHECK (difficulty_level IN ('beginner', 'intermediate', 'advanced')),
  category TEXT,
  learning_objectives TEXT,
  created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
  updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Step 4: Create schedules table
CREATE TABLE IF NOT EXISTS public.schedules (
  id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
  course_id UUID REFERENCES public.courses(id) ON DELETE CASCADE,
  start_time TIME NOT NULL,
  end_time TIME NOT NULL,
  day_of_week TEXT NOT NULL CHECK (day_of_week IN ('monday', 'tuesday', 'wednesday', 'thursday', 'friday', 'saturday', 'sunday')),
  occurrence TEXT DEFAULT 'weekly' CHECK (occurrence IN ('weekly', 'daily', 'monthly')),
  start_date DATE NOT NULL,
  end_date DATE NOT NULL,
  location TEXT,
  notes TEXT,
  created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
  updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Step 5: Create payments table
CREATE TABLE IF NOT EXISTS public.payments (
  id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
  parent_id UUID REFERENCES public.users(id),
  child_id UUID REFERENCES public.children(id),
  course_id UUID REFERENCES public.courses(id),
  item TEXT NOT NULL,
  amount DECIMAL(10,2) NOT NULL,
  status TEXT DEFAULT 'pending' CHECK (status IN ('pending', 'paid', 'overdue', 'cancelled')),
  due_date DATE,
  paid_date DATE,
  payment_method TEXT,
  notes TEXT,
  created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
  updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Step 6: Create course enrollments table
CREATE TABLE IF NOT EXISTS public.course_enrollments (
  id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
  course_id UUID REFERENCES public.courses(id) ON DELETE CASCADE,
  child_id UUID REFERENCES public.children(id) ON DELETE CASCADE,
  enrolled_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
  status TEXT DEFAULT 'active' CHECK (status IN ('active', 'completed', 'dropped', 'suspended')),
  UNIQUE(course_id, child_id)
);
