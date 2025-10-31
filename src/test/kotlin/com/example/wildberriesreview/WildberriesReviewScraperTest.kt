package com.example.wildberriesreview

import kotlinx.coroutines.*
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.openqa.selenium.By
import org.openqa.selenium.JavascriptExecutor
import org.openqa.selenium.WebDriver
import org.openqa.selenium.WebElement
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.chrome.ChromeOptions
import org.openqa.selenium.support.ui.WebDriverWait
import java.io.BufferedWriter
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets
import java.time.Duration

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class WildberriesReviewScraperTest {
    private val config = Config.load()
    private lateinit var driver: WebDriver
    private lateinit var wait: WebDriverWait
    private val feedbackSelector = By.cssSelector(ReviewUtils.FEEDBACK_SELECTOR)

    @BeforeAll
    fun setUp() {
        println("$config")
        System.setProperty("webdriver.chrome.driver", config.driverPath)//путь к исполняемому файлу ChromeDriver
        System.setProperty(
            "webdriver.http.factory",
            "jdk-http-client"
        )//использовать HTTP-клиент из JDK для стабильности соединения

        //Уменьшение количетва логов от ChromeDriver в консоль
        System.setProperty("webdriver.chrome.silentOutput", "true")
        java.util.logging.Logger.getLogger("org.openqa.selenium").setLevel(java.util.logging.Level.SEVERE)

        val options = ChromeOptions()
        options.addArguments("--headless=new")
        options.addArguments("--disable-gpu")
        options.addArguments("--window-size=1920,1080")
        options.addArguments("--no-sandbox")
        options.addArguments("--disable-dev-shm-usage")
        options.addArguments("--remote-allow-origins=*")

        driver = ChromeDriver(options)
        wait = WebDriverWait(driver, Duration.ofSeconds(config.waitSeconds))

    }

    @Test
    fun scrape_wildberries_reviews_to_csv() {
        driver.get(config.productPageUrl)
        waitForFeedback()

        println("Начало сбора отзывов с ${config.productPageUrl}")
        val allReviews = mutableListOf<Review>()

        var scrollAttempts = 0
        var consecutiveNoNewContent = 0
        var contAll = 0

        while (scrollAttempts < config.maxScrollAttempts && consecutiveNoNewContent < config.maxScrollNoNewContent) {
            println("Попытка скролла ${scrollAttempts + 1} из ${config.maxScrollAttempts}")

            // Скроллим и ждем
            (driver as JavascriptExecutor).executeScript("window.scrollTo(0, document.body.scrollHeight);")
            waitForFeedback(contAll)

            // Пытаемся найти новые отзывы
            val currentReviewElements = driver.findElements(feedbackSelector)
                .stream()
                .skip(contAll.toLong())
                .toList()

            val batch = runBlocking {
                processReviews(currentReviewElements)
            }
            if (batch.isEmpty()) {
                consecutiveNoNewContent++
            } else {
                consecutiveNoNewContent = 0
                allReviews.addAll(batch)
                contAll = allReviews.size
            }
            scrollAttempts++

            println("Собрано $contAll отзывов. Найдено новых: ${batch.size}")
        }
        saveReviewsToCsv(allReviews)
        println("Сбор завершен. Сохранено ${allReviews.size} отзывов в файл: ${config.outputFileName}")
    }

    @AfterAll
    fun tearDown() {
        driver.quit()
    }

    private fun saveReviewsToCsv(reviews: List<Review>) {
        if (reviews.isEmpty()) {
            println("Нет отзывов для сохранения")
            return
        }
        val file = File(config.outputFileName)
        try {
            val startTime = System.currentTimeMillis()

            BufferedWriter(OutputStreamWriter(FileOutputStream(file), StandardCharsets.UTF_8), 8192 * 4).use { writer ->
                writer.write("\"Дата публикации\",\"Автор\",\"Текст отзыва\",\"Оценка\",\"Количество фотографий\",\"Наличие видео\",\"Теги\"")
                writer.newLine()
                // Измеряем, что быстрее
                if (reviews.size > 2000) {
                    // Для большого количества - параллельная обработка
                    reviews.parallelStream().forEach { review ->
                        val line = review.toCsvString().replace("\n", " ") + "\n"
                        synchronized(writer) {
                            writer.write(line)
                        }
                    }
                } else {
                    // Для малого количества - последовательная обработка
                    reviews.forEach { review ->
                        writer.write(review.toCsvString().replace("\n", " ") + "\n")
                    }
                }
            }

            val endTime = System.currentTimeMillis()
            println("Успешно сохранено ${reviews.size} отзывов в ${config.outputFileName} за ${endTime - startTime} мс")
        } catch (e: Exception) {
            println("Ошибка при сохранении в CSV: ${e.message}")
            throw e
        }
    }


    private suspend fun processReviews(
        elements: List<WebElement>,
    ): List<Review> = coroutineScope {
        val results = mutableListOf<Review>()
        elements.chunked(config.maxParallelRequests).forEach { chunk ->
            val chunkResults = chunk.map { element ->
                async(Dispatchers.IO) {
                    try {
                        ReviewUtils.parseReview(element)
                    } catch (e: Exception) {
                        println("Ошибка обработки: ${e.message}")
                        null
                    }
                }
            }.awaitAll()

            results.addAll(chunkResults.filterNotNull())
        }
        results
    }

    fun waitForFeedback() {
        val wait = WebDriverWait(driver, Duration.ofSeconds(config.waitSeconds))
        try {
            wait.until {
                driver.findElements(feedbackSelector).isNotEmpty()
            }
        } catch (_: Exception) {
            println("Отзывы не загрузились в течение ${config.waitSeconds} секунд")
        }

    }

    fun waitForFeedback(oldCounter: Int): Boolean {
        val wait = WebDriverWait(driver, Duration.ofSeconds(config.waitSeconds))
        var stableCount = 0
        var lastCount = oldCounter

        return try {
            wait.until {
                val currentCount = driver.findElements(feedbackSelector).size

                // Проверяем стабильность количества
                val isStable = currentCount == lastCount
                val hasNewElements = currentCount > oldCounter

                if (isStable) {
                    stableCount++
                } else {
                    stableCount = 0
                    lastCount = currentCount
                }

                // Условие: есть новые элементы И количество стабильно 2 раза подряд
                hasNewElements && stableCount >= 2
            }
            true
        } catch (_: Exception) {
            if(lastCount == oldCounter) {
                println("Новые отзывы не найдены за ${config.waitSeconds} секунд")
            }
            false
        }
    }

}
