package splitties.coroutines

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.runtime.snapshots.SnapshotStateList
import kotlinx.coroutines.suspendCancellableCoroutine
import splitties.collections.forEachByIndex
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume

@Composable
inline fun <reified T> rememberCallableState(): CallableState<T> {
    return remember { CallableState() }
}

@Stable
class CallableState<T> @PublishedApi internal constructor(
    @PublishedApi
    internal val awaiters: SnapshotStateList<Continuation<T>>,
    private val isOfUnitType: Boolean
) : (T) -> Unit, () -> Unit {

    companion object {
        inline operator fun <reified T> invoke(): CallableState<T> {
            return CallableState<T>(mutableStateListOf(), isOfUnitType = T::class == Unit::class)
        }
    }

    suspend fun awaitOneCall(): T = suspendCancellableCoroutine { continuation ->
        awaiters.add(continuation)
        continuation.invokeOnCancellation { awaiters.remove(continuation) }
    }

    override fun invoke() {
        require(isOfUnitType)
        @Suppress("UNCHECKED_CAST")
        call(Unit as T)
    }

    override fun invoke(t: T) {
        call(t)
    }

    fun call(newValue: T): Boolean {
        val list = Snapshot.withMutableSnapshot {
            val result = awaiters.toList()
            awaiters.clear()
            result
        }
        list.forEachByIndex { it.resume(newValue) }
        return list.isNotEmpty()
    }

    inline val awaitersCount: Int get() = awaiters.size
    inline val isAwaitingCall: Boolean get() = awaiters.isNotEmpty()
}

inline fun CallableState<Unit>.call(): Boolean = call(Unit)
