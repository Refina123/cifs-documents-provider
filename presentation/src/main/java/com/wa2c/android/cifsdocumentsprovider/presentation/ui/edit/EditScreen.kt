package com.wa2c.android.cifsdocumentsprovider.presentation.ui.edit

import InputCheck
import InputOption
import android.annotation.SuppressLint
import android.content.res.Configuration
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.AutofillType
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wa2c.android.cifsdocumentsprovider.common.utils.getUriText
import com.wa2c.android.cifsdocumentsprovider.common.values.ConnectionResult
import com.wa2c.android.cifsdocumentsprovider.common.values.ProtocolType
import com.wa2c.android.cifsdocumentsprovider.common.values.StorageType
import com.wa2c.android.cifsdocumentsprovider.common.values.URI_AUTHORITY
import com.wa2c.android.cifsdocumentsprovider.common.values.URI_START
import com.wa2c.android.cifsdocumentsprovider.domain.model.DocumentId
import com.wa2c.android.cifsdocumentsprovider.domain.model.RemoteConnection
import com.wa2c.android.cifsdocumentsprovider.domain.model.StorageUri
import com.wa2c.android.cifsdocumentsprovider.presentation.R
import com.wa2c.android.cifsdocumentsprovider.presentation.ext.collectIn
import com.wa2c.android.cifsdocumentsprovider.presentation.ext.labelRes
import com.wa2c.android.cifsdocumentsprovider.presentation.ext.messageRes
import com.wa2c.android.cifsdocumentsprovider.presentation.ext.messageType
import com.wa2c.android.cifsdocumentsprovider.presentation.ui.common.AppSnackbarHost
import com.wa2c.android.cifsdocumentsprovider.presentation.ui.common.CommonDialog
import com.wa2c.android.cifsdocumentsprovider.presentation.ui.common.DialogButton
import com.wa2c.android.cifsdocumentsprovider.presentation.ui.common.DividerNormal
import com.wa2c.android.cifsdocumentsprovider.presentation.ui.common.MessageIcon
import com.wa2c.android.cifsdocumentsprovider.presentation.ui.common.OptionItem
import com.wa2c.android.cifsdocumentsprovider.presentation.ui.common.Theme
import com.wa2c.android.cifsdocumentsprovider.presentation.ui.common.collectAsMutableState
import com.wa2c.android.cifsdocumentsprovider.presentation.ui.common.getAppTopAppBarColors
import com.wa2c.android.cifsdocumentsprovider.presentation.ui.common.showError
import com.wa2c.android.cifsdocumentsprovider.presentation.ui.common.showPopup
import com.wa2c.android.cifsdocumentsprovider.presentation.ui.edit.components.InputText
import com.wa2c.android.cifsdocumentsprovider.presentation.ui.edit.components.SectionTitle
import com.wa2c.android.cifsdocumentsprovider.presentation.ui.edit.components.SubsectionTitle
import com.wa2c.android.cifsdocumentsprovider.presentation.ui.edit.components.UriText
import java.nio.charset.Charset

