package org.poo.system.user;


import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Getter;
import lombok.Setter;
import org.poo.io.StateWriter;
import org.poo.system.command.base.Command;
import org.poo.utils.NodeConvertable;

@Getter
public class Card implements NodeConvertable {

    public enum Type {
        CLASSIC,
        ONE_TIME;

        /**
         * Retrieves the command type corresponding to the card's type
         * @return {@code Command.Type} corresponding to the card type
         */
        public Command.Type command() {
            return switch (this) {
                case CLASSIC -> Command.Type.CREATE_CARD;
                case ONE_TIME -> Command.Type.CREATE_ONE_TIME_CARD;
            };
        }
    }


    private final Account account;
    private final User creator;
    private final Type cardType;
    private final String cardNumber;

    @Setter
    private boolean active = true;

    public Card(
            final Account account,
            final User creator,
            final Type cardType,
            final String cardNumber
    ) {
        this.account = account;
        this.creator = creator;
        this.cardType = cardType;
        this.cardNumber = cardNumber;
    }

    /**
     * @return the JSON representation of the {@code Card}
     */
    @Override
    public ObjectNode toNode() {
        ObjectNode root = StateWriter.getMapper().createObjectNode();
        root.put("cardNumber", cardNumber);
        root.put("status", active ? "active" : "frozen");

        return root;
    }

}
