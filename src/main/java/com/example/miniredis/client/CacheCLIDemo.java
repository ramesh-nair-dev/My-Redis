package com.example.miniredis.client;

import com.example.miniredis.models.CacheValue;
import com.example.miniredis.persistence.PersistenceManager;
import com.example.miniredis.store.CacheStore;
import com.example.miniredis.strategy.EvictionPolicy;
import com.example.miniredis.strategy.LRUCachePolicy;

import java.util.Scanner;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Logger;

public class CacheCLIDemo {

    private static final Logger logger = Logger.getLogger(CacheCLIDemo.class.getName());

    public static void main(String[] args) throws InterruptedException {
        // LRU policy, maxCapacity = 5
        EvictionPolicy<String> evictionPolicy = new LRUCachePolicy<>();
        PersistenceManager<String, String> persistenceManager = null;

        CacheStore<String, String> cacheStore = new CacheStore<>(5, evictionPolicy, persistenceManager);

        Scanner scanner = new Scanner(System.in);
        System.out.println("=== Mini-Redis CLI Demo ===");
        System.out.println("Commands: SET key value, GET key, DELETE key, TTL key, STATS, MULTI, EXIT");

        while (true) {
            System.out.print("> ");
            String input = scanner.nextLine().trim();
            if (input.isEmpty()) continue;

            String[] parts = input.split(" ", 3);
            String command = parts[0].toUpperCase();

            try {
                switch (command) {
                    case "SET":
                        if (parts.length < 3) {
                            System.out.println("Usage: SET key value");
                            break;
                        }
                        cacheStore.set(parts[1], parts[2], 5000); // TTL 5 sec
                        System.out.println("OK");
                        break;

                    case "GET":
                        if (parts.length < 2) {
                            System.out.println("Usage: GET key");
                            break;
                        }
                        String value = cacheStore.get(parts[1]);
                        if (value != null) {
                            System.out.println(AnsiColors.GREEN + "GET hit: " + parts[1] + " = " + value + AnsiColors.RESET);
                        } else {
                            System.out.println(AnsiColors.YELLOW + "GET miss: " + parts[1] + AnsiColors.RESET);
                        }
                        break;

                    case "DELETE":
                        if (parts.length < 2) {
                            System.out.println("Usage: DELETE key");
                            break;
                        }
                        cacheStore.delete(parts[1]);
                        System.out.println("Deleted " + parts[1]);
                        break;

                    case "TTL":
                        if (parts.length < 2) {
                            System.out.println("Usage: TTL key");
                            break;
                        }
                        CacheValue<String> wrapper = cacheStore.getCache().get(parts[1]);
                        if (wrapper != null) {
                            long ttlRemaining = wrapper.getTtlMillis() - (System.currentTimeMillis() - wrapper.getCreationTime());
                            System.out.println(ttlRemaining > 0 ?
                                    ttlRemaining + " ms remaining" :
                                    AnsiColors.CYAN + "Expired" + AnsiColors.RESET);
                        } else {
                            System.out.println("Key not found");
                        }
                        break;

                    case "STATS":
                        System.out.println("Cache Stats:");
                        System.out.println("Size: " + cacheStore.getCache().size());
                        System.out.println("Keys: " + cacheStore.listKeys());
                        break;

                    case "MULTI":
                        runMultiThreadDemo(cacheStore);
                        break;

                    case "EXIT":
                        cacheStore.shutdown();
                        System.out.println("Exiting CLI demo.");
                        return;

                    default:
                        System.out.println("Unknown command: " + command);
                }
            } catch (Exception e) {
                logger.warning("Error: " + e.getMessage());
            }
        }
    }

    private static void runMultiThreadDemo(CacheStore<String, String> cacheStore) throws InterruptedException {
        System.out.println("=== Running Multi-threaded Cache Demo ===");

        Thread writer1 = new Thread(() -> writerTask("Writer-1", cacheStore));
        Thread writer2 = new Thread(() -> writerTask("Writer-2", cacheStore));

        Thread reader1 = new Thread(() -> readerTask("Reader-1", cacheStore));
        Thread reader2 = new Thread(() -> readerTask("Reader-2", cacheStore));

        writer1.start();
        writer2.start();
        reader1.start();
        reader2.start();

        writer1.join();
        writer2.join();
        reader1.join();
        reader2.join();

        System.out.println("=== Multi-threaded Demo Completed ===");
        System.out.println("Final Cache Keys: " + cacheStore.listKeys());
    }

    private static void writerTask(String name, CacheStore<String, String> cacheStore) {
        String[] keys = {"A", "B", "C"};
        String[] values = {"A-Value", "B-Value", "C-Value"};
        for (int i = 0; i < keys.length; i++) {
            cacheStore.set(keys[i], values[i], 5000);
            System.out.println(name + " SET " + keys[i]);
            sleepRandom();
        }
        // Optional extra key to trigger eviction
        cacheStore.set("D", "D-Value", 5000);
        System.out.println(name + " SET D");
    }

    private static void readerTask(String name, CacheStore<String, String> cacheStore) {
        String[] keys = {"A", "B", "C", "D", "E"};
        for (int i = 0; i < keys.length; i++) {
            String value = cacheStore.get(keys[i]);
            if (value != null) {
                System.out.println(AnsiColors.GREEN + name + " GET " + keys[i] + " = " + value + AnsiColors.RESET);
            } else {
                System.out.println(AnsiColors.YELLOW + name + " GET " + keys[i] + " = NULL" + AnsiColors.RESET);
            }
            sleepRandom();
        }
    }

    private static void sleepRandom() {
        try {
            Thread.sleep(ThreadLocalRandom.current().nextInt(100, 201)); // 100-200ms
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
