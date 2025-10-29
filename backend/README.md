# School App Backend

Java Spring Boot backend server that acts as a middle layer between Flutter app and Supabase database.

## Architecture

```
Flutter App → Java Spring Boot Server → Supabase Database
```

## Prerequisites

- Java 17 or higher
- Maven 3.6 or higher
- PostgreSQL database (Supabase provides this)

## Setup

### 1. Environment Variables

Create a `.env` file in the backend directory:

```bash
# Supabase Database
SUPABASE_DB_PASSWORD=your-supabase-db-password

# Supabase Service Role Key (for admin operations)
SUPABASE_SERVICE_ROLE_KEY=your-service-role-key

# JWT Secret (generate a secure random string)
JWT_SECRET=your-jwt-secret-key-here-make-it-long-and-secure
```

### 2. Get Supabase Credentials

1. Go to your Supabase project dashboard
2. Go to Settings → Database
3. Copy the database password
4. Go to Settings → API
5. Copy the service_role key (not the anon key)

### 3. Run the Application

```bash
# Navigate to backend directory
cd backend

# Install dependencies
mvn clean install

# Run the application
mvn spring-boot:run
```

The server will start on `http://localhost:8080`

### 4. Test the API

```bash
# Health check
curl http://localhost:8080/api/v1/auth/health

# Register a user
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "password123",
    "firstName": "John",
    "lastName": "Doe",
    "role": "parent",
    "phone": "+1234567890"
  }'

# Login
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "password123"
  }'
```

## API Endpoints

### Authentication

- `POST /api/v1/auth/register` - Register a new user
- `POST /api/v1/auth/login` - Login user
- `POST /api/v1/auth/logout` - Logout user
- `GET /api/v1/auth/health` - Health check

## How It Works

1. **Flutter App** sends registration/login request to Java backend
2. **Java Backend** receives request and validates data
3. **Java Backend** calls Supabase Auth API to create/authenticate user
4. **Java Backend** creates/updates user profile in Supabase database
5. **Java Backend** returns response to Flutter app

## Database Schema

The backend expects the following tables in Supabase:

- `users` - User profiles
- `schools` - School information
- `courses` - Course information
- `payments` - Payment records
- `children` - Children information

## Development

### Adding New Endpoints

1. Create DTO classes in `dto/` package
2. Create service classes in `service/` package
3. Create controller classes in `controller/` package
4. Add repository interfaces in `repository/` package

### Database Migrations

The application uses JPA/Hibernate for database operations. Make sure your Supabase database schema matches the JPA entities.

## Troubleshooting

### Common Issues

1. **Database Connection Error**: Check your Supabase database password
2. **Supabase Auth Error**: Verify your Supabase URL and API keys
3. **Port Already in Use**: Change the port in `application.yml`

### Logs

Check the console output for detailed logs. The application logs all requests and responses for debugging.
