package com.github.oxo42.stateless4j;

import com.github.oxo42.stateless4j.delegates.Action1;
import com.github.oxo42.stateless4j.delegates.Action2;
import com.github.oxo42.stateless4j.delegates.Func;
import com.github.oxo42.stateless4j.transitions.Transition;
import com.github.oxo42.stateless4j.triggers.TriggerBehaviour;
import com.github.oxo42.stateless4j.triggers.TriggerWithParameters;
import com.github.oxo42.stateless4j.triggers.TriggerWithParameters1;
import com.github.oxo42.stateless4j.triggers.TriggerWithParameters2;
import com.github.oxo42.stateless4j.triggers.TriggerWithParameters3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import static java.util.Objects.requireNonNull;

/**
 * Models behaviour as transitions between a finite set of states
 *
 * @param <S> The type used to represent the states
 * @param <T> The type used to represent the triggers that cause state transitions
 */
public class StateMachine<S, T> {

    protected final StateMachineConfig<S, T> config;
    protected final Func<S> stateAccessor;
    protected final Action1<S> stateMutator;
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final String stateMachineNameLogPrefix;

    protected Action2<S, T> unhandledTriggerAction = (state, trigger) -> {
        throw new IllegalStateException(
                String.format(
                        "No valid leaving transitions are permitted from state '%s' for trigger '%s'. Consider ignoring the trigger.",
                        state, trigger)
        );
    };

    /**
     * Construct a state machine
     *
     * @param initialState The initial state
     */
    public StateMachine(final S initialState) {
        this(initialState, new StateMachineConfig<>());
    }

    /**
     * Construct a state machine
     *
     * @param initialState The initial state
     * @param config       State machine configuration
     */
    public StateMachine(final S initialState, final StateMachineConfig<S, T> config) {
        this.config = config;
        this.stateMachineNameLogPrefix = nameLoggingPrefix(config.getName().orElse(""));

        final StateReference<S, T> reference = new StateReference<>();
        reference.setState(initialState);
        stateAccessor = reference::getState;
        stateMutator = reference::setState;

        if (config.isEntryActionOfInitialStateEnabled()) {
            Transition<S,T> initialTransition = new Transition<>(initialState, initialState, null);
            getCurrentRepresentation().enter(initialTransition);
        }
    }

    /**
     * Construct a state machine with external state storage.
     *
     * @param initialState  The initial state
     * @param stateAccessor State accessor
     * @param stateMutator  State mutator
     * @param config        State machine configuration
     */
    public StateMachine(final S initialState, final Func<S> stateAccessor, final Action1<S> stateMutator, final StateMachineConfig<S, T> config) {
        this.config = config;
        this.stateAccessor = stateAccessor;
        this.stateMutator = stateMutator;
        this.stateMachineNameLogPrefix = nameLoggingPrefix(config.getName().orElse(""));
        stateMutator.doIt(initialState);
    }

    public StateConfiguration<S, T> configure(final S state) {
        return config.configure(state);
    }

    public StateMachineConfig<S, T> configuration() {
        return config;
    }

    /**
     * The current state
     *
     * @return The current state
     */
    public S getState() {
        return stateAccessor.call();
    }

    private void setState(S value) {
        stateMutator.doIt(value);
    }

    /**
     * The currently-permissible trigger values
     *
     * @return The currently-permissible trigger values
     */
    public List<T> getPermittedTriggers() {
        return getCurrentRepresentation().getPermittedTriggers();
    }

    StateRepresentation<S, T> getCurrentRepresentation() {
        StateRepresentation<S, T> representation = config.getRepresentation(getState());
        return representation == null ? new StateRepresentation<>(getState()) : representation;
    }

    /**
     * Transition from the current state via the specified trigger.
     * The target state is determined by the configuration of the current state.
     * Actions associated with leaving the current state and entering the new one
     * will be invoked
     *
     * @param trigger The trigger to fire
     */
    public void fire(T trigger) {
        publicFire(trigger);
    }

    /**
     * Transition from the current state via the specified trigger.
     * The target state is determined by the configuration of the current state.
     * Actions associated with leaving the current state and entering the new one
     * will be invoked.
     *
     * @param trigger The trigger to fire
     * @param arg0    The first argument
     * @param <TArg0> Type of the first trigger argument
     */
    public <TArg0> void fire(
            final TriggerWithParameters1<TArg0, S, T> trigger,
            final TArg0 arg0) {
        requireNonNull(trigger, "trigger is null");
        publicFire(trigger.getTrigger(), arg0);
    }

    /**
     * Transition from the current state via the specified trigger.
     * The target state is determined by the configuration of the current state.
     * Actions associated with leaving the current state and entering the new one
     * will be invoked.
     *
     * @param trigger The trigger to fire
     * @param arg0    The first argument
     * @param arg1    The second argument
     * @param <TArg0> Type of the first trigger argument
     * @param <TArg1> Type of the second trigger argument
     */
    public <TArg0, TArg1> void fire(
            final TriggerWithParameters2<TArg0, TArg1, S, T> trigger,
            final TArg0 arg0,
            final TArg1 arg1) {
        requireNonNull(trigger, "trigger is null");
        publicFire(trigger.getTrigger(), arg0, arg1);
    }

