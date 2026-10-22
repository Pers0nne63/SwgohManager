/* Onglet Roster : un clic sur le portrait agrandit la card et charge stats + mods (fragment HTML, mis en cache). */
(function () {
    var grille = document.getElementById('rosterGrid');
    if (!grille) return;

    var playerId = grille.dataset.playerId;
    var cache = {};        // idUnit -> HTML déjà chargé
    var ouverte = null;    // card actuellement agrandie

    function fermer(card) {
        card.classList.remove('col-span-full', 'roster-open');
        card.querySelector('.roster-detail').classList.add('hidden');
    }

    function ouvrir(card) {
        var zone = card.querySelector('.roster-detail');
        var idUnit = card.dataset.idUnit;
        card.classList.add('col-span-full', 'roster-open');
        zone.classList.remove('hidden');

        if (cache[idUnit]) {
            zone.innerHTML = cache[idUnit];
            return;
        }
        zone.innerHTML = '<p class="text-slate-400 text-sm italic">Chargement...</p>';
        fetch('/joueur/' + encodeURIComponent(playerId) + '/unite/' + encodeURIComponent(idUnit) + '/detail')
            .then(function (res) {
                if (!res.ok) throw new Error('HTTP ' + res.status);
                return res.text();
            })
            .then(function (html) {
                cache[idUnit] = html;
                zone.innerHTML = html;
            })
            .catch(function () {
                zone.innerHTML = '<p class="text-rose-400 text-sm">Impossible de charger le détail de cette unité.</p>';
            });
    }

    grille.addEventListener('click', function (event) {
        var unite = event.target.closest('.unit');
        if (!unite) return;
        var card = unite.closest('.roster-item');
        if (!card || !card.dataset.idUnit) return;

        if (ouverte && ouverte !== card) fermer(ouverte);

        if (card.classList.contains('roster-open')) {
            fermer(card);
            ouverte = null;
        } else {
            ouvrir(card);
            ouverte = card;
            card.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
        }
    });
})();