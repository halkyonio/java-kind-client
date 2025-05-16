package dev.snowdrop.container.output;

import java.util.function.Consumer;

public abstract class BaseConsumer<SELF extends BaseConsumer<SELF>> implements Consumer<OutputFrame> {

    public boolean isRemoveColorCodes() {
        return removeColorCodes;
    }

    public void setRemoveColorCodes(boolean removeColorCodes) {
        this.removeColorCodes = removeColorCodes;
    }

    private boolean removeColorCodes = true;

    public SELF withRemoveAnsiCodes(boolean removeAnsiCodes) {
        this.removeColorCodes = removeAnsiCodes;
        return (SELF) this;
    }
}
