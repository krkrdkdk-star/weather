package com.example.weather.feature.weather.data.remote

import com.example.weather.feature.weather.domain.error.WeatherFailure
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs

class AmapWeatherRemoteDataSourceTest {

    @Test
    fun requestsAndParsesCurrentWeather() = runTest {
        val engine = MockEngine { request ->
            assertEquals("test-key", request.url.parameters["key"])
            assertEquals("110101", request.url.parameters["city"])
            assertEquals("base", request.url.parameters["extensions"])
            jsonResponse(LIVE_RESPONSE)
        }
        val client = testClient(engine)
        val dataSource = AmapWeatherRemoteDataSource("test-key", client)

        val result = dataSource.getCurrentWeather("110101")

        assertEquals("东城区", result.city)
        assertEquals("雾", result.weather)
        client.close()
    }

    @Test
    fun requestsAndParsesForecast() = runTest {
        val engine = MockEngine { request ->
            assertEquals("all", request.url.parameters["extensions"])
            jsonResponse(FORECAST_RESPONSE)
        }
        val client = testClient(engine)
        val dataSource = AmapWeatherRemoteDataSource("test-key", client)

        val result = dataSource.getForecast("110101")

        assertEquals("晴", result.days.single().dayCondition)
        assertEquals("2", result.days.single().nightTemperature)
        client.close()
    }

    @Test
    fun mapsServiceFailureToDomainFailure() = runTest {
        val engine = MockEngine {
            jsonResponse("""{"status":"0","info":"INVALID_USER_KEY","infocode":"10001"}""")
        }
        val client = testClient(engine)
        val dataSource = AmapWeatherRemoteDataSource("test-key", client)

        val failure = assertIs<WeatherFailure.ServiceRejected>(
            assertFailsWith { dataSource.getCurrentWeather("110101") },
        )

        assertEquals("10001", failure.code)
        assertEquals("INVALID_USER_KEY", failure.serviceMessage)
        client.close()
    }

    @Test
    fun mapsHttpFailureToDomainFailure() = runTest {
        val engine = MockEngine {
            respond("unavailable", HttpStatusCode.ServiceUnavailable)
        }
        val client = testClient(engine)
        val dataSource = AmapWeatherRemoteDataSource("test-key", client)

        val failure = assertIs<WeatherFailure.Http>(
            assertFailsWith { dataSource.getCurrentWeather("110101") },
        )

        assertEquals(503, failure.statusCode)
        client.close()
    }

    @Test
    fun mapsInvalidJsonToInvalidResponse() = runTest {
        val engine = MockEngine { jsonResponse("not-json") }
        val client = testClient(engine)
        val dataSource = AmapWeatherRemoteDataSource("test-key", client)

        assertFailsWith<WeatherFailure.InvalidResponse> {
            dataSource.getCurrentWeather("110101")
        }
        client.close()
    }

    @Test
    fun mapsMissingPayloadToMissingData() = runTest {
        val engine = MockEngine {
            jsonResponse("""{"status":"1","info":"OK","infocode":"10000","lives":[]}""")
        }
        val client = testClient(engine)
        val dataSource = AmapWeatherRemoteDataSource("test-key", client)

        val failure = assertFailsWith<WeatherFailure.MissingData> {
            dataSource.getCurrentWeather("110101")
        }

        assertEquals("current weather", failure.dataName)
        client.close()
    }

    @Test
    fun rejectsBlankCityBeforeRequesting() = runTest {
        var requestCount = 0
        val engine = MockEngine {
            requestCount++
            jsonResponse(LIVE_RESPONSE)
        }
        val client = testClient(engine)
        val dataSource = AmapWeatherRemoteDataSource("test-key", client)

        assertFailsWith<WeatherFailure.InvalidCity> {
            dataSource.getCurrentWeather("   ")
        }
        assertEquals(0, requestCount)
        client.close()
    }

    private fun testClient(engine: MockEngine) = HttpClient(engine) {
        expectSuccess = true
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }

    private fun MockRequestHandleScope.jsonResponse(body: String) = respond(
        content = body,
        status = HttpStatusCode.OK,
        headers = headersOf(HttpHeaders.ContentType, "application/json"),
    )

    private companion object {
        val LIVE_RESPONSE = """
            {
              "status":"1",
              "info":"OK",
              "infocode":"10000",
              "lives":[{
                "province":"北京",
                "city":"东城区",
                "weather":"雾",
                "temperature":"4",
                "winddirection":"西南",
                "windpower":"≤3",
                "humidity":"80",
                "reporttime":"2026-01-01 08:00:00"
              }]
            }
        """.trimIndent()

        val FORECAST_RESPONSE = """
            {
              "status":"1",
              "info":"OK",
              "infocode":"10000",
              "forecasts":[{
                "province":"北京",
                "city":"东城区",
                "reporttime":"2026-01-01 08:00:00",
                "casts":[{
                  "date":"2026-01-01",
                  "week":"4",
                  "dayweather":"晴",
                  "nightweather":"晴",
                  "daytemp":"8",
                  "nighttemp":"2",
                  "daywind":"北",
                  "nightwind":"北",
                  "daypower":"≤3",
                  "nightpower":"≤3"
                }]
              }]
            }
        """.trimIndent()
    }
}
