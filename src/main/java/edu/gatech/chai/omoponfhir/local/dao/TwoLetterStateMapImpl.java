package edu.gatech.chai.omoponfhir.local.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import edu.gatech.chai.omoponfhir.local.model.TwoLetterSateMapEntry;


public class TwoLetterStateMapImpl extends BaseFhirOmopMap implements TwoLetterStateMap {

	@Override
	public int save(TwoLetterSateMapEntry mapEntry) {
		String sql = "INSERT INTO TwoLetterStateMap (state_name, two_letter) values (?,?)";

		try (Connection conn = this.connect(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
			pstmt.setString(1, mapEntry.getStateName());
			pstmt.setString(2, mapEntry.getTwoLetter());

			pstmt.executeUpdate();

			logger.info(
					"New Map entry data added (" + mapEntry.getStateName() + ", " + mapEntry.getTwoLetter());
		} catch (SQLException e) {
			logger.error(e.getMessage());
			e.printStackTrace();
		}

		return 0;
	}

	@Override
	public void update(TwoLetterSateMapEntry mapEntry) {
		String sql = "UPDATE TwoLetterStateMap SET state_name=?, two_letter=? where state_name=?";

		try (Connection conn = this.connect(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
			pstmt.setString(1, mapEntry.getStateName());
			pstmt.setString(2, mapEntry.getTwoLetter());
			pstmt.setString(3, mapEntry.getStateName());

			pstmt.executeUpdate();
			logger.info(
					"Map entry data (" + mapEntry.getStateName() + ") updated to (" + mapEntry.getStateName()
							+ ", " + mapEntry.getTwoLetter() + ")");
		} catch (SQLException e) {
			System.out.println(e.getMessage());
		}
	}

	@Override
	public void delete(String stateName) {
		String sql = "DELETE FROM TwoLetterStateMap where state_name = ?";

		try (Connection conn = this.connect(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
			pstmt.setString(1, stateName);
			pstmt.executeUpdate();
			logger.info("filter data (" + stateName + ") deleted");
		} catch (SQLException e) {
			System.out.println(e.getMessage());
		}
	}

	@Override
	public List<TwoLetterSateMapEntry> get() {
		List<TwoLetterSateMapEntry> codeMapEntryList = new ArrayList<TwoLetterSateMapEntry>();

		String sql = "SELECT * FROM TwoLetterStateMap";

		try (Connection conn = this.connect(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
			ResultSet rs = pstmt.executeQuery();
			while (rs.next()) {
				TwoLetterSateMapEntry codeMapEntry = new TwoLetterSateMapEntry();
				codeMapEntry.setStateName(rs.getString("state_name"));
				codeMapEntry.setTwoLetter(rs.getString("two_letter"));
				codeMapEntryList.add(codeMapEntry);
			}
			logger.info(codeMapEntryList.size() + " Concept Map entries obtained");
		} catch (SQLException e) {
			System.out.println(e.getMessage());
		}

		return codeMapEntryList;
	}

	@Override
	public String getStateName(String twoLetter) {
		String retv = "";
		String sql = "SELECT * FROM TwoLetterStateMap where two_letter=?";

		try (Connection conn = this.connect(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
			pstmt.setString(1, twoLetter);

			ResultSet rs = pstmt.executeQuery();
			if (rs.next()) {
				retv = rs.getString("state_name");
			}
			logger.debug("State Name," + retv + " , found for " + twoLetter);
		} catch (SQLException e) {
			System.out.println(e.getMessage());
		}

		return retv;
	}

	@Override
	public String getTwoLetter(String stateName) {
		String retv = null;
		String sql = "SELECT * FROM TwoLetterStateMap where state_name=?";

		try (Connection conn = this.connect(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
			pstmt.setString(1, stateName);

			ResultSet rs = pstmt.executeQuery();
			if (rs.next()) {
				retv = rs.getString("two_letter");
			}
			logger.debug(" Two Letter found " + retv + " for State Name " + stateName);
		} catch (SQLException e) {
			System.out.println(e.getMessage());
		}

		return retv;
	}
}
