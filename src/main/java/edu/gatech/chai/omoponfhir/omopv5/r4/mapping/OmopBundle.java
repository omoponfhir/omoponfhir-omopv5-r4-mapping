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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hl7.fhir.r4.model.AllergyIntolerance;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CodeSystem;
import org.hl7.fhir.r4.model.Condition;
import org.hl7.fhir.r4.model.Device;
import org.hl7.fhir.r4.model.DeviceUseStatement;
import org.hl7.fhir.r4.model.DocumentReference;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Immunization;
import org.hl7.fhir.r4.model.MedicationRequest;
import org.hl7.fhir.r4.model.MedicationStatement;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.hl7.fhir.r4.model.Organization;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Practitioner;
import org.hl7.fhir.r4.model.Procedure;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.Specimen;
import org.hl7.fhir.r4.model.Type;
import org.hl7.fhir.r4.model.ValueSet;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.Bundle.BundleEntryRequestComponent;
import org.hl7.fhir.r4.model.Bundle.BundleType;
import org.hl7.fhir.r4.model.Bundle.HTTPVerb;
import org.hl7.fhir.r4.model.Condition.ConditionEvidenceComponent;
import org.hl7.fhir.r4.model.Condition.ConditionStageComponent;
import org.hl7.fhir.r4.model.DocumentReference.DocumentReferenceRelatesToComponent;
import org.hl7.fhir.r4.model.Immunization.ImmunizationPerformerComponent;
import org.hl7.fhir.r4.model.Immunization.ImmunizationProtocolAppliedComponent;
import org.hl7.fhir.r4.model.Immunization.ImmunizationReactionComponent;
import org.hl7.fhir.r4.model.OperationOutcome.IssueSeverity;
import org.hl7.fhir.r4.model.OperationOutcome.IssueSeverityEnumFactory;
import org.hl7.fhir.r4.model.OperationOutcome.IssueType;
import org.hl7.fhir.r4.model.OperationOutcome.IssueTypeEnumFactory;
import org.hl7.fhir.r4.model.OperationOutcome.OperationOutcomeIssueComponent;
import org.hl7.fhir.r4.model.Patient.ContactComponent;
import org.hl7.fhir.r4.model.Patient.PatientLinkComponent;
import org.hl7.fhir.r4.model.Practitioner.PractitionerQualificationComponent;
import org.hl7.fhir.r4.model.Procedure.ProcedureFocalDeviceComponent;
import org.hl7.fhir.r4.model.Procedure.ProcedurePerformerComponent;
import org.hl7.fhir.r4.model.Specimen.SpecimenContainerComponent;
import org.hl7.fhir.r4.model.Specimen.SpecimenProcessingComponent;
import org.hl7.fhir.exceptions.FHIRException;
import org.springframework.http.HttpStatus;
import org.springframework.web.context.ContextLoaderListener;
import org.springframework.web.context.WebApplicationContext;

import edu.gatech.chai.omoponfhir.omopv5.r4.model.MyDeviceUseStatement;
import edu.gatech.chai.omoponfhir.omopv5.r4.model.USCorePatient;
import edu.gatech.chai.omoponfhir.omopv5.r4.utilities.StaticValues;
import edu.gatech.chai.omopv5.dba.service.ConceptService;
import edu.gatech.chai.omopv5.dba.service.ParameterWrapper;
import edu.gatech.chai.omopv5.model.entity.BaseEntity;
import edu.gatech.chai.omopv5.model.entity.Concept;

public class OmopBundle extends BaseOmopResource<Bundle, Concept, ConceptService> {

	private static OmopBundle omopBundle = new OmopBundle();
	Map<String, List<BundleEntryComponent>> entryMap = new HashMap<String, List<BundleEntryComponent>>();
	Map<String, String> updatedMap = new HashMap<String, String>();
	List<String> toBeDeleted = new ArrayList<String>();
	List<Resource> toPutBack = new ArrayList<Resource>();

	public OmopBundle(WebApplicationContext context) {
		super(context, Concept.class, ConceptService.class, OmopBundle.FHIRTYPE);
		
		initialize(context);

		// Get count and put it in the counts.
		getSize(true);
	}

	public OmopBundle() {
		super(ContextLoaderListener.getCurrentWebApplicationContext(), Concept.class, ConceptService.class, OmopBundle.FHIRTYPE);
		
		initialize(ContextLoaderListener.getCurrentWebApplicationContext());
	}
	
	private void initialize(WebApplicationContext context) {
	}

	
	public static OmopBundle getInstance() {
		return omopBundle;
	}

	public static String FHIRTYPE = "Bundle";
	

	private void transactionFailed(Bundle theBundle) {
		// Undo previous transactions.
		for (String id : toBeDeleted) {
			deleteFromId(new IdType(id));
		}

		for (Resource resource : toPutBack) {
			postResource(resource, resource.getIdElement(), theBundle);
		}
	}

	private boolean isUrlValid(String[] referenceUrl) {
		if (referenceUrl.length != 2) return false;

		if (referenceUrl[0] == null || referenceUrl[0].isBlank() || referenceUrl[0].startsWith("http")) 
			return false;

		if (referenceUrl[1] == null || referenceUrl[1].isBlank() || !StaticValues.isInt(referenceUrl[1])) 
			return false;

		return true;
	}

