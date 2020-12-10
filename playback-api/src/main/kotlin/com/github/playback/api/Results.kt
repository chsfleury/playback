package com.github.playback.api

sealed class Result<E, T> {
  abstract fun <R: Any?> map(mapper: (T) -> R): Result<E, R>
  abstract fun <R: Any?> mapTry(mapper: (T) -> R, eMapper: (Exception) -> E): Result<E, R>

  abstract fun isSuccess(): Boolean
  abstract fun getSuccess(): Success<E, T>
  abstract fun getFailure(): Failure<E, T>
}

data class Success<E, T>(val value: T) : Result<E, T>() {
  override fun <R> map(mapper: (T) -> R): Result<E, R> = Success(mapper(value))
  override fun <R> mapTry(mapper: (T) -> R, eMapper: (Exception) -> E): Result<E, R> = try {
    map(mapper)
  } catch (e: Exception) {
    Failure(eMapper(e))
  }

  override fun isSuccess(): Boolean = true
  override fun getSuccess(): Success<E, T> = this
  override fun getFailure(): Failure<E, T> = throw NoSuchElementException()
}

data class Failure<E, T>(val error: E) : Result<E, T>() {
  override fun <R> map(mapper: (T) -> R): Result<E, R> = Failure(error)
  override fun <R> mapTry(mapper: (T) -> R, eMapper: (Exception) -> E): Result<E, R> = Failure(error)

  override fun isSuccess(): Boolean = false
  override fun getSuccess(): Success<E, T> = throw NoSuchElementException()
  override fun getFailure(): Failure<E, T> = this
}

inline fun <reified E, T> success(value: T): Result<E, T> = Success(value)
inline fun <E, reified T> failure(error: E): Result<E, T> = Failure(error)
