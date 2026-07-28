package com.philmed.appointment;

import com.philmed.doctor.Doctor;
import com.philmed.user.User;
import jakarta.persistence.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * The whole "appointment" domain in one file: entity, repository, service,
 * booking-request DTO, response DTO, and the REST endpoints. Referenced
 * elsewhere as Appointment, Appointment.Repository, Appointment.Service,
 * Appointment.Controller.
 */
@Entity
@Table(name = "appointments")
public class Appointment {

    public enum CareType { GENERAL, SPECIALIST }
    public enum ConsultMode { IN_PERSON, VIDEO }
    public enum Status { PENDING, CONFIRMED, CANCELLED, COMPLETED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // EAGER on purpose: this app never runs inside a long-lived Hibernate
    // session, so a LAZY proxy here would throw LazyInitializationException
    // the moment the controller (or Jackson) touches patient/doctor after
    // the repository call returns. EAGER avoids that entirely for a
    // relationship this small.
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "patient_id", nullable = false)
    private User patient;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "doctor_id", nullable = false)
    private Doctor doctor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CareType careType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ConsultMode consultMode = ConsultMode.IN_PERSON;

    @Column(nullable = false)
    private LocalDate appointmentDate;

    @Column(nullable = false)
    private String appointmentTime; // e.g. "10:00 AM" — matches the booking form's dropdown

    @Column(length = 1000)
    private String notes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.PENDING;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public Appointment() {
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getPatient() { return patient; }
    public void setPatient(User patient) { this.patient = patient; }

    public Doctor getDoctor() { return doctor; }
    public void setDoctor(Doctor doctor) { this.doctor = doctor; }

    public CareType getCareType() { return careType; }
    public void setCareType(CareType careType) { this.careType = careType; }

    public ConsultMode getConsultMode() { return consultMode; }
    public void setConsultMode(ConsultMode consultMode) { this.consultMode = consultMode; }

    public LocalDate getAppointmentDate() { return appointmentDate; }
    public void setAppointmentDate(LocalDate appointmentDate) { this.appointmentDate = appointmentDate; }

    public String getAppointmentTime() { return appointmentTime; }
    public void setAppointmentTime(String appointmentTime) { this.appointmentTime = appointmentTime; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }

    public interface Repository extends JpaRepository<Appointment, Long> {
        List<Appointment> findByPatientId(Long patientId);
        List<Appointment> findByDoctorId(Long doctorId);
        List<Appointment> findByStatus(Status status);
    }

    /** What the booking form on the frontend submits. */
    public static class BookingRequest {
        @NotNull(message = "Please choose a doctor")
        public Long doctorId;

        @NotNull(message = "Please choose a care type")
        public CareType careType;

        public ConsultMode consultMode;

        @NotNull(message = "Please choose a date")
        @FutureOrPresent(message = "Appointment date can't be in the past")
        public LocalDate appointmentDate;

        @NotBlank(message = "Please choose a time")
        public String appointmentTime;

        public String notes;
    }

    /**
     * What the API actually returns. Never the raw entity — that would
     * either serialize the patient's password hash or blow up trying to
     * serialize a detached proxy. This is the safe, deliberate shape.
     */
    public static class AppointmentResponse {
        public Long id;
        public String doctorName;
        public String specialty;
        public CareType careType;
        public ConsultMode consultMode;
        public LocalDate appointmentDate;
        public String appointmentTime;
        public String notes;
        public Status status;
        public LocalDateTime createdAt;

        public static AppointmentResponse from(Appointment a) {
            AppointmentResponse r = new AppointmentResponse();
            r.id = a.getId();
            r.doctorName = a.getDoctor().getFullName();
            r.specialty = a.getDoctor().getSpecialty();
            r.careType = a.getCareType();
            r.consultMode = a.getConsultMode();
            r.appointmentDate = a.getAppointmentDate();
            r.appointmentTime = a.getAppointmentTime();
            r.notes = a.getNotes();
            r.status = a.getStatus();
            r.createdAt = a.getCreatedAt();
            return r;
        }
    }

    @org.springframework.stereotype.Service
    public static class Service {
        private final Repository repository;
        private final Doctor.Repository doctorRepository;

        @Autowired
        public Service(Repository repository, Doctor.Repository doctorRepository) {
            this.repository = repository;
            this.doctorRepository = doctorRepository;
        }

        public Appointment book(User patient, BookingRequest request) {
            Doctor doctor = doctorRepository.findById(request.doctorId)
                    .orElseThrow(() -> new IllegalArgumentException("Doctor not found: " + request.doctorId));

            Appointment appointment = new Appointment();
            appointment.setPatient(patient);
            appointment.setDoctor(doctor);
            appointment.setCareType(request.careType);
            appointment.setConsultMode(request.consultMode != null ? request.consultMode : ConsultMode.IN_PERSON);
            appointment.setAppointmentDate(request.appointmentDate);
            appointment.setAppointmentTime(request.appointmentTime);
            appointment.setNotes(request.notes);
            appointment.setStatus(Status.PENDING);
            return repository.save(appointment);
        }

        public List<Appointment> findForPatient(Long patientId) {
            return repository.findByPatientId(patientId);
        }

        public List<Appointment> findForDoctor(Long doctorId) {
            return repository.findByDoctorId(doctorId);
        }

        public Optional<Appointment> findById(Long id) {
            return repository.findById(id);
        }

        public Appointment updateStatus(Long id, Status status) {
            Appointment appointment = repository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Appointment not found: " + id));
            appointment.setStatus(status);
            return repository.save(appointment);
        }

        public void cancel(Long id) {
            Appointment appointment = repository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Appointment not found: " + id));
            appointment.setStatus(Status.CANCELLED);
            repository.save(appointment);
        }
    }

    @RestController
    @RequestMapping("/api/appointments")
    public static class Controller {
        private final Service service;
        private final User.Repository userRepository;

        @Autowired
        public Controller(Service service, User.Repository userRepository) {
            this.service = service;
            this.userRepository = userRepository;
        }

        @PostMapping
        public ResponseEntity<?> book(@Valid @RequestBody BookingRequest request, Authentication auth) {
            User patient = userRepository.findByEmail(auth.getName())
                    .orElseThrow(() -> new IllegalStateException("Authenticated user not found"));
            try {
                Appointment created = service.book(patient, request);
                return ResponseEntity.ok(AppointmentResponse.from(created));
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().body(e.getMessage());
            }
        }

        @GetMapping("/my")
        public List<AppointmentResponse> myAppointments(Authentication auth) {
            User patient = userRepository.findByEmail(auth.getName())
                    .orElseThrow(() -> new IllegalStateException("Authenticated user not found"));
            return service.findForPatient(patient.getId()).stream()
                    .map(AppointmentResponse::from)
                    .toList();
        }

        /** A doctor's own schedule. Any authenticated user can call this today —
         *  tighten to "this doctor's own account" once doctors have logins. */
        @GetMapping("/doctor/{doctorId}")
        public List<AppointmentResponse> forDoctor(@PathVariable Long doctorId) {
            return service.findForDoctor(doctorId).stream()
                    .map(AppointmentResponse::from)
                    .toList();
        }

        @GetMapping("/{id}")
        public ResponseEntity<AppointmentResponse> getById(@PathVariable Long id) {
            return service.findById(id)
                    .map(AppointmentResponse::from)
                    .map(ResponseEntity::ok)
                    .orElseGet(() -> ResponseEntity.notFound().build());
        }

        @PutMapping("/{id}/status")
        public ResponseEntity<?> updateStatus(@PathVariable Long id, @RequestBody StatusUpdateRequest body) {
            try {
                return ResponseEntity.ok(AppointmentResponse.from(service.updateStatus(id, body.status)));
            } catch (IllegalArgumentException e) {
                return ResponseEntity.notFound().build();
            }
        }

        @DeleteMapping("/{id}")
        public ResponseEntity<Void> cancel(@PathVariable Long id) {
            service.cancel(id);
            return ResponseEntity.noContent().build();
        }

        public static class StatusUpdateRequest {
            public Status status;
        }
    }
}
