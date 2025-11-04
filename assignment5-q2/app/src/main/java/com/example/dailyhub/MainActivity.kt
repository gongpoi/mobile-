package com.example.dailyhub

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.BottomNavigation
import androidx.compose.material.BottomNavigationItem
import androidx.compose.material.Icon
import androidx.compose.material.Text as M2Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.List
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
import androidx.compose.animation.togetherWith
import com.example.dailyhub.ui.theme.DailyHubTheme

// -------------------- Sealed Routes --------------------
sealed class Routes(val route: String) {
    // Three top-level destinations; use ?anim=... to trigger in-screen animation
    data object Notes : Routes("notes")
    data object Tasks : Routes("tasks")
    data object Calendar : Routes("calendar")
}

//Data / ViewModels
class NotesViewModel : ViewModel() {
    var notes by mutableStateOf(listOf("Welcome to My Daily Hub", "Add some notes…"))
        private set

    fun addNote(text: String) {
        if (text.isNotBlank()) notes = notes + text.trim()
    }
}

data class TaskItem(val id: Int, val text: String, val done: Boolean = false)

class TasksViewModel : ViewModel() {
    var tasks by mutableStateOf(
        listOf(
            TaskItem(1, "Read 10 pages"),
            TaskItem(2, "Workout 20 min", done = true),
            TaskItem(3, "Plan tomorrow")
        )
    )
        private set

    fun toggle(id: Int) {
        tasks = tasks.map { if (it.id == id) it.copy(done = !it.done) else it }
    }

    fun add(text: String) {
        if (text.isBlank()) return
        val next = (tasks.maxOfOrNull { it.id } ?: 0) + 1
        tasks = tasks + TaskItem(next, text.trim())
    }
}

//Activity
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DailyHubTheme {
                App()
            }
        }
    }
}

//  App + NavHost
@Composable
fun App(
    notesVM: NotesViewModel = viewModel(),
    tasksVM: TasksViewModel = viewModel()
) {
    val nav = rememberNavController()
    val items = listOf(
        BottomItem("Notes", Routes.Notes.route, Icons.Default.List),
        BottomItem("Tasks", Routes.Tasks.route, Icons.Default.List),
        BottomItem("Calendar", Routes.Calendar.route, Icons.Default.List),
    )

    Scaffold(
        bottomBar = { BottomBar(nav, items) }
    ) { padding ->
        NavHost(
            navController = nav,
            startDestination = Routes.Notes.route,
            modifier = Modifier.padding(padding)
        ) {
            composable(
                route = Routes.Notes.route + "?anim={anim}",
                arguments = listOf(navArgument("anim") { type = NavType.StringType; defaultValue = "" })
            ) { back ->
                val anim = back.arguments?.getString("anim").orEmpty()
                NotesScreen(notesVM, anim)
            }
            composable(
                route = Routes.Tasks.route + "?anim={anim}",
                arguments = listOf(navArgument("anim") { type = NavType.StringType; defaultValue = "" })
            ) { back ->
                val anim = back.arguments?.getString("anim").orEmpty()
                TasksScreen(tasksVM, anim)
            }
            composable(
                route = Routes.Calendar.route + "?anim={anim}",
                arguments = listOf(navArgument("anim") { type = NavType.StringType; defaultValue = "" })
            ) { back ->
                val anim = back.arguments?.getString("anim").orEmpty()
                CalendarScreen(anim)
            }
        }
    }
}

//Bottom Navigation
data class BottomItem(val label: String, val route: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)

@Composable
fun BottomBar(nav: NavHostController, items: List<BottomItem>) {
    val backEntry by nav.currentBackStackEntryAsState()
    val currentRoute = backEntry?.destination?.route?.substringBefore("?") // ignore

    BottomNavigation {
        items.forEach { item ->
            BottomNavigationItem(
                selected = currentRoute == item.route,
                onClick = {
                    //  append argument to trigger in-screen fade animation
                    nav.navigate("${item.route}?anim=fade") {
                        popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true           // prevent multiple instances of the same destination
                        restoreState = true              // // restore saved state for each destination
                    }
                },
                icon = { Icon(item.icon, contentDescription = item.label) },
                label = { M2Text(item.label) }
            )
        }
    }
}

//  Screens
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun NotesScreen(vm: NotesViewModel, anim: String) {
    AnimatedContent(
        targetState = anim == "fade",
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "NotesFade"
    ) {
        Column(
            Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Notes", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            var input by remember { mutableStateOf("") }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    label = { Text("New note") },
                    modifier = Modifier.weight(1f)
                )
                Button(onClick = { vm.addNote(input); input = "" }) { Text("Add") }
            }
            Divider()
            if (vm.notes.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("No notes yet.") }
            } else {
                LazyColumn {
                    items(vm.notes) { n ->
                        Text("• $n", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(vertical = 6.dp))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun TasksScreen(vm: TasksViewModel, anim: String) {
    AnimatedContent(
        targetState = anim == "fade",
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "TasksFade"
    ) {
        Column(
            Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Tasks", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            var input by remember { mutableStateOf("") }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    label = { Text("New task") },
                    modifier = Modifier.weight(1f)
                )
                Button(onClick = { vm.add(input); input = "" }) { Text("Add") }
            }
            Divider()
            if (vm.tasks.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("No tasks yet.") }
            } else {
                LazyColumn {
                    items(vm.tasks, key = { it.id }) { t ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { vm.toggle(t.id) }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(checked = t.done, onCheckedChange = { vm.toggle(t.id) })
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = t.text + if (t.done) " (done)" else "",
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                        Divider()
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun CalendarScreen(anim: String) {
    AnimatedContent(
        targetState = anim == "fade",
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "CalendarFade"
    ) {
        Box(Modifier.fillMaxSize().padding(16.dp)) {
            Column(Modifier.align(Alignment.TopStart), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Calendar", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text("None", style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}