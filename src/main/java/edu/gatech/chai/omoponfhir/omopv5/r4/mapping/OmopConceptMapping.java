/*******************************************************************************
 * Copyright (c) 2019 Georgia Tech Research Institute
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package edu.gatech.chai.omoponfhir.omopv5.r4.mapping;

import org.hl7.fhir.r4.model.codesystems.AdministrativeGender;
import org.hl7.fhir.r4.model.codesystems.OrganizationType;
import org.hl7.fhir.r4.model.codesystems.V3ActCode;
import org.hl7.fhir.r4.model.codesystems.ObservationCategory;
import org.hl7.fhir.r4.model.codesystems.ConditionCategory;
import org.hl7.fhir.exceptions.FHIRException;

public enum OmopConceptMapping {
	
	/**
	 * AdministrativeGender Mapping to OMOP Gender Concept ID.
	 */
	MALE(AdministrativeGender.MALE.toCode(), 8507L),
	FEMALE(AdministrativeGender.FEMALE.toCode(), 8532L),
	UNKNOWN(AdministrativeGender.UNKNOWN.toCode(), 8551L),
	ADMIN_OTHER(AdministrativeGender.OTHER.toCode(), 8521L),
	ADMIN_NULL(AdministrativeGender.NULL.toCode(),8551L),
	
	/**
	 * OrganizationType Mapping
	 */
	PROV(OrganizationType.PROV.toCode(), 4107295L), 
	DEPT(OrganizationType.DEPT.toCode(), 4318944L), 
	TEAM(OrganizationType.TEAM.toCode(), 4217012L), 
	GOVT(OrganizationType.GOVT.toCode(), 4195901L), 
	INS(OrganizationType.INS.toCode(), 8844L), 
	EDU(OrganizationType.EDU.toCode(), 4030303L), 
	RELI(OrganizationType.RELI.toCode(), 8844L), 
	CRS(OrganizationType.CRS.toCode(), 8844L), 
	CG(OrganizationType.CG.toCode(), 4127377L), 
	BUS(OrganizationType.BUS.toCode(), 8844L), 
	ORG_OTHER(OrganizationType.OTHER.toCode(), 8844L), 
	ORG_NULL(OrganizationType.NULL.toCode(), 8844L),
	
	/**
	 * Observation Category Mapping
	 */
	SOCIALHISTORY(ObservationCategory.SOCIALHISTORY.toCode(), 44788346L),
	VITAL(ObservationCategory.VITALSIGNS.toCode(), 44806924L),
	IMAGING(ObservationCategory.IMAGING.toCode(), 44788404L),
	LABORATORY(ObservationCategory.LABORATORY.toCode(), 44791245L),
	PROCEDURE(ObservationCategory.PROCEDURE.toCode(), 44810322L),
	SURVEY(ObservationCategory.SURVEY.toCode(), 45905771L),
	EXAM(ObservationCategory.EXAM.toCode(), 44803645L),
	THERAPY(ObservationCategory.THERAPY.toCode(), 44807025L),
	OBS_NULL(ObservationCategory.NULL.toCode(), 0L),
	
	/**
	 * Encounter Class Mapping
	 */
	INPATIENT(V3ActCode.IMP.toCode(), 9201L),
	OUTPATIENT(V3ActCode.AMB.toCode(), 9202L),
	EMERGENCY(V3ActCode.EMER.toCode(), 9203L),
	ENC_NULL(V3ActCode.NULL.toCode(), 0L),
	
	/**
	 * Condition Category Mapping
	 */
	PROBLEM_LIST_ITEM(ConditionCategory.PROBLEMLISTITEM.toCode(), 38000245L),
	// Couldn't find exact matching for encounter-diagnosis. This is 'Observation recorded from EHR' in Concept Table
	ENCOUNTER_DIAGNOSIS(ConditionCategory.ENCOUNTERDIAGNOSIS.toCode(), 43542353L),
	COND_NULL(ConditionCategory.NULL.toCode(), 0L);
	
	/**
	 * DocumentReference Type Mapping
	 */
	
	
	public static Long omopForAdministrativeGenderCode(String administrativeGenderCode) throws FHIRException {
		if (administrativeGenderCode == null || administrativeGenderCode.isEmpty()) {
			throw new FHIRException("Unknow Administrative Gender code: '"+administrativeGenderCode+"'");
		}
		
		if (MALE.getFhirCode().equals(administrativeGenderCode)) {
			return MALE.getOmopConceptId(); // MALE in OMOP
		}
		if (FEMALE.getFhirCode().equals(administrativeGenderCode)) {
			return FEMALE.getOmopConceptId(); // FEMALE in OMOP
		}
		if (UNKNOWN.getFhirCode().equals(administrativeGenderCode)) {
			return UNKNOWN.getOmopConceptId();
		}
		if (ADMIN_OTHER.getFhirCode().equals(administrativeGenderCode)) {
			return ADMIN_OTHER.getOmopConceptId();
		} else {
			return ADMIN_NULL.getOmopConceptId();
		}
	}
	
	public static Long omopForOrganizationTypeCode(String organizationTypeCode) throws FHIRException {
		if (organizationTypeCode == null || organizationTypeCode.isEmpty()) {
			throw new FHIRException("Unknow Organization Type code: '"+organizationTypeCode+"'");
		}
		
		if (PROV.getFhirCode().equals(organizationTypeCode)) {
			return PROV.getOmopConceptId();
		}
		if (DEPT.getFhirCode().equals(organizationTypeCode)) {
			return DEPT.getOmopConceptId();
		}
		if (TEAM.getFhirCode().equals(organizationTypeCode)) {
			return TEAM.getOmopConceptId();
		}
		if (GOVT.getFhirCode().equals(organizationTypeCode)) {
			return GOVT.getOmopConceptId();
		}
		if (INS.getFhirCode().equals(organizationTypeCode)) {
			return INS.getOmopConceptId(); // Other place of service... can't find right one.
		}
		if (EDU.getFhirCode().equals(organizationTypeCode)) {
			return EDU.getOmopConceptId();
		}
		if (RELI.getFhirCode().equals(organizationTypeCode)) {
			return RELI.getOmopConceptId();
		}
		if (CRS.getFhirCode().equals(organizationTypeCode)) {
			return CRS.getOmopConceptId();
		}
		if (CG.getFhirCode().equals(organizationTypeCode)) {
			return CG.getOmopConceptId();
		}
		if (BUS.getFhirCode().equals(organizationTypeCode)) {
			return BUS.getOmopConceptId();
		}
		if (ORG_OTHER.getFhirCode().equals(organizationTypeCode)) {
			return ORG_OTHER.getOmopConceptId();
		} else {
			return ORG_NULL.getOmopConceptId();
		}
	}
	
	public static Long omopForObservationCategoryCode(String observationCategoryCode) throws FHIRException {
		if (observationCategoryCode == null || observationCategoryCode.isEmpty()) {
			throw new FHIRException("Unknow Observation Category code: '"+observationCategoryCode+"'");
		}
		
		if (SOCIALHISTORY.getFhirCode().equals(observationCategoryCode)) {
			return SOCIALHISTORY.getOmopConceptId();
		}
		if (VITAL.getFhirCode().equals(observationCategoryCode)) {
			return VITAL.getOmopConceptId();
		}
		if (IMAGING.getFhirCode().equals(observationCategoryCode)) {
			return IMAGING.getOmopConceptId();
		}
		if (LABORATORY.getFhirCode().equals(observationCategoryCode)) {
			return LABORATORY.getOmopConceptId();
		}
		if (PROCEDURE.getFhirCode().equals(observationCategoryCode)) {
			return PROCEDURE.getOmopConceptId();
		}
		if (SURVEY.getFhirCode().equals(observationCategoryCode)) {
			return SURVEY.getOmopConceptId();
		}
		if (EXAM.getFhirCode().equals(observationCategoryCode)) {
			return EXAM.getOmopConceptId();
		}
		if (THERAPY.getFhirCode().equals(observationCategoryCode)) {
			return THERAPY.getOmopConceptId();
		} 
		
		return OBS_NULL.getOmopConceptId();
	}
	
	public static Long omopForConditionCategoryCode(String conditionCategoryCode) throws FHIRException {
		if (conditionCategoryCode == null || conditionCategoryCode.isEmpty()) {
			throw new FHIRException("Unknow Condition Category code: '"+conditionCategoryCode+"'");
		}
		
		if (PROBLEM_LIST_ITEM.getFhirCode().equals(conditionCategoryCode)) {
			return PROBLEM_LIST_ITEM.getOmopConceptId();
		} 
		if (ENCOUNTER_DIAGNOSIS.getFhirCode().equals(conditionCategoryCode)) {
			return ENCOUNTER_DIAGNOSIS.getOmopConceptId();
		}
		
		return COND_NULL.getOmopConceptId();
	}

	public static Long omopForEncounterClassCode(String encounterClassCode) throws FHIRException {
		if (encounterClassCode == null || encounterClassCode.isEmpty()) {
			throw new FHIRException("Unknow Observation Category code: '"+encounterClassCode+"'");
		}

		if (INPATIENT.getFhirCode().equals(encounterClassCode)) {
			return INPATIENT.getOmopConceptId();
		}
		if (OUTPATIENT.getFhirCode().equals(encounterClassCode)) {
			return OUTPATIENT.getOmopConceptId();
		}
		if (EMERGENCY.getFhirCode().equals(encounterClassCode)) {
			return EMERGENCY.getOmopConceptId();
		}
		
		return ENC_NULL.getOmopConceptId();
	}
	
	public static String fhirForConditionTypeConcept(Long conditionTypeConceptId) {
		if (PROBLEM_LIST_ITEM.getOmopConceptId() == conditionTypeConceptId) {
			return PROBLEM_LIST_ITEM.getFhirCode();
		}
		if (ENCOUNTER_DIAGNOSIS.getOmopConceptId() == conditionTypeConceptId) {
			return ENCOUNTER_DIAGNOSIS.getFhirCode();
		} 
		
		return COND_NULL.getFhirCode();
	}
	
	String fhirCode;
	Long omopConceptId;

	OmopConceptMapping(String fhirCode, Long omopConceptId) {
		this.fhirCode = fhirCode;
		this.omopConceptId = omopConceptId;
	}

	public Long getOmopConceptId() {
		return omopConceptId;
	}

	public void setOmopConceptId(Long omopConceptId) {
		this.omopConceptId = omopConceptId;
	}

	public String getFhirCode() {
		return fhirCode;
	}

	public void setFhirCode(String fhirCode) {
		this.fhirCode = fhirCode;
	}
}
