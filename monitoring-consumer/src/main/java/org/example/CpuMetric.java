package org.example;

/**
 * Représente une mesure instantanée des performances du processeur.
 * Ce record est utilisé pour le transport des données vers le stockage JSON.
 *
 * @param temperature La température du processeur en degrés Celsius.
 * @param cpuLoad La charge du processeur (entre 0.0 et 1.0).
 * @param timestamp Le moment de la mesure en millisecondes (Epoch time).
 * @param isOverheating Indique si la température dépasse le seuil critique.
 * @param isOverloaded Indique si la charge dépasse le seuil critique.
 */
public record CpuMetric(double temperature, double cpuLoad, long timestamp,
                        boolean isOverheating, boolean isOverloaded) {

    // --- CONSTANTES (Pour ne pas les avoir en dur partout) ---
    private static final double TEMP_LIMIT = 95.0;
    private static final double LOAD_LIMIT = 0.85;

    /**
     * Constructeur "Simplifié"
     * On ne lui donne que les valeurs brutes, il calcule le reste.
     */
    public CpuMetric(double temperature, double cpuLoad) {
        this(
                temperature,
                cpuLoad,
                System.currentTimeMillis(), // Il met l'heure tout seul
                temperature > TEMP_LIMIT,   // Il décide tout seul si ça chauffe
                cpuLoad > LOAD_LIMIT        // Il décide tout seul si c'est chargé
        );
    }
}

