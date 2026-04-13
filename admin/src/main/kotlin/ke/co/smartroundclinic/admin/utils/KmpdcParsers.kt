package ke.co.smartroundclinic.admin.utils

// ── Parsers.kt ───────────────────────────────────────────────────────────────

import ke.co.smartroundclinic.admin.domain.model.DentalGeneralPractitioner
import ke.co.smartroundclinic.admin.domain.model.DentalMasterRecord
import ke.co.smartroundclinic.admin.domain.model.DentalRegistrar
import ke.co.smartroundclinic.admin.domain.model.DentalSeniorRegistrar
import ke.co.smartroundclinic.admin.domain.model.DentalSpecialistPractitioner
import ke.co.smartroundclinic.admin.domain.model.DoctorLicenceStatus
import ke.co.smartroundclinic.admin.domain.model.MedicalGeneralPractitioner
import ke.co.smartroundclinic.admin.domain.model.MedicalMasterRecord
import ke.co.smartroundclinic.admin.domain.model.MedicalRegistrar
import ke.co.smartroundclinic.admin.domain.model.MedicalSeniorRegistrar
import ke.co.smartroundclinic.admin.domain.model.MedicalSpecialistPractitioner
import org.jsoup.Jsoup

// ── Shared string helpers ─────────────────────────────────────────────────────

fun String.nullIfBlank(): String? = trim().ifBlank { null }

fun String.cleanHtmlEntities(): String =
    replace("&amp;amp;", "&")
        .replace("&amp;", "&")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .trim()

// ═════════════════════════════════════════════════════════════════════════════
// MEDICAL PARSERS
// ═════════════════════════════════════════════════════════════════════════════

// 1. Medical Master
// Columns: Fullname | Reg_No | Address | Qualifications | Speciality | Sub_Speciality | status
fun parseMedicalMasterHtml(html: String): List<MedicalMasterRecord> {
    val rows = Jsoup.parse(html).select("table.bootstrap-datatable tbody tr")
    return rows.mapNotNull { row ->
        val c = row.select("td")
        if (c.size < 7) return@mapNotNull null
        MedicalMasterRecord(
            fullName       = c[0].text().trim(),
            regNumber      = c[1].text().trim(),
            address        = c[2].text().trim(),
            qualifications = c[3].text().cleanHtmlEntities(),
            speciality     = c[4].text().nullIfBlank(),
            subSpeciality  = c[5].text().nullIfBlank(),
            status         = DoctorLicenceStatus.from(c[6].text()),
        )
    }
}

// 2. Medical General Practice
// Columns: Fullname | Reg_No | Address | Qualifications | licence_Type | status
fun parseMedicalGpHtml(html: String): List<MedicalGeneralPractitioner> {
    val rows = Jsoup.parse(html).select("table.bootstrap-datatable tbody tr")
    return rows.mapNotNull { row ->
        val c = row.select("td")
        if (c.size < 6) return@mapNotNull null
        MedicalGeneralPractitioner(
            fullName       = c[0].text().trim(),
            regNumber      = c[1].text().trim(),
            address        = c[2].text().trim(),
            qualifications = c[3].text().cleanHtmlEntities(),
            licenceType    = c[4].text().trim(),
            status         = DoctorLicenceStatus.from(c[5].text()),
        )
    }
}

// 3. Medical Registrar
// Columns: Fullname | Reg_No | Address | Qualifications | Discipline | licence_Type | status
fun parseMedicalRegistrarHtml(html: String): List<MedicalRegistrar> {
    val rows = Jsoup.parse(html).select("table.bootstrap-datatable tbody tr")
    return rows.mapNotNull { row ->
        val c = row.select("td")
        if (c.size < 7) return@mapNotNull null
        MedicalRegistrar(
            fullName       = c[0].text().trim(),
            regNumber      = c[1].text().trim(),
            address        = c[2].text().trim(),
            qualifications = c[3].text().cleanHtmlEntities(),
            discipline     = c[4].text().nullIfBlank(),
            licenceType    = c[5].text().trim(),
            status         = DoctorLicenceStatus.from(c[6].text()),
        )
    }
}

// 4. Medical Senior Registrar
// Columns: Fullname | Reg_No | Address | Qualifications | Discipline | licence_Type | status
fun parseMedicalSeniorRegistrarHtml(html: String): List<MedicalSeniorRegistrar> {
    val rows = Jsoup.parse(html).select("table.bootstrap-datatable tbody tr")
    return rows.mapNotNull { row ->
        val c = row.select("td")
        if (c.size < 7) return@mapNotNull null
        MedicalSeniorRegistrar(
            fullName       = c[0].text().trim(),
            regNumber      = c[1].text().trim(),
            address        = c[2].text().trim(),
            qualifications = c[3].text().cleanHtmlEntities(),
            discipline     = c[4].text().nullIfBlank(),
            licenceType    = c[5].text().trim(),
            status         = DoctorLicenceStatus.from(c[6].text()),
        )
    }
}

