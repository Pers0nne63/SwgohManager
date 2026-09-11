package swgohManager.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import swgohManager.model.Guildes;

@Repository
public interface GuildesRepository extends JpaRepository<Guildes, Long> {
    Optional<Guildes> findByGuildId(String guildId);
    
    @Transactional
    @Modifying
    void deleteByGuildId(String guildId);
}