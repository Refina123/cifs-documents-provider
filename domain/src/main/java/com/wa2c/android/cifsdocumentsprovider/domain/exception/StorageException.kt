package com.wa2c.android.cifsdocumentsprovider.domain.exception

import java.io.IOException

/**
 * Edit exception.
 */
sealed class StorageException(message: String) : IOException(message) {
    class WritingNotAllowedException : StorageException("Writing is not allowed in options.")

}
