package lukulent.mindtoss.app.data

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.util.concurrent.atomic.AtomicReference

/** Holds shared text until MainViewModel can merge it into the live draft. */
object ShareInbox {
    private val pending = AtomicReference<String?>(null)
    private val _signal = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val signal: SharedFlow<Unit> = _signal.asSharedFlow()

    fun offer(text: String) {
        while (true) {
            val current = pending.get()
            val next = if (current.isNullOrBlank()) text else "$current\n\n$text"
            if (pending.compareAndSet(current, next)) break
        }
        _signal.tryEmit(Unit)
    }

    fun poll(): String? = pending.getAndSet(null)
}
