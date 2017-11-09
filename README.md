This is a fort of stateless4j v2.5.0 originally implemented by github user oxo42:  
https://github.com/oxo42/stateless4j

Unfortunately, the project seems abandoned and valid bugfixes and suggestions are
unanswered. Hence this fork is created to continue work on the code base.

Changes since the original release can be found in the release notes.  
https://github.com/Domo42/stateless4j/releases

Maven
=====

The lib is available as its own maven artifact, different to the one
published by oxo42. The code package names have not changed.

```xml
    <dependency>
        <groupId>com.codebullets.stateless4j</groupId>
        <artifactId>stateless4j</artifactId>
        <version>3.0.0</version>
    </dependency>
```

[![Build Status](https://travis-ci.org/oxo42/stateless4j.svg?branch=master)](https://travis-ci.org/oxo42/stateless4j)

Introduction
============
Create **state machines** and lightweight state machine-based workflows **directly in java code**.

```java
StateMachineConfig<State, Trigger> phoneCallConfig = new StateMachineConfig<>();

phoneCallConfig.configure(State.OffHook)
        .permit(Trigger.CallDialed, State.Ringing);

phoneCallConfig.configure(State.Ringing)
        .permit(Trigger.HungUp, State.OffHook)
        .permit(Trigger.CallConnected, State.Connected);

// this example uses Java 8 method references
// a Java 7 example is provided in /examples
phoneCallConfig.configure(State.Connected)
        .onEntry(this::startCallTimer)
        .onExit(this::stopCallTimer)
        .permit(Trigger.LeftMessage, State.OffHook)
        .permit(Trigger.HungUp, State.OffHook)
        .permit(Trigger.PlacedOnHold, State.OnHold);

// ...

StateMachine<State, Trigger> phoneCall =
        new StateMachine<>(State.OffHook, phoneCallConfig);

phoneCall.fire(Trigger.CallDialed);
assertEquals(State.Ringing, phoneCall.getState());
```

stateless4j is a port of [stateless](https://github.com/nblumhardt/stateless) for java


Features
========
Most standard state machine constructs are supported:

* Generic support for states and triggers of any java type (numbers, strings, enums, etc.)
* Hierarchical states
* Entry/exit events for states
* Guard clauses to support conditional transitions
* User-defined actions can be executed when transitioning
* Introspection


Some useful extensions are also provided:
* Parameterised triggers
* Reentrant states


Hierarchical States
===================
In the example below, the `OnHold` state is a substate of the `Connected` state. This means that an `OnHold` call is
still connected.

```java
phoneCall.configure(State.OnHold)
    .substateOf(State.Connected)
    .permit(Trigger.TakenOffHold, State.Connected)
    .permit(Trigger.HungUp, State.OffHook)
    .permit(Trigger.PhoneHurledAgainstWall, State.PhoneDestroyed);
```

In addition to the `StateMachine.getState()` property, which will report the precise current state, an `isInState(State)`
method is provided. `isInState(State)` will take substates into account, so that if the example above was in the
`OnHold` state, `isInState(State.Connected)` would also evaluate to `true`.

Entry/Exit Events
=================
In the example, the `startCallTimer()` method will be executed when a call is connected. The `stopCallTimer()` will be
executed when call completes (by either hanging up or hurling the phone against the wall.)

The call can move between the `Connected` and `OnHold` states without the `startCallTimer(`) and `stopCallTimer()`
methods being called repeatedly because the `OnHold` state is a substate of the `Connected` state.

Entry/Exit event handlers can be supplied with a parameter of type `Transition` that describes the trigger,
source and destination states.

Action on transition
===================
It is possible to execute a user-defined action when doing a transition.
For a 'normal' or 're-entrant' transition this action will be called
without any parameters. For 'dynamic' transitions (those who compute the
target state based on trigger-given parameters) the parameters of the
trigger will be given to the action.

This action is only executed if the transition is actually taken; so if
the transition is guarded and the guard forbids a transition, then the
action is not executed.

If the transition is taken, the action will be executed between the
`onExit` handler of the current state and the `onEntry` handler of the
target state (which might be the same state in case of a re-entrant
transition.

License
=======
Apache 2.0 License
