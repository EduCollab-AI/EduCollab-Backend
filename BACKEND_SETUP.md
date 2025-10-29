# EduCollab Backend Setup Guide

This guide will help you set up the EduCollab backend server locally to work with your Flutter app.

## Prerequisites

### Option 1: Install Node.js and PostgreSQL (Recommended)

1. **Install Node.js 18+**
   ```bash
   # On macOS with Homebrew
   brew install node
   
   # Or download from https://nodejs.org/
   ```

2. **Install PostgreSQL 15+**
   ```bash
   # On macOS with Homebrew
   brew install postgresql@15
   brew services start postgresql@15
   
   # Or download from https://www.postgresql.org/
   ```

3. **Install Redis 7+**
   ```bash
   # On macOS with Homebrew
   brew install redis
   brew services start redis
   
   # Or download from https://redis.io/
   ```

### Option 2: Use Docker (Easier)

1. **Install Docker Desktop**
   - Download from https://www.docker.com/products/docker-desktop/
   - Start Docker Desktop

## Setup Instructions

### Step 1: Navigate to Backend Directory
```bash
cd ../EduCollab-BE
```

### Step 2: Install Dependencies
```bash
npm install
```

### Step 3: Environment Configuration

Create a `.env` file in the backend directory:
```bash
# Copy the example environment file (if it exists)
cp .env.example .env

# Or create a new .env file with the following content:
```

```env
# Application
NODE_ENV=development
PORT=3000
API_VERSION=v1

# Database
DB_HOST=localhost
DB_PORT=5432
DB_NAME=educollab_dev
DB_USERNAME=educollab
DB_PASSWORD=password
DB_SSL=false

# Redis
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=
REDIS_DB=0

# JWT
JWT_SECRET=your-super-secret-jwt-key-change-this-in-production
JWT_ACCESS_EXPIRY=15m
JWT_REFRESH_EXPIRY=7d
JWT_RESET_EXPIRY=1h

# CORS
CORS_ORIGIN=http://localhost:3000,http://localhost:8080

# Optional: Email (for password reset)
SMTP_HOST=smtp.sendgrid.net
SMTP_PORT=587
SMTP_USER=your-sendgrid-username
SMTP_PASSWORD=your-sendgrid-password
FROM_EMAIL=noreply@educollab.com
FROM_NAME=EduCollab Platform
```

### Step 4: Database Setup

#### Option A: Manual Setup
```bash
# Create database
createdb educollab_dev

# Run migrations
npm run db:migrate

# Seed database (optional)
npm run db:seed
```

#### Option B: Docker Setup (Recommended)
```bash
# Start all services with Docker
npm run docker:dev

# This will start:
# - API server on http://localhost:3000
# - PostgreSQL on localhost:5432
# - Redis on localhost:6379
# - Adminer (DB UI) on http://localhost:8080
# - Redis Commander on http://localhost:8081
```

### Step 5: Start the Server

#### Development Mode
```bash
npm run dev
```

#### Production Mode
```bash
npm run build
npm start
```

## API Endpoints

Once the server is running, you can access:

- **API Base URL**: `http://localhost:3000/api/v1`
- **Health Check**: `http://localhost:3000/health`
- **API Documentation**: `http://localhost:3000/api-docs` (if Swagger is enabled)

### Authentication Endpoints
- `POST /api/v1/auth/register` - Register new user
- `POST /api/v1/auth/login` - User login
- `POST /api/v1/auth/refresh` - Refresh access token
- `POST /api/v1/auth/logout` - User logout

### User Endpoints
- `GET /api/v1/users/profile` - Get current user profile
- `PUT /api/v1/users/profile` - Update user profile

## Testing the Integration

### 1. Test with Flutter App
1. Make sure your Flutter app is configured to use `http://localhost:3000/api/v1` as the base URL
2. Run the Flutter app
3. Try to register/login using the app

### 2. Test with API Client (Postman/curl)
```bash
# Test health endpoint
curl http://localhost:3000/health

# Test registration
curl -X POST http://localhost:3000/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "password123",
    "firstName": "John",
    "lastName": "Doe",
    "role": "parent"
  }'

# Test login
curl -X POST http://localhost:3000/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "password123"
  }'
```

## Troubleshooting

### Common Issues

1. **Port 3000 already in use**
   ```bash
   # Find process using port 3000
   lsof -i :3000
   
   # Kill the process
   kill -9 <PID>
   ```

2. **Database connection failed**
   - Make sure PostgreSQL is running
   - Check database credentials in `.env`
   - Try restarting PostgreSQL service

3. **Redis connection failed**
   - Make sure Redis is running
   - Check Redis configuration in `.env`

4. **CORS errors in Flutter app**
   - Make sure `CORS_ORIGIN` in `.env` includes your Flutter app's URL
   - For development, you can use `CORS_ORIGIN=*` (not recommended for production)

### Logs
- Check server logs in the terminal where you started the server
- For Docker setup, use `docker-compose logs api` to see API logs

## Next Steps

1. **Customize the API**: Modify the backend code to match your specific requirements
2. **Add more endpoints**: Create additional routes for your app's features
3. **Set up production**: Configure the server for production deployment
4. **Add monitoring**: Set up logging and monitoring tools

## Support

If you encounter any issues:
1. Check the server logs for error messages
2. Verify all prerequisites are installed correctly
3. Ensure all environment variables are set properly
4. Check the EduCollab-BE README.md for more detailed information 