@Composable
fun EditScreen(
    viewModel: EditViewModel = hiltViewModel(),
    lifecycleOwner: LifecycleOwner = LocalLifecycleOwner.current,
    selectedHost: String? = null,
    selectedUri: StorageUri? = null,
    onNavigateBack: () -> Unit,
    onNavigateSearchHost: (connectionId: String) -> Unit,
    onNavigateSelectFolder: (RemoteConnection) -> Unit,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val showDeleteDialog = remember { mutableStateOf(false) }
    val showBackConfirmationDialog = remember { mutableStateOf(false) }
    val storageType = viewModel.storage.collectAsMutableState()

    selectedHost?.let { viewModel.host.value = selectedHost }
    selectedUri?.let { viewModel.folder.value = it.path }

    EditScreenContainer(
        snackbarHostState = snackbarHostState,
        isNew = viewModel.isNew,
        idState = viewModel.id.collectAsMutableState(),
        nameState = viewModel.name.collectAsMutableState(),
        storageState = storageType,
        domainState = viewModel.domain.collectAsMutableState(),
        hostState = viewModel.host.collectAsMutableState(),
        portState = viewModel.port.collectAsMutableState(),
        enableDfsState = viewModel.enableDfs.collectAsMutableState(),
        userState = viewModel.user.collectAsMutableState(),
        passwordState = viewModel.password.collectAsMutableState(),
        anonymousState = viewModel.anonymous.collectAsMutableState(),
        isFtpActiveModeState = viewModel.isFtpActiveMode.collectAsMutableState(),
        encodingState = viewModel.encoding.collectAsMutableState(),
        folderState = viewModel.folder.collectAsMutableState(),
        onClickBack = {
            if (viewModel.isChanged) {
                showBackConfirmationDialog.value = true
            } else {
                onNavigateBack()
            }
        },
        onClickDelete = { showDeleteDialog.value = true },
        safeTransferState = viewModel.safeTransfer.collectAsMutableState(),
        readOnlyState = viewModel.optionReadOnly.collectAsMutableState(),
        extensionState = viewModel.extension.collectAsMutableState(),
        isBusy = viewModel.isBusy.collectAsStateWithLifecycle().value,
        connectionResult = viewModel.connectionResult.collectAsStateWithLifecycle().value,
        onClickSearchHost = { viewModel.onClickSearchHost() },
        onClickSelectFolder = { viewModel.onClickSelectFolder() },
        onClickCheckConnection = { viewModel.onClickCheckConnection() },
        onClickSave = { viewModel.onClickSave() },
    )

    // Delete dialog
    if (showDeleteDialog.value) {
        CommonDialog(
            confirmButtons = listOf(
                DialogButton(label = stringResource(id = R.string.dialog_accept)) {
                    viewModel.onClickDelete()
                },
            ),
            dismissButton = DialogButton(label = stringResource(id = R.string.dialog_close)) {
                showDeleteDialog.value = false
            },
            onDismiss = {
                showDeleteDialog.value = false
            }
        ) {
            Text(stringResource(id = R.string.edit_delete_confirmation_message))
        }
    }

    // Back confirmation dialog
    if (showBackConfirmationDialog.value) {
        CommonDialog(
            confirmButtons = listOf(
                DialogButton(label = stringResource(id = R.string.dialog_accept)) {
                    onNavigateBack()
                },
            ),
            dismissButton = DialogButton(label = stringResource(id = R.string.dialog_close)) {
                showBackConfirmationDialog.value = false
            },
            onDismiss = {
                showBackConfirmationDialog.value = false
            }
        ) {
            Text(stringResource(id = R.string.edit_back_confirmation_message))
        }
    }

    LaunchedEffect(Unit) {
        viewModel.connectionResult.collectIn(lifecycleOwner) { result ->
            scope.showPopup(
                snackbarHostState = snackbarHostState,
                stringRes = result?.messageRes,
                type = result?.messageType,
                error = result?.cause
            )
        }

        viewModel.navigateSearchHost.collectIn(lifecycleOwner) { connectionId ->
            onNavigateSearchHost(connectionId)
        }

        viewModel.navigateSelectFolder.collectIn(lifecycleOwner) { result ->
            if (result.isSuccess) {
                result.getOrNull()?.let { onNavigateSelectFolder(it) }
            } else {
                scope.showError(snackbarHostState, R.string.provider_error_message, result.exceptionOrNull())
            }
        }

        viewModel.result.collectIn(lifecycleOwner) { result ->
            if (result.isSuccess) {
                onNavigateBack()
            } else {
                scope.showError(snackbarHostState, R.string.provider_error_message, result.exceptionOrNull())
            }
        }
    }
}

