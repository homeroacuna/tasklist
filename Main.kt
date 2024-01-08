package tasklist

import kotlinx.datetime.*
import com.squareup.moshi.*
import java.io.File

class Task (var lines: MutableList<String>, var priority: Priority, var date: MutableList<String>, var time: MutableList<String>) {
    enum class Priority(val code: String) {
        C("101"),
        H("103"),
        N("102"),
        L("104"),
        NONE("0") }

    enum class Due(val code: String) {
        I("102"),
        T("103"),
        O("101") }

    lateinit var due: Due
}

object Tasklist {

    enum class State { WORKING, STOPPED }

    var state = State.WORKING
    var tasks = emptyList<Task>().toMutableList()

    object System {
        fun add() {
            val newPriority = Reader.readPriority()
            val newDate = Reader.readDate()
            val newTime = Reader.readTime()
            val newLines = Reader.readLines()

            //check if empty, create task and add to tasklist
            if (newLines.isNotEmpty()) {
                val newTask = Task(newLines, newPriority, newDate, newTime)
                tasks.add(newTask)
            }
        }

        fun print() {
            if (tasks.isEmpty()) {
                println("No tasks have been input")
            } else {
                //print top
                val top = "+----+------------+-------+---+---+--------------------------------------------+\n" +
                        "| N  |    Date    | Time  | P | D |                   Task                     |\n" +
                        "+----+------------+-------+---+---+--------------------------------------------+"
                println(top)

                //print the rest
                tasks.forEach { task ->

                    //check if due
                    val taskDate = LocalDate(task.date[0].toInt(), task.date[1].toInt(), task.date[2].toInt())
                    val currentDate = Clock.System.now().toLocalDateTime(TimeZone.of("UTC+0")).date
                    val numberOfDays = currentDate.daysUntil(taskDate)

                    //set property
                    task.due = when (numberOfDays) {
                        0 -> Task.Due.T
                        else -> if (numberOfDays > 1) Task.Due.I else Task.Due.O
                    }

                    //write deadlineDate and deadlineTime
                    val deadlineDate = "${task.date[0]}-${task.date[1]}-${task.date[2]}"
                    val deadlineTime = "${task.time[0]}:${task.time[1]}"

                    //print header - number, deadline, priority and dueTag
                    val taskNumber = tasks.indexOf(task) + 1
                    val spacing = if (taskNumber > 9) "" else " " //simple spacing for under 10
                    val header = "| $taskNumber$spacing | $deadlineDate | $deadlineTime | \u001B[${task.priority.code}m \u001B[0m | \u001B[${task.due.code}m \u001B[0m |"
                    print(header)

                    //print all lines
                    task.lines.forEach { line ->
                        //multiline print
                        var remainingString = line
                        while (remainingString.length > 44) { //while longer than 44...
                            println(remainingString.substring(0, 44) + "|") //print first 44 ch...
                            remainingString = remainingString.substring(44) //keep only remaining ch...
                            print("|    |            |       |   |   |") //print next line left block
                        }

                        //when or if shorter than 44, calculate margin and print final right block
                        val rightMargin = 44 - remainingString.length
                        var rightMarginString = ""
                        repeat(rightMargin) { rightMarginString += " " }
                        println("$remainingString$rightMarginString|")

                        //if there's another line next in the task, print next left block
                        if (line != task.lines.last()) print("|    |            |       |   |   |")
                    }
                    //print bottom at the end of task
                    println("+----+------------+-------+---+---+--------------------------------------------+")
                }
            }
        }

        fun edit() {
            print()

            if(tasks.isNotEmpty()) {
                var taskNumber = -1

                //ask task number until ok
                val askTaskNumber = true
                while (askTaskNumber) {
                    println("Input the task number (1-${tasks.size}):")
                    try {
                        taskNumber = readln().toInt()
                        //modify task
                        if (taskNumber in 1..tasks.size) {
                            var askField = false
                            do {
                                println("Input a field to edit (priority, date, time, task):")
                                val field = readln()
                                when (field) {
                                    "priority" -> {
                                        tasks[taskNumber - 1].priority = Reader.readPriority()
                                        break
                                    }
                                    "date" -> {
                                        tasks[taskNumber - 1].date = Reader.readDate()
                                        //println(tasks[taskNumber - 1].date) --- imprime ok la fecha nueva
                                        break
                                    }
                                    "time" -> {
                                        tasks[taskNumber - 1].time = Reader.readTime()
                                        break
                                    }
                                    "task" -> {
                                        tasks[taskNumber - 1].lines = Reader.readLines()
                                        break
                                    }
                                    else -> {
                                        println("Invalid field")
                                        askField = true //ask again
                                    }
                                }
                            } while (askField)
                            println("The task is changed")
                            break
                        } else {
                            println("Invalid task number")
                            continue
                        }
                    } catch (e: NumberFormatException) {
                        println("Invalid task number")
                        continue
                    }
                }


            }
        }

