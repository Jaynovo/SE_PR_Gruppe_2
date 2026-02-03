package at.jku.se.gruppe2.infrastructure.persistence.statistics;

import at.jku.se.gruppe2.domain.model.home.Home;

import java.util.ArrayList;
import java.util.List;

public class StatisticsScopeRepository {

    public List<Integer> findDeviceIdsByHome(Home home) {
        if (home == null) return new ArrayList<>();
        int homeId = home.getId();
        List<Integer> deviceIds = new ArrayList<>();

        String request = """
                SELECT d.id
                FROM 
                """

        return deviceIds;
    }
}
