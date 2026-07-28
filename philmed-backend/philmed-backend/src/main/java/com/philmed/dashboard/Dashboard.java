package com.philmed.dashboard;

import com.philmed.appointment.Appointment;
import com.philmed.doctor.Doctor;
import com.philmed.user.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Lightweight admin numbers — just counts, so a full service layer would be
 * overkill. Locked to ADMIN role in SecurityConfig.
 */
public class Dashboard {

    public static class Stats {
        public long totalPatients;
        public long totalDoctors;
        public long totalAppointments;
        public long pendingAppointments;
        public long confirmedAppointments;
    }

    @RestController
    @RequestMapping("/api/dashboard")
    public static class Controller {
        private final User.Repository userRepository;
        private final Doctor.Repository doctorRepository;
        private final Appointment.Repository appointmentRepository;

        @Autowired
        public Controller(User.Repository userRepository, Doctor.Repository doctorRepository,
                           Appointment.Repository appointmentRepository) {
            this.userRepository = userRepository;
            this.doctorRepository = doctorRepository;
            this.appointmentRepository = appointmentRepository;
        }

        @GetMapping("/stats")
        public Stats stats() {
            Stats s = new Stats();
            s.totalPatients = userRepository.count();
            s.totalDoctors = doctorRepository.count();
            s.totalAppointments = appointmentRepository.count();
            s.pendingAppointments = appointmentRepository.findByStatus(Appointment.Status.PENDING).size();
            s.confirmedAppointments = appointmentRepository.findByStatus(Appointment.Status.CONFIRMED).size();
            return s;
        }
    }
}
