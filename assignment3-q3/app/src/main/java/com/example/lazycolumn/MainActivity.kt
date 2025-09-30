package com.example.lazycolumn

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.background
import androidx.compose.ui.Alignment
import androidx.compose.foundation.shape.CircleShape
import java.util.Locale
import com.example.lazycolumn.ui.theme.LazyColumnTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LazyColumnTheme {
                ContactsApp()
            }
        }
    }
}

@Composable
fun ContactsApp() {
    // List scroll state
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    // Show the FAB only when scrolled beyond the 10th item
    val showFab by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex > 10 ||
                    (listState.firstVisibleItemIndex == 10 && listState.firstVisibleItemScrollOffset > 0)
        }
    }
    // Generate at least 50 contacts
    val contacts = remember { generateSampleContacts(100) }
    // Group by first letter and sort the keys
    val grouped = remember(contacts) {
        contacts.groupBy { it.name.first().uppercaseChar() }.toSortedMap()
    }

    Scaffold(
        // FAB appears conditionally and scrolls to top when clicked
        floatingActionButton = {
            if (showFab) {
                FloatingActionButton(
                    onClick = {
                        scope.launch {
                            // Smooth scroll to the first item
                            listState.animateScrollToItem(0)
                        }
                    }
                ) {
                    Text("Top")
                }
            }
        }
    ) { innerPadding ->
        // Main list content with proper padding from Scaffold
        ContactsList(
            grouped = grouped,
            listState = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        )
    }
}

@OptIn(ExperimentalFoundationApi::class) // stickyHeader is marked experimental in Foundation
@Composable
fun ContactsList(
    grouped: Map<Char, List<Contact>>,
    listState: LazyListState,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        state = listState,
        modifier = modifier
    ) {
        // For each letter group, show a sticky header and its contacts
        grouped.forEach { (letter, people) ->
            stickyHeader {
                Header(letter)
            }
            items(
                items = people,
                key = { it.id }
            ) { contact ->
                ContactRow(contact)
            }
        }
        // Extra bottom space so the last rows won't be covered by the FAB
        item { Spacer(Modifier.height(80.dp)) }
    }
}

@Composable
private fun Header(letter: Char) {
    Surface(
        color = Color(0xFFEFEFEF),
        shadowElevation = 2.dp
    ) {
        Text(
            text = letter.toString(),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            fontWeight = FontWeight.Bold,
            color = Color(0xFF333333)
        )
    }
}

@Composable
private fun ContactRow(contact: Contact) {

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Simple circular initial "avatar" using the first letter of the name
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(Color(0xFFBBDEFB), shape = CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = contact.name.first().uppercaseChar().toString(),
                fontWeight = FontWeight.SemiBold
            )
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(text = contact.name, fontWeight = FontWeight.Medium)
        }
    }
}

data class Contact(val id: String, val name: String)

//Generate N sample contacts by combining first and last names.
//The list is then sorted by name to help create balanced A–Z groups.
//Names are generated by AI.

private fun generateSampleContacts(n: Int = 100): List<Contact> {
    val firstNames = listOf(
        "Alice","Ben","Carol","David","Emma","Frank","Grace","Helen","Ivan","Julia",
        "Kevin","Lily","Mason","Nina","Oscar","Paul","Queen","Ryan","Sara","Tom",
        "Uma","Vince","Wendy","Xavier","Yara","Zack"
    )
    val lastNames = listOf(
        "Anderson","Brown","Clark","Davis","Evans","Foster","Garcia","Harris","Irwin","Johnson",
        "King","Lopez","Miller","Nelson","Owens","Parker","Quinn","Roberts","Smith","Taylor",
        "Underwood","Vargas","White","Xu","Young","Zimmer"
    )

    val list = mutableListOf<Contact>()
    var idx = 0
    while (list.size < n) {
        val fn = firstNames[idx % firstNames.size]
        val ln = lastNames[(idx / firstNames.size) % lastNames.size]
        val name = "$fn $ln"

        list += Contact(id = "c$idx", name = name)
        idx++
    }
    // Sort by name so group headers (A–Z) appeard
    return list.sortedBy { it.name.lowercase(Locale.getDefault()) }
}

@Preview(showBackground = true)
@Composable
fun ContactsPreview() {
    LazyColumnTheme {
        ContactsApp()
    }
}