package net.devops.watcher;

import net.devops.watcher.service.Watcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class Main {

    private static Logger logger = LoggerFactory.getLogger(Main.class);
    private Thread thread;

    public Main() {
        Watcher watcher = new Watcher();
        thread = new Thread(watcher);
        thread.start();
    }

    public static void main(String[] args) throws IOException {
        logger.info("starting");
        new Main();

        System.in.read();
        logger.info("stoping");
    }
}
