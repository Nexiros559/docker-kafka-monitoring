package org.example.repository;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.query.FluxRecord;
import com.influxdb.query.FluxTable;
import org.example.CpuMetric;
import org.example.DiskMetric;
import org.example.RamMetric;
import org.example.SystemMetric;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Repository
public class InfluxRepository {

    private final InfluxDBClient influxDBClient;
    private final String query;
    private final String org;

    public InfluxRepository(InfluxDBClient influxDBClient) {
        this.influxDBClient = influxDBClient;
        this.org = System.getenv().getOrDefault("INFLUX_ORG", "monitoring-org");
        String bucket = System.getenv().getOrDefault("INFLUX_BUCKET", "monitoring");

        this.query = """
                import "date"
                from(bucket: "%s")
                |> range(start: -24h) 
                |> filter(fn: (r) => r["_measurement"] == "cpu_metrics" or r["_measurement"] == "ram_metrics" or r["_measurement"] == "disk_metrics")
                
                // 1. SÉCURITÉ : On ne garde que ce qui vient de 'localhost' (Ton constat)
                |> filter(fn: (r) => r["host"] == "localhost")
                
                // 2. SÉCURITÉ : On convertit TOUT en nombre à virgule (float)
                // Cela règle le conflit entre les "bytes" (entiers) et la "température" (virgule)
                |> map(fn: (r) => ({ r with _value: float(v: r._value) }))
                
                |> map(fn: (r) => ({ r with _time: date.truncate(t: r._time, unit: 1s) }))
                |> map(fn: (r) => ({
                    r with _field: 
                        if r._measurement == "cpu_metrics" and r._field == "temperature" then "cpu_temp"
                        else if r._measurement == "cpu_metrics" and r._field == "load_percent" then "cpu_load"
                        else if r._measurement == "ram_metrics" and r._field == "used_bytes" then "ram_used"
                        else if r._measurement == "ram_metrics" and r._field == "total_bytes" then "ram_total"
                        else if r._measurement == "ram_metrics" and r._field == "available_bytes" then "ram_free"
                        else if r._measurement == "disk_metrics" and r._field == "used_bytes" then "disk_used"
                        else if r._measurement == "disk_metrics" and r._field == "total_bytes" then "disk_total"
                        else if r._measurement == "disk_metrics" and r._field == "free_bytes" then "disk_free"
                        else r._field
                }))
                
                // 3. SÉCURITÉ : On fusionne tout
                |> group(columns: ["_time", "host"])
                |> pivot(rowKey:["_time"], columnKey: ["_field"], valueColumn: "_value")
                """.formatted(bucket);
    }

    public List<SystemMetric> getMetrics() {
        List<SystemMetric> metrics = new ArrayList<>();
        System.out.println("--- DÉBUT REQUÊTE INFLUX ---"); // Log de début

        try {
            List<FluxTable> tables = this.influxDBClient.getQueryApi().query(query, org);

            for (FluxTable table : tables) {
                for (FluxRecord record : table.getRecords()) {

                    // DEBUG : On affiche la ligne brute dans la console Docker
                    System.out.println("Record trouvé : " + record.getValues());

                    // 1. CPU (Déjà en float dans la base, donc facile)
                    Double cpuTemp = getDoubleOrDefault(record, "cpu_temp");
                    Double cpuLoad = getDoubleOrDefault(record, "cpu_load");

                    // 2. RAM (On récupère le Double, puis on convertit en long)
                    long ramUsed = getDoubleOrDefault(record, "ram_used").longValue();
                    long ramTotal = getDoubleOrDefault(record, "ram_total").longValue();

                    // Pour la RAM libre, on gère le cas où elle serait vide (calcul automatique)
                    Double ramFreeDbl = (Double) record.getValueByKey("ram_free");
                    long ramAvailable = (ramFreeDbl != null) ? ramFreeDbl.longValue() : (ramTotal - ramUsed);

                    // 3. DISQUE (Idem : Double -> long)
                    long diskUsed = getDoubleOrDefault(record, "disk_used").longValue();
                    long diskTotal = getDoubleOrDefault(record, "disk_total").longValue();

                    // Pour le Disque libre, idem
                    Double diskFreeDbl = (Double) record.getValueByKey("disk_free");
                    long diskFree = (diskFreeDbl != null) ? diskFreeDbl.longValue() : (diskTotal - diskUsed);

                    Instant time = record.getTime();

                    // 4. Création des objets
                    CpuMetric cpu = new CpuMetric(cpuTemp, cpuLoad);
                    RamMetric ram = new RamMetric(ramTotal, ramUsed, ramAvailable);
                    DiskMetric disk = new DiskMetric(diskTotal, diskUsed, diskFree);

                    metrics.add(new SystemMetric(time.toEpochMilli(), cpu, disk, ram));
                }
            }
        } catch (Exception e) {
            System.err.println("ERREUR INFLUX : " + e.getMessage());
            e.printStackTrace();
        }

        System.out.println("--- FIN REQUÊTE : " + metrics.size() + " résultats ---");
        return metrics;
    }

    // Petites méthodes utilitaires pour éviter les NullPointerException
    private Double getDoubleOrDefault(FluxRecord record, String key) {
        Object val = record.getValueByKey(key);
        if (val instanceof Number) {
            return ((Number) val).doubleValue();
        }
        return 0.0; // Valeur par défaut si manquant
    }

    private long getLongOrDefault(FluxRecord record, String key) {
        Object val = record.getValueByKey(key);
        if (val instanceof Number) {
            return ((Number) val).longValue();
        }
        return 0L; // Valeur par défaut si manquant
    }
}