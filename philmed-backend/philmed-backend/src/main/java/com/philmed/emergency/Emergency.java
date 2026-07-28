package com.philmed.emergency;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Emergency info is intentionally simple and public (no auth, no DB writes) —
 * a patient in a crisis should never be blocked by a login screen.
 * Update the phone numbers below to your real ones before deploying.
 */
public class Emergency {

    public static class Contact {
        public String label;
        public String phone;

        public Contact(String label, String phone) {
            this.label = label;
            this.phone = phone;
        }
    }

    @RestController
    @RequestMapping("/api/emergency")
    public static class Controller {

        @GetMapping("/contacts")
        public List<Contact> contacts() {
            return List.of(
                    new Contact("Philmed Emergency Line", "+254700111222"),
                    new Contact("Ambulance", "+254700999888"),
                    new Contact("Poison Control", "+254700555444")
            );
        }
    }
}
