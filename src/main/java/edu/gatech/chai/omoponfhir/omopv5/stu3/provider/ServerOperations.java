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
package edu.gatech.chai.omoponfhir.omopv5.stu3.provider;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.hl7.fhir.dstu3.model.BooleanType;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.CodeableConcept;
import org.hl7.fhir.dstu3.model.Coding;
import org.hl7.fhir.dstu3.model.Enumerations.MessageEvent;
import org.hl7.fhir.dstu3.model.MessageHeader;
import org.hl7.fhir.dstu3.model.OperationOutcome;
import org.hl7.fhir.dstu3.model.MessageHeader.MessageHeaderResponseComponent;
import org.hl7.fhir.dstu3.model.MessageHeader.ResponseType;
import org.hl7.fhir.dstu3.model.OperationOutcome.IssueSeverity;
import org.hl7.fhir.dstu3.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.dstu3.model.Bundle.BundleType;
import org.hl7.fhir.dstu3.model.Resource;
import org.hl7.fhir.dstu3.model.ResourceType;
import org.hl7.fhir.dstu3.model.UriType;
import org.hl7.fhir.exceptions.FHIRException;

import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.annotation.OperationParam;
import edu.gatech.chai.omoponfhir.omopv5.stu3.mapping.OmopServerOperations;
import edu.gatech.chai.omoponfhir.omopv5.stu3.utilities.CodeableConceptUtil;
import edu.gatech.chai.omoponfhir.omopv5.stu3.utilities.ThrowFHIRExceptions;

public class ServerOperations {
	private OmopServerOperations myMapper;
	
	public ServerOperations() {
		myMapper = new OmopServerOperations();
	}
	
	@Operation(name="$process-message")
	public Bundle processMessageOperation(
			@OperationParam(name="content") Bundle theContent,
			@OperationParam(name="async") BooleanType theAsync,
			@OperationParam(name="response-url") UriType theUri			
			) {
		Bundle retVal = new Bundle();
		MessageHeader messageHeader = null;
		List<Resource> resources = new ArrayList<Resource>();
		
		if (theContent.getType() == BundleType.MESSAGE) {
			List<BundleEntryComponent> entries = theContent.getEntry();
			// Evaluate the first entry, which must be MessageHeader
//			BundleEntryComponent entry1 = theContent.getEntryFirstRep();
//			Resource resource = entry1.getResource();
			if (entries != null && entries.size() > 0 && 
					entries.get(0).getResource() != null &&
					entries.get(0).getResource().getResourceType() == ResourceType.MessageHeader) {
				messageHeader = (MessageHeader) entries.get(0).getResource();
				// We handle observation-type.
				// TODO: Add other types later.
				Coding event = messageHeader.getEvent();
				Coding obsprovided = new Coding(MessageEvent.OBSERVATIONPROVIDE.getSystem(), MessageEvent.OBSERVATIONPROVIDE.toCode(), MessageEvent.OBSERVATIONPROVIDE.getDefinition());
				if (CodeableConceptUtil.compareCodings(event, obsprovided) == 0) {
					// This is lab report. they are all to be added to the server.
					for (int i=1; i<entries.size(); i++) {
						resources.add(entries.get(i).getResource());
					}
				} else {
					ThrowFHIRExceptions.unprocessableEntityException(
							"We currently support only observation-provided Message event");
				}
			}
		} else {
			ThrowFHIRExceptions.unprocessableEntityException(
					"The bundle must be a MESSAGE type");
		}
		MessageHeaderResponseComponent messageHeaderResponse = new MessageHeaderResponseComponent();
		messageHeaderResponse.setId(messageHeader.getId());

		List<BundleEntryComponent> resultEntries = null;
		try {
			resultEntries = myMapper.createEntries(resources);
			messageHeaderResponse.setCode(ResponseType.OK);
		} catch (FHIRException e) {
			e.printStackTrace();
			messageHeaderResponse.setCode(ResponseType.OK);
			OperationOutcome outcome = new OperationOutcome();
			CodeableConcept detailCode = new CodeableConcept();
			detailCode.setText(e.getMessage());
			outcome.addIssue().setSeverity(IssueSeverity.ERROR).setDetails(detailCode);
			messageHeaderResponse.setDetailsTarget(outcome);
		}
		
		messageHeader.setResponse(messageHeaderResponse);
		BundleEntryComponent responseMessageEntry = new BundleEntryComponent();
		UUID uuid = UUID.randomUUID();
		responseMessageEntry.setFullUrl("urn:uuid:"+uuid.toString());
		responseMessageEntry.setResource(messageHeader);
		
		if (resultEntries == null) resultEntries = new ArrayList<BundleEntryComponent>();
		
		resultEntries.add(0, responseMessageEntry);
		retVal.setEntry(resultEntries);
		
		return retVal;
	}
}
