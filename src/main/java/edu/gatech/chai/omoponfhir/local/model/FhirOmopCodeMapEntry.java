package edu.gatech.chai.omoponfhir.local.model;

public class FhirOmopCodeMapEntry {
	private Long omopConcept;
	private String fhirSystem;
	private String fhirCode;
	private String fhirDisplay;
	
	public FhirOmopCodeMapEntry() {}
	
	public Long getOmopConcept() {
		return this.omopConcept;
	}
	
	public void setOmopConcept(Long omopConcept) {
		this.omopConcept = omopConcept;
	}
	
	public String getFhirSystem() {
		return this.fhirSystem;
	}
	
	public void setFhirSystem(String fhirSystem) {
		this.fhirSystem = fhirSystem;
	}
	
	public String getFhirCode() {
		return this.fhirCode;
	}
	
	public void setFhirCode(String fhirCode) {
		this.fhirCode = fhirCode;
	}
	
	public String getFhirDisplay() {
		return this.fhirDisplay;
	}
	
	public void setFhirDisplay(String fhirDisplay) {
		this.fhirDisplay = fhirDisplay;
	}
}
