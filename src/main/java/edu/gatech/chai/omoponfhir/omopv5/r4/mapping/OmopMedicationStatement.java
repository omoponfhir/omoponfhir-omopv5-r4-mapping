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

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.hl7.fhir.r4.model.*;
import org.hl7.fhir.r4.model.MedicationStatement.MedicationStatementStatus;
import org.hl7.fhir.r4.model.ValueSet.ConceptReferenceComponent;
import org.hl7.fhir.r4.model.ValueSet.ConceptSetComponent;
import org.hl7.fhir.exceptions.FHIRException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.context.ContextLoaderListener;
import org.springframework.web.context.WebApplicationContext;

import ca.uhn.fhir.rest.param.DateRangeParam;
import ca.uhn.fhir.rest.param.ReferenceParam;
import ca.uhn.fhir.rest.param.TokenParam;
import ca.uhn.fhir.rest.param.TokenParamModifier;
import edu.gatech.chai.omoponfhir.omopv5.r4.utilities.CodeableConceptUtil;
import edu.gatech.chai.omoponfhir.omopv5.r4.utilities.DateUtil;
import edu.gatech.chai.omoponfhir.omopv5.r4.utilities.TerminologyServiceClient;
import edu.gatech.chai.omoponfhir.omopv5.r4.utilities.ThrowFHIRExceptions;
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
 *         concept id OHDSI drug type FHIR 38000179 Physician administered drug
 *         (identified as procedure), MedicationAdministration 38000180
 *         Inpatient administration, MedicationAdministration 43542356 Physician
 *         administered drug (identified from EHR problem list),
 *         MedicationAdministration 43542357 Physician administered drug
 *         (identified from referral record), MedicationAdministration 43542358
 *         Physician administered drug (identified from EHR observation),
 *         MedicationAdministration 581373 Physician administered drug
 *         (identified from EHR order), MedicationAdministration 38000175
 *         Prescription dispensed in pharmacy, MedicationDispense 38000176
 *         Prescription dispensed through mail order, MedicationDispense 581452
 *         Dispensed in Outpatient office, MedicationDispense ****** 38000177
 *         Prescription written, MedicationRequest ****** 44787730 Patient
 *         Self-Reported Medication, MedicationStatement 38000178 Medication
 *         list entry 38000181 Drug era - 0 days persistence window 38000182
 *         Drug era - 30 days persistence window 44777970 Randomized Drug
 * 
 *         NOTE: We will take all the drug exposure into MedicationStatement.
 *         It's hard to distinguish the medicaitons for MedicationStatement.
 *
 */
public class OmopMedicationStatement extends BaseOmopResource<MedicationStatement, DrugExposure, DrugExposureService> {
	private static final Logger logger = LoggerFactory.getLogger(OmopMedicationStatement.class);

	private static Long MEDICATIONSTATEMENT_CONCEPT_TYPE_ID = 44787730L;
	private static OmopMedicationStatement omopMedicationStatement = new OmopMedicationStatement();
	private VisitOccurrenceService visitOccurrenceService;
	private ConceptService conceptService;
	private ProviderService providerService;
	private FPersonService fPersonService;

	public OmopMedicationStatement(WebApplicationContext context) {
		super(context, DrugExposure.class, DrugExposureService.class, OmopMedicationStatement.FHIRTYPE);
		initialize(context);

		// Get count and put it in the counts.
		getSize(true);
	}

	public OmopMedicationStatement() {
		super(ContextLoaderListener.getCurrentWebApplicationContext(), DrugExposure.class, DrugExposureService.class,
				OmopMedicationStatement.FHIRTYPE);
		initialize(ContextLoaderListener.getCurrentWebApplicationContext());
	}

	private void initialize(WebApplicationContext context) {
		visitOccurrenceService = context.getBean(VisitOccurrenceService.class);
		conceptService = context.getBean(ConceptService.class);
		providerService = context.getBean(ProviderService.class);
		fPersonService = context.getBean(FPersonService.class);
	}



	public static OmopMedicationStatement getInstance() {
		return OmopMedicationStatement.omopMedicationStatement;
	}

	public static String FHIRTYPE = "MedicationStatement";

	@Override
	public Long toDbase(MedicationStatement fhirResource, IdType fhirId) throws FHIRException {
		Long omopId = null;
		if (fhirId != null) {
			// Update
			Long fhirIdLong = fhirId.getIdPartAsLong();
			omopId = IdMapping.getOMOPfromFHIR(fhirIdLong, OmopMedicationStatement.FHIRTYPE);
		}

		DrugExposure drugExposure = constructOmop(omopId, fhirResource);

		Long retOmopId = null;
		if (omopId == null) {
			retOmopId = getMyOmopService().create(drugExposure).getId();
		} else {
			retOmopId = getMyOmopService().update(drugExposure).getId();
		}

		return IdMapping.getFHIRfromOMOP(retOmopId, OmopMedicationStatement.FHIRTYPE);
	}

