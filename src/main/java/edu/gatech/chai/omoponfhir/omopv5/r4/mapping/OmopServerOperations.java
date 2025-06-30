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

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.MedicationRequest;
import org.hl7.fhir.r4.model.MedicationStatement;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Procedure;
import org.hl7.fhir.r4.model.Procedure.ProcedureFocalDeviceComponent;
import org.hl7.fhir.r4.model.Procedure.ProcedurePerformerComponent;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.ResourceType;
import org.hl7.fhir.r4.model.Type;
import org.hl7.fhir.r4.model.Attachment;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.Bundle.BundleEntryResponseComponent;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Condition;
import org.hl7.fhir.r4.model.Condition.ConditionEvidenceComponent;
import org.hl7.fhir.r4.model.Condition.ConditionStageComponent;
import org.hl7.fhir.r4.model.DocumentReference;
import org.hl7.fhir.r4.model.DocumentReference.DocumentReferenceContentComponent;
import org.hl7.fhir.r4.model.DocumentReference.DocumentReferenceRelatesToComponent;
import org.hl7.fhir.r4.model.Enumerations.DocumentReferenceStatus;
import org.springframework.web.context.ContextLoaderListener;
import org.springframework.web.context.WebApplicationContext;

import edu.gatech.chai.omoponfhir.omopv5.r4.model.USCorePatient;
import edu.gatech.chai.omoponfhir.omopv5.r4.utilities.ExtensionUtil;

public class OmopServerOperations {
	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(OmopServerOperations.class);

	private static OmopServerOperations omopServerOperations = new OmopServerOperations();

	private Map<String, String> referenceIds;

	public OmopServerOperations(WebApplicationContext context) {
		initialize(context);
	}

	public OmopServerOperations() {
		initialize(ContextLoaderListener.getCurrentWebApplicationContext());
	}

	private void initialize(WebApplicationContext context) {
		referenceIds = new HashMap<String, String>();
	}

	public static OmopServerOperations getInstance() {
		return omopServerOperations;
	}

	private void updateReference(Reference reference) {
		if (reference == null || reference.isEmpty())
			return;

		String originalId = reference.getReferenceElement().getValueAsString();
		String newId = referenceIds.get(originalId);
		if (newId == null || newId.isBlank()) {
			for (Entry<String, String> e : referenceIds.entrySet()) {
				if (e.getKey().endsWith(originalId)) {
					newId = e.getValue();
					break;
				}
			}
		}

		logger.debug("orginal id: " + originalId + " new id:" + newId);
		if (newId != null && !newId.isEmpty()) {
			String[] resourceId = newId.split("/");
			if (resourceId.length == 2) {
				reference.setReferenceElement(new IdType(resourceId[0], resourceId[1]));
			} else {
				reference.setReferenceElement(new IdType(newId));
			}
		} else {
			// delete all contents
			reference.setReference(null);
			reference.setType(null);
			reference.setIdentifier(null);
			reference.setDisplay(null);
			reference.setId(null);
			reference.setExtension(null);
		}
	}

	private void updateReferences(List<Reference> references) {
		for (Reference reference : references) {
			updateReference(reference);
		}
	}

	private BundleEntryComponent addResponseEntry(String status, String location) {
		BundleEntryComponent entryBundle = new BundleEntryComponent();
		UUID uuid = UUID.randomUUID();
		entryBundle.setFullUrl("urn:uuid:" + uuid.toString());
		BundleEntryResponseComponent responseBundle = new BundleEntryResponseComponent();
		responseBundle.setStatus(status);
		if (location != null)
			responseBundle.setLocation(location);
		entryBundle.setResponse(responseBundle);

		return entryBundle;
	}

