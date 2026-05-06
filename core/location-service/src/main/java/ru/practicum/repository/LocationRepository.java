package ru.practicum.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.practicum.model.Location;

import java.util.List;

@Repository
public interface LocationRepository extends JpaRepository<Location, Long> {

    @Query(value = """
        SELECT l.* FROM locations l
        WHERE distance(:lat, :lon, l.latitude, l.longitude) <= COALESCE(:radius, l.radius, 1.0)
        """, nativeQuery = true)
    List<Location> findLocationsContainingPoint(@Param("lat") Double lat,
                                                @Param("lon") Double lon,
                                                @Param("radius") Double radius);

}
