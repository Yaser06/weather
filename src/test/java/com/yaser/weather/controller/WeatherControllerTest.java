package com.yaser.weather.controller;

import com.yaser.weather.dto.WeatherDto;
import com.yaser.weather.exception.GeneralExceptionAdvice;
import com.yaser.weather.service.WeatherService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;

import static com.yaser.weather.TestSupport.formatter;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@AutoConfigureMockMvc
@SpringBootTest
public class WeatherControllerTest {
    @MockBean
    private WeatherService weatherService;

    @Autowired
    private MockMvc mockMvc;

    @Test
    public void testGetWeather_whenCityParameterValid_shouldReturnWeatherDto() throws Exception {
        LocalDateTime localDateTime = LocalDateTime.parse("2023-03-08 23:58", formatter);
        WeatherDto expected = new WeatherDto("Amsterdam", "Netherlands", 2, localDateTime);
        mockMvc = MockMvcBuilders.standaloneSetup(new WeatherController(weatherService))
                .setControllerAdvice(GeneralExceptionAdvice.class)
                .build();

        when(weatherService.getWeather("amsterdam")).thenReturn(expected);

        mockMvc.perform(get("/v1/api/weather/amsterdam").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cityName", is("Amsterdam")))
                .andExpect(jsonPath("$.country", is("Netherlands")))
                .andExpect(jsonPath("$.temperature", is(2)))
                .andExpect(jsonPath("$.updatedTime", is("2023-03-08 23:58")));

    }

    @Test
    public void testGetWeather_whenCityParameterIsNotValid_shouldReturnHTTP400BadRequest() throws Exception {
        mockMvc.perform(get("/v1/api/weather/"+123).contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

}
