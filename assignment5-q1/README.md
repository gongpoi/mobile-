## How AI Was Used

- Used ChatGPT (model: *GPT-5 Thinking*) to:
  - Draft initial navigation skeleton (sealed `Routes`, `NavHost`, argument passing)
  - Suggest back stack patterns (`popUpTo`, `launchSingleTop`, `restoreState`)
  - Generate seed recipes and wording for UI text and this README

### Where AI Misunderstood Navigation

- Initially suggested passing entire `Recipe` objects through the route; corrected to pass only `id` and fetch from `ViewModel`.
- Proposed `popUpTo(Detail)` which created odd back behavior; fixed with `popUpTo(startDestination)` then `navigate("detail/{id}")`.
- Omitted `restoreState = true` for bottom navigation; added to preserve list scroll state when switching tabs.
