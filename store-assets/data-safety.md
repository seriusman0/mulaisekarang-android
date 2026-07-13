# Play Console "Data Safety" form — answer key

This maps exactly what the app collects/transmits, derived from the actual code
(`data/model/Models.kt`, `data/AuthRepository.kt`, `data/ChatRepository.kt`, the
`/api/v1/courses/{id}/checkout` flow), to help fill Play Console's Data Safety
questionnaire. That form itself is web-UI only and can't be filled from a file —
use this as the reference while filling it in.

## Does your app collect or share any of the required user data types?
**Yes.**

## Data types collected

| Category | Field | Collected? | Shared with 3rd party? | Purpose |
|---|---|---|---|---|
| Personal info | Name | Yes | No | Account functionality |
| Personal info | Email address | Yes | No | Account functionality, authentication |
| Personal info | User IDs (username) | Yes | No | Account functionality |
| Photos | Profile photo | Yes (optional, incl. via Google Sign-In) | No | App functionality |
| Messages | In-app messages (chat with mentors) | Yes | No | App functionality (mentor-student chat) |
| Financial info | Purchase history (course enrollment/transaction status) | Yes | Yes — Xendit (payment processor) | Payment processing, fraud prevention |
| Financial info | Payment info (card/bank/e-wallet details) | **No** | — | Handled entirely by Xendit's hosted checkout page; the app/backend never receives or stores raw payment instrument data |
| App activity | App interactions (course progress, lesson completion) | Yes | No | App functionality, analytics |
| Device or other IDs | none collected beyond standard IP/user-agent in server logs | Yes (IP, minimal) | No | Security/fraud prevention |

## Data collection is always required?
Name/email: required (account creation). Profile photo: optional. Chat messages: only if the user chooses to use chat. Payment data: only for paid-course purchases.

## Is data encrypted in transit?
Yes — all production traffic is HTTPS only (see `app/src/release/res/xml/network_security_config.xml`, no cleartext exceptions in release builds).

## Can users request data deletion?
Yes — via the contact email in the privacy policy (`resources/views/public/privacy-policy.blade.php` on the backend). No in-app self-service deletion flow exists yet; mention this honestly in the form (Play Console allows describing an external/manual deletion process).

## Third parties data is shared with
- **Xendit** (payment processing) — transaction amount, reference ID, payer email.
- **Google** (Sign-In only, if the user opts in) — ID token verification; no data flows the other way beyond what Google Sign-In already collects on Google's side.
