package dev.asdf00.mc.advcomp.items;

public abstract class BaseKeycardItem extends BaseAcDyableItem {
    public BaseKeycardItem(Properties pProperties) {
        super(pProperties);
    }

    public abstract void onKeycardSwiped(BaseKeycardItem is);
}