	public List<BundleEntryComponent> createEntries(List<BundleEntryComponent> entries) throws Exception {
		List<BundleEntryComponent> responseEntries = new ArrayList<BundleEntryComponent>();
		// Map<String, Long> patientMap = new HashMap<String, Long>();

		// do patient first.
		for (BundleEntryComponent entry : entries) {
			Resource resource = entry.getResource();
			if (resource != null && resource.getResourceType() == ResourceType.Patient) {
				Long fhirId;
				BundleEntryComponent newEntry;
				USCorePatient patient = ExtensionUtil.usCorePatientFromResource(resource);

				updateReferences(patient.getGeneralPractitioner());
				
				fhirId = OmopPatient.getInstance().toDbase(patient, null);
				newEntry = addResponseEntry("201 Created", "Patient/" + fhirId);

				patient.setId(new IdType(OmopPatient.FHIRTYPE, fhirId));
				newEntry.setResource(patient);
				responseEntries.add(newEntry);

				referenceIds.put(entry.getFullUrl(), OmopPatient.FHIRTYPE + "/" + fhirId);
				logger.debug("Added patient info to referenceIds " + entry.getFullUrl() + "->" + fhirId);
			}
		}

		// any person related resources such as practitioners, person, etc. here when needed.

		// DocumentReference
		for (BundleEntryComponent entry : entries) {
			Resource resource = entry.getResource();

			if (resource.getResourceType() == ResourceType.DocumentReference) {
				logger.debug("Trying to add document reference: " + entry.getFullUrl());
				DocumentReference documentReference = (DocumentReference) resource;
				updateReference(documentReference.getSubject());
				updateReferences(documentReference.getAuthor());
				updateReference(documentReference.getAuthenticator());
				updateReference(documentReference.getCustodian());

				for (DocumentReferenceRelatesToComponent relatesTo : documentReference.getRelatesTo()) {
					updateReference(relatesTo.getTarget());
				}

				if (!documentReference.getContext().isEmpty()) {
					updateReferences(documentReference.getContext().getEncounter());
					updateReference(documentReference.getContext().getSourcePatientInfo());
					updateReferences(documentReference.getContext().getRelated());
				}

				Long fhirId = OmopDocumentReference.getInstance().toDbase(documentReference, null);
				BundleEntryComponent newEntry;
				if (fhirId == null || fhirId == 0L) {
					newEntry = addResponseEntry("400 Bad Request", null);
					newEntry.setResource(documentReference);
				} else {
					referenceIds.put(entry.getFullUrl(), OmopDocumentReference.FHIRTYPE + "/" + fhirId);
					newEntry = addResponseEntry("201 Created", "DocumentReference/" + fhirId);
				}

				responseEntries.add(newEntry);
				logger.debug("Added document reference info to referenceIds " + entry.getFullUrl() + "->" + fhirId);
			}
		}

		// In the bundle, we need to process medications, conditions, etc first before observation as
		// our observation in the bundle will have observation.focus to those resources. We will first
		// store meds and conds. Then, we will update the focus reference with the new values.
		//
		// Process the MedicationStatement
		for (BundleEntryComponent entry : entries) {
			Resource resource = entry.getResource();
			
			if (resource.getResourceType() == ResourceType.MedicationStatement) {
				logger.debug("Trying to add medication statement: " + entry.getFullUrl());
				MedicationStatement medicationStatement = (MedicationStatement) resource;
				updateReference(medicationStatement.getSubject());

				updateReferences(medicationStatement.getBasedOn());
				updateReferences(medicationStatement.getPartOf());

				// Type medicationType = medicationStatement.getMedication();
				// if (medicationType instanceof Reference) {
				// 	updateReference((Reference) medicationType);
				// }

				updateReference(medicationStatement.getContext());
				updateReference(medicationStatement.getInformationSource());
				updateReferences(medicationStatement.getDerivedFrom());
				updateReferences(medicationStatement.getReasonReference());

				Long fhirId = OmopMedicationStatement.getInstance().toDbase(medicationStatement, null);
				BundleEntryComponent newEntry;
				if (fhirId == null || fhirId == 0L) {
					newEntry = addResponseEntry("400 Bad Request", null);
					newEntry.setResource(medicationStatement);
				} else {
					referenceIds.put(entry.getFullUrl(), OmopMedicationStatement.FHIRTYPE + "/" + fhirId);
					newEntry = addResponseEntry("201 Created", "MedicationStatement/" + fhirId);
				}

				responseEntries.add(newEntry);
				logger.debug("Added medication statement info to referenceIds " + entry.getFullUrl() + "->" + fhirId);
			}
		}

		// Process the MedicationRequest
		for (BundleEntryComponent entry : entries) {
			Resource resource = entry.getResource();
			
			if (resource.getResourceType() == ResourceType.MedicationRequest) {
				logger.debug("Trying to add medication request: " + entry.getFullUrl());
				MedicationRequest medicationRequest = (MedicationRequest) resource;

				updateReference(medicationRequest.getSubject());

				Type reportedType = medicationRequest.getReported();
				if (reportedType instanceof Reference) {
					updateReference((Reference) reportedType);
				}

				// Type medicationType = medicationRequest.getMedication();
				// if (medicationType instanceof Reference) {
				// 	updateReference((Reference) medicationType);
				// }

				updateReference(medicationRequest.getEncounter());
				updateReferences(medicationRequest.getSupportingInformation());
				updateReference(medicationRequest.getRequester());
				updateReference(medicationRequest.getPerformer());
				updateReference(medicationRequest.getRecorder());
				updateReferences(medicationRequest.getReasonReference());
				updateReferences(medicationRequest.getBasedOn());
				updateReferences(medicationRequest.getInsurance());

				if (medicationRequest.getDispenseRequest() != null && !medicationRequest.getDispenseRequest().isEmpty()) {
					updateReference(medicationRequest.getDispenseRequest().getPerformer());
				}

				updateReference((medicationRequest.getPriorPrescription()));
				updateReferences(medicationRequest.getDetectedIssue());
				updateReference(medicationRequest.addEventHistory());

				Long fhirId = OmopMedicationRequest.getInstance().toDbase(medicationRequest, null);
				BundleEntryComponent newEntry;
				if (fhirId == null || fhirId == 0L) {
					newEntry = addResponseEntry("400 Bad Request", null);
					newEntry.setResource(medicationRequest);
				} else {
					referenceIds.put(entry.getFullUrl(), OmopMedicationRequest.FHIRTYPE + "/" + fhirId);
					newEntry = addResponseEntry("201 Created", "MedicationRequest/" + fhirId);
				}

				responseEntries.add(newEntry);
				logger.debug("Added medication request info to referenceIds " + entry.getFullUrl() + "->" + fhirId);
			}			
		}

		// Process Condition
		for (BundleEntryComponent entry : entries) {
			Resource resource = entry.getResource();

			if (resource.getResourceType() == ResourceType.Condition) {
				Condition condition = (Condition) resource;
				updateReference(condition.getSubject());
				updateReference(condition.getEncounter());
				updateReference(condition.getRecorder());
				updateReference(condition.getAsserter());

				for (ConditionStageComponent stage : condition.getStage()) {
					updateReferences(stage.getAssessment());
				}

				for (ConditionEvidenceComponent evidence : condition.getEvidence()) {
					updateReferences(evidence.getDetail());
				}

				Long fhirId = OmopCondition.getInstance().toDbase(condition, null);
				BundleEntryComponent newEntry;
				if (fhirId == null || fhirId == 0L) {
					newEntry = addResponseEntry("400 Bad Request", null);
					newEntry.setResource(condition);
				} else {
					referenceIds.put(entry.getFullUrl(), OmopCondition.FHIRTYPE + "/" + fhirId);
					newEntry = addResponseEntry("201 Created", "Condition/" + fhirId);
				}

				responseEntries.add(newEntry);
			}
		}

		// Process Observation
		for (BundleEntryComponent entry : entries) {
			Resource resource = entry.getResource();
			
			if (resource.getResourceType() == ResourceType.Observation) {
				Observation observation = (Observation) resource;
				// we preocess the one without focus.
				List<Reference> focuses = observation.getFocus();
				if (!focuses.isEmpty()) {
					// We write the observations that do not have focus.
					// this is needed because focus can focus itself.
					continue;
				}

				updateReference(observation.getSubject());
				updateReferences(observation.getBasedOn());
				updateReferences(observation.getPartOf());
				updateReference(observation.getEncounter());
				updateReferences(observation.getPerformer());
				updateReference(observation.getSpecimen());
				updateReference(observation.getDevice());
				updateReferences(observation.getHasMember());
				updateReferences(observation.getDerivedFrom());

				Long fhirId = OmopObservation.getInstance().toDbase(observation, null);
				BundleEntryComponent newEntry;
				if (fhirId == null || fhirId == 0L) {
					newEntry = addResponseEntry("400 Bad Request", null);
					newEntry.setResource(observation);
				} else {
					referenceIds.put(entry.getFullUrl(), OmopObservation.FHIRTYPE + "/" + fhirId);
					newEntry = addResponseEntry("201 Created", "Observation/" + fhirId);
				}
				responseEntries.add(newEntry);
				logger.debug("Added observation(non-focus contained) info to referenceIds " + entry.getFullUrl() + "->" + fhirId);
			} 
		}

		// We need to update self reference on Observation (for focus)
		for (BundleEntryComponent entry : entries) {
			Resource resource = entry.getResource();
			
			if (resource.getResourceType() == ResourceType.Observation) {
				Observation observation = (Observation) resource;
				// we preocess the one without focus.
				List<Reference> focuses = observation.getFocus();
				if (focuses.isEmpty()) {
					// We write the observations that do not have focus.
					// this is needed because focus can focus itself.
					continue;
				}

				updateReference(observation.getSubject());
				
				// For focus, if we are focusing on DocumentReference, we should check
				// if we have the DocumentReference attached or we just have a link to it. 
				// If we don't have it attached, then we need to create one and write the link.
				for (Reference reference : observation.getFocus()) {
					String origReferenceString = reference.getReference();
					updateReference(reference);

					if ("DocumentReference".equals(reference.getReferenceElement().getResourceType()) && reference.getReference().equals(origReferenceString)) {
						// The reference is not updated. This means that we don't have the document reference attached.
						// Create one here.
						DocumentReference linkNote = new DocumentReference();
						linkNote.setSubject(observation.getSubject());
						linkNote.setStatus(DocumentReferenceStatus.CURRENT);
						linkNote.setType(new CodeableConcept(new Coding("http://loinc.org", "34109-9", "Note")));
						linkNote.setDate(new Date());

						Identifier noteIdentifier = new Identifier();
						noteIdentifier.setSystem("urn:gtri:registry_manager");
						noteIdentifier.setValue(origReferenceString);
						linkNote.addIdentifier(noteIdentifier);

						Attachment attachment = new Attachment();
						attachment.setContentType("text/plain");
						attachment.setLanguage("en-US");
						attachment.setData(origReferenceString.getBytes());
						DocumentReferenceContentComponent docComponent = new DocumentReferenceContentComponent(attachment);
						linkNote.addContent(docComponent);

						Long fhirId = OmopDocumentReference.getInstance().toDbase(linkNote, null);
						BundleEntryComponent newEntry;
						if (fhirId == null || fhirId == 0L) {
							newEntry = addResponseEntry("400 Bad Request", null);
						} else {
							reference.setReferenceElement(new IdType(OmopDocumentReference.FHIRTYPE, fhirId));
							newEntry = addResponseEntry("201 Created", "DocumentReference/" + fhirId);
						}
						newEntry.setResource(linkNote);		
						responseEntries.add(newEntry);
					}

				}
				// updateReferences(observation.getFocus());
				updateReferences(observation.getBasedOn());
				updateReferences(observation.getPartOf());
				updateReference(observation.getEncounter());
				updateReferences(observation.getPerformer());
				updateReference(observation.getSpecimen());
				updateReference(observation.getDevice());
				updateReferences(observation.getHasMember());
				updateReferences(observation.getDerivedFrom());

				Long fhirId = OmopObservation.getInstance().toDbase(observation, null);
				BundleEntryComponent newEntry;
				if (fhirId == null || fhirId == 0L) {
					newEntry = addResponseEntry("400 Bad Request", null);
					newEntry.setResource(observation);
				} else {
					referenceIds.put(entry.getFullUrl(), OmopObservation.FHIRTYPE + "/" + fhirId);
					newEntry = addResponseEntry("201 Created", "Observation/" + fhirId);
				}
				responseEntries.add(newEntry);
				logger.debug("Added observation(focus-contained) info to referenceIds " + entry.getFullUrl() + "->" + fhirId);
			} 
		}

		// Process Procedure
		for (BundleEntryComponent entry : entries) {
			Resource resource = entry.getResource();
			
			if (resource.getResourceType() == ResourceType.Procedure) {
				Procedure procedure = (Procedure) resource;

				updateReference(procedure.getSubject());
				updateReferences(procedure.getBasedOn());
				updateReferences(procedure.getPartOf());
				updateReference(procedure.getEncounter());
				updateReference(procedure.getRecorder());
				updateReference(procedure.getAsserter());

				List<ProcedurePerformerComponent> performers = procedure.getPerformer();
				for (ProcedurePerformerComponent performer : performers) {
					updateReference(performer.getActor());
					updateReference(performer.getOnBehalfOf());
				}

				updateReference(procedure.getLocation());
				updateReference(procedure.getLocation());
				updateReferences(procedure.getReasonReference());
				updateReferences(procedure.getReport());
				updateReferences(procedure.getComplicationDetail());

				List<ProcedureFocalDeviceComponent> focalDevices = procedure.getFocalDevice();
				for (ProcedureFocalDeviceComponent focalDevice : focalDevices) {
					updateReference(focalDevice.getManipulated());
				}

				updateReferences(procedure.getUsedReference());

				Long fhirId = OmopProcedure.getInstance().toDbase(procedure, null);
				BundleEntryComponent newEntry;
				if (fhirId == null || fhirId == 0L) {
					newEntry = addResponseEntry("400 Bad Request", null);
					newEntry.setResource(procedure);
				} else {
					referenceIds.put(entry.getFullUrl(), OmopProcedure.FHIRTYPE + "/" + fhirId);
					newEntry = addResponseEntry("201 Created", "Observation/" + fhirId);
				}
				responseEntries.add(newEntry);
				logger.debug("Added procedure info to referenceIds " + entry.getFullUrl() + "->" + fhirId);
			} 
		}

		return responseEntries;
	}
}
