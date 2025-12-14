package com.abdat.clipwhisper

import android.Manifest
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.abdat.clipwhisper.clipboard.presentation.ClipboardScreen
import com.abdat.clipwhisper.clipboard.presentation.ClipboardViewModel
import org.koin.compose.viewmodel.koinViewModel


class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val viewModel: ClipboardViewModel = koinViewModel()
            val lifecycleOwner = LocalLifecycleOwner.current

            // Auto-fetch clipboard when app starts
            LaunchedEffect(Unit) {
                viewModel.autoFetchClipboard()
            }

            // Auto-fetch clipboard when app comes to foreground
            DisposableEffect(lifecycleOwner) {
                val observer = LifecycleEventObserver { _, event ->
                    when (event) {
                        Lifecycle.Event.ON_RESUME -> {
                            // App came to foreground, fetch clipboard
                            viewModel.autoFetchClipboard()
                        }
                        Lifecycle.Event.ON_DESTROY -> {
                            viewModel.onClear()
                        }
                        else -> {}
                    }
                }

                lifecycleOwner.lifecycle.addObserver(observer)

                onDispose {
                    lifecycleOwner.lifecycle.removeObserver(observer)
                }
            }

            MaterialTheme {
                Surface {
                    ClipboardScreen(viewModel)
                }
            }
        }
    }
}


@Preview
@Composable
fun AppAndroidPreview() {
    App()
}