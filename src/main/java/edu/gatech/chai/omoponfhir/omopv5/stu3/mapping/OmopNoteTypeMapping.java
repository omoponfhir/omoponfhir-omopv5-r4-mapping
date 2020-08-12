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
package edu.gatech.chai.omoponfhir.omopv5.stu3.mapping;

public enum OmopNoteTypeMapping {
	DISCHARGE_SUMMARY(44814637L, 3020091L),
	ADMISSION_NOTE(44814638L, 40770449L),
	INPATIENT_NOTE(44814639L, 3029447L),    // Hospital Note
	OUTPATIENT_NOTE(44814640L, 3030624L),
	RADIOLOGY_REPORT(44814641L, 46235057L),
	PATHOLOGY_REPORT(44814642L, 40763624L),
	ANCILLARY_REPORT(44814643L, 42869890L), // Ancillary eye tests Narrative
	NURSING_REPORT(44814644L, 3046947L),
	NOTE(44814645L, 3030653L),
	EMERGENCY_DEPARTMENT_NOTE(44814646L, 3029201L);
	
	
	Long omopOmopTypeConceptId;
	Long omopLoincTypeConceptId;
	
	OmopNoteTypeMapping(Long omopOmopTypeConceptId, Long omopLoincTypeConceptId) {
		this.omopOmopTypeConceptId = omopOmopTypeConceptId;
		this.omopLoincTypeConceptId = omopLoincTypeConceptId;
	}
	
	// Returns OMOP-generated concept ID for the given OMOP-LOINC concept ID
	// The mapping done with a best guess. Review is needed by coding experts
	public static Long getOmopConceptIdFor(Long loincTypeConceptId) {
		for (OmopNoteTypeMapping mapping: OmopNoteTypeMapping.values()) {
			if (loincTypeConceptId.equals(mapping.getOmopLoincTypeConceptId())) {
				return mapping.getOmopOmopTypeConceptId();
			}
		}
		
		return 0L;
	}
	
	// Returns OMOP-LOINC concept ID for the given OMOP-generated concept ID
	// The mapping done with a best guess. Review is needed by coding experts
	public static Long getLoincConceptIdFor(Long omopTypeConceptId) {
		for (OmopNoteTypeMapping mapping: OmopNoteTypeMapping.values()) {
			if (omopTypeConceptId.equals(mapping.getOmopOmopTypeConceptId())) {
				return mapping.getOmopLoincTypeConceptId();
			}
		}
		
		return 0L;
	}

	public Long getOmopOmopTypeConceptId() {
		return omopOmopTypeConceptId;
	}
	
	public void setOmopOmopTypeConceptId(Long omopOmopTypeConceptId) {
		this.omopOmopTypeConceptId = omopOmopTypeConceptId;
	}
	
	public Long getOmopLoincTypeConceptId() {
		return omopLoincTypeConceptId;
	}
	
	public void setOmopLoincTypeConceptId(Long omopLoincTypeConceptId) {
		this.omopLoincTypeConceptId = omopLoincTypeConceptId;
	}
}
