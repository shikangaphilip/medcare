package com.philmed;

import com.philmed.doctor.Doctor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

/**
 * Entry point. Spring Boot component-scans everything under com.philmed,
 * so every controller/service/repository below (however the file is organized)
 * is picked up automatically.
 */
@SpringBootApplication
public class PhilmedApplication {

    public static void main(String[] args) {
        SpringApplication.run(PhilmedApplication.class, args);
    }

    /**
     * Seeds a handful of doctors on first run so the frontend has real data
     * to book against immediately — no manual data entry required before you
     * can click through the site. Runs once: skips entirely if the table
     * already has rows, so it's safe to leave in place permanently.
     */
    @Bean
    public CommandLineRunner seedDoctors(Doctor.Repository doctorRepository) {
        return args -> {
            if (doctorRepository.count() > 0) {
                return;
            }
            doctorRepository.save(new Doctor("Dr. David Njoroge", "General Practitioner", "MBChB, University of Nairobi", 14, "KMPDC-GP-1001"));
            doctorRepository.save(new Doctor("Dr. Amina Yusuf", "General Practitioner", "MBChB, Moi University", 8, "KMPDC-GP-1002"));
            doctorRepository.save(new Doctor("Dr. Sarah Wanjiru", "Cardiology", "MMed Cardiology, Aga Khan University", 11, "KMPDC-CD-2001"));
            doctorRepository.save(new Doctor("Dr. Faith Achieng", "Obstetrics", "MMed O&G, Moi University", 9, "KMPDC-OB-2002"));
            doctorRepository.save(new Doctor("Dr. Grace Mumbi", "Gynaecology", "MMed O&G, University of Nairobi", 10, "KMPDC-GY-2003"));
            doctorRepository.save(new Doctor("Dr. James Mutua", "Paediatrics", "MMed Paediatrics, UoN", 13, "KMPDC-PD-2004"));
            doctorRepository.save(new Doctor("Dr. Peter Kamau", "Pulmonology", "MMed Internal Medicine, UoN", 12, "KMPDC-PU-2005"));
            doctorRepository.save(new Doctor("Dr. Lucy Chebet", "Neurology", "MMed Neurology, Moi University", 9, "KMPDC-NE-2006"));
            doctorRepository.save(new Doctor("Dr. Brian Otieno", "Orthopaedics", "MMed Orthopaedics, UoN", 15, "KMPDC-OR-2007"));
            doctorRepository.save(new Doctor("Dr. Naomi Wafula", "Dermatology", "MMed Dermatology, Aga Khan University", 7, "KMPDC-DM-2008"));
            doctorRepository.save(new Doctor("Dr. Samuel Kiptoo", "Ophthalmology", "MMed Ophthalmology, UoN", 10, "KMPDC-OP-2009"));
            doctorRepository.save(new Doctor("Dr. Ruth Nyambura", "ENT (Ear, Nose & Throat)", "MMed ENT, Moi University", 8, "KMPDC-EN-2010"));
            doctorRepository.save(new Doctor("Dr. Elijah Mwangi", "Endocrinology", "MMed Internal Medicine, UoN", 11, "KMPDC-ED-2011"));
            doctorRepository.save(new Doctor("Dr. Christine Njeri", "Oncology", "MMed Oncology, Aga Khan University", 14, "KMPDC-ON-2012"));
        };
    }
}
