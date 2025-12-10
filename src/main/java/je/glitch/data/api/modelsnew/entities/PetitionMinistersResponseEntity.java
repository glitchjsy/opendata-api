package je.glitch.data.api.modelsnew.entities;

import lombok.Data;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Date;

@Data
public class PetitionMinistersResponseEntity {
    private int id;
    private String createdAt;
    private String updatedAt;
    private Date publishedOn;
    private long petitionId;
    private String summary;
    private String description;

    public static PetitionMinistersResponseEntity fromResultSet(ResultSet rs) throws SQLException {
        PetitionMinistersResponseEntity e = new PetitionMinistersResponseEntity();

        e.setId(rs.getInt("id"));
        e.setCreatedAt(rs.getString("createdAt"));
        e.setUpdatedAt(rs.getString("updatedAt"));
        e.setPublishedOn(rs.getDate("publishedOn"));
        e.setPetitionId(rs.getLong("petitionId"));
        e.setSummary(rs.getString("summary"));
        e.setDescription(rs.getString("description"));

        return e;
    }
}
