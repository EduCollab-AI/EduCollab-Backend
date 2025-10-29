# 🚀 Quick Start Guide

## Scenario 1: Test with Railway → Supabase (Production)

### Step 1: Set Up Supabase Database

1. **Go to Supabase Dashboard**
   - https://supabase.com/dashboard
   - Select your project

2. **Run SQL Scripts** (in order):
   - Open **SQL Editor** → **New Query**
   - Copy/paste `supabase_schema_simplified.sql` → **Run**
   - Copy/paste `supabase_rls_policies.sql` → **Run**
   - Copy/paste `supabase_indexes_triggers.sql` → **Run**
   - Copy/paste `supabase_user_trigger.sql` → **Run**

3. **Get Supabase Credentials**
   - Go to **Settings** → **API**
   - Copy: **Project URL**, **Service Role Key**, **Anon Key**
   - Go to **Settings** → **Database**
   - Copy: **Connection String** (Connection pooling tab)

### Step 2: Configure Railway Environment Variables

1. **Go to Railway Dashboard**
   - https://railway.com/project/your-project-id
   - Click on your service

2. **Add Environment Variables** (Variables tab):
   ```
   SUPABASE_URL=https://your-project.supabase.co
   SUPABASE_SERVICE_KEY=your-service-role-key
   DATABASE_URL=postgresql://postgres:password@host:5432/postgres
   ```

3. **Wait for Deployment** (5-10 minutes)
   - Railway auto-deploys when you push to GitHub
   - Check **Deployments** tab for status

### Step 3: Update Flutter App

1. **Set Environment to Production**:
   ```dart
   // lib/config/api_config.dart
   static const String environment = 'production';
   ```

2. **Run Flutter App**:
   ```bash
   flutter run -d chrome --web-port=8080
   ```

3. **Test Registration**:
   - Try registering a new user
   - Check Supabase Dashboard → **Table Editor** → **users** table
   - Should see new user record!

---

## Scenario 2: Test Locally → Supabase (Development)

### Step 1: Set Up Supabase Database (Same as Scenario 1)

1. **Run SQL Scripts** in Supabase Dashboard (same as above)

2. **Get Supabase Credentials** (same as above)

### Step 2: Configure Local Environment

1. **Create `.env` file**:
   ```bash
   cd backend
   cp env.local.example .env
   ```

2. **Edit `.env` file** with your credentials:
   ```bash
   SUPABASE_URL=https://your-project.supabase.co
   SUPABASE_SERVICE_KEY=your-service-role-key
   SUPABASE_ANON_KEY=your-anon-key
   DATABASE_URL=postgresql://postgres:password@host:5432/postgres
   DATABASE_USERNAME=postgres
   DATABASE_PASSWORD=your-database-password
   ```

### Step 3: Start Backend Locally

1. **Run startup script**:
   ```bash
   cd backend
   ./start.sh
   ```

2. **Verify backend is running**:
   - Should see: "Starting application on localhost:8080"
   - Test: `curl http://localhost:8080/health`
   - Should return: `OK`

### Step 4: Update Flutter App

1. **Set Environment to Local**:
   ```dart
   // lib/config/api_config.dart
   static const String environment = 'local';
   ```

2. **Run Flutter App**:
   ```bash
   flutter run -d chrome --web-port=8080
   ```

3. **Test Registration**:
   - Try registering a new user
   - Check backend logs for SQL queries
   - Check Supabase Dashboard → **Table Editor** → **users** table

---

## Quick Reference

### Switch Between Local and Production

**For Local Development:**
```dart
// lib/config/api_config.dart
static const String environment = 'local';
```

**For Production Testing:**
```dart
// lib/config/api_config.dart
static const String environment = 'production';
```

### Common Commands

**Start Backend Locally:**
```bash
cd backend
./start.sh
```

**Test Backend Health:**
```bash
curl http://localhost:8080/health
```

**Test Registration Locally:**
```bash
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"test123","firstName":"Test","lastName":"User","role":"parent","phone":"1234567890"}'
```

**Check Railway Logs:**
- Go to Railway Dashboard → **Deployments** → Click latest deployment → **Logs**

**Check Supabase Database:**
- Go to Supabase Dashboard → **Table Editor** → Select table (e.g., `users`)

---

## Troubleshooting

### Railway Deployment Issues
- Check **Deployments** tab for build errors
- Verify environment variables are set correctly
- Check logs for database connection errors

### Local Backend Issues
- Verify `.env` file exists and has correct values
- Check that port 8080 is not in use: `lsof -i :8080`
- Verify Java and Maven are installed: `java -version` and `mvn -version`

### Database Connection Issues
- Verify `DATABASE_URL` is correct format
- Check Supabase database is accessible
- Test connection: Try connecting with a PostgreSQL client

### Flutter App Can't Connect
- **Local**: Make sure backend is running on `localhost:8080`
- **Production**: Check Railway service is deployed and running
- Check browser console for CORS errors
- Verify `environment` variable is set correctly

---

## Workflow Recommendations

### Recommended Development Flow:

1. **Start with Local Development**
   - Set `environment = 'local'`
   - Make changes and test locally
   - Faster iteration, better debugging

2. **When Ready to Deploy**
   - Commit changes: `git add . && git commit -m "description"`
   - Push to GitHub: `git push origin main`
   - Railway auto-deploys (5-10 minutes)

3. **Test in Production**
   - Set `environment = 'production'`
   - Test with Railway backend
   - Verify everything works

4. **Repeat!**
   - Switch back to local for next feature
   - Develop → Test Locally → Deploy → Test Production

This workflow saves time and Railway deployment cycles! 🚀

