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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.DateTimeType;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Quantity;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Specimen;
import org.hl7.fhir.r4.model.Type;
import org.hl7.fhir.r4.model.Specimen.SpecimenCollectionComponent;
import org.hl7.fhir.r4.model.Specimen.SpecimenStatus;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.context.ContextLoaderListener;
import org.springframework.web.context.WebApplicationContext;

import ca.uhn.fhir.rest.api.SortSpec;
import ca.uhn.fhir.rest.param.DateParam;
import ca.uhn.fhir.rest.param.DateRangeParam;
import ca.uhn.fhir.rest.param.ParamPrefixEnum;
import ca.uhn.fhir.rest.param.TokenParam;
import edu.gatech.chai.omoponfhir.omopv5.r4.utilities.CodeableConceptUtil;
import edu.gatech.chai.omoponfhir.omopv5.r4.utilities.DateUtil;
import edu.gatech.chai.omoponfhir.omopv5.r4.utilities.ExtensionUtil;
import edu.gatech.chai.omopv5.dba.service.ConceptService;
import edu.gatech.chai.omopv5.dba.service.ParameterWrapper;
import edu.gatech.chai.omopv5.dba.service.SpecimenService;
import edu.gatech.chai.omopv5.model.entity.Concept;
import edu.gatech.chai.omopv5.model.entity.FPerson;

public class OmopSpecimen extends BaseOmopResource<Specimen, edu.gatech.chai.omopv5.model.entity.Specimen, SpecimenService> {

	static final Logger logger = LoggerFactory.getLogger(OmopSpecimen.class);
	private static OmopSpecimen omopSpecimen = new OmopSpecimen();

	private ConceptService conceptService;

	public OmopSpecimen(WebApplicationContext context) {
		super(context, edu.gatech.chai.omopv5.model.entity.Specimen.class, SpecimenService.class, OmopSpecimen.FHIRTYPE);
		initialize(context);

		// Get count and put it in the counts.
		getSize(true);
	}

	public OmopSpecimen() {
		super(ContextLoaderListener.getCurrentWebApplicationContext(), edu.gatech.chai.omopv5.model.entity.Specimen.class,
				SpecimenService.class, OmopSpecimen.FHIRTYPE);
		initialize(ContextLoaderListener.getCurrentWebApplicationContext());
	}

	private void initialize(WebApplicationContext context) {
		// Get bean for other services that we need for mapping.
		conceptService = context.getBean(ConceptService.class);
	}

	public static OmopSpecimen getInstance() {
		return OmopSpecimen.omopSpecimen;
	}

	public static String FHIRTYPE = "Specimen";

