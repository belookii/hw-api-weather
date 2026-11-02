package weather;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class WeatherApp {

    // Настройки API
    private static final String API_KEY = "f6d6778b-e514-4ab1-950c-bf6b9d3bd03a";
    private static final double LAT = 55.67;
    private static final double LON = 37.27;
    private static final int LIMIT = 7;

    private static final String URL = "https://api.weather.yandex.ru/v2/forecast";

    public static void main(String[] args) throws Exception {
        // GET-запрос
        String fullUrl = "%s?lat=%s&lon=%s&limit=%d".formatted(URL, LAT, LON, LIMIT);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(fullUrl))
                .header("X-Yandex-Weather-Key", API_KEY)
                .header("Accept", "application/json")
                .GET()
                .build();

        HttpClient client = HttpClient.newHttpClient();
        HttpResponse<String> response =
                client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        if (response.statusCode() != 200) {
            System.err.printf("Ошибка HTTP %d: %s%n", response.statusCode(), response.body());
            return;
        }

        String body = response.body();

        // Полный JSON «как есть»
        System.out.println("Ответ сервиса как есть:");
        System.out.println(body);

        // Разбираем в модели
        ObjectMapper mapper = new ObjectMapper();
        WeatherResponse data = mapper.readValue(body, WeatherResponse.class);

        // Достаем текущую температуру fact.temp
        if (data.fact() == null || data.fact().temp() == null) {
            System.err.println("В ответе нет fact.temp");
            return;
        }
        int currentTemp = data.fact().temp();
        System.out.printf("Текущая температура (fact.temp): %s%n", currentTemp);

        // Достаем среднюю температуру по первым LIMIT суткам
        if (data.forecasts() == null || data.forecasts().isEmpty()) {
            System.err.println("В ответе нет forecasts");
            return;
        }

        List<Forecast> firstDays = data.forecasts().stream()
                .limit(LIMIT)
                .toList();

        var temps = firstDays.stream()
                .map(f -> f.parts().day().tempAvg())
                .filter(t -> t != null)
                .toList();

        if (temps.isEmpty()) {
            System.err.println("Не удалось извлечь parts.day.temp_avg");
            return;
        }

        double average = temps.stream().mapToInt(Integer::intValue).average().orElseThrow();

        System.out.printf("Значения temp_avg за %d суток: %s%n", temps.size(), temps);
        System.out.printf("Средняя температура за %d суток: %.2f °C%n", temps.size(), average);
    }

    // Модели ответа API
    @JsonIgnoreProperties(ignoreUnknown = true)
    record WeatherResponse(
            @JsonProperty("fact") Fact fact,
            @JsonProperty("forecasts") List<Forecast> forecasts
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Fact(@JsonProperty("temp") Integer temp) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Forecast(@JsonProperty("parts") Parts parts) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Parts(@JsonProperty("day") DayPart day) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record DayPart(@JsonProperty("temp_avg") Integer tempAvg) {}
}