	private Resource getResource(IdType fhirId) {
		String resourceType = fhirId.getResourceType();

		if ("Organization".equals(resourceType)) {
			return OmopOrganization.getInstance().toFHIR(fhirId);
		}

		if ("AllergyIntolerance".equals(resourceType)) {
			return OmopAllergyIntolerance.getInstance().toFHIR(fhirId);
		} 

		if ("Bundle".equals(resourceType)) {
			// we are not supporting Bundle read.
			return null;
		}

		if ("CodeSystem".equals(resourceType)) {
			return OmopCodeSystem.getInstance().toFHIR(fhirId);
		}

		if ("ConceptMap".equals(resourceType)) {
			return null;
		}

		if ("Condition".equals(resourceType)) {
			return OmopCondition.getInstance().toFHIR(fhirId);
		}

		if ("Device".equals(resourceType)) {
			return OmopDevice.getInstance().toFHIR(fhirId);
		}

		if ("DeviceUseStatement".equals(resourceType)) {
			return OmopDeviceUseStatement.getInstance().toFHIR(fhirId);
		}

		if ("DocumentReference".equals(resourceType)) {
			return OmopDocumentReference.getInstance().toFHIR(fhirId);
		}

		if ("Immunization".equals(resourceType)) {
			return OmopImmunization.getInstance().toFHIR(fhirId);
		}

		if ("Medication".equals(resourceType)) {
			return OmopMedication.getInstance().toFHIR(fhirId);
		}

		if ("MedicationRequest".equals(resourceType)) {
			return OmopMedicationRequest.getInstance().toFHIR(fhirId);
		}

		if ("MedicationStatement".equals(resourceType)) {
			return OmopMedicationStatement.getInstance().toFHIR(fhirId);
		}

		if ("Observation".equals(resourceType)) {
			return OmopObservation.getInstance().toFHIR(fhirId);
		}

		if ("Patient".equals(resourceType)) {
			return OmopPatient.getInstance().toFHIR(fhirId);
		}

		if ("Practitioner".equals(resourceType)) {
			return OmopPractitioner.getInstance().toFHIR(fhirId);
		}
		
		if ("Procedure".equals(resourceType)) {
			return OmopProcedure.getInstance().toFHIR(fhirId);
		}

		if ("Specimen".equals(resourceType)) {
			return OmopSpecimen.getInstance().toFHIR(fhirId);
		}

		if ("ValueSet".equals(resourceType)) {
			return OmopValueSet.getInstance().toFHIR(fhirId);
		}

		return null;
	}

	private Long deleteFromId(IdType fhirId) {
		String resourceType = fhirId.getResourceType();

		if ("Organization".equals(resourceType)) {
			return OmopOrganization.getInstance().removeByFhirId(fhirId);
		}

		if ("AllergyIntolerance".equals(resourceType)) {
			return OmopAllergyIntolerance.getInstance().removeByFhirId(fhirId);
		} 

		if ("CodeSystem".equals(resourceType)) {
			return OmopCodeSystem.getInstance().removeByFhirId(fhirId);
		}

		if ("Condition".equals(resourceType)) {
			return OmopCondition.getInstance().removeByFhirId(fhirId);
		}

		if ("Device".equals(resourceType)) {
			return OmopDevice.getInstance().removeByFhirId(fhirId);
		}

		if ("DeviceUseStatement".equals(resourceType)) {
			return OmopDeviceUseStatement.getInstance().removeByFhirId(fhirId);
		}

		if ("DocumentReference".equals(resourceType)) {
			return OmopDocumentReference.getInstance().removeByFhirId(fhirId);
		}

		if ("Immunization".equals(resourceType)) {
			return OmopImmunization.getInstance().removeByFhirId(fhirId);
		}

		if ("Medication".equals(resourceType)) {
			return OmopMedication.getInstance().removeByFhirId(fhirId);
		}

		if ("MedicationRequest".equals(resourceType)) {
			return OmopMedicationRequest.getInstance().removeByFhirId(fhirId);
		}

		if ("MedicationStatement".equals(resourceType)) {
			return OmopMedicationStatement.getInstance().removeByFhirId(fhirId);
		}

		if ("Observation".equals(resourceType)) {
			return OmopObservation.getInstance().removeByFhirId(fhirId);
		}

		if ("Patient".equals(resourceType)) {
			return OmopPatient.getInstance().removeByFhirId(fhirId);
		}

		if ("Practitioner".equals(resourceType)) {
			return OmopPractitioner.getInstance().removeByFhirId(fhirId);
		}
		
		if ("Procedure".equals(resourceType)) {
			return OmopProcedure.getInstance().removeByFhirId(fhirId);
		}

		if ("Specimen".equals(resourceType)) {
			return OmopSpecimen.getInstance().removeByFhirId(fhirId);
		}

		if ("ValueSet".equals(resourceType)) {
			return OmopValueSet.getInstance().removeByFhirId(fhirId);
		}
		
		return null;
	}

	private int updateReferences(List<Reference> references, Bundle theBundle) {
		if (references == null || references.isEmpty()) return 0;

		for (Reference reference : references) {
			if (updateReference(reference, theBundle) < 0) return -1;
		}

		return 0;
	}

