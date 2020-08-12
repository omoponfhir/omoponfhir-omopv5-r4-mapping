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
package edu.gatech.chai.omoponfhir.omopv5.stu3.utilities;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.dstu3.model.Resource;
import org.hl7.fhir.dstu3.model.ResourceType;
import org.hl7.fhir.dstu3.model.ValueSet;
import org.hl7.fhir.dstu3.model.ValueSet.ConceptSetComponent;
import org.hl7.fhir.dstu3.model.ValueSet.ValueSetComposeComponent;
import org.springframework.web.context.ContextLoaderListener;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;

public class TerminologyServiceClient {
	private static TerminologyServiceClient terminologyServiceClient = new TerminologyServiceClient();
	private static String terminologyServerDefault = "http://measure.eval.kanvix.com/cqf-ruler/baseDstu3";
	private String terminologyServerUrl;
	private IGenericClient client = null;
	
	public static TerminologyServiceClient getInstance() {
		return terminologyServiceClient;
	}

	public String getTerminologyServerUrl() {
		return terminologyServerUrl;
	}
	
	public TerminologyServiceClient() {
		terminologyServerUrl = ContextLoaderListener.getCurrentWebApplicationContext().getServletContext()
				.getInitParameter("terminologyServerUrl");
		if (terminologyServerUrl == null || terminologyServerUrl.isEmpty()) {
			terminologyServerUrl = terminologyServerDefault;
		}
		
		// Set up FHIR client to make ValueSet calls
		FhirContext ctx = FhirContext.forDstu3();
		client = ctx.newRestfulGenericClient(terminologyServerUrl);
	}
	
	public Map<String, List<ConceptSetComponent>> getValueSetByUrl(String url) {
		Map<String, List<ConceptSetComponent>> retVal = new HashMap<String, List<ConceptSetComponent>>();
		Bundle results = client
				.search()
				.forResource(ValueSet.class)
				.where(ValueSet.URL.matches().value(url))
				.returnBundle(Bundle.class)
				.execute();
		
		// Results should contain entries for ValueSet with the specified url.
		// We only search codeset URL. Other supports may be added later..
		for (BundleEntryComponent entry : results.getEntry()) {
			if (url.contains(entry.getId())) {
				// Ok, we found the entry. Now, get compose.
				Resource resource = entry.getResource();
				if (resource.getResourceType().getPath().equals("ValueSet")) {
					ValueSet valueSet = (ValueSet) resource;
					ValueSetComposeComponent compose = valueSet.getCompose();
					if (!compose.isEmpty()) {
						List<ConceptSetComponent> include = compose.getInclude();
						List<ConceptSetComponent> exclude = compose.getExclude();
						
						// Puth these two information in the hash map and return
						retVal.put("include", include);
						retVal.put("exclude", exclude);
					}
				}
			}
		}
		
		return retVal;
	}

}
