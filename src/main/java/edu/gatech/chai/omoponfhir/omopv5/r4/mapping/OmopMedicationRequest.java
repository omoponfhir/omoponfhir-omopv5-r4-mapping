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
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.hl7.fhir.r4.model.*;
import org.hl7.fhir.r4.model.Dosage.DosageDoseAndRateComponent;
import org.hl7.fhir.r4.model.Medication.MedicationIngredientComponent;
import org.hl7.fhir.r4.model.MedicationRequest.MedicationRequestDispenseRequestComponent;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;
import org.springframework.web.context.ContextLoaderListener;
import org.springframework.web.context.WebApplicationContext;

import ca.uhn.fhir.rest.param.DateRangeParam;
import ca.uhn.fhir.rest.param.ReferenceParam;
import ca.uhn.fhir.rest.param.TokenParam;
import edu.gatech.chai.omoponfhir.omopv5.r4.utilities.CodeableConceptUtil;
import edu.gatech.chai.omoponfhir.omopv5.r4.utilities.DateUtil;
import edu.gatech.chai.omoponfhir.omopv5.r4.utilities.ExtensionUtil;
import edu.gatech.chai.omopv5.dba.service.ConceptService;
import edu.gatech.chai.omopv5.dba.service.DrugExposureService;
import edu.gatech.chai.omopv5.dba.service.FPersonService;
import edu.gatech.chai.omopv5.dba.service.ParameterWrapper;
import edu.gatech.chai.omopv5.dba.service.ProviderService;
import edu.gatech.chai.omopv5.dba.service.VisitOccurrenceService;
import edu.gatech.chai.omopv5.model.entity.Concept;
import edu.gatech.chai.omopv5.model.entity.DrugExposure;
import edu.gatech.chai.omopv5.model.entity.FPerson;
import edu.gatech.chai.omopv5.model.entity.Provider;
import edu.gatech.chai.omopv5.model.entity.VisitOccurrence;

/**
 * 
 * @author mc142
 *
 * concept id	OHDSI drug type	FHIR
 * 38000179		Physician administered drug (identified as procedure), MedicationAdministration
 * 38000180		Inpatient administration, MedicationAdministration
 * 43542356	Physician administered drug (identified from EHR problem list), MedicationAdministration
 * 43542357	Physician administered drug (identified from referral record), MedicationAdministration
 * 43542358	Physician administered drug (identified from EHR observation), MedicationAdministration
 * 581373	Physician administered drug (identified from EHR order), MedicationAdministration
 * ******
 * 38000175	Prescription dispensed in pharmacy, MedicationDispense
 * 38000176	Prescription dispensed through mail order, MedicationDispense
 * 581452	Dispensed in Outpatient office, MedicationDispense
 * ******
 * 38000177	Prescription written, MedicationRequest
 * ******
 * 44787730	Patient Self-Reported Medication, MedicationStatement
 * 38000178	Medication list entry	 
 * 38000181	Drug era - 0 days persistence window	 
 * 38000182	Drug era - 30 days persistence window	 
 * 44777970	Randomized Drug	 
 */
public class OmopMedicationRequest extends BaseOmopResource<MedicationRequest, DrugExposure, DrugExposureService> {

	public static Long MEDICATIONREQUEST_CONCEPT_TYPE_ID = 38000177L;
	private static OmopMedicationRequest omopMedicationRequest = new OmopMedicationRequest();
	private VisitOccurrenceService visitOccurrenceService;
	private ConceptService conceptService;
	private ProviderService providerService;
	private FPersonService fPersonService;

	public OmopMedicationRequest(WebApplicationContext context) {
		super(context, DrugExposure.class, DrugExposureService.class, OmopMedicationRequest.FHIRTYPE);
		initialize(context);

		// Get count and put it in the counts.
		getSize(true);
	}
	
	public OmopMedicationRequest() {
		super(ContextLoaderListener.getCurrentWebApplicationContext(), DrugExposure.class, DrugExposureService.class, OmopMedicationRequest.FHIRTYPE);
		initialize(ContextLoaderListener.getCurrentWebApplicationContext());
	}

