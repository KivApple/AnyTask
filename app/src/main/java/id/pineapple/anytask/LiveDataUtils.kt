package id.pineapple.anytask

import android.arch.lifecycle.*

fun <T> LiveData<T>.observeOnce(observer: Observer<T>) {
	val owner = object : LifecycleOwner {
		val lifecycleRegistry = LifecycleRegistry(this)
		
		override fun getLifecycle(): Lifecycle = lifecycleRegistry
	}
	owner.lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
	owner.lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
	owner.lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
	observe(owner, Observer { data ->
		owner.lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
		owner.lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
		owner.lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
		observer.onChanged(data)
	})
}
