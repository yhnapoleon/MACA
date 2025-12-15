# Backend Integration Map (Revised)

This document outlines the parts of the Android application that currently use the `MockRepository` and need to be connected to a real .NET backend. This revised version includes critical details about authentication and image sourcing.

## 1. General Architecture & Authentication

- **Integration Point**: The entire `MockRepository` class, which implements the `GameRepository` interface, needs to be replaced with a `RemoteRepository` that makes real network calls.
- **Authentication Flow**: To secure the application, the backend should implement a token-based authentication system (e.g., JWT - JSON Web Tokens).
    1. On successful login, the `/api/auth/login` endpoint should return an authentication token along with the user type.
    2. The Android client must securely store this token.
    3. For all subsequent authenticated requests (like submitting a score), the client must include this token in the `Authorization` HTTP header (e.g., `Authorization: Bearer <your_token>`).

## 2. API Endpoint Details

### Login

- **File**: `app/src/main/java/com/team06/maca/repository/MockRepository.kt`
- **Method**: `login(username: String, password: String)`
- **.NET API Endpoint**:
    - **Method**: `POST`
    - **Endpoint**: `/api/auth/login`
    - **Request Body**: `{ "username": "...", "password": "..." }`
    - **Success Response (200 OK)**: `{ "userType": "Paid User", "token": "..." }` or `{ "userType": "Free User", "token": "..." }`
    - **Error Response (401 Unauthorized)**: `{ "error": "Invalid credentials" }`

### Image Source (New Section)

- **File**: `app/src/main/java/com/team06/maca/FetchActivity.kt`
- **Method**: `fetchAndDownloadImages()`
- **Current Logic**: Scrapes images directly from `https://stocksnap.io`. This is not a production-ready approach as it is unreliable and may violate the source's terms of service.
- **.NET API Endpoint (Recommended)**:
    - **Method**: `GET`
    - **Endpoint**: `/api/game/images?count=20`
    - **Description**: This endpoint should return a list of 20 image URLs for the memory game. This decouples the client from the image source.
    - **Success Response (200 OK)**: `{ "images": ["url1", "url2", ...] }`

### Ads

- **File**: `app/src/main/java/com/team06/maca/repository/MockRepository.kt`
- **Method**: `getNextAd()` (Note: `getAds()` is currently unused).
- **Current Logic**: Cycles through a hardcoded list of placeholder image URLs.
- **.NET API Endpoint**:
    - **Method**: `GET`
    - **Endpoint**: `/api/ads/next`
    - **Success Response (200 OK)**: `{ "adUrl": "..." }`

### Score Submission

- **File**: `app/src/main/java/com/team06/maca/repository/MockRepository.kt`
- **Method**: `submitScore(user: String, score: Int)`
- **Critical Note**: The client currently calls `submitScore("User", elapsedTime)`, hardcoding the username. The backend **should not** use the `user` field from the request body. It **must** identify the user via the authentication token provided in the `Authorization` header.
- **.NET API Endpoint**:
    - **Method**: `POST`
    - **Endpoint**: `/api/scores`
    - **Header**: `Authorization: Bearer <user_token>`
    - **Request Body**: `{ "score": ... }`
    - **Success Response (201 Created)**: `{}`

### Leaderboard

- **File**: `app/src/main/java/com/team06/maca/repository/MockRepository.kt`
- **Method**: `getTop5()`
- **.NET API Endpoint**:
    - **Method**: `GET`
    - **Endpoint**: `/api/leaderboard/top5`
    - **Success Response (200 OK)**: `{ "leaderboard": [{ "user": "...", "score": ... }, ...] }`
