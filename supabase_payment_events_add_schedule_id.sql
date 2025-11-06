-- Add payment_schedule_id column to payment_events table
-- This creates a foreign key relationship to payment_schedules table
-- Run this in your Supabase SQL Editor

-- Add the column (nullable initially to handle existing data)
ALTER TABLE public.payment_events 
ADD COLUMN IF NOT EXISTS payment_schedule_id UUID REFERENCES public.payment_schedules(id) ON DELETE SET NULL;

-- Create index for better query performance
CREATE INDEX IF NOT EXISTS idx_payment_events_payment_schedule_id 
ON public.payment_events(payment_schedule_id);

-- Create index for duplicate checking (student_id + payment_schedule_id + due_date)
CREATE INDEX IF NOT EXISTS idx_payment_events_schedule_duplicate_check 
ON public.payment_events(student_id, payment_schedule_id, due_date);

-- Optional: Add comment to document the column
COMMENT ON COLUMN public.payment_events.payment_schedule_id IS 'References the payment_schedules table to track which schedule generated this payment event';

