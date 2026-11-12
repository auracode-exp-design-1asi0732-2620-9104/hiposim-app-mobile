package com.auracode.hiposim

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.auracode.hiposim.feature.report.data.FakeReportService
import com.auracode.hiposim.feature.report.domain.ReportFile
import com.auracode.hiposim.feature.report.ui.ReportSharer
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/** US08: the report is shared with the PDF viewer through the FileProvider declared in the manifest. */
@RunWith(AndroidJUnit4::class)
class ReportSharerTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    private fun report(id: String) = ReportFile("hiposim-simulacion-$id.pdf", FakeReportService.demoPdf(id))

    @Test
    fun theReportIsReadableThroughTheProviderAddress() =
        runBlocking {
            val report = report("one")

            val uri = ReportSharer.prepare(context, report)

            assertEquals("content", uri.scheme)
            assertEquals("${context.packageName}.reports", uri.authority)
            val read = context.contentResolver.openInputStream(uri)
            assertNotNull(read)
            assertArrayEquals(report.bytes, read!!.use { it.readBytes() })
        }

    @Test
    fun onlyTheLastReportIsKept() =
        runBlocking {
            ReportSharer.prepare(context, report("old"))
            ReportSharer.prepare(context, report("new"))

            val kept = File(context.cacheDir, "reports").list().orEmpty().toList()

            assertEquals(listOf("hiposim-simulacion-new.pdf"), kept)
        }
}
