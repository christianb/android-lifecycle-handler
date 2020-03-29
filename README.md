# android-lifecycle-handler

If you are using a `Handler` to execute delayed actions with `postDelayed()` you can run into troubles when the execution of the action happens after your Activity or Fragment has been destroyed.

There is a simple solution to this. Bind your Handler to the lifecycle.

## Create a LifecycleObserver
First lets create a `LifecycleObserver` that gets a `Handler` instance.
In the event of `Lifecycle.Event.ON_DESTROY` it will remove all callbacks and messages from that `Handler`.
```
private class LifecycleObververHandler(private val handler: Handler) : LifecycleObserver {
	@OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
	internal fun onDestroy() {
		handler.removeCallbacksAndMessages(null)
	}
}
```

## Add the LifecycleObserver to the LifecycleOwner
Next we have to add the `LifecycleObververHandler` to a `LifecycleOwner`. We also wanna create these lifecycle observed handlers easily. So lets create a `LifecycleHandlerFactory`. 

That factory gets created with a lambda `handlerFactory` that gives you an instance of a `Handler` (default is a `Handler` with a main Looper). It has one function `create` that expects a `LifecycleOwner`.

Within that function it checks that the state of the `Lifecycle` is not `DESTROYED`. It calls the `handlerFactory` to get an instance of `Handler`. Then it creates a `LifecycleObserverHandler`, which takes the handler, and adds that `Observer` to the `LifecycleOwner`. Finally the `Handler` gets returned.
```
class LifecycleHandlerFactory(private val handlerFactory: (() -> Handler) = { Handler(Looper.getMainLooper()) }) {

	fun create(owner: LifecycleOwner): Handler {
		check(owner.lifecycle.currentState != Lifecycle.State.DESTROYED) {
			"Cannot create a Handler for a destroyed life-cycle"
		}
		val handler = handlerFactory.invoke()
		val observer = LifecycleObververHandler(handler)
		owner.lifecycle.addObserver(observer)
		return handler
	}
}
```

## Inject the lifecycle aware Handler
When you are using a DependendencyInjection Framework or a service locater like [Koin](https://insert-koin.io/) you can inject the lifecycle aware `Handler`.
```
module {
  // a single instance of LifecycleHandlerFactory
  // it gets a lambda that every time its being called returnes a new Handler with a main looper.
  single { LifecycleHandlerFactory() }
  
  // uses the LifecycleHandlerFactory to create a new handler with a LifecycleOwner as parameter.
  factory<Handler> { (lifecycleOwner: LifecycleOwner) -> get<LifecycleHandlerFactory>().create(lifecycleOwner) }
}
```

Finally you can inject a lifecycle handler in your Fragment (or Activity).
```
// injects a new handler with a LifecycleOwner as a parameter
private val handler: Handler by inject { parametersOf(viewLifecycleOwner) }
```

Thanks to [Ronaldo Pace](https://github.com/budius)
