package com.sanden.data.transfer

import com.sanden.domain.model.VaultFile
import com.sanden.domain.repository.FileTransferProvider
import com.sanden.domain.repository.TransferEvent
import com.sanden.domain.repository.TransferProgress
import com.sanden.domain.repository.TransferTarget
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Concrete Stage 2 implementation of [FileTransferProvider] backed by [LanTransferHub].
 */
@Singleton
class LanFileTransferProvider @Inject constructor(
    private val hub: LanTransferHub
) : FileTransferProvider {

    override fun startServer(port: Int): Flow<TransferEvent> = flow {
        val targetPort = if (port <= 0) LanTransferHub.DEFAULT_PORT else port
        hub.start(targetPort)
        emit(TransferEvent.ServerStarted(targetPort))
    }

    override fun connectToServer(host: String, port: Int): Flow<TransferEvent> = emptyFlow()

    override fun sendFiles(files: List<VaultFile>, target: TransferTarget): Flow<TransferProgress> = flow {
        val total = files.size
        for ((index, file) in files.withIndex()) {
            hub.sendPhoneFile(
                filePath = file.path,
                fileName = file.name,
                fileSize = file.size,
                mimeType = file.mimeType ?: "application/octet-stream"
            )
            emit(
                TransferProgress(
                    currentFileName = file.name,
                    currentFileIndex = index + 1,
                    totalFiles = total,
                    bytesTransferred = file.size,
                    totalBytes = file.size,
                    overallProgress = (index + 1).toFloat() / total.coerceAtLeast(1)
                )
            )
        }
    }

    override fun receiveFiles(): Flow<TransferProgress> = emptyFlow()

    override fun discoverDevices(): Flow<List<TransferTarget>> = emptyFlow()
}
