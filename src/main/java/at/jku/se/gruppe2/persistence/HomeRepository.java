package at.jku.se.gruppe2.persistence;

import at.jku.se.gruppe2.domain.*;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

public class HomeRepository {
    public Optional<Home> getHomeById(int id) {
        String request = "SELECT * FROM home WHERE id = ?";

        return JdbcTemplate.queryForObject(
                request,
                preparedStatement ->
                        preparedStatement.setInt(1, id),
                this::mapHome
        );
    }

    private Home mapHome(ResultSet rs) throws SQLException {
        Home home = new Home();
        home.setId(rs.getInt("id"));
        home.setFloors(rs.getInt("floors"));

        //TODO This still needs creating
        home.setAddress(
                AddressRepository.getAddressById(rs.getInt("address_information")));
        return home;
    }
}
