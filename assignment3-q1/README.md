# Q1  Row & Column Weight Split Layout

## Explanation about the app

- **Goal**: Create a screen where the top-level **Row** is split into two sections:
  - Left: **25% width**
  - Right: **75% width**
- Inside the right section, a Column contains three children that share the available height using weights in the ratio **2:3:5**.
- Distinct background **colors** and **text labels** make it easy to see the space usage:
  - Left block (25%)  Yellow: `Pokemon`
  - Right/Top (2 parts)  Green: `Bulbasaur`
  - Right/Middle (3 parts)  Blue: `Squirtle`
  - Right/Bottom (5 parts)  Red: `Charmander`

### How it works

- In a **Row**, `Modifier.weight(x)` divides width among siblings.
  - We use `0.25f` and `0.75f` to achieve 25% / 75%.
- In a **Column**, `Modifier.weight(x)` divides height among siblings.
  - We use `2f`, `3f`, and `5f` (total 10) to achieve 2:3:5.
- `fillMaxHeight()`/`fillMaxWidth()` ensure each block expands fully in the non-weighted dimension.

## How to use the app

1. Run on an emulator or a device.
2. You should see:
   - A yellow left panel taking ~25% width.
   - A green / blue / red column on the right, whose heights follow 2:3:5.
