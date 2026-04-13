package com.example.demo.repository;

import com.example.demo.entity.MEg;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface MEgRepository extends JpaRepository<MEg, Long> {
    // Одна запись — для формы заявления
    Optional<MEg> findFirstByIin(Long iin);

    // Все записи — для карточки клиента (может быть несколько заявлений)
    List<MEg> findAllByIin(Long iin);
}