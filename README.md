# JustAuth - Spring Boot Authentication Service

JustAuth is a robust authentication and authorization service built with Spring Boot. It provides secure user registration, login, and token management using OAuth2 Resource Server and JWT (JSON Web Tokens).

## Key Features

*   **OAuth2 Resource Server:** Secures APIs using stateless JWT authentication.
*   **JWT Implementation:** Uses Nimbus JOSE + JWT for secure token generation and validation.
*   **Role-Based Access Control (RBAC):** Manages user permissions via dynamic roles stored in the database.
*   **Token Management:**
    *   **Access Tokens:** Short-lived tokens for API access.
    *   **Refresh Tokens:** Secure mechanism to obtain new access tokens.
    *   **Token Blacklisting:** Revokes access tokens upon logout (implementation pending migration to Caffeine cache).
*   **User Registration:** Secure sign-up process with email verification.
*   **Email Verification:** Integration with Spring Mail for sending verification links.
*   **Security Best Practices:**
    *   Stateless session management.
    *   BCrypt password hashing.
    *   CORS configuration.
    *   Custom exception handling for clear API responses.

## Tech Stack

*   **Core:** Java 21, Spring Boot 3.x
*   **Security:** Spring Security, OAuth2 Resource Server, Nimbus JOSE + JWT
*   **Database:** H2 Database (Dev), JPA/Hibernate
*   **Tools:** Maven, Lombok, MapStruct

## Architecture Highlights

*   **Entity-Based Roles:** Roles are stored as entities in the database (`_role` table) allowing for flexible role management, linked to users via a many-to-many relationship.
*   **Custom Exception Handling:** Centralized `GlobalExceptionHandler` ensures consistent and informative error responses (e.g., `ResourceNotFoundException`, `DuplicateResourceException`).
*   **Separation of Concerns:** Clear separation between Controllers, Services, Repositories, and Security configurations.

## API Endpoints (Key)

*   `POST /api/v1/auth/register`: Register a new user.
*   `POST /api/v1/auth/authenticate`: Login and receive Access/Refresh tokens.
*   `POST /api/v1/auth/refresh-token`: Obtain a new Access Token using a Refresh Token.
*   `GET /api/v1/auth/verify`: Verify user email via token.
*   `POST /api/v1/auth/logout`: Logout and blacklist the current token.

## Getting Started

1.  **Clone the repository.**
2.  **Configure `application.yml`:** Set up your database and mail server properties.
3.  **Run the application:** `./mvnw spring-boot:run`

## Reproduction Prompt

Use the following prompt to recreate this project's core structure and logic:

