package states

import java.util.Arrays

class SparseIntArray private (
  private var keys: Array[Int],
  private var values: Array[Int],
  private var currentSize: Int,
) {

  require(currentSize >= 0)
  require(currentSize <= keys.length)
  require(currentSize <= values.length)

  def size: Int = {
    currentSize
  }

  def indices: Array[Int] = {
    Arrays.copyOfRange(keys, 0, currentSize)
  }

  def apply(key: Int): Int = {
    val i = Arrays.binarySearch(keys, 0, currentSize, key)
    if (i >= 0) {
      values(i)
    } else {
      throw new NoSuchElementException
    }
  }

  def get(key: Int): Option[Int] = {
    val i = Arrays.binarySearch(keys, 0, currentSize, key)
    if (i >= 0) {
      Some(values(i))
    } else {
      None
    }
  }

  def update(key: Int, value: Int): Unit = {
    var i = Arrays.binarySearch(keys, 0, currentSize, key)
    if (i >= 0) {
      values(i) = value
    } else {
      i = ~i
      keys = ArrayUtils.insert(keys, currentSize, i, key)
      values = ArrayUtils.insert(values, currentSize, i, value)
      currentSize += 1
    }
  }

  def copy(): SparseIntArray = {
    new SparseIntArray(
      Arrays.copyOf(keys, keys.length),
      Arrays.copyOf(values, values.length),
      currentSize,
    )
  }

}

object SparseIntArray {

  def apply(): SparseIntArray = {
    apply(ArrayUtils.growSize(0))
  }

  def apply(initialCapacity: Int): SparseIntArray = {
    require(initialCapacity >= 0)
    if (initialCapacity == 0) {
      new SparseIntArray(Array.emptyIntArray, Array.emptyIntArray, 0)
    } else {
      new SparseIntArray(new Array[Int](initialCapacity), new Array[Int](initialCapacity), 0)
    }
  }

}
