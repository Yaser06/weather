package com.yaser.weather.repository;

import com.yaser.weather.model.Weather;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface WeatherRepository extends JpaRepository<Weather, String> {
    Optional<Weather> findFirstByRequestedCityNameOrderByUpdatedTimeDesc(String city);
}
