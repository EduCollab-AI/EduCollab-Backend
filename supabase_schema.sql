-- Supabase Database Schema for EduCollab App
-- Run this in your Supabase SQL Editor

-- Note: auth.users table is managed by Supabase and already has RLS enabled

-- Create users table
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

-- Create children table
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

-- Create courses table
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

-- Create schedules table
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

-- Create payments table
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

-- Create course enrollments table
CREATE TABLE IF NOT EXISTS public.course_enrollments (
  id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
  course_id UUID REFERENCES public.courses(id) ON DELETE CASCADE,
  child_id UUID REFERENCES public.children(id) ON DELETE CASCADE,
  enrolled_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
  status TEXT DEFAULT 'active' CHECK (status IN ('active', 'completed', 'dropped', 'suspended')),
  UNIQUE(course_id, child_id)
);

-- Enable Row Level Security on all tables
ALTER TABLE public.users ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.children ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.courses ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.schedules ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.payments ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.course_enrollments ENABLE ROW LEVEL SECURITY;

-- Create RLS policies for users table
CREATE POLICY "Users can view own profile" ON public.users
  FOR SELECT USING (auth.uid() = id);

CREATE POLICY "Users can update own profile" ON public.users
  FOR UPDATE USING (auth.uid() = id);

-- Create RLS policies for children table
CREATE POLICY "Parents can view own children" ON public.children
  FOR SELECT USING (auth.uid() = parent_id);

CREATE POLICY "Parents can insert own children" ON public.children
  FOR INSERT WITH CHECK (auth.uid() = parent_id);

CREATE POLICY "Parents can update own children" ON public.children
  FOR UPDATE USING (auth.uid() = parent_id);

CREATE POLICY "Parents can delete own children" ON public.children
  FOR DELETE USING (auth.uid() = parent_id);

-- Create RLS policies for courses table
CREATE POLICY "Everyone can view courses" ON public.courses
  FOR SELECT USING (true);

CREATE POLICY "Teachers can insert courses" ON public.courses
  FOR INSERT WITH CHECK (
    EXISTS (
      SELECT 1 FROM public.users 
      WHERE id = auth.uid() AND role IN ('teacher', 'admin', 'school')
    )
  );

CREATE POLICY "Teachers can update own courses" ON public.courses
  FOR UPDATE USING (
    teacher_id = auth.uid() OR 
    EXISTS (
      SELECT 1 FROM public.users 
      WHERE id = auth.uid() AND role IN ('admin', 'school')
    )
  );

-- Create RLS policies for schedules table
CREATE POLICY "Everyone can view schedules" ON public.schedules
  FOR SELECT USING (true);

CREATE POLICY "Teachers can manage schedules" ON public.schedules
  FOR ALL USING (
    EXISTS (
      SELECT 1 FROM public.courses 
      WHERE id = course_id AND teacher_id = auth.uid()
    ) OR
    EXISTS (
      SELECT 1 FROM public.users 
      WHERE id = auth.uid() AND role IN ('admin', 'school')
    )
  );

-- Create RLS policies for payments table
CREATE POLICY "Parents can view own payments" ON public.payments
  FOR SELECT USING (auth.uid() = parent_id);

CREATE POLICY "Parents can insert own payments" ON public.payments
  FOR INSERT WITH CHECK (auth.uid() = parent_id);

CREATE POLICY "Parents can update own payments" ON public.payments
  FOR UPDATE USING (auth.uid() = parent_id);

-- Create RLS policies for course enrollments table
CREATE POLICY "Parents can view children enrollments" ON public.course_enrollments
  FOR SELECT USING (
    EXISTS (
      SELECT 1 FROM public.children 
      WHERE id = child_id AND parent_id = auth.uid()
    )
  );

CREATE POLICY "Parents can enroll children" ON public.course_enrollments
  FOR INSERT WITH CHECK (
    EXISTS (
      SELECT 1 FROM public.children 
      WHERE id = child_id AND parent_id = auth.uid()
    )
  );

-- Create indexes for better performance
CREATE INDEX IF NOT EXISTS idx_children_parent_id ON public.children(parent_id);
CREATE INDEX IF NOT EXISTS idx_courses_teacher_id ON public.courses(teacher_id);
CREATE INDEX IF NOT EXISTS idx_schedules_course_id ON public.schedules(course_id);
CREATE INDEX IF NOT EXISTS idx_payments_parent_id ON public.payments(parent_id);
CREATE INDEX IF NOT EXISTS idx_payments_child_id ON public.payments(child_id);
CREATE INDEX IF NOT EXISTS idx_course_enrollments_course_id ON public.course_enrollments(course_id);
CREATE INDEX IF NOT EXISTS idx_course_enrollments_child_id ON public.course_enrollments(child_id);

-- Create updated_at trigger function
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
  NEW.updated_at = NOW();
  RETURN NEW;
END;
$$ language 'plpgsql';

-- Create triggers for updated_at
CREATE TRIGGER update_users_updated_at BEFORE UPDATE ON public.users
  FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_children_updated_at BEFORE UPDATE ON public.children
  FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_courses_updated_at BEFORE UPDATE ON public.courses
  FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_schedules_updated_at BEFORE UPDATE ON public.schedules
  FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_payments_updated_at BEFORE UPDATE ON public.payments
  FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