	private void initialize(WebApplicationContext context) {
		visitOccurrenceService = context.getBean(VisitOccurrenceService.class);
		conceptService = context.getBean(ConceptService.class);
		providerService = context.getBean(ProviderService.class);
		fPersonService = context.getBean(FPersonService.class);		
	}

	public static OmopMedicationRequest getInstance() {
		return OmopMedicationRequest.omopMedicationRequest;
	}
	
	public static String FHIRTYPE = "MedicationRequest";

	@Override
	public Long toDbase(MedicationRequest fhirResource, IdType fhirId) throws FHIRException {
		Long omopId = null;
		DrugExposure drugExposure = null;
		if (fhirId != null) {
			omopId = IdMapping.getOMOPfromFHIR(fhirId.getIdPartAsLong(), OmopMedicationRequest.FHIRTYPE);
		}

		drugExposure = constructOmop(omopId, fhirResource);

		Long retOmopId = null;
		if (omopId == null) {
			retOmopId = getMyOmopService().create(drugExposure).getId();
		} else {
			retOmopId = getMyOmopService().update(drugExposure).getId();
		}
		
		return IdMapping.getFHIRfromOMOP(retOmopId, OmopMedicationRequest.FHIRTYPE);
	}
	
	@Override
	public MedicationRequest constructResource(Long fhirId, DrugExposure entity, List<String> includes) {
		MedicationRequest fhirResource = constructFHIR(fhirId, entity);

		if (!includes.isEmpty()) {
			if (includes.contains("MedicationRequest:medication")) {
				Type medicationType = fhirResource.getMedication();
				if (medicationType instanceof Reference) {
					// We can include the medication resource as this is reference.
					// If medication is codeable concept, we can't include medication as
					// it does not have reference.
					Reference medicationReference = fhirResource.getMedicationReference();
					if (medicationReference != null && !medicationReference.isEmpty()) {
						IIdType medicationId = medicationReference.getReferenceElement();
						Long medicationFhirId = medicationId.getIdPartAsLong();
						Medication medication = OmopMedication.getInstance().constructFHIR(medicationFhirId, entity.getDrugConcept());
						medicationReference.setResource(medication);
					}
				}
			}
		}
		
		return fhirResource;
	}