	@Override
	public Specimen constructFHIR(Long fhirId, edu.gatech.chai.omopv5.model.entity.Specimen specimen_) {
		Specimen specimen = new Specimen();

		specimen.setId(new IdType(fhirId));

		// we just hard-code the status to available as the OMOP data is usually captured and available.
		specimen.setStatus(SpecimenStatus.AVAILABLE);

		// FHIR Specimen type is recommended to be from V2. However, OMOP has this in SNOMED CT. So,
		// We use it. If we need to use V2, then we need to map this to V2...
		// Note: FHIR Specimen Type is OMOP Specimen Concept. OMOP Specimen Type is a source of Specimen record,
		//       which we can't map to FHIR.... This is a gap.
		Concept specimenConcept = specimen_.getSpecimenConcept();
		String omopVocabulary = specimenConcept.getVocabularyId();
		String specimentTypeSystemUri = fhirOmopVocabularyMap.getFhirSystemNameFromOmopVocabulary(omopVocabulary);
		if ("None".equals(specimentTypeSystemUri)) {
			// If we can't find FHIR Uri or system name from the Vocabulary ID, we just use Omop Vocabulary Id.
			specimentTypeSystemUri = omopVocabulary;
		}
		String specimentTypeCode = specimenConcept.getConceptCode();
		String specimentTypeDisplay = specimenConcept.getConceptName();
		specimen.setType(new CodeableConcept(new Coding(specimentTypeSystemUri, specimentTypeCode, specimentTypeDisplay)));

		if (specimen_.getFPerson() != null) {
			Reference personRef = new Reference(new IdType(OmopPatient.FHIRTYPE, specimen_.getFPerson().getId()));
			personRef.setDisplay(specimen_.getFPerson().getNameAsSingleString());
			specimen.setSubject(personRef);
		}

		// Collection mapping.
		// OMOP only has date/datetime, not period for the collection time. 
		SpecimenCollectionComponent collection = specimen.getCollection();
		if (specimen_.getSpecimenDate() != null) {
			Date myDate = createDateTime(specimen_);
			if (myDate != null) {
				DateTimeType collectedDate = new DateTimeType(myDate);
				collection.setCollected(collectedDate);
			}
		}

		// Quantity
		if (specimen_.getQuantity() != null) {
			collection.setQuantity(new Quantity(specimen_.getQuantity()));
		}

		// If we have unit, this should be used across all the value.
		String unitSystemUri = null;
		String unitCode = null;
		String unitUnit = null;
		String unitSource = null;
		Concept unitConcept = specimen_.getUnitConcept();
		if (unitConcept == null || unitConcept.getId() == 0L) {
			// see if we can get the unit from source column.
			unitSource = specimen_.getUnitSourceValue();
			if (unitSource != null && !unitSource.isEmpty()) {
				unitUnit = unitSource;
				unitConcept = CodeableConceptUtil.getOmopConceptWithOmopVacabIdAndCode(conceptService, OmopCodeableConceptMapping.UCUM.getOmopVocabulary(), unitSource);
			}
		}
		
		if (unitConcept != null && unitConcept.getId() != 0L) {
			String omopUnitVocabularyId = unitConcept.getVocabularyId();
			unitSystemUri = fhirOmopVocabularyMap.getFhirSystemNameFromOmopVocabulary(omopUnitVocabularyId);
			if ("None".equals(unitSystemUri)) {
				unitSystemUri = omopUnitVocabularyId;
			}

			unitUnit = unitConcept.getConceptName();
			unitCode = unitConcept.getConceptCode();
		}

		if (unitSystemUri != null && !unitSystemUri.isBlank()) {
			collection.getQuantity().setSystem(unitSystemUri);
		}

		if (unitUnit != null && !unitUnit.isBlank()) {
			collection.getQuantity().setUnit(unitUnit);
		}

		if (unitCode != null && !unitCode.isBlank()) {
			collection.getQuantity().setCode(unitCode);
		}
		
		if (specimen_.getAnatomicSiteConcept() != null && specimen_.getAnatomicSiteConcept().getId() != 0L) {
			String anatomicSiteVocabularyId = specimen_.getAnatomicSiteConcept().getVocabularyId();
			String anatomicSiteSystemUri = fhirOmopVocabularyMap.getFhirSystemNameFromOmopVocabulary(anatomicSiteVocabularyId);
			if ("None".equals(anatomicSiteSystemUri)) {
				anatomicSiteSystemUri = anatomicSiteVocabularyId;
			}

			String anatomicSiteCode = specimen_.getAnatomicSiteConcept().getConceptCode();
			String anatomicSiteDisplay = specimen_.getAnatomicSiteConcept().getConceptName();

			collection.setBodySite(new CodeableConcept(new Coding(anatomicSiteSystemUri, anatomicSiteCode, anatomicSiteDisplay)));
		} else if (specimen_.getAnatomicSiteSourceValue() != null && !specimen_.getAnatomicSiteSourceValue().isBlank()) {
			collection.setBodySite((new CodeableConcept()).setText(specimen_.getAnatomicSiteSourceValue()));
		}
		
		if (specimen_.getSpecimenSourceId() != null && !specimen_.getSpecimenSourceId().isBlank()) {
			specimen.addIdentifier((new Identifier()).setValue(specimen_.getSpecimenSourceId()));
		}

		return specimen;
	}

	@Override
	public Long removeByFhirId(IdType fhirId) {
		Long id_long_part = fhirId.getIdPartAsLong();
		Long myId = IdMapping.getOMOPfromFHIR(id_long_part, getMyFhirResourceType());

		return getMyOmopService().removeById(myId);
	}
	
