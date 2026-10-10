package com.example.usagelimit.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@OptIn(ExperimentalMaterial3Api::class)

/**
 * Màn hình chính điều hướng dạng Tab:
 * - 0: Thống Kê (FirstPage)
 * - 1: Giới hạn (SecondPage)
 * Sử dụng State Recomposition để chuyển đổi nội dung.
 */
@Composable
fun HomeScreen() {
    // State lưu tab hiện tại; dùng remember để giữ giá trị khi UI recompose
    var selectedTab by remember { mutableIntStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.primary,
                ),
                title = {
                    Text(if (selectedTab == 0) "Thống Kê" else "Giới Hạn")
                }
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.DateRange, contentDescription = "Trang 1") },
                    label = { Text("Thống Kê") }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.AddCircle, contentDescription = "Trang 2") },
                    label = { Text("Giới Hạn") }
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Đổi giao diện hiển thị tùy theo tab được chọn
            when (selectedTab) {
                0 -> FirstPage()
                1 -> SecondPage()
            }
        }
    }
}

@Composable
fun FirstPage() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text("Thống kê", style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
fun SecondPage() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text("Giới hạn", style = MaterialTheme.typography.titleMedium)
    }
}