/**
 * Edit Screen
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
private fun EditScreenContainer(
    snackbarHostState: SnackbarHostState,
    isNew: Boolean,
    idState: MutableState<String?>,
    nameState: MutableState<String?>,
    storageState: MutableState<StorageType>,
    domainState: MutableState<String?>,
    hostState: MutableState<String?>,
    portState: MutableState<String?>,
    enableDfsState: MutableState<Boolean>,
    userState: MutableState<String?>,
    passwordState: MutableState<String?>,
    anonymousState: MutableState<Boolean>,
    isFtpActiveModeState: MutableState<Boolean>,
    encodingState: MutableState<String>,
    folderState: MutableState<String?>,
    safeTransferState: MutableState<Boolean>,
    readOnlyState: MutableState<Boolean>,
    extensionState: MutableState<Boolean>,
    isBusy: Boolean,
    connectionResult: ConnectionResult?,
    onClickBack: () -> Unit,
    onClickDelete: () -> Unit,
    onClickSearchHost: () -> Unit,
    onClickSelectFolder: () -> Unit,
    onClickCheckConnection: () -> Unit,
    onClickSave: () -> Unit,
) {
    val focusManager = LocalFocusManager.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(id = R.string.edit_title)) },
                colors = getAppTopAppBarColors(),
                actions = {
                    BadgedBox(
                        badge = {
                            Box(
                                modifier = Modifier
                                    .size(Theme.SizeM)
                                    .offset(x = (-Theme.SizeM), y = Theme.SizeM)
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_check_bg),
                                    contentDescription = "",
                                    tint = MaterialTheme.colorScheme.onSurface,
                                )
                                MessageIcon(type = connectionResult?.messageType)
                            }
                        }
                    ) {
                        IconButton(
                            onClick = onClickCheckConnection,
                            enabled = isBusy.not(),
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_folder_check),
                                contentDescription = stringResource(id = R.string.edit_check_connection_button),
                            )
                        }
                    }
                    IconButton(
                        onClick = onClickDelete,
                        enabled = isBusy.not(),
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_delete),
                            contentDescription = stringResource(id = R.string.edit_delete_button),
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onClickBack) {
                        Icon(painter = painterResource(id = R.drawable.ic_back), contentDescription = "")
                    }
                },
            )
        },
        snackbarHost = { AppSnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .padding(paddingValues)
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
            ) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(Theme.Sizes.ScreenMargin)
                        .weight(1f)
                ) {
                    val protocol = storageState.value.protocol

                    // ID
                    InputText(
                        title = stringResource(id = R.string.edit_id_title),
                        hint = stringResource(id = R.string.edit_id_hint),
                        state = idState,
                        focusManager = focusManager,
                        enabled = isNew,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Next,
                        )
                    )

                    // Name
                    InputText(
                        title = stringResource(id = R.string.edit_name_title),
                        hint = stringResource(id = R.string.edit_name_hint),
                        state = nameState,
                        focusManager = focusManager,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Next,
                        )
                    )

                    // Storage
                    InputOption(
                        title = stringResource(id = R.string.edit_storage_title),
                        items = StorageType.entries
                            .map { OptionItem(it, stringResource(id = it.labelRes)) },
                        state = storageState,
                        focusManager = focusManager,
                    )

                    SectionTitle(
                        text = stringResource(id = R.string.edit_settings_section_title),
                    )

                    // Domain
                    if (protocol == ProtocolType.SMB) {
                        InputText(
                            title = stringResource(id = R.string.edit_domain_title),
                            hint = stringResource(id = R.string.edit_domain_hint),
                            state = domainState,
                            focusManager = focusManager,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Uri,
                                imeAction = ImeAction.Next,
                            ),
                        )
                    }

                    // Host
                    InputText(
                        title = stringResource(id = R.string.edit_host_title),
                        hint = stringResource(id = R.string.edit_host_hint),
                        state = hostState,
                        focusManager = focusManager,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Uri,
                            imeAction = ImeAction.Next,
                        ),
                        iconResource = R.drawable.ic_search,
                    ) {
                        onClickSearchHost()
                    }

                    // Port
                    InputText(
                        title = stringResource(id = R.string.edit_port_title),
                        hint = stringResource(id = R.string.edit_port_hint),
                        state = portState,
                        focusManager = focusManager,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Next,
                        )
                    )

                    // Enable DFS
                    if (protocol == ProtocolType.SMB) {
                        InputCheck(
                            title = stringResource(id = R.string.edit_enable_dfs_label),
                            state = enableDfsState,
                            focusManager = focusManager,
                        )
                    }

                    // User
                    InputText(
                        title = stringResource(id = R.string.edit_user_title),
                        hint = stringResource(id = R.string.edit_user_hint),
                        state = userState,
                        focusManager = focusManager,
                        enabled = !anonymousState.value,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Next,
                        ),
                        autofillType = AutofillType.Username,
                    )

                    // Password
                    InputText(
                        title = stringResource(id = R.string.edit_password_title),
                        hint = stringResource(id = R.string.edit_password_hint),
                        state = passwordState,
                        focusManager = focusManager,
                        enabled = !anonymousState.value,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Next,
                        ),
                        autofillType = AutofillType.Password,
                    )

                    // Anonymous
                    InputCheck(
                        title = stringResource(id = R.string.edit_anonymous_label),
                        state = anonymousState,
                        focusManager = focusManager,
                    )

                    // FTP Mode
                    if (protocol == ProtocolType.FTP || protocol == ProtocolType.FTPS) {
                        InputCheck(
                            title = stringResource(id = R.string.edit_ftp_mode_title),
                            state = isFtpActiveModeState,
                            focusManager = focusManager,
                        )
                    }

                    // Encoding
                    if (protocol == ProtocolType.FTP || protocol == ProtocolType.FTPS) {
                        InputOption(
                            title = stringResource(id = R.string.edit_encoding_title),
                            items = Charset.availableCharsets()
                                .map { OptionItem(it.key, it.value.name()) },
                            state = encodingState,
                            focusManager = focusManager,
                        )
                    }

                    // Folder
                    InputText(
                        title = stringResource(id = R.string.edit_folder_title),
                        hint = stringResource(id = R.string.edit_folder_hint),
                        state = folderState,
                        focusManager = focusManager,
                        iconResource = R.drawable.ic_folder,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Uri,
                            imeAction = ImeAction.Next,
                        ),
                    ) {
                        onClickSelectFolder()
                    }

                    // Option

                    SectionTitle(
                        text = stringResource(id = R.string.edit_option_section_title),
                    )

                    if (protocol == ProtocolType.SMB) {
                        InputCheck(
                            title = stringResource(id = R.string.edit_option_safe_transfer_label),
                            state = safeTransferState,
                            focusManager = focusManager,
                        )
                    }

                    InputCheck(
                        title = stringResource(id = R.string.edit_option_read_only_label),
                        state = readOnlyState ,
                        focusManager = focusManager,
                    )

                    InputCheck(
                        title = stringResource(id = R.string.edit_option_extension_label),
                        state = extensionState,
                        focusManager = focusManager,
                    )

                    // URI

                    SectionTitle(
                        text = stringResource(id = R.string.edit_info_section_title),
                    )

                    // Storage URI
                    SubsectionTitle(
                        text = stringResource(id = R.string.edit_storage_uri_title),
                    )
                    val storageUri = getUriText(
                        storageState.value,
                        hostState.value,
                        portState.value,
                        folderState.value,
                        true
                    ) ?: ""
                    UriText(uriText = storageUri)

                    // Shared URI
                    SubsectionTitle(
                        text = stringResource(id = R.string.edit_provider_uri_title),
                    )
                    val sharedUri =  DocumentId.fromConnection(idState.value)?.takeIf { !it.isRoot }?.let{
                        "content$URI_START$URI_AUTHORITY/tree/${Uri.encode(it.idText)}"
                    } ?: ""
                    UriText(uriText = sharedUri)
                }

                DividerNormal()

                Column(
                    modifier = Modifier
                        .padding(Theme.Sizes.ScreenMargin, Theme.Sizes.S)
                ) {
                    Button(
                        onClick = onClickSave,
                        shape = RoundedCornerShape(Theme.SizeSS),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = Theme.SizeS)
                    ) {
                        Text(text = stringResource(id = R.string.edit_save_button))
                    }
                }
            }

            // isBusy
            if (isBusy) {
                val interactionSource = remember { MutableInteractionSource() }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Theme.Colors.LoadingBackground)
                        .clickable(
                            indication = null,
                            interactionSource = interactionSource,
                            onClick = {}
                        ),
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .align(Alignment.Center)
                    )
                }
            }
        }
    }

    // Back button
    BackHandler { onClickBack() }
}

/**
 * Preview
 */
