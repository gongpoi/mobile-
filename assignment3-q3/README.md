# Q3  LazyColumn with Sticky Headers


## Explanation about the app
This app demonstrates an alphabetically grouped contact list using Jetpack Compose:
- Uses `LazyColumn` with `stickyHeader` so the current letter stays visible while scrolling.
- Provides >= 50 contacts (100 generated) as sample data.
- Shows a "Scroll to Top" FAB only after scrolling past item #10.
- The FAB uses `animateScrollToItem()` with coroutines for smooth scrolling.


### How it works
- **Grouping**: Contacts are generated and grouped by the first letter of their names.  
  `contacts.groupBy { it.name.first().uppercaseChar() }.toSortedMap()` ensures A-Z ordering.
- **Sticky headers**: Each group has a header that sticks at the top using `stickyHeader`.
- **FAB visibility**: `derivedStateOf { listState.firstVisibleItemIndex > 10 }` controls whether to show the FAB.
- **Smooth scroll**: `rememberCoroutineScope()` + `listState.animateScrollToItem(0)` are used to scroll back to the top.

## How to use the app
1. Run on an emulator or a device.
2. Scroll through the contact list:
   - The letter header stays at the top while you scroll within its section.
   - After you pass item #10, a "Top" FAB appears on the screen.
3. Tap the FAB to smoothly scroll back to the top.