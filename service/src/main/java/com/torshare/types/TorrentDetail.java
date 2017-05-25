package com.torshare.types;

import com.torshare.db.Tables;
import org.javalite.activejdbc.LazyList;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by tyler on 12/4/16.
 */
public class TorrentDetail implements JSONWriter {
    private String name, infoHash;
    private Timestamp creationDate;
    private List<FileDetail> files;
    private Integer seeders, peers;
    private Long sizeBytes;


    private TorrentDetail(String name,
                          String infoHash,
                          Timestamp creationDate,
                          List<FileDetail> files,
                          Integer seeders,
                          Integer peers,
                          Long sizeBytes) {
        this.name = name;
        this.infoHash = infoHash;
        this.creationDate = creationDate;
        this.sizeBytes = sizeBytes;
        this.seeders = seeders;
        this.peers = peers;
        this.files = files;


    }

    public static TorrentDetail create(
            Tables.Torrent torrent,
            LazyList<Tables.File> files) {

        List<FileDetail> fileDetails = new ArrayList<>();
        for (Tables.File f : files) {
            fileDetails.add(
                    FileDetail.create(
                            f.getString("path"),
                            f.getLong("size_bytes"),
                            f.getInteger("index_")));
        }

        return new TorrentDetail(torrent.getString("name"),
                torrent.getString("info_hash"),
                torrent.getTimestamp("creation_date"),
                fileDetails,
                torrent.getInteger("seeders"),
                torrent.getInteger("peers"),
                torrent.getLong("size_bytes"));
    }

    public String getName() {
        return name;
    }

    public String getInfoHash() {
        return infoHash;
    }

    public Timestamp getCreationDate() {
        return creationDate;
    }

    public List<FileDetail> getFiles() {
        return files;
    }

    public Integer getSeeders() {
        return seeders;
    }

    public Integer getPeers() {
        return peers;
    }

    public Long getSizeBytes() {
        return sizeBytes;
    }
}
