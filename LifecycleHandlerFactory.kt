import android.os.Handler
import android.os.Looper
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner

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
