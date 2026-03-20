# Changelog

All notable changes to this project will be documented in this file.

## [2.0.0] - 2026-03-21

### Added
- **Issue Explorer**: New platform to discover open-source opportunities using GitHub's Search API.
- **Dark Mode**: Fully themed midnight blue UI with dynamic icon/text toggling.
- **Parallel Processing**: Asynchronous API calls with `CompletableFuture` for faster data retrieval.
- **Caching Layer**: Integrated Spring Cache to minimize API rate limit consumption.
- **Advanced UI**: Premium Glassmorphism design and responsive layout improvements.

### Fixed
- **Cache Key Resolution**: Resolved "Null key" errors by using robust index-based parameter mapping (`#p0`).
- **Dependency Issues**: Fixed iText 7 modules and missing Spring Boot starters.
- **Runtime Errors**: Resolved `@RequestParam` naming conflicts and Thymeleaf nesting bugs.
- **Search History**: Fixed persistence and UI visibility across different view states.

### Refactored
- **Code Quality**: Migrated all models to **Lombok** to reduce boilerplate.
- **Error Handling**: Implemented `@ControllerAdvice` for centralized Exception Management.
- **API Integration**: Updated `GithubService` with `ParameterizedTypeReference` for type-safe RestTemplate calls.