	@Override
	public MedicationStatement constructFHIR(Long fhirId, DrugExposure entity) {
		MedicationStatement medicationStatement = new MedicationStatement();
		medicationStatement.setId(new IdType(fhirId));

		// status is required field in FHIR MedicationStatement.
		// However, we do not have a field in OMOP.
		// We will use stop_reason field to see if there is any data in there.
		// If we have data there, we set the status stopped. Otherwise, active.
		// We may need to use reasonNotTaken. But, we don't have a code for
		// that.
		// We will use note to put the reason if exists.
		if (entity.getStopReason() != null) {
			medicationStatement.setStatus(MedicationStatementStatus.STOPPED);
			Annotation annotation = new Annotation();
			annotation.setText(entity.getStopReason());
			medicationStatement.addNote(annotation);
		} else {
			medicationStatement.setStatus(MedicationStatementStatus.ACTIVE);
		}

		FPerson fPerson = entity.getFPerson();
		if (fPerson != null) {
			Long omopFpersonId = fPerson.getId();
			Long fhirPatientId = IdMapping.getFHIRfromOMOP(omopFpersonId,
					OmopMedicationStatement.FHIRTYPE);
			Reference subjectReference = new Reference(new IdType(OmopPatient.FHIRTYPE, fhirPatientId));
			String familyName = fPerson.getFamilyName();
			String given1 = fPerson.getGivenName1();
			String given2 = fPerson.getGivenName2();
			String name = null;
			if (familyName != null && !familyName.isEmpty()) {
				name = familyName;
				if (given1 != null && !given1.isEmpty()) {
					name = name.concat(", " + given1);
					if (given2 != null && !given2.isEmpty()) {
						name = name.concat(" " + given2);
					}
				} else {
					if (given2 != null && !given2.isEmpty()) {
						name = name.concat(", " + given2);
					}
				}
			} else {
				if (given1 != null && !given1.isEmpty()) {
					name = given1;
					if (given2 != null && given2.isEmpty()) {
						name = name.concat(" " + given2);
					}
				} else if (given2 != null && given2.isEmpty()) {
					name = given2;
				}

			}
			if (name != null)
				subjectReference.setDisplay(name);
			medicationStatement.setSubject(subjectReference);
		}

		// See if we have encounter associated with this medication statement.
		VisitOccurrence visitOccurrence = entity.getVisitOccurrence();
		if (visitOccurrence != null) {
			Long fhirEncounterId = IdMapping.getFHIRfromOMOP(visitOccurrence.getId(), OmopEncounter.FHIRTYPE);
			Reference reference = new Reference(new IdType(OmopEncounter.FHIRTYPE, fhirEncounterId));
			medicationStatement.setContext(reference);
		}

		// Get medicationCodeableConcept
		Concept drugConcept = entity.getDrugConcept();
		if (drugConcept == null) {
			// drug concept is required field. But, in case that this is null, we use 
			// no matching code concept here to avoid an exception.
			drugConcept = conceptService.findById(0L);
		}

		CodeableConcept medication;
		try {
			medication = CodeableConceptUtil.getCodeableConceptFromOmopConcept(drugConcept);
		} catch (FHIRException e1) {
			e1.printStackTrace();
			return null;
		}

		medicationStatement.setMedication(medication);

		// See if we can add ingredient version of this medication.
		// Concept ingredient = conceptService.getIngredient(drugConcept);
		// if (ingredient != null) {
		// CodeableConcept ingredientCodeableConcept;
		// try {
		// ingredientCodeableConcept =
		// CodeableConceptUtil.getCodeableConceptFromOmopConcept(ingredient);
		// if (!ingredientCodeableConcept.isEmpty()) {
		// // We have ingredient information. Add this to MedicationStatement.
		// // To do this, we need to add Medication resource to contained
		// section.
		// Medication medicationResource = new Medication();
		// medicationResource.setCode(medication);
		// MedicationIngredientComponent medIngredientComponent = new
		// MedicationIngredientComponent();
		// medIngredientComponent.setItem(ingredientCodeableConcept);
		// medicationResource.addIngredient(medIngredientComponent);
		// medicationResource.setId("med1");
		// medicationStatement.addContained(medicationResource);
		// medicationStatement.setMedication(new Reference("#med1"));
		// }
		// } catch (FHIRException e) {
		// e.printStackTrace();
		// return null;
		// }
		// } else {
		// medicationStatement.setMedication(medication);
		// }

		// Get effectivePeriod
		Date startDate = entity.getDrugExposureStartDate();
		Date startDateTime = entity.getDrugExposureEndDateTime();
		Date endDate = entity.getDrugExposureEndDate();
		Date endDateTime = entity.getDrugExposureEndDateTime();

		// startDate is required. Thus, if startDate is null, we 
		// just put 0, which is Jan 1970.
		Calendar calendar = Calendar.getInstance();
		if (startDate != null) {
			// First make one date including time if available.
			if (startDateTime != null) {
				calendar.setTime(startDateTime);
				int hour24 = calendar.get(Calendar.HOUR_OF_DAY);
				int min = calendar.get(Calendar.MINUTE);
				int sec = calendar.get(Calendar.SECOND);

				calendar.setTime(startDate);
				calendar.set(Calendar.HOUR_OF_DAY, hour24);
				calendar.set(Calendar.MINUTE, min);
				calendar.set(Calendar.SECOND, sec);

				startDate = calendar.getTime();
			}

			if (endDate != null && endDateTime != null) {
				calendar.setTime(endDateTime);
				int hour24 = calendar.get(Calendar.HOUR_OF_DAY);
				int min = calendar.get(Calendar.MINUTE);
				int sec = calendar.get(Calendar.SECOND);

				calendar.setTime(endDate);
				calendar.set(Calendar.HOUR_OF_DAY, hour24);
				calendar.set(Calendar.MINUTE, min);
				calendar.set(Calendar.SECOND, sec);

				endDate = calendar.getTime();
			}

			if (endDate == null || (startDate.equals(endDate))) {
				// This is single datatime
				medicationStatement.setEffective(new DateTimeType(startDate));
			} else {
				Period period = new Period();

				period.setStart(startDate);
				period.setEnd(endDate);
				
				medicationStatement.setEffective(period);
			}
		} else {
			medicationStatement.setEffective(new DateTimeType(new Date(0L)));
		}

		// Get drug dose
//		Double effectiveDrugDose = entity.getEffectiveDrugDose();
		Double omopQuantity = entity.getQuantity();
		SimpleQuantity quantity = new SimpleQuantity();
//		if (effectiveDrugDose != null) {
//			quantity.setValue(effectiveDrugDose);
		if (omopQuantity != null) {
			quantity.setValue(omopQuantity);
		}

		String unitString = entity.getDoseUnitSourceValue();
		if (unitString != null && !unitString.isBlank()) {
			try {
				Concept unitConcept = CodeableConceptUtil.getOmopConceptWithOmopCode(conceptService, unitString);
				if (unitConcept != null) {
					String unitFhirUri = OmopCodeableConceptMapping
							.fhirUriforOmopVocabulary(unitConcept.getVocabularyId());
					if (!"None".equals(unitFhirUri)) {
						String unitDisplay = unitConcept.getConceptName();
						String unitCode = unitConcept.getConceptCode();
						quantity.setUnit(unitDisplay);
						quantity.setSystem(unitFhirUri);
						quantity.setCode(unitCode);
					}
				} else {
					quantity.setUnit(unitString);
				}
			} catch (FHIRException e) {
				// We have null vocabulary id in the unit concept.
				// Throw error and move on.
				e.printStackTrace();
			}
		}

		Dosage dosage = new Dosage();
		if (!quantity.isEmpty()) {
			Dosage.DosageDoseAndRateComponent tempComponent = new Dosage.DosageDoseAndRateComponent();
			tempComponent.setDose(quantity);
			dosage.addDoseAndRate(tempComponent);
		}

		Concept routeConcept = entity.getRouteConcept();
		if (routeConcept != null) {
			try {
				String myUri = fhirOmopVocabularyMap.getFhirSystemNameFromOmopVocabulary(routeConcept.getVocabularyId());
				if (!"None".equals(myUri)) {
					CodeableConcept routeCodeableConcept = new CodeableConcept();
					Coding routeCoding = new Coding();
					routeCoding.setSystem(myUri);
					routeCoding.setCode(routeConcept.getConceptCode());
					routeCoding.setDisplay(routeConcept.getConceptName());

					routeCodeableConcept.addCoding(routeCoding);
					dosage.setRoute(routeCodeableConcept);
				}
			} catch (FHIRException e) {
				e.printStackTrace();
			}
		}

		String sig = entity.getSig();
		if (sig != null && !sig.isEmpty()) {
			dosage.setText(sig);
		}

		if (!dosage.isEmpty())
			medicationStatement.addDosage(dosage);

		// Get information source
		Provider provider = entity.getProvider();
		if (provider != null) {
			Long fhirPractitionerId = IdMapping.getFHIRfromOMOP(provider.getId(), OmopPractitioner.FHIRTYPE);
			Reference infoSourceReference = new Reference(
					new IdType(OmopPractitioner.FHIRTYPE, fhirPractitionerId));
			if (provider.getProviderName() != null && !provider.getProviderName().isEmpty())
				infoSourceReference.setDisplay(provider.getProviderName());
			medicationStatement.setInformationSource(infoSourceReference);
		}

		// If OMOP medication type has the following prescription type, we set
		// basedOn reference to the prescription.
		if (entity.getDrugTypeConcept() != null) {
			if (entity.getDrugTypeConcept().getId() == OmopMedicationRequest.MEDICATIONREQUEST_CONCEPT_TYPE_ID) {
				IdType referenceIdType = new IdType(OmopMedicationRequest.FHIRTYPE, IdMapping.getFHIRfromOMOP(entity.getId(), OmopMedicationRequest.FHIRTYPE));
				Reference basedOnReference = new Reference(referenceIdType);
				medicationStatement.addBasedOn(basedOnReference);
			} else if (entity.getDrugTypeConcept().getId() == 38000179L
					|| entity.getDrugTypeConcept().getId() == 38000180L
					|| entity.getDrugTypeConcept().getId() == 43542356L
					|| entity.getDrugTypeConcept().getId() == 43542357L
					|| entity.getDrugTypeConcept().getId() == 43542358L
					|| entity.getDrugTypeConcept().getId() == 581373L) {
				// This is administration related...
				// TODO: add partOf to MedicationAdministration reference after we implement
				// Medication Administration
			} else if (entity.getDrugTypeConcept().getId() == 38000175L
					|| entity.getDrugTypeConcept().getId() == 38000176L
					|| entity.getDrugTypeConcept().getId() == 581452L) {
				// TODO: add partOf to MedicationDispense reference.
//				IdType referenceIdType = new IdType("MedicationDispense", IdMapping.getFHIRfromOMOP(entity.getId(), "MedicationDispense"));
//				medicationStatement.addPartOf(new Reference(referenceIdType));
			}

		}

		// If OMOP medicaiton type has the administration or dispense, we set
		// partOf reference to this.

		return medicationStatement;
	}

