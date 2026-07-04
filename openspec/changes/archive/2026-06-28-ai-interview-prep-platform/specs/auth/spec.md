## ADDED Requirements

### Requirement: User can register with email and password
A user SHALL be able to create an account by providing a name, email address, and password. The system SHALL validate that the email is unique before creating the account.

#### Scenario: Successful registration
- **WHEN** user submits valid registration form with unique email
- **THEN** system creates user account and redirects to login page

#### Scenario: Duplicate email registration
- **WHEN** user submits registration with an already registered email
- **THEN** system displays error message "Email already in use"

### Requirement: User can log in with credentials
A registered user SHALL be able to authenticate by providing their email and password. Upon successful authentication, the system SHALL set a JWT token in an httpOnly cookie and redirect to the dashboard.

#### Scenario: Successful login
- **WHEN** user submits valid email and password
- **THEN** system sets JWT in httpOnly cookie and redirects to dashboard

#### Scenario: Failed login with invalid credentials
- **WHEN** user submits incorrect email or password
- **THEN** system displays error message "Invalid credentials"

### Requirement: User can log out
An authenticated user SHALL be able to log out by clearing the JWT token from the httpOnly cookie.

#### Scenario: Successful logout
- **WHEN** user clicks logout button
- **THEN** system clears the httpOnly cookie and redirects to login page
