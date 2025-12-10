package je.glitch.data.api.modelsnew.entities;

import lombok.Data;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Date;

@Data
public class PetitionDebateEntity {
    private int id;
    private String debatedOn;
    private long petitionId;
    private String transcriptUrl;
    private String videoUrl;
    private String debatePackUrl;
    private String overview;

    public static PetitionDebateEntity fromResultSet(ResultSet rs) throws SQLException {
        PetitionDebateEntity e = new PetitionDebateEntity();

        e.setId(rs.getInt("id"));
        e.setDebatedOn(rs.getString("debatedOn"));
        e.setPetitionId(rs.getLong("petitionId"));
        e.setTranscriptUrl(rs.getString("transcriptUrl"));
        e.setVideoUrl(rs.getString("videoUrl"));
        e.setDebatePackUrl(rs.getString("debatePackUrl"));
        e.setOverview(rs.getString("overview"));

        return e;
    }
}
