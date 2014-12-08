package net.devops.watcher.service;

import com.sun.nio.file.ExtendedWatchEventModifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class Watcher implements Runnable {

    private static Logger logger = LoggerFactory.getLogger(Watcher.class);

    private Path rootPath = Paths.get("dir").toAbsolutePath();
    private File rootFile;
    private WatchService watchService;
    private Map<Path, String> files = new ConcurrentHashMap<>();

    public Watcher() {
        logger.info("watcher start");

        rootFile = rootPath.toFile();
        rootFile.mkdirs();

        register(rootPath);
        initFiles();
    }

    private void register(Path path) {
        if (!rootFile.exists()) {
            rootFile.mkdirs();
        }
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

    private void initFiles() {
        try {
            Files.walkFileTree(rootPath, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult preVisitDirectory(Path path, BasicFileAttributes attrs)
                        throws IOException {
                    path.toAbsolutePath().toFile().listFiles(pathname -> {
                        if (pathname.isFile()) {
                            logger.info("add file {} from path {}", pathname.toString(), pathname.getName());
                            files.put(pathname.toPath(), pathname.getName());
                        }
                        return false;
                    });
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void reInit(Path path) {
        if (!rootFile.exists()) {
            rootFile.mkdirs();
        }
        try {
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
                reInit(rootPath);
                WatchKey key = watchService.poll(10, TimeUnit.SECONDS);
                if (key == null) {
                    continue;
                }
                if (key.isValid()) {
                    List<WatchEvent<?>> events = key.pollEvents();
                    for (WatchEvent<?> event : events) {
                        WatchEvent<Path> watchEvent = (WatchEvent<Path>) event;

                        Path toWatch = rootPath.resolve(watchEvent.context()).toAbsolutePath();
                        logger.info("count {}\tkind {}\tcontext {}\t", watchEvent.count(), watchEvent.kind(), toWatch);
                        checkEvent(watchEvent, toWatch);
                    }
                }
                boolean reset = key.reset();
                logger.info("key reset {}", reset);
            } catch (InterruptedException e) {
                logger.error("cannot get event", e);
            }
        }
    }

    private void checkEvent(WatchEvent<Path> watchEvent, Path path) {
        if (watchEvent.kind().equals(StandardWatchEventKinds.ENTRY_DELETE)) {
            files.entrySet().stream().filter(file -> file.getKey().startsWith(path)).forEach(file -> {
                files.remove(file.getKey());
                logger.info("remove file {} from path {}", file.getKey(), path);
            });
        }
        if (path.toFile().isFile()) {
            files.put(path, path.getFileName().toString());
            logger.info("add file {} from path {}", path.getFileName().toString(), path);
        }
    }
}
