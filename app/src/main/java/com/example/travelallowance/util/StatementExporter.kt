package com.example.travelallowance.util

import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.example.travelallowance.data.EmployeeProfile
import com.example.travelallowance.data.Journey
import java.io.File
import java.io.FileOutputStream
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import java.time.YearMonth
import java.time.format.DateTimeFormatter

object StatementExporter {
    fun exportPdf(context: Context, profile: EmployeeProfile, month: YearMonth, journeys: List<Journey>): File {
        val file = File(context.cacheDir, "TA_Statement_${month}.pdf")
        val doc = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 2400, 1).create()
        val page = doc.startPage(pageInfo)
        val canvas = page.canvas
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 10f; color = android.graphics.Color.DKGRAY }
        val bold = Paint(paint).apply { typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); textSize = 11f; color = android.graphics.Color.BLACK }
        var y = 48f
        fun line(text: String, p: Paint = paint, step: Float = 19f) { canvas.drawText(text.take(105), 40f, y, p); y += step }
        line("TRAVEL ALLOWANCE STATEMENT", Paint(bold).apply { textSize = 17f }, 30f)
        line(month.format(DateTimeFormatter.ofPattern("MMMM yyyy")), bold, 27f)
        line("Employee: ${profile.name}     Employee ID: ${profile.employeeId}")
        line("Designation: ${profile.designation}")
        line("Office: ${profile.officeName}")
        line("Address: ${profile.officeAddress}", step = 28f)
        line("DATE       ROUTE / PURPOSE                         KM       TA TYPE       AMOUNT", bold, 22f)
        journeys.sortedBy { it.date }.forEach { j ->
            val route = "${j.start.label} - ${j.destination.label} - ${j.returnPoint.label}"
            line("${j.date}   ${route.take(36).padEnd(36)} ${fmt(j.totalKm).padStart(6)}   ${j.taType.padEnd(5)}   ${inr(j.taAmount)}", step = 18f)
        }
        y += 12f
        line("Total journeys: ${journeys.size}", bold)
        line("Total distance: ${fmt(journeys.sumOf { it.totalKm })} km", bold)
        line("Full TA: ${journeys.count { it.taType == "FULL" }}     Half TA: ${journeys.count { it.taType == "HALF" }}", bold)
        line("Total payable: ${inr(journeys.sumOf { it.taAmount })}", Paint(bold).apply { textSize = 14f }, 24f)
        doc.finishPage(page)
        FileOutputStream(file).use { doc.writeTo(it) }
        doc.close()
        return file
    }

    // A standards-based XLSX workbook is assembled using the OpenXML package parts.
    fun exportExcel(context: Context, profile: EmployeeProfile, month: YearMonth, journeys: List<Journey>): File {
        val file = File(context.cacheDir, "TA_Statement_${month}.xlsx")
        val rows = mutableListOf<List<String>>()
        rows += listOf("TRAVEL ALLOWANCE STATEMENT", month.toString())
        rows += listOf("Employee", profile.name, "Employee ID", profile.employeeId)
        rows += listOf("Designation", profile.designation, "Office", profile.officeName)
        rows += listOf("Office address", profile.officeAddress)
        rows += listOf("Date", "Starting point", "Destination", "Return point", "Start-Destination km", "Destination-Return km", "Combined km", "TA type", "TA amount (INR)")
        journeys.sortedBy { it.date }.forEach { j -> rows += listOf(j.date, j.start.label, j.destination.label, j.returnPoint.label, fmt(j.outwardKm), fmt(j.returnKm), fmt(j.totalKm), j.taType, fmt(j.taAmount)) }
        rows += listOf("", "", "", "", "", "TOTAL", fmt(journeys.sumOf { it.totalKm }), "", fmt(journeys.sumOf { it.taAmount }))
        ZipOutputStream(FileOutputStream(file)).use { zip ->
            fun part(name: String, content: String) { zip.putNextEntry(ZipEntry(name)); zip.write(content.toByteArray(Charsets.UTF_8)); zip.closeEntry() }
            part("[Content_Types].xml", """<?xml version="1.0" encoding="UTF-8"?><Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types"><Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/><Default Extension="xml" ContentType="application/xml"/><Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/><Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/></Types>""")
            part("_rels/.rels", """<?xml version="1.0" encoding="UTF-8"?><Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/></Relationships>""")
            part("xl/workbook.xml", """<?xml version="1.0" encoding="UTF-8"?><workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships"><sheets><sheet name="TA Statement" sheetId="1" r:id="rId1"/></sheets></workbook>""")
            part("xl/_rels/workbook.xml.rels", """<?xml version="1.0" encoding="UTF-8"?><Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/></Relationships>""")
            val sheet = buildString {
                append("<?xml version=\"1.0\" encoding=\"UTF-8\"?><worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\"><sheetData>")
                rows.forEachIndexed { ri, row ->
                    append("<row r=\"${ri + 1}\">")
                    row.forEachIndexed { ci, value ->
                        val ref = column(ci) + (ri + 1)
                        append("<c r=\"$ref\" t=\"inlineStr\"><is><t xml:space=\"preserve\">${xml(value)}</t></is></c>")
                    }
                    append("</row>")
                }
                append("</sheetData></worksheet>")
            }
            part("xl/worksheets/sheet1.xml", sheet)
        }
        return file
    }

    fun share(context: Context, file: File, mime: String) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply { type = mime; putExtra(Intent.EXTRA_STREAM, uri); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) }
        context.startActivity(Intent.createChooser(intent, "Share TA statement"))
    }
    private fun column(index: Int): String { var n = index + 1; var s = ""; while (n > 0) { val r = (n - 1) % 26; s = ('A'.code + r).toChar() + s; n = (n - 1) / 26 }; return s }
    private fun xml(s: String) = s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&apos;")
    private fun fmt(v: Double) = String.format(Locale.US, "%.2f", v)
    private fun inr(v: Double) = "Rs. " + String.format(Locale.US, "%,.2f", v)
}
