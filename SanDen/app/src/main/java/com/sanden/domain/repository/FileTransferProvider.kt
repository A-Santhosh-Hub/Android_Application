package com.sanden.domain.repository

import com.sanden.domain.model.VaultFile
import kotlinx.coroutines.flow.Flow

/**
 * Stage 2 – LAN / Wi-Fi Direct file-sharing contract.
 *
 * This interface defines the peer-to-peer transfer capability that will be
 * implemented in a later stage. For now it serves as the **documented API
 * contract** so the domain and presentation layers can be designed against it.
 *
 * ### Lifecycle
 * 1. **Discovery** – call [discoverDevices] to observe nearby peers.
 * 2. **Connection** – either [startServer] (receiver) or [connectToServer] (sender).
 * 3. **Transfer** – [sendFiles] / [receiveFiles] stream progress until complete.
 *
 * ### Threading
 * All functions return cold [Flow]s; collection should happen on a background
 * dispatcher. Implementations must be safe for concurrent use from multiple
 * coroutines.
 */
interface FileTransferProvider {

    /**
     * Start a transfer server on the given [port].
     *
     * The returned flow emits [TransferEvent]s describing the server lifecycle
     * (started, client connected, transfer complete, errors).
     *
     * @param port TCP port to bind. Use `0` to let the OS pick a free port.
     * @return a cold flow of server events.
     */
    fun startServer(port: Int = 0): Flow<TransferEvent>

    /**
     * Connect to a running transfer server at [host]:[port].
     *
     * @return a cold flow of connection events.
     */
    fun connectToServer(host: String, port: Int): Flow<TransferEvent>

    /**
     * Send [files] to the given [target] device.
     *
     * The flow emits [TransferProgress] for every chunk written so the UI can
     * display a progress bar.
     */
    fun sendFiles(files: List<VaultFile>, target: TransferTarget): Flow<TransferProgress>

    /**
     * Begin receiving files from a connected peer.
     *
     * The flow emits [TransferProgress] for every chunk received.
     */
    fun receiveFiles(): Flow<TransferProgress>

    /**
     * Continuously discover devices on the local network that are running a
     * SanDen transfer server.
     *
     * @return a flow that emits the current snapshot of discovered devices
     *         whenever the list changes.
     */
    fun discoverDevices(): Flow<List<TransferTarget>>
}

// ── Supporting data types ──────────────────────────────────────────────────

/**
 * Represents a discovered peer device that can send or receive files.
 */
data class TransferTarget(
    /** Unique identifier for this device (e.g. a UUID). */
    val id: String,

    /** Human-readable device name. */
    val name: String,

    /** IP address or hostname of the peer. */
    val host: String,

    /** TCP port the peer's transfer server is listening on. */
    val port: Int
)

/**
 * Events emitted during the server/client lifecycle.
 */
sealed class TransferEvent {
    /** Server has bound to a port and is ready for connections. */
    data class ServerStarted(val port: Int) : TransferEvent()

    /** A remote client has connected. */
    data class ClientConnected(val target: TransferTarget) : TransferEvent()

    /** Successfully connected to a remote server. */
    data class ConnectedToServer(val target: TransferTarget) : TransferEvent()

    /** The transfer session completed normally. */
    data object Completed : TransferEvent()

    /** Connection was lost or an error occurred. */
    data class Error(val message: String, val cause: Throwable? = null) : TransferEvent()

    /** The session was explicitly disconnected. */
    data object Disconnected : TransferEvent()
}

/**
 * Progress snapshot emitted during a file transfer.
 */
data class TransferProgress(
    /** Name of the file currently being transferred. */
    val currentFileName: String,

    /** 1-based index of the file being transferred. */
    val currentFileIndex: Int,

    /** Total number of files in this transfer batch. */
    val totalFiles: Int,

    /** Bytes transferred for the current file. */
    val bytesTransferred: Long,

    /** Total bytes of the current file. */
    val totalBytes: Long,

    /** Overall progress across all files, 0 f .. 1 f. */
    val overallProgress: Float
)