	@Override
	public MedicationRequest constructFHIR(Long fhirId, DrugExposure entity) {
		MedicationRequest medicationRequest = new MedicationRequest();
		
		medicationRequest.setId(new IdType(fhirId));
		
		// Subject from FPerson
		Reference patientRef = new Reference(new IdType(OmopPatient.FHIRTYPE, entity.getFPerson().getId()));
		patientRef.setDisplay(entity.getFPerson().getNameAsSingleString());
		medicationRequest.setSubject(patientRef);		
		
		// AuthoredOn. TODO: endDate is lost if we map to AuthoredOn.
		Date startDate = entity.getDrugExposureStartDate();
//		Date endDate = entity.getDrugExposureEndDate();
		if (startDate != null)
			medicationRequest.setAuthoredOn(startDate);
		
		// See what type of Medication info we want to return
		String medType = System.getenv("MEDICATION_TYPE");			
		if (medType != null && !medType.isEmpty() && "local".equalsIgnoreCase(medType)) {
			CodeableConcept medicationCodeableConcept;
			CodeableConcept ingredientCodeableConcept;
			Medication medicationResource = new Medication();
			try {
				medicationCodeableConcept = CodeableConceptUtil.getCodeableConceptFromOmopConcept(entity.getDrugConcept());
				List<Concept> ingredients = conceptService.getIngredient(entity.getDrugConcept());
				for (Concept ingredient: ingredients) {
					ingredientCodeableConcept = CodeableConceptUtil.getCodeableConceptFromOmopConcept(ingredient);
					MedicationIngredientComponent medIngredientComponent = new MedicationIngredientComponent();
					medIngredientComponent.setItem(ingredientCodeableConcept);
					medicationResource.addIngredient(medIngredientComponent);					
				}
			} catch (FHIRException e) {
				e.printStackTrace();
				return null;
			}
			medicationResource.setCode(medicationCodeableConcept);
			medicationResource.setId("med1");
			medicationRequest.addContained(medicationResource);
			medicationRequest.setMedication(new Reference("#med1"));			
		} else if (medType != null && !medType.isEmpty() && "link".equalsIgnoreCase(medType)) {
			// Get Medication in a reference. 
			Reference medicationReference = new Reference(new IdType(OmopMedication.FHIRTYPE, entity.getDrugConcept().getId()));
			medicationRequest.setMedication(medicationReference);			
		} else {
			CodeableConcept medicationCodeableConcept;
			try {
				medicationCodeableConcept = CodeableConceptUtil.getCodeableConceptFromOmopConcept(entity.getDrugConcept());
			} catch (FHIRException e1) {
				e1.printStackTrace();
				return null;
			}
			medicationRequest.setMedication(medicationCodeableConcept);
		}
		
		// Dosage mapping
//		Double dose = entity.getEffectiveDrugDose();
//		SimpleQuantity doseQuantity = new SimpleQuantity();
//		if (dose != null) {
//			doseQuantity.setValue(dose);
//		}
//		
//		Concept unitConcept = entity.getDoseUnitConcept();
//		String unitUnit = null;
//		String unitCode = null;
//		String unitSystem = null;
//		if (unitConcept != null) {
//			String omopUnitVocab = unitConcept.getVocabulary();
//			String omopUnitCode = unitConcept.getConceptCode();
//			String omopUnitName = unitConcept.getName();
//			
//			String fhirUnitUri;
//			try {
//				fhirUnitUri = OmopCodeableConceptMapping.fhirUriforOmopVocabulary(omopUnitVocab);
//				if ("None".equals(fhirUnitUri)) {
////					fhirUnitUri = unitConcept.getVocabulary().getVocabularyReference();
//					fhirUnitUri = "NotAvailable";
//				}
//
//				unitUnit = omopUnitName;
//				unitCode = omopUnitCode;
//				unitSystem = fhirUnitUri; 
//			} catch (FHIRException e) {
//				e.printStackTrace();
//			}
//		}
//		
//		if (!doseQuantity.isEmpty()) {
//			doseQuantity.setUnit(unitUnit);
//			doseQuantity.setCode(unitCode);
//			doseQuantity.setSystem(unitSystem);
//			
//			Dosage dosage = new Dosage();
//			dosage.setDose(doseQuantity);
//			medicationRequest.addDosageInstruction(dosage);
//		}

		// dispense request mapping.
		Integer refills = entity.getRefills();
		MedicationRequestDispenseRequestComponent dispenseRequest = new MedicationRequestDispenseRequestComponent();
		if (refills != null) {
			dispenseRequest.setNumberOfRepeatsAllowed(refills);
		}

		String unitSystem = "";
		String unitCode = "";
		String unitUnit = entity.getDoseUnitSourceValue();
		if (unitUnit != null && !unitUnit.isEmpty()) {
			Concept unitConcept = CodeableConceptUtil.getOmopConceptWithOmopCode(conceptService, unitUnit);
			if (unitConcept != null) {
				String vocId = unitConcept.getVocabularyId();
				unitSystem = fhirOmopVocabularyMap.getFhirSystemNameFromOmopVocabulary(vocId);
				unitCode = unitConcept.getConceptCode();
				unitUnit = unitConcept.getConceptName();
			}
		}
		
		Double quantity = entity.getQuantity();
		if (quantity != null) {
			SimpleQuantity simpleQty = new SimpleQuantity();
			simpleQty.setValue(quantity);
			simpleQty.setUnit(unitUnit);
			simpleQty.setCode(unitCode);
			simpleQty.setSystem(unitSystem);
			dispenseRequest.setQuantity(simpleQty);
		}
		
		Integer daysSupply = entity.getDaysSupply();
		if (daysSupply != null) {
			Duration qty = new Duration();
			qty.setValue(daysSupply);
			// Set the UCUM unit to day.
			String fhirUri = fhirOmopVocabularyMap.getFhirSystemNameFromOmopVocabulary("UCUM");
			qty.setSystem(fhirUri);
			qty.setCode("d");
			qty.setUnit("day");
			dispenseRequest.setExpectedSupplyDuration(qty);
		}
		
		if (!dispenseRequest.isEmpty()) {
			medicationRequest.setDispenseRequest(dispenseRequest);
		}
		
		// Recorder mapping
		Provider provider = entity.getProvider();
		if (provider != null) {
			Reference recorderReference = 
					new Reference(new IdType(OmopPractitioner.FHIRTYPE, provider.getId()));
			recorderReference.setDisplay(provider.getProviderName());
			medicationRequest.setRecorder(recorderReference);
		}
		
		// Context mapping
		VisitOccurrence visitOccurrence = entity.getVisitOccurrence();
		if (visitOccurrence != null) {
			Reference contextReference = 
					new Reference(new IdType(OmopEncounter.FHIRTYPE, visitOccurrence.getId()));
			medicationRequest.setEncounter(contextReference);
		}
		
		return medicationRequest;
	}
	
