package org.apache.kafka.connect.document;

import org.apache.kafka.common.utils.AppInfoParser;
import org.apache.kafka.connect.connector.Task;
import org.apache.kafka.connect.errors.ConnectException;
import org.apache.kafka.connect.source.SourceConnector;
import org.apache.kafka.connect.util.ConnectorUtils;
import org.apache.kafka.connect.utils.StringUtils;

import java.util.*;


/**
 * DocumentSourceConnector implements the connector interface
 * to send extracted contents to Kafka
 *
 * @author Sergio Spinatelli
 */
public class DocumentSourceConnector extends SourceConnector {
    public static final String SCHEMA_NAME = "schema.name";
    public static final String TOPIC = "topic";
    public static final String FILES = "files";
    public static final String CONTENT_EXTRACTOR = "content.extractor";
    public static final String OUTPUT_TYPE = "output.type";
    public static final String PREFIX = "files.prefix";
    private static final List<String> ALLOWED_OUTPUTS = Arrays.asList("text", "text_xml", "xml_text", "getXHTML");

    private String schema_name;
    private String topic;
    private String filenames;
    private String content_extractor;
    private String output_type;
    private String filePrefix;


    /**
     * Get the version of this connector.
     *
     * @return the version, formatted as a String
     */
    @Override
    public String version() {
        return AppInfoParser.getVersion();
    }


    /**
     * Start this Connector. This method will only be called on a clean Connector, i.e. it has
     * either just been instantiated and initialized or {@link #stop()} has been invoked.
     *
     * @param props configuration settings
     */
    @Override
    public void start(Map<String, String> props) {
        schema_name = props.get(SCHEMA_NAME);
        topic = props.get(TOPIC);
        filenames = props.get(FILES);
        content_extractor = props.get(CONTENT_EXTRACTOR);
        output_type = props.get(OUTPUT_TYPE);
        filePrefix = props.get(PREFIX);

        if (schema_name == null || schema_name.isEmpty())
            throw new ConnectException("missing schema.name");

        if (topic == null || topic.isEmpty())
            throw new ConnectException("missing topic");

        if (filenames == null || filenames.isEmpty())
            throw new ConnectException("missing files");

        if (content_extractor == null || content_extractor.isEmpty())
            content_extractor = "tika";

        if (output_type == null || output_type.isEmpty())
            output_type = "text_xml";

        if (!ALLOWED_OUTPUTS.contains(output_type))
            throw new ConnectException("output.type has to be one of [text, text_xml, xml_text, getXHTML]");

        if (filePrefix == null || filePrefix.isEmpty())
            filePrefix = "";
    }


    /**
     * Returns the Task implementation for this Connector.
     *
     * @return tha Task implementation Class
     */
    @Override
    public Class<? extends Task> taskClass() {
        return DocumentSourceTask.class;
    }


    /**
     * Returns a set of configurations for the Task based on the current configuration.
     * It always creates a single set of configurations.
     *
     * @param maxTasks maximum number of configurations to generate
     * @return configurations for the Task
     */
    @Override
    public List<Map<String, String>> taskConfigs(int maxTasks) {
        ArrayList<Map<String, String>> configs = new ArrayList<>();
        List<String> files = Arrays.asList(filenames.split(","));
        int numGroups = Math.min(files.size(), maxTasks);
        List<List<String>> filesGrouped = ConnectorUtils.groupPartitions(files, numGroups);

        for (int i = 0; i < numGroups; i++) {
            Map<String, String> config = new HashMap<>();
            config.put(FILES, StringUtils.join(filesGrouped.get(i), ","));
            config.put(SCHEMA_NAME, schema_name);
            config.put(TOPIC, topic);
            config.put(CONTENT_EXTRACTOR, content_extractor);
            config.put(OUTPUT_TYPE, output_type);
            config.put(PREFIX, filePrefix);
            configs.add(config);
        }
        return configs;
    }


    /**
     * Stop this connector.
     */
    @Override
    public void stop() {

    }

}
