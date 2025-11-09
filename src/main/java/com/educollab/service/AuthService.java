package com.educollab.service;

import com.educollab.config.SupabaseConfig;
import com.educollab.model.User;
import com.educollab.model.Student;
import com.educollab.repository.UserRepository;
import com.educollab.repository.StudentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class AuthService {
    
    @Autowired
    private SupabaseConfig supabaseConfig;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private StudentRepository studentRepository;
    
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> register(Map<String, Object> request) {
        try {
            // Step 1: Register user in Supabase Auth
            Map<String, Object> authRequest = new HashMap<>();
            authRequest.put("email", request.get("email"));
            authRequest.put("password", request.get("password"));
            
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("name", request.get("name"));
            metadata.put("role", request.get("role"));
            metadata.put("phone", request.get("phone"));
            authRequest.put("data", metadata);
            
            // Call Supabase Auth API
            WebClient webClient = supabaseConfig.supabaseWebClient();
            System.out.println("========================================");
            System.out.println("📤 Calling Supabase Auth API:");
            System.out.println("Endpoint: /auth/v1/signup");
            System.out.println("Supabase URL: " + supabaseConfig.getSupabaseUrl());
            System.out.println("Request payload: " + authRequest);
            System.out.println("Email: " + request.get("email"));
            System.out.println("Email type: " + (request.get("email") != null ? request.get("email").getClass().getName() : "null"));
            System.out.println("Has password: " + (request.get("password") != null));
            System.out.println("Metadata: " + metadata);
            System.out.println("========================================");
            
            // Build full URL for logging
            String fullUrl = supabaseConfig.getSupabaseUrl() + "/auth/v1/signup";
            System.out.println("🌐 Full Supabase Auth URL: " + fullUrl);
            System.out.println("📝 Request Method: POST");
            try {
                ObjectMapper objectMapper = new ObjectMapper();
                System.out.println("📦 Request Body JSON: " + objectMapper.writeValueAsString(authRequest));
            } catch (Exception e) {
                System.out.println("📦 Request Body (unable to serialize as JSON): " + authRequest);
            }
            
            Map<String, Object> authResponse;
            try {
                System.out.println("========================================");
                System.out.println("📡 HTTP Request Details:");
                System.out.println("Request URL: " + fullUrl);
                System.out.println("Request Method: POST");
                System.out.println("Request Body: " + authRequest);
                System.out.println("========================================");
                
                authResponse = webClient.post()
                    .uri("/auth/v1/signup")
                    .bodyValue(authRequest)
                    .retrieve()
                    .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(),
                        response -> {
                            System.err.println("========================================");
                            System.err.println("❌ Supabase Auth API HTTP Error:");
                            System.err.println("Status Code: " + response.statusCode());
                            System.err.println("Response Headers: " + response.headers().asHttpHeaders());
                            return response.bodyToMono(String.class)
                                .flatMap(body -> {
                                    System.err.println("Error Response Body: " + body);
                                    System.err.println("========================================");
                                    return Mono.error(new RuntimeException("Supabase Auth API error " + response.statusCode() + ": " + body));
                                });
                        })
                    .bodyToMono(Map.class)
                    .doOnSuccess(response -> {
                        System.out.println("========================================");
                        System.out.println("✅ Supabase Auth API Success Response:");
                        System.out.println("Full Response: " + response);
                        System.out.println("Response Keys: " + response.keySet());
                        if (response.containsKey("user")) {
                            System.out.println("User object: " + response.get("user"));
                        }
                        if (response.containsKey("access_token")) {
                            System.out.println("Access token present: " + (response.get("access_token") != null ? "Yes" : "No"));
                        }
                        System.out.println("========================================");
                    })
                    .doOnError(error -> {
                        System.err.println("========================================");
                        System.err.println("❌ Supabase Auth API Error:");
                        System.err.println("Error: " + error.getMessage());
                        System.err.println("========================================");
                    })
                    .block();
            } catch (Exception e) {
                System.err.println("========================================");
                System.err.println("❌ Exception calling Supabase Auth API:");
                System.err.println("Exception: " + e.getMessage());
                System.err.println("Exception Type: " + e.getClass().getName());
                e.printStackTrace();
                System.err.println("========================================");
                throw new RuntimeException("Failed to create user in Supabase Auth: " + e.getMessage(), e);
            }
            
            // Log the full response for debugging
            System.out.println("========================================");
            System.out.println("📋 Processing Supabase Response:");
            System.out.println("Response is null: " + (authResponse == null));
            if (authResponse != null) {
                System.out.println("Response class: " + authResponse.getClass().getName());
                System.out.println("Response keys: " + authResponse.keySet());
                System.out.println("Full response: " + authResponse);
                System.out.println("Has 'user' key: " + authResponse.containsKey("user"));
                System.out.println("Has 'error' key: " + authResponse.containsKey("error"));
                System.out.println("Has 'access_token' key: " + authResponse.containsKey("access_token"));
            }
            System.out.println("========================================");
            
            if (authResponse == null) {
                System.err.println("❌ Auth response is null");
                throw new RuntimeException("Failed to create user in Supabase Auth - null response");
            }
            
            // Check if Supabase returned an error
            if (authResponse.containsKey("error") || authResponse.containsKey("error_description")) {
                String errorMsg = (String) authResponse.getOrDefault("error", authResponse.getOrDefault("error_description", "Unknown error"));
                System.err.println("❌ Supabase Auth error in response: " + authResponse);
                throw new RuntimeException("Failed to create user in Supabase Auth: " + errorMsg);
            }
            
            // Step 2: Handle Supabase response format
            // Supabase can return user data either nested under "user" key or directly in the response
            Map<String, Object> supabaseUser;
            if (authResponse.containsKey("user")) {
                // Standard format: response contains "user" key
                supabaseUser = (Map<String, Object>) authResponse.get("user");
                System.out.println("✅ Found user in 'user' key");
            } else if (authResponse.containsKey("id")) {
                // Direct format: user data is at top level
                supabaseUser = authResponse;
                System.out.println("✅ Found user data at top level");
            } else {
                System.err.println("❌ Auth response missing user data");
                System.err.println("Full response: " + authResponse);
                System.err.println("Response keys: " + authResponse.keySet());
                throw new RuntimeException("Failed to create user in Supabase Auth - response missing user data. Response: " + authResponse);
            }
            
            String userId = (String) supabaseUser.get("id");
            System.out.println("✅ Extracted user ID: " + userId);
            
            // Step 3: Check if user profile already exists (from trigger)
            System.out.println("========================================");
            System.out.println("🔍 Checking if user profile exists in database:");
            System.out.println("User ID: " + userId);
            User user = userRepository.findById(UUID.fromString(userId)).orElse(null);
            
            if (user != null) {
                System.out.println("✅ User profile already exists (created by trigger)");
                System.out.println("User email: " + user.getEmail());
            } else {
                System.out.println("⚠️ User profile not found, creating manually...");
            }
            
            // Step 4: If user profile doesn't exist, create it manually
            if (user == null) {
                System.out.println("📝 Creating new user profile:");
                user = new User();
                user.setId(UUID.fromString(userId));
                user.setEmail((String) request.get("email"));
                user.setName((String) request.get("name"));
                user.setRole((String) request.get("role"));
                user.setPhone((String) request.get("phone"));
                user.setAvatarUrl(null);
                user.setCreatedAt(java.time.LocalDateTime.now());
                user.setUpdatedAt(java.time.LocalDateTime.now());
                
                System.out.println("User object created:");
                System.out.println("  ID: " + user.getId());
                System.out.println("  Email: " + user.getEmail());
                System.out.println("  Name: " + user.getName());
                System.out.println("  Role: " + user.getRole());
                
                try {
                    System.out.println("💾 Attempting to save user to database...");
                    User savedUser = userRepository.save(user);
                    System.out.println("✅ User profile saved successfully!");
                    System.out.println("Saved user ID: " + savedUser.getId());
                    System.out.println("Saved user email: " + savedUser.getEmail());
                } catch (org.springframework.transaction.CannotCreateTransactionException e) {
                    System.err.println("❌ Database connection error:");
                    System.err.println("Error: " + e.getMessage());
                    System.err.println("This usually means:");
                    System.err.println("  1. DATABASE_URL is incorrect or not set");
                    System.err.println("  2. Database is not accessible");
                    System.err.println("  3. Connection pool issues");
                    e.printStackTrace();
                    throw new RuntimeException("Database connection failed. Please check DATABASE_URL environment variable: " + e.getMessage(), e);
                } catch (Exception e) {
                    System.err.println("❌ Error saving user profile to database:");
                    System.err.println("Error: " + e.getMessage());
                    System.err.println("Exception type: " + e.getClass().getName());
                    e.printStackTrace();
                    throw new RuntimeException("Failed to save user profile to database: " + e.getMessage(), e);
                }
            }
            System.out.println("========================================");
            
            // Step 5: Build response
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Registration successful");
            
            Map<String, Object> userData = new HashMap<>();
            userData.put("id", user.getId().toString());
            userData.put("email", user.getEmail());
            userData.put("name", user.getName());
            userData.put("role", user.getRole());
            
            Map<String, Object> data = new HashMap<>();
            data.put("user", userData);
            // Handle access token - Supabase may not return it if email confirmation is required
            String accessToken = (String) authResponse.get("access_token");
            String refreshToken = (String) authResponse.get("refresh_token");
            
            if (accessToken != null) {
                data.put("accessToken", accessToken);
                data.put("refreshToken", refreshToken);
                data.put("expiresIn", 3600);
                System.out.println("✅ Access token present in response");
            } else {
                // Email confirmation may be required
                System.out.println("⚠️ No access token in response - email confirmation may be required");
                data.put("accessToken", "pending_email_confirmation");
                data.put("refreshToken", "pending_email_confirmation");
                data.put("expiresIn", 0);
            }
            
            response.put("data", data);
            
            return response;
            
        } catch (Exception e) {
            System.err.println("Registration error: " + e.getMessage());
            e.printStackTrace();
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Registration failed: " + e.getMessage());
            
            return errorResponse;
        }
    }
    
    @Transactional(readOnly = true)
    public Map<String, Object> login(Map<String, Object> request) {
        try {
            // Extract login credentials - support both email and phone
            String email = (String) request.get("email");
            String phone = (String) request.get("phone");
            String password = (String) request.get("password");
            String requestedRole = (String) request.get("role");
            
            System.out.println("========================================");
            System.out.println("🔐 Login Request:");
            System.out.println("Email: " + email);
            System.out.println("Phone: " + phone);
            System.out.println("Requested Role: " + requestedRole);
            System.out.println("========================================");
            
            // Validate that either email or phone is provided
            if ((email == null || email.isEmpty()) && (phone == null || phone.isEmpty())) {
                throw new RuntimeException("Either email or phone must be provided");
            }
            
            // If phone is provided but not email, we need to find the user's email first
            String loginEmail = email;
            if (email == null || email.isEmpty()) {
                System.out.println("📱 Phone login detected, looking up email by phone...");
                User phoneUser = userRepository.findByPhone(phone)
                    .orElseThrow(() -> new RuntimeException("No user found with this phone number"));
                loginEmail = phoneUser.getEmail();
                System.out.println("✅ Found email: " + loginEmail + " for phone: " + phone);
            }
            
            // Call Supabase Auth API for login (Supabase only supports email/password)
            Map<String, Object> authRequest = new HashMap<>();
            authRequest.put("email", loginEmail);
            authRequest.put("password", password);
            
            System.out.println("🔑 Authenticating with Supabase using email: " + loginEmail);
            
            WebClient webClient = supabaseConfig.supabaseWebClient();
            Map<String, Object> authResponse;
            try {
                authResponse = webClient.post()
                    .uri("/auth/v1/token?grant_type=password")
                    .bodyValue(authRequest)
                    .retrieve()
                    .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(),
                        response -> {
                            System.err.println("========================================");
                            System.err.println("❌ Supabase Auth API HTTP Error:");
                            System.err.println("Status Code: " + response.statusCode());
                            return response.bodyToMono(String.class)
                                .flatMap(body -> {
                                    System.err.println("Error Response Body: " + body);
                                    System.err.println("========================================");
                                    
                                    // Check if it's an email not confirmed error
                                    if (body != null && body.contains("email_not_confirmed")) {
                                        return Mono.error(new RuntimeException("Email not confirmed. Please check your email and click the confirmation link."));
                                    }
                                    
                                    return Mono.error(new RuntimeException("Supabase Auth API error " + response.statusCode() + ": " + body));
                                });
                        })
                    .bodyToMono(Map.class)
                    .block();
            } catch (Exception e) {
                System.err.println("========================================");
                System.err.println("❌ Exception calling Supabase Auth API:");
                System.err.println("Exception: " + e.getMessage());
                System.err.println("Exception Type: " + e.getClass().getName());
                e.printStackTrace();
                System.err.println("========================================");
                throw new RuntimeException("Failed to authenticate with Supabase: " + e.getMessage(), e);
            }
            
            if (authResponse == null || authResponse.get("user") == null) {
                throw new RuntimeException("Invalid credentials");
            }
            
            // Get user from database
            Map<String, Object> supabaseUser = (Map<String, Object>) authResponse.get("user");
            String userId = (String) supabaseUser.get("id");
            User user = userRepository.findById(UUID.fromString(userId))
                .orElseThrow(() -> new RuntimeException("User profile not found"));
            
            System.out.println("✅ User found in database:");
            System.out.println("  ID: " + user.getId());
            System.out.println("  Email: " + user.getEmail());
            System.out.println("  Role: " + user.getRole());
            
            // Validate role if provided
            if (requestedRole != null && !requestedRole.isEmpty()) {
                if (!requestedRole.equalsIgnoreCase(user.getRole())) {
                    System.err.println("❌ Role mismatch:");
                    System.err.println("  Expected: " + requestedRole);
                    System.err.println("  Actual: " + user.getRole());
                    throw new RuntimeException("Invalid role for this account. Expected: " + requestedRole + ", but account is: " + user.getRole());
                }
                System.out.println("✅ Role validated: " + user.getRole());
            }
            
            // Build response
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Login successful");
            
            Map<String, Object> userData = new HashMap<>();
            userData.put("id", user.getId().toString());
            userData.put("email", user.getEmail());
            userData.put("role", user.getRole());
            userData.put("name", user.getName());
            userData.put("user_Name", user.getName());
            userData.put("phone", user.getPhone());
            userData.put("avatar_url", user.getAvatarUrl());
            // Keep camelCase variants for backward compatibility with existing clients
            userData.put("userName", user.getName());
            userData.put("avatarUrl", user.getAvatarUrl());
            
            Map<String, Object> data = new HashMap<>();
            data.put("user", userData);
            
            // If user is a parent, fetch their children
            if ("parent".equalsIgnoreCase(user.getRole())) {
                System.out.println("🔍 Fetching children for parent: " + user.getId());
                List<Student> children = studentRepository.findByAssociatedParentId(user.getId().toString());
                System.out.println("✅ Found " + children.size() + " children");
                
                List<Map<String, Object>> childrenData = new ArrayList<>();
                for (Student child : children) {
                    Map<String, Object> childData = new HashMap<>();
                    childData.put("studentId", child.getId().toString());
                    childData.put("studentName", child.getName());
                    childrenData.add(childData);
                }
                
                data.put("children", childrenData);
                System.out.println("✅ Added children data to response");
            }
            
            // Handle access token - Supabase may not return it if email confirmation is required
            String accessToken = (String) authResponse.get("access_token");
            String refreshToken = (String) authResponse.get("refresh_token");
            
            if (accessToken != null) {
                data.put("accessToken", accessToken);
                data.put("refreshToken", refreshToken);
                data.put("expiresIn", 3600);
                System.out.println("✅ Access token present in response");
            } else {
                // Email confirmation may be required
                System.out.println("⚠️ No access token in response - email confirmation may be required");
                data.put("accessToken", "pending_email_confirmation");
                data.put("refreshToken", "pending_email_confirmation");
                data.put("expiresIn", 0);
            }
            
            response.put("data", data);
            
            return response;
            
        } catch (Exception e) {
            System.err.println("Login error: " + e.getMessage());
            e.printStackTrace();
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Login failed: " + e.getMessage());
            
            return errorResponse;
        }
    }
    
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> addChild(Map<String, Object> request) {
        try {
            String studentIdStr = (String) request.get("studentId");
            String parentId = (String) request.get("parentId");
            Boolean isAssociated = (Boolean) request.getOrDefault("isAssociated", true);
            LocalDate birthdate = null;
            
            if (request.get("birthdate") != null) {
                String birthdateStr = (String) request.get("birthdate");
                if (birthdateStr != null && !birthdateStr.isEmpty()) {
                    birthdate = LocalDate.parse(birthdateStr);
                }
            }
            
            System.out.println("========================================");
            System.out.println("📝 Adding Child:");
            System.out.println("Student ID: " + studentIdStr);
            System.out.println("Parent ID: " + parentId);
            System.out.println("Is Associated: " + isAssociated);
            System.out.println("Birthdate: " + birthdate);
            System.out.println("========================================");
            
            UUID studentId = null;
            if (studentIdStr != null && !studentIdStr.isEmpty()) {
                try {
                    studentId = UUID.fromString(studentIdStr);
                } catch (IllegalArgumentException e) {
                    throw new RuntimeException("Invalid studentId format: " + studentIdStr);
                }
            }
            
            Student student = null;
            if (studentId != null) {
                student = studentRepository.findById(studentId).orElse(null);
            }
            
            if (student == null) {
                System.out.println("Creating new student record" + (studentId != null ? " with provided ID: " + studentId : " with auto-generated ID"));
                student = new Student();
                if (studentId != null) {
                    student.setId(studentId);
                }
                student.setCreatedAt(LocalDateTime.now());
            } else {
                System.out.println("Updating existing student: " + student.getName() + " (ID: " + student.getId() + ")");
            }
            
            String name = (String) request.get("name");
            if (name != null && !name.isEmpty()) {
                student.setName(name);
            } else if (student.getName() == null || student.getName().isEmpty()) {
                throw new RuntimeException("Name is required when creating a new student");
            }
            
            student.setAssociatedParentId(parentId);
            student.setIsAssociated(isAssociated);
            if (birthdate != null) {
                student.setBirthDate(birthdate);
            }
            
            Student savedStudent = studentRepository.save(student);
            System.out.println("✅ Child saved successfully with ID: " + savedStudent.getId());
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Child added successfully");
            
            Map<String, Object> data = new HashMap<>();
            data.put("id", savedStudent.getId().toString());
            response.put("data", data);
            
            return response;
            
        } catch (Exception e) {
            System.err.println("Add child error: " + e.getMessage());
            e.printStackTrace();
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Failed to add child: " + e.getMessage());
            
            return errorResponse;
        }
    }
}

