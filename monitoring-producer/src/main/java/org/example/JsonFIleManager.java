package org.example;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.example.SystemMetric;
/**
 * Gère la persistance des données au format JSON.
 * <p>
 * Cette classe utilise la bibliothèque Jackson pour lire et écrire des listes
 * d'objets {@link CpuMetric} dans un fichier local.
 * </p>
 */
class JsonFileManager {
    private final String filePath;
    ObjectMapper objectMapper;
    List<CpuMetric> metrics = null;
    File file = null;

    /**
     * Initialise le gestionnaire et vérifie l'existence du fichier.
     * Si le fichier n'existe pas, il est créé avec une liste vide [].
     * * @param filePath Le chemin vers le fichier JSON de stockage.
     */
    JsonFileManager(String filePath) {
        this.filePath = filePath;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.enable(SerializationFeature.INDENT_OUTPUT);

        File file = new File(this.filePath);

        if(!file.exists()){
            try {
                objectMapper.writeValue(file, new ArrayList<>());
                System.out.println("Fichier crée avec succès" + filePath);
            }catch(IOException e){
                e.printStackTrace();
                System.err.println("Erreur lors de la création du fichier JSON" + e.getMessage());
            }
        }
    }

    /**
     * Enregistre une nouvelle métrique en l'ajoutant à l'historique existant.
     * La méthode charge le fichier, ajoute l'élément, puis réécrit tout le contenu.
     * * @param metric L'objet {@link CpuMetric} à sauvegarder.
     */
    public void saveMetric(SystemMetric metric) {
        List<SystemMetric> metrics;
        File file = new File(this.filePath);

        // 1. On essaie de lire l'existant
        try {
            if (file.exists() && file.length() > 0) {
                metrics = objectMapper.readValue(file, new TypeReference<List<SystemMetric>>() {});
            } else {
                metrics = new ArrayList<>();
            }
        } catch (IOException e) {
            // Si le fichier est corrompu ou illisible, on repart à zéro
            metrics = new ArrayList<>();
            System.err.println("Erreur lecture, création d'une nouvelle liste : " + e.getMessage());
        }

        // 2. On ajoute la nouvelle mesure
        metrics.add(metric);

        // 3. On réécrit tout le fichier
        try {
            objectMapper.writeValue(file, metrics);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Lit l'intégralité du fichier JSON et le transforme en liste d'objets Java.
     * * @return Une {@link List} contenant toutes les métriques enregistrées.
     * Retourne une liste vide en cas d'erreur de lecture.
     */
    public List<SystemMetric> readAllMetrics() {
        File file = new File(this.filePath);
        if (!file.exists()) return new ArrayList<>();

        try {
            return objectMapper.readValue(file, new TypeReference<List<SystemMetric>>() {});
        } catch (IOException e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }
}