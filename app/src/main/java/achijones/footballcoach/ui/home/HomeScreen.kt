package achijones.footballcoach.ui.home

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import achijones.footballcoach.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToMain: () -> Unit,
    viewModel: HomeViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(state.navigateToMain) {
        if (state.navigateToMain) {
            viewModel.consumeNavigateToMain()
            onNavigateToMain()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                ),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Spacer(Modifier.height(16.dp))
            Image(
                painter = painterResource(R.drawable.main_menu_logo),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .height(160.dp),
            )
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = viewModel::startNewLeague,
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.loading,
            ) { Text("New Game") }
            Button(
                onClick = viewModel::openLoadDialog,
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.loading,
            ) { Text("Load Game") }
            OutlinedButton(
                onClick = {
                    context.startActivity(
                        Intent(Intent.ACTION_VIEW, Uri.parse("http://m.reddit.com/r/FootballCoach"))
                    )
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Subreddit") }
            OutlinedButton(
                onClick = {
                    openStoreOrWeb(context, "io.coachapps.collegebasketballcoach")
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Hoops Coach") }
            OutlinedButton(
                onClick = {
                    openStoreOrWeb(context, "com.achijones.profootballcoach")
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Pro Football Coach") }
            if (state.loading) {
                Spacer(Modifier.height(16.dp))
                CircularProgressIndicator()
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    if (state.showLoadDialog) {
        AlertDialog(
            onDismissRequest = viewModel::dismissLoadDialog,
            title = { Text("Choose File to Load:") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    state.saveSlotInfos.forEachIndexed { index, info ->
                        TextButton(
                            onClick = { viewModel.loadSlot(index) },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                text = "${index + 1}. $info",
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Start,
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = viewModel::dismissLoadDialog) { Text("Cancel") }
            },
        )
    }

    state.errorMessage?.let { msg ->
        AlertDialog(
            onDismissRequest = viewModel::clearError,
            title = { Text("Error") },
            text = { Text(msg) },
            confirmButton = {
                TextButton(onClick = viewModel::clearError) { Text("OK") }
            },
        )
    }
}

private fun openStoreOrWeb(context: android.content.Context, packageId: String) {
    try {
        context.startActivity(
            Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageId"))
        )
    } catch (_: ActivityNotFoundException) {
        context.startActivity(
            Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://play.google.com/store/apps/details?id=$packageId"),
            )
        )
    }
}
