package states

import java.io.File
import java.util.UUID

object Main extends App {

    def time[R](block: => R): R = {
        val t0 = System.nanoTime()
        val result = block
        val t1 = System.nanoTime()
        println(s"Elapsed time: ${(t1-t0)*1e-9} seconds")
        result
    }

    println("Making kv store")
    val store = time {
        new MentionStore("the store", new File("store-temp"))
    }

    val simpleMention = Mention(
        UUID.randomUUID(),
        StateMatch(0, 1),
        "Protein",
        "rule1",
        0,
        0,
        Map.empty,
    )

    println("\nAdd mention to store")
    time {
        store.putMention(simpleMention)
    }

    println("\nGet value from store")
    val result = time {
        store.getMention(simpleMention.uuid)
    }

    println("\nTry to add again")
    time {
        store.putMention(simpleMention)
    }

    val arg1 = Mention(
        UUID.randomUUID(),
        StateMatch(0, 1),
        "Flat",
        "rule1",
        0,
        0,
        Map.empty,
    )

    val arg2 = Mention(
        UUID.randomUUID(),
        StateMatch(1, 2),
        "Flat",
        "rule2",
        0,
        0,
        Map.empty,
    )

    val event = Mention(
        UUID.randomUUID(),
        StateMatch(2, 3),
        "Event",
        "rule3",
        0,
        0,
        Map("arg1" -> Array(arg1), "arg2" -> Array(arg2)),
    )

    println(s"\nAdd event mention to store")
    time {
        store.putMention(event)
    }

    println(s"\nRetrieve event mention from store")
    val ev = time {
        store.getMention(event.uuid)
    }

    println(ev)
    println(ev.get.arguments("arg1")(0))
    println(ev.get.arguments("arg2")(0))

}