	private int updateReference(Reference reference, Bundle theBundle) {
		if (reference == null || reference.isEmpty()) return 0;

		int ret = 0;

		if (updatedMap.get(reference.getId()) != null) {
			reference.setId(updatedMap.get(reference.getId()));
		} else {
			// Walk through the entry
			for (BundleEntryComponent entry : theBundle.getEntry()) {
				if (entry.getId().contains(reference.getId())) {
					HTTPVerb method = HTTPVerb.POST;
					if (entry.hasRequest()) {
						if (entry.getRequest().getMethod() != null) {
							method = entry.getRequest().getMethod();
						}
					}

					if (HTTPVerb.POST == method) {
						processPost(null, theBundle);
					} else if (HTTPVerb.PUT == method) {
						processPut(entry, theBundle);
					} else if (HTTPVerb.GET == method) {
						processGet(entry, theBundle);
					} else if (HTTPVerb.DELETE == method) {
						errorOnTransactionBatch(theBundle, entry, HttpStatus.BAD_REQUEST.toString(), "Trying to delete that is referenced by another resource.");
						return -1;
					} else {
						processPost(entry, theBundle);
					}

					if (entry.getResponse().getStatus().contains("201") || entry.getResponse().getStatus().contains("200")) {
						reference.setId(entry.getResource().getId());
					} else {
						ret = -1;
					}

				}
			}
		}

		return ret;
	}

