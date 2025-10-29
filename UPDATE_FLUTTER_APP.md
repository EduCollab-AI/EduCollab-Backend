# Update Flutter App for Railway Deployment

## After Railway deployment, update this file:

### File: `lib/config/api_config.dart`

Replace the baseUrl with your Railway app URL:

```dart
class ApiConfig {
  // Update this with your Railway app URL
  static const String baseUrl = 'https://your-app-name.railway.app/api/v1';
  
  // ... rest of config stays the same
}
```

### Example:
If your Railway app URL is `https://school-app-backend-production.up.railway.app`, then:

```dart
static const String baseUrl = 'https://school-app-backend-production.up.railway.app/api/v1';
```

## Test the Connection

After updating, test your app:

1. **Run the Flutter app**
2. **Try to register a new user**
3. **Check Railway logs** to see if requests are coming through
4. **Check Supabase** to see if user was created

## Troubleshooting

If you get connection errors:

1. **Check Railway URL** - make sure it's correct
2. **Check environment variables** in Railway dashboard
3. **Check Railway logs** for errors
4. **Verify Supabase setup** - run the SQL commands
5. **Test API directly** with curl:

```bash
curl https://your-app-name.railway.app/api/v1/auth/health
```
