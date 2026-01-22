package org.example;

public class MainConsumer {
    public static void main(String[] args) {
        System.out.println("Démarrage du Consumer Kafka...");

        ConsumerJava consumer = new ConsumerJava();

        // Shutdown hook pour arrêter proprement avec Ctrl+C
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\nArrêt du consumer...");
            consumer.stop();
        }));

        // Démarrage de la consommation
        consumer.start();
    }
}
