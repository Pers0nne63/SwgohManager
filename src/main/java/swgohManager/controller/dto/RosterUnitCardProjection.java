package swgohManager.controller.dto;

/**
 * Une unité du roster d'un joueur, avec tout ce qu'il faut pour dessiner son icône.
 * Les alias de la requête native (findRosterCartesJoueur) doivent correspondre exactement aux getters.
 */
public interface RosterUnitCardProjection {
    String getBaseId();
    String getLibelle();
    /** Nom du bundle ae2 (thumbnailName sans le préfixe "tex."), null si inconnu. */
    String getPortrait();
    /** ATK, HLR, SUP, TNK. */
    String getRole();
    /** 1 = LS, 2 = DS, 3 = Neutre. */
    Integer getForceAlignment();
    Boolean getLegend();
    String getUltimate();
    Boolean getConquete();
    Integer getEtoiles();
    Integer getGear();
    Integer getRelic();
    Long getNbZetas();
    Long getNbOmicrons();
    Integer getCombatType();
    String getIdUnit();
    String getFactions();
    Boolean getLeader();
}