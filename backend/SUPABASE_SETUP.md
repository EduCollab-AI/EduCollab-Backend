# Supabase Database Setup Guide

This guide will help you set up your Supabase database for the EduCollab backend.

## Step 1: Create Tables

1. Go to your Supabase project dashboard
2. Navigate to **SQL Editor**
3. Copy and paste the contents of `supabase_schema_simplified.sql`
4. Click **Run** to execute the SQL

This will create the following tables:
- `users` - User profiles
- `children` - Children information
- `courses` - Course information
- `schedules` - Course schedules
- `payments` - Payment records
- `course_enrollments` - Course enrollment records

## Step 2: Enable Row Level Security (RLS)

1. In the SQL Editor, copy and paste the contents of `supabase_rls_policies.sql`
2. Click **Run** to execute the SQL

This will:
- Enable RLS on all tables
- Create policies for data access control

## Step 3: Create Indexes and Triggers

1. In the SQL Editor, copy and paste the contents of `supabase_indexes_triggers.sql`
2. Click **Run** to execute the SQL

This will:
- Create indexes for better query performance
- Create triggers to automatically update `updated_at` timestamps

## Step 4: Create User Profile Trigger

1. In the SQL Editor, copy and paste the contents of `supabase_user_trigger.sql`
2. Click **Run** to execute the SQL

This will automatically create a user profile in `public.users` when a new user signs up via Supabase Auth.

## Step 5: Get Your Supabase Credentials

1. Go to **Settings** → **API** in your Supabase dashboard
2. Copy these values:
   - **Project URL** (e.g., `https://xxxxx.supabase.co`)
   - **Service Role Key** (secret key - use this in Railway)
   - **Anon Key** (public key - optional for Java backend)

## Step 6: Configure Railway Environment Variables

Add these environment variables in your Railway project:

```
SUPABASE_URL=https://your-project.supabase.co
SUPABASE_SERVICE_KEY=your-service-role-key
DATABASE_URL=postgresql://postgres:[PASSWORD]@[HOST]:5432/postgres
```

To get your `DATABASE_URL`:
1. Go to **Settings** → **Database** in Supabase
2. Copy the **Connection string** under "Connection pooling"
3. Replace `[YOUR-PASSWORD]` with your database password

## Step 7: Verify Setup

Test your database connection by:
1. Checking that tables exist in Supabase Dashboard → **Table Editor**
2. Testing registration from your Flutter app
3. Verifying that a user profile is created in `public.users` table

## Troubleshooting

### Error: "permission denied for table users"
- Make sure you ran the RLS policies SQL
- Check that the service role key is being used correctly

### Error: "relation does not exist"
- Make sure you ran the schema SQL files in order
- Check that tables exist in Supabase Dashboard → **Table Editor**

### Trigger not working
- Check that `supabase_user_trigger.sql` was executed
- Verify the trigger exists: `SELECT * FROM pg_trigger WHERE tgname = 'on_auth_user_created';`

