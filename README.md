Dore Guide
==========

A second rewrite of the [GuideAndroid](https://github.com/VandyMobile/guide-android) in Scala.
This version uses a different approach to the Reactive Programming technique in building
an Android app. The [previous version](https://github.com/AliceCengal/doreguide-scala) uses Scala's native Actor library to do events and
messaging. I ran into some troubles with that. First, there is the uncertainty that the Dalvik
VM might not implement the same threading model as the JVM, so we're not completely sure that
Scala's Actor library will behave correctly.

Second, Scala's Actor library is deprecated, and
might be removed in future versions of Scala. We don't want to be stuck with using Scala 2.10.3
with no future upgrade possible. They recommend using Akka instead, but I couldn't get that to
work on Android and Maven, and furthermore the Akka workflow necessitates that we have full control over the
creation of our program's components, which we don't because the Android system does most of
the initializations and we can only hook into the process through all the "onXXX" methods.

My solution is to use the existing messaging infrastructure on Android, the `Handler` class.
We can get all the nice DSL and convenience of Scala's Actor, with the performance, compatibility
and optimization of the native messaging infrastructure by wrapping the `Handler` with `ActorShell`.
This transformation is made transparent with implicit conversion provided by the `ActorConversion`
trait.

Using the `ActorShell(handler)` as a building block, we can build a higher level infrastructure
that provide more utilities. The traits `ChattyFragment` and `ChattyActivity` configures any Fragment
and Activity for messaging with minimum boilerplate. The global `EventHub`, initialized
in `AppService` and provided through the `XXXInjection` mixins, allows various components of the app
to communicate with minimal coupling.

These custom contructs will hopefully result in a more responsive and sophisticated application
that is still easy to develop.

Build
-----

Please refer to my [previous Scala Android apps](https://github.com/AliceCengal/doreguide-scala) for build instructions.