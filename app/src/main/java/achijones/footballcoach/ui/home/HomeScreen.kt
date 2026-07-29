package achijones.footballcoach.ui.home

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import achijones.footballcoach.save.SlotStatus
import achijones.footballcoach.ui.util.SaveExportShare
import achijones.footballcoach.ui.util.SaveSlots

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToMain: () -> Unit,
    viewModel: HomeViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri == null) {
            viewModel.dismissImport()
            return@rememberLauncherForActivityResult
        }
        try {
            val json = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
            if (json != null) {
                viewModel.onImportJsonRead(json)
            } else {
                viewModel.dismissImport()
            }
        } catch (_: Exception) {
            viewModel.dismissImport()
        }
    }

    LaunchedEffect(state.navigateToMain) {
        if (state.navigateToMain) {
            viewModel.consumeNavigateToMain()
            onNavigateToMain()
        }
    }

    LaunchedEffect(state.showImportPicker) {
        if (state.showImportPicker) {
            importLauncher.launch(arrayOf("application/json", "text/*", "*/*"))
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
            Spacer(modifier = Modifier.height(16.dp))
            Image(
                painter = painterResource(R.drawable.main_menu_logo),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .height(160.dp),
            )
            Spacer(modifier = Modifier.height(8.dp))
            state.resumeSlot?.let { resume ->
                Button(
                    onClick = viewModel::resumeCareer,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !state.loading,
                ) { Text("Resume: ${resume.summary}") }
            }
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
                Spacer(modifier = Modifier.height(16.dp))
                CircularProgressIndicator()
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    if (state.showLoadDialog) {
        AlertDialog(
            onDismissRequest = viewModel::dismissLoadDialog,
            title = { Text("Choose File to Load:") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    state.saveSlots.forEach { info ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            TextButton(
                                onClick = { viewModel.loadSlot(info.index) },
                                enabled = SaveSlots.canLoad(info),
                                modifier = Modifier.weight(1f),
                            ) {
                                Text(
                                    text = SaveSlots.label(info),
                                    modifier = Modifier.fillMaxWidth(),
                                    textAlign = TextAlign.Start,
                                )
                            }
                            if (info.status == SlotStatus.OK) {
                                TextButton(onClick = {
                                    viewModel.exportSlot(info.index) { json ->
                                        SaveExportShare.shareJson(context, info.index, json)
                                    }
                                }) { Text("Export") }
                            }
                            if (info.status != SlotStatus.EMPTY) {
                                TextButton(onClick = { viewModel.requestDeleteSlot(info.index) }) {
                                    Text("Del")
                                }
                            }
                            TextButton(onClick = { viewModel.beginImportToSlot(info.index) }) {
                                Text("Import")
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = viewModel::dismissLoadDialog) { Text("Cancel") }
            },
        )
    }

    state.confirmDeleteSlot?.let { index ->
        AlertDialog(
            onDismissRequest = viewModel::dismissDeleteConfirm,
            title = { Text("Delete slot ${index + 1}?") },
            text = { Text("This permanently removes the career in this slot.") },
            confirmButton = {
                TextButton(onClick = viewModel::confirmDeleteSlot) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissDeleteConfirm) { Text("Cancel") }
            },
        )
    }

    if (state.confirmImportOverwrite) {
        AlertDialog(
            onDismissRequest = viewModel::dismissImport,
            title = { Text("Overwrite slot?") },
            text = { Text("Import will replace the save in slot ${(state.importTargetSlot ?: 0) + 1}.") },
            confirmButton = {
                TextButton(onClick = viewModel::confirmImportOverwrite) { Text("Import") }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissImport) { Text("Cancel") }
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
