package edu.taylors.io.capstone.eservices.security;

import edu.taylors.io.capstone.eservices.entity.User;
import edu.taylors.io.capstone.eservices.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String studentId)
            throws UsernameNotFoundException {

        return userRepository.findByStudentId(studentId)
                .orElseThrow(() ->
                        new UsernameNotFoundException("User not found with Student ID: " + studentId));
    }
}