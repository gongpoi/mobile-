package com.example.dinner

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.dinner.ui.theme.DinnerTheme

// Navigation routes
sealed class Routes(val route: String) {
    data object Home : Routes("home")
    data object Detail : Routes("detail")
    data object Add : Routes("add")
    data object Settings : Routes("settings")
}


data class Recipe(
    val id: String,
    val title: String,
    val ingredients: String,
    val steps: String
)

// In-memory ViewModel
class RecipeViewModel : ViewModel() {
    private var _recipes by mutableStateOf(
        listOf(
            Recipe(
                id = "1",
                title = "Pasta Aglio e Olio",
                ingredients = "Spaghetti\nGarlic\nOlive oil\nChili flakes\nParsley\nSalt",
                steps = "1. Boil pasta.\n2. Sauté garlic in oil.\n3. Toss with chili, parsley, pasta."
            ),
            Recipe(
                id = "2",
                title = "Avocado Toast",
                ingredients = "Bread\nAvocado\nLemon\nSalt & Pepper",
                steps = "1. Toast bread.\n2. Mash avocado with lemon.\n3. Spread and season."
            )
        )
    )
    val recipes: List<Recipe> get() = _recipes

    fun getById(id: String): Recipe? = _recipes.find { it.id == id }

    fun addRecipe(title: String, ingredients: String, steps: String): String {
        val newId = ((_recipes.maxOfOrNull { it.id.toIntOrNull() ?: 0 } ?: 0) + 1).toString()
        _recipes = _recipes + Recipe(newId, title.trim(), ingredients.trim(), steps.trim())
        return newId
    }
}

//  Activity
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { DinnerTheme { App() } }
    }
}

// Scaffold + NavHost
@Composable
fun App(vm: RecipeViewModel = viewModel()) {
    val nav = rememberNavController()
    val bottomItems = listOf(
        BottomItem("Home", Routes.Home.route) { Icon(Icons.Default.Home, contentDescription = "Home") },
        BottomItem("Add", Routes.Add.route) { Icon(Icons.Default.Add, contentDescription = "Add") },
        BottomItem("Settings", Routes.Settings.route) { Icon(Icons.Default.Settings, contentDescription = "Settings") },
    )

    Scaffold(
        bottomBar = { BottomBar(nav, bottomItems) }
    ) { padding ->
        NavHost(
            navController = nav,
            startDestination = Routes.Home.route,
            modifier = Modifier.padding(padding)
        ) {
            // Home: list of recipes
            composable(Routes.Home.route) {
                HomeScreen(
                    recipes = vm.recipes,
                    onClick = { r -> nav.navigate("${Routes.Detail.route}/${r.id}") }
                )
            }

            // Detail with argument extracted from back stack
            composable(
                route = Routes.Detail.route + "/{id}",
                arguments = listOf(navArgument("id") { type = NavType.StringType })
            ) { backStackEntry ->
                val id = backStackEntry.arguments?.getString("id")
                val recipe = id?.let { vm.getById(it) }
                DetailScreen(recipe = recipe) { nav.popBackStack() }
            }

            // Add new recipe
            composable(Routes.Add.route) {
                AddRecipeScreen(
                    onSave = { title, ingredients, steps ->
                        val newId = vm.addRecipe(title, ingredients, steps)
                        // Go to Detail(newId) and clean Add from stack, keep single top
                        nav.navigate("${Routes.Detail.route}/$newId") {
                            popUpTo(nav.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onCancel = { nav.popBackStack() }
                )
            }

            // Settings: placeholder
            composable(Routes.Settings.route) {
                SettingsScreen()
            }
        }
    }
}

// Bottom Navigation
// Ensures a single instance of each root destination.
data class BottomItem(
    val label: String,
    val route: String,
    val icon: @Composable () -> Unit
)

@Composable
fun BottomBar(nav: NavHostController, items: List<BottomItem>) {
    val navBackStackEntry by nav.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    NavigationBar {
        items.forEach { item ->
            NavigationBarItem(
                selected = currentRoute == item.route,
                onClick = {
                    nav.navigate(item.route) {
                        popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = item.icon,
                label = { Text(item.label) }
            )
        }
    }
}

//Screens
// Home list with LazyColumn
@Composable
fun HomeScreen(recipes: List<Recipe>, onClick: (Recipe) -> Unit) {
    Column(Modifier.fillMaxSize()) {
        Text(
            text = "Recipes",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(16.dp)
        )
        if (recipes.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No recipes yet. Tap Add to create one.")
            }
        } else {
            LazyColumn(Modifier.fillMaxSize()) {
                items(recipes) { r ->
                    RecipeRow(r) { onClick(r) }
                    Divider()
                }
            }
        }
    }
}

// Single row item used in Home list
@Composable
fun RecipeRow(r: Recipe, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text(r.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(4.dp))
        Text(r.ingredients.lines().joinToString(limit = 1, truncated = "…"), style = MaterialTheme.typography.bodyMedium)
    }
}
// Detail screen that renders full recipe content
@Composable
fun DetailScreen(recipe: Recipe?, onBack: () -> Unit) {
    if (recipe == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Recipe not found") }
        return
    }
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text(recipe.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        Text("Ingredients", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(6.dp))
        Text(recipe.ingredients)
        Spacer(Modifier.height(12.dp))
        Text("Steps", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(6.dp))
        Text(recipe.steps)
        Spacer(Modifier.height(24.dp))
        Button(onClick = onBack) { Text("Back") }
    }
}
// Add Recipe screen
@Composable
fun AddRecipeScreen(onSave: (String, String, String) -> Unit, onCancel: () -> Unit) {
    var title by remember { mutableStateOf("") }
    var ingredients by remember { mutableStateOf("") }
    var steps by remember { mutableStateOf("") }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Add Recipe", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("Title") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = ingredients,
            onValueChange = { ingredients = it },
            label = { Text("Ingredients (one per line)") },
            modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp)
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = steps,
            onValueChange = { steps = it },
            label = { Text("Steps (one per line)") },
            modifier = Modifier.fillMaxWidth().heightIn(min = 160.dp)
        )
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                enabled = title.isNotBlank(),
                onClick = { onSave(title, ingredients, steps) }
            ) { Text("Save") }
            OutlinedButton(onClick = onCancel) { Text("Cancel") }
        }
    }
}

@Composable
fun SettingsScreen() {
    Column(
        Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("Settings", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("None")
    }
}