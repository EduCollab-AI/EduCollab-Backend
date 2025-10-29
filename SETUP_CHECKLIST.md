# ✅ Setup Checklist

## 🎯 Scenario 1: Railway → Supabase (Production)

### Supabase Setup
- [ ] Run `supabase_schema_simplified.sql` in Supabase SQL Editor
- [ ] Run `supabase_rls_policies.sql` in Supabase SQL Editor
- [ ] Run `supabase_indexes_triggers.sql` in Supabase SQL Editor
- [ ] Run `supabase_user_trigger.sql` in Supabase SQL Editor
- [ ] Get Supabase URL from Settings → API
- [ ] Get Service Role Key from Settings → API
- [ ] Get Database Connection String from Settings → Database

### Railway Setup
- [ ] Add `SUPABASE_URL` environment variable
- [ ] Add `SUPABASE_SERVICE_KEY` environment variable
- [ ] Add `DATABASE_URL` environment variable
- [ ] Wait for Railway deployment to complete (5-10 min)
- [ ] Verify deployment succeeded in Railway dashboard

### Flutter App Setup
- [ ] Set `environment = 'production'` in `lib/config/api_config.dart`
- [ ] Run `flutter run -d chrome --web-port=8080`
- [ ] Test registration flow
- [ ] Verify user appears in Supabase `users` table

---

## 🛠️ Scenario 2: Local → Supabase (Development)

### Supabase Setup
- [ ] Run `supabase_schema_simplified.sql` in Supabase SQL Editor
- [ ] Run `supabase_rls_policies.sql` in Supabase SQL Editor
- [ ] Run `supabase_indexes_triggers.sql` in Supabase SQL Editor
- [ ] Run `supabase_user_trigger.sql` in Supabase SQL Editor
- [ ] Get Supabase URL from Settings → API
- [ ] Get Service Role Key from Settings → API
- [ ] Get Database Connection String from Settings → Database

### Local Backend Setup
- [ ] Copy `backend/env.local.example` to `backend/.env`
- [ ] Fill in `SUPABASE_URL` in `.env`
- [ ] Fill in `SUPABASE_SERVICE_KEY` in `.env`
- [ ] Fill in `DATABASE_URL` in `.env`
- [ ] Run `cd backend && ./start.sh`
- [ ] Verify backend running: `curl http://localhost:8080/health`

### Flutter App Setup
- [ ] Set `environment = 'local'` in `lib/config/api_config.dart`
- [ ] Run `flutter run -d chrome --web-port=8080`
- [ ] Test registration flow
- [ ] Check backend logs for SQL queries
- [ ] Verify user appears in Supabase `users` table

---

## 🔄 Switching Between Environments

### To Switch to Local:
1. Set `environment = 'local'` in `lib/config/api_config.dart`
2. Make sure backend is running: `cd backend && ./start.sh`
3. Restart Flutter app

### To Switch to Production:
1. Set `environment = 'production'` in `lib/config/api_config.dart`
2. Make sure Railway service is deployed and running
3. Restart Flutter app

---

## 🧪 Testing Checklist

### After Setup, Test:
- [ ] Registration creates user in Supabase Auth
- [ ] User profile created in `public.users` table
- [ ] Login works with registered credentials
- [ ] Backend logs show successful database queries
- [ ] No errors in Flutter app console
- [ ] No errors in Railway/backend logs

### Quick Test Commands:

**Test Backend Health:**
```bash
# Local
curl http://localhost:8080/health

# Production
curl https://educollab-backend-production.up.railway.app/health
```

**Test Registration:**
```bash
# Local
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"test123","firstName":"Test","lastName":"User","role":"parent","phone":"1234567890"}'

# Production
curl -X POST https://educollab-backend-production.up.railway.app/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"test123","firstName":"Test","lastName":"User","role":"parent","phone":"1234567890"}'
```

---

## 📝 Notes

- **Local development** is faster for iteration (no deployment wait)
- **Production testing** validates deployment and environment
- **Always test locally first** before deploying to Railway
- **Both scenarios** connect to the same Supabase database
- **Environment variable** in Flutter app controls which backend to use