// 5. Medical Specialist Practice
// Columns: Fullname | Reg_No | Address | Qualifications | Specialty | licence_Type | status
fun parseMedicalSpecialistHtml(html: String): List<MedicalSpecialistPractitioner> {
    val rows = Jsoup.parse(html).select("table.bootstrap-datatable tbody tr")
    return rows.mapNotNull { row ->
        val c = row.select("td")
        if (c.size < 7) return@mapNotNull null
        MedicalSpecialistPractitioner(
            fullName       = c[0].text().trim(),
            regNumber      = c[1].text().trim(),
            address        = c[2].text().trim(),
            qualifications = c[3].text().cleanHtmlEntities(),
            specialty      = c[4].text().nullIfBlank(),
            licenceType    = c[5].text().trim(),
            status         = DoctorLicenceStatus.from(c[6].text()),
        )
    }
}

fun parseDentalMasterHtml(html: String): List<DentalMasterRecord> {
    val rows = Jsoup.parse(html).select("table.bootstrap-datatable tbody tr")
    return rows.mapNotNull { row ->
        val c = row.select("td")
        if (c.size < 7) return@mapNotNull null
        DentalMasterRecord(
            fullName       = c[0].text().trim(),
            regNumber      = c[1].text().trim(),
            address        = c[2].text().trim(),
            qualifications = c[3].text().cleanHtmlEntities(),
            speciality     = c[4].text().nullIfBlank(),
            subSpeciality  = c[5].text().nullIfBlank(),
            status         = DoctorLicenceStatus.from(c[6].text()),
        )
    }
}

fun parseDentalGpHtml(html: String): List<DentalGeneralPractitioner> {
    val rows = Jsoup.parse(html).select("table.bootstrap-datatable tbody tr")
    return rows.mapNotNull { row ->
        val c = row.select("td")
        if (c.size < 6) return@mapNotNull null
        DentalGeneralPractitioner(
            fullName = c[0].text().trim(),
            regNumber = c[1].text().trim(),
            address = c[2].text().trim(),
            qualifications = c[3].text().cleanHtmlEntities(),
            licenceType = c[4].text().trim(),
            status = DoctorLicenceStatus.from(c[5].text()),
        )
    }
}

// 8. Dental Registrar
// Columns: Fullname | Reg_No | Address | Qualifications | Discipline | licence_Type | status
fun parseDentalRegistrarHtml(html: String): List<DentalRegistrar> {
    val rows = Jsoup.parse(html).select("table.bootstrap-datatable tbody tr")
    return rows.mapNotNull { row ->
        val c = row.select("td")
        if (c.size < 7) return@mapNotNull null
        DentalRegistrar(
            fullName = c[0].text().trim(),
            regNumber = c[1].text().trim(),
            address = c[2].text().trim(),
            qualifications = c[3].text().cleanHtmlEntities(),
            discipline = c[4].text().nullIfBlank(),
            licenceType = c[5].text().trim(),
            status = DoctorLicenceStatus.from(c[6].text()),
        )
    }
}

// 9. Dental Senior Registrar
// Columns: Fullname | Reg_No | Address | Qualifications | Discipline | licence_Type | status
fun parseDentalSeniorRegistrarHtml(html: String): List<DentalSeniorRegistrar> {
    val rows = Jsoup.parse(html).select("table.bootstrap-datatable tbody tr")
    return rows.mapNotNull { row ->
        val c = row.select("td")
        if (c.size < 7) return@mapNotNull null
        DentalSeniorRegistrar(
            fullName = c[0].text().trim(),
            regNumber = c[1].text().trim(),
            address = c[2].text().trim(),
            qualifications = c[3].text().cleanHtmlEntities(),
            discipline = c[4].text().nullIfBlank(),
            licenceType = c[5].text().trim(),
            status = DoctorLicenceStatus.from(c[6].text()),
        )
    }
}

// 10. Dental Specialist Practice
// Columns: Fullname | Reg_No | Address | Qualifications | Specialty | licence_Type | status
fun parseDentalSpecialistHtml(html: String): List<DentalSpecialistPractitioner> {
    val rows = Jsoup.parse(html).select("table.bootstrap-datatable tbody tr")
    return rows.mapNotNull { row ->
        val c = row.select("td")
        if (c.size < 7) return@mapNotNull null
        DentalSpecialistPractitioner(
            fullName       = c[0].text().trim(),
            regNumber      = c[1].text().trim(),
            address        = c[2].text().trim(),
            qualifications = c[3].text().cleanHtmlEntities(),
            specialty      = c[4].text().nullIfBlank(),
            licenceType    = c[5].text().trim(),
            status         = DoctorLicenceStatus.from(c[6].text()),
        )
    }
}