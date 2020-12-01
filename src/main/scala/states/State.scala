package states

import java.io.File
import java.util.UUID
import scala.collection.mutable.{ HashMap, PriorityQueue }

class State {

  case class MetaMention(start: Int, end: Int, uuid: UUID)

  implicit val ordering: Ordering[MetaMention] = Ordering.by[MetaMention, (Int, Int)] {
    mm => (-mm.start, -mm.end)
  }

  val store = new MentionStore("the store", new File("store-temp"))

  // FIXME replace HashMap with Int key, use SparseArray instead
  val order = HashMap.empty[Int, PriorityQueue[MetaMention]]

  def addMention(m: Mention): Unit = {
    store.putMention(m)
    val pq = order.getOrElseUpdate(m.luceneDocId, PriorityQueue.empty[MetaMention])
    val mm = MetaMention(m.odinsonMatch.start, m.odinsonMatch.end, m.uuid)
    pq.enqueue(mm)
  }

  def addMentions(mentions: Seq[Mention]): Unit = {
    mentions.foreach(addMention)
  }

  def getMentionsForDocument(doc: Int): Iterable[Mention] = {
    order(doc)
      .clone
      .dequeueAll
      .toIterable
      .map(mm => store.getMention(mm.uuid).get)
  }

  def getMentionsForDocument(doc: Int, label: String): Iterable[Mention] = {
    getMentionsForDocument(doc).filter(_.label == label)
  }

}
