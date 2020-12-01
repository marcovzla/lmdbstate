package states

import java.util.Arrays
import scala.reflect.ClassTag

object ArrayUtils {

  def growSize(currentSize: Int): Int = {
    if (currentSize <= 4) 8 else currentSize * 2
  }

  def append[A: ClassTag](array: Array[A], currentSize: Int, element: A): Array[A] = {
    require(currentSize <= array.length)
    if (currentSize + 1 <= array.length) {
      array(currentSize) = element
      array
    } else {
      val newArray = new Array[A](growSize(array.length))
      System.arraycopy(array, 0, newArray, 0, currentSize)
      newArray(currentSize) = element
      newArray
    }
  }

  def insert[A: ClassTag](array: Array[A], currentSize: Int, index: Int, element: A): Array[A] = {
    require(currentSize <= array.length)
    if (currentSize + 1 <= array.length) {
      System.arraycopy(array, index, array, index + 1, currentSize - index)
      array(index) = element
      array
    } else {
      val newArray = new Array[A](growSize(array.length))
      System.arraycopy(array, 0, newArray, 0, index)
      newArray(index) = element
      System.arraycopy(array, index, newArray, index + 1, array.length - index)
      newArray
    }
  }

}
