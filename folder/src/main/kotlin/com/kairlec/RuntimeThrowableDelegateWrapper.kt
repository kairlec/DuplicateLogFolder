package com.kairlec

import com.kairlec.log.LogResult
import com.kairlec.log.ThrowableDelegate
import com.kairlec.log.causeOrNull
import com.kairlec.log.throwableDelegate
import mu.KotlinLogging

internal class RuntimeThrowableDelegateWrapper private constructor(
    private val throwable: Throwable?,
    private val throwableStorage: ThrowableStorage
) {
    fun asThrowable(): Throwable = throwable ?: throwableStorage

    companion object {
        fun from(throwable: Throwable): RuntimeThrowableDelegateWrapper {
            return RuntimeThrowableDelegateWrapper(throwable, ThrowableStorage(throwable.delegate()))
        }

        fun from(throwableDelegate: ThrowableDelegate): RuntimeThrowableDelegateWrapper {
            return RuntimeThrowableDelegateWrapper(null, ThrowableStorage(throwableDelegate))
        }
    }
}

class RuntimeLogResultWrapper private constructor(
    val logResult: LogResult,
    val rawThrowable: Throwable?
) {
    var written: Boolean = false
    companion object {
        fun from(logResult: LogResult, rawThrowable: Throwable?): RuntimeLogResultWrapper {
            return RuntimeLogResultWrapper(logResult, rawThrowable)
        }
    }
}

internal class ThrowableStorage(private val throwableDelegate: ThrowableDelegate) : Throwable() {
    override val message: String
        get() = "${throwableDelegate.className}: ${throwableDelegate.message}"

    private val delegateStackTrace by lazy {
        throwableDelegate.stackTraceList.map { it.toObject<StackTraceElement>() }.toTypedArray()
    }

    init {
        throwableDelegate.suppressedList.forEach {
            this.addSuppressed(ThrowableStorage(it))
        }
        throwableDelegate.causeOrNull?.let {
            initCause(ThrowableStorage(it))
        }
    }

    override fun getStackTrace(): Array<StackTraceElement> {
        return delegateStackTrace
    }
}

internal fun Throwable.delegate(): ThrowableDelegate {
    return throwableDelegate {
        this@delegate.message?.let {
            this.message = it
        }
        this.className = this@delegate.javaClass.name
        this.stackTrace.addAll(
            this@delegate.stackTrace.map { it.toByteString() }
        )
        val suppressed = this@delegate.suppressed
        // reversed read
        for (i in suppressed.indices.reversed()) {
            this.suppressed.add(suppressed[i].delegate())
        }
        this@delegate.cause?.let {
            this.cause = it.delegate()
        }
    }
}
