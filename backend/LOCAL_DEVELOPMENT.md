# 🛠️ Local Development Guide

This guide will help you run the backend locally and connect to Supabase for faster development.

## Prerequisites

- Java 17 or higher
- Maven 3.6 or higher
- Supabase account and project

## Step 1: Get Your Supabase Credentials

1. **Go to Supabase Dashboard**
   - Navigate to: https://supabase.com/dashboard
   - Select your project

2. **Get API Credentials**
   - Go to **Settings** → **API**
   - Copy:
     - **Project URL**: `https://xxxxx.supabase.co`
     - **Service Role Key**: `eyJhbGc...` (secret key)
     - **Anon Key**: `eyJhbGc...` (public key)

3. **Get Database Connection String**
   - Go to **Settings** → **Database**
   - Copy the **Connection String** under "Connection pooling"
   - Format: `postgresql://postgres:[PASSWORD]@[HOST]:5432/postgres`

## Step 2: Create Environment File

1. **Copy the example file**
   ```bash
   cd backend
   cp env.local.example .env
   ```

2. **Edit `.env` file** with your actual credentials:
   ```bash
   # Supabase Configuration
   SUPABASE_URL=https://your-project.supabase.co
   SUPABASE_SERVICE_KEY=your-service-role-key-here
   SUPABASE_ANON_KEY=your-anon-key-here

   # Database Configuration
   DATABASE_URL=postgresql://postgres:your-password@db.your-project.supabase.co:5432/postgres
   DATABASE_USERNAME=postgres
   DATABASE_PASSWORD=your-database-password
   ```

## Step 3: Run the Backend Locally

### Option 1: Using the Startup Script (Recommended)
```bash
cd backend
chmod +x start.sh
./start.sh
```

### Option 2: Using Maven Directly
```bash
cd backend
export $(cat .env | xargs)
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

The backend will start on **http://localhost:8080**

## Step 4: Update Flutter App to Use Local Backend

1. **Update API Config** (temporarily for local testing):
   ```dart
   // lib/config/api_config.dart
   static const String baseUrl = 'http://localhost:8080/api/v1';
   ```

2. **Run Flutter App**
   ```bash
   flutter run -d chrome --web-port=8080
   ```

## Step 5: Test Your API

### Test Health Endpoint
```bash
curl http://localhost:8080/health
```

### Test Registration
```bash
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "password123",
    "firstName": "Test",
    "lastName": "User",
    "role": "parent",
    "phone": "1234567890"
  }'
```

## Development Workflow

1. **Make Changes**: Edit Java files in `backend/src/main/java/`
2. **Test Locally**: Run `./start.sh` and test with Flutter app
3. **Commit Changes**: `git add . && git commit -m "your message"`
4. **Push to GitHub**: `git push origin main`
5. **Railway Auto-Deploys**: Railway will automatically deploy from GitHub

## Benefits of Local Development

✅ **Faster Iteration**: No need to wait for Railway deployment (5-10 minutes)  
✅ **Better Debugging**: See detailed logs and stack traces immediately  
✅ **SQL Logging**: View actual SQL queries in console  
✅ **Cost Savings**: No unnecessary Railway deployments  
✅ **Offline Development**: Can develop without internet (once dependencies are cached)

## Troubleshooting

### Port Already in Use
```bash
# Find process using port 8080
lsof -i :8080

# Kill the process
kill -9 <PID>
```

### Database Connection Failed
- Check that `.env` file has correct `DATABASE_URL`
- Verify Supabase database is accessible
- Check firewall/network settings

### Build Errors
```bash
# Clean and rebuild
mvn clean install

# Or force update dependencies
mvn clean install -U
```

### Environment Variables Not Loading
- Make sure `.env` file is in `backend/` directory
- Check that `.env` file has correct format (no spaces around `=`)
- Try running with explicit variables:
  ```bash
  export SUPABASE_URL=your-url
  export SUPABASE_SERVICE_KEY=your-key
  mvn spring-boot:run -Dspring-boot.run.profiles=local
  ```

## Hot Reload (For Faster Development)

Unfortunately, Java doesn't support hot reload like Flutter. But you can:
- Use **Spring Boot DevTools** (add to `pom.xml`):
  ```xml
  <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-devtools</artifactId>
      <optional>true</optional>
  </dependency>
  ```
- Restart quickly: Just press `Ctrl+C` and run `./start.sh` again

## Switching Between Local and Railway

### For Local Development:
```dart
// lib/config/api_config.dart
static const String baseUrl = 'http://localhost:8080/api/v1';
```

### For Production Testing:
```dart
// lib/config/api_config.dart
static const String baseUrl = 'https://educollab-backend-production.up.railway.app/api/v1';
```

## Next Steps

Once you've tested locally and everything works:
1. Commit your changes: `git add . && git commit -m "your changes"`
2. Push to GitHub: `git push origin main`
3. Railway will auto-deploy (5-10 minutes)
4. Update Flutter app to use Railway URL for production testing

Happy coding! 🚀

