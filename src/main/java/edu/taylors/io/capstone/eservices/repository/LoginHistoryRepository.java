package edu.taylors.io.capstone.eservices.repository;

import edu.taylors.io.capstone.eservices.entity.LoginHistory;
import edu.taylors.io.capstone.eservices.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LoginHistoryRepository extends JpaRepository<LoginHistory, Long> {

    List<LoginHistory> findByUserOrderByLoginTimeDesc(User user);

    List<LoginHistory> findTop10ByUserOrderByLoginTimeDesc(User user);
}