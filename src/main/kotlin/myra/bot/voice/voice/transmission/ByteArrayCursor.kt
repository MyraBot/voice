package myra.bot.voice.voice.transmission

class ByteArrayCursor(bytes: ByteArray) {

    private val bytes: ByteArray = bytes
    var cursor: Int = 0
    val size: Int get() = bytes.size

    fun writeBytes(bytes: ByteArray) {
        bytes.copyInto(this.bytes, cursor)
    }

    fun retrieve(amount: Int): ByteArray {
        val toIndex = when (cursor + amount > size) {
            true  -> size - 1
            false -> cursor + amount
        }
        val copy = bytes.copyOfRange(cursor, toIndex)
        cursor += amount
        return copy
    }

}