	private Long postResource(Resource resource, IdType fhirId, Bundle theBundle) {
		String resourceType = fhirId.getResourceType();
		Long id = null;
		// Make sure to visit all the reference fields for each resource
		if (OmopOrganization.FHIRTYPE.equals(resourceType)) {
			Organization organization = (Organization) resource;
			id = OmopOrganization.getInstance().toDbase(organization, fhirId);

			organization.setId(new IdType(OmopOrganization.FHIRTYPE, id));

			if (updateReference(organization.getPartOf(), theBundle) < 0) {
				return null;
			}

			if (updateReferences(organization.getEndpoint(), theBundle) < 0) {
				return null;
			}
		} else if (OmopAllergyIntolerance.FHIRTYPE.equals(resourceType)) {
			AllergyIntolerance allergyIntolerance = (AllergyIntolerance) resource;
			id = OmopAllergyIntolerance.getInstance().toDbase(allergyIntolerance, fhirId);

			allergyIntolerance.setId(new IdType(OmopAllergyIntolerance.FHIRTYPE, id));

			if (updateReference(allergyIntolerance.getPatient(), theBundle) < 0){
				return null;
			}

			if (updateReference(allergyIntolerance.getEncounter(), theBundle) < 0) {
				return null;
			}

			if (updateReference(allergyIntolerance.getRecorder(), theBundle) < 0) {
				return null;
			}

			if (updateReference(allergyIntolerance.getAsserter(), theBundle) < 0) {
				return null;
			}
		} else if (OmopCodeSystem.FHIRTYPE.equals(resourceType)) {
			CodeSystem codeSystem = (CodeSystem) resource;
			id = OmopCodeSystem.getInstance().toDbase(codeSystem, fhirId);

			codeSystem.setId(new IdType(OmopCodeSystem.FHIRTYPE, id));
		} else if (OmopCondition.FHIRTYPE.equals(resourceType)) {
			Condition condition = (Condition) resource;
			id = OmopCondition.getInstance().toDbase(condition, fhirId);

			condition.setId(new IdType(OmopCondition.FHIRTYPE, id));

			if (updateReference(condition.getSubject(), theBundle) < 0){
				return null;
			}

			if (updateReference(condition.getEncounter(), theBundle) < 0){
				return null;
			}

			if (updateReference(condition.getRecorder(), theBundle) < 0){
				return null;
			}

			if (updateReference(condition.getAsserter(), theBundle) < 0){
				return null;
			}

			for (ConditionStageComponent stage : condition.getStage()) {
				if (updateReferences(stage.getAssessment(), theBundle) < 0) {
					return null;
				}
			}

			for (ConditionEvidenceComponent evidence : condition.getEvidence()) {
				if (updateReferences(evidence.getDetail(), theBundle) < 0) {
					return null;
				}
			}

		} else if (OmopDevice.FHIRTYPE.equals(resourceType)) {
			Device device = (Device) resource;
			id = OmopDevice.getInstance().toDbase(device, fhirId);

			device.setId(new IdType(OmopDevice.FHIRTYPE, id));

			if (updateReference(device.getParent(), theBundle) < 0) {
				return null;
			}

			if (updateReference(device.getOwner(), theBundle) < 0) {
				return null;
			}

			if (updateReference(device.getLocation(), theBundle) < 0) {
				return null;
			}

			if (updateReference(device.getParent(), theBundle) < 0) {
				return null;
			}
		} else if (OmopDeviceUseStatement.FHIRTYPE.equals(resourceType)) {
			MyDeviceUseStatement deviceUseStatement = (MyDeviceUseStatement) resource;
			id = OmopDeviceUseStatement.getInstance().toDbase(deviceUseStatement, fhirId);

			deviceUseStatement.setId(new IdType(OmopDeviceUseStatement.FHIRTYPE, id));

			if (updateReferences(deviceUseStatement.getBasedOn(), theBundle) < 0) {
				return null;
			}

			if (updateReference(deviceUseStatement.getSubject(), theBundle) < 0) {
				return null;
			}

			if (updateReferences(deviceUseStatement.getDerivedFrom(), theBundle) < 0) {
				return null;
			}

			if (updateReference(deviceUseStatement.getSource(), theBundle) < 0) {
				return null;
			}

			if (updateReference(deviceUseStatement.getDevice(), theBundle) < 0) {
				return null;
			}

			if (updateReferences(deviceUseStatement.getReasonReference(), theBundle) < 0) {
				return null;
			}
		} else if (OmopDocumentReference.FHIRTYPE.equals(resourceType)) {
			DocumentReference documentReference = (DocumentReference) resource;
			id = OmopDocumentReference.getInstance().toDbase(documentReference, fhirId);

			documentReference.setId(new IdType(OmopDocumentReference.FHIRTYPE, id));

			if (updateReference(documentReference.getSubject(), theBundle) < 0) {
				return null;
			}

			if (updateReferences(documentReference.getAuthor(), theBundle) < 0) {
				return null;
			}

			if (updateReference(documentReference.getAuthenticator(), theBundle) < 0) {
				return null;
			}

			if (updateReference(documentReference.getCustodian(), theBundle) < 0) {
				return null;
			}

			for (DocumentReferenceRelatesToComponent relatedTo : documentReference.getRelatesTo()) {
				if (updateReference(relatedTo.getTarget(), theBundle) < 0) {
					return null;
				}
			}

			if (!documentReference.getContext().isEmpty()) {
				for (Reference encounter : documentReference.getContext().getEncounter()) {
					if (updateReference(encounter, theBundle) < 0) {
						return null;
					}
				}

				if (updateReference(documentReference.getContext().getSourcePatientInfo(), theBundle) < 0) {
					return null;
				}

				if (updateReferences(documentReference.getContext().getRelated(), theBundle) < 0) {
					return null;
				}
			}
		} else if (OmopImmunization.FHIRTYPE.equals(resourceType)) {
			Immunization immunization = (Immunization) resource;
			id = OmopImmunization.getInstance().toDbase(immunization, fhirId);

			immunization.setId(new IdType(OmopImmunization.FHIRTYPE, id));

			if (updateReference(immunization.getPatient(), theBundle) < 0) {
				return null;
			}

			if (updateReference(immunization.getEncounter(), theBundle) < 0) {
				return null;
			}


			if (updateReference(immunization.getLocation(), theBundle) < 0) {
				return null;
			}

			if (updateReference(immunization.getManufacturer(), theBundle) < 0) {
				return null;
			}

			for (ImmunizationPerformerComponent performer : immunization.getPerformer()) {
				if (updateReference(performer.getActor(), theBundle) < 0) {
					return null;
				}
			}

			if (updateReferences(immunization.getReasonReference(), theBundle) < 0) {
				return null;
			}

			for (ImmunizationReactionComponent reaction : immunization.getReaction()) {
				if (updateReference(reaction.getDetail(), theBundle) < 0) {
					return null;
				}
			}

			for (ImmunizationProtocolAppliedComponent protocolApplied : immunization.getProtocolApplied()) {
				if (updateReference(protocolApplied.getAuthority(), theBundle) < 0) {
					return null;
				}	
			}
 		// } else if ("Medication".equals(resourceType)) {
		// 	return null;
		} else if (OmopMedicationRequest.FHIRTYPE.equals(resourceType)) {
			MedicationRequest medicationRequest = (MedicationRequest) resource;
			id = OmopMedicationRequest.getInstance().toDbase(medicationRequest, fhirId);

			medicationRequest.setId(new IdType(OmopMedicationRequest.FHIRTYPE, id));

			Type reported = medicationRequest.getReported();
			if (reported instanceof Reference) {
				if (updateReference((Reference) reported, theBundle) < 0) {
					return null;
				}
			}

			Type medication = medicationRequest.getMedication();
			if (medication instanceof Reference) {
				if (updateReference((Reference) medication, theBundle) < 0) {
					return null;
				}
			}

			if (updateReference(medicationRequest.getSubject(), theBundle) < 0) {
				return null;
			}

			if (updateReference(medicationRequest.getEncounter(), theBundle) < 0) {
				return null;
			}

			if (updateReferences(medicationRequest.getSupportingInformation(), theBundle) < 0) {
				return null;
			}

			if (updateReference(medicationRequest.getRequester(), theBundle) < 0) {
				return null;
			}

			if (updateReference(medicationRequest.getPerformer(), theBundle) < 0) {
				return null;
			}

			if (updateReference(medicationRequest.getRecorder(), theBundle) < 0) {
				return null;
			}

			if (updateReferences(medicationRequest.getReasonReference(), theBundle) < 0) {
				return null;
			}

			if (updateReferences(medicationRequest.getBasedOn(), theBundle) < 0) {
				return null;
			}

			if (updateReferences(medicationRequest.getInsurance(), theBundle) < 0) {
				return null;
			}

			if (!medicationRequest.getDispenseRequest().isEmpty()) {
				if (updateReference(medicationRequest.getDispenseRequest().getPerformer(), theBundle) < 0) {
					return null;
				}
			}

			if (updateReference(medicationRequest.getPriorPrescription(), theBundle) < 0) {
				return null;
			}

			if (updateReferences(medicationRequest.getDetectedIssue(), theBundle) < 0) {
				return null;
			}

			if (updateReferences(medicationRequest.getEventHistory(), theBundle) < 0) {
				return null;
			}
		} else if (OmopMedicationStatement.FHIRTYPE.equals(resourceType)) {
			MedicationStatement medicationStatement = (MedicationStatement) resource;
			id = OmopMedicationStatement.getInstance().toDbase(medicationStatement, fhirId);

			medicationStatement.setId(new IdType(OmopMedicationStatement.FHIRTYPE, id));

			if (updateReferences(medicationStatement.getBasedOn(), theBundle) < 0) {
				return null;
			}

			if (updateReferences(medicationStatement.getPartOf(), theBundle) < 0) {
				return null;
			}

			Type medication = medicationStatement.getMedication();
			if (medication instanceof Reference) {
				if (updateReference((Reference) medication, theBundle) < 0) {
					return null;
				}
			}

			if (updateReference(medicationStatement.getSubject(), theBundle) < 0) {
				return null;
			}

			if (updateReference(medicationStatement.getContext(), theBundle) < 0) {
				return null;
			}

			if (updateReference(medicationStatement.getInformationSource(), theBundle) < 0) {
				return null;
			}

			if (updateReferences(medicationStatement.getDerivedFrom(), theBundle) < 0) {
				return null;
			}

			if (updateReferences(medicationStatement.getReasonReference(), theBundle) < 0) {
				return null;
			}
		} else if (OmopObservation.FHIRTYPE.equals(resourceType)) {
			Observation observation = (Observation) resource;
			id = OmopObservation.getInstance().toDbase(observation, fhirId);

			observation.setId(new IdType(OmopObservation.FHIRTYPE, id));

			if (updateReferences(observation.getBasedOn(), theBundle) < 0) {
				return null;
			}

			if (updateReferences(observation.getPartOf(), theBundle) < 0) {
				return null;
			}

			if (updateReference(observation.getSubject(), theBundle) < 0) {
				return null;
			}

			if (updateReferences(observation.getFocus(), theBundle) < 0) {
				return null;
			}

			if (updateReference(observation.getEncounter(), theBundle) < 0) {
				return null;
			}
			
			if (updateReferences(observation.getPerformer(), theBundle) < 0) {
				return null;
			}

			if (updateReference(observation.getSpecimen(), theBundle) < 0) {
				return null;
			}
			
			if (updateReference(observation.getDevice(), theBundle) < 0) {
				return null;
			}
			
			if (updateReferences(observation.getHasMember(), theBundle) < 0) {
				return null;
			}

			if (updateReferences(observation.getDerivedFrom(), theBundle) < 0) {
				return null;
			}

		} else if (OmopPatient.FHIRTYPE.equals(resourceType)) {
			USCorePatient patient = (USCorePatient) resource;
			id = OmopPatient.getInstance().toDbase(patient, fhirId);

			patient.setId(new IdType(OmopPatient.FHIRTYPE, id));

			for (ContactComponent contact : patient.getContact()) {
				if (updateReference(contact.getOrganization(), theBundle) < 0) {
					return null;
				}
			}
			
			if (updateReferences(patient.getGeneralPractitioner(), theBundle) < 0) {
				return null;
			}
				
			if (updateReference(patient.getManagingOrganization(), theBundle) < 0) {
				return null;
			}
				
			for (PatientLinkComponent link : patient.getLink()) {
				if (updateReference(link.getOther(), theBundle) < 0) {
					return null;
				}
			}
		} else if (OmopPractitioner.FHIRTYPE.equals(resourceType)) {
			Practitioner practitioner = (Practitioner) resource;
			id = OmopPractitioner.getInstance().toDbase(practitioner, fhirId);

			practitioner.setId(new IdType(OmopPractitioner.FHIRTYPE, id));

			for (PractitionerQualificationComponent qualification : practitioner.getQualification()) {
				if (updateReference(qualification.getIssuer(), theBundle) < 0) {
					return null;
				}
			}
		} else if (OmopProcedure.FHIRTYPE.equals(resourceType)) {
			Procedure procedure = (Procedure) resource;
			id = OmopProcedure.getInstance().toDbase(procedure, fhirId);

			procedure.setId(new IdType(OmopProcedure.FHIRTYPE, id));

			if (updateReferences(procedure.getBasedOn(), theBundle) < 0) {
				return null;
			}

			if (updateReferences(procedure.getPartOf(), theBundle) < 0) {
				return null;
			}

			if (updateReference(procedure.getSubject(), theBundle) < 0) {
				return null;
			}

			if (updateReference(procedure.getEncounter(), theBundle) < 0) {
				return null;
			}

			if (updateReference(procedure.getRecorder(), theBundle) < 0) {
				return null;
			}

			if (updateReference(procedure.getAsserter(), theBundle) < 0) {
				return null;
			}

			for (ProcedurePerformerComponent performer : procedure.getPerformer()) {
				if (updateReference(performer.getActor(), theBundle) < 0) {
					return null;
				}
				if (updateReference(performer.getOnBehalfOf(), theBundle) < 0) {
					return null;
				}
			}

			if (updateReference(procedure.getLocation(), theBundle) < 0) {
				return null;
			}


			if (updateReferences(procedure.getReasonReference(), theBundle) < 0) {
				return null;
			}			

			if (updateReferences(procedure.getReport(), theBundle) < 0) {
				return null;
			}			

			if (updateReferences(procedure.getComplicationDetail(), theBundle) < 0) {
				return null;
			}			

			for (ProcedureFocalDeviceComponent focalDevice : procedure.getFocalDevice()) {
				if (updateReference(focalDevice.getManipulated(), theBundle) < 0) {
					return null;
				}
			}
			
			if (updateReferences(procedure.getUsedReference(), theBundle) < 0) {
				return null;
			}			
		} else if (OmopSpecimen.FHIRTYPE.equals(resourceType)) {
			Specimen specimen = (Specimen) resource;
			id = OmopSpecimen.getInstance().toDbase(specimen, fhirId);

			specimen.setId(new IdType(OmopSpecimen.FHIRTYPE, id));

			if (updateReference(specimen.getSubject(), theBundle) < 0) {
				return null;
			}

			if (updateReferences(specimen.getParent(), theBundle) < 0) {
				return null;
			}


			if (updateReferences(specimen.getRequest(), theBundle) < 0) {
				return null;
			}

			if (!specimen.getCollection().isEmpty()) {
				if (updateReference(specimen.getCollection().getCollector(), theBundle) < 0) {
					return null;
				}
			}

			for (SpecimenProcessingComponent processing : specimen.getProcessing()) {
				if (updateReferences(processing.getAdditive(), theBundle) < 0) {
					return null;
				}
			}

			for (SpecimenContainerComponent container : specimen.getContainer()) {
				Type additive = container.getAdditive();
				if (additive instanceof Reference) {
					if (updateReference((Reference)additive, theBundle) < 0) {
						return null;
					}
				}
			}

		} else if (OmopValueSet.FHIRTYPE.equals(resourceType)) {
			ValueSet valueSet = (ValueSet) resource;
			id = OmopValueSet.getInstance().toDbase(valueSet, fhirId);
			
			valueSet.setId(new IdType(OmopValueSet.FHIRTYPE, id));
		}
		
		return id;
	}

