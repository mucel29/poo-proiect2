package org.poo.system.commerce;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Getter;
import lombok.Setter;
import org.poo.io.StateWriter;
import org.poo.system.exchange.Amount;
import org.poo.utils.NodeConvertable;

import java.util.ArrayList;
import java.util.List;

@Getter
public class CommerciantSpending implements NodeConvertable {

    private final String name;
    private final List<String> employees = new ArrayList<>();
    private final List<String> managers = new ArrayList<>();

    @Setter
    private Amount received;

    public CommerciantSpending(
            final String name,
            final String currency
    ) {
        this.name = name;
        this.received = new Amount(0.0, currency);
    }


    /**
     * Converts the implementing instance to an {@code ObjectNode}
     *
     * @return the instance's JSON representation
     */
    @Override
    public ObjectNode toNode() {
        ObjectNode root = StateWriter.getMapper().createObjectNode();
        root.put("commerciant", name);

        ArrayNode employeesArray = root.putArray("employees");
        ArrayNode managersArray = root.putArray("managers");

        for (String employee : employees) {
            employeesArray.add(employee);
        }
        for (String manager : managers) {
            managersArray.add(manager);
        }

        root.put("total received", received.total());

        return root;
    }
}
