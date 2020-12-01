package states

import sun.misc.Unsafe
import java.nio.ByteBuffer
import java.util.UUID
import java.nio.charset.StandardCharsets.UTF_8
import scala.collection.mutable

class UnsafeSerializer(val bytes: Array[Byte]) {

  import UnsafeSerializer._

  def this(capacity: Int) = {
    this(new Array[Byte](capacity))
  }

  private var pos = 0

  def getInt(): Int = {
    val offset = (byteArrayOffset + pos).toLong
    val value = unsafe.getInt(bytes, offset)
    pos += sizeOfInt
    value
  }

  def putInt(value: Int): Unit = {
    val offset = (byteArrayOffset + pos).toLong
    unsafe.putInt(bytes, offset, value)
    pos += sizeOfInt
  }

  def getLong(): Long = {
    val offset = (byteArrayOffset + pos).toLong
    val value = unsafe.getLong(bytes, offset)
    pos += sizeOfLong
    value
  }

  def putLong(value: Long): Unit = {
    val offset = (byteArrayOffset + pos).toLong
    unsafe.putLong(bytes, offset, value)
    pos += sizeOfLong
  }

  def getIntArray(): Array[Int] = {
    val length = getInt()
    val values = new Array[Int](length)
    val bytesToCopy = length * sizeOfInt
    unsafe.copyMemory(
      bytes, byteArrayOffset + pos,
      values, intArrayOffset,
      bytesToCopy
    )
    pos += bytesToCopy
    values
  }

  def putIntArray(values: Array[Int]): Unit = {
    val length = values.length
    val bytesToCopy = length * sizeOfInt
    putInt(length)
    unsafe.copyMemory(
      values, intArrayOffset,
      bytes, byteArrayOffset + pos,
      bytesToCopy
    )
    pos += bytesToCopy
  }

  def getString(): String = {
    val bytesToCopy = getInt()
    val encoded = new Array[Byte](bytesToCopy)
    unsafe.copyMemory(
      bytes, byteArrayOffset + pos,
      encoded, byteArrayOffset,
      bytesToCopy
    )
    pos += bytesToCopy
    charset.decode(ByteBuffer.wrap(encoded)).toString()
  }

  def putString(string: String): Unit = {
    val encoded = charset.encode(string).array()
    val bytesToCopy = encoded.length
    putInt(bytesToCopy)
    unsafe.copyMemory(
      encoded, byteArrayOffset,
      bytes, byteArrayOffset + pos,
      bytesToCopy
    )
    pos += bytesToCopy
  }

  def getUUID(): UUID = {
    val high = getLong()
    val low = getLong()
    new UUID(high, low)
  }

  def putUUID(uuid: UUID): Unit = {
    putLong(uuid.getMostSignificantBits())
    putLong(uuid.getLeastSignificantBits())
  }

  def getUUIDArray(): Array[UUID] = {
    val length = getInt()
    val values = new Array[UUID](length)
    for (i <- 0 until length) {
      values(i) = getUUID()
    }
    values
  }

  def putUUIDArray(uuids: Array[UUID]): Unit = {
    val length = uuids.length
    putInt(length)
    for (uuid <- uuids) {
      putUUID(uuid)
    }
  }

  def getNamedCaptures(): Map[String, Array[UUID]] = {
    val numNames = getInt()
    val namedCaptures = mutable.Map.empty[String, Array[UUID]]
    var i = 0
    while (i < numNames) {
      i += 1
      val name = getString()
      val ids = getUUIDArray()
      namedCaptures(name) = ids
    }
    namedCaptures.toMap
  }

  def putNamedCaptures(namedCaptures: Map[String, Array[UUID]]): Unit = {
    val numNames = namedCaptures.size
    putInt(numNames)
    for ((name, uuids) <- namedCaptures.toIterator) {
      putString(name)
      putUUIDArray(uuids)
    }
  }

  def getUnresolvedMention(): UnresolvedMention = {
    new UnresolvedMention(
      uuid = getUUID(),
      label = getString(),
      foundBy = getString(),
      indices = getIntArray(),
      namedCaptures = getNamedCaptures(),
    )
  }

  def putUnresolvedMention(mention: UnresolvedMention): Unit = {
    putUUID(mention.uuid)
    putString(mention.label)
    putString(mention.foundBy)
    putIntArray(mention.indices)
    putNamedCaptures(mention.namedCaptures)
  }

}

object UnsafeSerializer {

  private val unsafe = {
    val field = classOf[Unsafe].getDeclaredField("theUnsafe")
    field.setAccessible(true)
    field.get(null).asInstanceOf[Unsafe]
  }

  val byteArrayOffset = unsafe.arrayBaseOffset(classOf[Array[Byte]])
  val intArrayOffset = unsafe.arrayBaseOffset(classOf[Array[Int]])

  val sizeOfInt = 4
  val sizeOfLong = 8
  val sizeOfUUID = 16
  val charset = UTF_8

  def getBytes(mention: UnresolvedMention): Array[Byte] = {
    val ser = new UnsafeSerializer(sizeInBytes(mention))
    ser.putUnresolvedMention(mention)
    ser.bytes
  }

  def getBytes(uuid: UUID): Array[Byte] = {
    val ser = new UnsafeSerializer(16)
    ser.putUUID(uuid)
    ser.bytes
  }

  def getUnresolvedMention(bytes: Array[Byte]): UnresolvedMention = {
    val ser = new UnsafeSerializer(bytes)
    ser.getUnresolvedMention()
  }

  def sizeInBytes(string: String): Int = {
    sizeOfInt + charset.encode(string).array().length
  }

  def sizeInBytes(array: Array[Int]): Int = {
    sizeOfInt + sizeOfInt * array.length
  }

  def sizeInBytes(array: Array[UUID]): Int = {
    sizeOfInt + sizeOfUUID * array.length
  }

  def sizeInBytes(arguments: Map[String, Array[UUID]]): Int = {
    var size = 0
    size += sizeOfInt
    for ((name, uuids) <- arguments.toIterator if uuids.length > 0) {
      size += sizeInBytes(name)
      size += sizeInBytes(uuids)
    }
    size
  }

  def sizeInBytes(m: UnresolvedMention): Int = {
    var size: Int = 0
    size += sizeOfUUID
    size += sizeInBytes(m.label)
    size += sizeInBytes(m.foundBy)
    size += sizeInBytes(m.indices)
    size += sizeInBytes(m.namedCaptures)
    size
  }

}
