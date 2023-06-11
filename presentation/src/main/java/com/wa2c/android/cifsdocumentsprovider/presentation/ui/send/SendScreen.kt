package com.wa2c.android.cifsdocumentsprovider.presentation.ui.send

import android.content.res.Configuration
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wa2c.android.cifsdocumentsprovider.common.utils.fileName
import com.wa2c.android.cifsdocumentsprovider.common.values.SendDataState
import com.wa2c.android.cifsdocumentsprovider.domain.model.SendData
import com.wa2c.android.cifsdocumentsprovider.presentation.R
import com.wa2c.android.cifsdocumentsprovider.presentation.ext.getSummaryText
import com.wa2c.android.cifsdocumentsprovider.presentation.ui.common.AppSnackbar
import com.wa2c.android.cifsdocumentsprovider.presentation.ui.common.AppTopAppBarColors
import com.wa2c.android.cifsdocumentsprovider.presentation.ui.common.CommonDialog
import com.wa2c.android.cifsdocumentsprovider.presentation.ui.common.DialogButton
import com.wa2c.android.cifsdocumentsprovider.presentation.ui.common.DividerNormal
import com.wa2c.android.cifsdocumentsprovider.presentation.ui.common.DividerThin
import com.wa2c.android.cifsdocumentsprovider.presentation.ui.common.MessageSnackbarVisual
import com.wa2c.android.cifsdocumentsprovider.presentation.ui.common.Theme

@Composable
fun SendScreen(
    viewModel: SendViewModel = hiltViewModel(),
    lifecycleOwner: LifecycleOwner = LocalLifecycleOwner.current,
    uriList: List<Uri>,
    onNavigateFinish: () -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val sendDataList = viewModel.sendDataList.collectAsStateWithLifecycle()
    val showFinishDialog = remember { mutableStateOf(false) }
    val isFileBrowserShown = remember { mutableStateOf(false) }

    /** Single URI result launcher */
    val singleUriLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("*/*")) { uri ->
        isFileBrowserShown.value = false
        if (uri == null) {
            onNavigateFinish()
            return@rememberLauncherForActivityResult
        }

        val source = uriList.firstOrNull() ?: run {
            onNavigateFinish()
            return@rememberLauncherForActivityResult
        }

        viewModel.sendUri(listOf(source), uri)
    }

    /** Multiple URI result launcher */
    val multipleUriLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        isFileBrowserShown.value = false
        if (uri == null) {
            onNavigateFinish()
            return@rememberLauncherForActivityResult
        }

        val source = uriList.toList().ifEmpty {
            onNavigateFinish()
            return@rememberLauncherForActivityResult
        }

        viewModel.sendUri(source, uri)
    }


    if (uriList.isNotEmpty() && !isFileBrowserShown.value) {
        when {
            uriList.size == 1 -> {
                // Single
                SideEffect {
                    singleUriLauncher.launch(uriList.first().fileName)
                    isFileBrowserShown.value = true
                }
            }
            uriList.size > 1 -> {
                // Multiple
                SideEffect {
                    multipleUriLauncher.launch(uriList.first())
                    isFileBrowserShown.value = false
                }
            }
        }
    }

    SendScreenContainer(
        snackbarHostState = snackbarHostState,
        sendDataList = sendDataList.value,
        onClickCancel = { viewModel.onClickCancel(it) },
        onClickRetry = { viewModel.onClickRetry(it) },
        onClickRemove = { viewModel.onClickRemove(it) },
        onClickCancelAll = { viewModel.onClickCancelAll() },
        onClickClose = { showFinishDialog.value = true },
    )

    if (showFinishDialog.value) {
        CommonDialog(
            confirmButtons = listOf(
                DialogButton(label = stringResource(id = R.string.dialog_accept)) {
                    onNavigateFinish()
                },
            ),
            dismissButton = DialogButton(label = stringResource(id = R.string.dialog_close)) {
                showFinishDialog.value = false
            },
            onDismiss = { showFinishDialog.value = false }
        ) {
            Text(stringResource(id = R.string.send_exit_confirmation_message))
        }
    }

}

//@Composable
//fun ReceiveUri(
//    viewModel: SendViewModel,
//    onNavigateFinish: () -> Unit,
//    onSendUri: (sourceUris: List<Uri>, targetUri: Uri) -> Unit,
//) {
//    val uriList = viewModel.receivedUriList.collectAsStateWithLifecycle().value
//
//    /** Single URI result launcher */
//    val singleUriLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("*/*")) { uri ->
//        if (uri == null) {
//            onNavigateFinish()
//            return@rememberLauncherForActivityResult
//        }
//
//        val source = uriList.firstOrNull() ?: run {
//            onNavigateFinish()
//            return@rememberLauncherForActivityResult
//        }
//
//        onSendUri(source, uri)
//        //viewModel.sendUri(listOf(source), uri)
//    }
//
//    /** Multiple URI result launcher */
//    val multipleUriLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
//        if (uri == null) {
//            onNavigateFinish()
//            return@rememberLauncherForActivityResult
//        }
//
//        val source = uriList.toList().ifEmpty {
//            onNavigateFinish()
//            return@rememberLauncherForActivityResult
//        }
//
//        onSendUri(source, uri)
//        //viewModel.sendUri(source, uri)
//    }
//
//
//    when {
//        uriList.size == 1 -> {
//            // Single
//            SideEffect {
//                singleUriLauncher.launch(uriList.first().fileName)
//            }
//        }
//        uriList.size > 1 -> {
//            // Multiple
//            SideEffect {
//                multipleUriLauncher.launch(uriList.first())
//            }
//        }
//    }
//}

