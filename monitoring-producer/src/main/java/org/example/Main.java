package org.example;


/**
 * Cette fonction est le principale du code.
 * Elle permet de créer un fichier "metrics.json".
 * Elle lance l'application générale avec app.start()
 * */

public class Main {
    public static void main(String[] args) {
        // 1. Création des services
        JsonFileManager fileManager = new JsonFileManager("metrics.json");
        MonitorApp consumer = new MonitorApp(fileManager); // Le Consommateur
        OshiEngine producer = new OshiEngine();           // Le Producteur

        // 2. Câblage (On branche le consommateur sur le producteur)
        producer.addListener(consumer);

        // 3. Démarrage
        System.out.println("Démarrage du système de monitoring...");
        producer.start();


    }
}