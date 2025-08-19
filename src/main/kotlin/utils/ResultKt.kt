package org.app.utils

import java.util.Optional

/**
 * An idiomatic Kotlin Result that can be either Success(value) or Failure(error).
 * Generic parameters R and E may themselves be nullable types if needed by the caller,
 * but Result itself is always non-null.
 */
sealed class ResultKt<out R, E> {

    val isSuccessful: Boolean get() = this is Success
    val isFailure: Boolean get() = !isSuccessful

    fun getOrElse(defaultValue: @UnsafeVariance R): R = when (this) {
        is Success -> value
        is Failure -> defaultValue
    }

    fun or(other: ResultKt<@UnsafeVariance R, @UnsafeVariance E>): ResultKt<R, E> =
        this as? Success ?: other

    fun or(other: () -> ResultKt<@UnsafeVariance R, @UnsafeVariance E>): ResultKt<R, E> =
        this as? Success ?: other()

    fun getOrElseGet(defaultValue: () -> @UnsafeVariance R): R = when (this) {
        is Success -> value
        is Failure -> defaultValue()
    }

    fun getOrThrow(ex: () -> RuntimeException): R = when (this) {
        is Success -> value
        is Failure -> throw ex()
    }

    fun orThrow(): R = getOrThrow { IllegalStateException("Not successful") }

    fun <U> map(f: (R) -> U): ResultKt<U, E> = when (this) {
        is Success -> Success(f(value))
        is Failure -> Failure(error)
    }

    fun <U> mapError(f: (E) -> U): ResultKt<R, U> = when (this) {
        is Success -> Success(value)
        is Failure -> Failure(f(error))
    }

    fun <U> flatMap(f: (R) -> ResultKt<U, @UnsafeVariance E>): ResultKt<U, E> = when (this) {
        is Success -> f(value)
        is Failure -> Failure(error)
    }

    /**
     * If this is Success and predicate holds, return this; else return Failure(errorValue).
     * If this is already Failure, it is returned unchanged.
     */
    fun filter(predicate: (R) -> Boolean, errorValue: @UnsafeVariance E): ResultKt<R, E> = when (this) {
        is Success -> if (predicate(value)) this else Failure(errorValue)
        is Failure -> this
    }

    /** Returns the error value if Failure, otherwise throws IllegalStateException. */
    fun error(): E = when (this) {
        is Success -> throw IllegalStateException("No Error")
        is Failure -> error
    }

    fun ifSuccessful(f: (R) -> Unit) {
        if (this is Success) f(value)
    }

    fun ifFailure(f: (E) -> Unit) {
        if (this is Failure) f(error)
    }
    fun ifSuccessfulOrElse(f: (R) -> Unit, g: () -> Unit) {
        if (this is Success) f(value) else g()
    }

    /**
     * Combine two results. If both are successes, applies f; if one fails, that failure is returned;
     * if both fail, errors are combined using errorMapper.
     */
    fun <V, G> map2(
        other: ResultKt<G, @UnsafeVariance E>,
        f: (R, G) -> V,
        errorMapper: (E, E) -> E
    ): ResultKt<V, E> {
        return when (this) {
            is Success -> when (other) {
                is Success -> Success(f(this.value, other.value))
                is Failure -> Failure(other.error)
            }
            is Failure -> when (other) {
                is Success -> Failure(this.error)
                is Failure -> Failure(errorMapper(this.error, other.error))
            }
        }
    }

    /**
     * Combine two results collecting both errors into a list when any fails.
     */
    fun <V, G> map2(
        other: ResultKt<G, @UnsafeVariance E>,
        f: (R, G) -> V
    ): ResultKt<V, List<E>> {
        return when (this) {
            is Success -> when (other) {
                is Success -> Success(f(this.value, other.value))
                is Failure -> Failure(listOf(other.error))
            }
            is Failure -> when (other) {
                is Success -> Failure(listOf(this.error))
                is Failure -> Failure(listOf(this.error, other.error))
            }
        }
    }

    /** Recover a failure by mapping its error to a success value. */
    fun recover(f: (E) -> @UnsafeVariance R): ResultKt<R, E> = when (this) {
        is Success -> this
        is Failure -> Success(f(error))
    }

    /** Recover a failure by mapping its error to another Result. */
    fun recoverWith(f: (E) -> ResultKt<@UnsafeVariance R, @UnsafeVariance E>): ResultKt<R, E> = when (this) {
        is Success -> this
        is Failure -> f(error)
    }

    data class Failure<R, E>(val error: E) : ResultKt<R, E>()
    data class Success<out R, E>(val value: R) : ResultKt<R, E>()

    companion object {
        fun <V, U> failure(message: U): ResultKt<V, U> = Failure(message)
        fun <V, U> success(value: V): ResultKt<V, U> = Success(value)

        fun <V> ofNullable(value: V?): ResultKt<V, String> =
            if (value == null) Failure("Object is Null") else Success(value)

        fun <V, U> ofNullable(value: V?, error: U): ResultKt<V, U> =
            if (value == null) Failure(error) else Success(value)

        fun <I> fromOptional(opt: Optional<I>?): ResultKt<I, String> =
            fromOptional(opt, "Empty")

        fun <I> fromOptional(opt: Optional<I>?, error: String): ResultKt<I, String> {
            return when {
                opt == null -> Failure("Optional is null")
                opt.isPresent -> Success(opt.get())
                else -> Failure(error)
            }
        }

        /**
         * Sequence a list of Results into a Result of list, collecting all errors into a list when any fail.
         * On all-success, returns Success(list of values). On any failure, returns Failure(list of errors).
         */
        fun <A, B> sequence(list: List<ResultKt<A, B>>): ResultKt<List<A>, List<B>> {
            val values = ArrayList<A>()
            val errors = ArrayList<B>()
            for (r in list) {
                when (r) {
                    is Success -> values.add(r.value)
                    is Failure -> errors.add(r.error)
                }
            }
            return if (errors.isEmpty()) Success(values) else Failure(errors)
        }

        /** Sequence with error mapper that reduces all errors to a single error. */
        fun <A, B> sequenceWithErrorMapper(
            list: List<ResultKt<A, B>>,
            errorMapper: (B, B) -> B
        ): ResultKt<List<A>, B> {
            return when (val seq = sequence(list)) {
                is Success -> Success(seq.value)
                is Failure -> {
                    val reduced = seq.error.reduce(errorMapper)
                    Failure(reduced)
                }
            }
        }

        /** Sequence using a constant error to represent any failure. */
        fun <A, B> sequence(list: List<ResultKt<A, B>>, error: B): ResultKt<List<A>, B> =
            sequenceWithErrorMapper(list) { _, _ -> error }

        /** Try to produce a value, converting exceptions into Failure(errorMessage). */
        fun <A, B> failable(producer: () -> A, errorMessage: B): ResultKt<A, B> =
            try {
                ofNullable(producer(), errorMessage)
            } catch (e: Exception) {
                Failure(errorMessage)
            }
    }
}