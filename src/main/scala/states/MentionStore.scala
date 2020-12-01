package states

import java.io.File
import java.nio.ByteBuffer
import java.util.UUID
import scala.collection.mutable
import org.lmdbjava.{ Env, Dbi, Txn }
import org.lmdbjava.DbiFlags.MDB_CREATE
import org.lmdbjava.PutFlags.MDB_NOOVERWRITE
import ai.lum.common.TryWithResources.using

class MentionStore(val dbName: String, val path: File) {

    private val env: Env[ByteBuffer] = Env.create()
        .setMapSize(10485760)
        .setMaxDbs(1)
        .open(path)

    private val db: Dbi[ByteBuffer] = env.openDbi(dbName, MDB_CREATE)

    private val mentionCache = mutable.Map.empty[UUID, Mention]

    def clear(): Unit = {
        mentionCache.clear()
    }

    def close(): Unit = {
        env.close()
    }

    def getMention(uuid: UUID): Option[Mention] = {
        if (mentionCache.contains(uuid)) {
            return Some(mentionCache(uuid))
        }
        val mention = using (env.txnRead()) { txn =>
            getMention(uuid, txn)
        }
        if (mention.isDefined) {
            mentionCache.update(uuid, mention.get)
        }
        mention
    }

    def putMention(mention: Mention): Unit = {
        using (env.txnWrite()) { txn =>
            putMention(mention, txn)
            txn.commit()
        }
    }

    private def getBytesFromUUID(uuid: UUID): ByteBuffer = {
        val bytes = UnsafeSerializer.getBytes(uuid)
        val buffer = ByteBuffer.allocateDirect(bytes.length)
        buffer.put(bytes)
        buffer.flip().asInstanceOf[ByteBuffer]
    }

    private def getBytesFromMention(mention: UnresolvedMention): ByteBuffer = {
        val bytes = UnsafeSerializer.getBytes(mention)
        val buffer = ByteBuffer.allocateDirect(bytes.length)
        buffer.put(bytes)
        buffer.flip().asInstanceOf[ByteBuffer]
    }

    private def putMention(mention: Mention, txn: Txn[ByteBuffer]): Unit = {
        if (putUnresolvedMention(UnresolvedMention(mention), txn)) {
            // if we added the unresolvedmention successfully,
            // then we need to add the children too
            for {
                mentions <- mention.arguments.values
                m <- mentions
            } putMention(m, txn)
        }
    }

    private def putUnresolvedMention(unresolved: UnresolvedMention, txn: Txn[ByteBuffer]): Boolean = {
        val key = getBytesFromUUID(unresolved.uuid)
        val value = getBytesFromMention(unresolved)
        db.put(txn, key, value, MDB_NOOVERWRITE)
    }

    private def getMention(uuid: UUID, txn: Txn[ByteBuffer]): Option[Mention] = {
        val key = getBytesFromUUID(uuid)
        if (db.get(txn, key) == null) {
            None
        } else {
            val buffer = txn.`val`
            val bytes = new Array[Byte](buffer.capacity())
            buffer.get(bytes)
            val ser = new UnsafeSerializer(bytes)
            val unresolved = ser.getUnresolvedMention()
            val mention = resolve(unresolved, txn)
            Some(mention)
        }
    }

    private def resolve(unresolved: UnresolvedMention, txn: Txn[ByteBuffer]): Mention = {
        Mention(
            unresolved.uuid,
            StateMatch(unresolved.startToken, unresolved.endToken),
            unresolved.label,
            unresolved.foundBy,
            unresolved.luceneSegmentDocId,
            unresolved.luceneSegmentDocBase,
            resolve(unresolved.namedCaptures, txn),
        )
    }

    private def resolve(unresolved: Map[String, Array[UUID]], txn: Txn[ByteBuffer]): Map[String, Array[Mention]] = {
        val result = mutable.Map.empty[String, Array[Mention]]
        for (name <- unresolved.keys) {
            val uuids = unresolved(name)
            val mentions = uuids.map(uuid => getMention(uuid, txn).get)
            result(name) = mentions
        }
        result.toMap
    }

}