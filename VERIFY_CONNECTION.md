# ✅ Verify Railway → Supabase Connection

## Current Status

The backend is returning an error indicating it's still using placeholder values from the config file instead of your environment variables.

**Error:** `Failed to resolve 'your-project.supabase.co'`

This means Railway hasn't picked up your environment variables yet, or they weren't set correctly.

---

## 🔍 Step 1: Verify Environment Variables in Railway

### Check Railway Dashboard:

1. **Go to Railway Dashboard**
   - https://railway.com/project/your-project-id
   - Click on your service

2. **Check Variables Tab**
   - Click **Variables** tab
   - Verify these variables exist:

   ```
   SUPABASE_URL=https://xxxxx.supabase.co
   SUPABASE_SERVICE_KEY=eyJhbGc... (long token)
   DATABASE_URL=postgresql://postgres:password@host:5432/postgres
   ```

3. **Important Notes:**
   - `SUPABASE_URL` should NOT have quotes or spaces
   - `SUPABASE_SERVICE_KEY` should be the full token (starts with `eyJ`)
   - `DATABASE_URL` should be the full connection string

---

## 🔄 Step 2: Force Railway Redeployment

After setting/updating environment variables:

1. **Option 1: Trigger Redeploy**
   - Railway should auto-redeploy when you change variables
   - Go to **Deployments** tab
   - Wait for new deployment to complete (5-10 minutes)

2. **Option 2: Manual Redeploy**
   - Go to **Deployments** tab
   - Click **Deploy** or **Redeploy** button
   - Wait for deployment to complete

---

## 🧪 Step 3: Test Connection Again

### Test 1: Health Check
```bash
curl https://educollab-backend-production.up.railway.app/health
```
**Expected:** `OK`

### Test 2: Registration Test
```bash
curl -X POST https://educollab-backend-production.up.railway.app/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "Test123456!",
    "firstName": "Test",
    "lastName": "User",
    "role": "parent",
    "phone": "1234567890"
  }'
```

**Expected Success Response:**
```json
{
  "success": true,
  "message": "Registration successful",
  "data": {
    "user": {
      "id": "uuid-here",
      "email": "test@example.com",
      ...
    },
    "accessToken": "token-here",
    ...
  }
}
```

**If Still Failing:**
- Check Railway logs for detailed error messages
- Verify environment variable names are correct (case-sensitive!)
- Make sure there are no extra spaces or quotes

---

## 🔍 Step 4: Check Railway Logs

1. **Go to Railway Dashboard**
2. **Click on your service**
3. **Go to Deployments → Latest → Logs**
4. **Look for:**
   - Startup messages showing Supabase URL
   - Database connection attempts
   - Any error messages about Supabase or database

**What to Look For:**
- ✅ Success: "Application is ready and listening on port..."
- ❌ Error: "Failed to resolve 'your-project.supabase.co'"
- ❌ Error: "Connection refused" or "Failed to connect to database"

---

## ✅ Success Indicators

Connection is working if:

1. ✅ Health endpoint returns `OK`
2. ✅ Registration endpoint returns success response
3. ✅ No "Failed to resolve" errors
4. ✅ Railway logs show successful startup
5. ✅ User is created in Supabase `auth.users` table
6. ✅ User profile is created in Supabase `public.users` table

---

## 🛠️ Common Issues

### Issue 1: Variables Not Applied

**Solution:**
- Make sure variables are set in Railway dashboard
- Wait for redeployment to complete
- Check variable names are exact: `SUPABASE_URL`, `SUPABASE_SERVICE_KEY`, `DATABASE_URL`

### Issue 2: Wrong Supabase URL Format

**Wrong:**
```
SUPABASE_URL=https://your-project.supabase.co  # Placeholder
SUPABASE_URL="https://xxxxx.supabase.co"      # With quotes
```

**Correct:**
```
SUPABASE_URL=https://abcdefghijklmnop.supabase.co  # Your actual URL
```

### Issue 3: Wrong Service Key

**Wrong:**
```
SUPABASE_SERVICE_KEY=your-service-key-here  # Placeholder
```

**Correct:**
```
SUPABASE_SERVICE_KEY=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...  # Full token
```

### Issue 4: Wrong Database URL Format

**Wrong:**
```
DATABASE_URL=postgresql://localhost:5432/postgres  # Localhost
```

**Correct:**
```
DATABASE_URL=postgresql://postgres:[PASSWORD]@db.xxxxx.supabase.co:5432/postgres
```

---

## 📝 Quick Verification Checklist

- [ ] Environment variables set in Railway dashboard
- [ ] Variable names are correct (case-sensitive)
- [ ] No extra spaces or quotes in values
- [ ] Railway redeployed after setting variables
- [ ] Health endpoint returns `OK`
- [ ] Registration test returns success (not error)
- [ ] Railway logs show no connection errors

---

## 🎯 Next Steps

Once connection is verified:

1. ✅ Test registration from Flutter app
2. ✅ Verify user created in Supabase `auth.users`
3. ✅ Verify user profile in Supabase `public.users`
4. ✅ Test login functionality
5. ✅ Continue building other features

---

Let me know what you see in the Railway logs and test results!

