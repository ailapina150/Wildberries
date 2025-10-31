package com.example.wildberriesreview

import java.util.Properties

data class Config(
    var driverPath: String =  "chromedriver-win64/chromedriver.exe",
    var waitSeconds: Long = 10L,
    var maxScrollAttempts: Int = 5,
    var maxScrollNoNewContent: Int = 3,
    var outputFileName: String =  "wildberries_reviews.csv",
    var productPageUrl: String = "https://www.wildberries.ru/catalog/521896959/feedbacks?imtId=234818091&size=720932801",
    var maxParallelRequests: Int = 5
) {
    companion object {
        fun load(): Config {
            val properties = Properties().apply {
                val inputStream = Config::class.java
                    .classLoader
                    .getResourceAsStream("application.properties")
                if (inputStream == null) {
                    println("Файл application.properties не найден! Используются значения по умолчанию.")
                } else {
                    load(inputStream)
                }
            }

            return Config(
                driverPath = properties.getProperty("webdriver.chrome.driver", "chromedriver-win64/chromedriver.exe"),
                waitSeconds = properties.getProperty("webdriver.wait.seconds", "10").toLong(),
                maxScrollAttempts = properties.getProperty("scroll.attempts", "5").toInt(),
                maxScrollNoNewContent = properties.getProperty("scroll.no.new.content", "3").toInt(),
                outputFileName = properties.getProperty("output.filename", "wildberries_reviews.csv"),
                productPageUrl = properties.getProperty(
                    "feedbacks.url",
                    "https://www.wildberries.ru/catalog/521896959/feedbacks?imtId=234818091&size=720932801"
                ),
                maxParallelRequests = properties.getProperty("max.parallel.requests", "5").toInt()
            )
        }
    }
}
