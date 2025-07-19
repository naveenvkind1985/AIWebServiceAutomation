/*
package models;

import java.nio.file.*;
import java.util.HashSet;

public class FeatureFileWatcher {
    private final Path featuresDir;
    private final HashSet<Path> processedFiles = new HashSet<>(); // To track processed files

    public FeatureFileWatcher(Path featuresDir) {
        this.featuresDir = featuresDir;
    }

    public void startWatching() {
        try (WatchService watchService = FileSystems.getDefault().newWatchService()) {
            // Register the directory to watch for new or modified feature files
            featuresDir.register(watchService,
                    StandardWatchEventKinds.ENTRY_CREATE,
                    StandardWatchEventKinds.ENTRY_MODIFY);
            System.out.println("Watching for changes in: " + featuresDir.toString());

            while (true) {
                WatchKey key;
                try {
                    key = watchService.take(); // Wait for key to be available
                } catch (InterruptedException e) {
                    System.err.println("Watcher interrupted: " + e.getMessage());
                    return; // Exit if interrupted
                }

                for (WatchEvent<?> event : key.pollEvents()) {
                    WatchEvent.Kind<?> kind = event.kind();
                    if (kind == StandardWatchEventKinds.OVERFLOW) {
                        System.out.println("Overflow event occurred, ignoring.");
                        continue; // Ignore overflow events
                    }

                    @SuppressWarnings("unchecked")
                    WatchEvent<Path> ev = (WatchEvent<Path>) event;
                    Path fileName = ev.context();
                    Path fullPath = featuresDir.resolve(fileName); // Get full path
                    System.out.println("File changed: " + fileName);

                    // Check if the changed file ends with .feature
                    if (fileName.toString().endsWith(".feature")) {
                        // Only process if not already processed
                        if (!processedFiles.contains(fullPath)) {
                            processedFiles.add(fullPath); // Mark file as processed
                            System.out.println("Detected new/modified feature file: " + fullPath);
                            // Call the StepDefinitionGenerator to process the feature file
                            StepDefinitionGenerator.execute(fullPath.toString());
                        } else {
                            System.out.println("File already processed: " + fullPath);
                        }
                    } else {
                        System.out.println("Ignored file: " + fileName);
                    }
                }
                key.reset(); // Reset the key for the next event
            }
        } catch (Exception e) {
            System.err.println("Error watching directory: " + e.getMessage());
            
        }
    }
}*/
