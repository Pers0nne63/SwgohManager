package swgohManager.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import swgohManager.model.GuildBilanActuel;

public interface GuildBilanActuelRepository extends JpaRepository<GuildBilanActuel, Long> {
	void deleteByGuildId(String guildId);
}