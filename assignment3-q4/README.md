# Q4  Scaffold with TopAppBar, BottomBar, and FAB

## Explanation about the app
This sample demonstrates a Material 3 `Scaffold` with:
- A **TopAppBar** that shows the app title.
- A **Bottom navigation bar** with three text items: Home, Settings, Profile.
- A **Floating Action Button (FAB)** that triggers a Snackbar when clicked.
- Proper application of `innerPadding` to ensure content doesn't overlap the bars or FAB.


### How it works
- **Scaffold layout**: The `Scaffold` arranges the screen's top bar, bottom bar, body content, FAB, and snackbar.
- **Top bar**: Implemented with `CenterAlignedTopAppBar` to display the title.
- **Bottom bar**: Implemented with `NavigationBar` + `NavigationBarItem` (text-only items for simplicity).
- **FAB + Snackbar**: Clicking the FAB calls `snackbarHostState.showSnackbar()` within a coroutine (`rememberCoroutineScope()`).


## How to use the app
1. Run on an emulator or a device.
2. Tap different bottom bar items (Home/Settings/Profile): the main content updates to show the current tab.
3. Press the FAB (`+`): a Snackbar appears at the bottom with the current tab name.