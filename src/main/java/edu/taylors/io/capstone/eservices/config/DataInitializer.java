package edu.taylors.io.capstone.eservices.config;

import edu.taylors.io.capstone.eservices.entity.User;
import edu.taylors.io.capstone.eservices.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class DataInitializer {
    //This class for init database users

    @Bean
    CommandLineRunner initDatabase(UserRepository userRepository,
                                   PasswordEncoder passwordEncoder) {
        return args -> {
            // This is 3 users (2 students , 1 Staff) we using them for testing
            if (userRepository.count() == 0) {
                User student1 = User.builder()
                        .studentId("TP012345")
                        .email("TestSTU1@gmail.com")
                        .password(passwordEncoder.encode("123456"))
                        .role("ROLE_STUDENT")
                        .enabled(true)
                        .twoFactorEnabled(false)
                        .firstName("Mohammed")
                        .lastName("Awadallah")
                        .phoneNumber("+966501234567")
                        .address("Riyadh, Saudi Arabia")
                        .build();

                User student2 = User.builder()
                        .studentId("TP067890")
                        .email("Test@hotmail.com")
                        .password(passwordEncoder.encode("123456"))
                        .role("ROLE_STUDENT")
                        .enabled(true)
                        .twoFactorEnabled(false)
                        .firstName("Ahmad")
                        .lastName("Ali")
                        .phoneNumber("+966507654321")
                        .address("Jeddah, Saudi Arabia")
                        .build();

                User staff = User.builder()
                        .studentId("STAFF001")
                        .email("staff@taylors.edu.my")
                        .password(passwordEncoder.encode("123456"))
                        .role("ROLE_STAFF")
                        .enabled(true)
                        .twoFactorEnabled(false)
                        .firstName("Dr. Sarah")
                        .lastName("Johnson")
                        .phoneNumber("+60123456789")
                        .address("Kuala Lumpur, Malaysia")
                        .build();

                userRepository.save(student1);
                userRepository.save(student2);
                userRepository.save(staff);

                System.out.println("=================================");
                System.out.println("✅ Test users created with profile data!");
                System.out.println("Student 1: TP012345 / 123456");
                System.out.println("Student 2: TP067890 / 123456");
                System.out.println("Staff: STAFF001 / 123456");
                System.out.println("=================================");
            }
        };
    }
}