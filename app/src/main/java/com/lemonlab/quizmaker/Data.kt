package com.lemonlab.quizmaker

import android.content.Context
import android.util.Log
import android.util.SparseArray
import androidx.core.util.forEach
import com.google.firebase.firestore.FirebaseFirestore

enum class QuizType(val id: Int) {
    MultipleChoice(R.string.mulChoice), TrueFalse(R.string.trueFalse)
}

enum class NotificationType {
    MESSAGE, QUIZ
}

data class TheClass(
    val teach: String,
    val title: String,
    val date: Long,
    val members: ArrayList<String>,
    val id: String,
    val open: Boolean
) {
    constructor() : this("", "", 0, ArrayList(), "", false)

    private fun addMember(name: String) =
        members.add(name)

}


data class User(
    var username: String,
    val email: String,
    var id: String,
    var userBio: String,
    var points: Int,
    val joinDate: Long
) {
    constructor() : this("", "", "", "", 0, 0)

    fun joinTimeAsAString() = joinDate.timeAsAString()

}


abstract class Question

data class MultipleChoiceQuestion(
    val question: String,
    val first: String, val second: String,
    val third: String, val fourth: String,
    val correctAnswer: Int
) : Question() {
    constructor() : this("", "", "", "", "", 0)

}

data class TrueFalseQuestion(
    val question: String,
    val answer: Boolean
) : Question() {
    constructor() : this("", false)
}

abstract class Quizzer {

    abstract fun score(answers: SparseArray<*>): Int
    abstract fun getQuestion(pos: Int): String

    abstract fun getSize(): Int
    abstract fun getAuthor(): String
    abstract fun getTitle(): String
    abstract fun getID(): String

    abstract fun setTitle(title: String)

    abstract fun deleteQuestion(pos: Int)

}

data class MultipleChoiceQuiz(
    val quiz: Quiz?,
    var questions: HashMap<String, MultipleChoiceQuestion>?
) : Quizzer() {
    constructor() : this(null, null)

    override fun getAuthor() = quiz!!.quizAuthor
    override fun getID() = quiz!!.quizUUID
    override fun getTitle() = quiz!!.quizTitle


    override fun deleteQuestion(pos: Int) {

        questions!!.remove(pos.toString())
        quiz!!.questionsCount--

        val copy = HashMap<String, MultipleChoiceQuestion>()
        copy.putAll(questions!!)

        val iterator = copy.entries.iterator()
        while (iterator.hasNext()) {
            val item = iterator.next()
            if (item.key.toInt() > pos) {
                questions!![(item.key.toInt() - 1).toString()] = item.value
                questions!!.remove(item.key)
            }
        }
    }


    override fun score(answers: SparseArray<*>): Int {
        var score = 0
        answers.forEach { key, value ->
            val actual = questions!![key.toString()]!!.correctAnswer
            if (actual == value)
                score++
        }
        return score
    }

    override fun setTitle(title: String) {
        quiz!!.quizTitle = title
    }

    override fun getSize() = quiz!!.questionsCount

    override fun getQuestion(pos: Int) =
        questions!![pos.toString()]!!.question


    fun getChoiceOne(pos: Int) = questions!![pos.toString()]!!.first
    fun getChoiceTwo(pos: Int) = questions!![pos.toString()]!!.second
    fun getChoiceThree(pos: Int) = questions!![pos.toString()]!!.third
    fun getChoiceFour(pos: Int) = questions!![pos.toString()]!!.fourth


}

data class TrueFalseQuiz(
    val quiz: Quiz?,
    var questions: HashMap<String, TrueFalseQuestion>?
) : Quizzer() {
    constructor() : this(null, null)

    override fun deleteQuestion(pos: Int) {

        questions!!.remove(pos.toString())
        quiz!!.questionsCount--

        val copy = HashMap<String, TrueFalseQuestion>()
        copy.putAll(questions!!)

        val iterator = copy.entries.iterator()
        while (iterator.hasNext()) {
            val item = iterator.next()
            if (item.key.toInt() > pos) {
                questions!![(item.key.toInt() - 1).toString()] = item.value
                questions!!.remove(item.key)
            }
        }
    }


    override fun setTitle(title: String) {
        quiz!!.quizTitle = title
    }

    override fun getSize() = quiz!!.questionsCount

    override fun getAuthor() = quiz!!.quizAuthor
    override fun getID() = quiz!!.quizUUID
    override fun getTitle() = quiz!!.quizTitle

    override fun score(answers: SparseArray<*>): Int {
        var score = 0

        answers.forEach { key, value ->
            val actual = questions!![key.toString()]!!.answer
            Log.i("TFQUIZ", "I am $key, you said $value, I know it's $actual")
            if (actual == value)
                score++
        }
        return score
    }

    override fun getQuestion(pos: Int) =
        questions!![pos.toString()]!!.question


}


data class QuizLog(
    val userLog: MutableList<String>
) {
    fun addQuiz(
        quizUUID: String,
        userName: String,
        pointsToGet: Int,
        quizAuthor: String,
        total: Int,
        context: Context
    ) {
        if (!userLog.contains(quizUUID)) {
            userLog.add(quizUUID)
            val usersRef = FirebaseFirestore.getInstance().collection("users")
            usersRef.document(userName).get().addOnSuccessListener { document ->
                var points = document.get("user.points", Int::class.java)!!
                points += pointsToGet
                usersRef.document(userName).update("user.points", points)
                usersRef.document(quizAuthor).get().addOnSuccessListener { doc ->
                    val authorPoints = doc.get("user.points", Int::class.java)!! + total
                    usersRef.document(quizAuthor).update("user.points", authorPoints)
                    if (userName != quizAuthor)
                        NotificationSender().sendNotification(
                            context,
                            quizAuthor,
                            NotificationType.QUIZ
                        )
                }
            }

        }
    }

    constructor() : this(mutableListOf())
}

data class Quiz(
    var quizTitle: String,
    val passwordProtected: Boolean,
    var questionsCount: Int,
    val quizPin: String,
    val quizType: QuizType?,
    val quizAuthor: String,
    val quizUUID: String,
    var rating: Float,
    var totalRatingsCount: Int,
    val milliSeconds: Long
) {

    fun setNewRating(aNewRating: Float) {
        totalRatingsCount += 1
        rating += (aNewRating - rating) / totalRatingsCount

    }

    constructor() : this(
        "", false, 0,
        "", null, "", "", 0f, 0, 0
    )
}

data class Message(
    val sender: String,
    val message: String,
    val milliSeconds: Long,
    val id: String
) {
    constructor() : this("", "", 0, "")
}

enum class Option {
    CACHE, FAQ, APPS, LOGOUT, ABOUT, PRIVACY, THEME, FEEDBACK
}

data class OptionsItem(
    val icon: Int,
    val text: String,
    val type: Option
)