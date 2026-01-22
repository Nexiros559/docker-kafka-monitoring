package org.example;

/**
 * Orchestrateur principal de l'application de monitoring.
 * <p>
 * Cette classe gère la boucle principale, l'intervalle de mesure (1s)
 * et la fréquence de génération des rapports statistiques (5s).
 * </p>
 */
public class MonitorApp implements MonitorListener {

    private final JsonFileManager jsonFileManager;

    public MonitorApp(JsonFileManager jsonFileManager) {
        this.jsonFileManager = jsonFileManager;
    }

    // C'est ici que la magie opère : on réagit à l'arrivée d'une mesure
    @Override
    public void onMeasureReceived(SystemMetric metric) {
        // 1. On sauvegarde
        jsonFileManager.saveMetric(metric);

        // 2. On affiche (Log console pour vérifier le mode Headless)
        System.out.println("--- Nouvelle Mesure ---");
        System.out.printf("CPU: %.1f°C | RAM Used: %d Go | Disk Free: %d Go%n",
                metric.cpu().temperature(),
                metric.ram().used() / (1024*1024*1024), // Conversion rapide en Go
                metric.disk().freeSpace() / (1024*1024*1024)
        );
    }
}
