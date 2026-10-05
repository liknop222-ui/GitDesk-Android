package com.gitdesk.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gitdesk.app.ui.BuildScreen
import com.gitdesk.app.ui.Gd
import com.gitdesk.app.ui.GdTheme
import com.gitdesk.app.ui.LoginScreen
import com.gitdesk.app.ui.MirrorScreen
import com.gitdesk.app.ui.OverviewScreen
import com.gitdesk.app.ui.PillShape
import com.gitdesk.app.ui.PushScreen
import com.gitdesk.app.ui.SettingsScreen

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val vm: AppViewModel = viewModel()
            GdTheme(theme = vm.theme) {
                LaunchedEffect(Unit) { vm.boot() }

                val snackbar = remember { SnackbarHostState() }
                LaunchedEffect(vm.toast) {
                    val msg = vm.toast
                    if (msg.isNotBlank()) {
                        snackbar.showSnackbar(msg)
                        vm.say("")
                    }
                }

                Box(
                    Modifier
                        .fillMaxSize()
                        .background(Gd.c.paper)
                ) {
                    if (vm.user == null) {
                        LoginScreen(vm)
                    } else {
                        MainShell(vm)
                    }
                    SnackbarHost(
                        hostState = snackbar,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .navigationBarsPadding()
                            .padding(16.dp)
                    )
                }
            }
        }
    }
}

private val TAB_TITLES = listOf("概览", "推送", "编译", "加速", "设置")

@Composable
private fun MainShell(vm: AppViewModel) {
    var tab by rememberSaveable { mutableStateOf(0) }

    Column(
        Modifier
            .fillMaxSize()
            .background(Gd.c.paper)
            .statusBarsPadding()
    ) {
        Box(Modifier.weight(1f)) {
            when (tab) {
                0 -> OverviewScreen(vm) { tab = it }
                1 -> PushScreen(vm)
                2 -> BuildScreen(vm)
                3 -> MirrorScreen(vm)
                else -> SettingsScreen(vm)
            }
        }
        BottomBar(tab) { tab = it }
    }
}

@Composable
private fun BottomBar(current: Int, onSelect: (Int) -> Unit) {
    val c = Gd.c
    Column(
        Modifier
            .fillMaxWidth()
            .background(c.paper)
            .navigationBarsPadding()
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(c.hair)
        )
        Row(
            Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TAB_TITLES.forEachIndexed { index, label ->
                val selected = index == current
                Box(
                    Modifier
                        .weight(1f)
                        .clip(PillShape)
                        .clickable { onSelect(index) }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        label,
                        color = if (selected) c.text1 else c.text3,
                        fontSize = 13.sp,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                    )
                }
            }
        }
    }
}
