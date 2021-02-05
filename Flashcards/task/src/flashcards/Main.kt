package flashcards

import java.io.*

class Flashcards(importFile: String? = "", exportFile: String? = "") {
    data class Card(val term: String, val def: String, var wrongAnswers: Int = 0) : Serializable

    enum class RESULT {
        TERM_ALREADY_EXIST,
        DEFINITION_ALREADY_EXIST,
        TERM_NOT_EXIST,
        FILE_NOT_FOUND,
        SUCCESS,
        WRONG_ANSWERS_NO,
        WRONG_ANSWERS_YES,
    }

    private val consoleLog = mutableListOf<String>()
    private val cardsByTerm = mutableMapOf<String, Card>()
    private val cardsByDef = mutableMapOf<String, Card>()

    init {
        importFile?.importCards()
        do {
            println("\nInput the action (add, remove, import, export, ask, exit, log, hardest card, reset stats):")
            val choice = readLine()!!
            when (choice) {
                "add" -> addCard()
                "remove" -> removeCard()
                "import" -> importCardsMenu()
                "export" -> exportCardsMenu()
                "ask" -> askCards()
                "log" -> log()
                "hardest card" -> hardestCards()
                "reset stats" -> resetWrongs()
            }
        } while (choice != "exit")
        println("Bye bye!")
        exportFile?.exportCards()
    }

    private fun println(s: String) { // Override function println()
        consoleLog.add(s)
        kotlin.io.println(s)
    }

    private fun readLine(): String? { // Override function readLine()
        val s = kotlin.io.readLine()
        if (s != null) consoleLog.add("> $s")
        return s
    }

    private fun addCard(): RESULT {
        println("The card:")
        val term = readLine()!!
        if (cardsByTerm[term] != null) {
            println("The card \"$term\" already exists.")
            return RESULT.TERM_ALREADY_EXIST
        }
        println("The definition of the card:")
        val def = readLine()!!
        if (cardsByDef[def] != null) {
            println("The definition \"$def\" already exists.")
            return RESULT.DEFINITION_ALREADY_EXIST
        }
        val card = Card(term, def)
        cardsByTerm[term] = card
        cardsByDef[def] = card
        println("The pair (\"$term\":\"$def\") has been added.")
        return RESULT.SUCCESS
    }

    private fun removeCard(): RESULT {
        println("Which card?")
        val term = readLine()!!
        val card = cardsByTerm[term]
        if (card == null) {
            println("Can't remove \"$term\": there is no such card.")
            return RESULT.TERM_NOT_EXIST
        }
        cardsByTerm.remove(card.term)
        cardsByDef.remove(card.def)
        println("The card has been removed.")
        return RESULT.SUCCESS
    }

    private fun importCardsMenu(): RESULT {
        println("File name:")
        val fileName = readLine()!!
        return fileName.importCards()
    }

    private fun exportCardsMenu(): RESULT {
        println("File name:")
        val fileName = readLine()!!
        return fileName.exportCards()
    }

    private fun askCards(): RESULT {
        println("How many times to ask?")
        val numberOfCards = readLine()!!.toInt()
        val list = cardsByTerm.toList().map { it.second }
        repeat(numberOfCards) {
            val (term, def) = list[(0..list.lastIndex).random()]
            println("Print the definition of \"$term\":")
            val ans = readLine()!!
            when {
                ans == def ->
                    println("Correct!")
                cardsByDef[ans] != null -> {
                    println("Wrong. The right answer is \"$def\", but your definition is correct for \"${cardsByDef[ans]!!.term}\".")
                    cardsByTerm[term]!!.wrongAnswers++
                }
                else -> {
                    println("""Wrong. The right answer is "$def".""")
                    cardsByTerm[term]!!.wrongAnswers++
                }
            }
        }
        return RESULT.SUCCESS
    }

    private fun log(): RESULT {
        println("File name:")
        val out = File(readLine()!!).printWriter()
        consoleLog.forEach { out.println(it) }
        out.close()
        println("The log has been saved.")
        return RESULT.SUCCESS
    }

    private fun hardestCards(): RESULT {
        val cards = cardsByTerm.map { it.value }
        val max = cards.maxOfOrNull { it.wrongAnswers }
        if (max == null || max == 0) {
            println("There are no cards with errors.")
            return RESULT.WRONG_ANSWERS_NO
        }
        val wrongs = cards.filter { it.wrongAnswers == max }
        var terms = ""
        wrongs.forEach { terms += "\"${it.term}\", " }
        terms = terms.dropLast(2)

        if (wrongs.size == 1)
            println("The hardest card is $terms. You have $max errors answering it.")
        else {
            println("The hardest cards are $terms. You have $max errors answering them.")
        }
        return RESULT.WRONG_ANSWERS_YES
    }

    private fun resetWrongs(): RESULT {
        cardsByTerm.map { it.value }.forEach { it.wrongAnswers = 0 }
        println("Card statistics have been reset.")
        return RESULT.SUCCESS
    }

    private fun String.importCards(): RESULT {
        val file = File(this)
        if (!file.exists()) {
            println("File not found.")
            return RESULT.FILE_NOT_FOUND
        }
        val fis = file.inputStream()
        val ois = ObjectInputStream(fis)
        var count = 0
        try {
            while (true) {
                val card = ois.readObject() as Card
                count++
                cardsByTerm[card.term] = card
                cardsByDef[card.def] = card
            }
        } catch (e: EOFException) {
        }
        ois.close()
        fis.close()
        println("$count cards have been loaded.")
        return RESULT.SUCCESS
    }

    private fun String.exportCards(): RESULT {
        val file = File(this)
        val fos = file.outputStream()
        val oos = ObjectOutputStream(fos)
        cardsByTerm.forEach { oos.writeObject(it.value) }
        oos.close()
        fos.close()
        println("${cardsByTerm.size} cards have been saved")
        return RESULT.SUCCESS
    }
}

fun main(args: Array<String>) {
    val param = args.joinToString(" ")
    val import = Regex("""(?:-import )(\S+)""").find(param)?.groupValues?.get(1)
    val export = Regex("""(?:-export )(\S+)""").find(param)?.groupValues?.get(1)
    Flashcards(import, export)
}