	private void errorOnTransactionBatch(Bundle theBundle, BundleEntryComponent entry, String status, String message) {
		if (BundleType.TRANSACTION == theBundle.getType()) {
			// Get this resource and save it for the restoration.
			transactionFailed(theBundle);

			throw new FHIRException(message);
		} else {
			entry.getResponse().setStatus(status);
			OperationOutcome oo = new OperationOutcome();
			OperationOutcomeIssueComponent ooic = new OperationOutcomeIssueComponent();
			ooic.setSeverity(IssueSeverity.ERROR);
			ooic.setCode(IssueType.VALUE);
			ooic.setDiagnostics(message);
			oo.getIssue().add(ooic);

			entry.getResponse().setOutcome(oo);
		}								
	}

	private void processDeletes(List<BundleEntryComponent> entries, Bundle theBundle) {
		for (BundleEntryComponent entry : entries) {
			processDelete(entry, theBundle);
		}
	}

	private void processDelete(BundleEntryComponent entry, Bundle theBundle) {
		if (entry.hasRequest()) {
			String deleteUrl = entry.getRequest().getUrl();
			if (deleteUrl != null && !deleteUrl.isBlank()) {
				String[] deleteResourceTypeId = deleteUrl.split(deleteUrl, 2);
				if (isUrlValid(deleteResourceTypeId)) {
					IdType idType = new IdType(deleteUrl);
					Resource existingResource = getResource(idType);
					if (existingResource != null) {
						toPutBack.add(existingResource);

						Long retId = deleteFromId(idType);
						if (retId == null || retId == 0L) {
							errorOnTransactionBatch(theBundle, entry, HttpStatus.INTERNAL_SERVER_ERROR.toString(), "Failed to delete " + deleteUrl);
						}
					} else {
						errorOnTransactionBatch(theBundle, entry, HttpStatus.NOT_FOUND.toString(), "Resource " + deleteUrl + " does not exist");
					}					
				} else {
					errorOnTransactionBatch(theBundle, entry, HttpStatus.BAD_REQUEST.toString(), "This entry has invalid FHIR ID (" + deleteUrl+ "). In OMOPonFHIR, the resource.id must be Long");
				}
			}
		} else if (entry.hasResource()) {
			Resource deleteResource = entry.getResource();
			String ids = deleteResource.getIdPart();
			if (!StaticValues.isInt(ids)) {
				errorOnTransactionBatch(theBundle, entry, HttpStatus.BAD_REQUEST.toString(), "For " + deleteResource.getIdElement().asStringValue() + ", Resource ID must be long");
			} else {
				Resource existingResource = getResource(deleteResource.getIdElement());
				if (existingResource != null) {
					Long retId = deleteFromId(deleteResource.getIdElement());
					if (retId == null || retId == 0L) {
						errorOnTransactionBatch(theBundle, entry, HttpStatus.BAD_REQUEST.toString(), "Resource (" + deleteResource.getIdElement().asStringValue() + ") delete failed");
					} else {
						toPutBack.add(existingResource);
						entry.getResponse().setStatus(HttpStatus.OK.toString());
					}
				} else {
					errorOnTransactionBatch(theBundle, entry, HttpStatus.BAD_REQUEST.toString(), "Resource (" + deleteResource.getIdElement().asStringValue() + ") does not exist. In OMOPonFHIR, the resource.id must be Long");
				}					
			}
		}			
	}

