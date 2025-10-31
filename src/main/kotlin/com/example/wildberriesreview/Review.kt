package com.example.wildberriesreview

data class Review(
    val date: String,
    val author: String,
    val text: String,
    val rating: Int,
    val photoCount: Int,
    val hasVideo: Boolean,
    val tags: String
) {

    fun toCsvString(): String {
        val escapedText = text.replace("\"", "\"\"").replace(",", "，")
        return "\"$date\",\"$author\",\"$escapedText\",$rating,$photoCount,$hasVideo,\"$tags\""
    }
}
