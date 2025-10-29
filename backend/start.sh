#!/bin/bash

# School App Backend Startup Script for Local Development

echo "🚀 Starting School App Backend..."

# Check if Java is installed
if ! command -v java &> /dev/null; then
    echo "❌ Java is not installed. Please install Java 17 or higher."
    exit 1
fi

# Check if Maven is installed
if ! command -v mvn &> /dev/null; then
    echo "❌ Maven is not installed. Please install Maven 3.6 or higher."
    exit 1
fi

# Check if .env file exists
if [ ! -f .env ]; then
    echo "⚠️  .env file not found. Creating from template..."
    if [ -f env.local.example ]; then
        cp env.local.example .env
        echo "📝 Created .env file from template."
        echo "⚠️  Please edit .env file with your actual Supabase credentials and run again."
        exit 1
    else
        echo "❌ env.local.example not found. Please create .env file manually."
        exit 1
    fi
fi

# Load environment variables from .env file
set -a
source .env
set +a

echo "✅ Loaded environment variables from .env"

# Build application
echo "🔧 Building application..."
mvn clean install -DskipTests

if [ $? -ne 0 ]; then
    echo "❌ Build failed. Please check the errors above."
    exit 1
fi

# Start application with local profile
echo "🎯 Starting application on localhost:8080..."
echo "📝 Using profile: local"
echo "🌐 API will be available at: http://localhost:8080/api/v1"
echo ""
mvn spring-boot:run -Dspring-boot.run.profiles=local
