# Deployment Guide

## 🚀 Railway Deployment (Recommended)

### Step 1: Prepare Repository
1. Push your code to GitHub
2. Make sure `backend/` folder is in the root of your repository

### Step 2: Deploy to Railway
1. Go to [railway.app](https://railway.app)
2. Sign up with GitHub
3. Click "New Project" → "Deploy from GitHub repo"
4. Select your repository
5. Choose "Deploy Now"

### Step 3: Configure Environment Variables
In Railway dashboard, go to your project → Variables tab and add:
```
SUPABASE_DB_PASSWORD=your-supabase-db-password
SUPABASE_SERVICE_ROLE_KEY=your-service-role-key
JWT_SECRET=your-jwt-secret-key-here-make-it-long-and-secure
```

### Step 4: Update Flutter App
Update `lib/config/api_config.dart`:
```dart
static const String baseUrl = 'https://your-app.railway.app/api/v1';
```

## 🌐 Other Hosting Options

### Heroku
1. Install Heroku CLI
2. Create `Procfile` in backend folder:
   ```
   web: mvn spring-boot:run
   ```
3. Deploy:
   ```bash
   heroku create your-app-name
   git push heroku main
   ```

### AWS EC2
1. Launch EC2 instance (Ubuntu)
2. Install Java 17 and Maven
3. Clone repository
4. Run: `mvn spring-boot:run`
5. Configure security groups for port 8080

### DigitalOcean Droplet
1. Create droplet (Ubuntu)
2. Install Java 17 and Maven
3. Clone repository
4. Run: `mvn spring-boot:run`
5. Configure firewall for port 8080

## 🔧 Production Considerations

### Environment Variables
- Never commit `.env` files
- Use platform-specific environment variable settings
- Rotate secrets regularly

### Database
- Use Supabase production database
- Set up proper RLS policies
- Monitor database performance

### Security
- Use HTTPS in production
- Set up proper CORS policies
- Implement rate limiting
- Use secure JWT secrets

### Monitoring
- Set up logging
- Monitor application health
- Set up alerts for failures

## 📱 Flutter App Updates

After deploying, update your Flutter app's API configuration:

```dart
// lib/config/api_config.dart
class ApiConfig {
  // Update this to your deployed URL
  static const String baseUrl = 'https://your-app.railway.app/api/v1';
  // ... rest of config
}
```

## 🧪 Testing Deployment

Test your deployed API:
```bash
# Health check
curl https://your-app.railway.app/api/v1/auth/health

# Register user
curl -X POST https://your-app.railway.app/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "password123",
    "firstName": "John",
    "lastName": "Doe",
    "role": "parent"
  }'
```
