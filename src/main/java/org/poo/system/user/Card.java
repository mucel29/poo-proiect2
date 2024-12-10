package org.poo.system.user;


import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Getter;
import lombok.Setter;
import org.poo.io.StateWriter;
import org.poo.system.command.base.Command;
import org.poo.utils.NodeConvertable;

@Getter @Setter
public class Card implements NodeConvertable {

    public enum Type {
        CLASSIC,
        ONE_TIME;

        public Command.Type command() {
            return switch (this) {
                case CLASSIC -> Command.Type.CREATE_CARD;
                case ONE_TIME -> Command.Type.CREATE_ONE_TIME_CARD;
            };
        }
    }


    private Account account;
    private Type cardType;
    private String cardNumber;
    private boolean status;

    public Card(Account account, Type cardType, String cardNumber) {
        this.account = account;
        this.cardType = cardType;
        this.cardNumber = cardNumber;
        this.status = true;
    }

    @Override
    public ObjectNode toNode() {
        ObjectNode root = StateWriter.getMapper().createObjectNode();
        root.put("cardNumber", cardNumber);
        root.put("status", status ? "active" : "frozen");

        return root;
    }

}