	@Override
	public List<ParameterWrapper> mapParameter(String parameter, Object value, boolean or) {
		List<ParameterWrapper> mapList = new ArrayList<ParameterWrapper>();
		ParameterWrapper paramWrapper = new ParameterWrapper();
        if (or) paramWrapper.setUpperRelationship("or");
        else paramWrapper.setUpperRelationship("and");

		switch (parameter) {
		case MedicationRequest.SP_RES_ID:
			String medicationRequestId = ((TokenParam) value).getValue();
			paramWrapper.setParameterType("Long");
			paramWrapper.setParameters(Arrays.asList("id"));
			paramWrapper.setOperators(Arrays.asList("="));
			paramWrapper.setValues(Arrays.asList(medicationRequestId));
			paramWrapper.setRelationship("or");
			mapList.add(paramWrapper);
			break;
		case MedicationRequest.SP_CODE:
		case "Medication:"+Medication.SP_CODE:
			String system = ((TokenParam) value).getSystem();
			String code = ((TokenParam) value).getValue();
			
			if ((system == null || system.isEmpty()) && (code == null || code.isEmpty()))
				break;
			
			String omopVocabulary = "None";
			if (system != null && !system.isEmpty()) {
				try {
//					omopVocabulary = OmopCodeableConceptMapping.omopVocabularyforFhirUri(system);
					omopVocabulary = fhirOmopVocabularyMap.getOmopVocabularyFromFhirSystemName(system);
				} catch (FHIRException e) {
					e.printStackTrace();
					break;
				}
			}

			paramWrapper.setParameterType("String");
			if ("None".equals(omopVocabulary) && code != null && !code.isEmpty()) {
				paramWrapper.setParameters(Arrays.asList("drugConcept.conceptCode"));
				paramWrapper.setOperators(Arrays.asList("like"));
				paramWrapper.setValues(Arrays.asList(code));
				paramWrapper.setRelationship("or");
			} else if (!"None".equals(omopVocabulary) && (code == null || code.isEmpty())) {
				paramWrapper.setParameters(Arrays.asList("drugConcept.vocabularyId"));
				paramWrapper.setOperators(Arrays.asList("like"));
				paramWrapper.setValues(Arrays.asList(omopVocabulary));
				paramWrapper.setRelationship("or");
			} else {
				paramWrapper.setParameters(Arrays.asList("drugConcept.vocabularyId", "drugConcept.conceptCode"));
				paramWrapper.setOperators(Arrays.asList("like","like"));
				paramWrapper.setValues(Arrays.asList(omopVocabulary, code));
				paramWrapper.setRelationship("and");
			}
			mapList.add(paramWrapper);
			break;
		case MedicationRequest.SP_ENCOUNTER:
			Long fhirEncounterId = ((ReferenceParam) value).getIdPartAsLong();
			Long omopVisitOccurrenceId = IdMapping.getOMOPfromFHIR(fhirEncounterId, OmopEncounter.FHIRTYPE);
//			String resourceName = ((ReferenceParam) value).getResourceType();
			
			// We support Encounter so the resource type should be Encounter.
			if (omopVisitOccurrenceId != null) {
				paramWrapper.setParameterType("Long");
				paramWrapper.setParameters(Arrays.asList("visitOccurrence.id"));
				paramWrapper.setOperators(Arrays.asList("="));
				paramWrapper.setValues(Arrays.asList(String.valueOf(omopVisitOccurrenceId)));
				paramWrapper.setRelationship("or");
				mapList.add(paramWrapper);
			}
			break;
		case MedicationRequest.SP_AUTHOREDON:
			DateRangeParam authoredOnDataParam = ((DateRangeParam) value);
 			DateUtil.constructParameterWrapper(authoredOnDataParam, "drugExposureStartDate", paramWrapper, mapList);
 			ParameterWrapper paramWrapper1 = new ParameterWrapper();
 			paramWrapper1.setUpperRelationship("or");
 			DateUtil.constructParameterWrapper(authoredOnDataParam, "drugExposureEndDate", paramWrapper1, mapList);
			break;
//		case MedicationRequest.SP_PATIENT:
//		case MedicationRequest.SP_SUBJECT:
//			ReferenceParam patientReference = ((ReferenceParam) value);
//			Long fhirPatientId = patientReference.getIdPartAsLong();
//			Long omopPersonId = IdMapping.getOMOPfromFHIR(fhirPatientId, PatientResourceProvider.getType());
//
//			String omopPersonIdString = String.valueOf(omopPersonId);
//			
//			paramWrapper.setParameterType("Long");
//			paramWrapper.setParameters(Arrays.asList("fPerson.id"));
//			paramWrapper.setOperators(Arrays.asList("="));
//			paramWrapper.setValues(Arrays.asList(omopPersonIdString));
//			paramWrapper.setRelationship("or");
//			mapList.add(paramWrapper);
//			break;
		case "Patient:" + Patient.SP_RES_ID:
			addParamlistForPatientIDName(parameter, (String) value, paramWrapper, mapList);
			break;
		case "Patient:" + Patient.SP_NAME:
			addParamlistForPatientIDName(parameter, (String) value, paramWrapper, mapList);
			break;
		case "Patient:" + Patient.SP_IDENTIFIER:
			addParamlistForPatientIDName(parameter, (String) value, paramWrapper, mapList);
			break;
		case "Medication:"+Medication.SP_RES_ID:
			String pId = (String) value;
			paramWrapper.setParameterType("Long");
			paramWrapper.setParameters(Arrays.asList("drugConcept.id"));
			paramWrapper.setOperators(Arrays.asList("="));
			paramWrapper.setValues(Arrays.asList(pId));
			paramWrapper.setRelationship("or");
			mapList.add(paramWrapper);
			break;
		default:
			mapList = null;
		}
		
		return mapList;
	}
	
