package com.philmed.doctor;

import jakarta.persistence.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

/**
 * The whole "doctor" domain in one file: entity, repository, service,
 * and the REST endpoints that expose it. Referenced elsewhere as
 * Doctor, Doctor.Repository, Doctor.Service, Doctor.Controller.
 */
@Entity
@Table(name = "doctors")
public class Doctor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String fullName;

    @Column(nullable = false)
    private String specialty; // e.g. "General Practitioner", "Cardiology"

    @Column(nullable = false)
    private String qualification;

    private int yearsExperience;

    @Column(nullable = false, unique = true)
    private String licenseNumber;

    private boolean available = true;

    public Doctor() {
    }

    public Doctor(String fullName, String specialty, String qualification, int yearsExperience, String licenseNumber) {
        this.fullName = fullName;
        this.specialty = specialty;
        this.qualification = qualification;
        this.yearsExperience = yearsExperience;
        this.licenseNumber = licenseNumber;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    @NotBlank(message = "Full name is required")
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    @NotBlank(message = "Specialty is required")
    public String getSpecialty() { return specialty; }
    public void setSpecialty(String specialty) { this.specialty = specialty; }

    @NotBlank(message = "Qualification is required")
    public String getQualification() { return qualification; }
    public void setQualification(String qualification) { this.qualification = qualification; }

    public int getYearsExperience() { return yearsExperience; }
    public void setYearsExperience(int yearsExperience) { this.yearsExperience = yearsExperience; }

    @NotBlank(message = "License number is required")
    public String getLicenseNumber() { return licenseNumber; }
    public void setLicenseNumber(String licenseNumber) { this.licenseNumber = licenseNumber; }

    public boolean isAvailable() { return available; }
    public void setAvailable(boolean available) { this.available = available; }

    public interface Repository extends JpaRepository<Doctor, Long> {
        List<Doctor> findBySpecialtyIgnoreCase(String specialty);
        List<Doctor> findByAvailableTrue();
    }

    /** Business logic for doctors — sits next to the entity it operates on. */
    @org.springframework.stereotype.Service
    public static class Service {
        private final Repository repository;

        @Autowired
        public Service(Repository repository) {
            this.repository = repository;
        }

        public List<Doctor> findAll() {
            return repository.findAll();
        }

        public List<Doctor> findAvailable() {
            return repository.findByAvailableTrue();
        }

        public List<Doctor> findBySpecialty(String specialty) {
            return repository.findBySpecialtyIgnoreCase(specialty);
        }

        public Optional<Doctor> findById(Long id) {
            return repository.findById(id);
        }

        public Doctor create(Doctor doctor) {
            return repository.save(doctor);
        }

        public Doctor update(Long id, Doctor updated) {
            Doctor existing = repository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Doctor not found: " + id));
            existing.setFullName(updated.getFullName());
            existing.setSpecialty(updated.getSpecialty());
            existing.setQualification(updated.getQualification());
            existing.setYearsExperience(updated.getYearsExperience());
            existing.setAvailable(updated.isAvailable());
            return repository.save(existing);
        }

        public void delete(Long id) {
            repository.deleteById(id);
        }
    }

    /** HTTP endpoints for the doctor domain. Public — patients browse before logging in. */
    @RestController
    @RequestMapping("/api/doctors")
    public static class Controller {
        private final Service service;

        @Autowired
        public Controller(Service service) {
            this.service = service;
        }

        @GetMapping
        public List<Doctor> getAll() {
            return service.findAll();
        }

        @GetMapping("/{id}")
        public ResponseEntity<Doctor> getById(@PathVariable Long id) {
            return service.findById(id)
                    .map(ResponseEntity::ok)
                    .orElseGet(() -> ResponseEntity.notFound().build());
        }

        @GetMapping("/specialty/{specialty}")
        public List<Doctor> getBySpecialty(@PathVariable String specialty) {
            return service.findBySpecialty(specialty);
        }

        @GetMapping("/available")
        public List<Doctor> getAvailable() {
            return service.findAvailable();
        }

        @PostMapping
        public Doctor create(@Valid @RequestBody Doctor doctor) {
            return service.create(doctor);
        }

        @PutMapping("/{id}")
        public ResponseEntity<Doctor> update(@PathVariable Long id, @Valid @RequestBody Doctor doctor) {
            try {
                return ResponseEntity.ok(service.update(id, doctor));
            } catch (IllegalArgumentException e) {
                return ResponseEntity.notFound().build();
            }
        }

        @DeleteMapping("/{id}")
        public ResponseEntity<Void> delete(@PathVariable Long id) {
            service.delete(id);
            return ResponseEntity.noContent().build();
        }
    }
}
