## ADDED Requirements

### Requirement: Persistent navigation bar renders on authenticated routes

When a user is logged in, the system SHALL display a top navigation bar across all authenticated pages containing the app branding, current page indicator, and user controls.

#### Scenario: Navbar displays after login

- **WHEN** user completes login or registration successfully
- **THEN** the system displays a top navbar with "AI Interview Prep" branding on the left
- **AND** shows navigation links for active pages (Interview, Analytics)
- **AND** displays the logged-in user's name in the top-right corner

#### Scenario: Navbar persists across route changes

- **WHEN** user navigates between any authenticated routes (`/interview/*`, `/results/:sessionId`, `/analytics`)
- **THEN** the navbar remains visible with appropriate active page indicator

### Requirement: Logout functionality is accessible from navigation bar

When a user clicks the logout button in the navigation bar, the system SHALL clear the authentication session and navigate to the login page.

#### Scenario: User logs out via navbar

- **WHEN** user clicks the "Logout" button in the top-right of the navbar
- **THEN** the system calls `POST /auth/logout` endpoint
- **AND** clears `isAuthenticated` and `user_name` from sessionStorage
- **AND** navigates to `/login`

#### Scenario: Logout button only shows when authenticated

- **WHEN** user is not logged in (on login or register page)
- **THEN** the navbar does not display a logout button

### Requirement: User name displays in navigation bar

When a user is logged in, the system SHALL display their name in the top-right corner of the navigation bar.

#### Scenario: Display current user name

- **WHEN** user is authenticated and viewing any page
- **THEN** the navbar shows the user's name retrieved from `AuthService.getUserName()`
