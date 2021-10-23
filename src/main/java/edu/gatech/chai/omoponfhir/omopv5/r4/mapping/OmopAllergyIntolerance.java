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

import org.hl7.fhir.r4.model.AllergyIntolerance;
import org.hl7.fhir.r4.model.AllergyIntolerance.AllergyIntoleranceCategory;
import org.hl7.fhir.r4.model.AllergyIntolerance.AllergyIntoleranceType;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.DateTimeType;
import org.hl7.fhir.r4.model.Enumeration;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.instance.model.api.IBaseResource;

import ca.uhn.fhir.rest.api.SortSpec;
import ca.uhn.fhir.rest.param.DateRangeParam;
import ca.uhn.fhir.rest.param.ReferenceParam;
import ca.uhn.fhir.rest.param.TokenParam;
import edu.gatech.chai.omoponfhir.omopv5.r4.provider.AllergyIntoleranceResourceProvider;
import edu.gatech.chai.omoponfhir.omopv5.r4.provider.PatientResourceProvider;
import edu.gatech.chai.omoponfhir.omopv5.r4.provider.PractitionerResourceProvider;
import edu.gatech.chai.omoponfhir.omopv5.r4.utilities.CodeableConceptUtil;
import edu.gatech.chai.omoponfhir.omopv5.r4.utilities.DateUtil;
import edu.gatech.chai.omoponfhir.omopv5.r4.utilities.ExtensionUtil;
import edu.gatech.chai.omopv5.dba.service.*;
import edu.gatech.chai.omopv5.model.entity.*;
import edu.gatech.chai.omopv5.model.entity.Observation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.context.ContextLoader;
import org.springframework.web.context.WebApplicationContext;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class OmopAllergyIntolerance extends BaseOmopResource<AllergyIntolerance, Observation, ObservationService> {

	private static final Logger logger = LoggerFactory.getLogger(OmopAllergyIntolerance.class);

	private static OmopAllergyIntolerance omopAllergyIntolerance = new OmopAllergyIntolerance();

	private FPersonService fPersonService;
	private ProviderService providerService;
	private ConceptService conceptService;

	public OmopAllergyIntolerance(WebApplicationContext context) {
		super(context, Observation.class, ObservationService.class,
				AllergyIntoleranceResourceProvider.getType());
		initialize(context);
	}

	public OmopAllergyIntolerance() {
		super(ContextLoader.getCurrentWebApplicationContext(), Observation.class,
				ObservationService.class, AllergyIntoleranceResourceProvider.getType());
		initialize(ContextLoader.getCurrentWebApplicationContext());
		
		getSize();
	}

	private void initialize(WebApplicationContext context) {
		// Get bean for other services that we need for mapping.
		if (context != null) {
			fPersonService = context.getBean(FPersonService.class);
			providerService = context.getBean(ProviderService.class);
			conceptService = context.getBean(ConceptService.class);
		} else {
			logger.error("context must be NOT null");
		}
	}

	public static OmopAllergyIntolerance getInstance() {
		return OmopAllergyIntolerance.omopAllergyIntolerance;
	}

	@Override
	public String constructOrderParams(SortSpec theSort) {
		if (theSort == null) return null;

		String direction;

		if (theSort.getOrder() != null) direction = theSort.getOrder().toString();
		else direction = "ASC";

		String orderParam = new String();

		if (theSort.getParamName().equals(AllergyIntolerance.SP_CODE)) {
			orderParam = "observationConcept.conceptCode " + direction;
		} else if (theSort.getParamName().equals(AllergyIntolerance.SP_DATE)) {
			orderParam = "observationDate " + direction;
		} else if (theSort.getParamName().equals(AllergyIntolerance.SP_PATIENT)) {
			orderParam = "fPerson.id " + direction;
		} else {
			orderParam = "id " + direction;
		}

		String orderParams = orderParam;

		if (theSort.getChain() != null) {
			orderParams = orderParams.concat(","+constructOrderParams(theSort.getChain()));
		}

		return orderParams;
	}

	@Override
	public AllergyIntolerance constructFHIR(Long fhirId, Observation observation) {
		AllergyIntolerance allergyIntolerance = new AllergyIntolerance();
		allergyIntolerance.setId(new IdType(fhirId));

		FPerson fPerson = observation.getFPerson();
		
		// set the Patient
		Reference subjectRef = new Reference(new IdType(PatientResourceProvider.getType(), fPerson.getId()));
		subjectRef.setDisplay(fPerson.getNameAsSingleString());
		allergyIntolerance.setPatient(subjectRef);

		// set the onset date
		Date observationDate = observation.getObservationDate();
		DateTimeType recordedDateTime = new DateTimeType(observationDate);
		allergyIntolerance.setOnset(recordedDateTime);

		// set the recorder
		Provider provider = observation.getProvider();
		if (provider != null && provider.getId() != 0L) {
			Reference providerRef = new Reference(new IdType(PractitionerResourceProvider.getType(), provider.getId()));
			allergyIntolerance.setRecorder(providerRef);
		}
		
		// set substance
		AllergyIntoleranceCategory categoryValue = null;
		Concept observationValueConcept = observation.getValueAsConcept();
		Concept observationConcept = observation.getObservationConcept();

		if (observationValueConcept == null || observationValueConcept.getId() == 0L) {
			if (observationConcept != null && observationConcept.getId() != 0L) {
				CodeableConcept substanceCodeableConcept = CodeableConceptUtil.getCodeableConceptFromOmopConcept(observationConcept, getFhirOmopVocabularyMap());
				allergyIntolerance.setCode(substanceCodeableConcept);
			}
		} else {
			CodeableConcept substanceCodeableConcept = CodeableConceptUtil.getCodeableConceptFromOmopConcept(observationValueConcept, getFhirOmopVocabularyMap());
			allergyIntolerance.setCode(substanceCodeableConcept);
		}
		
		if (observationConcept != null && 
				(observationConcept.getId() == 439224L) ||
				(observationConcept.getId() == 4166257L) ||
				(observationConcept.getId() == 4297808L) ||
				(observationConcept.getId() == 4299541L) ||
				(observationConcept.getId() == 4165345L) ||
				(observationConcept.getId() == 37017420L) ||
				(observationConcept.getId() == 4164867L) ||
				(observationConcept.getId() == 4171468L)) {
			categoryValue = AllergyIntoleranceCategory.MEDICATION;
		} else {
			if (observationConcept != null 
					&& observationConcept.getConceptName().contains("Allerg")
					&& observationConcept.getConceptName().contains("food")) {
				categoryValue = AllergyIntoleranceCategory.FOOD;
			}
		}
		
		if (categoryValue != null) {
			allergyIntolerance.addCategory(categoryValue);
		}
		
		allergyIntolerance.setType(AllergyIntoleranceType.ALLERGY);
		
		return allergyIntolerance;
	}

	@Override
	public Long toDbase(AllergyIntolerance fhirResource, IdType fhirId) throws FHIRException {
		Long retval;
		Long omopId = null;

		if (fhirId != null) {
			omopId = fhirId.getIdPartAsLong();
			if (omopId == null) {
				logger.error("Failed to get Condition.id as Long Value");
				return null;
			}
		}

		Observation observation = constructOmop(omopId, fhirResource);

		if (observation.getId() != null) {
			retval = getMyOmopService().update(observation).getId();
		} else {
			retval = getMyOmopService().create(observation).getId();
		}

		return retval;
	}

	final List<ParameterWrapper> filterParams = Arrays.asList( 
			new ParameterWrapper("String", Arrays.asList("observationConcept.conceptName"),	Arrays.asList("like"), Arrays.asList("%Allerg%"), "or"), 
			new ParameterWrapper("String", Arrays.asList("observationConcept.domainId"),	Arrays.asList("="), Arrays.asList("Observation"), "or"));

	@Override
	public Long getSize() {
		List<ParameterWrapper> paramList = new ArrayList<ParameterWrapper>();
		// call getSize with empty parameter list. The getSize will add filter
		// parameter.
		Long size = getSize(paramList);

		ExtensionUtil.addResourceCount(getMyFhirResourceType(), size);

		return size;
	}

	@Override
	public Long getSize(List<ParameterWrapper> paramList) {
		paramList.addAll(filterParams);

		return getMyOmopService().getSize(paramList);
	}

	@Override
	public void searchWithoutParams(int fromIndex, int toIndex, List<IBaseResource> listResources,
			List<String> includes, String sort) {

		// This is read all. But, since we will add an exception conditions to add
		// filter.
		// we will call the search with params method.
		List<ParameterWrapper> paramList = new ArrayList<ParameterWrapper>();
		searchWithParams(fromIndex, toIndex, paramList, listResources, includes, sort);
	}

	@Override
	public void searchWithParams(int fromIndex, int toIndex, List<ParameterWrapper> mapList,
			List<IBaseResource> listResources, List<String> includes, String sort) {
		mapList.addAll(filterParams);

		List<Observation> entities = getMyOmopService().searchWithParams(fromIndex, toIndex, mapList, sort);

		for (Observation entity : entities) {
			Long fhirId = entity.getIdAsLong();
			AllergyIntolerance fhirResource = constructResource(fhirId, entity, includes);
			if (fhirResource != null) {
				listResources.add(fhirResource);
				// Do the rev_include and add the resource to the list.
				addRevIncludes(fhirId, includes, listResources);
			}

		}
	}

	
	public List<ParameterWrapper> mapParameter(String parameter, Object value, boolean or) {
		List<ParameterWrapper> mapList = new ArrayList<ParameterWrapper>();
		ParameterWrapper paramWrapper = new ParameterWrapper();
		if (or)
			paramWrapper.setUpperRelationship("or");
		else
			paramWrapper.setUpperRelationship("and");

		switch (parameter) {
			case AllergyIntolerance.SP_RECORDER:
				// Condition.asserter -> Omop Provider
				ReferenceParam providerReference = ((ReferenceParam) value);
				String providerId = String.valueOf(providerReference.getIdPartAsLong());

				paramWrapper.setParameterType("Long");
				paramWrapper.setParameters(Arrays.asList("provider.id"));
				paramWrapper.setOperators(Arrays.asList("="));
				paramWrapper.setValues(Arrays.asList(providerId));
				paramWrapper.setRelationship("or");
				mapList.add(paramWrapper);
				break;
			case AllergyIntolerance.SP_CODE:
				String system = ((TokenParam) value).getSystem();
				String code = ((TokenParam) value).getValue();

				if ((system == null || system.isEmpty()) && (code == null || code.isEmpty()))
					break;

				String omopVocabulary = "None";
				if (system != null && !system.isEmpty()) {
					try {
						omopVocabulary = getFhirOmopVocabularyMap().getOmopVocabularyFromFhirSystemName(system);
					} catch (FHIRException e) {
						e.printStackTrace();
					}
				}

				paramWrapper.setParameterType("String");
				if ("None".equals(omopVocabulary) && code != null && !code.isEmpty()) {
					paramWrapper.setParameters(Arrays.asList("observationConcept.conceptCode"));
					paramWrapper.setOperators(Arrays.asList("="));
					paramWrapper.setValues(Arrays.asList(code));
				} else if (!"None".equals(omopVocabulary) && (code == null || code.isEmpty())) {
					paramWrapper.setParameters(Arrays.asList("observationConcept.vocabularyId"));
					paramWrapper.setOperators(Arrays.asList("="));
					paramWrapper.setValues(Arrays.asList(omopVocabulary));
				} else {
					paramWrapper.setParameters(Arrays.asList("observationConcept.vocabularyId", "observationConcept.conceptCode"));
					paramWrapper.setOperators(Arrays.asList("=", "="));
					paramWrapper.setValues(Arrays.asList(omopVocabulary, code));
				}
				paramWrapper.setRelationship("and");
				mapList.add(paramWrapper);
				
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
			case AllergyIntolerance.SP_RES_ID:
				String allergyIntoleranceId = ((TokenParam) value).getValue();
				paramWrapper.setParameterType("Long");
				paramWrapper.setParameters(Arrays.asList("id"));
				paramWrapper.setOperators(Arrays.asList("="));
				paramWrapper.setValues(Arrays.asList(allergyIntoleranceId));
				paramWrapper.setRelationship("or");
				mapList.add(paramWrapper);
				break;
			case AllergyIntolerance.SP_ONSET:
				DateRangeParam dateRangeParam = ((DateRangeParam) value);
				DateUtil.constructParameterWrapper(dateRangeParam, "observationDate", paramWrapper, mapList);
				break;
			default:
				mapList = null;
		}

		return mapList;
	}

	@Override
	public Observation constructOmop(Long omopId, AllergyIntolerance fhirResource) {
		// things to update Condition_Occurrence, Concept, FPerson, Provider,
		// VisitOccurrence
		Observation observation;
		FPerson fPerson;
		Provider provider;

		// check for an existing condition
		if (omopId != null) {
			observation = getMyOmopService().findById(omopId);
		} else {
			observation = new Observation();
		}

		// set the person
		if (fhirResource.getPatient() != null) {
			Long subjectId = fhirResource.getPatient().getReferenceElement().getIdPartAsLong();
			Long subjectFhirId = subjectId;
			fPerson = fPersonService.findById(subjectFhirId);
			if (fPerson == null) {
				try {
					throw new FHIRException("Could not get Person class.");
				} catch (FHIRException e) {
					e.printStackTrace();
				}
			}
			observation.setFPerson(fPerson);
		} else {
			// throw an error
			try {
				throw new FHIRException("FHIR Resource does not contain a Subject.");
			} catch (FHIRException e) {
				e.printStackTrace();
			}
		}
		
		// set the date.
		if (fhirResource.getOnsetDateTimeType() != null) {
			observation.setObservationDate(fhirResource.getOnsetDateTimeType().getValue());
		}

		// set the provider
		if (fhirResource.getRecorder() != null && !fhirResource.getRecorder().isEmpty()) {
			Long providerId = fhirResource.getRecorder().getReferenceElement().getIdPartAsLong();
			provider = providerService.findById(providerId);
			if (provider != null) {
				observation.setProvider(provider);
			}
		}

		// set value as Concept
		CodeableConcept code = fhirResource.getCode();
		String valueSourceString = null;
		Concept substanceConcept = fhirCode2OmopConcept(conceptService, code, valueSourceString);
		observation.setValueAsConcept(substanceConcept);
		observation.setValueAsString(valueSourceString);

		// set concept code
		if (fhirResource.getCategory() != null) {
			boolean matched = false;
			for (Enumeration<AllergyIntoleranceCategory> category : fhirResource.getCategory()) {
				if (category.getValue() == AllergyIntoleranceCategory.FOOD) {
					observation.setObservationConcept(new Concept(4188027L));
					matched = true;
				} else if (category.getValue() == AllergyIntoleranceCategory.MEDICATION) {
					observation.setObservationConcept(new Concept(439224L));
					matched = true;
				}
			}
			
			if (matched == false) {				
				observation.setObservationConcept(new Concept(40772948L));
			}
			
			
		} else {
			if ("Drug".equals(substanceConcept.getDomainId())) {
				observation.setObservationConcept(new Concept(439224L));
			} else {
				observation.setObservationConcept(new Concept(40772948L));
			}
		}
		
		// get the start and end date. We are expecting both to be of type DateTimeType
		DateTimeType onSetDateTime = fhirResource.getOnsetDateTimeType();
		if (onSetDateTime != null) {
			observation.setObservationDate(onSetDateTime.getValue());
		} 

		// set type concept - fixed value
		observation.setObservationTypeConcept(new Concept(38000280L));

		return observation;
	}
}