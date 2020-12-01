package states

import java.util.UUID

class UnresolvedMention(
    val uuid: UUID,
    val label: String,
    val foundBy: String,
    val indices: Array[Int],
    val namedCaptures: Map[String, Array[UUID]],
) {

    val luceneSegmentDocId = indices(0)
    val luceneSegmentDocBase = indices(1)
    val startToken = indices(2)
    val endToken = indices(3)

}

object UnresolvedMention {

    def apply(mention: Mention): UnresolvedMention = {
        new UnresolvedMention(
            mention.uuid,
            mention.label,
            mention.foundBy,
            Array(
                mention.luceneSegmentDocBase,
                mention.luceneSegmentDocId,
                mention.odinsonMatch.start,
                mention.odinsonMatch.end,
            ),
            mention.arguments.map {
                case (name, mentions) => (name, mentions.map(_.uuid))
            }
        )
    }

}