        fun delete() {
            print()

            if (tasks.isNotEmpty()) {
                var taskNumber: Int

                //ask task number until ok
                val askTaskNumber = true
                while (askTaskNumber) {
                    println("Input the task number (1-${tasks.size}):")
                    try {
                        taskNumber = readln().toInt()
                        //delete task
                        if (taskNumber in 1..tasks.size) {
                            tasks.removeAt(taskNumber - 1)
                            println("The task is deleted")
                        } else {
                            println("Invalid task number")
                            continue
                        }
                        break
                    } catch (e: NumberFormatException) {
                        println("Invalid task number")
                        continue
                    }
                }
            }

        }

        fun end() {
            println("Tasklist exiting!")
            state = State.STOPPED
        }
    }

    object Printer {
        fun askAction() = println("Input an action (add, print, edit, delete, end):")
        fun invalidAction() = println("The input action is invalid")
        fun askPriority() = println("Input the task priority (C, H, N, L):")
        fun askDate() = println("Input the date (yyyy-mm-dd):")
        fun invalidDate() = println("The input date is invalid")
        fun askTime() = println("Input the time (hh:mm):")
        fun invalidTime() = println("The input time is invalid")
        fun askTask() = println("Input a new task (enter a blank line to end):")
        fun blankTask() = println("The task is blank")
    }

    object Reader {
        fun readPriority(): Task.Priority {
            var newPriority = Task.Priority.NONE
            var askPriority = true
            while (askPriority) {
                Printer.askPriority()
                val inputPriority = readln().uppercase()
                newPriority = when (inputPriority) {
                    "C" ->  Task.Priority.C
                    "H" ->  Task.Priority.H
                    "N" ->  Task.Priority.N
                    "L" ->  Task.Priority.L
                    else -> Task.Priority.NONE
                }
                if (newPriority != Task.Priority.NONE) askPriority = false //exit loop
            }
            return newPriority
        }

        fun readDate(): MutableList<String> {
            val newDate = mutableListOf<String>()

            var askDate = true
            while (askDate) {
                Printer.askDate()

                val inputDate = try {
                    readln().split('-').map { it.toInt() }.toList()
                } catch (e: NumberFormatException) {
                    Printer.invalidDate()
                    continue
                }

                if (inputDate.size != 3) continue else { //if format is wrong, continue loop
                    try {
                        //validate date and save in intArray
                        val localDate = LocalDate(inputDate[0], inputDate[1], inputDate[2])
                        newDate.add(0, localDate.year.toString())
                        newDate.add(1, if (localDate.monthNumber < 10) "0${localDate.monthNumber}" else localDate.monthNumber.toString()) //add trailing 0s if needed
                        newDate.add(2, if (localDate.dayOfMonth < 10) "0${localDate.dayOfMonth}" else localDate.dayOfMonth.toString()) //add trailing 0s if needed
                        askDate = false
                    } catch (e: IllegalArgumentException) { //if date is invalid, continue loop //IllegalArgumentException - NumberFormatException
                        Printer.invalidDate()
                        continue
                    }
                }
            }
            return newDate
        }

        fun readTime(): MutableList<String> {
            val newTime = mutableListOf<String>()

            var askTime = true
            while (askTime) {
                Printer.askTime()

                val inputTime = try {
                    readln().split(':').map { it.toInt() }.toList()
                } catch (e: NumberFormatException) {
                    Printer.invalidTime()
                    continue
                }

                if (inputTime.size != 2) continue else { //if format is wrong, continue loop
                    try {
                        //validate time and save in intArray
                        val localDate = LocalDateTime(2000, 1, 1, inputTime[0], inputTime[1])
                        newTime.add(0, if (localDate.hour < 10) "0${localDate.hour}" else localDate.hour.toString()) //add trailing 0s if needed
                        newTime.add(1, if (localDate.minute < 10) "0${localDate.minute}" else localDate.minute.toString()) //add trailing 0s if needed
                        askTime = false
                    } catch (e: IllegalArgumentException) { //if time is invalid, continue loop
                        Printer.invalidTime()
                        continue
                    }
                }
            }
            return newTime
        }

        fun readLines(): MutableList<String> {
            val newLines = mutableListOf<String>()
            val askTask = true
            Printer.askTask()
            while (askTask) {
                var line = readln()

                //check if empty and break
                if (line.isEmpty()) {
                    break
                }

                //trim and save if not blank
                line = line.trim()
                if (line.isEmpty()) {
                    Printer.blankTask()
                    break
                } else newLines.add(line)
            }
            return newLines
        }
    }
}

fun main() {
    val system = Tasklist.System
    val printer = Tasklist.Printer
    /**
     * aiojdoiasjd
     */
    while (Tasklist.state == Tasklist.State.WORKING) {
        printer.askAction()
        val action = readln()
        when (action.lowercase()) {
            "add" -> system.add()
            "print" -> system.print()
            "edit" -> system.edit()
            "delete" -> system.delete()
            "end" -> system.end()
            else -> printer.invalidAction()
        }
    }
}