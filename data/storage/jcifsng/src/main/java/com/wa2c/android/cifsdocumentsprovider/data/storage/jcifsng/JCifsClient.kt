package com.wa2c.android.cifsdocumentsprovider.data.storage.jcifsng

import android.os.ProxyFileDescriptorCallback
import android.util.LruCache
import com.wa2c.android.cifsdocumentsprovider.common.utils.isDirectoryUri
import com.wa2c.android.cifsdocumentsprovider.common.utils.logD
import com.wa2c.android.cifsdocumentsprovider.common.utils.logE
import com.wa2c.android.cifsdocumentsprovider.common.utils.logW
import com.wa2c.android.cifsdocumentsprovider.common.utils.optimizeUri
import com.wa2c.android.cifsdocumentsprovider.common.values.AccessMode
import com.wa2c.android.cifsdocumentsprovider.common.values.CACHE_TIMEOUT
import com.wa2c.android.cifsdocumentsprovider.common.values.CONNECTION_TIMEOUT
import com.wa2c.android.cifsdocumentsprovider.common.values.ConnectionResult
import com.wa2c.android.cifsdocumentsprovider.common.values.READ_TIMEOUT
import com.wa2c.android.cifsdocumentsprovider.data.storage.entity.CifsClientInterface
import com.wa2c.android.cifsdocumentsprovider.data.storage.entity.StorageConnection
import com.wa2c.android.cifsdocumentsprovider.data.storage.entity.StorageFile
import com.wa2c.android.cifsdocumentsprovider.data.storage.entity.getCause
import jcifs.CIFSContext
import jcifs.config.PropertyConfiguration
import jcifs.context.BaseContext
import jcifs.context.CIFSContextWrapper
import jcifs.smb.NtStatus
import jcifs.smb.NtlmPasswordAuthenticator
import jcifs.smb.SmbException
import jcifs.smb.SmbFile
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Properties


/**
 * jCIFS-ng Client
 */
