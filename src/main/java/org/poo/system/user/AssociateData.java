package org.poo.system.user;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.poo.io.StateWriter;
import org.poo.system.exchange.Amount;
import org.poo.utils.NodeConvertable;

public record AssociateData(
        User associate,
        BusinessAccount.Role role,
        Amount spent,
        Amount deposited
) implements NodeConvertable {

    /**
     * Converts the implementing instance to an {@code ObjectNode}
     *
     * @return the instance's JSON representation
     */
    @Override
    public ObjectNode toNode() {
        ObjectNode output = StateWriter.getMapper().createObjectNode();
        output.put("username", associate.getLastName() + " " + associate.getFirstName());
        output.put("spent", spent.total());
        output.put("deposited", deposited.total());

        return output;
    }
}
