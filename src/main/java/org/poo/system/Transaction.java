package org.poo.system;

import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.poo.io.StateWriter;
import org.poo.utils.NodeConvertable;

import java.lang.reflect.Field;

@Getter
@Setter
@Accessors(chain = true)
public class Transaction implements NodeConvertable, Cloneable {

    public enum Type {
        SENT,
        RECEIVED,
        PAYMENT,
        OPERATION,
        UNKNOWN;

        @Override
        public String toString() {
            return name().toLowerCase();
        }
    }


    // Common
    private Type transferType;
    private Integer timestamp;
    private String description;

    // Card creation
    private String account;
    private String cardHolder;
    private String card;

    // Payment / transfer
    private Double amount;

    // Transfer
    private String senderIBAN;
    private String receiverIBAN;
    private Exchange.Currency currency;

    // Payment
    private String commerciant;

    public Transaction(String description, int timestamp) {
        this.description = description;
        this.timestamp = timestamp;
        this.transferType = Type.UNKNOWN;
    }


    @Override
    public ObjectNode toNode() {
        ObjectNode root = StateWriter.getMapper().createObjectNode();
        try {
            for (Field field : this.getClass().getDeclaredFields()) {
                if (field.get(this) == null) {
                    continue;
                }

                // Ignore currency, it's added to the amount
                if (field.equals(Transaction.class.getDeclaredField("currency"))) {
                    continue;
                }

                // No need to print the transactionType if it's not sent / received
                if (field.equals(Transaction.class.getDeclaredField("transferType"))

                ) {
                    if (this.transferType == Transaction.Type.SENT || this.transferType == Transaction.Type.RECEIVED) {
                        root.put(field.getName(), field.get(this).toString());
                    }
                    continue;
                }

                if (field.equals(Transaction.class.getDeclaredField("amount"))) {
                    if (this.transferType == Transaction.Type.SENT
                            || this.transferType == Transaction.Type.RECEIVED
                    ) {
                        root.put(field.getName(), (Double) field.get(this) + " " + this.currency);
                        continue;
                    }

                }

                if (field.getType().isAssignableFrom(String.class)) {
                    root.put(field.getName(), (String) field.get(this));
                } else if (field.getType().isAssignableFrom(Double.class)) {
                    root.put(field.getName(), (Double) field.get(this));
                } else if (field.getType().isAssignableFrom(Integer.class)) {
                    root.put(field.getName(), (Integer) field.get(this));
                } else {
                    throw new RuntimeException("Could not match type of field `" + field.getName() + "` [" + field.getName() + "]");
                }


            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }

        return root;
    }

    @Override
    public Transaction clone() {
        try {
            return (Transaction) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }
}
