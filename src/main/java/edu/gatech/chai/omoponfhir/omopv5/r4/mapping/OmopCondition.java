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

import ca.uhn.fhir.rest.param.DateParam;
import ca.uhn.fhir.rest.param.ParamPrefixEnum;
import ca.uhn.fhir.rest.param.ReferenceParam;
import ca.uhn.fhir.rest.param.TokenParam;
import edu.gatech.chai.omoponfhir.omopv5.r4.utilities.CodeableConceptUtil;
import edu.gatech.chai.omopv5.dba.service.ConceptService;
import edu.gatech.chai.omopv5.dba.service.ConditionOccurrenceService;
import edu.gatech.chai.omopv5.dba.service.FPersonService;
import edu.gatech.chai.omopv5.dba.service.ParameterWrapper;
import edu.gatech.chai.omopv5.dba.service.ProviderService;
import edu.gatech.chai.omopv5.dba.service.VisitOccurrenceService;
import edu.gatech.chai.omopv5.model.entity.Concept;
import edu.gatech.chai.omopv5.model.entity.ConditionOccurrence;
import edu.gatech.chai.omopv5.model.entity.FPerson;
import edu.gatech.chai.omopv5.model.entity.Provider;
import edu.gatech.chai.omopv5.model.entity.VisitOccurrence;
import edu.gatech.chai.omoponfhir.omopv5.r4.provider.ConditionResourceProvider;
import edu.gatech.chai.omoponfhir.omopv5.r4.provider.EncounterResourceProvider;
import edu.gatech.chai.omoponfhir.omopv5.r4.provider.PatientResourceProvider;
import edu.gatech.chai.omoponfhir.omopv5.r4.provider.PractitionerResourceProvider;

