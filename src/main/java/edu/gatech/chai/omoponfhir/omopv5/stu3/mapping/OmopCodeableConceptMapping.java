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

import org.hl7.fhir.exceptions.FHIRException;

/***
 * 
 * @author mc142
 *
 * URI information for coding system is obtained from 
 *   https://www.hl7.org/fhir/terminologies-systems.html
 * 
 * 
 */
public enum OmopCodeableConceptMapping {
	ATC("http://www.whocc.no/atc", "ATC"),
	CPT("http://www.ama-assn.org/go/cpt", "CPT4"),
	HCPCS("urn:oid:2.16.840.1.113883.6.14", "HCPCS"),
	ICD9CM("http://hl7.org/fhir/sid/icd-9-cm", "ICD9CM"),
	ICD9PROC("http://hl7.org/fhir/sid/icd-9-proc", "ICD9Proc"),
	ICD10("http://hl7.org/fhir/sid/icd-10", "ICD10"),
	ICD10CM("http://hl7.org/fhir/sid/icd-10-cm", "ICD10CM"),
	ISBT128("urn:oid:2.16.840.1.113883.6.18", "ISBT128"),
	LOINC("http://loinc.org", "LOINC"),
	NDC("http://hl7.org/fhir/sid/ndc", "NDC"),
	NDFRT("http://hl7.org/fhir/ndfrt", "NDFRT"),
	RXNORM("http://www.nlm.nih.gov/research/umls/rxnorm", "RxNorm"),
	SCT("http://snomed.info/sct", "SNOMED"),
	UCUM("http://unitsofmeasure.org", "UCUM");
	
	
	public static String omopVocabularyforFhirUri(String fhirUri) throws FHIRException {
		if (fhirUri == null || fhirUri.isEmpty()) {
			throw new FHIRException("FHIR URI cannot be null or empty: '"+fhirUri+"'");
		}

		if (ATC.getFhirUri().equals(fhirUri)) 
			return ATC.getOmopVocabulary();
		if (CPT.getFhirUri().equals(fhirUri))
			return CPT.getOmopVocabulary();
		if (HCPCS.getFhirUri().equals(fhirUri))
			return HCPCS.getOmopVocabulary();
		if (ICD9CM.getFhirUri().equals(fhirUri))
			return ICD9CM.getOmopVocabulary();
		if (ICD9PROC.getFhirUri().equals(fhirUri))
			return ICD9PROC.getOmopVocabulary();
		if (ICD10.getFhirUri().equals(fhirUri))
			return ICD10.getOmopVocabulary();
		if (ICD10CM.getFhirUri().equals(fhirUri))
			return ICD10CM.getOmopVocabulary();
		if (ISBT128.getFhirUri().equals(fhirUri))
			return ISBT128.getOmopVocabulary();
		if (LOINC.getFhirUri().equals(fhirUri))
			return LOINC.getOmopVocabulary();
		if (NDC.getFhirUri().equals(fhirUri))
			return NDC.getOmopVocabulary();
		if (NDFRT.getFhirUri().equals(fhirUri))
			return NDFRT.getOmopVocabulary();
		if (RXNORM.getFhirUri().equals(fhirUri))
			return RXNORM.getOmopVocabulary();
		if (SCT.getFhirUri().equals(fhirUri))
			return SCT.getOmopVocabulary();
		if (UCUM.getFhirUri().equals(fhirUri))
			return UCUM.getOmopVocabulary();
		
		return "None";
		
	}

	public static String fhirUriforOmopVocabulary(String omopVocabulary) throws FHIRException {
		if (omopVocabulary == null || omopVocabulary.isEmpty()) {
			throw new FHIRException("Omop Vocabulary ID cannot be null or empty: '"+omopVocabulary+"'");
		}

		if (ATC.getOmopVocabulary().equals(omopVocabulary)) 
			return ATC.getFhirUri();
		if (CPT.getOmopVocabulary().equals(omopVocabulary))
			return CPT.getFhirUri();
		if (HCPCS.getOmopVocabulary().equals(omopVocabulary))
			return HCPCS.getFhirUri();
		if (ICD9CM.getOmopVocabulary().equals(omopVocabulary))
			return ICD9CM.getFhirUri();
		if (ICD9PROC.getOmopVocabulary().equals(omopVocabulary))
			return ICD9PROC.getFhirUri();
		if (ICD10.getOmopVocabulary().equals(omopVocabulary))
			return ICD10.getFhirUri();
		if (ICD10CM.getOmopVocabulary().equals(omopVocabulary))
			return ICD10CM.getFhirUri();
		if (LOINC.getOmopVocabulary().equals(omopVocabulary))
			return LOINC.getFhirUri();
		if (ISBT128.getOmopVocabulary().equals(omopVocabulary))
			return ISBT128.getFhirUri();
		if (NDC.getOmopVocabulary().equals(omopVocabulary))
			return NDC.getFhirUri();
		if (NDFRT.getOmopVocabulary().equals(omopVocabulary))
			return NDFRT.getFhirUri();
		if (RXNORM.getOmopVocabulary().equals(omopVocabulary))
			return RXNORM.getFhirUri();
		if (SCT.getOmopVocabulary().equals(omopVocabulary))
			return SCT.getFhirUri();
		if (UCUM.getOmopVocabulary().equals(omopVocabulary))
			return UCUM.getFhirUri();
		
		return "None";
		
	}

	String fhirUri;
	String omopVocabulary;
	
	OmopCodeableConceptMapping(String fhirUri, String omopVocabulary) {
		this.fhirUri = fhirUri;
		this.omopVocabulary = omopVocabulary;
	}
	
	public String getFhirUri() {
		return fhirUri;
	}
	
	public void setFhirUri(String fhirUri) {
		this.fhirUri = fhirUri;
	}
	
	public String getOmopVocabulary() {
		return omopVocabulary;
	}
	
	public void setOmopVocabulary(String omopVocabulary) {
		this.omopVocabulary = omopVocabulary;
	}
}
