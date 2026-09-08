# CivicFix Security & Privacy Notice

## Data Collection
CivicFix collects the following data when users submit reports:
- **Photos** — images of reported infrastructure issues
- **GPS coordinates** — location of the reported issue
- **Text descriptions** — user-provided issue descriptions
- **Email** — for user authentication (if account created)

## Data Retention
- Reports and associated images are retained indefinitely for civic accountability purposes.
- Users may request deletion of their account and associated personal data.
- Anonymized aggregate data (issue counts, locations) may be retained for public interest.

## Public Posting Disclaimer
> **⚠️ WARNING:** When a report is posted to X (Twitter), the following information becomes **publicly visible**:
> - Issue type, description, and location address
> - Photo evidence
> - GPS coordinates (as a Google Maps link)
>
> Users should NOT include personally identifiable information in report descriptions or photos.

## Minimal PII Policy
- No collection of unnecessary personal data.
- Device IDs are used only for rate limiting and offline sync.
- Passwords are hashed using bcrypt.
- JWT tokens expire after 24 hours.

## Security Measures
- File uploads limited to 10MB, JPEG/PNG only.
- Filenames are sanitized and replaced with random UUIDs.
- Input validation via Pydantic models.
- Rate limiting to prevent abuse.
- CORS configured for allowed origins only.
- Admin endpoints require role-based authentication.
