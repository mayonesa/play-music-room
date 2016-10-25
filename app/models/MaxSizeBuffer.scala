package models

import collection.immutable.Queue

object MaxSizeBuffer {
  def apply[T](maxSize: Int) = new MaxSizeBuffer(Queue.empty[T], maxSize)
  private def apply[T](q: Queue[T], maxSize: Int) = new MaxSizeBuffer(q, maxSize)
}

class MaxSizeBuffer[T](q: Queue[T], maxSize: Int) {
  def enqueue(t: T): MaxSizeBuffer[T] = {
    val newQ = if (q.size == maxSize) q drop 1 else q
    MaxSizeBuffer(newQ enqueue t, maxSize)
  }
  def dequeue: (T, MaxSizeBuffer[T]) = {
    val (t, newQ) = q.dequeue
    (t, MaxSizeBuffer(newQ, maxSize))
  }
  def isEmpty: Boolean = q.isEmpty
}