class JCifsClient constructor(
    private val openFileLimit: Int,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
): CifsClientInterface {

    /** Session cache */
    private val contextCache = object : LruCache<com.wa2c.android.cifsdocumentsprovider.data.storage.entity.StorageConnection, CIFSContext>(openFileLimit) {
        override fun entryRemoved(evicted: Boolean, key: com.wa2c.android.cifsdocumentsprovider.data.storage.entity.StorageConnection?, oldValue: CIFSContext?, newValue: CIFSContext?) {
            try {
                oldValue?.close()
                logD("Session Disconnected: ${key?.name}")
            } catch (e: Exception) {
                logE(e)
            }
            super.entryRemoved(evicted, key, oldValue, newValue)
            logD("Session Removed: $key")
        }
    }

    /**
     * Get auth by user. Anonymous if user and password are empty.
     */
    private fun getCifsContext(
        connection: com.wa2c.android.cifsdocumentsprovider.data.storage.entity.StorageConnection,
    ): CIFSContext {
        val property = Properties().apply {
            setProperty("jcifs.smb.client.minVersion", "SMB202")
            setProperty("jcifs.smb.client.maxVersion", "SMB311")
            setProperty("jcifs.smb.client.responseTimeout", READ_TIMEOUT.toString())
            setProperty("jcifs.smb.client.connTimeout", CONNECTION_TIMEOUT.toString())
            setProperty("jcifs.smb.client.attrExpirationPeriod", CACHE_TIMEOUT.toString())
            setProperty("jcifs.smb.client.dfs.disabled", (!connection.enableDfs).toString())
            setProperty("jcifs.smb.client.ipcSigningEnforced", (!connection.user.isNullOrEmpty() && connection.user != "guest").toString())
            setProperty("jcifs.smb.client.guestUsername", "cifs-documents-provider")
        }

        val context = BaseContext(PropertyConfiguration(property)).let {
            when {
                connection.isAnonymous -> it.withAnonymousCredentials() // Anonymous
                connection.isGuest -> it.withGuestCrendentials() // Guest if empty username
                else -> it.withCredentials(NtlmPasswordAuthenticator(connection.domain, connection.user, connection.password, null))
            }
        }
        logD("CIFSContext Created: $context")
        return CIFSContextWrapper(context).also {
            contextCache.put(connection, it)
        }
    }

    /**
     * Get SMB file
     */
    private suspend fun getSmbFile(dto: com.wa2c.android.cifsdocumentsprovider.data.storage.entity.StorageConnection, forced: Boolean = false): SmbFile? {
        return withContext(dispatcher) {
            try {
                val context = (if (forced) null else contextCache[dto]) ?: getCifsContext(dto)
                SmbFile(dto.uri, context).apply {
                    connectTimeout = CONNECTION_TIMEOUT
                    readTimeout = READ_TIMEOUT
                }
            } catch (e: Exception) {
                logE(e)
                null
            }
        }
    }

    /**
     * Check setting connectivity.
     */
    override suspend fun checkConnection(dto: com.wa2c.android.cifsdocumentsprovider.data.storage.entity.StorageConnection): ConnectionResult {
        return withContext(dispatcher) {
            try {
                getSmbFile(dto, true)?.use { it.list() }
                ConnectionResult.Success
            } catch (e: Exception) {
                logW(e)
                val c = e.getCause()
                if (e is SmbException && e.ntStatus in warningStatus) {
                    // Warning
                    ConnectionResult.Warning(c)
                } else {
                    // Failure
                    ConnectionResult.Failure(c)
                }
            } finally {
                contextCache.remove(dto)
            }
        }
    }

    /**
     * Get CifsFile
     */
    override suspend fun getFile(dto: com.wa2c.android.cifsdocumentsprovider.data.storage.entity.StorageConnection, forced: Boolean): com.wa2c.android.cifsdocumentsprovider.data.storage.entity.StorageFile? {
        return getSmbFile(dto, forced)?.use { it.toCifsFile() }
    }

    /**
     * Get children CifsFile list
     */
    override suspend fun getChildren(dto: com.wa2c.android.cifsdocumentsprovider.data.storage.entity.StorageConnection, forced: Boolean): List<com.wa2c.android.cifsdocumentsprovider.data.storage.entity.StorageFile> {
        return getSmbFile(dto, forced)?.use { parent ->
            parent.listFiles()?.mapNotNull { child ->
                child.use { it.toCifsFile() }
            }
        } ?: emptyList()
    }


    /**
     * Create new CifsFile.
     */
    override suspend fun createFile(dto: com.wa2c.android.cifsdocumentsprovider.data.storage.entity.StorageConnection, mimeType: String?): com.wa2c.android.cifsdocumentsprovider.data.storage.entity.StorageFile? {
        return withContext(dispatcher) {
            val optimizedUri = dto.uri.optimizeUri(if (dto.extension) mimeType else null)
            getSmbFile(dto.copy(inputUri = optimizedUri))?.use {
                if (optimizedUri.isDirectoryUri) {
                    // Directory
                    it.mkdir()
                } else {
                    // File
                    it.createNewFile()
                }
                it.toCifsFile()
            }
        }
    }

    /**
     * Copy CifsFile
     */
    override suspend fun copyFile(
        sourceDto: com.wa2c.android.cifsdocumentsprovider.data.storage.entity.StorageConnection,
        targetDto: com.wa2c.android.cifsdocumentsprovider.data.storage.entity.StorageConnection,
    ): com.wa2c.android.cifsdocumentsprovider.data.storage.entity.StorageFile? {
        return withContext(dispatcher) {
            getSmbFile(sourceDto)?.use { source ->
                getSmbFile(targetDto)?.use { target ->
                    source.copyTo(target)
                    target.toCifsFile()
                }
            }
        }
    }

    /**
     * Rename file
     */
    override suspend fun renameFile(
        sourceDto: com.wa2c.android.cifsdocumentsprovider.data.storage.entity.StorageConnection,
        targetDto: com.wa2c.android.cifsdocumentsprovider.data.storage.entity.StorageConnection,
    ): com.wa2c.android.cifsdocumentsprovider.data.storage.entity.StorageFile? {
        return withContext(dispatcher) {
            getSmbFile(sourceDto)?.use { source ->
                getSmbFile(targetDto)?.use { target ->
                    source.renameTo(target)
                    target.toCifsFile()
                }
            }
        }
    }

    /**
     * Delete file
     */
    override suspend fun deleteFile(
        dto: com.wa2c.android.cifsdocumentsprovider.data.storage.entity.StorageConnection,
    ): Boolean {
        return withContext(dispatcher) {
            getSmbFile(dto)?.use {
                it.delete()
                true
            } ?: false
        }
    }

    /**
     * Move file
     */
    override suspend fun moveFile(
        sourceDto: com.wa2c.android.cifsdocumentsprovider.data.storage.entity.StorageConnection,
        targetDto: com.wa2c.android.cifsdocumentsprovider.data.storage.entity.StorageConnection,
    ): com.wa2c.android.cifsdocumentsprovider.data.storage.entity.StorageFile? {
        return withContext(dispatcher) {
            if (sourceDto == targetDto) {
                // Same connection
                renameFile(sourceDto, targetDto)
            } else {
                // Different connection
                copyFile(sourceDto, targetDto)?.also {
                    deleteFile(sourceDto)
                }
            }
        }
    }

    /**
     * Get ParcelFileDescriptor
     */
    override suspend fun getFileDescriptor(dto: com.wa2c.android.cifsdocumentsprovider.data.storage.entity.StorageConnection, mode: AccessMode, onFileRelease: () -> Unit): ProxyFileDescriptorCallback? {
        return withContext(dispatcher) {
            val file = getSmbFile(dto) ?: return@withContext null
            val release = fun () {
                try { file.close() } catch (e: Exception) { logE(e) }
                onFileRelease()
            }

            if (dto.safeTransfer) {
                JCifsProxyFileCallbackSafe(file, mode, release)
            } else {
                JCifsProxyFileCallback(file, mode, release)
            }
        }
    }

    override suspend fun close() {
        contextCache.evictAll()
    }

    /**
     * Convert SmbFile to CifsFile
     */
    private suspend fun SmbFile.toCifsFile(): com.wa2c.android.cifsdocumentsprovider.data.storage.entity.StorageFile {
        val urlText = url.toString()
        return withContext(dispatcher) {
            val isDir = urlText.isDirectoryUri || isDirectory
            com.wa2c.android.cifsdocumentsprovider.data.storage.entity.StorageFile(
                name = name.trim('/'),
                uri = urlText,
                size = if (isDir || !isFile) 0 else length(),
                lastModified = lastModified,
                isDirectory = isDir,
            )
        }
    }

    companion object {
        /** Warning status */
        private val warningStatus = arrayOf(
            NtStatus.NT_STATUS_BAD_NETWORK_NAME, // No root folder
            NtStatus.NT_STATUS_OBJECT_NAME_NOT_FOUND, // No sub folder
        )
    }

}
