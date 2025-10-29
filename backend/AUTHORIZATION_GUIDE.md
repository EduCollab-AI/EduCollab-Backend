# Authorization Setup: Railway + Supabase

## 🔐 **How Authorization Works**

```
Flutter App → Railway (Java Backend) → Supabase (Database)
     ↓              ↓                      ↓
  User Login → Validate Token → Access Database
```

## 🛠️ **Configuration Steps**

### **1. Railway Environment Variables**

Set these in your Railway dashboard:

```bash
# Supabase Database
SUPABASE_DB_PASSWORD=your-supabase-db-password

# Supabase Service Role Key (for admin operations)
SUPABASE_SERVICE_ROLE_KEY=your-service-role-key

# Supabase JWT Secret (for token validation)
SUPABASE_JWT_SECRET=your-jwt-secret

# Your custom JWT secret
JWT_SECRET=your-custom-jwt-secret
```

### **2. Supabase Configuration**

In your Supabase project:

1. **Go to Settings → API**
2. **Copy the JWT Secret** (not the anon key)
3. **Copy the Service Role Key** (for admin operations)

### **3. Database RLS Policies**

Run these SQL commands in Supabase SQL Editor:

```sql
-- Enable RLS on users table
ALTER TABLE public.users ENABLE ROW LEVEL SECURITY;

-- Policy: Users can view their own profile
CREATE POLICY "Users can view own profile" ON public.users
  FOR SELECT USING (auth.uid() = id);

-- Policy: Users can update their own profile
CREATE POLICY "Users can update own profile" ON public.users
  FOR UPDATE USING (auth.uid() = id);

-- Policy: Service role can do everything (for backend operations)
CREATE POLICY "Service role full access" ON public.users
  FOR ALL USING (auth.role() = 'service_role');
```

## 🔄 **Authorization Flow**

### **1. User Registration/Login**
```
1. Flutter App → POST /auth/register or /auth/login
2. Railway Backend → Calls Supabase Auth API
3. Supabase → Returns JWT token + user data
4. Railway Backend → Creates/updates user profile
5. Flutter App → Receives token for future requests
```

### **2. API Requests**
```
1. Flutter App → Sends request with "Authorization: Bearer <token>"
2. Railway Backend → Validates token with Supabase
3. Railway Backend → Accesses database with service role
4. Supabase → Applies RLS policies
5. Railway Backend → Returns data to Flutter App
```

## 🛡️ **Security Features**

### **1. JWT Token Validation**
- All API requests require valid JWT token
- Tokens are validated with Supabase on every request
- Invalid tokens are rejected immediately

### **2. Role-Based Access Control**
- **Admin**: Can access all user data
- **Parent**: Can access their own data + children data
- **Teacher**: Can access their own data + student data

### **3. Row Level Security (RLS)**
- Database-level security policies
- Users can only see their own data
- Service role can access all data for backend operations

## 📱 **Flutter App Integration**

### **1. Send Authorization Header**
```dart
// In your HTTP client
final response = await dio.post(
  '/users/profile',
  options: Options(
    headers: {
      'Authorization': 'Bearer $accessToken',
    },
  ),
);
```

### **2. Handle Token Refresh**
```dart
// When token expires, refresh it
final refreshResponse = await authService.refreshToken(refreshToken);
if (refreshResponse.success) {
  // Update stored tokens
  await httpClient.setAuthTokens(
    refreshResponse.data!.accessToken,
    refreshResponse.data!.refreshToken,
  );
}
```

## 🧪 **Testing Authorization**

### **1. Test Public Endpoints**
```bash
# Should work without token
curl http://localhost:8080/api/v1/auth/health
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"password123","firstName":"John","lastName":"Doe","role":"parent"}'
```

### **2. Test Protected Endpoints**
```bash
# Should fail without token
curl http://localhost:8080/api/v1/users/profile

# Should work with valid token
curl http://localhost:8080/api/v1/users/profile \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

### **3. Test Role-Based Access**
```bash
# Admin endpoint - should work for admin users
curl http://localhost:8080/api/v1/users \
  -H "Authorization: Bearer ADMIN_JWT_TOKEN"

# Should fail for non-admin users
curl http://localhost:8080/api/v1/users \
  -H "Authorization: Bearer PARENT_JWT_TOKEN"
```

## 🔧 **Troubleshooting**

### **Common Issues**

1. **"Invalid token" errors**
   - Check if JWT secret is correct
   - Verify token hasn't expired
   - Ensure token format is correct

2. **"Access denied" errors**
   - Check user role in Supabase
   - Verify RLS policies are set up
   - Check if user exists in database

3. **"User not found" errors**
   - Verify user profile was created
   - Check if user ID matches between auth and database

### **Debug Steps**

1. **Check logs** in Railway dashboard
2. **Verify environment variables** are set correctly
3. **Test with Supabase dashboard** to verify auth works
4. **Check database** to ensure user profiles exist

## 🚀 **Deployment Checklist**

- [ ] Set all environment variables in Railway
- [ ] Configure RLS policies in Supabase
- [ ] Test authentication flow
- [ ] Test authorization for different roles
- [ ] Verify token validation works
- [ ] Test error handling

## 📊 **Monitoring**

### **Railway Logs**
- Check application logs for auth errors
- Monitor token validation failures
- Watch for database connection issues

### **Supabase Dashboard**
- Monitor auth events
- Check user registrations
- Review API usage

This setup provides secure, scalable authorization between your Railway backend and Supabase database!
