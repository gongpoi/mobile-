# Q2  Box Overlay with Badge

## Explanation about the app

- **Layout**: A `Box` contains two children:
  1) a circular avatar image,
  2) a small red badge overlay aligned to the bottom-end (`Alignment.BottomEnd`).
- **State**: A `Boolean` state (`showBadge`) controls whether the badge is visible.
- **Interaction**: A button toggles the badge Show/Hide.
- **Theming**: Uses Material 3 typography for the badge text; the avatar is clipped to a `CircleShape`.

### How it works
- `Box()`  stack children to overlay the badge on the avatar.
- `Modifier.align(Alignment.BottomEnd)`  place the badge at the bottom-right corner of the avatar container.
- `remember { mutableStateOf(true) }`  simple state to toggle visibility.
- `clip(CircleShape)` + `ContentScale.Crop`  circular avatar with proper image scaling.

## How to use the app

1. Place an avatar image in `app/src/main/res/drawable/` and name it (for example) `r.png`.
2. Run on an emulator or a device.
3. Tap Show/Hide Badge to toggle the red notification dot on the avatar.