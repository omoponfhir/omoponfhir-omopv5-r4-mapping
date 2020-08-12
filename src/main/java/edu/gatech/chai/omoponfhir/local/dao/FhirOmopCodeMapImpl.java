package edu.gatech.chai.omoponfhir.local.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.hl7.fhir.dstu3.model.Coding;

import edu.gatech.chai.omoponfhir.local.model.FhirOmopCodeMapEntry;

public class FhirOmopCodeMapImpl extends BaseFhirOmopMap implements FhirOmopCodeMap {

	@Override
	public int save(FhirOmopCodeMapEntry codeMapEntry) {
		String sql = "INSERT INTO FhirOmopCodeMap (omop_concept, fhir_system, fhir_code, fhir_display) values (?,?,?,?)";

		try (Connection conn = this.connect(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
			pstmt.setLong(1, codeMapEntry.getOmopConcept());
			pstmt.setString(2, codeMapEntry.getFhirSystem());
			pstmt.setString(3, codeMapEntry.getFhirCode());
			pstmt.setString(4, codeMapEntry.getFhirDisplay());

			pstmt.executeUpdate();

			logger.info(
					"New Map entry data added (" + codeMapEntry.getOmopConcept() + ", " + codeMapEntry.getFhirSystem()
							+ ", " + codeMapEntry.getFhirCode() + ", " + codeMapEntry.getFhirDisplay());
		} catch (SQLException e) {
			logger.error(e.getMessage());
			e.printStackTrace();
		}

		return 0;
	}

	@Override
	public void update(FhirOmopCodeMapEntry codeMapEntry) {
		String sql = "UPDATE FhirOmopCodeMap SET fhir_system=?, fhir_code=?, fhir_display=? where omop_concept=?";

		try (Connection conn = this.connect(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
			pstmt.setString(1, codeMapEntry.getFhirSystem());
			pstmt.setString(2, codeMapEntry.getFhirCode());
			pstmt.setString(3, codeMapEntry.getFhirDisplay());
			pstmt.setLong(4, codeMapEntry.getOmopConcept());
			pstmt.executeUpdate();
			logger.info(
					"Map entry data (" + codeMapEntry.getOmopConcept() + ") updated to (" + codeMapEntry.getFhirSystem()
							+ ", " + codeMapEntry.getFhirCode() + ", " + codeMapEntry.getFhirDisplay() + ")");
		} catch (SQLException e) {
			System.out.println(e.getMessage());
		}
	}

	@Override
	public void delete(Long omopConcept) {
		String sql = "DELETE FROM FhirOmopCodeMap where omop_concept = ?";

		try (Connection conn = this.connect(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
			pstmt.setLong(1, omopConcept);
			pstmt.executeUpdate();
			logger.info("filter data (" + omopConcept + ") deleted");
		} catch (SQLException e) {
			System.out.println(e.getMessage());
		}
	}

	@Override
	public List<FhirOmopCodeMapEntry> get() {
		List<FhirOmopCodeMapEntry> codeMapEntryList = new ArrayList<FhirOmopCodeMapEntry>();

		String sql = "SELECT * FROM FhirOmopCodeMap";

		try (Connection conn = this.connect(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
			ResultSet rs = pstmt.executeQuery();
			while (rs.next()) {
				FhirOmopCodeMapEntry codeMapEntry = new FhirOmopCodeMapEntry();
				codeMapEntry.setOmopConcept(rs.getLong("omop_concept"));
				codeMapEntry.setFhirSystem(rs.getString("fhir_system"));
				codeMapEntry.setFhirCode(rs.getString("fhir_code"));
				codeMapEntry.setFhirDisplay(rs.getString("fhir_display"));
				codeMapEntryList.add(codeMapEntry);
			}
			logger.info(codeMapEntryList.size() + " Concept Map entries obtained");
		} catch (SQLException e) {
			System.out.println(e.getMessage());
		}

		return codeMapEntryList;
	}

	@Override
	public Long getOmopCodeFromFhirCoding(Coding fhirCoding) {
		Long retv = 0L;
		String sql = "SELECT * FROM FhirOmopCodeMap where fhir_system=? and fhir_code=?";

		String fhirSystem = fhirCoding.getSystem();
		String fhirCode = fhirCoding.getCode();
		if (fhirSystem == null || fhirSystem.isEmpty() || fhirCode == null || fhirCode.isEmpty()) {
			// We need to know system and code.
			return retv;
		}

		try (Connection conn = this.connect(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
			pstmt.setString(1, fhirSystem);
			pstmt.setString(2, fhirCode);

			ResultSet rs = pstmt.executeQuery();
			if (rs.next()) {
				retv = rs.getLong("omop_concept");
			}
			logger.debug("Omop Concept," + retv + " , found for " + fhirSystem + " and " + fhirCode);
		} catch (SQLException e) {
			System.out.println(e.getMessage());
		}

		return retv;
	}

	@Override
	public Coding getFhirCodingFromOmopConcept(Long omopConcept) {
		Coding retv = null;
		String sql = "SELECT * FROM FhirOmopCodeMap where omop_concept=?";

		try (Connection conn = this.connect(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
			pstmt.setLong(1, omopConcept);

			ResultSet rs = pstmt.executeQuery();
			String fhirSystem = "";
			String fhirCode = "";
			String fhirDisplay = "";
			if (rs.next()) {
				fhirSystem = rs.getString("fhir_system");
				fhirCode = rs.getString("fhir_code");
				fhirDisplay = rs.getString("fhir_display");

				retv = new Coding();
				retv.setSystem(fhirSystem);
				retv.setCode(fhirCode);
				retv.setDisplay(fhirDisplay);
			}
			logger.debug(" Fhir coding found " + fhirSystem + " and " + fhirCode + " and " + fhirDisplay
					+ " for Omop Concept " + omopConcept);
		} catch (SQLException e) {
			System.out.println(e.getMessage());
		}

		return retv;
	}

	@Override
	public Coding getFhirCodingFromOmopSourceString(String omopSourceString) {
		Coding retv = null;
		String sql = "SELECT * FROM FhirOmopCodeMap where fhir_display=?";

		try (Connection conn = this.connect(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
			pstmt.setString(1, omopSourceString);

			ResultSet rs = pstmt.executeQuery();
			String fhirSystem = "";
			String fhirCode = "";
			String fhirDisplay = "";
			if (rs.next()) {
				fhirSystem = rs.getString("fhir_system");
				fhirCode = rs.getString("fhir_code");
				fhirDisplay = rs.getString("fhir_display");

				retv = new Coding();
				retv.setSystem(fhirSystem);
				retv.setCode(fhirCode);
				retv.setDisplay(fhirDisplay);
			}
			logger.debug(" Fhir coding found " + fhirSystem + " and " + fhirCode + " and " + fhirDisplay
					+ " for Omop Source String " + omopSourceString);
		} catch (SQLException e) {
			System.out.println(e.getMessage());
		}

		return retv;
	}

}
