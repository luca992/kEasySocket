package co.spin.utils

class Queue<T> {
    val elements: MutableList<T> = mutableListOf()
    fun isEmpty() = elements.isEmpty()
    fun count() = elements.size
    fun enqueue(item: T) = elements.add(item)
    fun dequeue() = if (!isEmpty()) elements.removeAt(0) else null
    fun peek() = if (!isEmpty()) elements[0] else null

    override fun toString(): String = elements.toString()
}

fun <T> Queue<T>.push(items: Collection<T>) = items.forEach { this.enqueue(it) }