	private void processPosts(List<BundleEntryComponent> entries, Bundle theBundle) {
		for (BundleEntryComponent entry : entries) {
			processPost(entry, theBundle);
		}
	}

	private void processPost(BundleEntryComponent entry, Bundle theBundle) {
		if (entry.hasResource()) {
			Resource postResource = entry.getResource();
			Long idc = postResource(postResource, null, theBundle);
			if (idc != null) {
				toBeDeleted.add(postResource.fhirType() + "/" + idc);
				entry.getResponse().setStatus(HttpStatus.CREATED.toString());
				entry.getResponse().setLocation(postResource.fhirType() + "/" + idc);
			} else {
				errorOnTransactionBatch(theBundle, entry, HttpStatus.INTERNAL_SERVER_ERROR.toString(), "Resource (" + postResource.getIdElement().asStringValue() + ") post failed");
			}
		} else {
			errorOnTransactionBatch(theBundle, entry, HttpStatus.BAD_REQUEST.toString(), "Bundle has no resource, which is needed for POST");
		}
	}

	private void processPuts(List<BundleEntryComponent> entries, Bundle theBundle) {
		for (BundleEntryComponent entry : entries) {
			processPut(entry, theBundle);
		}
	}

	private void processPut(BundleEntryComponent entry, Bundle theBundle) {
		if (entry.hasResource()) {
			Resource putResource = entry.getResource();
			IdType putResourceFhirId = putResource.getIdElement();
			if (putResourceFhirId == null || putResourceFhirId.isEmpty()) {
				if (entry.hasRequest()) {
					String url = entry.getRequest().getUrl();
					if (url != null && !url.isBlank()) {
						putResourceFhirId = new IdType(url);
					}
				}
			}

			if (putResourceFhirId == null || putResourceFhirId.isEmpty()) {
				errorOnTransactionBatch(theBundle, entry, HttpStatus.BAD_REQUEST.toString(), "Resource ID is needed for PUT");
			} else {
				Resource existingResource = getResource(putResourceFhirId);
				if (existingResource == null || existingResource.isEmpty()) {
					errorOnTransactionBatch(theBundle, entry, HttpStatus.NOT_FOUND.toString(), "Resource for update does not exist");
				} else {
					Long idc = postResource(putResource, putResourceFhirId, theBundle);
					if (idc != null && idc != 0L) {
						toPutBack.add(existingResource);
						entry.getResponse().setStatus(HttpStatus.OK.toString());
					} else {
						errorOnTransactionBatch(theBundle, entry, HttpStatus.INTERNAL_SERVER_ERROR.toString(), "Resource (" + putResource.getIdElement().asStringValue() + ") put failed");
					}
				}
			}
		} else {
			errorOnTransactionBatch(theBundle, entry, HttpStatus.BAD_REQUEST.toString(), "Bundle has no resource, which is needed for PUT");
		}
	}

