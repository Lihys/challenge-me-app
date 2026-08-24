package com.course.challengeme

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.course.challengeme.ui.theme.ChallengeMeTheme
import com.course.challengeme.navigations.NavigationHost


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ChallengeMeTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    NavigationHost()
                }
            }
        }
    }
}

