package swgohManager.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import swgohManager.model.Guildes;

import java.util.Optional;

@Repository
public interface GuildesRepository extends JpaRepository<Guildes, Long> {
    Optional<Guildes> findByGuildId(String guildId);
    void deleteByGuildId(String guildId);
}