	final ParameterWrapper filterParam = new ParameterWrapper(
			"Long",
			Arrays.asList("drugTypeConcept.id"),
			Arrays.asList("="),
			Arrays.asList(String.valueOf(OmopMedicationRequest.MEDICATIONREQUEST_CONCEPT_TYPE_ID)),
			"or"
			);

	@Override
	public Long getSize() {
		List<ParameterWrapper> paramList = new ArrayList<ParameterWrapper> ();
		// call getSize with empty parameter list. The getSize will add filter parameter.

		Long size = getSize(paramList);
		ExtensionUtil.addResourceCount(OmopMedicationRequest.FHIRTYPE, size);
		
		return size;
	}

	@Override
	public Long getSize(List<ParameterWrapper> paramList) {
		paramList.add(filterParam);

		return getMyOmopService().getSize(paramList);
	}

	@Override
	public void searchWithoutParams(int fromIndex, int toIndex, List<IBaseResource> listResources,
			List<String> includes, String sort) {

		// This is read all. But, since we will add an exception conditions to add filter.
		// we will call the search with params method.
		List<ParameterWrapper> paramList = new ArrayList<ParameterWrapper> ();
		searchWithParams (fromIndex, toIndex, paramList, listResources, includes, sort);
	}

	@Override
	public void searchWithParams(int fromIndex, int toIndex, List<ParameterWrapper> mapList,
			List<IBaseResource> listResources, List<String> includes, String sort) {
		mapList.add(filterParam);

		List<DrugExposure> entities = getMyOmopService().searchWithParams(fromIndex, toIndex, mapList, sort);

		for (DrugExposure entity : entities) {
			Long omopId = entity.getIdAsLong();
			Long fhirId = IdMapping.getFHIRfromOMOP(omopId, getMyFhirResourceType());
			MedicationRequest fhirResource = constructResource(fhirId, entity, includes);
			if (fhirResource != null) {
				listResources.add(fhirResource);			
				// Do the rev_include and add the resource to the list.
				addRevIncludes(omopId, includes, listResources);
			}

		}
	}

