package swgohManager.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;

import swgohManager.model.GuildBilanActuel;

public interface GuildBilanActuelRepository extends JpaRepository<GuildBilanActuel, Long> {
	@Transactional
    @Modifying
	void deleteByGuildId(String guildId);
}