	@Override
	public List<ParameterWrapper> mapParameter(String parameter, Object value, boolean or) {
		List<ParameterWrapper> mapList = new ArrayList<ParameterWrapper>();
		ParameterWrapper paramWrapper = new ParameterWrapper();
		if (or)
			paramWrapper.setUpperRelationship("or");
		else
			paramWrapper.setUpperRelationship("and");

		switch (parameter) {
		case MedicationStatement.SP_RES_ID:
			String medicationStatementId = ((TokenParam) value).getValue();
			paramWrapper.setParameterType("Long");
			paramWrapper.setParameters(Arrays.asList("id"));
			paramWrapper.setOperators(Arrays.asList("="));
			paramWrapper.setValues(Arrays.asList(medicationStatementId));
			paramWrapper.setRelationship("or");
			mapList.add(paramWrapper);
			break;
		case MedicationStatement.SP_CODE:
			TokenParam theCode = (TokenParam) value;
			String system = theCode.getSystem();
			String code = theCode.getValue();
			String omopVocabulary = "None";

			if ((system == null || system.isEmpty()) && (code == null || code.isEmpty()))
				break;

			if (theCode.getModifier() != null && theCode.getModifier().compareTo(TokenParamModifier.IN) == 0) {
				// code has URI for the valueset search.
				TerminologyServiceClient terminologyService = TerminologyServiceClient.getInstance();
				Map<String, List<ConceptSetComponent>> theIncExcl = terminologyService.getValueSetByUrl(code);

				List<ConceptSetComponent> includes = theIncExcl.get("include");
				List<String> values = new ArrayList<String>();
				for (ConceptSetComponent include : includes) {
					// We need to loop
					ParameterWrapper myParamWrapper = new ParameterWrapper();
					myParamWrapper.setParameterType("Code:In");
					myParamWrapper.setParameters(Arrays.asList("drugConcept.vocabularyId", "drugConcept.conceptCode"));
					myParamWrapper.setOperators(Arrays.asList("=", "in"));

					String valueSetSystem = include.getSystem();
					try {
						omopVocabulary = OmopCodeableConceptMapping.omopVocabularyforFhirUri(valueSetSystem);
					} catch (FHIRException e) {
						e.printStackTrace();
					}
					if ("None".equals(omopVocabulary)) {
						ThrowFHIRExceptions.unprocessableEntityException(
								"We don't understand the system, " + valueSetSystem + " in code:in valueset");
					}
					values.add(valueSetSystem);

					List<ConceptReferenceComponent> concepts = include.getConcept();
					for (ConceptReferenceComponent concept : concepts) {
						String valueSetCode = concept.getCode();
						values.add(valueSetCode);
					}
					myParamWrapper.setValues(values);
					myParamWrapper.setUpperRelationship("or");
					mapList.add(myParamWrapper);
				}

				List<ConceptSetComponent> excludes = theIncExcl.get("exclude");
				for (ConceptSetComponent exclude : excludes) {
					// We need to loop
					ParameterWrapper myParamWrapper = new ParameterWrapper();
					myParamWrapper.setParameterType("Code:In");
					myParamWrapper.setParameters(Arrays.asList("drugConcept.vocabularyId", "drugConcept.conceptCode"));
					myParamWrapper.setOperators(Arrays.asList("=", "out"));

					String valueSetSystem = exclude.getSystem();
					try {
						omopVocabulary = OmopCodeableConceptMapping.omopVocabularyforFhirUri(valueSetSystem);
					} catch (FHIRException e) {
						e.printStackTrace();
					}
					if ("None".equals(omopVocabulary)) {
						ThrowFHIRExceptions.unprocessableEntityException(
								"We don't understand the system, " + valueSetSystem + " in code:in valueset");
					}
					values.add(valueSetSystem);

					List<ConceptReferenceComponent> concepts = exclude.getConcept();
					for (ConceptReferenceComponent concept : concepts) {
						String valueSetCode = concept.getCode();
						values.add(valueSetCode);
					}
					myParamWrapper.setValues(values);
					myParamWrapper.setUpperRelationship("and");
					mapList.add(myParamWrapper);
				}
			} else {
				if (system != null && !system.isEmpty()) {
					try {
						omopVocabulary = fhirOmopVocabularyMap.getOmopVocabularyFromFhirSystemName(system);
					} catch (FHIRException e) {
						e.printStackTrace();
					}
				}

				paramWrapper.setParameterType("String");
				if ("None".equals(omopVocabulary) && code != null && !code.isEmpty()) {
					paramWrapper.setParameters(Arrays.asList("drugConcept.conceptCode"));
					paramWrapper.setOperators(Arrays.asList("like"));
					paramWrapper.setValues(Arrays.asList(code));
				} else if (!"None".equals(omopVocabulary) && (code == null || code.isEmpty())) {
					paramWrapper.setParameters(Arrays.asList("drugConcept.vocabularyId"));
					paramWrapper.setOperators(Arrays.asList("like"));
					paramWrapper.setValues(Arrays.asList(omopVocabulary));
				} else {
					paramWrapper.setParameters(Arrays.asList("drugConcept.vocabularyId", "drugConcept.conceptCode"));
					paramWrapper.setOperators(Arrays.asList("like", "like"));
					paramWrapper.setValues(Arrays.asList(omopVocabulary, code));
				}
				paramWrapper.setRelationship("and");
				mapList.add(paramWrapper);
			}
			break;
		case MedicationStatement.SP_CONTEXT:
			Long fhirEncounterId = ((ReferenceParam) value).getIdPartAsLong();
			Long omopVisitOccurrenceId = IdMapping.getOMOPfromFHIR(fhirEncounterId,
					OmopEncounter.FHIRTYPE);
			// String resourceName = ((ReferenceParam) value).getResourceType();

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
		case MedicationStatement.SP_EFFECTIVE:
			DateRangeParam effectiveDateParam = ((DateRangeParam) value);
 			paramWrapper.setUpperRelationship("or"); // or these two maps
 			DateUtil.constructParameterWrapper(effectiveDateParam, "drugExposureStartDate", paramWrapper, mapList);
 			ParameterWrapper paramWrapper1 = new ParameterWrapper();
 			paramWrapper1.setUpperRelationship("or");
 			DateUtil.constructParameterWrapper(effectiveDateParam, "drugExposureEndDate", paramWrapper1, mapList);
			break;
		case "Patient:" + Patient.SP_RES_ID:
			addParamlistForPatientIDName(parameter, (String) value, paramWrapper, mapList);
			break;
		case "Patient:" + Patient.SP_NAME:
			addParamlistForPatientIDName(parameter, (String) value, paramWrapper, mapList);
			break;
		case "Patient:" + Patient.SP_IDENTIFIER:
			addParamlistForPatientIDName(parameter, (String) value, paramWrapper, mapList);
			break;
//		case MedicationStatement.SP_PATIENT:
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
		case MedicationStatement.SP_SOURCE:
			ReferenceParam sourceReference = ((ReferenceParam) value);
			String sourceReferenceId = String.valueOf(sourceReference.getIdPartAsLong());

			paramWrapper.setParameterType("Long");
			paramWrapper.setParameters(Arrays.asList("provider.id"));
			paramWrapper.setOperators(Arrays.asList("="));
			paramWrapper.setValues(Arrays.asList(sourceReferenceId));
			paramWrapper.setRelationship("or");
			mapList.add(paramWrapper);
			break;
		default:
			mapList = null;
		}

		return mapList;
	}

//	final ParameterWrapper filterParam = new ParameterWrapper("Long", Arrays.asList("drugTypeConcept.id"),
//			Arrays.asList("="), Arrays.asList(String.valueOf(MEDICATIONSTATEMENT_CONCEPT_TYPE_ID)), "or");

//	@Override
//	public Long getSize() {
//		List<ParameterWrapper> mapList = new ArrayList<ParameterWrapper>();
//		return getMyOmopService().getSize(mapList);
//	}
//
//	@Override
//	public Long getSize(List<ParameterWrapper> mapList) {
////		List<ParameterWrapper> exceptions = new ArrayList<ParameterWrapper>();
////		exceptions.add(filterParam);
////		map.put(MAP_EXCEPTION_FILTER, exceptions);
//
//		return getMyOmopService().getSize(mapList);
//	}

//	@Override
//	public void searchWithoutParams(int fromIndex, int toIndex, List<IBaseResource> listResources,
//			List<String> includes) {
//
//		// This is read all. But, since we will add an exception conditions to
//		// add filter.
//		// we will call the search with params method.
//		List<ParameterWrapper> mapList = new ArrayList<ParameterWrapper> ();
//		searchWithParams(fromIndex, toIndex, mapList, listResources, includes);
//	}
//
//	@Override
//	public void searchWithParams(int fromIndex, int toIndex, List<ParameterWrapper> mapList,
//			List<IBaseResource> listResources, List<String> includes) {
////		List<ParameterWrapper> exceptions = new ArrayList<ParameterWrapper>();
////		exceptions.add(filterParam);
////		map.put(MAP_EXCEPTION_FILTER, exceptions);
//
//		List<DrugExposure> entities = getMyOmopService().searchWithParams(fromIndex, toIndex, mapList);
//
//		for (DrugExposure entity : entities) {
//			Long omopId = entity.getIdAsLong();
//			Long fhirId = IdMapping.getFHIRfromOMOP(omopId, getMyFhirResourceType());
//			MedicationStatement fhirResource = constructResource(fhirId, entity, includes);
//			if (fhirResource != null) {
//				listResources.add(fhirResource);
//				// Do the rev_include and add the resource to the list.
//				addRevIncludes(omopId, includes, listResources);
//			}
//
//		}
//	}