	@Override
	public DrugExposure constructOmop(Long omopId, MedicationRequest fhirResource) {
		DrugExposure drugExposure = null;


		// Set patient.
		Reference patientReference = fhirResource.getSubject();
		if (patientReference == null) {
			throw new FHIRException("Patient must exist.");
		}

		Long patientFhirId = patientReference.getReferenceElement().getIdPartAsLong();
		Long omopFPersonId = IdMapping.getOMOPfromFHIR(patientFhirId, OmopPatient.FHIRTYPE);

		FPerson fPerson = fPersonService.findById(omopFPersonId);
		if (fPerson == null) {
			throw new FHIRException("Patient/"+patientFhirId+" is not valid");
		}

		// Get start date from authored on date 
		Date authoredDate = fhirResource.getAuthoredOn();

		if (omopId != null) {
			// Update
			drugExposure = getMyOmopService().findById(omopId);
			if (drugExposure == null) { 
				throw new FHIRException(fhirResource.getId()+" does not exist");
			}
		} else {
			// Create
			List<Identifier> identifiers = fhirResource.getIdentifier();
			for (Identifier identifier: identifiers) {
				if (identifier.isEmpty()) continue;
				String identifierValue = identifier.getValue();
				List<DrugExposure> results = getMyOmopService().searchByColumnString("drugSourceValue", identifierValue);
				if (results.size() > 0) {
					drugExposure = results.get(0);
					omopId = drugExposure.getId();
					break;
				}
			}
			
			if (drugExposure == null) {
				drugExposure = new DrugExposure();
				// Add the source column.
				Identifier identifier = fhirResource.getIdentifierFirstRep();
				if (!identifier.isEmpty()) {
					drugExposure.setDrugSourceValue(identifier.getValue());
				}
			}
		}
		
		// we should have a valid drugExposure.
		drugExposure.setFPerson(fPerson);

		// Get medication[x]
		Type medicationType = fhirResource.getMedication();
		Concept omopConcept = null;
		CodeableConcept medicationCodeableConcept = null;
		if (medicationType instanceof Reference) {
			// We may have reference.
			Reference medicationReference = null;
			try {
				medicationReference = fhirResource.getMedicationReference();
				if (medicationReference.isEmpty()) {
					// This is an error. We require this.
					throw new FHIRException("Medication[CodeableConcept or Reference] is missing");
				} else {
					String medicationReferenceId = medicationReference.getReferenceElement().getIdPart();
					if (medicationReference.getReferenceElement().isLocal()) {
						List<Resource> contains = fhirResource.getContained();
						for (Resource resource: contains) {
							if (!resource.isEmpty() &&
								resource.getIdElement().getIdPart().equals(medicationReferenceId)) {

								// This must medication resource. 
								Medication medicationResource = (Medication) resource;
								medicationCodeableConcept = medicationResource.getCode();
								break;
							}
						}
					}
				}			
			} catch (FHIRException e) {
				e.printStackTrace();
			}

			if (medicationCodeableConcept == null || medicationCodeableConcept.isEmpty()) {
				if (medicationReference != null) {
					String medicationReferenceDisplay = medicationReference.getDisplay();
					if (medicationReferenceDisplay != null && !medicationReferenceDisplay.isBlank()) {
						medicationCodeableConcept = new CodeableConcept();
						medicationCodeableConcept.setText(medicationReferenceDisplay);
					}
				}
			}
		} else {
			try {
				medicationCodeableConcept = fhirResource.getMedicationCodeableConcept();
			} catch (FHIRException e) {
				e.printStackTrace();
			}
		}
		
		if (medicationCodeableConcept == null || medicationCodeableConcept.isEmpty()) { 		
			throw new FHIRException("Medication[CodeableConcept or Reference] could not be mapped");
		}

		try {
			omopConcept = CodeableConceptUtil.searchConcept(conceptService, medicationCodeableConcept);
			if (omopConcept == null) {
				if (medicationCodeableConcept.getText() != null && !medicationCodeableConcept.getText().isEmpty()) {
					drugExposure.setDrugSourceValue(medicationCodeableConcept.getText());
				} else {
					drugExposure.setDrugSourceValue(CodeableConceptUtil.convert2String(medicationCodeableConcept.getCodingFirstRep()));
				}
				drugExposure.setDrugConcept(new Concept(0L));
				// throw new FHIRException("Medication[CodeableConcept or Reference] could not be found");
			} else {
				drugExposure.setDrugConcept(omopConcept);
			}
		} catch (FHIRException e) {
			e.printStackTrace();
		}

		// Set drug exposure type
		Concept drugExposureType = new Concept();
		drugExposureType.setId(MEDICATIONREQUEST_CONCEPT_TYPE_ID);
		drugExposure.setDrugTypeConcept(drugExposureType);
				
		// Set start date from authored on date 
		drugExposure.setDrugExposureStartDate(authoredDate);
		drugExposure.setDrugExposureEndDate(authoredDate);

		// Set VisitOccurrence 
		Reference encounterReference = fhirResource.getEncounter();
		if (encounterReference != null && !encounterReference.isEmpty() && OmopEncounter.FHIRTYPE.equals(encounterReference.getReferenceElement().getResourceType())) {
			// Get fhirIDLong.
			Long fhirEncounterIdLong = encounterReference.getReferenceElement().getIdPartAsLong();
			Long omopEncounterId = IdMapping.getOMOPfromFHIR(fhirEncounterIdLong, OmopEncounter.FHIRTYPE);
			if (omopEncounterId != null) {
				VisitOccurrence visitOccurrence = visitOccurrenceService.findById(omopEncounterId);
				if (visitOccurrence != null)
					drugExposure.setVisitOccurrence(visitOccurrence);
			} else {
				throw new FHIRException("Encounter/"+fhirEncounterIdLong+" is not valid.");
			}
		}		
		
		// dosageInstruction 
		List<Dosage> dosageInstructions = fhirResource.getDosageInstruction();
		if (dosageInstructions != null && !dosageInstructions.isEmpty()) {
			for (Dosage dosageInstruction: dosageInstructions) {
				CodeableConcept route = dosageInstruction.getRoute();
				if (route != null && !route.isEmpty()) {
					for (Coding routeCoding : route.getCoding()) {
						Concept routeConept = CodeableConceptUtil.getOmopConceptWithFhirConcept(conceptService, routeCoding);
						if (routeConept != null && routeConept.getId() != null && routeConept.getId() != 0) {
							drugExposure.setRouteConcept(routeConept);
							if (route.getText() != null && !route.getText().isEmpty()) {
								drugExposure.setRouteSourceValue(route.getText());
							} else {
								drugExposure.setRouteSourceValue(routeCoding.getDisplay());
							}
							break;
						} 
					}

					if (drugExposure.getRouteConcept() == null || drugExposure.getRouteConcept().getId() == null || drugExposure.getRouteConcept().getId() == 0) {
						// capture text or display if possible.
						if (route.getText() != null && !route.getText().isEmpty()) {
							drugExposure.setRouteSourceValue(route.getText());
						} else {
							drugExposure.setRouteSourceValue(route.getCodingFirstRep().getDisplay());
						}						
					} 
				}

				List<DosageDoseAndRateComponent> dosageAndRates = dosageInstruction.getDoseAndRate();
				for (DosageDoseAndRateComponent dosageAndRate : dosageAndRates) {
					Type doseType = dosageAndRate.getDose();
					if (doseType instanceof Quantity) {
						Quantity qty = (Quantity) doseType;
						if (qty.isEmpty()) continue;
						drugExposure.setQuantity(qty.getValue().doubleValue());
						String doseCode = qty.getCode();
						String doseSystem = qty.getSystem();
						if (doseSystem != null && !doseSystem.isBlank() && doseCode != null && !doseCode.isBlank()) {
							String vocabId = fhirOmopVocabularyMap.getOmopVocabularyFromFhirSystemName(doseSystem);
							Concept unitConcept = 
								CodeableConceptUtil.getOmopConceptWithOmopVacabIdAndCode(conceptService, vocabId, doseCode);
							if (unitConcept != null) {
								drugExposure.setDoseUnitSourceValue(unitConcept.getConceptCode());
							}
						} else {
							if (qty.getCode() != null && !qty.getCode().isBlank()) {
								drugExposure.setDoseUnitSourceValue(qty.getCode());
							} else if (qty.getUnit() != null && !qty.getUnit().isBlank()) {
								drugExposure.setDoseUnitSourceValue(qty.getUnit());
							}
						}
						break;
					}
				}
			}
		} else {
			// dispense request 
			MedicationRequestDispenseRequestComponent dispenseRequest = fhirResource.getDispenseRequest();
			if (dispenseRequest != null && !dispenseRequest.isEmpty()) {
				Integer refills = dispenseRequest.getNumberOfRepeatsAllowed();
				if (refills != null) {
					drugExposure.setRefills(refills);
				}

				Quantity qty = dispenseRequest.getQuantity();
				if (qty != null) {
					drugExposure.setQuantity(qty.getValue().doubleValue());
					String doseCode = qty.getCode();
					String doseSystem = qty.getSystem();
					if (doseSystem != null && !doseSystem.isBlank() && doseCode != null && !doseCode.isBlank()) {
						String vocabId = fhirOmopVocabularyMap.getOmopVocabularyFromFhirSystemName(doseSystem);
						Concept unitConcept = 
						CodeableConceptUtil.getOmopConceptWithOmopVacabIdAndCode(conceptService, vocabId, doseCode);
						if (unitConcept != null) {
							drugExposure.setDoseUnitSourceValue(unitConcept.getConceptCode());
						}
					} else {
						if (qty.getCode() != null && !qty.getCode().isBlank()) {
							drugExposure.setDoseUnitSourceValue(qty.getCode());
						} else if (qty.getUnit() != null && !qty.getUnit().isBlank()) {
							drugExposure.setDoseUnitSourceValue(qty.getUnit());
						}
					}
				}
			}
		}
		
		Reference practitionerRef = fhirResource.getRecorder();
		if (practitionerRef != null && !practitionerRef.isEmpty()) {
			Long fhirPractitionerIdLong = 
					practitionerRef.getReferenceElement().getIdPartAsLong();
			Long omopProviderId = IdMapping.getOMOPfromFHIR(fhirPractitionerIdLong, OmopPractitioner.FHIRTYPE);
			Provider provider = providerService.findById(omopProviderId);
			if (provider != null) {
				drugExposure.setProvider(provider);
			}			
		}
		
		return drugExposure;
	}
}
