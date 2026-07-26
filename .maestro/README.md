# Maestro E2E

End-to-end flows for both roles:

- **Student / subscriber** (`role = subscriber`) — catalogue, my-courses, reviews,
  cart, quiz + assignment history, notifications inbox, mentor-chat paywall,
  transactions.
- **Instructor** (`tutor_instructor`) — dashboard, course create,
  wallet/withdrawal, grading, chat.

## Prerequisites

- Android emulator or device with the debug build installed:
  `./gradlew installDebug`
- [Maestro](https://maestro.mobile.dev): `curl -Ls "https://get.maestro.mobile.dev" | bash`
- Seeded accounts reachable at the app's `BASE_URL` (see `database/seeders/UserSeeder.php`
  in the backend repo):
  - student: `siswa1@mulaisekarang.com` / `password`
  - instructor: `budi@mulaisekarang.com` / `password`

**Emulator note:** the host user must be in the `kvm` group or the x86_64 system
image refuses to boot (`sudo gpasswd -a $USER kvm`, then re-login).

## Run

```bash
maestro test .maestro                              # both smoke suites via config.yaml
maestro test .maestro/flows/student-smoke.yaml     # student only
maestro test .maestro/flows/student-write-review.yaml
maestro test --format junit .maestro               # CI-friendly report
```

## How selectors work

`MainActivity` sets `Modifier.semantics { testTagsAsResourceId = true }`, so every
Compose `Modifier.testTag("x")` is matchable in Maestro as `id: "x"`.

Flows that depend on seeded data (an enrolled course, an existing quiz attempt)
wrap the deeper assertions in a conditional `runFlow: when:` block, so they prove
the behaviour when the data is there and still pass on a bare account.

### Student ids

Navigation: `nav_tab_beranda`, `nav_tab_kursus_saya`, `nav_tab_chat`, `nav_tab_akun`,
`course_card`.

Profile menu: `menu_transactions`, `menu_cart`, `menu_notifications`,
`menu_my_reviews`, `menu_quiz_history`, `menu_submission_history`,
`menu_chat_subscription`.

Reviews: `write_review_section`, `review_star_1`…`review_star_5`,
`review_body_input`, `review_submit_button`, `my_review_list`, `my_review_row`.

Cart: `add_to_cart_button`, `buy_now_button`, `cart_list`, `cart_row`,
`cart_remove_{courseId}`, `cart_issue`, `cart_total`, `cart_checkout_button`.

History: `quiz_attempt_list`, `quiz_attempt_row`, `attempt_detail`,
`attempt_detail_{id}`, `submission_list`, `submission_row`,
`submission_filter_all`, `submission_filter_graded`.

Notifications: `notification_list`, `notification_row`,
`notifications_unread_filter`, `notifications_mark_all`.

Chat paywall: `chat_paywall`, `paywall_active`, `paywall_trial_button`,
`paywall_purchase_button`.

### Instructor ids

`email_input`, `password_input`, `login_button`, `instructor_portal_entry`,
`nav_courses`, `nav_wallet`, `nav_grading`, `nav_chat`, `metric_courses`,
`metric_students`, `wallet_balance`, `create_course_button`, `course_title_input`,
`course_price_input`, `status_published`, `course_save_button`, `course_row`,
`delete_course_{id}`, `withdrawal_amount_input`, `withdrawal_account_name`,
`withdrawal_account_number`, `withdrawal_submit`, `submission_row`, `grade_input`,
`grade_save_button`.
