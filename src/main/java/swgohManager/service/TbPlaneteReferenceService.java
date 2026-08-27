package swgohManager.service;

import swgohManager.model.TbPlaneteReference;
import swgohManager.repository.TbPlaneteReferenceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TbPlaneteReferenceService {

    private final TbPlaneteReferenceRepository tbPlaneteReferenceRepository;

    private record Entree(int planeteId, int phase, int conflict, boolean bonus, String nom,
                           int toonClassic, int toonSpecial, int shipStrike, int vagues, int ms,
                           long winToonClassic, long winToonSpecial, long winShip, long gpCombat) {}

    // Référentiel figé — ne devrait jamais évoluer.
    private static final List<Entree> DONNEES = List.of(
            new Entree(1,1,1,false,"R5 - Coruscant",4,0,1,9,0,200000,0,400000,1200000),
            new Entree(2,1,2,false,"R5 - Mustafar",4,0,1,9,0,200000,0,400000,1200000),
            new Entree(3,1,3,false,"R5 - Corellia",3,0,1,7,1,200000,0,400000,1000000),
            new Entree(4,2,1,false,"R6 - Bracca",3,0,1,7,1,250000,0,500000,1250000),
            new Entree(5,2,2,false,"R6 - Geonosis",4,0,1,9,0,250000,0,500000,1500000),
            new Entree(6,2,3,false,"R6 - Felucia",4,0,1,9,0,250000,0,500000,1500000),
            new Entree(7,3,1,false,"R7 - Kashyyyk",3,0,1,7,1,341250,0,682500,1706250),
            new Entree(8,3,1,true,"Zeffo",2,1,1,5,1,341250,1023750,682500,2388750),
            new Entree(9,3,2,false,"R7 - Dathomir",4,0,0,8,1,341250,0,682500,1365000),
            new Entree(10,3,3,false,"R7 - Tatooine",3,0,1,7,2,341250,0,682500,1706250),
            new Entree(11,4,1,false,"R8 - Lothal",3,0,1,7,0,493594,0,987188,2467970),
            new Entree(12,4,2,false,"R8 - Station Medicale Haven Class",4,0,0,8,1,493594,0,987188,1974376),
            new Entree(13,4,3,false,"R8 - Kessel",3,0,1,7,1,493594,0,987188,2467970),
            new Entree(14,4,3,true,"Mandalore",2,1,1,5,0,493594,1480782,987188,3455158),
            new Entree(15,5,1,false,"R9 - Anneau de Kafrene",4,0,1,9,0,721744,0,1443488,4330464),
            new Entree(16,5,2,false,"R9 - Malachor",4,0,0,8,0,721744,0,1443488,2886976),
            new Entree(17,5,3,false,"R9 - Vandor",3,0,1,7,1,721744,0,1443488,3608720),
            new Entree(18,6,1,false,"R9 - Scarif",4,0,1,9,0,1151719,0,2303438,6910314),
            new Entree(19,6,2,false,"R9 - Death Star",4,0,1,9,0,1151719,0,2303438,6910314),
            new Entree(20,6,3,false,"R9 - Hoth",4,0,1,9,0,1151719,0,2303438,6910314)
    );

    @Transactional
    public String seedDonnees() {
        Map<Integer, TbPlaneteReference> existantes = tbPlaneteReferenceRepository.findAll().stream()
                .collect(Collectors.toMap(TbPlaneteReference::getPlaneteId, p -> p));

        List<TbPlaneteReference> aSauvegarder = new ArrayList<>();

        for (Entree e : DONNEES) {
            TbPlaneteReference p = existantes.get(e.planeteId());
            if (p == null) {
                p = new TbPlaneteReference();
                p.setPlaneteId(e.planeteId());
                existantes.put(e.planeteId(), p);
            }
            p.setPhase(e.phase());
            p.setConflict(e.conflict());
            p.setBonus(e.bonus());
            p.setPlaneteName(e.nom());
            p.setToonStrikeClassic(e.toonClassic());
            p.setToonStrikeSpecial(e.toonSpecial());
            p.setShipStrike(e.shipStrike());
            p.setVagues(e.vagues());
            p.setMs(e.ms());
            p.setWinToonClassic(e.winToonClassic());
            p.setWinToonSpecial(e.winToonSpecial());
            p.setWinShip(e.winShip());
            p.setGpCombat(e.gpCombat());
            aSauvegarder.add(p);
        }

        tbPlaneteReferenceRepository.saveAll(aSauvegarder);
        String resultat = String.format("%d planète(s) de référence TB chargée(s)", aSauvegarder.size());
        log.info(resultat);
        return resultat;
    }
}