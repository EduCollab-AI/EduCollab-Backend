# 🧪 Test Registration Flow: Flutter → Railway → Supabase

## Prerequisites Checklist

Before testing, make sure:

### ✅ 1. Supabase Database Setup
- [ ] Run `supabase_schema_simplified.sql` in Supabase SQL Editor
- [ ] Run `supabase_rls_policies.sql` in Supabase SQL Editor
- [ ] Run `supabase_indexes_triggers.sql` in Supabase SQL Editor
- [ ] Run `supabase_user_trigger.sql` in Supabase SQL Editor

### ✅ 2. Railway Environment Variables
Go to Railway Dashboard → Your Service → Variables tab

- [ ] `SUPABASE_URL` is set (e.g., `https://xxxxx.supabase.co`)
- [ ] `SUPABASE_SERVICE_KEY` is set (Service Role Key)
- [ ] `DATABASE_URL` is set (PostgreSQL connection string)

### ✅ 3. Railway Deployment Status
- [ ] Check Railway Dashboard → Deployments
- [ ] Latest deployment is successful (green status)
- [ ] Service is running

### ✅ 4. Flutter App Configuration
- [ ] `lib/config/api_config.dart` has `environment = 'production'`
- [ ] Flutter app is ready to run

---

## 🧪 Testing Steps

### Step 1: Verify Railway Backend is Running

**Test Health Endpoint:**
```bash
curl https://educollab-backend-production.up.railway.app/health
```

**Expected Response:**
```
OK
```

**If it fails:**
- Check Railway deployment status
- Check Railway logs for errors
- Verify environment variables are set

---

### Step 2: Test Registration Endpoint Directly

**Test Registration API:**
```bash
curl -X POST https://educollab-backend-production.up.railway.app/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "testpassword123",
    "firstName": "Test",
    "lastName": "User",
    "role": "parent",
    "phone": "1234567890"
  }'
```

**Expected Response:**
```json
{
  "success": true,
  "message": "Registration successful",
  "data": {
    "user": {
      "id": "uuid-here",
      "email": "test@example.com",
      "firstName": "Test",
      "lastName": "User",
      "role": "parent"
    },
    "accessToken": "jwt-token-here",
    "refreshToken": "refresh-token-here",
    "expiresIn": 3600
  }
}
```

**If you get errors:**
- Check Railway logs for detailed error messages
- Verify Supabase credentials are correct
- Check if database connection is working

---

### Step 3: Run Flutter App and Test Registration

1. **Start Flutter App:**
   ```bash
   flutter run -d chrome --web-port=8080
   ```

2. **Navigate to Registration Screen:**
   - Click "Register" or "Sign Up"
   - Fill in registration form

3. **Submit Registration:**
   - Enter test email (use unique email for testing)
   - Enter password
   - Fill in other required fields
   - Click "Register" or "Submit"

4. **Observe Results:**
   - Check Flutter console for logs
   - Check if registration succeeds or fails
   - Note any error messages

---

### Step 4: Verify Data in Supabase

**Check Supabase Dashboard:**

1. **Go to Supabase Dashboard:**
   - https://supabase.com/dashboard
   - Select your project

2. **Check `auth.users` table:**
   - Go to **Table Editor** → **auth** schema → **users**
   - Should see new user record with email you registered

3. **Check `public.users` table:**
   - Go to **Table Editor** → **public** schema → **users**
   - Should see user profile with `id`, `email`, `first_name`, `last_name`, `role`, etc.

**Expected Results:**
- ✅ User exists in `auth.users` (created by Supabase Auth API)
- ✅ User profile exists in `public.users` (created by JPA or trigger)
- ✅ User data matches what you entered in Flutter app

---

## 🔍 Troubleshooting

### Issue 1: Registration Fails in Flutter App

**Symptoms:**
- Error message: "No internet connection" or "Registration failed"
- No response from backend

**Solutions:**
1. Check Railway service is running:
   ```bash
   curl https://educollab-backend-production.up.railway.app/health
   ```

