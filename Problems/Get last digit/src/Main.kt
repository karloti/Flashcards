// write your code here
fun getLastDigit(a: Int) = a.toString().last()

/* Do not change code below */
fun main() {
    val a = readLine()!!.toInt()
    println(getLastDigit(a))
}
