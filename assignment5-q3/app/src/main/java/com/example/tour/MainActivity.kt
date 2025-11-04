package com.example.tour

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import androidx.compose.runtime.saveable.rememberSaveable
import com.example.tour.ui.theme.TourTheme

//Routes
sealed class Routes(val route: String) {
    data object Home : Routes("home")
    // Structured route:list/{category}
    data object List : Routes("list")
    // Structured route:detail/{category}/{id}  (id 为 Int)
    data object Detail : Routes("detail")
    // Categories destination: categories
    data object Categories : Routes("categories")
}

// Demo
private val demoCategories = listOf("Museums", "Parks", "Restaurants")

// Each category exposes several places; id is Int
private val demoPlaces: Map<String, List<Pair<Int, String>>> = mapOf(
    "Museums" to listOf(1 to "MIT Museum", 2 to "Museum of Science", 3 to "Isabella Stewart Gardner Museum"),
    "Parks" to listOf(10 to "Boston Common", 11 to "Public Garden", 12 to "Charles River Esplanade"),
    "Restaurants" to listOf(20 to "Regina Pizzeria", 21 to "Neptune Oyster", 22 to "Oleana")
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TourTheme {
                App()
            }
        }
    }
}

//App Scaffold + NavHost
@Composable
fun App() {
    val nav = rememberNavController()
    // After clearing back stack to Home, lock the back button
    var homeBackLocked by rememberSaveable { mutableStateOf(false) }

    // Current route
    val currentRoute = nav.currentBackStackEntryAsState().value
        ?.destination?.route?.substringBefore("?")

    // Reusable TopAppBar across all screens
    Scaffold(
        topBar = {
            TopBar(
                title = when (currentRoute) {
                    Routes.Home.route -> "Explore Boston — Home"
                    Routes.Categories.route -> "Categories"
                    Routes.List.route -> "Places"
                    Routes.Detail.route -> "Place Detail"
                    else -> "Explore Boston"
                },
                canNavigateBack = currentRoute != Routes.Home.route,
                onBack = { nav.popBackStack() },
                onHome = {
                    // clearing the stack when going Home
                    nav.navigate(Routes.Home.route) {
                        popUpTo(nav.graph.findStartDestination().id) {
                            inclusive = true
                        }
                        launchSingleTop = true
                    }
                    homeBackLocked = true
                }
            )
        }
    ) { padding ->
        // On Home, disable back after we cleared to Home
        if (currentRoute == Routes.Home.route && homeBackLocked) {
            BackHandler(enabled = true) { /* consume back */}
        }

        AppNavGraph(
            nav = nav,
            modifier = Modifier.padding(padding),
            onClearHomeLock = { homeBackLocked = false } // unlock back once we leave Home
        )
    }
}

// Reusable TopBar
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBar(
    title: String,
    canNavigateBack: Boolean,
    onBack: () -> Unit,
    onHome: () -> Unit
) {
    TopAppBar(
        title = { Text(title, fontWeight = FontWeight.SemiBold) },
        navigationIcon = {
            if (canNavigateBack) {
                IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") }
            }
        },
        actions = {
            IconButton(onClick = onHome) { Icon(Icons.Default.Home, contentDescription = "Home") }
        }
    )
}

// NavGraph
@Composable
fun AppNavGraph(
    nav: NavHostController,
    modifier: Modifier = Modifier,
    onClearHomeLock: () -> Unit
) {
    NavHost(navController = nav, startDestination = Routes.Home.route, modifier = modifier) {
        // Home
        composable(Routes.Home.route) {
            onClearHomeLock() // Entering Home unlocks back button
            HomeScreen(
                onStartTour = {
                    // Demonstrate navigate() + structured route string: to Categories
                    nav.navigate(Routes.Categories.route) {
                        // Top-level push; do not clear stack here
                        launchSingleTop = true
                    }
                }
            )
        }

        // Categories
        composable(Routes.Categories.route) {
            onClearHomeLock()
            CategoriesScreen(
                categories = demoCategories,
                onPick = { category ->
                    // Structured route: list/{category}
                    nav.navigate("${Routes.List.route}/$category") {
                        // No need to clear stack when going back to Categories
                        launchSingleTop = true
                    }
                }
            )
        }

        // List：list/{category}
        composable(
            route = Routes.List.route + "/{category}",
            arguments = listOf(navArgument("category") { type = NavType.StringType })
        ) { back ->
            onClearHomeLock()
            val category = back.arguments?.getString("category") ?: ""
            val places = demoPlaces[category].orEmpty()
            ListScreen(
                category = category,
                places = places,
                onPick = { id ->
                    // Structured route: detail/{category}/{id}
                    nav.navigate("${Routes.Detail.route}/$category/$id") {
                        launchSingleTop = true
                    }
                }
            )
        }

        // Detail: detail/{category}/{id}
        composable(
            route = Routes.Detail.route + "/{category}/{id}",
            arguments = listOf(
                navArgument("category") { type = NavType.StringType },
                navArgument("id") { type = NavType.IntType }
            )
        ) { back ->
            onClearHomeLock()
            val category = back.arguments?.getString("category") ?: ""
            val id = back.arguments?.getInt("id") ?: -1
            val name = demoPlaces[category]?.firstOrNull { it.first == id }?.second ?: "Unknown"
            DetailScreen(
                category = category,
                id = id,
                name = name
            )
        }
    }
}

// Screens
@Composable
fun HomeScreen(onStartTour: () -> Unit) {
    Box(Modifier.fillMaxSize().padding(16.dp)) {
        Column(Modifier.align(Alignment.TopStart), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Explore Boston", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text("Welcome!", style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(16.dp))
            Button(onClick = onStartTour) { Text("Start Tour") }
        }
    }
}

@Composable
fun CategoriesScreen(categories: List<String>, onPick: (String) -> Unit) {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Categories", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        LazyColumn {
            items(categories) { c ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onPick(c) }
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(c, style = MaterialTheme.typography.bodyLarge)
                }
                Divider()
            }
        }
    }
}

@Composable
fun ListScreen(category: String, places: List<Pair<Int, String>>, onPick: (Int) -> Unit) {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("All $category", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        if (places.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("No places.") }
        } else {
            LazyColumn {
                items(places) { (id, name) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onPick(id) }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("#$id  $name", style = MaterialTheme.typography.bodyLarge)
                    }
                    Divider()
                }
            }
        }
    }
}

@Composable
fun DetailScreen(category: String, id: Int, name: String) {
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("Category: $category", style = MaterialTheme.typography.bodyLarge)
        Text("Location ID: $id", style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(12.dp))
        Text(
            "some details",
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
