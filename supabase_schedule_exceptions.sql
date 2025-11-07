-- Create schedule_exceptions table to track per-occurrence overrides
-- Run in Supabase SQL editor before deploying backend changes

CREATE TABLE IF NOT EXISTS public.schedule_exceptions (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    schedule_id UUID NOT NULL REFERENCES public.schedules(id) ON DELETE CASCADE,
    original_date DATE NOT NULL,
    original_start_time TIME NOT NULL,
    is_cancelled BOOLEAN DEFAULT FALSE,
    new_date DATE,
    new_start_time TIME,
    new_duration_minutes BIGINT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_schedule_exceptions_schedule_id
    ON public.schedule_exceptions(schedule_id);

CREATE UNIQUE INDEX IF NOT EXISTS idx_schedule_exceptions_unique_occurrence
    ON public.schedule_exceptions(schedule_id, original_date, original_start_time);
