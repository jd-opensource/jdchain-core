package com.jd.blockchain.consensus.raft.server;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import utils.serialize.json.JSONSerializeUtils;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;

public class RaftSnapshotFile {

    private static final Logger LOGGER = LoggerFactory.getLogger(RaftSnapshotFile.class);

    private static final String SNAPSHOT_FILE_NAME = "snap";

    private String filePath;

    public RaftSnapshotFile(String parentPath) {
        this.filePath = parentPath + File.separator + SNAPSHOT_FILE_NAME;
    }

    public boolean save(RaftSnapshotData data) {
        String serializeToJSON = JSONSerializeUtils.serializeToJSON(data);
        try {
            FileUtils.writeStringToFile(new File(filePath), serializeToJSON);
            return true;
        } catch (IOException e) {
            LOGGER.error("save snapshot failed", e);
            return false;
        }
    }

    public RaftSnapshotData load() throws IOException {
        final String jsonStr = FileUtils.readFileToString(new File(filePath));
        if (!StringUtils.isBlank(jsonStr)) {
            return JSONSerializeUtils.deserializeAs(jsonStr, RaftSnapshotData.class);
        }
        throw new IOException("load snapshot error. file: " + filePath + ",content: " + jsonStr);
    }

    public String getFilePath() {
        return filePath;
    }

    public String getName() {
        return SNAPSHOT_FILE_NAME;
    }

    static class RaftSnapshotData implements Serializable {

        private static final long serialVersionUID = 3759164370894729258L;

        private long height;

        public RaftSnapshotData(long height) {
            this.height = height;
        }

        public long getHeight() {
            return height;
        }

        public void setHeight(long height) {
            this.height = height;
        }
    }


}
