package com.example.wildberriesreview

import org.openqa.selenium.By
import org.openqa.selenium.WebElement
import kotlin.collections.firstOrNull

object ReviewUtils {
    const val FEEDBACK_SELECTOR = ".comments__item.feedback"
    private const val DATE_SELECTOR = ".feedback__date"
    private const val AUTHOR_SELECTOR = ".feedback__header"
    private const val TEXT_SELECTOR = ".feedback__text"
    private const val PRO_SELECTOR = ".feedback__text--item-pro"
    private const val CON_SELECTOR = ".feedback__text--item-con"
    private const val RATING_SELECTOR = ".feedback__rating"
    private const val PHOTO_SELECTOR = ".feedback__photo img"
    private const val VIDEO_SELECTOR = ".feedback__video-btn"
    private const val BABLES_SELECTOR = ".feedbacks-bables__item"
    private const val STATE_SELECTOR = ".feedback__state--text"

    fun extractDate(reviewElement: WebElement): String =
        reviewElement.findElements(By.cssSelector(DATE_SELECTOR))
            .firstOrNull()
            ?.text
            ?: "Дата не указана"

    fun extractAuthor(reviewElement: WebElement): String =
        reviewElement.findElements(By.cssSelector(AUTHOR_SELECTOR))
            .firstOrNull()
            ?.text
            ?: "Автор не указана"

    fun extractText(element: WebElement): String {
        val textParts = mutableListOf<String>()

        val mainText = element.findElements(By.cssSelector(TEXT_SELECTOR))
        if (mainText.isNotEmpty()) {
            val prosText = mainText.joinToString(" ") { it.text.trim() }
            textParts.add(prosText)
        }

        val prosElements = element.findElements(By.cssSelector(PRO_SELECTOR))
        if (prosElements.isNotEmpty()) {
            val prosText = prosElements.joinToString(" ") { it.text.trim() }
            textParts.add(prosText)
        }

        val consElements = element.findElements(By.cssSelector(CON_SELECTOR))
        if (consElements.isNotEmpty()) {
            val consText = consElements.joinToString(" ") { it.text.trim() }
            textParts.add(consText)
        }

        return if (textParts.isNotEmpty()) {
            textParts.joinToString(". ").replace(Regex("\\s+"), " ").trim()
        } else {
            "Нет текста"
        }
    }


    fun extractRating(element: WebElement): Int {
        return try {
            val ratingElement = element.findElement(By.cssSelector(RATING_SELECTOR))
            val classAttribute = ratingElement.getAttribute("class")
            when {
                classAttribute.contains("star5") -> 5
                classAttribute.contains("star4") -> 4
                classAttribute.contains("star3") -> 3
                classAttribute.contains("star2") -> 2
                classAttribute.contains("star1") -> 1
                else -> 5
            }

        } catch (_: Exception) {
            0
        }
    }

    fun extractPhotoCount(reviewElement: WebElement): Int =
        reviewElement.findElements(By.cssSelector(PHOTO_SELECTOR)).size -
                reviewElement.findElements(By.cssSelector(".feedback__video-btn")).size

    fun extractHasVideo(reviewElement: WebElement): Boolean =
        reviewElement.findElements(By.cssSelector(VIDEO_SELECTOR)).isNotEmpty()

    fun extractTags(element: WebElement): String {
        val tagsParts = mutableListOf<String>()

        // Теги из "Плюсы товара"
        val prosItems = element.findElements(By.cssSelector(BABLES_SELECTOR))
        if (prosItems.isNotEmpty()) {
            tagsParts.addAll(prosItems.map { it.text.trim() })
        }

        // Статус "Выкупили"
        val purchasedElement = element.findElements(By.cssSelector(STATE_SELECTOR))
        if (purchasedElement.isNotEmpty()) {
            tagsParts.addAll(prosItems.map { it.text.trim() })
        }

        return if (tagsParts.isNotEmpty()) {
            tagsParts.joinToString("; ")
        } else {
            "Нет тегов"
        }
    }

    fun parseReview(reviewElement: WebElement): Review = Review(
        date = extractDate(reviewElement),
        author = extractAuthor(reviewElement),
        text = extractText(reviewElement),
        rating = extractRating(reviewElement),
        photoCount = extractPhotoCount(reviewElement),
        hasVideo = extractHasVideo(reviewElement),
        tags = extractTags(reviewElement)
    )

}
