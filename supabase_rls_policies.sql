-- Row Level Security Policies for EduCollab App
-- Run this AFTER creating the tables

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
