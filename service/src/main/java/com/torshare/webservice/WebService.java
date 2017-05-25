package com.torshare.webservice;

/**
 * Created by tyler on 11/30/16.
 */

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import com.torshare.tools.DataSources;
import com.torshare.tools.Tools;
import com.torshare.torrent.LibtorrentEngine;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.slf4j.LoggerFactory;
import spark.Spark;

import java.io.File;
import java.io.IOException;

import static spark.Spark.staticFiles;

public class WebService {

    static Logger log = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);

    @Option(name="-loglevel", usage="Sets the log level [INFO, DEBUG, etc.]")
    private String loglevel = "INFO";

    @Option(name="-ui_dist",usage="The location of the ui dist folder.")
    private File uiDist = new File("../ui/dist");

    @Option(name="-torrents_dir",usage="Scans and watches a torrent directory, and adds them to the DB")
    private File torrentsDir;

    @Option(name="-ssl",usage="The location of the java keystore .jks file.")
    private File jks;

    @Option(name="-liquibase", usage="Run liquibase changesets")
    private Boolean liquibase = false;

    @Option(name="-peer_scanner", usage="Scans for peers in batches")
    private Boolean peerScanner = false;

    public void doMain(String[] args) throws IOException {

        if (args != null) {
            parseArguments(args);
        }

        log.setLevel(Level.toLevel(loglevel));
        log.getLoggerContext().getLogger("org.eclipse.jetty").setLevel(Level.OFF);
        log.getLoggerContext().getLogger("spark.webserver").setLevel(Level.OFF);



        if (liquibase) {
            Tools.runLiquibase();
        }

        LibtorrentEngine lte = LibtorrentEngine.INSTANCE;

        // Add torrents to DB
        if (torrentsDir != null) {
            Tools.scanAndWatchTorrentsDir(torrentsDir);
        }

        if (jks != null) {
            Spark.secure(jks.getAbsolutePath(), "changeit", null,null);
            DataSources.SSL = true;
        }
        staticFiles.externalLocation(uiDist.getAbsolutePath());
        staticFiles.expireTime(DataSources.EXPIRE_SECONDS);

        // Set up endpoints
        Endpoints.status();
        Endpoints.search();
        Endpoints.upload();
        Endpoints.detail();
        Endpoints.download();
        Endpoints.export();
        Endpoints.exceptions();

        // Scan through torrent DB to pick up peer counts
        if (peerScanner) {
            lte.scanForPeers();
        }

    }

    private void parseArguments(String[] args) {
        CmdLineParser parser = new CmdLineParser(this);

        try {
            parser.parseArgument(args);
        } catch (CmdLineException e) {
            // if there's a problem in the command line,
            // you'll get this exception. this will report
            // an error message.
            System.err.println(e.getMessage());
            System.err.println("java -jar reddit-history.jar [options...] arguments...");
            // print the list of available options
            parser.printUsage(System.err);
            System.err.println();
            System.exit(0);

            return;
        }
    }

    public static void main(String[] args) throws Exception {
        new WebService().doMain(args);
    }

}

