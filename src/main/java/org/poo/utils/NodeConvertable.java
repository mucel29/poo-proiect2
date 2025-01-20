package org.poo.utils;

import com.fasterxml.jackson.databind.node.ObjectNode;

public interface NodeConvertable {

    /**
     * Converts the implementing instance to an {@code ObjectNode}
     *
     * @return the instance's JSON representation
     */
    ObjectNode toNode();

}
