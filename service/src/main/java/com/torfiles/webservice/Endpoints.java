package com.torfiles.webservice;

import ch.qos.logback.classic.Logger;
import com.torfiles.db.Actions;
import com.torfiles.db.Tables;
import com.torfiles.tools.DataSources;
import com.torfiles.tools.Tools;
import com.torfiles.types.TorrentDetail;
import org.eclipse.jetty.http.HttpStatus;
import org.javalite.activejdbc.LazyList;
import org.slf4j.LoggerFactory;

import javax.servlet.MultipartConfigElement;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

import static spark.Spark.*;

/**
 * Created by tyler on 11/30/16.
 */
public class Endpoints {

    public static Logger log = (Logger) LoggerFactory.getLogger(Endpoints.class);

    public static void status() {

        get("/hello", (req, res) -> "hello");

        get("/version", (req, res) -> {
            return "{\"version\":\"" + DataSources.PROPERTIES.getProperty("version") + "\"}";
        });

        get("/table_counts", (req, res) -> {
            return Tables.TableCountView.findAll().toJson(false);
        });

        before((req, res) -> {
            res.header("Access-Control-Allow-Origin", "*");
            res.header("Access-Control-Allow-Credentials", "true");
            res.header("Access-Control-Allow-Headers", "Origin, content-type,X-Requested-With");
            res.header("Access-Control-Allow-Methods", "POST, GET, PUT, DELETE, OPTIONS");
            Tools.dbInit();
        });

        after((req, res) -> {
            res.header("Content-Encoding", "gzip");
            Tools.dbClose();
        });

    }

    public static void exceptions() {

        exception(Exception.class, (e, req, res) -> {
            e.printStackTrace();
            res.status(HttpStatus.BAD_REQUEST_400);
            res.body(e.getMessage());
            Tools.dbClose();
        });
    }

    public static void search() {

        get("/search", (req, res) -> {

            String nameParam = req.queryParams("q");
            String nameTokens = Tools.tokenizeNameQuery(nameParam);

            String limitParam = req.queryParams("limit");
            Integer limit = (limitParam != null) ? Integer.valueOf(limitParam) : 25;

            String pageParam = req.queryParams("page");
            Integer page = (pageParam != null) ? Integer.valueOf(pageParam) : 1;

            Integer offset = (page - 1) * limit;

            LazyList<Tables.FileView> files = (nameTokens != null) ?

                    Tables.FileView.findBySQL(
                            "with cte as ( select * from file_view " +
                                    "where text_search @@ to_tsquery('" + nameTokens + "')) " +
                                    "select * from cte " +
                                    "order by peers desc, size_bytes desc limit " + limit + " offset " + offset) :
                    Tables.FileView.findAll().orderBy("peers desc, size_bytes desc").limit(limit).offset(offset);

            return Tools.wrapPagedResults(files.toJson(false),
                    999L,
                    page);
        });

   }

    public static void detail() {
        get("/torrent_detail/:info_hash", (req, res) -> {
            String infoHash = req.params(":info_hash");

            Tables.Torrent torrent = Tables.Torrent.findFirst("info_hash = ?", infoHash);
            LazyList<Tables.FileView> files = Tables.FileView.where("info_hash = ?", infoHash).orderBy("path");

            TorrentDetail td = TorrentDetail.create(torrent, files);

            return td.json();

        });

    }

}
