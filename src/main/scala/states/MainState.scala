package states

import java.util.UUID
import scala.util.Random
import scala.collection.mutable.HashSet
import ai.lum.common.RandomUtils._

object MainState extends App {

    val state = new State

    def time[R](block: => R): R = {
        val t0 = System.nanoTime()
        val result = block
        val t1 = System.nanoTime()
        println(s"Elapsed time: ${(t1-t0)*1e-9} seconds")
        result
    }

    def makeRandomMention(): (Int, Mention) = {
        val vocab = Array("A", "B", "C", "D", "E", "F", "G")
        val label = Random.choice(vocab)
        val luceneSegmentDocId = Random.nextInt(100)
        val luceneSegmentDocBase = Random.choice(Array(0, 100, 200))
        val luceneDocId = luceneSegmentDocId + luceneSegmentDocBase
        val mention = Mention(
            UUID.randomUUID(),
            StateMatch(0, 1),
            label,
            "rule0",
            luceneSegmentDocId,
            luceneSegmentDocBase,
            Map.empty,
        )
        (luceneDocId, mention)
    }

    val docIds = HashSet.empty[Int]

    println("Adding mentions to state")
    time {
        for (i <- 0 until 5000) {
            val (luceneDocId, mention) = makeRandomMention()
            state.addMention(mention)
            docIds += luceneDocId
        }
    }

    println("Get mentions for sentence")
    time {
        for (luceneDocId <- docIds) {
            val mentions = state.getMentionsForDocument(luceneDocId)
            println(s"doc=$luceneDocId mentions=${mentions.size}")
        }
    }

}