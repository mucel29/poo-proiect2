package org.poo.system;

import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Getter;
import lombok.Setter;
import org.poo.io.StateWriter;
import org.poo.utils.NodeConvertable;

@Getter @Setter
public class Transaction implements NodeConvertable {

    public enum Type {
        SEND,
        RECEIVE,
        UNKNOWN;

        @Override
        public String toString() {
            return name().toLowerCase();
        }
    }

    private Type transactionType;
    private double amount;
    private String sender;
    private String receiver;
    private int timestamp;
    private Exchange.Currency currency;
    private String description;

    public Transaction(String description, int timestamp) {
        this.description = description;
        this.timestamp = timestamp;
        this.transactionType = Type.UNKNOWN;
    }


    @Override
    public ObjectNode toNode() {
        ObjectNode root = StateWriter.getMapper().createObjectNode();
        root.put("description", description);
        root.put("timestamp", timestamp);

        if (transactionType != Type.UNKNOWN) {
            root.put("transferType", transactionType.toString());
            root.put("amount", amount + " " + currency);
            root.put("senderIBAN", sender);
            root.put("receiverIBAN", receiver);
        }

        return root;
    }
}
