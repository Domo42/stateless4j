package com.github.oxo42.stateless4j;

import org.junit.Test;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class StateMachineTests {

    final Enum StateA = State.A, StateB = State.B, StateC = State.C,
            TriggerX = Trigger.X, TriggerY = Trigger.Y;
    private boolean fired = false;

    @Test
    public void CanUseReferenceTypeMarkers() {
        RunSimpleTest(
                new Enum[]{StateA, StateB, StateC},
                new Enum[]{TriggerX, TriggerY});
    }

    @Test
    public void CanUseValueTypeMarkers() {
        RunSimpleTest(State.values(), Trigger.values());
    }

    <S extends Enum, T extends Enum> void RunSimpleTest(S[] states, T[] transitions) {
        S a = states[0];
        S b = states[1];
        T x = transitions[0];

        StateMachineConfig<S, T> config = new StateMachineConfig<>();

        config.configure(a)
                .permit(x, b);

        StateMachine<S, T> sm = new StateMachine<>(a, config);
        sm.fire(x);

        assertEquals(b, sm.getState());
    }

    @Test
    public void InitialStateIsCurrent() {
        State initial = State.B;
        StateMachine<State, Trigger> sm = new StateMachine<>(initial, new StateMachineConfig<State, Trigger>());
        assertEquals(initial, sm.getState());
    }

    @Test
    public void SubstateIsIncludedInCurrentState() {
        StateMachineConfig<State, Trigger> config = new StateMachineConfig<>();

        config.configure(State.B).substateOf(State.C);

        StateMachine<State, Trigger> sm = new StateMachine<>(State.B, config);

        assertEquals(State.B, sm.getState());
        assertTrue(sm.isInState(State.C));
    }

    @Test
    public void WhenInSubstate_TriggerIgnoredInSuperstate_RemainsInSubstate() {
        StateMachineConfig<State, Trigger> config = new StateMachineConfig<>();

        config.configure(State.B)
                .substateOf(State.C);

        config.configure(State.C)
                .ignore(Trigger.X);

        StateMachine<State, Trigger> sm = new StateMachine<>(State.B, config);
        sm.fire(Trigger.X);

        assertEquals(State.B, sm.getState());
    }

    @Test
    public void PermittedTriggersIncludeSuperstatePermittedTriggers() {
        StateMachineConfig<State, Trigger> config = new StateMachineConfig<>();

        config.configure(State.A)
                .permit(Trigger.Z, State.B);

        config.configure(State.B)
                .substateOf(State.C)
                .permit(Trigger.X, State.A);

        config.configure(State.C)
                .permit(Trigger.Y, State.A);

        StateMachine<State, Trigger> sm = new StateMachine<>(State.B, config);
        List<Trigger> permitted = sm.getPermittedTriggers();

        assertTrue(permitted.contains(Trigger.X));
        assertTrue(permitted.contains(Trigger.Y));
        assertFalse(permitted.contains(Trigger.Z));
    }

    @Test
    public void PermittedTriggersAreDistinctValues() {
        StateMachineConfig<State, Trigger> config = new StateMachineConfig<>();

        config.configure(State.B)
                .substateOf(State.C)
                .permit(Trigger.X, State.A);

        config.configure(State.C)
                .permit(Trigger.X, State.B);

        StateMachine<State, Trigger> sm = new StateMachine<>(State.B, config);
        List<Trigger> permitted = sm.getPermittedTriggers();

        assertEquals(1, permitted.size());
        assertEquals(Trigger.X, permitted.get(0));
    }

    @Test
    public void AcceptedTriggersRespectGuards() {
        StateMachineConfig<State, Trigger> config = new StateMachineConfig<>();

        config.configure(State.B)
                .permitIf(Trigger.X, State.A, () -> false);

        StateMachine<State, Trigger> sm = new StateMachine<>(State.B, config);

        assertEquals(0, sm.getPermittedTriggers().size());
    }

    @Test
    public void WhenDiscriminatedByGuard_ChoosesPermitedTransition() {
        StateMachineConfig<State, Trigger> config = new StateMachineConfig<>();

        config.configure(State.B)
                .permitIf(Trigger.X, State.A, IgnoredTriggerBehaviourTests.returnFalse)
                .permitIf(Trigger.X, State.C, IgnoredTriggerBehaviourTests.returnTrue);

        StateMachine<State, Trigger> sm = new StateMachine<>(State.B, config);
        sm.fire(Trigger.X);

        assertEquals(State.C, sm.getState());
    }

    @Test
    public void WhenTriggerIsIgnored_ActionsNotExecuted() {
        StateMachineConfig<State, Trigger> config = new StateMachineConfig<>();

        config.configure(State.B)
                .onEntry(() -> setFired())
                .ignore(Trigger.X);

        fired = false;

        StateMachine<State, Trigger> sm = new StateMachine<>(State.B, config);
        sm.fire(Trigger.X);

        assertFalse(fired);
    }

    @Test
    public void IfSelfTransitionPermited_ActionsFire() {
        StateMachineConfig<State, Trigger> config = new StateMachineConfig<>();

        config.configure(State.B)
                .onEntry(() -> setFired())
                .permitReentry(Trigger.X);

        fired = false;

        StateMachine<State, Trigger> sm = new StateMachine<>(State.B, config);
        sm.fire(Trigger.X);

        assertTrue(fired);
    }

    @Test(expected = IllegalStateException.class)
    public void ImplicitReentryIsDisallowed() {
        StateMachineConfig<State, Trigger> config = new StateMachineConfig<>();

        StateMachine<State, Trigger> sm = new StateMachine<>(State.B, config);

        config.configure(State.B)
                .permit(Trigger.X, State.B);
    }

    @Test(expected = IllegalStateException.class)
    public void TriggerParametersAreImmutableOnceSet() {
        StateMachineConfig<State, Trigger> config = new StateMachineConfig<>();

        StateMachine<State, Trigger> sm = new StateMachine<>(State.B, config);

        config.setTriggerParameters(Trigger.X, String.class, int.class);
        config.setTriggerParameters(Trigger.X, String.class);
    }

    @Test
    public void anActionIsPerformed_givenAnInput_expectedResult() {
        // Given
        StateMachineConfig<State, Trigger> config = new StateMachineConfig<>();
        StateMachine<State, Trigger> sm = new StateMachine<>(State.A, config);
        config.configure(State.A)
              .permit(Trigger.Z, State.B);
        sm.onUnhandledTrigger(sm.performUnhandledLoggingWarning());

        // When
        sm.fire(Trigger.Y);

        // Then
        // No exception is thrown for unhandled trigger in current state.
    }

    @Test
    public void toString_nameConfigured_stringContainsName() {
        // given
        final String customName = "theCustomStateMachineName";
        StateMachineConfig<State, Trigger> config = new StateMachineConfig<>();

        StateMachine<State, Trigger> sm = new StateMachine<>(State.A, config);
        config.withName(customName)
                .configure(State.A)
                .permit(Trigger.Z, State.B);

        sm.onUnhandledTrigger(sm.performUnhandledLoggingWarning());
        sm.fire(Trigger.Y);

        // when
        String toString = sm.toString();

        // then
        assertThat("Expected toString() to contain state machine name.", toString, containsString(customName));
    }

    private void setFired() {
        fired = true;
    }
}