    /**
     * Transition from the current state via the specified trigger.
     * The target state is determined by the configuration of the current state.
     * Actions associated with leaving the current state and entering the new one
     * will be invoked.
     *
     * @param trigger The trigger to fire
     * @param arg0    The first argument
     * @param arg1    The second argument
     * @param arg2    The third argument
     * @param <TArg0> Type of the first trigger argument
     * @param <TArg1> Type of the second trigger argument
     * @param <TArg2> Type of the third trigger argument
     */
    public <TArg0, TArg1, TArg2> void fire(
            final TriggerWithParameters3<TArg0, TArg1, TArg2, S, T> trigger,
            final TArg0 arg0,
            final TArg1 arg1,
            final TArg2 arg2) {
        requireNonNull(trigger, "trigger is null");
        publicFire(trigger.getTrigger(), arg0, arg1, arg2);
    }

    protected void publicFire(final T trigger, final Object... args) {
        logger.trace(stateMachineNameLogPrefix + "Firing {}", trigger);
        TriggerWithParameters<S, T> configuration = config.getTriggerConfiguration(trigger);
        if (configuration != null) {
            configuration.validateParameters(args);
        }

        TriggerBehaviour<S, T> triggerBehaviour = getCurrentRepresentation().tryFindHandler(trigger);
        if (triggerBehaviour == null) {
            unhandledTriggerAction.doIt(getCurrentRepresentation().getUnderlyingState(), trigger);
            return;
        }

        S source = getState();
        OutVar<S> destination = new OutVar<>();
        if (triggerBehaviour.resultsInTransitionFrom(source, args, destination)) {
            Transition<S, T> transition = new Transition<>(source, destination.get(), trigger);

            getCurrentRepresentation().exit(transition);
            triggerBehaviour.performAction(args);
            setState(destination.get());
            getCurrentRepresentation().enter(transition, args);
            logger.debug(stateMachineNameLogPrefix + "{} --> {} : {}", stateMachineNameLogPrefix, source, destination.get(), trigger);
        }
    }

    /**
     * Override the default behaviour of throwing an exception when an unhandled trigger is fired
     *
     * @param unhandledTriggerAction An action to call when an unhandled trigger is fired
     */
    public void onUnhandledTrigger(final Action2<S, T> unhandledTriggerAction) {
        if (unhandledTriggerAction == null) {
            throw new IllegalArgumentException("unhandledTriggerAction must not be null");
        }

        this.unhandledTriggerAction = unhandledTriggerAction;
    }

    /**
     * Determine if the state machine is in the supplied state
     *
     * @param state The state to test for
     * @return True if the current state is equal to, or a substate of, the supplied state
     */
    public boolean isInState(final S state) {
        return getCurrentRepresentation().isIncludedIn(state);
    }

    /**
     * Returns true if {@code trigger} can be fired  in the current state
     *
     * @param trigger Trigger to test
     * @return True if the trigger can be fired, false otherwise
     */
    public boolean canFire(final T trigger) {
        return getCurrentRepresentation().canHandle(trigger);
    }

    private void logUnhandledTriggerAction(final S state, final T trigger) {
        logger.warn(
                stateMachineNameLogPrefix + "No transition defined for trigger {} when in state {}. Consider ignoring the trigger as part of the configuration.",
                trigger,
                state);
    }

    /**
     * This method returns a function that will log a warning of a non defined state transition.
     *
     * <p>This might be an sensible action besides the default of throwing an {@code IllegateStateException}. </p>
     *
     * <p>If one wants to change the default unhandled behavior call {@link #onUnhandledTrigger(Action2)} with
     * the return value of this method.</p>
     *
     * <p>The benefit of using this method compared to others is that the existing logger instance will
     * be re-used. In addition, the log message will contain the optionally configured state machine
     * name as well.</p>
     *
     * @return Returns an action logging an undefined state transition warning.
     */
    public Action2<S, T> performUnhandledLoggingWarning() {
        return this::logUnhandledTriggerAction;
    }

    /**
     * A human-readable representation of the state machine
     *
     * @return A description of the current state and permitted triggers
     */
    @Override
    public String toString() {
        List<T> permittedTriggers = getPermittedTriggers();
        List<String> parameters = new ArrayList<>();

        for (T tTrigger : permittedTriggers) {
            parameters.add(tTrigger.toString());
        }

        StringBuilder params = new StringBuilder();
        String delim = "";
        for (String param : parameters) {
            params.append(delim);
            params.append(param);
            delim = ", ";
        }

        return String.format(
                "StateMachine %s {{ State = %s, PermittedTriggers = {{ %s }}}}",
                stateMachineNameLogPrefix,
                getState(),
                params.toString());
    }

    private String nameLoggingPrefix(final String stateMachineName) {
        return !stateMachineName.isEmpty() ? stateMachineName + " - " : "";
    }
}