/**
 * Send Screen
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SendScreenContainer(
    snackbarHostState: SnackbarHostState,
    sendDataList: List<SendData>,
    onClickCancel: (SendData) -> Unit,
    onClickRetry: (SendData) -> Unit,
    onClickRemove: (SendData) -> Unit,
    onClickCancelAll: () -> Unit,
    onClickClose: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(id = R.string.send_title)) },
                colors = AppTopAppBarColors(),
                actions = {
                    IconButton(
                        onClick = { onClickClose() }
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_close),
                            contentDescription = stringResource(id = R.string.send_close_button),
                        )
                    }
                },
            )
        },
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                (data.visuals as? MessageSnackbarVisual)?.let {
                    AppSnackbar(message = it.popupMessage)
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .padding(paddingValues)
        ) {
            Box(
                modifier = Modifier.weight(weight = 1f, fill = true)
            ) {
                LazyColumn {
                    items(items = sendDataList) { sendData ->
                        SendDataItem(
                            sendData = sendData,
                            onClickCancel = onClickCancel,
                            onClickRetry = onClickRetry,
                            onClickRemove = onClickRemove,
                        )
                        DividerThin()
                    }
                }
            }

            DividerNormal()

            Column(
                modifier = Modifier
                    .padding(Theme.SizeS),
            ) {
                Button(
                    onClick = onClickCancelAll,
                    shape = RoundedCornerShape(Theme.SizeSS),
                    enabled = sendDataList.any { it.state.isCancelable },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = Theme.SizeS)
                ) {
                    Text(text = stringResource(id = R.string.send_action_cancel))
                }
            }
        }
    }
}

@Composable
private fun SendDataItem(
    sendData: SendData,
    onClickCancel: (SendData) -> Unit,
    onClickRetry: (SendData) -> Unit,
    onClickRemove: (SendData) -> Unit,
) {
    val showPopup = remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = true, onClick = {
                showPopup.value = true
            })
            .padding(horizontal = Theme.SizeM, vertical = Theme.SizeS)
    ) {
        Text(
            text = sendData.name,
            style = MaterialTheme.typography.titleLarge,
        )
        Text(
            text = sendData.getSummaryText(context = LocalContext.current),
            style = MaterialTheme.typography.bodyMedium,
        )
        LinearProgressIndicator(
            progress = (sendData.progress.toFloat() / 100),
            modifier = Modifier
                .fillMaxWidth()
                .alpha(if (sendData.state.inProgress) 1f else 0f),
        )
    }

    if (showPopup.value) {
        DropdownMenu(
            expanded = showPopup.value ,
            onDismissRequest = {
                showPopup.value = false
            }
        ) {
            if (sendData.state.isCancelable) {
                DropdownMenuItem(
                    text = { Text(stringResource(id = R.string.send_action_cancel)) },
                    onClick = {
                        onClickCancel(sendData)
                        showPopup.value = false
                    }
                )
            }
            if (sendData.state.isRetryable) {
                DropdownMenuItem(
                    text = { Text(stringResource(id = R.string.send_action_retry)) },
                    onClick = {
                        onClickRetry(sendData)
                        showPopup.value = false
                    }
                )
            }
            DropdownMenuItem(
                text = { Text(stringResource(id = R.string.send_action_remove)) },
                onClick = {
                    onClickRemove(sendData)
                    showPopup.value = false
                }
            )
        }
    }
}


/**
 * Preview
 */
@Preview(
    name = "Preview",
    group = "Group",
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
)
@Composable
private fun SendScreenPreview() {
    Theme.AppTheme {
        SendScreenContainer(
            snackbarHostState = SnackbarHostState(),
            sendDataList = listOf(
                SendData(
                    id = "1",
                    name = "Sample1.jpg",
                    size = 1000000,
                    mimeType = "image/jpeg",
                    sourceUri = Uri.parse("smb://pc1/send"),
                    targetUri = Uri.parse("smb://pc2/receive"),
                    state = SendDataState.PROGRESS,
                    progressSize = 500000,
                ),
                SendData(
                    id = "2",
                    name = "Sample2.MP3",
                    size = 2000000,
                    mimeType = "audio/mpeg",
                    sourceUri = Uri.parse("smb://pc1/send"),
                    targetUri = Uri.parse("smb://pc2/receive"),
                    state = SendDataState.READY,
                    progressSize = 0,
                ),
                SendData(
                    id = "3",
                    name = "sample3.mp4",
                    size = 3000000,
                    mimeType = "video/mp4",
                    sourceUri = Uri.parse("smb://pc1/send"),
                    targetUri = Uri.parse("smb://pc2/receive"),
                    state = SendDataState.CANCEL,
                    progressSize = 2000000,
                ),
            ),
            onClickCancel = {},
            onClickRetry = {},
            onClickRemove = {},
            onClickCancelAll = {},
            onClickClose = {},
        )
    }
}
