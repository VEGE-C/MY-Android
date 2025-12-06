package com.example.vege

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.vege.ui.theme.VEGETheme
import androidx.compose.material3.Text

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            VEGETheme {
                var selectedTab by remember { mutableStateOf(1) }
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        NavigationBar {
                            NavigationBarItem(
                                selected = selectedTab == 0,
                                onClick = { selectedTab = 0 },
                                label = { Text("TODO") },
                                icon = {}
                            )
                            NavigationBarItem(
                                selected = selectedTab == 1,
                                onClick = { selectedTab = 1 },
                                label = { Text("查询成绩") },
                                icon = {}
                            )
                            NavigationBarItem(
                                selected = selectedTab == 2,
                                onClick = { selectedTab = 2 },
                                label = { Text("次数记录") },
                                icon = {}
                            )
                            NavigationBarItem(
                                selected = selectedTab == 3,
                                onClick = { selectedTab = 3 },
                                label = { Text("我的") },
                                icon = {}
                            )
                        }
                    }
                ) { innerPadding ->
                    when (selectedTab) {
                        0 -> TodoScreen(modifier = Modifier.padding(innerPadding))
                        1 -> GradeQueryScreen(modifier = Modifier.padding(innerPadding))
                        2 -> CountLogScreen(modifier = Modifier.padding(innerPadding))
                        else -> ProfileScreen(modifier = Modifier.padding(innerPadding))
                    }
                }
            }
        }
    }
}

 
 
