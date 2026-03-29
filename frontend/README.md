# OWASP Demo - React Frontend

A React frontend for the OWASP Top 10 Java Spring Boot demo application.

## Features

- JWT-based authentication
- Role-based access control (User/Admin)
- Dashboard with user profile
- Admin user search functionality
- Responsive design

## Getting Started

1. Install dependencies:
   ```bash
   npm install
   ```

2. Start the development server:
   ```bash
   npm run dev
   ```

3. Open [http://localhost:5173](http://localhost:5173) in your browser

## Backend Requirements

Make sure the Spring Boot backend is running on `http://localhost:8080` with the following endpoints:

- `POST /api/v1/secure/auth/login` - User login
- `GET /api/v1/secure/auth/me` - Get current user profile
- `GET /api/v1/secure/accounts/{userId}` - Get user account (admin only)

## Demo Credentials

- **Admin**: username: `admin`, password: `Admin123!`
- **User**: username: `alice`, password: `Alice123!`

## Available Routes

- `/login` - Login page
- `/dashboard` - Main dashboard (authenticated users)
- `/admin/users` - User search (admin only)