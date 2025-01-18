package org.poo.system;

import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.poo.io.StateWriter;
import org.poo.utils.NodeConvertable;
import org.poo.utils.ReflectionUtils;

import java.lang.reflect.Field;
import java.util.List;

public interface Transaction extends NodeConvertable, Cloneable, Comparable<Transaction> {
    enum TransferType {
        SENT,
        RECEIVED;

        @Override
        public String toString() {
            return name().toLowerCase();
        }

    }

    /**
     * @return a clone of the transaction
     */
    Transaction clone();

    /**
     * @return the transaction's timestamp
     */
    int getTimestamp();

    /**
     * @return the commerciant associated or null if it's not present
     */
    String getCommerciant();

    /**
     * @return the transaction's total or 0.0 if it's not a payment or transfer
     */
    double getAmount();

    class Base implements Transaction {
        private final String description;
        private final int timestamp;

        public Base(
                final String description,
                final int timestamp
        ) {
            this.description = description;
            this.timestamp = timestamp;
        }


        /**
         * {@inheritDoc}
         */
        @Override
        public ObjectNode toNode() {
            ObjectNode root = StateWriter.getMapper().createObjectNode();

            // (won't work if the transaction doesn't directly extend Base)
            // Set super(base) class fields first
            for (Field superField : Base.class.getDeclaredFields()) {
                ReflectionUtils.addField(root, superField, this);
            }
            // Set instance class fields
            for (Field instanceField : this.getClass().getDeclaredFields()) {
                ReflectionUtils.addField(root, instanceField, this);
            }

            return root;
        }


        /**
         * {@inheritDoc}
         */
        @Override
        public Transaction clone() {
            Transaction to;
            try {
                to = this
                        .getClass()
                        .getConstructor(String.class, int.class)
                        .newInstance(this.description, this.timestamp);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            // Clone superclass fields
            ReflectionUtils.copyFields(
                    this.getClass().getSuperclass().getDeclaredFields(),
                    to.getClass().getSuperclass().getDeclaredFields(),
                    this,
                    to
            );

            // Copy instance fields
            ReflectionUtils.copyFields(
                    this.getClass().getDeclaredFields(),
                    to.getClass().getDeclaredFields(),
                    this,
                    to
            );

            return to;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int getTimestamp() {
            return timestamp;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int compareTo(final Transaction o) {
            return this.timestamp - o.getTimestamp();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String getCommerciant() {
            Field f = ReflectionUtils.findField("commerciant", this);
            if (f != null) {
                f.setAccessible(true);
                try {
                    return f.get(this).toString();
                } catch (Exception e) {
                    return null;
                }
            }

            return null;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public double getAmount() {
            Field f = ReflectionUtils.findField("amount", this);
            if (f != null) {
                f.setAccessible(true);
                try {
                    return f.getDouble(this);
                } catch (Exception e) {
                    return 0.0;
                }
            }

            return 0.0;
        }

    }

    /**
     * A transaction between 2 accounts
     */
    @Setter @Accessors(chain = true)
    class Transfer extends Base {
        private String senderIBAN;
        private String receiverIBAN;
        private String currency;
        private TransferType transferType;

        private double amount;

        public Transfer(
                final String description,
                final int timestamp
        ) {
            super(description, timestamp);
        }

        /**
         * {@inheritDoc}
         * </br>
         * Need to change the total field generated by {@code Base}
         * </br>
         * Also remove the currency field, it's included in the total now
         */
        @Override
        public ObjectNode toNode() {
            ObjectNode root = super.toNode();
            root.put("amount", amount + " " + currency);
            root.remove("currency");
            return root;
        }

    }

    /**
     * A transaction to a commerciant
     */
    @Setter @Accessors(chain = true)
    class Payment extends Base {
        private String commerciant;
        private double amount;

        public Payment(
                final String description,
                final int timestamp
        ) {
            super(description, timestamp);
        }

    }

    /**
     * An operation executed on a {@code Card}
     */
    @Setter @Accessors(chain = true)
    class CardOperation extends Base {
        private String account;
        private String cardHolder;
        private String card;

        public CardOperation(
                final String description,
                final int timestamp
        ) {
            super(description, timestamp);
        }

    }

    /**
     * A transaction split between multiple accounts
     */
    @Setter @Accessors(chain = true)
    class SplitPayment extends Base {
        private String splitPaymentType;
        private String currency;
        private Double amount;
        private List<Double> amountForUsers;
        private List<String> involvedAccounts;

        @Getter
        private String error = null;

        public SplitPayment(
                final String description,
                final int timestamp
        ) {
            super(description, timestamp);
        }

    }

    /**
     * A plan upgrade transaction
     */
    @Setter @Accessors(chain = true)
    class PlanUpgrade extends Base {
        private String accountIBAN;
        private String newPlanType;

        public PlanUpgrade(
                final String description,
                final int timestamp
        ) {
            super(description, timestamp);
        }
    }

    /**
     * A cash withdrawal transaction
     */
    @Setter @Accessors(chain = true)
    class CashWithdrawal extends Base {
        private double amount;

        public CashWithdrawal(
                final String description,
                final int timestamp
        ) {
            super(description, timestamp);
        }
    }

    /**
     * An interest rate payout
     */
    @Setter @Accessors(chain = true)
    class InterestPayout extends Base {
        private double amount;
        private String currency;

        public InterestPayout(
                final String description,
                final int timestamp
        ) {
            super(description, timestamp);
        }
    }

}

