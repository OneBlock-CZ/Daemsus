package cz.projectzet.core.state;

/**
 * This is used to hold the state. Even though thread-safe, it could be written better.
 */
public class StateHolder {

    // I probably should use AtomicReference<State> here, but I do not know how to do it.
    private State state;

    public StateHolder(State state) {
        this.state = state;
    }

    public void setStateOrThrow(State newState, State... expectedStates) {
        synchronized (this) {
            var match = false;

            for (State state : expectedStates) {
                if (this.state == state) {
                    match = true;
                    break;
                }
            }

            if (!match) throw new IllegalStateException("Invalid state transition: " + state + " -> " + newState);

            state = newState;
        }
    }

    /**
     * Please be aware, that until the runnable is executed, the entire holder remains locked to ensure that the state does not change meantime. Try to do it as fast as possible.
     *
     * @param onSuccess      The runnable to execute when the state is correct.
     * @param expectedStates The states that are expected.
     */
    public void requireStates(Runnable onSuccess, State... expectedStates) {
        synchronized (this) {
            var match = false;

            for (State state : expectedStates) {
                if (this.state == state) {
                    match = true;
                    break;
                }
            }

            if (!match) throw new IllegalStateException("Invalid state: " + state);

            onSuccess.run();
        }

    }

    /**
     * Please be aware, that until the runnable is executed, the entire holder remains locked to ensure that the state does not change meantime. Try to do it as fast as possible.
     *
     * @param onSuccess      The runnable to execute when the state is correct.
     * @param expectedStates The states that are expected.
     */
    public boolean onStates(Runnable onSuccess, State... expectedStates) {
        synchronized (this) {
            var match = false;

            for (State state : expectedStates) {
                if (this.state == state) {
                    match = true;
                    break;
                }
            }

            if (!match) return false;

            onSuccess.run();
            return true;
        }

    }

    public void requireStatesOrThrow(State... expectedStates) {
        synchronized (this) {
            var match = false;

            for (State state : expectedStates) {
                if (this.state == state) {
                    match = true;
                    break;
                }
            }

            if (!match) throw new IllegalStateException("Invalid state: " + state);
        }
    }

    public State getState() {
        return state;
    }
}
