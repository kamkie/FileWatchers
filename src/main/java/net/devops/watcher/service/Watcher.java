package net.devops.watcher.service;

import com.sun.nio.file.ExtendedWatchEventModifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.List;

public class Watcher implements Runnable {

    private static Logger logger = LoggerFactory.getLogger(Watcher.class);

    private Path rootPath = Paths.get("dir").toAbsolutePath();
    private File rootFile;
    private WatchService watchService;

    public Watcher() {
        logger.info("watcher start");

        rootFile = rootPath.toFile();
        rootFile.mkdirs();

        register(rootPath);
    }

    private void register(Path path) {
        try {
            watchService = rootPath.getFileSystem().newWatchService();
            path.register(watchService, new WatchEvent.Kind<?>[]{StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_DELETE, StandardWatchEventKinds.ENTRY_MODIFY, StandardWatchEventKinds.OVERFLOW},
                    ExtendedWatchEventModifier.FILE_TREE);
            logger.info("path register {}", path);
        } catch (IOException e) {
            logger.error("cannot create watcher service for path{}", path);
            logger.error("cannot create watcher service", e);
        }
    }

    @Override
    public void run() {
        while (true) {
            try {
                WatchKey key = watchService.take();
                if (key == null) {
                    logger.info("continue loop");
                    continue;
                }
                if (key.isValid()) {
                    List<WatchEvent<?>> events = key.pollEvents();
                    for (WatchEvent<?> event : events) {
                        WatchEvent<Path> watchEvent = (WatchEvent<Path>) event;

                        Path toWatch = rootPath.resolve(watchEvent.context()).toAbsolutePath();
                        logger.info("count {}\tkind {}\tcontext {}\t", watchEvent.count(), watchEvent.kind(), toWatch);
                        checkDir(toWatch);
                    }
                }
                boolean reset = key.reset();
                logger.info("key reset {}", reset);
            } catch (InterruptedException e) {
                logger.error("cannot get event", e);
            }
        }
    }

    private void checkDir(Path path) {
        if (path.toFile().isDirectory()) {
            //register(path);
        }
    }
}
