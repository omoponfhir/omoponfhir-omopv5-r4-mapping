package edu.gatech.chai.omoponfhir.omopv5.r4.utilities;

import java.util.HashMap;
import java.util.Map;

import org.hl7.fhir.r4.model.Resource;

import ca.uhn.fhir.parser.IParser;
import edu.gatech.chai.omoponfhir.omopv5.r4.model.USCorePatient;

public class ExtensionUtil {

	public static Map<String, Long>  resourceCounts = new HashMap<String, Long>();
	
	public static USCorePatient usCorePatientFromResource(Resource resource) {
		IParser p = StaticValues.myFhirContext.newJsonParser();
		String patientJSON = p.encodeResourceToString(resource);

		return p.parseResource(USCorePatient.class, patientJSON);
	}
	
	public static Map<String, Long> getResourceCounts () {
		return ExtensionUtil.resourceCounts;
	}
	
	public static void setResourceCounts (Map<String, Long> resourceCounts) {
		ExtensionUtil.resourceCounts = resourceCounts;
	}
	
	public static void addResourceCount (String resourceName, Long count) {
		Map<String, Long> counts = ExtensionUtil.getResourceCounts();
		
		counts.put(resourceName, count);
	}
	
	public static Long getResourceCount (String resourceName) {
		Map<String, Long> counts = ExtensionUtil.getResourceCounts();
		Long count = counts.get(resourceName);
		
		if (count == null) return 0L;
		
		return count;
	}
}
