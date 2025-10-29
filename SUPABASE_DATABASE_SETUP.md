# 🗄️ Supabase Database Setup Guide

Follow these steps to set up your Supabase database for the EduCollab backend.

## Step 1: Run Database Schema SQL

1. **Go to your Supabase Dashboard**
   - Navigate to: https://supabase.com/dashboard
   - Select your project

2. **Open SQL Editor**
   - Click on **SQL Editor** in the left sidebar
   - Click **New Query**

3. **Create Tables**
   - Copy the entire contents of `supabase_schema_simplified.sql`
   - Paste into the SQL Editor
   - Click **Run** (or press Cmd/Ctrl + Enter)
   - ✅ You should see: "Success. No rows returned"

4. **Enable Row Level Security**
   - Copy the entire contents of `supabase_rls_policies.sql`
   - Paste into the SQL Editor
   - Click **Run**
   - ✅ You should see: "Success. No rows returned"

5. **Create Indexes and Triggers**
   - Copy the entire contents of `supabase_indexes_triggers.sql`
   - Paste into the SQL Editor
   - Click **Run**
   - ✅ You should see: "Success. No rows returned"

6. **Create User Profile Trigger**
   - Copy the entire contents of `supabase_user_trigger.sql`
   - Paste into the SQL Editor
   - Click **Run**
   - ✅ You should see: "Success. No rows returned"

## Step 2: Get Your Supabase Credentials

1. **Go to Settings → API**
   - Click **Settings** (gear icon) in the left sidebar
   - Click **API**

2. **Copy these values:**
   - **Project URL**: `https://xxxxx.supabase.co`
   - **Service Role Key**: `eyJhbGc...` (secret key - keep this safe!)
   - **Anon Key**: `eyJhbGc...` (public key)

## Step 3: Get Your Database Connection String

1. **Go to Settings → Database**
   - Click **Settings** → **Database**

2. **Copy Connection String**
   - Scroll down to **Connection String**
   - Select **Connection pooling** tab
   - Copy the **URI** format: `postgresql://postgres:[YOUR-PASSWORD]@[HOST]:5432/postgres`
   - Replace `[YOUR-PASSWORD]` with your actual database password

## Step 4: Configure Railway Environment Variables

1. **Go to your Railway project**
   - Navigate to: https://railway.com/project/your-project-id

2. **Add Environment Variables**
   - Click on your service
   - Go to **Variables** tab
   - Add these variables:

   ```
   SUPABASE_URL=https://your-project.supabase.co
   SUPABASE_SERVICE_KEY=your-service-role-key-here
   SUPABASE_ANON_KEY=your-anon-key-here
   DATABASE_URL=postgresql://postgres:password@host:5432/postgres
   ```

   **Important Notes:**
   - Replace `your-project.supabase.co` with your actual Supabase URL
   - Replace `your-service-role-key-here` with your actual Service Role Key
   - Replace `your-anon-key-here` with your actual Anon Key
   - Replace the entire `DATABASE_URL` with your actual connection string from Step 3

3. **Save Changes**
   - Railway will automatically redeploy your service

## Step 5: Verify Setup

1. **Check Railway Logs**
   - Go to Railway dashboard → **Deployments**
   - Check that deployment succeeded
   - Look for any database connection errors

2. **Test Registration**
   - Go to your Flutter app
   - Try registering a new user
   - Check Supabase Dashboard → **Table Editor** → **users**
   - You should see a new user record!

3. **Verify Trigger**
   - After registration, check the `public.users` table
   - A user profile should be automatically created
   - If not, check the `auth.users` table - the user should exist there

## Troubleshooting

### ❌ "Failed to create user in Supabase Auth"
- Check that `SUPABASE_URL` and `SUPABASE_SERVICE_KEY` are correct
- Verify the Supabase project is active

### ❌ "Failed to connect to database"
- Check that `DATABASE_URL` is correct
- Verify the database password is correct
- Make sure the connection string uses the correct format

### ❌ "User profile not found"
- Check that the trigger was created: Run `SELECT * FROM pg_trigger WHERE tgname = 'on_auth_user_created';`
- If trigger doesn't exist, re-run `supabase_user_trigger.sql`
- Check that RLS policies allow inserts: Verify `supabase_rls_policies.sql` was run

### ❌ "Permission denied for table users"
- Make sure you ran `supabase_rls_policies.sql`
- Check that the Service Role Key is being used (not Anon Key)
- Verify RLS policies allow the operation

## Next Steps

Once the database is set up:
1. ✅ Test registration from Flutter app
2. ✅ Test login from Flutter app
3. ✅ Verify user data in Supabase Dashboard
4. ✅ Start building course and payment features!

## Need Help?

- Check Railway logs for detailed error messages
- Check Supabase Dashboard → **Logs** for API errors
- Verify all environment variables are set correctly
- Make sure all SQL scripts were run successfully