	private void processGets(List<BundleEntryComponent> entries, Bundle theBundle) {
		for (BundleEntryComponent entry : entries) {
			processGet(entry, theBundle);
		}
	}

	private void processGet(BundleEntryComponent entry, Bundle theBundle) {
		IdType fhirIdType = null;
		if (entry.hasRequest()) {
			String urlString = entry.getRequest().getUrl();
			if (urlString != null && !urlString.isBlank()) {
				fhirIdType = new IdType(urlString);
			}
		}

		if (fhirIdType == null) {
			// get it from resource.
			if (entry.hasResource()) {
				fhirIdType = entry.getResource().getIdElement();
			}
		}

		if (fhirIdType == null || fhirIdType.isEmpty()) {
			errorOnTransactionBatch(theBundle, entry, HttpStatus.BAD_REQUEST.toString(), "Request has no FHIR ID to GET");
		} else {
			Resource resource = getResource(fhirIdType);
			if (resource != null && !resource.isEmpty()) {
				entry.getResponse().setStatus(HttpStatus.OK.toString());
				entry.setResource(resource);
			} else {
				errorOnTransactionBatch(theBundle, entry, HttpStatus.INTERNAL_SERVER_ERROR.toString(), "Failed to GET Resource (" + fhirIdType.asStringValue() + ")");
			}
		}
	}

