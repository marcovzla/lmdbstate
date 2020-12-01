package states

import java.util.UUID

case class StateMatch(start: Int, end: Int)

case class Mention(
    uuid: UUID,
    odinsonMatch: StateMatch, 
    label: String,
    foundBy: String,
    luceneSegmentDocId: Int,
    luceneSegmentDocBase: Int,
    arguments: Map[String, Array[Mention]],
) {
    def luceneDocId = luceneSegmentDocBase + luceneSegmentDocId
}
