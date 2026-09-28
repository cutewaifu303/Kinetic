package secret.kinetic.api.events;

import secret.kinetic.api.events.annotations.Cancellable;

public abstract class CancellableEvent implements Event, Cancellable {
    


    private boolean cancelled;

    




    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    




    @Override
    public boolean isCancelled() {
        return cancelled;
    }
}
