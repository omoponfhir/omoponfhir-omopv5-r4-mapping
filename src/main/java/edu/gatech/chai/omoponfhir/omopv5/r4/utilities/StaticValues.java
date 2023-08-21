package edu.gatech.chai.omoponfhir.omopv5.r4.utilities;

import ca.uhn.fhir.context.FhirContext;

public class StaticValues {
	public static final FhirContext myFhirContext = FhirContext.forR4();
	public static boolean isSupported(String resourceType) {
		if (
			"Organization".equals(resourceType) ||
			"AllergyIntolerance".equals(resourceType) ||
			"Bundle".equals(resourceType) ||
			"CodeSystem".equals(resourceType) ||
			"ConceptMap".equals(resourceType) ||
			"Condition".equals(resourceType) ||
			"Device".equals(resourceType) ||
			"DeviceUseStatement".equals(resourceType) ||
			"DocumentReference".equals(resourceType) ||
			"Immunization".equals(resourceType) ||
			"Medication".equals(resourceType) ||
			"MedicationRequest".equals(resourceType) ||
			"MedicationStatement".equals(resourceType) ||
			"Observation".equals(resourceType) ||
			"Patient".equals(resourceType) ||
			"Practitioner".equals(resourceType) ||
			"Procedure".equals(resourceType) ||
			"Specimen".equals(resourceType) ||
			"ValueSet".equals(resourceType)
		) {
			return true;
		}
		return false;
	}
	public static boolean isInt(String strInt) {
		if (strInt == null) {
			return false;
		}
		try {
			int d = Integer.parseInt(strInt);
		} catch (NumberFormatException nfe) {
			return false;
		}
		return true;
	}	
}