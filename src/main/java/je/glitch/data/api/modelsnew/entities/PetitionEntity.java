package je.glitch.data.api.modelsnew.entities;

import lombok.Data;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;

@Data
public class PetitionEntity {

    private long id;
    private String createdAt;
    private String updatedAt;
    private String openedAt;
    private String closedAt;
    private String rejectedAt;
    private String moderationThresholdReachedAt;
    private String responseThresholdReachedAt;
    private String debateThresholdReachedAt;
    private String scheduledDebateDate;
    private String debateOutcomeAt;

    private String state;
    private String creatorName;
    private String title;
    private String summary;
    private String description;

    private Integer signatureCount;
    private String rejectionCode;
    private String rejectionDetails;
    private boolean fullyProcessedClosed;

    private PetitionDebateEntity debate;
    private List<PetitionMinistersResponseEntity> ministersResponses;
    private List<PetitionSignaturesByParishEntity> signaturesByParish;

    public static PetitionEntity fromResultSet(ResultSet rs) throws SQLException {
        PetitionEntity e = new PetitionEntity();

        e.setId(rs.getLong("id"));
        e.setCreatedAt(rs.getString("createdAt"));
        e.setUpdatedAt(rs.getString("updatedAt"));
        e.setOpenedAt(rs.getString("openedAt"));
        e.setClosedAt(rs.getString("closedAt"));
        e.setRejectedAt(rs.getString("rejectedAt"));
        e.setModerationThresholdReachedAt(rs.getString("moderationThresholdReachedAt"));
        e.setResponseThresholdReachedAt(rs.getString("responseThresholdReachedAt"));
        e.setDebateThresholdReachedAt(rs.getString("debateThresholdReachedAt"));
        e.setScheduledDebateDate(rs.getString("scheduledDebateDate"));
        e.setDebateOutcomeAt(rs.getString("debateOutcomeAt"));

        e.setState(rs.getString("state"));
        e.setCreatorName(rs.getString("creatorName"));
        e.setTitle(rs.getString("title"));
        e.setSummary(rs.getString("summary"));
        e.setDescription(rs.getString("description"));

        int count = rs.getInt("signatureCount");
        e.setSignatureCount(rs.wasNull() ? null : count);

        e.setRejectionCode(rs.getString("rejectionCode"));
        e.setRejectionDetails(rs.getString("rejectionDetails"));
        e.setFullyProcessedClosed(rs.getBoolean("fullyProcessedClosed"));

        return e;
    }
}
