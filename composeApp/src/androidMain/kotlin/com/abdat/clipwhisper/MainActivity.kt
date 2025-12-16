package com.abdat.clipwhisper

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.abdat.clipwhisper.core.presentation.ui.theme.AppTheme


class MainActivity : ComponentActivity() {

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            // Handle permission denied
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionLauncher.launch(Manifest.permission.NEARBY_WIFI_DEVICES)
        }
        /* setContent {
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
         }*/
        setContent {
            AppTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    App()
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