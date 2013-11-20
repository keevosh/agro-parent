package com.agroknow.indexer;

import com.agroknow.domain.InternalFormat;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.nio.charset.Charset;
import java.util.List;
import org.apache.commons.io.FileUtils;
import org.elasticsearch.action.bulk.BulkItemResponse;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequestBuilder;
import org.elasticsearch.client.Client;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

/**
 * BulkIndexWorker is used to parse a list of files and submit them to elasticsearch
 * if they were updated later than indexer's last run (if any).
 *
 * @author aggelos
 * @param <T>
 */
public class BulkIndexWorker<T extends InternalFormat> implements Runnable {

    private static final Logger LOG = LoggerFactory.getLogger(BulkIndexWorker.class);

    private List<File> files;
    private String fileFormat;
    private Charset charset;
    private long lastCheck;
    private Client esClient;
    private ObjectMapper objectMapper;

    private Class<T> fileFormatClass;

    public BulkIndexWorker() {}

    /**
     * BulkIndexWorker is used to parse a list of files and submit them to elasticsearch
     * if they were updated later than indexer's last run (if any). Files are read
     * in UTF-8 charset.
     *
     * @param files The list of Files to process
     * @param fileFormat The data format of those files
     * @param objectMapper  The objectMapper to use to serialize/deserialize json
     * @param esClient The elasticsearch client to use for (bulk) indexing
     */
    public BulkIndexWorker(List<File> files, String fileFormat, ObjectMapper objectMapper, Client esClient) {
        this(files, fileFormat, Charset.forName("UTF-8"), objectMapper, new DateTime().withZone(DateTimeZone.UTC).getMillis(), esClient);
    }

    /**
     * BulkIndexWorker is used to parse a list of files and submit them to elasticsearch
     * if they were updated later than indexer's last run (if any)
     *
     * @param files The list of Files to process
     * @param fileFormat The data format of those files
     * @param charset The charset to read files with
     * @param objectMapper  The objectMapper to use to serialize/deserialize json
     * @param lastCheck The timestamp of indexer's last run
     * @param esClient The elasticsearch client to use for (bulk) indexing
     */
    public BulkIndexWorker(List<File> files, String fileFormat, Charset charset, ObjectMapper objectMapper, long lastCheck, Client esClient) {
        this.init(files, fileFormat, charset, objectMapper, lastCheck, esClient);
    }

    public final void init(List<File> files, String fileFormat, Charset charset, ObjectMapper objectMapper, long lastCheck, Client esClient) {
        this.files = files;
        this.fileFormat = fileFormat;
        this.charset = charset;
        this.objectMapper = objectMapper;
        this.lastCheck = lastCheck;
        this.esClient = esClient;
    }

    /**
     * Create a bulk index request based on the list of files passed to the worker.
     * It also adds metrics about its progress.
     *
     * @throws Exception
     */
    public void index() throws Exception {
        Assert.notNull(this.files);
        Assert.notNull(this.fileFormat);
        Assert.notNull(this.charset);
        Assert.notNull(this.lastCheck);
        Assert.notNull(this.esClient);

        LOG.debug("START bulk indexer");
        BulkRequestBuilder bulkRequest = esClient.prepareBulk();

        // add documents to bulk request
        String source;
        T doc;
        for(File f : files) {
            LOG.debug("PROCESS file: {}", f.getAbsolutePath());

            // check if file changed after lastCheck
            if(this.lastCheck > f.lastModified()) {
                MetricsRegistryHolder.getCounter("FILES[SKIPPED]").inc();
                continue;
            }

            // read the file contents into source string
            source = FileUtils.readFileToString(f, charset);

            // if file is touched after the last check, parse it to Akif.class
            try {
                doc = objectMapper.reader(getFileFormatClass()).readValue(source);
            } catch(IOException ex) {
                LOG.error("File [{}] failed to get parsed: {}", f.getCanonicalPath(), ex.getMessage());
                MetricsRegistryHolder.getCounter("FILES[FAILED]").inc();
                continue;
            }

            //INFO: lastUpdateDate is currently do not get involved when checking dates
            //      for now because it does not contain a datetime value but only a date.
            //      We'll keep this snippet here just in case we agree to change the
            //      lastUpdateDate to datetime. If not, it will be removed in a later commit.
            //
            //// and compare Akif.lastUpdateDate with lastCheck
            //if(this.lastCheck > doc.getLastUpdateDate().getTime()) {
            //    MetricsRegistryHolder.getCounter("FILES[SKIPPED]").inc();
            //    continue;
            //}

            // create an indexRequest and add it to the bulk
            String id = doc.getIdentifier(); // id is also in the filename and can be read with FilenameUtils.getBaseName(f.getAbsolutePath());
            IndexRequestBuilder indexRequestBuilder = esClient.prepareIndex(fileFormat, fileFormat, id)
                                                              .setSource(source);
            bulkRequest.add(indexRequestBuilder);
        }

        // if we added documents in the bulk
        // execute the request and read data
        // from bulkResponse
        if(bulkRequest.numberOfActions() > 0) {
            BulkResponse bulkResponse = bulkRequest.execute().actionGet();
            if (bulkResponse.hasFailures()) {
                for (BulkItemResponse item : bulkResponse.getItems()) {
                    if (item.isFailed()) {
                        LOG.error("Document [{}] failed to get indexed: {}", item.getId(), item.getFailureMessage());
                        MetricsRegistryHolder.getCounter("FILES[FAILED]").inc();
                    } else {
                        MetricsRegistryHolder.getCounter("FILES[INDEXED]").inc();
                    }
                }
            } else {
                MetricsRegistryHolder.getCounter("FILES[INDEXED]").inc(bulkResponse.getItems().length);
            }
        }

        LOG.debug("END bulk indexer");
    }

    public void run() {
        try {
            index();
        } catch (Exception ex) {
            LOG.error(ex.getMessage(), (LOG.isDebugEnabled() ? ex : null));
        }
    }

    private Class<T> getFileFormatClass() {
        if (fileFormatClass == null) {
            this.fileFormatClass = (Class<T>) ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[0];
        }
        return fileFormatClass;
    }

    public void setFiles(List<File> files) {
        this.files = files;
    }

    public void setFileFormat(String fileFormat) {
        this.fileFormat = fileFormat;
    }

    public void setCharset(Charset charset) {
        this.charset = charset;
    }

    public void setLastCheck(long lastCheck) {
        this.lastCheck = lastCheck;
    }

    public void setEsClient(Client esClient) {
        this.esClient = esClient;
    }
}
