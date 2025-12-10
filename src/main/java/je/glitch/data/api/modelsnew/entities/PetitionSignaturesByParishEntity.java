package je.glitch.data.api.modelsnew.entities;

import lombok.Data;
import java.sql.ResultSet;
import java.sql.SQLException;

@Data
public class PetitionSignaturesByParishEntity {
    private int id;
    private long petitionId;
    private String parishName;
    private Integer signatureCount;

    public static PetitionSignaturesByParishEntity fromResultSet(ResultSet rs) throws SQLException {
        PetitionSignaturesByParishEntity e = new PetitionSignaturesByParishEntity();

        e.setId(rs.getInt("id"));
        e.setPetitionId(rs.getLong("petitionId"));
        e.setParishName(rs.getString("parishName"));

        int count = rs.getInt("signatureCount");
        e.setSignatureCount(rs.wasNull() ? null : count);

        return e;
    }
}