> **Objective:** Create a production-ready authentication service using Spring Boot 3 and Java 21, implementing OAuth2 Resource Server with JWT for stateless security.
>
> **Tech Stack:**
> *   Language: Java 21
> *   Framework: Spring Boot 3.x (Spring Security, Spring Data JPA, Spring Web, Spring Mail)
> *   Token Library: **Nimbus JOSE + JWT** (Do NOT use jjwt)
> *   Database: H2 (for dev), configured with Hibernate
> *   Tools: Lombok, Maven
>
> **Core Requirements:**
>
> 1.  **Database & Entities:**
>     *   **User Entity:** Table name provided as `_user` (to avoid SQL keywords). Fields: `id`, `firstname`, `lastname`, `email` (unique), `password`, `enabled`.
>     *   **Role Entity:** Table name `_role`. Fields: `id`, `name`. Create a Many-to-Many relationship with `User` (`user_roles` join table).
>     *   **Token Entity:** Store refresh tokens or state if necessary, linked to User.
>
> 2.  **Security Configuration:**
>     *   Configure as an **OAuth2 Resource Server** with JWT support.
>     *   Implement a `JwtService` using **Nimbus JOSE** for generating signed Access Tokens (short-lived) and Refresh Tokens (long-lived).
>     *   Ensure the JWT claims include the user's roles (mapped as a list of strings).
>     *   Enable `BCryptPasswordEncoder`.
>     *   Define a public allowlist for auth endpoints (`/api/v1/auth/**`).
>
> 3.  **Authentication Features:**
>     *   **Register:** Endpoint to create a user. Validate email uniqueness. Assign default role. Send an async verification email containing a verification token.
>     *   **Login:** Validate credentials. Return `AuthenticationResponse` with `accessToken` and `refreshToken`.
>     *   **Refresh Token:** Endpoint to validate a refresh token and issue a new access token.
>     *   **Verify Email:** Endpoint to validate the token sent via email and enable the user account.
>     *   **Logout:** Implement a mechanism to blacklist specific Access Tokens to prevent reuse until expiration (e.g., using a custom Filter or database check).
>
> 4.  **Exception Handling:**
>     *   Create a robust custom exception hierarchy: Base `AppException` (containing `HttpStatus`) extended by `ResourceNotFoundException`, `DuplicateResourceException`, `InvalidTokenException`, etc.
>     *   Implement a `GlobalExceptionHandler` (`@RestControllerAdvice`) to catch these exceptions and return a standardized JSON response (`status`, `message`, `path`).
>
> 5.  **Service Layer:**
>     *   Use Interface-based services (`AuthenticationService`, `UserService`, `EmailService`) with implementations.
>     *   Avoid putting business logic in Controllers.
>
> 6.  **Code Quality:**
>     *   Use Lombok for boilerplate (`@Data`, `@Builder`, `@RequiredArgsConstructor`).
>     *   Follow RESTful API naming conventions.
>     *   Write an integration test for the full authentication flow (Register -> Login -> Verify).

## Using this Project as a Template

If you have downloaded this source code and want to use it as a starting point for a new project, follow these steps:

1.  **Clone the Project:**
    Download the ZIP or clone the repository to your local machine.

2.  **Reset Git History (Recommended):**
    If you want to start a fresh git history for your new project:
    ```bash
    rm -rf .git
    git init
    ```

3.  **Update Project Metadata (`pom.xml`):**
    *   Open `pom.xml`.
    *   Change the `<groupId>` (e.g., `com.yourcompany`).
    *   Change the `<artifactId>` (e.g., `your-new-project-name`).
    *   Change the `<version>` if needed.
    *   Change the `<name>` and `<description>`.

4.  **Refactor Package Names (IntelliJ IDEA):**
    *   **Step 1:** Open the **Project** view sidebar.
    *   **Step 2:** Click the **Gear icon** (Options) in the Project view header and **uncheck "Compact Middle Packages"**. This separates the package levels (e.g., `com` -> `peter` -> `justauth`), allowing you to rename them individually.
    *   **Step 3:** Right-click on the package folder you want to rename (e.g., `peter` or `justauth`).
    *   **Step 4:** Select **Refactor** > **Rename** (or press `Shift+F6`).
    *   **Step 5:** **CRITICAL:** A dialog will appear asking what you want to rename. Select **"Rename package"** (ensure you do NOT just select "Rename directory").
    *   **Step 6:** Enter your new package name (e.g., `yourcompany`) and click **Refactor**.
    *   **Step 7:** Review the changes in the preview pane (if prompted) and click **"Do Refactor"**. IntelliJ will automatically update all `package` declarations and `import` statements in your Java files.

5.  **Configure Environment Variables:**
    *   Open `src/main/resources/application.yml`.
    *   Update `application.security.jwt.secret-key`: Generate a new secure 256-bit secret key.
    *   Update database credentials (`spring.datasource.url`, `username`, `password`) if you are switching from H2 to PostgreSQL/MySQL.
    *   Update mail server settings (`spring.mail.username`, `password`) for email verification to work.

6.  **Clean and Verify:**
    *   Run `./mvnw clean install` to ensure everything builds correctly with the new names.
    *   Start the application: `./mvnw spring-boot:run`.

7.  **Customize:**
    *   Add your own Entities, Services, and Controllers on top of this solid authentication foundation.