	@Override
	public DrugExposure constructOmop(Long omopId, MedicationStatement fhirResource) {
		DrugExposure drugExposure = null;
		if (omopId != null) {
			// Update
			drugExposure = getMyOmopService().findById(omopId);
			if (drugExposure == null) {
				throw new FHIRException(fhirResource.getId() + " does not exist");
			}
		} else {
			// Create
			List<Identifier> identifiers = fhirResource.getIdentifier();
			for (Identifier identifier : identifiers) {
				if (!identifier.isEmpty()) {
					String identifierValue = identifier.getValue();
					List<DrugExposure> results = getMyOmopService().searchByColumnString("drugSourceValue",
							identifierValue);
					if (!results.isEmpty()) {
						drugExposure = results.get(0);
						// omopId = drugExposure.getId();
						break;
					}
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

		// context.
		Reference contextReference = fhirResource.getContext();
		if (contextReference != null && !contextReference.isEmpty()) {
			if (OmopEncounter.FHIRTYPE.equals(contextReference.getReferenceElement().getResourceType())) {
				Long encounterFhirIdLong = contextReference.getReferenceElement().getIdPartAsLong();
				if (encounterFhirIdLong != null) {
					Long visitOccurrenceId = IdMapping.getOMOPfromFHIR(encounterFhirIdLong,
							OmopEncounter.FHIRTYPE);
					// find the visit occurrence from OMOP database.
					VisitOccurrence newVisitOccurrence = visitOccurrenceService.findById(visitOccurrenceId);
					if (newVisitOccurrence != null) {
						drugExposure.setVisitOccurrence(newVisitOccurrence);
					} else {
						throw new FHIRException("Context Reference (Encounter/" + encounterFhirIdLong
								+ ") couldn't be found in our local database");
					}
				}
			}
		}

		MedicationStatementStatus status = fhirResource.getStatus();
		if (status != null && status.equals(MedicationStatementStatus.STOPPED)) {
			// This medication is stopped. See if we have a reason stopped.
			List<CodeableConcept> reasonNotTakens = fhirResource.getStatusReason();
//			TODO we may need to adjust this code block since we're no longer sure that this is only the reason not taken
			String reasonsForStopped = "";
			for (CodeableConcept reasonNotTaken : reasonNotTakens) {
				List<Coding> rNTCodings = reasonNotTaken.getCoding();
				for (Coding rNTCoding : rNTCodings) {
					String rNTCodingDisplay = rNTCoding.getDisplay();
					if (rNTCodingDisplay == null || rNTCodingDisplay.isEmpty()) {
						Concept rNTOmopConcept;
						try {
							rNTOmopConcept = CodeableConceptUtil.getOmopConceptWithFhirConcept(conceptService,
									rNTCoding);
							if (rNTOmopConcept != null) {
								reasonsForStopped = reasonsForStopped.concat(" " + rNTOmopConcept.getConceptName());
							}
						} catch (FHIRException e) {
							e.printStackTrace();
						}
					} else {
						reasonsForStopped = reasonsForStopped.concat(" " + rNTCodingDisplay);
					}
				}
			}

			// See if we have any reasons found. If so, put them in the stop
			// reason field.
			if (!"".equals(reasonsForStopped)) {
				reasonsForStopped = reasonsForStopped.trim().substring(0, 20);
				drugExposure.setStopReason(reasonsForStopped);
			}
		}

		// Get medication[x]
		Type medicationType = fhirResource.getMedication();
		Concept omopConcept = null;
		CodeableConcept medicationCodeableConcept = null;
		if (medicationType instanceof Reference) {
			// We may have reference.
			Reference medicationReference;
			try {
				medicationReference = fhirResource.getMedicationReference();
				if (medicationReference.isEmpty()) {
					// This is an error. We require this.
					throw new FHIRException("Medication[CodeableConcept or Reference] is missing");
				} else {
					String medicationReferenceId = medicationReference.getReferenceElement().getIdPart();
					if (medicationReference.getReferenceElement().isLocal()) {
						List<Resource> contains = fhirResource.getContained();
						for (Resource resource : contains) {
							if (!resource.isEmpty()
									&& resource.getIdElement().getIdPart().equals(medicationReferenceId)) {

								// This must medication resource.
								Medication medicationResource = (Medication) resource;
								medicationCodeableConcept = medicationResource.getCode();
								break;
							}
						}
					} else {
						throw new FHIRException("Medication Reference must have the medication in the contained");
					}
				}
			} catch (FHIRException e) {
				e.printStackTrace();
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

		omopConcept = CodeableConceptUtil.searchConcept(conceptService, medicationCodeableConcept);
		if (omopConcept == null) {
			throw new FHIRException("Medication[CodeableConcept or Reference] could not be found");
		} else {
			drugExposure.setDrugConcept(omopConcept);
		}

		// Effective Time.
		Type effective = fhirResource.getEffective();
		if (effective != null && !effective.isEmpty()) {
			if (effective instanceof DateTimeType) {
				// In OMOP on FHIR, we do Period. But,
				// if DateTime is provided, we set start time and end time
				Date date = ((DateTimeType) effective).getValue();
				drugExposure.setDrugExposureStartDate(date);
				drugExposure.setDrugExposureEndDate(date);
			} else if (effective instanceof Period) {
				Date startDate = ((Period) effective).getStart();
				Date endDate = ((Period) effective).getEnd();
				if (startDate == null) {
					throw new FHIRException("Effective start time cannot be empty");
				} else {
					drugExposure.setDrugExposureStartDate(startDate);
					drugExposure.setDrugExposureStartDateTime(startDate);
				}

				if (endDate != null) {
					drugExposure.setDrugExposureEndDate(endDate);
					drugExposure.setDrugExposureEndDateTime(endDate);
				} else {
					drugExposure.setDrugExposureStartDate(startDate);
					drugExposure.setDrugExposureStartDateTime(startDate);
				}
			}
		}

		// Information Source.
		Reference infoSourceReference = fhirResource.getInformationSource();
		if (infoSourceReference != null && !infoSourceReference.isEmpty()) {
			if (OmopPractitioner.FHIRTYPE.equals(infoSourceReference.getReferenceElement().getResourceType())) {
				Long practitionerIdLong = infoSourceReference.getReferenceElement().getIdPartAsLong();
				if (practitionerIdLong != null) {
					Long providerId = IdMapping.getOMOPfromFHIR(practitionerIdLong, OmopPractitioner.FHIRTYPE);
					if (providerId != null) {
						Provider provider = providerService.findById(providerId);
						if (provider == null) {
							try {
								throw new FHIRException(
										"Information Source (Practitioner/" + practitionerIdLong + ") does not exist");
							} catch (FHIRException e) {
								e.printStackTrace();
							}
						} else {
							drugExposure.setProvider(provider);
						}
					}
				}
			}
		}

		// Subject
		Reference subjectReference = fhirResource.getSubject();
		if (!subjectReference.isEmpty()) {
			if (OmopPatient.FHIRTYPE.equals(subjectReference.getReferenceElement().getResourceType())) {
				Long patientIdLong = subjectReference.getReferenceElement().getIdPartAsLong();
				if (patientIdLong != null) {
					Long fPersonId = IdMapping.getOMOPfromFHIR(patientIdLong, OmopPatient.FHIRTYPE);
					if (fPersonId != null) {
						FPerson fPerson = fPersonService.findById(fPersonId);
						if (fPerson != null) {
							drugExposure.setFPerson(fPerson);
						} else {
							throw new FHIRException("Subject (Patient/" + patientIdLong + ") does not exist");
						}
					} else {
						throw new FHIRException("Subject (Patient/" + patientIdLong + ") does not have ID mapping");
					}
				} else {
					throw new FHIRException(
							"Subject (Patient/" + patientIdLong + ") does not have Long part of ID");
				}
			}
		}

		// Dosage.
		List<Dosage> dosages = fhirResource.getDosage();
		Concept unitConcept = null;
		Concept routeConcept = null;
		for (Dosage dosage : dosages) {
			// We need quantity.
			Quantity qty=null;
			try {
				List<Dosage.DosageDoseAndRateComponent> dosesAndRates = dosage.getDoseAndRate();
				for(Dosage.DosageDoseAndRateComponent doseAndRate : dosesAndRates){
					if(doseAndRate.hasDoseQuantity()){
						qty=doseAndRate.getDoseQuantity();
					}
				}
				if (qty!=null && !qty.isEmpty()) {
					// get value
					BigDecimal value = qty.getValue();
					if (value != null) {
						drugExposure.setQuantity(value.doubleValue());
					}

					// get unit
					String system = qty.getSystem();
					String unit = qty.getUnit();
					String code = qty.getCode();
					if (unit != null && !unit.isEmpty())
						drugExposure.setDoseUnitSourceValue(unit);

					CodeableConcept routeFhirConcept = dosage.getRoute();
					routeConcept = CodeableConceptUtil.searchConcept(conceptService, routeFhirConcept);
					if (routeConcept != null) {
						drugExposure.setRouteConcept(routeConcept);
					}

					if (system != null && !system.isEmpty() && code != null && !code.isEmpty()) {
						String omopVocabularyId = fhirOmopVocabularyMap.getOmopVocabularyFromFhirSystemName(system);
						unitConcept = CodeableConceptUtil.getOmopConceptWithOmopVacabIdAndCode(conceptService,
								omopVocabularyId, code);
						if (unitConcept != null) {
							drugExposure.setDoseUnitSourceValue(unitConcept.getConceptCode());
							break;
						}
					}
				}
			} catch (FHIRException e) {
				e.printStackTrace();
			}
		}

		// Drug type concept should be hard-coded to MedicationStatement
		Concept drugTypeConcept = null;
		for (Reference basedOnReference : fhirResource.getBasedOn()) {
			if (basedOnReference.getReferenceElement().getResourceType().equals(OmopMedicationRequest.FHIRTYPE)) {
				drugTypeConcept = new Concept();
				drugTypeConcept.setId(OmopMedicationRequest.MEDICATIONREQUEST_CONCEPT_TYPE_ID);
			}
		}

		if (drugTypeConcept == null) {
			for (Reference partOfReference : fhirResource.getPartOf()) {
				if (partOfReference.getReferenceElement().getResourceType().equals("MedicationAdministration")) {
					drugTypeConcept = new Concept();
					drugTypeConcept.setId(38000179L);
				} else if (partOfReference.getReferenceElement().getResourceType().equals("MedicationDispense")) {
					drugTypeConcept = new Concept();
					drugTypeConcept.setId(38000175L);
				}
			}
		}

		if (drugTypeConcept == null) {
			drugTypeConcept = new Concept();
			drugTypeConcept.setId(MEDICATIONSTATEMENT_CONCEPT_TYPE_ID);
		}
		drugExposure.setDrugTypeConcept(drugTypeConcept);

		// Follows are default settings for OMOP's not null requirement
		if (drugExposure.getDrugConcept() == null) {
			drugExposure.setDrugConcept(new Concept(0L));
		}

		if (drugExposure.getDrugExposureStartDate() == null) {
			drugExposure.setDrugExposureStartDate(new Date());
		}

		if (drugExposure.getDrugExposureEndDate() == null) {
			drugExposure.setDrugExposureEndDate(new Date());
		}

		if (drugExposure.getDrugTypeConcept() == null) {
			drugExposure.setDrugTypeConcept(new Concept(0L));
		}

		if (drugExposure.getRouteConcept() == null) {
			drugExposure.setRouteConcept(new Concept(0L));
		}
		
		return drugExposure;
	}
}
