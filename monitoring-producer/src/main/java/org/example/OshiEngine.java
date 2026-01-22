package org.example;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.Sensors;



import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.Properties;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;


import static java.util.concurrent.TimeUnit.SECONDS;

public final class OshiEngine implements MetricEngine{
    private final SystemInfo systemInfo;
    private final CentralProcessor processor;
    private final Sensors sensors;
    private  long [] previousTicks;
    ScheduledExecutorService scheduler;
    List<MonitorListener> listeners = new ArrayList<>();
    private final KafkaProducer<String, String> producer;
    Properties properties = new Properties();
    private final ObjectMapper mapper = new ObjectMapper();




    public OshiEngine(){
        this.systemInfo = new SystemInfo();
        this.processor = systemInfo.getHardware().getProcessor();
        this.sensors = systemInfo.getHardware().getSensors();
        this.previousTicks = processor.getSystemCpuLoadTicks();
        this.scheduler = Executors.newSingleThreadScheduledExecutor();
        properties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "kafka:29092");
        properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        this.producer = new KafkaProducer<>(properties);
    }

    public SystemMetric createFullMeasure(){
        double temp = sensors.getCpuTemperature();
        double load = processor.getSystemCpuLoadBetweenTicks(previousTicks);
        CpuMetric cpu = new CpuMetric(temp, load);

        var memory = systemInfo.getHardware().getMemory();
        RamMetric ram = new RamMetric(
                memory.getTotal(),
                memory.getTotal() - memory.getAvailable(),
                memory.getAvailable()
        );

        var rootDisk = systemInfo.getOperatingSystem().getFileSystem().getFileStores().get(0);
        DiskMetric disk = new DiskMetric(
                rootDisk.getTotalSpace(),
                rootDisk.getTotalSpace() - rootDisk.getFreeSpace(),
                rootDisk.getFreeSpace()
        );

        return new SystemMetric(System.currentTimeMillis(), cpu, disk, ram);
    }




    @Override
    public void addListener(MonitorListener listener){
        listeners.add(listener);
    }

    @Override
    public void start(){

        this.scheduler = Executors.newScheduledThreadPool(1);

        final Runnable instructionsOSHI = new Runnable() {
            @Override
            public void run() {
                // --- 1. CPU (Existant) ---
                double load = processor.getSystemCpuLoadBetweenTicks(previousTicks);
                previousTicks = processor.getSystemCpuLoadTicks();
                double temp = sensors.getCpuTemperature();
                CpuMetric cpu = new CpuMetric(temp, load); // Utilise ton constructeur intelligent

                // --- 2. RAM (Nouveau) ---
                var memory = systemInfo.getHardware().getMemory();
                // On passe Total et Available, le record calcule le Used
                RamMetric ram = new RamMetric(memory.getTotal(), memory.getAvailable());

                // --- 3. DISQUE (Nouveau) ---
                // On prend le premier disque trouvé (souvent C: ou /) pour simplifier
                var fileStores = systemInfo.getOperatingSystem().getFileSystem().getFileStores();
                DiskMetric disk;
                if (!fileStores.isEmpty()) {
                    var rootDisk = fileStores.get(0);
                    disk = new DiskMetric(rootDisk.getTotalSpace(), rootDisk.getUsableSpace());
                } else {
                    // Sécurité si aucun disque n'est trouvé (ex: Docker mal configuré)
                    disk = new DiskMetric(0, 0);
                }

                // --- 4. ASSEMBLAGE ---
                SystemMetric metric = new SystemMetric(System.currentTimeMillis(), cpu, disk, ram);

                try {
                    // C'est cette ligne qui fait le travail :
                    String json = mapper.writeValueAsString(metric);

                    ProducerRecord<String, String> record = new ProducerRecord<>("system-metrics", null, json);
                    producer.send(record, (metadata, exception) -> {
                        if (exception != null) {
                            // Erreur d'envoi
                            System.err.println("Erreur envoi Kafka " + exception.getMessage());
                        }else {
                            System.out.println("Envoyé à Kafka");
                        }
                    });

                } catch (JsonProcessingException e) {
                    e.printStackTrace();
                }

                // --- 5. DIFFUSION (La boucle modifiée) ---
                for (MonitorListener listener : listeners) {
                    // Le listener attend maintenant un SystemMetric !
                    listener.onMeasureReceived(metric);
                }
            }
        };

        this.scheduler.scheduleAtFixedRate(instructionsOSHI, 1, 1, SECONDS);
    }

    @Override
    public void stop(){
        if (scheduler != null){
            scheduler.shutdownNow();
        }
        if (producer != null) {
            producer.close();  // Ferme proprement le producer
        }
    }
}
