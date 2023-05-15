package com.yaser.weather.service;


import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import com.yaser.weather.TestSupport;
import com.yaser.weather.constans.Constants;
import com.yaser.weather.dto.WeatherDto;
import com.yaser.weather.dto.WeatherResponse;
import com.yaser.weather.exception.ErrorResponse;
import com.yaser.weather.exception.WeatherStackApiException;
import com.yaser.weather.model.Weather;
import com.yaser.weather.repository.WeatherRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.time.Clock;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@AutoConfigureMockMvc
public class WeatherServiceTest extends TestSupport {

    private WeatherRepository weatherRepository;
    private RestTemplate restTemplate;
    private ObjectMapper objectMapper;
    private WeatherService weatherService;


    @BeforeEach
    public void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new ParameterNamesModule());

        restTemplate = mock(RestTemplate.class);
        weatherRepository = mock(WeatherRepository.class);
        Clock clock = mock(Clock.class);

        Constants constants = new Constants();
        constants.setWeatherStackApiBaseUrl("weather-base-api-url");
        constants.setApiKey("api-key");
        constants.setApiCallLimit(30);

        weatherService = new WeatherService(weatherRepository, restTemplate, clock);

        when(clock.instant()).thenReturn(getCurrentInstant());
        when(clock.getZone()).thenReturn(Clock.systemDefaultZone().getZone());
    }

    @Test
    public void testGetWeather_whenWeatherStackReturnError_shouldThrowWeatherStackApiException() throws Exception {
        String requestedCity = "xyz";
        String responseJson = getErrorResponseJson();
        ErrorResponse response = objectMapper.readValue(responseJson, ErrorResponse.class);

        when(weatherRepository.findFirstByRequestedCityNameOrderByUpdatedTimeDesc(requestedCity)).thenReturn(Optional.empty());
        when(restTemplate.getForEntity(WEATHER_STACK_API_URL + requestedCity, String.class)).thenReturn(ResponseEntity.ok(responseJson));

        assertThatThrownBy(() -> weatherService.getWeather(requestedCity))
                .isInstanceOf(WeatherStackApiException.class)
                .isEqualTo(new WeatherStackApiException(response));

        verify(restTemplate).getForEntity(WEATHER_STACK_API_URL + requestedCity, String.class);
        verify(weatherRepository).findFirstByRequestedCityNameOrderByUpdatedTimeDesc(requestedCity);
        verifyNoMoreInteractions(weatherRepository);
    }

    @Test
    public void testGetWeather_whenWeatherStackReturnUnknownResponse_shouldThrowRuntimeException() {
        String responseJson = "UnknownResponse";

        when(weatherRepository.findFirstByRequestedCityNameOrderByUpdatedTimeDesc(requestedCity)).thenReturn(Optional.empty());
        when(restTemplate.getForEntity(WEATHER_STACK_API_URL + requestedCity, String.class)).thenReturn(ResponseEntity.ok(responseJson));

        assertThatThrownBy(() -> weatherService.getWeather(requestedCity))
                .isInstanceOf(RuntimeException.class);

        verify(restTemplate).getForEntity(WEATHER_STACK_API_URL + requestedCity, String.class);
        verify(weatherRepository).findFirstByRequestedCityNameOrderByUpdatedTimeDesc(requestedCity);
        verifyNoMoreInteractions(weatherRepository);
    }

    @Test
    public void testGetWeather_whenCityAlreadyExistsAndNotOlderThan30Minutes_shouldReturnWeatherDtoAndNotCallWeatherStackAPI() throws Exception {
        String responseJson = getAmsterdamWeatherJson();
        WeatherResponse response = objectMapper.readValue(responseJson, WeatherResponse.class);
        Weather weather = getSavedWeather(response.location().localtime());

        WeatherDto expected = new WeatherDto(weather.getCityName(), weather.getCountry(), weather.getTemperature(), weather.getUpdatedTime());

        when(weatherRepository.findFirstByRequestedCityNameOrderByUpdatedTimeDesc(requestedCity)).thenReturn(Optional.of(weather));

        WeatherDto result = weatherService.getWeather(requestedCity);

        assertEquals(expected, result);

        verifyNoInteractions(restTemplate);
        verify(weatherRepository).findFirstByRequestedCityNameOrderByUpdatedTimeDesc(requestedCity);
        verifyNoMoreInteractions(weatherRepository);
    }

    @Test
    public void testClearCache() {
        Logger logger = (Logger) LoggerFactory.getLogger(WeatherService.class);
        ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
        listAppender.start();
        logger.addAppender(listAppender);

        weatherService.clearCache();

        List<ILoggingEvent> logsList = listAppender.list;

        assertEquals("Caches are cleared", logsList.get(0).getMessage());
        assertEquals(Level.INFO, logsList.get(0).getLevel());
    }

}