import org.hl7.fhir.r4.model.*;
import org.hl7.fhir.r4.model.codesystems.ConditionCategory;
import org.hl7.fhir.exceptions.FHIRException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.context.ContextLoaderListener;
import org.springframework.web.context.WebApplicationContext;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class OmopCondition extends BaseOmopResource<Condition, ConditionOccurrence, ConditionOccurrenceService> {

	private static final Logger logger = LoggerFactory.getLogger(OmopCondition.class);

	private static OmopCondition omopCondition = new OmopCondition();

//	private ConditionOccurrenceService conditionOccurrenceService;
	private FPersonService fPersonService;
	private ProviderService providerService;
	private ConceptService conceptService;
	private VisitOccurrenceService visitOccurrenceService;

	public OmopCondition(WebApplicationContext context) {
		super(context, ConditionOccurrence.class, ConditionOccurrenceService.class,
				ConditionResourceProvider.getType());
		initialize(context);
		
		// Get count and put it in the counts.
		getSize();
	}

	public OmopCondition() {
		super(ContextLoaderListener.getCurrentWebApplicationContext(), ConditionOccurrence.class,
				ConditionOccurrenceService.class, ConditionResourceProvider.getType());
		initialize(ContextLoaderListener.getCurrentWebApplicationContext());
	}

	private void initialize(WebApplicationContext context) {
		// Get bean for other services that we need for mapping.
		if (context != null) {
	//		conditionOccurrenceService = context.getBean(ConditionOccurrenceService.class);
			fPersonService = context.getBean(FPersonService.class);
			providerService = context.getBean(ProviderService.class);
			conceptService = context.getBean(ConceptService.class);
			visitOccurrenceService = context.getBean(VisitOccurrenceService.class);
		} else {
			logger.error("context must be NOT null");
		}
	}

	public static OmopCondition getInstance() {
		return OmopCondition.omopCondition;
	}

	@Override
	public Condition constructFHIR(Long fhirId, ConditionOccurrence conditionOccurrence) {
		Condition condition = new Condition();
		condition.setId(new IdType(fhirId));

		addPersonToCondition(conditionOccurrence, condition);
		addCodeToCondition(conditionOccurrence, condition);
		addStartAndEndDateToCondition(conditionOccurrence, condition);
		addTypeToCondition(conditionOccurrence, condition);
		addAsserterToCondition(conditionOccurrence, condition);
		addContextToCondition(conditionOccurrence, condition);

		// TODO: Need to map the following
		// ??Condition.abatement.abatementString, but we are using abatementDateTime for
		// the end date and Abatement[x] has a 0..1 cardinality.
//		String stopReason = conditionOccurrence.getStopReason();
		// ??
//		String sourceValue = conditionOccurrence.getConditionSourceValue();
		// ??
//		Concept sourceConceptId = conditionOccurrence.getConditionSourceConcept();

		return condition;
	}

	@Override
	public Long toDbase(Condition fhirResource, IdType fhirId) throws FHIRException {
		Long retval;
		Long omopId = null, fhirIdLong = null;

		if (fhirId != null) {
			fhirIdLong = fhirId.getIdPartAsLong();
			if (fhirIdLong == null) {
				logger.error("Failed to get Condition.id as Long Value");
				return null;
			}
			
			omopId = IdMapping.getOMOPfromFHIR(fhirIdLong, ConditionResourceProvider.getType());
		}

		ConditionOccurrence conditionOccurrence = constructOmop(omopId, fhirResource);

		// TODO: Do you need to call other services to update links resources.

		if (conditionOccurrence.getId() != null) {
			retval = getMyOmopService().update(conditionOccurrence).getId();
		} else {
			retval = getMyOmopService().create(conditionOccurrence).getId();
		}

		return IdMapping.getFHIRfromOMOP(retval, ConditionResourceProvider.getType());
	}

	public List<ParameterWrapper> mapParameter(String parameter, Object value, boolean or) {
		List<ParameterWrapper> mapList = new ArrayList<ParameterWrapper>();
		ParameterWrapper paramWrapper = new ParameterWrapper();
		if (or)
			paramWrapper.setUpperRelationship("or");
		else
			paramWrapper.setUpperRelationship("and");

		switch (parameter) {
		case Condition.SP_ABATEMENT_AGE:
			// not supporting
			break;
//		case Condition.SP_ABATEMENT_BOOLEAN:
//			// not supporting
//			break;
		case Condition.SP_ABATEMENT_DATE:
			// Condition.abatementDate -> Omop ConditionOccurrence.conditionEndDate
			putDateInParamWrapper(paramWrapper, value, "conditionEndDate");
			mapList.add(paramWrapper);
			break;
		case Condition.SP_ABATEMENT_STRING:
			// not supporting
			break;
		case Condition.SP_RECORDED_DATE:
			// Condition.assertedDate -> Omop ConditionOccurrence.conditionStartDate
			putDateInParamWrapper(paramWrapper, value, "conditionStartDate");
			mapList.add(paramWrapper);
			break;
		case Condition.SP_ASSERTER:
			// Condition.asserter -> Omop Provider
			ReferenceParam patientReference = ((ReferenceParam) value);
			String patientId = String.valueOf(patientReference.getIdPartAsLong());

			paramWrapper.setParameterType("Long");
			paramWrapper.setParameters(Arrays.asList("provider.id"));
			paramWrapper.setOperators(Arrays.asList("="));
			paramWrapper.setValues(Arrays.asList(patientId));
			paramWrapper.setRelationship("or");
			mapList.add(paramWrapper);
			break;
		case Condition.SP_BODY_SITE:
			// not supporting
			break;
		case Condition.SP_CATEGORY:
			// Condition.category
			putConditionInParamWrapper(paramWrapper, value);
			mapList.add(paramWrapper);
			break;
		case Condition.SP_CLINICAL_STATUS:
			break;
		case Condition.SP_CODE:
			String system = ((TokenParam) value).getSystem();
			String code = ((TokenParam) value).getValue();
//    			System.out.println("\n\n\n\n\nSystem:"+system+"\n\ncode:"+code+"\n\n\n\n\n");
			if ((system == null || system.isEmpty()) && (code == null || code.isEmpty()))
				break;

			String omopVocabulary = "None";
			if (system != null && !system.isEmpty()) {
				try {
					omopVocabulary = OmopCodeableConceptMapping.omopVocabularyforFhirUri(system);
				} catch (FHIRException e) {
					e.printStackTrace();
				}
			}

			paramWrapper.setParameterType("String");
			if ("None".equals(omopVocabulary) && code != null && !code.isEmpty()) {
				paramWrapper.setParameters(Arrays.asList("conditionConcept.conceptCode"));
				paramWrapper.setOperators(Arrays.asList("="));
				paramWrapper.setValues(Arrays.asList(code));
			} else if (!"None".equals(omopVocabulary) && (code == null || code.isEmpty())) {
				paramWrapper.setParameters(Arrays.asList("conditionConcept.vocabularyId"));
				paramWrapper.setOperators(Arrays.asList("="));
				paramWrapper.setValues(Arrays.asList(omopVocabulary));
			} else {
				paramWrapper.setParameters(Arrays.asList("conditionConcept.vocabularyId", "conditionConcept.conceptCode"));
				paramWrapper.setOperators(Arrays.asList("=", "="));
				paramWrapper.setValues(Arrays.asList(omopVocabulary, code));
			}
			paramWrapper.setRelationship("and");
			mapList.add(paramWrapper);

//                //Condition.code -> Omop Concept
//                putConditionInParamWrapper(paramWrapper, value);
//                mapList.add(paramWrapper);

			break;
		case Condition.SP_ENCOUNTER:
			// Condition.context -> Omop VisitOccurrence
			ReferenceParam visitReference = (ReferenceParam) value;
			String visitId = String.valueOf(visitReference.getIdPartAsLong());
			paramWrapper.setParameterType("Long");
			paramWrapper.setParameters(Arrays.asList("visitOccurrence.id"));
			paramWrapper.setOperators(Arrays.asList("="));
			paramWrapper.setValues(Arrays.asList(visitId));
			paramWrapper.setRelationship("or");
			mapList.add(paramWrapper);
			break;
		case Condition.SP_EVIDENCE:
			// not supporting
			break;
		case Condition.SP_EVIDENCE_DETAIL:
			// not supporting
			break;
		case Condition.SP_IDENTIFIER:
			// not supporting
			break;
		case Condition.SP_ONSET_AGE:
			// not supporting
			break;
		case Condition.SP_ONSET_DATE:
			// not supporting
			break;
		case Condition.SP_ONSET_INFO:
			// not supporting
			break;
		case Condition.SP_PATIENT:
		case Condition.SP_SUBJECT:
			ReferenceParam subjectReference = ((ReferenceParam) value);
			Long fhirPatientId = subjectReference.getIdPartAsLong();
			Long omopPersonId = IdMapping.getOMOPfromFHIR(fhirPatientId, PatientResourceProvider.getType());

			String omopPersonIdString = String.valueOf(omopPersonId);

			paramWrapper.setParameterType("Long");
			paramWrapper.setParameters(Arrays.asList("fPerson.id"));
			paramWrapper.setOperators(Arrays.asList("="));
			paramWrapper.setValues(Arrays.asList(omopPersonIdString));
			paramWrapper.setRelationship("or");
			mapList.add(paramWrapper);
			break;
		case Procedure.SP_RES_ID:
			String conditionId = ((TokenParam) value).getValue();
			paramWrapper.setParameterType("Long");
			paramWrapper.setParameters(Arrays.asList("id"));
			paramWrapper.setOperators(Arrays.asList("="));
			paramWrapper.setValues(Arrays.asList(conditionId));
			paramWrapper.setRelationship("or");
			mapList.add(paramWrapper);
			break;
		case Condition.SP_SEVERITY:
			// not supporting
			break;
		case Condition.SP_STAGE:
			// not supporting
			break;
		case Condition.SP_VERIFICATION_STATUS:
			// not supporting
			break;
		default:
			mapList = null;
		}

		return mapList;
	}

	/* ====================================================================== */
	/* PRIVATE METHODS */
	/* ====================================================================== */

	private void putConditionInParamWrapper(ParameterWrapper paramWrapper, Object value) {
		String system = ((TokenParam) value).getSystem();
		String code = ((TokenParam) value).getValue();

		paramWrapper.setParameterType("String");
		paramWrapper.setParameters(Arrays.asList("concept.vocabularyId", "concept.conceptCode"));
		paramWrapper.setParameters(Arrays.asList("like", "like"));
		paramWrapper.setValues(Arrays.asList(system, code));
		paramWrapper.setRelationship("and");
	}

	private void putDateInParamWrapper(ParameterWrapper paramWrapper, Object value, String omopTableColumn) {
		DateParam dateParam = (DateParam) value;
		ParamPrefixEnum apiOperator = dateParam.getPrefix();
		String sqlOperator = null;
		if (apiOperator.equals(ParamPrefixEnum.GREATERTHAN)) {
			sqlOperator = ">";
		} else if (apiOperator.equals(ParamPrefixEnum.GREATERTHAN_OR_EQUALS)) {
			sqlOperator = ">=";
		} else if (apiOperator.equals(ParamPrefixEnum.LESSTHAN)) {
			sqlOperator = "<";
		} else if (apiOperator.equals(ParamPrefixEnum.LESSTHAN_OR_EQUALS)) {
			sqlOperator = "<=";
		} else if (apiOperator.equals(ParamPrefixEnum.NOT_EQUAL)) {
			sqlOperator = "!=";
		} else {
			sqlOperator = "=";
		}
		Date effectiveDate = dateParam.getValue();

		paramWrapper.setParameterType("Date");
		paramWrapper.setParameters(Arrays.asList(omopTableColumn));
		paramWrapper.setOperators(Arrays.asList(sqlOperator));
		paramWrapper.setValues(Arrays.asList(String.valueOf(effectiveDate.getTime())));
		paramWrapper.setRelationship("or");
	}

	private CodeableConcept retrieveCodeableConcept(Concept concept) {
		CodeableConcept conditionCodeableConcept = null;
		try {
			conditionCodeableConcept = CodeableConceptUtil.createFromConcept(concept);
		} catch (FHIRException fe) {
			logger.error("Could not generate CodeableConcept from Concept.", fe);
		}
		return conditionCodeableConcept;
	}

	private void addPersonToCondition(ConditionOccurrence conditionOccurrence, Condition condition) {
		// Condition.subject
		FPerson fPerson = conditionOccurrence.getFPerson();
		// set the person
		Reference subjectRef = new Reference(new IdType(PatientResourceProvider.getType(), fPerson.getId()));
		subjectRef.setDisplay(fPerson.getNameAsSingleString());
		condition.setSubject(subjectRef);
	}

	private void addCodeToCondition(ConditionOccurrence conditionOccurrence, Condition condition) {
		// Condition.code SNOMED-CT
		Concept conceptId = conditionOccurrence.getConditionConcept();
		if (conceptId != null) {
			CodeableConcept conditionCodeableConcept = retrieveCodeableConcept(conceptId);
			if (conditionCodeableConcept != null) {
				condition.setCode(conditionCodeableConcept);
			}
		}
	}

	private void addStartAndEndDateToCondition(ConditionOccurrence conditionOccurrence, Condition condition) {
		// Condition.onsetDateTime
		Date startDate = conditionOccurrence.getConditionStartDate();
		if (startDate != null) {
			DateTimeType onsetDateTime = new DateTimeType(startDate);
			condition.setOnset(onsetDateTime);
		}
		// Condition.abatementDateTime
		Date endDate = conditionOccurrence.getConditionEndDate();
		if (endDate != null) {
			DateTimeType abatementDateTime = new DateTimeType(endDate);
			condition.setAbatement(abatementDateTime);
		}
	}

	private void addTypeToCondition(ConditionOccurrence conditionOccurrence, Condition condition) {
		// Condition.category
		Concept typeConceptId = conditionOccurrence.getConditionTypeConcept();
		if (typeConceptId != null) {
			String systemUri = ConditionCategory.PROBLEMLISTITEM.getSystem();
			String code = null;
			
			String fhirCategoryCode = OmopConceptMapping.fhirForConditionTypeConcept(typeConceptId.getId());
			if (OmopConceptMapping.COND_NULL.fhirCode == fhirCategoryCode) {
				// We couldn't fine one. Default to problem-list
				code = ConditionCategory.PROBLEMLISTITEM.toCode(); // default
			} else {
				code = fhirCategoryCode;
			}
			
			Coding typeCoding = new Coding();
			typeCoding.setSystem(systemUri);
			typeCoding.setCode(code);
			CodeableConcept typeCodeableConcept = new CodeableConcept();
			typeCodeableConcept.addCoding(typeCoding);
			condition.addCategory(typeCodeableConcept);
//			if (typeCodeableConcept != null) {
//				List<CodeableConcept> typeList = new ArrayList<CodeableConcept>();
//				typeList.add(typeCodeableConcept);
//				condition.setCategory(typeList);
//			}
		}
	}

	private void addAsserterToCondition(ConditionOccurrence conditionOccurrence, Condition condition) {
		// Condition.asserter
		Provider provider = conditionOccurrence.getProvider();
		if (provider != null) {
			Reference providerRef = new Reference(new IdType(PractitionerResourceProvider.getType(), provider.getId()));
			providerRef.setDisplay(provider.getProviderName());
			condition.setAsserter(providerRef);
		}
	}

	private void addContextToCondition(ConditionOccurrence conditionOccurrence, Condition condition) {
		// Condition.context
		VisitOccurrence visitOccurrence = conditionOccurrence.getVisitOccurrence();
		if (visitOccurrence != null) {
			Reference visitRef = new Reference(
					new IdType(EncounterResourceProvider.getType(), visitOccurrence.getId()));
			condition.setEncounter(visitRef);
		}
	}

	@Override
	public ConditionOccurrence constructOmop(Long omopId, Condition fhirResource) {
		// things to update Condition_Occurrence, Concept, FPerson, Provider,
		// VisitOccurrence
		ConditionOccurrence conditionOccurrence;
		FPerson fPerson;
		Provider provider;

		// check for an existing condition
		if (omopId != null) {
			conditionOccurrence = getMyOmopService().findById(omopId);
		} else {
			conditionOccurrence = new ConditionOccurrence();
		}

		// get the Subject
		if (fhirResource.getSubject() != null) {
			Long subjectId = fhirResource.getSubject().getReferenceElement().getIdPartAsLong();
			Long subjectFhirId = IdMapping.getOMOPfromFHIR(subjectId, PatientResourceProvider.getType());
			fPerson = fPersonService.findById(subjectFhirId);
			if (fPerson == null) {
				try {
					throw new FHIRException("Could not get Person class.");
				} catch (FHIRException e) {
					e.printStackTrace();
				}
			}
			conditionOccurrence.setFPerson(fPerson);
		} else {
			// throw an error
			throw new FHIRException("FHIR Resource does not contain a Subject.");
		}

		// get the Provider
		if (fhirResource.getAsserter() != null && !fhirResource.getAsserter().isEmpty()) {
			Long providerId = fhirResource.getAsserter().getReferenceElement().getIdPartAsLong();
			Long providerOmopId = IdMapping.getOMOPfromFHIR(providerId, PractitionerResourceProvider.getType());
			provider = providerService.findById(providerOmopId);
			if (provider != null) {
				conditionOccurrence.setProvider(provider);
			}
		}

		// get the concept code
		CodeableConcept code = fhirResource.getCode();
		String valueSourceString = null;
		Concept concept = fhirCode2OmopConcept(conceptService, code, valueSourceString);
		conditionOccurrence.setConditionConcept(concept);

//		if (code != null) {
//			List<Coding> codes = code.getCoding();
//			Concept omopConcept;
//			// there is only one so get the first
//			try {
//				omopConcept = CodeableConceptUtil.getOmopConceptWithFhirConcept(conceptService, codes.get(0));
//				// set the concept
//				conditionOccurrence.setConceptId(omopConcept);
//			} catch (FHIRException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//		} else {
//			// is there a generic condition concept to use?
//			try {
//				throw new FHIRException("FHIR Resource does not contain a Condition Code.");
//			} catch (FHIRException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//		}
		
		// get the start and end date. We are expecting both to be of type DateTimeType
		Type onSet = fhirResource.getOnset();
		if (onSet instanceof DateTimeType) {
			Date start = ((DateTimeType) fhirResource.getOnset()).toCalendar().getTime();
			conditionOccurrence.setConditionStartDate(start);
			conditionOccurrence.setConditionStartDateTime(start);
		} else if (onSet instanceof Period) {
			Period period = (Period)onSet;
			Date start = period.getStart();
			Date end = period.getEnd();
			if (start != null) { 
				conditionOccurrence.setConditionStartDate(start);
				conditionOccurrence.setConditionStartDateTime(start);
			}
			if (end != null) conditionOccurrence.setConditionEndDate(end);
		} 

		if (fhirResource.getAbatement() instanceof DateTimeType) {
			conditionOccurrence.setConditionEndDate(((DateTimeType) fhirResource.getAbatement()).toCalendar().getTime());
		} else {
			// leave alone, end date not required
		}

		// set the category
		List<CodeableConcept> categories = fhirResource.getCategory();
		Long typeConceptId = 0L;
		for (CodeableConcept category : categories) {
			List<Coding> codings = category.getCoding();
			for (Coding coding : codings) {
				String fhirSystem = coding.getSystem();
				String fhirCode = coding.getCode();
				if (fhirSystem == null || fhirSystem.isEmpty() || fhirCode == null || fhirCode.isEmpty()) {
					continue;
				}
				try {
					typeConceptId = OmopConceptMapping.omopForConditionCategoryCode(fhirCode);
				} catch (FHIRException e) {
					e.printStackTrace();
				}
				if (typeConceptId > 0L)
					break;
			}
			if (typeConceptId > 0L)
				break;
		}

		concept = conceptService.findById(typeConceptId);
		conditionOccurrence.setConditionTypeConcept(concept);

		// set the context
		/* Set visit occurrence */
		Reference contextReference = fhirResource.getEncounter();
		VisitOccurrence visitOccurrence = fhirContext2OmopVisitOccurrence(visitOccurrenceService, contextReference);
		if (visitOccurrence != null) {
			conditionOccurrence.setVisitOccurrence(visitOccurrence);
		}
		
		// non-null handler
		if (conditionOccurrence.getConditionConcept() == null) {
			conditionOccurrence.setConditionConcept(new Concept(0L));
		}

		if (conditionOccurrence.getConditionTypeConcept() == null) {
			conditionOccurrence.setConditionTypeConcept(new Concept(0L));
		}

		if (conditionOccurrence.getConditionSourceConcept() == null) {
			conditionOccurrence.setConditionSourceConcept(new Concept(0L));
		}
		
		return conditionOccurrence;
	}
}
