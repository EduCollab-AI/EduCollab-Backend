-- Alternative approach: Manual user profile creation
-- This creates a simpler setup without triggers

-- First, make sure the users table exists and has the right structure
CREATE TABLE IF NOT EXISTS public.users (
  id UUID REFERENCES auth.users(id) PRIMARY KEY,
  email TEXT NOT NULL,
  first_name TEXT NOT NULL DEFAULT '',
  last_name TEXT NOT NULL DEFAULT '',
  role TEXT NOT NULL DEFAULT 'parent',
  phone TEXT DEFAULT '',
  avatar_url TEXT DEFAULT '',
  is_email_verified BOOLEAN DEFAULT FALSE,
  is_phone_verified BOOLEAN DEFAULT FALSE,
  created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
  updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Enable RLS
ALTER TABLE public.users ENABLE ROW LEVEL SECURITY;

-- Create policies for users table
CREATE POLICY "Users can view own profile" ON public.users
  FOR SELECT USING (auth.uid() = id);

CREATE POLICY "Users can update own profile" ON public.users
  FOR UPDATE USING (auth.uid() = id);

CREATE POLICY "Allow service role to insert users" ON public.users
  FOR INSERT WITH CHECK (true);

-- Create a function to manually create user profile
CREATE OR REPLACE FUNCTION public.create_user_profile(
  user_id UUID,
  user_email TEXT,
  user_first_name TEXT,
  user_last_name TEXT,
  user_role TEXT DEFAULT 'parent',
  user_phone TEXT DEFAULT '',
  user_avatar TEXT DEFAULT ''
)
RETURNS VOID AS $$
BEGIN
  INSERT INTO public.users (
    id, email, first_name, last_name, role, phone, avatar_url, created_at
  ) VALUES (
    user_id, user_email, user_first_name, user_last_name, 
    user_role, user_phone, user_avatar, NOW()
  );
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;
