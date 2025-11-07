-- Add deactivated_at column to course_enrollments for tracking inactive date
-- Run this in Supabase SQL editor before deploying the updated backend code

ALTER TABLE public.course_enrollments
ADD COLUMN IF NOT EXISTS deactivated_at TIMESTAMP WITH TIME ZONE;