	@Override
	public Long toDbase(Bundle theBundle, IdType fhirId) throws FHIRException {
		Long retVal = null;

		List<BundleEntryComponent> deleteList = new ArrayList<BundleEntryComponent>();
		List<BundleEntryComponent> postList = new ArrayList<BundleEntryComponent>();
		List<BundleEntryComponent> putList = new ArrayList<BundleEntryComponent>();
		List<BundleEntryComponent> getList = new ArrayList<BundleEntryComponent>();

		if (theBundle.getType() == null) {
			throw new FHIRException("The bundle is required to have type.");
		}

		// mark unsupported resourceTypes
		for (BundleEntryComponent entry : theBundle.getEntry()) {
			if (entry.hasResource() && !StaticValues.isSupported(entry.getResource().fhirType())) {
				entry.getResponse().setStatus(HttpStatus.NOT_IMPLEMENTED.toString());
				continue;
			}

			if (entry.hasResponse()) {
				// we have this already processed this
				continue;
			}
			
			HTTPVerb myMethod = HTTPVerb.POST;
			if (entry.hasRequest()) {
				if (entry.getRequest().hasMethod()) {
					myMethod = entry.getRequest().getMethod();
				}
			}

			if (myMethod == HTTPVerb.DELETE) {
				deleteList.add(entry);
			} else if (myMethod == HTTPVerb.POST) {
				postList.add(entry);
			} else if (myMethod == HTTPVerb.PUT) {
				putList.add(entry);
			} else if (myMethod == HTTPVerb.GET) {
				getList.add(entry);
			} else {
				postList.add(entry);
			}
		}

		// In Bundle transaction/batch, we process the entry requests in the following order.
		// DELETE, POST, PUT, and GET
		processDeletes(deleteList, theBundle);
		processPosts(deleteList, theBundle);
		processPuts(deleteList, theBundle);
		processGets(deleteList, theBundle);

		// Do POST now. 
		for (BundleEntryComponent entry : postList) {
			if (entry.hasResource()) {
				Resource postResource = entry.getResource();
				Long idc = postResource(postResource, null, theBundle);
				if (idc != null) {
					toBeDeleted.add(postResource.fhirType() + "/" + idc);
					entry.getResponse().setStatus(HttpStatus.CREATED.toString());
					entry.getResponse().setLocation(postResource.fhirType() + "/" + idc);
				} else {
					errorOnTransactionBatch(theBundle, entry, HttpStatus.INTERNAL_SERVER_ERROR.toString(), "Resource (" + postResource.getIdElement().asStringValue() + ") post failed");
				}
			} else {
				errorOnTransactionBatch(theBundle, entry, HttpStatus.BAD_REQUEST.toString(), "Bundle has no resource, which is needed for POST");
			}
		}

		// Do PUT now. 
		for (BundleEntryComponent entry : putList) {
			if (entry.hasResource()) {
				Resource putResource = entry.getResource();
				IdType putResourceFhirId = putResource.getIdElement();
				if (putResourceFhirId == null || putResourceFhirId.isEmpty()) {
					if (entry.hasRequest()) {
						String url = entry.getRequest().getUrl();
						if (url != null && !url.isBlank()) {
							putResourceFhirId = new IdType(url);
						}
					}
				}

				if (putResourceFhirId == null || putResourceFhirId.isEmpty()) {
					errorOnTransactionBatch(theBundle, entry, HttpStatus.BAD_REQUEST.toString(), "Resource ID is needed for PUT");
				} else {
					Resource existingResource = getResource(putResourceFhirId);
					if (existingResource == null || existingResource.isEmpty()) {
						errorOnTransactionBatch(theBundle, entry, HttpStatus.NOT_FOUND.toString(), "Resource for update does not exist");
					} else {
						Long idc = postResource(putResource, putResourceFhirId, theBundle);
						if (idc != null && idc != 0L) {
							toPutBack.add(existingResource);
							entry.getResponse().setStatus(HttpStatus.OK.toString());
						} else {
							errorOnTransactionBatch(theBundle, entry, HttpStatus.INTERNAL_SERVER_ERROR.toString(), "Resource (" + putResource.getIdElement().asStringValue() + ") put failed");
						}
					}
				}
			} else {
				errorOnTransactionBatch(theBundle, entry, HttpStatus.BAD_REQUEST.toString(), "Bundle has no resource, which is needed for PUT");
			}
		}

		// Do GET now.
		for (BundleEntryComponent entry : getList) {
			IdType fhirIdType = null;
			if (entry.hasRequest()) {
				String urlString = entry.getRequest().getUrl();
				if (urlString != null && !urlString.isBlank()) {
					fhirIdType = new IdType(urlString);
				}
			}

			if (fhirIdType == null) {
				// get it from resource.
				if (entry.hasResource()) {
					fhirIdType = entry.getResource().getIdElement();
				}
			}

			if (fhirIdType == null || fhirIdType.isEmpty()) {
				errorOnTransactionBatch(theBundle, entry, HttpStatus.BAD_REQUEST.toString(), "Request has no FHIR ID to GET");
			} else {
				Resource resource = getResource(fhirIdType);
				if (resource != null && !resource.isEmpty()) {
					entry.getResponse().setStatus(HttpStatus.OK.toString());
					entry.setResource(resource);
				} else {
					errorOnTransactionBatch(theBundle, entry, HttpStatus.INTERNAL_SERVER_ERROR.toString(), "Failed to GET Resource (" + fhirIdType.asStringValue() + ")");
				}
			}
		}


		return retVal;
	}

	@Override
	public List<ParameterWrapper> mapParameter(String parameter, Object value, boolean or) {
		throw new UnsupportedOperationException("Unimplemented method 'mapParameter'");
	}

	@Override
	public Bundle constructFHIR(Long fhirId, Concept entity) {
		throw new UnsupportedOperationException("Unimplemented method 'constructFHIR'");
	}

	@Override
	public Concept constructOmop(Long omopId, Bundle fhirResource) {
		throw new UnsupportedOperationException("Unimplemented method 'constructOmop'");
	}
}
