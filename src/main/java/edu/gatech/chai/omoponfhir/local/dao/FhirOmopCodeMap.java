package edu.gatech.chai.omoponfhir.local.dao;

import java.sql.Connection;
import java.util.List;

import org.hl7.fhir.dstu3.model.Coding;

import edu.gatech.chai.omoponfhir.local.model.FhirOmopCodeMapEntry;

public interface FhirOmopCodeMap {
	public Connection connect();
	
	public int save(FhirOmopCodeMapEntry codeMapEntry);
	public void update(FhirOmopCodeMapEntry codeMapEntry);
	public void delete(Long omopConcept);
	public List<FhirOmopCodeMapEntry> get();
	public Long getOmopCodeFromFhirCoding(Coding fhirCoding);
	public Coding getFhirCodingFromOmopConcept(Long omopConcept);
	public Coding getFhirCodingFromOmopSourceString(String omopSourceString);
}
