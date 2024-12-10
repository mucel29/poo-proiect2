package org.poo.system.exchange;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class Pair <T, V>{
    private T first;
    private V second;

    public Pair(T first, V second) {
        this.first = first;
        this.second = second;
    }

}
