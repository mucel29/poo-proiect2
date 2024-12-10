package org.poo.io;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.node.ArrayNode;
import lombok.Getter;

import java.io.File;
import java.io.IOException;

public final class StateWriter {

    @Getter
    private static ObjectMapper mapper = new ObjectMapper();
    private static ArrayNode outputNodes = mapper.createArrayNode();

    private StateWriter() { }

    /**
     * Resets writer's state and deletes all objects
     */
    public static void reset() {
        mapper = new ObjectMapper();
        outputNodes = mapper.createArrayNode();
    }

    /**
     * Adds a node to the output buffer
     * @param node the json node to add
     */
    public static void write(final JsonNode node) {
        if (node == null) {
            return;
        }

        outputNodes.add(node);
    }

    /**
     * Dumps the output buffer into the given file
     * @param f the file in which to write the objects
     * @throws IOException in case of an IO error
     */
    public static void dump(final File f) throws IOException {
        System.out.println("Dumping to " + f.getAbsolutePath());
        ObjectWriter writer = mapper.writerWithDefaultPrettyPrinter();
        writer.writeValue(f, outputNodes);
    }
}