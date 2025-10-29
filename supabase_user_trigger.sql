-- Database trigger to automatically create user profile when auth.users is created
-- Run this in your Supabase SQL Editor

-- Create a function that will be called when a new user is created
CREATE OR REPLACE FUNCTION public.handle_new_user()
RETURNS TRIGGER AS $$
BEGIN
  INSERT INTO public.users (
    id,
    email,
    first_name,
    last_name,
    role,
    phone,
    avatar_url,
    created_at
  )
  VALUES (
    NEW.id,
    NEW.email,
    COALESCE(NEW.raw_user_meta_data->>'first_name', ''),
    COALESCE(NEW.raw_user_meta_data->>'last_name', ''),
    COALESCE(NEW.raw_user_meta_data->>'role', 'parent'),
    COALESCE(NEW.raw_user_meta_data->>'phone', ''),
    COALESCE(NEW.raw_user_meta_data->>'avatar', ''), -- Changed from 'avatar_url' to 'avatar'
    NOW()
  );
  RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Create the trigger
DROP TRIGGER IF EXISTS on_auth_user_created ON auth.users;
CREATE TRIGGER on_auth_user_created
  AFTER INSERT ON auth.users
  FOR EACH ROW EXECUTE FUNCTION public.handle_new_user();

-- Update the RLS policy to allow the trigger function to insert
CREATE POLICY "Allow trigger to insert users" ON public.users
  FOR INSERT WITH CHECK (true);
