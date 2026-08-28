# Technology Stack

**Analysis Date:** 2026-08-28

## Languages

**Primary:**
- Kotlin 1.7.10 - Backend application logic in `backend/src/main/kotlin/`
- TypeScript 5.0.4 - Frontend application and type safety in `frontend/src/`
- React 18.2.0 - Frontend UI framework via Next.js

**Secondary:**
- JavaScript - Configuration files and build tools
- Shell - Setup scripts (`setup.sh`)

## Runtime

**Environment:**
- Java 11+ - Runs Kotlin backend via Dropwizard framework
- Node.js 18+ - Runs Next.js frontend development and build server
- Docker - Containerization for Ollama LLM service

**Package Manager:**
- Gradle 8.x (wrapper via `backend/gradlew`) - Backend dependency and build management
- npm (via `frontend/package.json`) - Frontend dependency and script management
- Lockfile: `package-lock.json` present (backend uses Gradle dependency locking implicitly)

## Frameworks

**Core:**
- Dropwizard 2.1.4 - RESTful web framework and microservices foundation for backend (`backend/build.gradle.kts`)
- Next.js 13.3.0 - React framework with server-side rendering and built-in API routing (`frontend/package.json`)
- React 18.2.0 - UI library for frontend components

**Testing:**
- JUnit 5 (Jupiter 5.8.1) - Backend unit testing framework (`backend/buildSrc/src/main/kotlin/Libraries.kt`)
- Mockk 1.9.3 - Kotlin mocking library for backend tests

**Build/Dev:**
- Gradle 8.x - Backend build automation and task execution
- Next.js 13.3.0 - Frontend dev server, build, and lint commands
- TypeScript 5.0.4 - Compiler for frontend type checking

## Key Dependencies

**Critical:**
- Jackson (Kotlin module 2.14.0, JSR310 2.14.0) - JSON serialization/deserialization for API requests/responses (`backend/buildSrc/src/main/kotlin/Libraries.kt`)
- Kotlin Logging 1.7.8 - Structured logging facade for backend (`backend/buildSrc/src/main/kotlin/Libraries.kt`)
- Jackson Datatype JSR310 2.14.0 - Java time API support for JSON serialization

**Infrastructure:**
- Tailwind CSS 3.3.1 - Utility-first CSS framework for frontend styling (`frontend/package.json`)
- Autoprefixer 10.4.14 - CSS vendor prefixing for cross-browser compatibility
- PostCSS 8.4.22 - CSS transformation tool for Tailwind integration
- ESLint 8.38.0 - JavaScript/TypeScript linting in frontend
- eslint-config-next 13.3.0 - Next.js ESLint configuration presets

## Configuration

**Environment:**
- Backend configuration via YAML (`backend/config.yml`) - Server ports (8080 for application, 8081 for admin)
- No environment variable dependency detected - configuration is hardcoded or YAML-based
- Frontend API proxy via Next.js rewrites (`frontend/next.config.js`) - routes `/api/*` to `http://localhost:8080/*`

**Build:**
- Backend: `backend/build.gradle.kts` - Gradle multi-project setup with Kotlin JVM plugin
- Backend: `backend/buildSrc/src/main/kotlin/Libraries.kt` - Centralized dependency version management
- Frontend: `frontend/next.config.js` - Next.js configuration with rewrites
- Frontend: `frontend/tsconfig.json` - TypeScript compiler options (target ES5, JSX preserve)
- Frontend: `frontend/tailwind.config.js` - Tailwind CSS content scanning for `pages/` and `components/`
- Frontend: `frontend/postcss.config.js` - PostCSS plugins (Tailwind, Autoprefixer)
- Gradle properties: `backend/gradle.properties` - Kotlin code style set to official

## Platform Requirements

**Development:**
- Java Development Kit (JDK) 11+ - Required to compile and run backend
- Node.js 18+ - Required for frontend dev server and npm
- Docker & Docker Compose - Required to run Ollama LLM service
- Terminal/Shell - For running `setup.sh` and build commands

**Production:**
- Java 11+ runtime (JRE or JDK) - Runs compiled Dropwizard backend
- Node.js 18+ - Required for Next.js production server (or static export)
- Ollama service - Requires Docker or local Ollama installation for LLM functionality
- Port availability: 8080 (backend), 8081 (backend admin), 3000 (frontend dev), 11434 (Ollama)

---

*Stack analysis: 2026-08-28*
