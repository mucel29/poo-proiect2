package org.poo.system.commerce.cashback;

import lombok.Getter;
import lombok.Setter;
import org.poo.system.commerce.Commerciant;

@Getter @Setter
public class CommerciantData {

    private Commerciant.Type type;
    private double spending;
    private int transactionCount;

    public CommerciantData(
            final Commerciant.Type type,
            final double spending,
            final int transactionCount
    ) {
        this.type = type;
        this.spending = spending;
        this.transactionCount = transactionCount;
    }


}
