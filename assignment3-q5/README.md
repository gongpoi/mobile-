# Q5 — Themed Form with TextFields and Submit Button

## Explanation about the app
This sample implements a login form using Material 3:
- Two fields: Username and Password (outlined text fields).
- Styling via MaterialTheme colors and typography.
- **Validation** on submit: if any field is empty, show an inline error text.
- **Snackbar** feedback when the form is submitted successfully.




- **Form fields**: Implemented with `OutlinedTextField` to get a clean, accessible look.  
  The fields use the current Material 3 theme (`MaterialTheme.colorScheme` & `typography`).
- **Password visibility**: A simple text-based toggle (`Show/Hide`) avoids extra icon dependencies while keeping behavior clear.
- **Validation**: On Submit, each field is checked; if empty, an error message appears right under that field.
- **Feedback**: On success, the app uses a `Snackbar` (via `SnackbarHostState`) to acknowledge submission.
- **Insets handling**: `Scaffold` provides `innerPadding`, which is applied to the content to avoid any overlap.

## How to use the app

1. Run on an emulator or a device.
2. Type a username and password.
3. Tap Submit:
   - If a field is empty, an error appears beneath that field.
   - If both fields are filled, a Snackbar saying 'submitted' appears.