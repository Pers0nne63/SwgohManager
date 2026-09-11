package swgohManager.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import swgohManager.model.GuildComparisonResult;

public interface GuildComparisonResultRepository extends JpaRepository<GuildComparisonResult, Long> {
    Optional<GuildComparisonResult> findByGuildIdB(String guildIdB);
}
