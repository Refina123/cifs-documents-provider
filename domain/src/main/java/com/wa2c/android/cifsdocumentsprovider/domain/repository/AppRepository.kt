package com.wa2c.android.cifsdocumentsprovider.domain.repository

import com.wa2c.android.cifsdocumentsprovider.common.values.ProtocolType
import com.wa2c.android.cifsdocumentsprovider.common.values.StorageType
import com.wa2c.android.cifsdocumentsprovider.common.values.UiTheme
import com.wa2c.android.cifsdocumentsprovider.data.storage.manager.SshKeyManager
import com.wa2c.android.cifsdocumentsprovider.data.db.ConnectionSettingDao
import com.wa2c.android.cifsdocumentsprovider.data.preference.AppPreferencesDataStore
import com.wa2c.android.cifsdocumentsprovider.domain.mapper.DomainMapper.toDataModel
import com.wa2c.android.cifsdocumentsprovider.domain.mapper.DomainMapper.toDomainModel
import com.wa2c.android.cifsdocumentsprovider.domain.model.KnownHost
import javax.inject.Inject
import javax.inject.Singleton

/**
 * App Repository
 */
@Singleton
class AppRepository @Inject internal constructor(
    private val appPreferences: AppPreferencesDataStore,
    private val sshKeyManager: SshKeyManager,
    private val connectionSettingDao: ConnectionSettingDao,
) {

    /** UI Theme */
    val uiThemeFlow = appPreferences.uiThemeFlow

    /** UI Theme */
    suspend fun setUiTheme(value: UiTheme) = appPreferences.setUiTheme(value)

    /** Open File limit */
    val openFileLimitFlow = appPreferences.openFileLimitFlow

    /** Open File limit */
    suspend fun setOpenFileLimit(value: Int) = appPreferences.setOpenFileLimit(value)

    /** Use as local */
    val useAsLocalFlow = appPreferences.useAsLocalFlow

    /** Use as local */
    suspend fun setUseAsLocal(value: Boolean) = appPreferences.setUseAsLocal(value)

    /** Use foreground to make the app resilient to closing by Android OS */
    val useForegroundFlow = appPreferences.useForegroundFlow

    /** Use foreground to make the app resilient to closing by Android OS */
    suspend fun setUseForeground(value: Boolean) = appPreferences.setUseForeground(value)

    /**
     * Get known hosts
     */
    suspend fun getKnownHosts(): List<KnownHost> {
        val hostList = connectionSettingDao.getTypedList(StorageType.entries.filter { it.protocol == ProtocolType.SFTP }
            .map { it.value })
            .map { it.toDataModel() }
        return sshKeyManager.knownHostList.map { entity ->
            KnownHost(
                host = entity.host,
                type = entity.type,
                key = entity.key,
                connections = hostList.filter { it.host.equals(entity.host, true) }.map { it.toDomainModel() }
            )
        }
    }

    /**
     * Delete known host
     */
    fun deleteKnownHost(knownHost: KnownHost) {
        sshKeyManager.deleteKnownHost(knownHost.host, knownHost.type)
    }

    /**
     * Migrate
     */
    suspend fun migrate() {
        appPreferences.migrate()
    }

}
