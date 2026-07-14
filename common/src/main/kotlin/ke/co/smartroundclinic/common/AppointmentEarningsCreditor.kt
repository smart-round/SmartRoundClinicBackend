package ke.co.smartroundclinic.common

interface AppointmentEarningsCreditor {
    suspend fun creditEarningsForAppointment(appointmentId: String, doctorId: String)
}