@SuppressLint("UnrememberedMutableState")
@Preview(
    name = "Preview",
    group = "Group",
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
)
@Composable
private fun EditScreenPreview() {
    Theme.AppTheme {
        EditScreenContainer(
            snackbarHostState = SnackbarHostState(),
            isNew = true,
            idState = mutableStateOf("id1"),
            nameState = mutableStateOf("name1"),
            storageState = mutableStateOf(StorageType.SMBJ),
            domainState = mutableStateOf(""),
            hostState = mutableStateOf("pc1"),
            portState = mutableStateOf(""),
            enableDfsState = mutableStateOf(false),
            userState = mutableStateOf("user"),
            passwordState = mutableStateOf("password"),
            anonymousState = mutableStateOf(false),
            isFtpActiveModeState = mutableStateOf(false),
            encodingState = mutableStateOf("UTF-8"),
            folderState = mutableStateOf("/test"),
            safeTransferState = mutableStateOf(false),
            readOnlyState = mutableStateOf(false),
            extensionState = mutableStateOf(false),
            isBusy = false,
            connectionResult = null,
            onClickBack = {},
            onClickDelete = {},
            onClickSearchHost = {},
            onClickSelectFolder = {},
            onClickCheckConnection = {},
            onClickSave = {},
        )
    }
}