2. Check CORS is enabled (should be already set up)

3. Check Flutter app console for detailed error messages

4. Verify `environment = 'production'` in `api_config.dart`

---

### Issue 2: Backend Returns Error

**Check Railway Logs:**
1. Go to Railway Dashboard
2. Click on your service
3. Go to **Deployments** → Latest deployment → **Logs**
4. Look for error messages

**Common Errors:**

**"Failed to create user in Supabase Auth"**
- Check `SUPABASE_URL` is correct
- Check `SUPABASE_SERVICE_KEY` is correct (Service Role Key, not Anon Key)
- Verify Supabase project is active

**"Failed to connect to database"**
- Check `DATABASE_URL` is correct format
- Verify database password is correct
- Check Supabase database is accessible

**"User profile not found"**
- Check if `supabase_user_trigger.sql` was run
- Check if RLS policies allow inserts
- Verify `public.users` table exists

---

### Issue 3: User Created but No Profile in `public.users`

**Symptoms:**
- User exists in `auth.users`
- No user in `public.users`

**Solutions:**
1. **Check if trigger exists:**
   ```sql
   SELECT * FROM pg_trigger WHERE tgname = 'on_auth_user_created';
   ```

2. **Check Railway logs:**
   - Look for errors when creating user profile
   - Check if JPA fallback is working

3. **Manual fallback should work:**
   - `AuthService.java` has code to create profile if trigger fails
   - Check Railway logs to see if it's working

---

### Issue 4: Can't See Data in Supabase

**Solutions:**
1. **Refresh Supabase Dashboard:**
   - Click refresh button in Table Editor

2. **Check correct table:**
   - `auth.users` - managed by Supabase Auth
   - `public.users` - your custom table

3. **Check RLS policies:**
   - Make sure RLS allows Service Role Key to read/write
   - Check if policies were applied correctly

---

## ✅ Success Indicators

**Registration is successful if:**

1. ✅ Flutter app shows success message
2. ✅ User receives authentication tokens
3. ✅ User exists in `auth.users` table
4. ✅ User profile exists in `public.users` table
5. ✅ User data matches what was entered
6. ✅ No errors in Railway logs
7. ✅ No errors in Flutter console

---

## 📊 Expected Flow Visualization

```
┌─────────────────┐
│  Flutter App   │
│   (Register)   │
└────────┬────────┘
         │ POST /api/v1/auth/register
         ▼
┌─────────────────────────────────────┐
│      Railway Backend                │
│  - Receives request                 │
│  - Calls Supabase Auth API          │
│  - Saves to database via JPA        │
└────────┬────────────────────────────┘
         │
         ├──→ POST /auth/v1/signup
         │    Creates: auth.users ✅
         │
         └──→ JPA save()
              Creates: public.users ✅
         ▼
┌─────────────────────────────────────┐
│      Supabase Database              │
│  ✅ auth.users (Auth API)           │
│  ✅ public.users (JPA)               │
└─────────────────────────────────────┘
```

---

## 📝 Test Data Examples

**Test User 1:**
```json
{
  "email": "parent1@test.com",
  "password": "Test123456!",
  "firstName": "John",
  "lastName": "Doe",
  "role": "parent",
  "phone": "6171234567"
}
```

**Test User 2:**
```json
{
  "email": "parent2@test.com",
  "password": "Test123456!",
  "firstName": "Jane",
  "lastName": "Smith",
  "role": "parent",
  "phone": "6177654321"
}
```

---

## 🎯 Next Steps After Successful Test

Once registration works:

1. ✅ Test login functionality
2. ✅ Test user profile retrieval
3. ✅ Add more features (courses, payments, etc.)
4. ✅ Test error handling
5. ✅ Test edge cases

---

Good luck with testing! 🚀

If you encounter any issues, check:
1. Railway logs for backend errors
2. Flutter console for client errors
3. Supabase Dashboard for database state
4. Environment variables are set correctly

