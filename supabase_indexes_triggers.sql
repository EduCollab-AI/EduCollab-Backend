-- Indexes and Triggers for EduCollab App
-- Run this AFTER creating tables and RLS policies

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