	@Override
	public String constructOrderParams(SortSpec theSort) {
		if (theSort == null) return "id ASC";
		
		String direction;
		
		if (theSort.getOrder() != null) direction = theSort.getOrder().toString();
		else direction = "ASC";

		String orderParam = new String(); 
		
		if (theSort.getParamName().equals(Specimen.SP_TYPE)) {
			orderParam = "specimenConcept.concept_code " + direction;
		} else if (theSort.getParamName().equals(Specimen.SP_COLLECTED)) {
			orderParam = "specimenDate " + direction;
		} else if (theSort.getParamName().equals(Specimen.SP_PATIENT) 
				|| theSort.getParamName().equals(Specimen.SP_SUBJECT)) {
			orderParam = "fPerson " + direction;
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
	public edu.gatech.chai.omopv5.model.entity.Specimen constructOmop(Long omopId, Specimen fhirResource) {
		edu.gatech.chai.omopv5.model.entity.Specimen specimen_ = null;
		if (omopId == null) {
			// This is CREATE.
			specimen_ = new edu.gatech.chai.omopv5.model.entity.Specimen();
		} else {
			specimen_ = getMyOmopService().findById(omopId);
			if (specimen_ == null) {
				// We have no Specimen to update.
				throw new FHIRException("We have no matching FHIR Specimen to update.");
			}
		}

		// FHIR Specimen.identifier --> OMOP Specimen.specimen_source_id
		Identifier identifier = fhirResource.getIdentifierFirstRep();
		if (identifier != null && !identifier.isEmpty()) {
			// Specimen Source Id is identifier from the source system.
			specimen_.setSpecimenSourceId(identifier.getValue());
		}

		// FHIR Specimen.type --> OMOP Specimen.specimen_concept_id
		CodeableConcept code = fhirResource.getType();

		// code should NOT be null as this is required field.
		// And, validation should check this.
		List<Coding> codings = code.getCoding();
		Coding codingFound = null;
		Coding codingSecondChoice = null;
		String OmopSystem = null;
		String valueSourceString = null;
		for (Coding coding : codings) {
			String fhirSystemUri = coding.getSystem();

			if (code.getText() != null && !code.getText().isEmpty()) {
				valueSourceString = code.getText();
			} else {
				valueSourceString = codingInString(coding, 50);
			}

			if (fhirSystemUri.equals(OmopCodeableConceptMapping.SCT.getFhirUri())) {
				// Found the code we want, which is LOINC
				codingFound = coding;
				break;
			} else {
				// See if we can handle this coding.
				try {
					if (fhirSystemUri != null && !fhirSystemUri.isEmpty()) {
						OmopSystem = fhirOmopVocabularyMap.getOmopVocabularyFromFhirSystemName(fhirSystemUri);
						if ("None".equals(OmopSystem) == false) {
							// We can at least handle this. Save it
							// We may find another one we can handle. Let it replace.
							// 2nd choice is just 2nd choice.
							codingSecondChoice = coding;
						}
					}
				} catch (FHIRException e) {
					e.printStackTrace();
				}
			}
		}

		Concept concept = null;
		if (codingFound != null) {
			// Find the concept id for this coding.
			concept = CodeableConceptUtil.getOmopConceptWithOmopVacabIdAndCode(conceptService,
					OmopCodeableConceptMapping.LOINC.getOmopVocabulary(), codingFound.getCode());
		}
		if (codingSecondChoice != null) {
			// This is not our first choice. But, found one that we can
			// map.
			concept = CodeableConceptUtil.getOmopConceptWithOmopVacabIdAndCode(conceptService, OmopSystem,
					codingSecondChoice.getCode());
		} 

		if (concept == null) {
			concept = conceptService.findById(0L);
		}

		specimen_.setSpecimenConcept(concept);

		// Set this in the source column
		// FHIR Specimen.type --> OMOP Specimen.specimen_source_value
		if (concept == null || concept.getIdAsLong() == 0L) {
			specimen_.setSpecimenSourceValue(valueSourceString);
		}

		// FHIR Specimen.subject --> OMOP Specimen.person_id
		Long fhirSubjectId = fhirResource.getSubject().getReferenceElement().getIdPartAsLong();
		Long omopPersonId = IdMapping.getOMOPfromFHIR(fhirSubjectId, OmopPatient.FHIRTYPE);

		FPerson tPerson = new FPerson();
		tPerson.setId(omopPersonId);
		specimen_.setFPerson(tPerson);

		// FHIR Specimen.collection.collectedDateTime --> OMOP Specimen.specimen_date/datetime
		/* get collection information */
		SpecimenCollectionComponent collection = fhirResource.getCollection();
		if (collection.isEmpty()) {
			throw new FHIRException("We have no collection information in this specimen.");
		}

		Type collected = collection.getCollected();
		if (collected instanceof DateTimeType) {
			DateTimeType collectedDateTime = (DateTimeType) collected;
			specimen_.setSpecimenDate(collectedDateTime.getValue());
			specimen_.setSpecimenDateTime(collectedDateTime.getValue());
		} else {
			throw new FHIRException("OMOP Specimen only support collected date/datetime.");
		}

		Quantity quantity = collection.getQuantity();
		if (!quantity.isEmpty()) {
			// FHIR Specimen.collection.quantity.value --> OMOP Specimen.quantity
			specimen_.setQuantity(quantity.getValue().doubleValue());

			// FHIR Specimen.collection.quantity.system --> OMOP Specimen.unit_concept.vocabulary_id
			String fhirSystemUri = quantity.getSystem();
			String OmopVocabularyId = fhirOmopVocabularyMap.getOmopVocabularyFromFhirSystemName(fhirSystemUri);
			Concept unitConcept = CodeableConceptUtil.getOmopConceptWithOmopVacabIdAndCode(conceptService, OmopVocabularyId, quantity.getCode());
			if (unitConcept != null) {
				specimen_.setUnitConcept(unitConcept);
			}

			// FHIR Specimen.collection.quantity.unit --> OMOP Specimen.unit_source_value
			// FHIR Specimen.collection.quantity.code --> OMOP Specimen.unit_source_value if above is n/a.
			if (quantity.getUnit() != null && quantity.getUnit().isBlank()) {
				specimen_.setUnitSourceValue(quantity.getUnit());
			} else if (quantity.getCode() != null && !quantity.getCode().isBlank()) {
				specimen_.setUnitSourceValue(quantity.getCode());
			}
		}

		// FHIR Specimen.collection.bodySite --> OMOP Specimen.anatomic_stie_concept_id / anatomic_site_source_value
		CodeableConcept bodySite = collection.getBodySite();
		if (!bodySite.isEmpty()) {
			Concept omopBodySiteConcept = CodeableConceptUtil.getOmopConceptWithFhirConcept(conceptService, bodySite.getCodingFirstRep());
			if (omopBodySiteConcept != null) {
				specimen_.setAnatomicSiteConcept(omopBodySiteConcept);
			}

			if (bodySite.getText() != null && !bodySite.getText().isBlank()) {
				specimen_.setAnatomicSiteSourceValue(bodySite.getText());
			} else {
				Coding bodySiteCoding = bodySite.getCodingFirstRep();
				if (bodySiteCoding.getDisplay() != null && !bodySiteCoding.getDisplay().isBlank()) {
					specimen_.setAnatomicSiteSourceValue(bodySiteCoding.getDisplay());
				} else if (bodySiteCoding.getCode() != null && !bodySiteCoding.getCode().isBlank()) {
					specimen_.setAnatomicSiteSourceValue(bodySiteCoding.getCode());
				}
			}
		}

		// Non mappable element(s) in OMOP.
		specimen_.setSpecimenTypeConcept(new Concept(0L));

		return specimen_;
	}

	public void validation(Specimen fhirResource, IdType fhirId) throws FHIRException {
		Reference subjectReference = fhirResource.getSubject();
		if (subjectReference == null || subjectReference.isEmpty()) {
			throw new FHIRException("We requres subject to contain a Patient");
		}
		if (!subjectReference.getReferenceElement().getResourceType()
				.equalsIgnoreCase(OmopPatient.FHIRTYPE)) {
			throw new FHIRException("We only support " + OmopPatient.FHIRTYPE
					+ " for subject. But provided [" + subjectReference.getReferenceElement().getResourceType() + "]");
		}

		Long fhirSubjectId = subjectReference.getReferenceElement().getIdPartAsLong();
		Long omopPersonId = IdMapping.getOMOPfromFHIR(fhirSubjectId, OmopPatient.FHIRTYPE);
		if (omopPersonId == null) {
			throw new FHIRException("We couldn't find the patient in the Subject");
		}
	}

	@Override
	public Long toDbase(Specimen fhirResource, IdType fhirId) throws FHIRException {
		Long fhirIdLong = null;
		Long omopId = null;

		// fhirResource validation. This will throw if validation failed. 
		validation(fhirResource, fhirId);

		if (fhirId != null) {
			fhirIdLong = fhirId.getIdPartAsLong();
			omopId = IdMapping.getOMOPfromFHIR(fhirIdLong, OmopSpecimen.FHIRTYPE);
		} else {
			Long patientFhirId = fhirResource.getSubject().getReferenceElement().getIdPartAsLong();

			// get specimen concept, which is specimen type in FHIR
			CodeableConcept specimenType = fhirResource.getType();
			Concept	omopSpecimenConcept = CodeableConceptUtil.searchConcept(conceptService, specimenType);

			// get date and time
			SpecimenCollectionComponent collection =  fhirResource.getCollection();
			DateTimeType collectedDate = collection.getCollectedDateTimeType();
			Date date = collectedDate.getValue();

			// check if we already have this entry by comparing
			// type, date, and patient
			List<ParameterWrapper> paramList = new ArrayList<ParameterWrapper>();

			// Add patient to search
			paramList.addAll(mapParameter("Patient:" + Patient.SP_RES_ID, String.valueOf(patientFhirId), false));

			if (omopSpecimenConcept != null) {
				ParameterWrapper paramWrapper = new ParameterWrapper();
				paramWrapper.setParameterType("String");
				paramWrapper.setParameters(
							Arrays.asList("specimenConcept.vocabularyId", "specimenConcept.conceptCode"));
				paramWrapper.setOperators(Arrays.asList("like", "like"));
				paramWrapper.setValues(Arrays.asList(omopSpecimenConcept.getVocabularyId(), omopSpecimenConcept.getConceptCode()));
				
				paramWrapper.setRelationship("and");
				paramList.add(paramWrapper);

				DateParam dateParam = new DateParam();
				dateParam.setPrefix(ParamPrefixEnum.EQUAL);
				dateParam.setValue(date);
				DateRangeParam dateRangeParam = new DateRangeParam(dateParam);
				paramList.addAll(mapParameter(Specimen.SP_COLLECTED, dateRangeParam, false));

				// Search specimen table.
				List<edu.gatech.chai.omopv5.model.entity.Specimen> specimens_ = getMyOmopService().searchWithParams(0, 0, paramList, null);
				if (specimens_.size() > 0) {
					omopId = specimens_.get(0).getId();
				}
			}
		}

		edu.gatech.chai.omopv5.model.entity.Specimen omopSecimen = constructOmop(omopId, fhirResource);
		Long omopRecordId = null;
		if (omopSecimen.getId() != null) {
			omopRecordId = getMyOmopService().update(omopSecimen).getId();
		} else {
			omopRecordId = getMyOmopService().create(omopSecimen).getId();
		}

		return IdMapping.getFHIRfromOMOP(omopRecordId, OmopSpecimen.FHIRTYPE);
	}

	private static Date createDateTime(edu.gatech.chai.omopv5.model.entity.Specimen specimen_) {
		Date myDate = null;
		if (specimen_.getSpecimenDate() != null) {
			try {
				if (specimen_.getSpecimenDateTime() != null) {
					SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
					String dateTimeString = fmt.format(specimen_.getSpecimenDateTime());
					myDate = fmt.parse(dateTimeString);
				} else {
					SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd");
					String dateString = fmt.format(specimen_.getSpecimenDate());
					myDate = fmt.parse(dateString);
				}
			} catch (ParseException e) {
				e.printStackTrace();
			}
		}

		return myDate;
	}

	public List<ParameterWrapper> mapParameter(String parameter, Object value, boolean or) {
		List<ParameterWrapper> mapList = new ArrayList<ParameterWrapper>();
		ParameterWrapper paramWrapper = new ParameterWrapper();
		if (or)
			paramWrapper.setUpperRelationship("or");
		else
			paramWrapper.setUpperRelationship("and");

		switch (parameter) {
		case Specimen.SP_RES_ID:
			String organizationId = ((TokenParam) value).getValue();
			paramWrapper.setParameterType("Long");
			paramWrapper.setParameters(Arrays.asList("id"));
			paramWrapper.setOperators(Arrays.asList("="));
			paramWrapper.setValues(Arrays.asList(organizationId));
			paramWrapper.setRelationship("or");
			mapList.add(paramWrapper);
			break;
		case Specimen.SP_COLLECTED:
			DateRangeParam dateRangeParam = ((DateRangeParam) value);
			DateUtil.constructParameterWrapper(dateRangeParam, "specimenDate", paramWrapper, mapList);
			break;
		case "Patient:" + Patient.SP_RES_ID:
			addParamlistForPatientIDName(parameter, (String) value, paramWrapper, mapList);
			break;
		case "Patient:" + Patient.SP_NAME:
			addParamlistForPatientIDName(parameter, (String) value, paramWrapper, mapList);
			break;
		default:
			mapList = null;
		}

		return mapList;
	}
}
