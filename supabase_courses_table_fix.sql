-- Fix courses table to match the Course entity
-- Run this in your Supabase SQL Editor

-- Add missing columns to courses table
ALTER TABLE public.courses 
ADD COLUMN IF NOT EXISTS teacher_name TEXT,
ADD COLUMN IF NOT EXISTS total_sessions INTEGER,
ADD COLUMN IF NOT EXISTS location TEXT;

-- Make teacher_name required (only if no existing NULL values)
DO $$ 
BEGIN
    IF NOT EXISTS (SELECT 1 FROM public.courses WHERE teacher_name IS NULL) THEN
        ALTER TABLE public.courses ALTER COLUMN teacher_name SET NOT NULL;
    END IF;
END $$;

-- Make total_sessions required (only if no existing NULL values)
DO $$ 
BEGIN
    IF NOT EXISTS (SELECT 1 FROM public.courses WHERE total_sessions IS NULL) THEN
        ALTER TABLE public.courses ALTER COLUMN total_sessions SET NOT NULL;
    END IF;
END $$;

-- Drop old teacher_id column if it exists
ALTER TABLE public.courses 
DROP COLUMN IF EXISTS teacher_id;

-- Drop other unused columns if they exist
ALTER TABLE public.courses 
DROP COLUMN IF EXISTS academic_year,
DROP COLUMN IF EXISTS semester,
DROP COLUMN IF EXISTS credits,
DROP COLUMN IF EXISTS price,
DROP COLUMN IF EXISTS duration_weeks,
DROP COLUMN IF EXISTS prerequisites,
DROP COLUMN IF EXISTS materials_needed,
DROP COLUMN IF EXISTS difficulty_level,
DROP COLUMN IF EXISTS category,
DROP COLUMN IF EXISTS learning_objectives;

-- Fix schedules table
ALTER TABLE public.schedules 
ADD COLUMN IF NOT EXISTS id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
ADD COLUMN IF NOT EXISTS day_of_week TEXT,
ADD COLUMN IF NOT EXISTS start_date DATE,
ADD COLUMN IF NOT EXISTS duration_minutes BIGINT,
ADD COLUMN IF NOT EXISTS recurrence_rule TEXT;

-- Drop old end_time and occurrence columns if they exist
ALTER TABLE public.schedules 
DROP COLUMN IF EXISTS end_time,
DROP COLUMN IF EXISTS notes;

-- Add primary key constraint if not exists
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint 
        WHERE conname = 'schedules_pkey'
    ) THEN
        ALTER TABLE public.schedules 
        ADD PRIMARY KEY (id);
    END IF;
END $$;

-- Verify the table structures
SELECT 'courses' as table_name, column_name, data_type, is_nullable 
FROM information_schema.columns 
WHERE table_schema = 'public' 
AND table_name = 'courses'
ORDER BY ordinal_position;

SELECT 'schedules' as table_name, column_name, data_type, is_nullable 
FROM information_schema.columns 
WHERE table_schema = 'public' 
AND table_name = 'schedules'
ORDER BY ordinal_position;

