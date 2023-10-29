package net.minecraftforge.fml.util.thread;

import net.minecraftforge.fml.LogicalSide;

public class EffectiveSide {

    public static LogicalSide get() {
        //Sad string literal assumption.
        return "Server thread".equals(Thread.currentThread().getName()) ? LogicalSide.SERVER : LogicalSide.CLIENT;
    }
}
