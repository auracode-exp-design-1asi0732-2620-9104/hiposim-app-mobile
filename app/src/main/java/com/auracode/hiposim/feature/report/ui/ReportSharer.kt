package com.auracode.hiposim.feature.report.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.auracode.hiposim.R
import com.auracode.hiposim.feature.report.domain.ReportFailure
import com.auracode.hiposim.feature.report.domain.ReportFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Hands the downloaded PDF to the phone (US08): it is saved in the private cache folder and opened with the PDF viewer
 * of the device. When there is none, the share sheet is offered instead, so the buyer can send it by mail or chat,
 * or save it in Drive, which is what the story is for ("send it to my partner or take it to the bank").
 */
object ReportSharer {
    /** Folder of the cache directory that `res/xml/report_file_paths.xml` shares. Only the last report is kept. */
    private const val FOLDER = "reports"

    /** Authority declared by the FileProvider of the manifest. */
    fun authority(context: Context) = "${context.packageName}.reports"

    /** Writes the report in the shared cache folder, replacing the previous one, and returns the address to open it. */
    suspend fun prepare(
        context: Context,
        report: ReportFile,
    ): Uri =
        withContext(Dispatchers.IO) {
            val folder = File(context.cacheDir, FOLDER).apply { mkdirs() }
            folder.listFiles()?.forEach { it.delete() }
            val file = File(folder, report.fileName).apply { writeBytes(report.bytes) }
            FileProvider.getUriForFile(context, authority(context), file)
        }

    /** Returns false when no app could take the file. */
    suspend fun open(
        context: Context,
        report: ReportFile,
    ): Boolean {
        val uri = prepare(context, report)
        val view =
            Intent(Intent.ACTION_VIEW)
                .setDataAndType(uri, "application/pdf")
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        return try {
            context.startActivity(view)
            true
        } catch (_: ActivityNotFoundException) {
            share(context, uri)
        }
    }

    private fun share(
        context: Context,
        uri: Uri,
    ): Boolean {
        val send =
            Intent(Intent.ACTION_SEND)
                .setType("application/pdf")
                .putExtra(Intent.EXTRA_STREAM, uri)
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        val chooser =
            Intent
                .createChooser(send, context.getString(R.string.pdf_share_title))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return try {
            context.startActivity(chooser)
            true
        } catch (_: ActivityNotFoundException) {
            false
        }
    }
}

/** The message that explains why the PDF could not be downloaded. */
fun ReportFailure.messageRes(): Int =
    when (this) {
        ReportFailure.SimulationMissing -> R.string.pdf_error_simulation_missing
        ReportFailure.SessionExpired -> R.string.pdf_error_session_expired
        ReportFailure.NotFound -> R.string.pdf_error_not_found
        ReportFailure.NoConnection -> R.string.pdf_error_no_connection
        ReportFailure.ServiceUnavailable -> R.string.pdf_error_service_unavailable
        ReportFailure.NotYours, ReportFailure.Unexpected -> R.string.pdf_error_unexpected
    }
