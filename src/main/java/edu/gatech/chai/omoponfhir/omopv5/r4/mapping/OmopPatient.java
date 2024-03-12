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
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.hl7.fhir.r4.model.Address;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.ContactPoint;
import org.hl7.fhir.r4.model.ContactPoint.ContactPointSystem;
import org.hl7.fhir.r4.model.ContactPoint.ContactPointUse;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.HumanName;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Organization;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Patient.PatientLinkComponent;
import org.hl7.fhir.r4.model.Practitioner;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.Address.AddressUse;
import org.hl7.fhir.r4.model.Enumerations.AdministrativeGender;
import org.hl7.fhir.r4.model.codesystems.V3MaritalStatus;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.context.ContextLoaderListener;
import org.springframework.web.context.WebApplicationContext;

import ca.uhn.fhir.rest.api.SortSpec;
import ca.uhn.fhir.rest.param.DateParam;
import ca.uhn.fhir.rest.param.ParamPrefixEnum;
import ca.uhn.fhir.rest.param.StringParam;
import ca.uhn.fhir.rest.param.TokenParam;
import edu.gatech.chai.omoponfhir.omopv5.r4.model.USCorePatient;
import edu.gatech.chai.omoponfhir.omopv5.r4.model.USCorePatient.Ethnicity;
import edu.gatech.chai.omoponfhir.omopv5.r4.model.USCorePatient.Race;
import edu.gatech.chai.omoponfhir.omopv5.r4.utilities.AddressUtil;
import edu.gatech.chai.omopv5.dba.service.ConceptService;
import edu.gatech.chai.omopv5.dba.service.FPersonService;
import edu.gatech.chai.omopv5.dba.service.LocationService;
import edu.gatech.chai.omopv5.dba.service.ParameterWrapper;
import edu.gatech.chai.omopv5.dba.service.ProviderService;
import edu.gatech.chai.omopv5.dba.service.VisitOccurrenceService;
import edu.gatech.chai.omopv5.model.entity.Concept;
import edu.gatech.chai.omopv5.model.entity.FPerson;
import edu.gatech.chai.omopv5.model.entity.Location;
import edu.gatech.chai.omopv5.model.entity.Provider;
import edu.gatech.chai.omopv5.model.entity.VisitOccurrence;

public class OmopPatient extends BaseOmopResource<USCorePatient, FPerson, FPersonService> {

	private static final Logger logger = LoggerFactory.getLogger(OmopPatient.class);

	private static OmopPatient omopPatient = new OmopPatient();

	private ConceptService conceptService;
	private LocationService locationService;
	private ProviderService providerService;
	private VisitOccurrenceService visitOccurrenceService;

	// * condition_occurrence : Condition
	// * death : death on FHIR (need to revisit) TODO
	// * device_exposure : DeviceUseStatement
	// * drug_exposure : Medication[x]
	// * measurement & observation : Observation
	// * note : DocumentReference
	// * procedure_occurrence : Procecure
	// * visit_occurrence: : Encounter

	public OmopPatient(WebApplicationContext context) {
		super(context, FPerson.class, FPersonService.class, OmopPatient.FHIRTYPE);
		initialize(context);
		
		// Get count and put it in the counts.
		getSize(true);
	}

	public OmopPatient() {
		super(ContextLoaderListener.getCurrentWebApplicationContext(), FPerson.class, FPersonService.class,
				OmopPatient.FHIRTYPE);
		initialize(ContextLoaderListener.getCurrentWebApplicationContext());
	}

	private void initialize(WebApplicationContext context) {
		locationService = context.getBean(LocationService.class);
		providerService = context.getBean(ProviderService.class);
		visitOccurrenceService = context.getBean(VisitOccurrenceService.class);
		conceptService = context.getBean(ConceptService.class);

	}

	public static OmopPatient getInstance() {
		return omopPatient;
	}

	public static String FHIRTYPE = "Patient";

	@Override
	public USCorePatient constructResource(Long fhirId, FPerson entity, List<String> includes) {
		USCorePatient patient = constructFHIR(fhirId, entity);
		Long omopId = entity.getId();

		if (!includes.isEmpty()) {
			if (includes.contains("Patient:general-practitioner")) {
				if (patient.hasGeneralPractitioner()) {
					List<Reference> generalPractitioners = patient.getGeneralPractitioner();
					for (Reference generalPractitioner : generalPractitioners) {
						if (generalPractitioner.fhirType().equals(OmopPractitioner.FHIRTYPE)) {
							// We map generalPractitioner to Provider, which is
							// Practitioner.
							IIdType generalPractitionerId = generalPractitioner.getReferenceElement();
							Long generalPractFhirId = generalPractitionerId.getIdPartAsLong();
							Practitioner practitioner = OmopPractitioner.getInstance().constructFHIR(generalPractFhirId,
									entity.getProvider());
							generalPractitioner.setResource(practitioner);
						}
					}
				}
			}

			if (includes.contains("Patient:organization")) {
				if (patient.hasManagingOrganization()) {
					Reference managingOrganization = patient.getManagingOrganization();
					IIdType managingOrganizationId = managingOrganization.getReferenceElement();
					Long manageOrgFhirId = managingOrganizationId.getIdPartAsLong();
					Organization organization = OmopOrganization.getInstance().constructFHIR(manageOrgFhirId,
							entity.getCareSite());
					patient.getManagingOrganization().setResource(organization);
				}
			}

			// TODO: OMOP table cannot handle link patient....
			// We just put the code assuming somehow linked was made via person
			// table.
			if (includes.contains("Patient:link")) {
				if (patient.hasLink()) {
					List<PatientLinkComponent> patientLinks = patient.getLink();
					for (PatientLinkComponent patientLink : patientLinks) {
						if (patientLink.hasOther()) {
							Reference patientLinkOther = patientLink.getOther();
							IIdType patientLinkOtherId = patientLinkOther.getReferenceElement();
							Patient linkedPatient;
							if (patientLinkOther.fhirType().equals(OmopPatient.FHIRTYPE)) {
								FPerson linkedPerson = getMyOmopService().findById(omopId);
								linkedPatient = constructFHIR(patientLinkOtherId.getIdPartAsLong(), linkedPerson);
							} else {
								FPerson linkedPerson = getMyOmopService().findById(omopId);
								linkedPatient = constructFHIR(patientLinkOtherId.getIdPartAsLong(), linkedPerson);
							}
							patientLink.getOther().setResource(linkedPatient);
						}
					}
				}
			}
		}
		return patient;
	}

	@Override
	public USCorePatient constructFHIR(Long fhirId, FPerson fPerson) {
		USCorePatient patient = new USCorePatient();
		patient.setId(new IdType(fhirId));

		// if source column is not empty, add it to identifier.
		String personSourceValue = fPerson.getPersonSourceValue();
		if (personSourceValue != null && !personSourceValue.isEmpty() && !"".equals(personSourceValue.trim())) {
			Identifier identifier = new Identifier();
			String[] personIdentifier = personSourceValue.trim().split("\\^");
			if (personIdentifier.length == 1) {
				// There is no ^ delimiter found from string. First one is just value.
				identifier.setValue(personIdentifier[0]);
			} else {
				String omopVoc = personIdentifier[0];
				String system = fhirOmopVocabularyMap.getFhirSystemNameFromOmopVocabulary(omopVoc);
				String value = "";

				if (personIdentifier.length > 2) {
					// if the length is more than 2, it means we have to set type not system.
					String code = personIdentifier[1];
					CodeableConcept typeCodeable = new CodeableConcept();
					Coding typeCoding = new Coding();
					if (!"None".equals(system)) {
						typeCoding.setSystem(system);
					}
					typeCoding.setCode(code);
					typeCodeable.addCoding(typeCoding);
					identifier.setType(typeCodeable);

					for (int i = 2; i < personIdentifier.length; i++) {
						value = value.concat(personIdentifier[i]);
					}

				} else {
					// This must be length = 2. And, we set system.
					if (!"None".equals(system)) {
						identifier.setSystem(system);
					}
					value = personIdentifier[1];
				}
				identifier.setValue(value);
			}

//			identifier.setValue(personSourceValue.trim());
			patient.addIdentifier(identifier);
		}

		String ssn = fPerson.getSsn();
		if (ssn != null && !ssn.isEmpty()) {
			// add ssn to identifier
			Identifier identifier = new Identifier();
			identifier.setSystem("http://hl7.org/fhir/sid/us-ssn");
			identifier.setValue(ssn);
			patient.addIdentifier(identifier);
		}

		// if (patient.getIdentifier().isEmpty()) {
		// 	Identifier identifier = new Identifier();
		// 	identifier.setValue(fhirId.toString());
		// 	patient.addIdentifier(identifier);
		// }

		// Start mapping Person/FPerson table to Patient Resource.
		Date birthDateTime = fPerson.getBirthDateTime();
		if (birthDateTime != null) {
			patient.setBirthDate(birthDateTime);
		} else {
			Calendar calendar = Calendar.getInstance();
			int yob, mob, dob;
			if (fPerson.getYearOfBirth() != null && fPerson.getYearOfBirth() > 0)
				yob = fPerson.getYearOfBirth();
			else
				yob = 1970;

			if (fPerson.getMonthOfBirth() != null && fPerson.getMonthOfBirth() > 0)
				mob = fPerson.getMonthOfBirth();
			else
				mob = 6;

			if (fPerson.getDayOfBirth() != null && fPerson.getDayOfBirth() != 0) {
				dob = fPerson.getDayOfBirth();
			} else {
				if (fPerson.getMonthOfBirth() == null || fPerson.getMonthOfBirth() == 0) {
					dob = 15;
				} else {
					dob = 1;
				}
			}

			calendar.set(yob, mob - 1, dob);
			patient.setBirthDate(calendar.getTime());
		}

		if (fPerson.getLocation() != null && fPerson.getLocation().getId() != 0L) {
			// WARNING check if mapping for lines are correct
			patient.addAddress().setUse(AddressUse.HOME).addLine(fPerson.getLocation().getAddress1())
					.addLine(fPerson.getLocation().getAddress2()).setCity(fPerson.getLocation().getCity())
					.setPostalCode(fPerson.getLocation().getZip()).setState(fPerson.getLocation().getState());
		}

		if (fPerson.getGenderConcept() != null) {
			String gName = fPerson.getGenderConcept().getConceptName();
			if (gName == null || gName.isEmpty()) {
				Concept genderConcept = conceptService.findById(fPerson.getGenderConcept().getId());
				if (genderConcept != null)
					gName = genderConcept.getConceptName();
				else
					gName = null;
			}
			if (gName != null) {
				gName = gName.toLowerCase();
				AdministrativeGender gender;
				try {
					gender = AdministrativeGender.fromCode(gName);
					patient.setGender(gender);
				} catch (FHIRException e) {
					e.printStackTrace();
					patient.setGender(AdministrativeGender.OTHER);
				}
			}
		}

		if (fPerson.getProvider() != null && fPerson.getProvider().getId() != 0L) {
			Long genPracFhirId = IdMapping.getFHIRfromOMOP(fPerson.getProvider().getId(),
					OmopPractitioner.FHIRTYPE);
			Reference generalPractitioner = new Reference(
					new IdType(OmopPractitioner.FHIRTYPE, genPracFhirId));
			generalPractitioner.setDisplay(fPerson.getProvider().getProviderName());
			List<Reference> generalPractitioners = new ArrayList<Reference>();
			generalPractitioners.add(generalPractitioner);
			patient.setGeneralPractitioner(generalPractitioners);
		}

		if (fPerson.getCareSite() != null && fPerson.getCareSite().getId() != 0L) {
			Long manageOrgFhirId = IdMapping.getFHIRfromOMOP(fPerson.getCareSite().getId(),
					OmopOrganization.FHIRTYPE);
			Reference managingOrganization = new Reference(
					new IdType(OmopOrganization.FHIRTYPE, manageOrgFhirId));
			managingOrganization.setDisplay(fPerson.getCareSite().getCareSiteName());
			patient.setManagingOrganization(managingOrganization);
		}

		HumanName humanName = new HumanName();
		humanName.setFamily(fPerson.getFamilyName()).addGiven(fPerson.getGivenName1());
		patient.addName(humanName);
		if (fPerson.getGivenName2() != null)
			patient.getName().get(0).addGiven(fPerson.getGivenName2());

		if (fPerson.getActive() == null || fPerson.getActive() == 0)
			patient.setActive(false);
		else
			patient.setActive(true);

		if (fPerson.getMaritalStatus() != null && !fPerson.getMaritalStatus().isEmpty()) {
			CodeableConcept maritalStatusCode = new CodeableConcept();
			V3MaritalStatus maritalStatus;
			try {
				maritalStatus = V3MaritalStatus.fromCode(fPerson.getMaritalStatus().toUpperCase());
				Coding coding = new Coding(maritalStatus.getSystem(), maritalStatus.toCode(),
						maritalStatus.getDisplay());
				maritalStatusCode.addCoding(coding);
				patient.setMaritalStatus(maritalStatusCode);
			} catch (FHIRException e) {
				e.printStackTrace();
			}
		}

		List<ContactPoint> contactPoints = new ArrayList<ContactPoint>();
		if (fPerson.getContactPoint1() != null && !fPerson.getContactPoint1().isEmpty()) {
			String[] contactInfo = fPerson.getContactPoint1().split(":");
			if (contactInfo.length == 3) {
				ContactPoint contactPoint = new ContactPoint();
				if(!contactInfo[0].isBlank() && !contactInfo[0].equalsIgnoreCase("null")){
					contactPoint.setSystem(ContactPoint.ContactPointSystem.valueOf(contactInfo[0].toUpperCase()));
				}
				if(!contactInfo[0].isBlank() && !contactInfo[1].equalsIgnoreCase("null")){
					contactPoint.setUse(ContactPoint.ContactPointUse.valueOf(contactInfo[1].toUpperCase()));
				}
				contactPoint.setValue(contactInfo[2]);
				contactPoints.add(contactPoint);
			}
		}
		if (fPerson.getContactPoint2() != null && !fPerson.getContactPoint2().isEmpty()) {
			String[] contactInfo = fPerson.getContactPoint2().split(":");
			if (contactInfo.length == 3) {
				ContactPoint contactPoint = new ContactPoint();
				if(!contactInfo[0].equalsIgnoreCase("null")){
					contactPoint.setSystem(ContactPoint.ContactPointSystem.valueOf(contactInfo[0].toUpperCase()));
				}
				if(!contactInfo[1].equalsIgnoreCase("null")){
					contactPoint.setUse(ContactPoint.ContactPointUse.valueOf(contactInfo[1].toUpperCase()));
				}
				contactPoint.setValue(contactInfo[2]);
				contactPoints.add(contactPoint);
			}
		}
		if (fPerson.getContactPoint3() != null && !fPerson.getContactPoint3().isEmpty()) {
			String[] contactInfo = fPerson.getContactPoint3().split(":");
			if (contactInfo.length == 3) {
				ContactPoint contactPoint = new ContactPoint();
				if(!contactInfo[0].equalsIgnoreCase("null")){
					contactPoint.setSystem(ContactPoint.ContactPointSystem.valueOf(contactInfo[0].toUpperCase()));
				}
				if(!contactInfo[1].equalsIgnoreCase("null")){
					contactPoint.setUse(ContactPoint.ContactPointUse.valueOf(contactInfo[1].toUpperCase()));
				}
				contactPoint.setValue(contactInfo[2]);
				contactPoints.add(contactPoint);
			}
		}

		patient.setTelecom(contactPoints);

		// US Core Patient Extension
		// Race
		Concept raceConcept = fPerson.getRaceConcept();
		String raceSourceString = fPerson.getRaceSourceValue();
		Coding raceCoding = null;
		if (raceConcept == null) {
			if (raceSourceString != null && !raceSourceString.isEmpty()) {
				raceCoding = fhirOmopCodeMap.getFhirCodingFromOmopSourceString(raceSourceString);
			}
		} else {
			Long raceConceptId = raceConcept.getIdAsLong();
			if (raceConceptId != 0L) {
				raceCoding = fhirOmopCodeMap.getFhirCodingFromOmopConcept(raceConceptId);
			}
		}

		if (raceCoding != null) {
			Race myRace = patient.getRace();
			myRace.getCategory().add(raceCoding);
			myRace.setText(new StringType(raceCoding.getDisplay()));
			patient.setRace(myRace);
		}

		// Ethnicity
		Concept ethnicityConcept = fPerson.getEthnicityConcept();
		String ethnicitySourceString = fPerson.getEthnicitySourceValue();
		Coding ethnicityCoding = null;
		if (ethnicityConcept == null) {
			if (ethnicitySourceString != null && !ethnicitySourceString.isEmpty()) {
				ethnicityCoding = fhirOmopCodeMap.getFhirCodingFromOmopSourceString(ethnicitySourceString);
			}
		} else {
			Long ethnicityConceptId = ethnicityConcept.getIdAsLong();
			if (ethnicityConceptId != 0L) {
				ethnicityCoding = fhirOmopCodeMap.getFhirCodingFromOmopConcept(ethnicityConceptId);
			}
		}

		if (ethnicityCoding != null) {
			Ethnicity myEthnicity = patient.getEthnicity();
			myEthnicity.getCategory().add(ethnicityCoding);
			myEthnicity.setText(new StringType(ethnicityCoding.getDisplay()));
			patient.setEthnicity(myEthnicity);
		}

		return patient;
	}

	private String getPersonSourceValue(Identifier identifier) {
		String value = identifier.getValue();
		String system = identifier.getSystem();

		String personSourceValue = value;

		if (system != null && !system.isEmpty()) {
			String omopVoc = fhirOmopVocabularyMap.getOmopVocabularyFromFhirSystemName(system);
			if (!"None".equals(omopVoc)) {
				personSourceValue = omopVoc + "^" + value;
			}
		} else {
			// if system is null or empty, then we check type.
			// type is codeable concept. We put systemUri and code with ^ delimiter
			CodeableConcept typeCodeableConcept = identifier.getType();
			if (typeCodeableConcept != null && !typeCodeableConcept.isEmpty()) {
				for (Coding coding : typeCodeableConcept.getCoding()) {
					if (coding != null && !coding.isEmpty()) {
						String systemUri = coding.getSystem();
						String code = coding.getCode();
						String omopVoc = fhirOmopVocabularyMap
								.getOmopVocabularyFromFhirSystemName(systemUri + "^" + code);
						if (!"None".equals(omopVoc)) {
							// We found something from internal mapping.
							personSourceValue = omopVoc + "^" + personSourceValue;
						}
					}
				}
			}
		}

		return personSourceValue;
	}

	/**
	 * OMOP on FHIR mapping - from FHIR to OMOP
	 * 
	 * @param Patient resource.
	 * @param IdType  fhirId that you want to update
	 * 
	 * @return Resource ID. Returns ID in Long. This is what needs to be used to
	 *         refer this resource.
	 */
	@Override
	public Long toDbase(USCorePatient patient, IdType fhirId) throws FHIRException {
		Long omopId = null, fhirIdLong = null;

		if (fhirId != null) {
			// update
			fhirIdLong = fhirId.getIdPartAsLong();
			if (fhirIdLong == null) {
				// Invalid fhirId.
				logger.error("Failed to get Patient.id as Long value");
				return null;
			}
			
			omopId = IdMapping.getOMOPfromFHIR(fhirIdLong, OmopPatient.FHIRTYPE);
		}

		FPerson fperson = constructOmop(omopId, patient);

		Long omopRecordId = null;
		if (fperson.getId() != null) {
			omopRecordId = getMyOmopService().update(fperson).getId();
		} else {
			omopRecordId = getMyOmopService().create(fperson).getId();
		}
		Long fhirRecordId = IdMapping.getFHIRfromOMOP(omopRecordId, OmopPatient.FHIRTYPE);
		return fhirRecordId;
	}

	@Override
	public void addRevIncludes(Long omopId, List<String> includes, List<IBaseResource> listResources) {
		List<ParameterWrapper> mapList = new ArrayList<ParameterWrapper>();

		if (includes.contains("Encounter:subject")) {
			final ParameterWrapper revIncludeparam = new ParameterWrapper("Long", Arrays.asList("fPerson.id"),
					Arrays.asList("="), Arrays.asList(String.valueOf(omopId)), "or");

			mapList.add(revIncludeparam);

			List<VisitOccurrence> VisitOccurrences = visitOccurrenceService.searchWithParams(0, 0, mapList, null);
			for (VisitOccurrence visitOccurrence : VisitOccurrences) {
				Long fhirId = IdMapping.getFHIRfromOMOP(visitOccurrence.getId(), OmopEncounter.FHIRTYPE);
				Encounter enc = OmopEncounter.getInstance().constructFHIR(fhirId, visitOccurrence);
				if (enc != null)
					listResources.add(enc);
			}
		}
		if (includes.contains("Observation:subject")) {

		}
		if (includes.contains("Device:patient")) {

		}
		if (includes.contains("Condition:subject")) {

		}
		if (includes.contains("Procedure:subject")) {

		}
		if (includes.contains("MedicationRequest:subject")) {

		}
		if (includes.contains("MedicationAdministration:subject")) {

		}
		if (includes.contains("MedicationDispense:subject")) {

		}
		if (includes.contains("MedicationStatement:subject")) {

		}

	}

	/**
	 * searchAndUpdate: search the database for general Practitioner. This is
	 * provider table in OMOP. If exist, return it. We may have this received
	 * before, in this case, search it from source column and return it. Otherwise,
	 * create a new one.
	 * 
	 * Returns provider entity in OMOP.
	 * 
	 * @param generalPractitioner
	 * @return
	 */
	public Provider searchAndUpdate(Reference generalPractitioner) {
		if (generalPractitioner == null)
			return null;

		// See if this exists.
		Long fhirId = generalPractitioner.getReferenceElement().getIdPartAsLong();
		Long omopId = IdMapping.getOMOPfromFHIR(fhirId, OmopPractitioner.FHIRTYPE);
		Provider provider = providerService.findById(omopId);
		if (provider != null) {
			return provider;
		} else {
			// Check source column to see if we have received this before.
			List<Provider> providers = providerService.searchByColumnString("providerSourceValue",
					generalPractitioner.getReferenceElement().getIdPart());
			if (!providers.isEmpty()) {
				return providers.get(0);
			} else {
				provider = new Provider();
				provider.setProviderSourceValue(generalPractitioner.getReferenceElement().getIdPart());
				if (generalPractitioner.getDisplay() != null)
					provider.setProviderName(generalPractitioner.getDisplay().toString());
				return provider;
			}
		}
	}

	// @Override
	// public Long getSize() {
	// return myOmopService.getSize(FPerson.class);
	// }
	//
	// public Long getSize(Map<String, List<ParameterWrapper>> map) {
	// return myOmopService.getSize(FPerson.class, map);
	// }

	/**
	 * mapParameter: This maps the FHIR parameter to OMOP column name.
	 * 
	 * @param parameter FHIR parameter name.
	 * @param value     FHIR value for the parameter
	 * @return returns ParameterWrapper class, which contains OMOP column name and
	 *         value with operator.
	 */
	public List<ParameterWrapper> mapParameter(String parameter, Object value, boolean or) {
		List<ParameterWrapper> mapList = new ArrayList<ParameterWrapper>();
		ParameterWrapper paramWrapper = new ParameterWrapper();
		if (or)
			paramWrapper.setUpperRelationship("or");
		else
			paramWrapper.setUpperRelationship("and");

		switch (parameter) {
		case Patient.SP_ACTIVE:
			// True of False in FHIR. In OMOP, this is 1 or 0.
			String activeValue = ((TokenParam) value).getValue();
			String activeString;
			if (activeValue.equalsIgnoreCase("true"))
				activeString = "1";
			else
				activeString = "0";
			paramWrapper.setParameterType("Short");
			paramWrapper.setParameters(Arrays.asList("active"));
			paramWrapper.setOperators(Arrays.asList("="));
			paramWrapper.setValues(Arrays.asList(activeString));
			paramWrapper.setRelationship("or");
			mapList.add(paramWrapper);
			break;
		case Patient.SP_EMAIL:
			String emailValue = ((TokenParam) value).getValue();
			String emailSystemValue = ContactPoint.ContactPointSystem.EMAIL.toCode();
			paramWrapper.setParameterType("String");
			paramWrapper.setParameters(Arrays.asList("contactPoint1", "contactPoint2", "contactPoint3"));
			paramWrapper.setOperators(Arrays.asList("like", "like", "like"));
			paramWrapper.setValues(Arrays.asList("%" + emailSystemValue + ":%:%" + emailValue + "%"));
			paramWrapper.setRelationship("or");
			mapList.add(paramWrapper);
			break;
		case Patient.SP_PHONE:
			String phoneValue = ((TokenParam) value).getValue();
			String phoneSystemValue = ContactPoint.ContactPointSystem.PHONE.toCode();
			paramWrapper.setParameterType("String");
			paramWrapper.setParameters(Arrays.asList("contactPoint1", "contactPoint2", "contactPoint3"));
			paramWrapper.setOperators(Arrays.asList("like", "like", "like"));
			paramWrapper.setValues(Arrays.asList("%" + phoneSystemValue + ":%:%" + phoneValue + "%"));
			paramWrapper.setRelationship("or");
			mapList.add(paramWrapper);
			break;
		case Patient.SP_TELECOM:
			String telecomValue = ((TokenParam) value).getValue();
			String telecomSystemValue = ((TokenParam) value).getSystem();
			paramWrapper.setParameterType("String");
			paramWrapper.setParameters(Arrays.asList("contactPoint1", "contactPoint2", "contactPoint3"));
			paramWrapper.setOperators(Arrays.asList("like", "like", "like"));
			paramWrapper.setValues(Arrays.asList("%" + telecomSystemValue + ":%:%" + telecomValue + "%"));
			paramWrapper.setRelationship("or");
			mapList.add(paramWrapper);
			break;
		case Patient.SP_BIRTHDATE:
			// We only compare date (no time). Get year, month, date
			// form DateParam value.
			Date date = ((DateParam) value).getValue();
			ParamPrefixEnum relation = ((DateParam) value).getPrefix();
			String operator;
			if (relation.equals(ParamPrefixEnum.LESSTHAN))
				operator = "<";
			else if (relation.equals(ParamPrefixEnum.LESSTHAN_OR_EQUALS))
				operator = "<=";
			else if (relation.equals(ParamPrefixEnum.GREATERTHAN))
				operator = ">";
			else if (relation.equals(ParamPrefixEnum.GREATERTHAN_OR_EQUALS))
				operator = ">=";
			else
				operator = "=";

			Calendar cal = Calendar.getInstance();
			cal.setTime(date);
			int year = cal.get(Calendar.YEAR);
			int month = cal.get(Calendar.MONDAY) + 1;
			int day = cal.get(Calendar.DAY_OF_MONTH);
			paramWrapper.setParameterType("Integer");
			paramWrapper.setParameters(Arrays.asList("yearOfBirth", "monthOfBirth", "dayOfBirth"));
			paramWrapper.setOperators(Arrays.asList(operator, operator, operator));
			paramWrapper.setValues(Arrays.asList(String.valueOf(year), String.valueOf(month), String.valueOf(day)));
			paramWrapper.setRelationship("and");
			mapList.add(paramWrapper);
			break;
		case Patient.SP_FAMILY:
			// This is family name, which is string. use like.
			String familyString;
			if (((StringParam) value).isExact())
				familyString = ((StringParam) value).getValue();
			else
				familyString = "%" + ((StringParam) value).getValue() + "%";
			paramWrapper.setParameterType("String");
			paramWrapper.setParameters(Arrays.asList("familyName"));
			paramWrapper.setOperators(Arrays.asList("like"));
			paramWrapper.setValues(Arrays.asList(familyString));
			paramWrapper.setRelationship("or");
			mapList.add(paramWrapper);
			break;
		case Patient.SP_GIVEN:
			// This is given name, which is string. use like.
			String givenName;
			if (((StringParam) value).isExact())
				givenName = ((StringParam) value).getValue();
			else
				givenName = "%" + ((StringParam) value).getValue() + "%";
			paramWrapper.setParameterType("String");
			paramWrapper.setParameters(Arrays.asList("givenName1", "givenName2"));
			paramWrapper.setOperators(Arrays.asList("like", "like"));
			paramWrapper.setValues(Arrays.asList(givenName));
			paramWrapper.setRelationship("or");
			mapList.add(paramWrapper);
			break;
		case Patient.SP_NAME:
			// This is family name, which is string. use like.
			String nameString;
			if (((StringParam) value).isExact())
				nameString = ((StringParam) value).getValue();
			else
				nameString = "%" + ((StringParam) value).getValue() + "%";
			paramWrapper.setParameterType("String");
			paramWrapper
					.setParameters(Arrays.asList("familyName", "givenName1", "givenName2", "prefixName", "suffixName"));
			paramWrapper.setOperators(Arrays.asList("like", "like", "like", "like", "like"));
			paramWrapper.setValues(Arrays.asList(nameString));
			paramWrapper.setRelationship("or");
			mapList.add(paramWrapper);
			break;
		case Patient.SP_RES_ID:
			String patientId = ((TokenParam) value).getValue();
			paramWrapper.setParameterType("Long");
			paramWrapper.setParameters(Arrays.asList("id"));
			paramWrapper.setOperators(Arrays.asList("="));
			paramWrapper.setValues(Arrays.asList(patientId));
			paramWrapper.setRelationship("or");
			mapList.add(paramWrapper);
			break;
		case Patient.SP_IDENTIFIER:
			String identifierSystem = ((TokenParam) value).getSystem();
			String identifierValue = ((TokenParam) value).getValue();

			String searchString = identifierValue;

			if (identifierSystem != null && !identifierSystem.isEmpty()) {
				String omopVocabId = fhirOmopVocabularyMap.getOmopVocabularyFromFhirSystemName(identifierSystem);
				if (!"None".equals(omopVocabId)) {
					searchString = omopVocabId + "^" + searchString;
				}
			}

			paramWrapper.setParameterType("String");
			List<String> parameterList = new ArrayList<String>();
			parameterList.add("personSourceValue");
			paramWrapper.setParameters(parameterList);

			List<String> operatorList = new ArrayList<String>();
			List<String> valueList = new ArrayList<String>();

			if (identifierValue == null || identifierValue.trim().isEmpty()) {
				// We are only searching by system. So, we should select with % after system
				// name.
				operatorList.add("like");
				paramWrapper.setOperators(operatorList);

				valueList.add(searchString + "%");
				paramWrapper.setValues(valueList);
			} else if (identifierSystem == null || identifierSystem.isEmpty()) {
				operatorList.add("like");
				paramWrapper.setOperators(operatorList);

				valueList.add("%^" + searchString);
				paramWrapper.setValues(valueList);

				// for the case where we only have a value in the personSourceValue
				paramWrapper.addParameter("personSourceValue");
				paramWrapper.addOperator("=");
				paramWrapper.addValue(searchString);
			} else {
				operatorList.add("=");
				paramWrapper.setOperators(operatorList);

				valueList.add(searchString);
				paramWrapper.setValues(valueList);
			}
			paramWrapper.setRelationship("or");

			if ("http://hl7.org/fhir/sid/us-ssn".equals(identifierSystem) 
				|| "urn:oid:2.16.840.1.113883.4.1".equals(identifierSystem)) {
				paramWrapper.addParameter("ssn");
				if (identifierValue == null || identifierValue.trim().isEmpty()) {
					paramWrapper.addOperator("like");
					paramWrapper.addValue("%");
				} else {
					paramWrapper.addOperator("=");
					paramWrapper.addValue(identifierValue);
				}
			}

			mapList.add(paramWrapper);
			break;
		case Patient.SP_ADDRESS:
			String addressName;
			if (((StringParam) value).isExact())
				addressName = ((StringParam) value).getValue();
			else
				addressName = "%" + ((StringParam) value).getValue() + "%";
			paramWrapper.setParameterType("String");
			paramWrapper.setParameters(Arrays.asList("location.address1", "location.address2", "location.city",
					"location.state", "location.zipCode"));
			paramWrapper.setOperators(Arrays.asList("like", "like", "like", "like", "like"));
			paramWrapper.setValues(Arrays.asList(addressName));
			paramWrapper.setRelationship("or");
			mapList.add(paramWrapper);
			break;
		case Patient.SP_ADDRESS_CITY:
			String addressCityName;
			if (((StringParam) value).isExact())
				addressCityName = ((StringParam) value).getValue();
			else
				addressCityName = "%" + ((StringParam) value).getValue() + "%";
			paramWrapper.setParameterType("String");
			paramWrapper.setParameters(Arrays.asList("location.city"));
			paramWrapper.setOperators(Arrays.asList("like"));
			paramWrapper.setValues(Arrays.asList(addressCityName));
			paramWrapper.setRelationship("or");
			mapList.add(paramWrapper);
			break;
		case Patient.SP_ADDRESS_STATE:
			String addressStateName;
			if (((StringParam) value).isExact())
				addressStateName = ((StringParam) value).getValue();
			else
				addressStateName = "%" + ((StringParam) value).getValue() + "%";
			paramWrapper.setParameterType("String");
			paramWrapper.setParameters(Arrays.asList("location.state"));
			paramWrapper.setOperators(Arrays.asList("like"));
			paramWrapper.setValues(Arrays.asList(addressStateName));
			paramWrapper.setRelationship("or");
			mapList.add(paramWrapper);
			break;
		case Patient.SP_ADDRESS_POSTALCODE:
			String addressZipName;
			if (((StringParam) value).isExact())
				addressZipName = ((StringParam) value).getValue();
			else
				addressZipName = "%" + ((StringParam) value).getValue() + "%";
			paramWrapper.setParameterType("String");
			paramWrapper.setParameters(Arrays.asList("location.zip"));
			paramWrapper.setOperators(Arrays.asList("like"));
			paramWrapper.setValues(Arrays.asList(addressZipName));
			paramWrapper.setRelationship("or");
			mapList.add(paramWrapper);
			break;
		case "Organization:" + Organization.SP_NAME:
			String orgName = (String) value;
			paramWrapper.setParameterType("String");
			paramWrapper.setParameters(Arrays.asList("careSite.careSiteName"));
			paramWrapper.setOperators(Arrays.asList("like"));
			paramWrapper.setValues(Arrays.asList("%" + orgName + "%"));
			paramWrapper.setRelationship("or");
			mapList.add(paramWrapper);
			break;
		case "Organization:" + Organization.SP_RES_ID:
			String orgId = (String) value;
			paramWrapper.setParameterType("Long");
			paramWrapper.setParameters(Arrays.asList("careSite.id"));
			paramWrapper.setOperators(Arrays.asList("="));
			paramWrapper.setValues(Arrays.asList(orgId));
			paramWrapper.setRelationship("or");
			mapList.add(paramWrapper);
			break;
		default:
			mapList = null;
		}

		return mapList;
	}

//	@Override
//	public String constructSort(String sp, String direction) {
//		String retv;
//		if (sp.equals(Patient.SP_FAMILY)) {
//			retv = "familyName " + direction;
//		} else if (sp.equals(Patient.SP_GIVEN)) {
//			retv = "givenName1 " + direction + ",givenName2 " + direction;
//		} else if (sp.equals(Patient.SP_GENDER)) {
//			retv = "genderConcept " + direction;
//		} else if (sp.equals(Patient.SP_BIRTHDATE)) {
//			retv = "yearOfBirth " + direction + ",monthOfBirth " + direction + ",dayOfBirth " + direction
//					+ ",timeOfBirth " + direction;
//		} else {
//			retv = "id " + direction;
//		}
//
//		return retv;
//	}
//

	@Override
	public String constructOrderParams(SortSpec theSort) {
		if (theSort == null)
			return "id ASC";

//		String orderParams = new String();
		String direction;

		if (theSort.getOrder() != null)
			direction = theSort.getOrder().toString();
		else
			direction = "ASC";

		String orderParam = new String(); // getMyMapper().constructSort(theSort.getParamName(), direction);

		if (theSort.getParamName().equals(Patient.SP_FAMILY)) {
			orderParam = "familyName " + direction;
		} else if (theSort.getParamName().equals(Patient.SP_GIVEN)) {
			orderParam = "givenName1 " + direction + ",givenName2 " + direction;
		} else if (theSort.getParamName().equals(Patient.SP_GENDER)) {
			orderParam = "genderConcept " + direction;
		} else if (theSort.getParamName().equals(Patient.SP_BIRTHDATE)) {
			orderParam = "yearOfBirth " + direction + ",monthOfBirth " + direction + ",dayOfBirth " + direction
					+ ",timeOfBirth " + direction;
		} else {
			orderParam = "id " + direction;
		}

//		if (orderParams.isEmpty()) orderParams = orderParams.concat(orderParam);
//		else orderParams = orderParams.concat(","+orderParam);

		String orderParams = orderParam;

		if (theSort.getChain() != null) {
			orderParams = orderParams.concat("," + constructOrderParams(theSort.getChain()));
		}

		return orderParams;
	}

	@Override
	public FPerson constructOmop(Long omopId, USCorePatient patient) {
		FPerson fperson = null;
		String personSourceValue = null;

		List<Identifier> identifiers = patient.getIdentifier();
		boolean first = true;
		for (Identifier identifier : identifiers) {
			if (identifier.getValue() != null && !identifier.getValue().isEmpty()) {
				String personSourceValueTemp = getPersonSourceValue(identifier);
				if (first) {
					personSourceValue = personSourceValueTemp;
					first = false;
				}

				if (personSourceValueTemp != null) {
					List<FPerson> fPersons = getMyOmopService().searchByColumnString("personSourceValue",
							personSourceValueTemp);
					if (!fPersons.isEmpty()) {
						fperson = fPersons.get(0);
						omopId = fperson.getId();
						break;
					}
				}
			}
		}

		if (omopId != null) {
			// update
			fperson = getMyOmopService().findById(omopId);
			if (fperson == null) {
				try {
					throw new FHIRException(patient.getId() + " does not exist");
				} catch (FHIRException e) {
					e.printStackTrace();
				}
			}
		}

		if (fperson == null) {
			fperson = new FPerson();
		}

		if (personSourceValue != null) {
			fperson.setPersonSourceValue(personSourceValue);
			if (personSourceValue.startsWith("SS^")) {
				fperson.setSsn(personSourceValue.substring("SS^".length()));
			}
		}

		// Set name
		Iterator<HumanName> patientIterator = patient.getName().iterator();
		if (patientIterator.hasNext()) {
			HumanName next = patientIterator.next();
			// the next method was not advancing to the next element, then the
			// need to use the get(index) method.
			if (!next.getGiven().isEmpty()) {
				fperson.setGivenName1(next.getGiven().get(0).getValue());
				if (next.getGiven().size() > 1) // TODO add unit tests, to assure
												// this won't be changed to hasNext
					fperson.setGivenName2(next.getGiven().get(1).getValue());
			}
			String family = next.getFamily();
			fperson.setFamilyName(family);
			if (next.getSuffix().iterator().hasNext())
				fperson.setSuffixName(next.getSuffix().iterator().next().getValue());
			if (next.getPrefix().iterator().hasNext())
				fperson.setPrefixName(next.getPrefix().iterator().next().getValue());
		}

		// Search Location entity to see if we have this address available.
		// If not, create this one.
		List<Address> addresses = patient.getAddress();
		Location retLocation = null;
		if (addresses != null && !addresses.isEmpty()) {
			Address address = addresses.get(0);
			// We should check the state.
			String state = address.getState();
			if (state.length() != 2) {
				String twoState = twoLetterStateMap.getTwoLetter(state);
				if (twoState == null || twoState.isEmpty()) {
					if (state.length() < 2) {
						address.setState(state.substring(0, 1));
					} else {
						address.setState(state.substring(0, 2));
					}
				} else {
					address.setState(twoState);
				}
			}
			retLocation = AddressUtil.searchAndUpdate(locationService, address, null);
			if (retLocation != null) {
				fperson.setLocation(retLocation);
			}
		}

		// Now check if we have
		// WE DO NOT CHECK NAMES FOR EXISTENCE. TOO DANGEROUS.
		// if (retLocation != null && person == null) {
		// // FHIR Patient identifier is empty. Use name and address
		// // to see if we have a patient exits.
		// if (retLocation.getId() != null) {
		// FPerson existingPerson =
		// myOmopService.searchByNameAndLocation(fperson.getFamilyName(),
		// fperson.getGivenName1(), fperson.getGivenName2(), retLocation);
		// if (existingPerson != null) {
		// System.out.println("Patient Exists with PID=" +
		// existingPerson.getId());
		// fperson.setId(existingPerson.getId());
		// }
		// }
		// }

		Calendar c = Calendar.getInstance();
		if (patient.getBirthDate() != null) {
			c.setTime(patient.getBirthDate());
			fperson.setYearOfBirth(c.get(Calendar.YEAR));
			fperson.setMonthOfBirth(c.get(Calendar.MONTH) + 1);
			fperson.setDayOfBirth(c.get(Calendar.DAY_OF_MONTH));
		}
		// TODO set deceased value in Person; Set gender concept (source value
		// is set); list of addresses (?)
		// this.death = patient.getDeceased();

		fperson.setGenderConcept(new Concept());
		String genderCode;
		if (patient.getGender() != null) {
			genderCode = patient.getGender().toCode();
		} else {
			genderCode = AdministrativeGender.NULL.toString();
		}
		try {
			fperson.getGenderConcept().setId(OmopConceptMapping.omopForAdministrativeGenderCode(genderCode));
		} catch (FHIRException e) {
			e.printStackTrace();
		}

		List<Reference> generalPractitioners = patient.getGeneralPractitioner();
		if (!generalPractitioners.isEmpty()) {
			// We can handle only one provider.
			Reference gPref = generalPractitioners.get(0);
			if (gPref != null && !gPref.isEmpty()) {
				Provider retProvider = searchAndUpdate(generalPractitioners.get(0));
				if (retProvider != null) {
					fperson.setProvider(retProvider);
				}
			}
		}

//		if (personSourceValue != null)
//			fperson.setPersonSourceValue(personSourceValue);

		if (patient.getActive())
			fperson.setActive((short) 1);
		else
			fperson.setActive((short) 0);

		CodeableConcept maritalStat = patient.getMaritalStatus();
		if (maritalStat != null && !maritalStat.isEmpty()) {
			Coding coding = maritalStat.getCodingFirstRep();
			if (coding != null && !coding.isEmpty()) {
				logger.debug("MARITAL STATUS:" + coding.getCode());
				fperson.setMaritalStatus(coding.getCode());
			}
		}

		// Get contact information.
		List<ContactPoint> contactPoints = patient.getTelecom();
		int index = 0;
		for (ContactPoint contactPoint : contactPoints) {
			ContactPointSystem contactSystem = contactPoint.getSystem();
			String system = new String();
			if (contactSystem != null)
				system = contactSystem.toCode();

			String use = new String();
			ContactPointUse contactUse = contactPoint.getUse();
			if (contactUse != null)
				use = contactUse.toCode();
			String value = contactPoint.getValue();
			if (index == 0) {
				fperson.setContactPoint1(system + ":" + use + ":" + value);
			} else if (index == 1) {
				fperson.setContactPoint2(system + ":" + use + ":" + value);
			} else {
				fperson.setContactPoint3(system + ":" + use + ":" + value);
				break;
			}
			index++;
		}

		// Do extension for race.
		Race myRace = patient.getRace();
		Concept omopRaceConcept = new Concept(8552L);
		if (!myRace.isEmpty()) {
			for (Coding myCategory : myRace.getCategory()) {
				Long omopRaceConceptId = fhirOmopCodeMap.getOmopCodeFromFhirCoding(myCategory);
				fperson.setRaceSourceValue(myCategory.getDisplay());
				if (omopRaceConceptId != 0L) {
					omopRaceConcept.setId(omopRaceConceptId);
					break;
				}
			}
		}
		fperson.setRaceConcept(omopRaceConcept);

		// Do extension for ethnicity.
		Ethnicity myEthnicity = patient.getEthnicity();
		Concept omopEthnicityConcept = new Concept(0L);
		if (!myEthnicity.isEmpty()) {
			for (Coding myCategory : myEthnicity.getCategory()) {
				Long omopEthnicityConceptId = fhirOmopCodeMap.getOmopCodeFromFhirCoding(myCategory);
				fperson.setEthnicitySourceValue(myCategory.getDisplay());
				if (omopEthnicityConceptId != 0L) {
					omopEthnicityConcept.setId(omopEthnicityConceptId);
					break;
				}
			}
		}
		fperson.setEthnicityConcept(omopEthnicityConcept);
		fperson.setEthnicitySourceConcept(omopEthnicityConcept);

		return fperson;
	}

	private ParameterWrapper constructDateParameterWrapper(List<String> keys, Date startDate, Date endDate) {
		String key1, key2;

		key1 = keys.get(0);
		key2 = keys.get(0);
		if (keys.size() == 2) {
			key2 = keys.get(1);
		}

		ParameterWrapper dateParamWrapper = new ParameterWrapper();
		dateParamWrapper.setUpperRelationship("and");
		if (startDate != null && endDate != null) {
			dateParamWrapper.setParameterType("Date");
			dateParamWrapper.setParameters(Arrays.asList(key1, key2));
			dateParamWrapper.setOperators(Arrays.asList(">=", "<="));
			dateParamWrapper
					.setValues(Arrays.asList(String.valueOf(startDate.getTime()), String.valueOf(endDate.getTime())));
			dateParamWrapper.setRelationship("and");
		} else if (startDate != null) {
			dateParamWrapper.setParameterType("Date");
			dateParamWrapper.setParameters(Arrays.asList(key1));
			dateParamWrapper.setOperators(Arrays.asList(">="));
			dateParamWrapper.setValues(Arrays.asList(String.valueOf(startDate.getTime())));
			dateParamWrapper.setRelationship("or");
		} else if (endDate != null) {
			dateParamWrapper.setParameterType("Date");
			dateParamWrapper.setParameters(Arrays.asList(key1));
			dateParamWrapper.setOperators(Arrays.asList("<="));
			dateParamWrapper.setValues(Arrays.asList(String.valueOf(endDate.getTime())));
			dateParamWrapper.setRelationship("or");
		} else {
			return null;
		}

		return dateParamWrapper;
	}

	public void getEverthingfor(List<IBaseResource> resources, Long patientId, Date startDate, Date endDate) {
		// OMOP Tables that references the patient are as follows.
		// * condition_occurrence : Condition
		// * death : death on FHIR (need to revisit) TODO
		// * device_exposure : DeviceUseStatement
		// * drug_exposure : Medication[x]
		// * measurement & observation : Observation
		// * note : DocumentReference
		// * procedure_occurrence : Procecure
		// * visit_occurrence: : Encounter
		// * observation : AllergyIntolerance
		// * drug_exposure & procedure : Immunization

		ParameterWrapper paramWrapper = new ParameterWrapper();
		String pId = String.valueOf(patientId);
		paramWrapper.setParameterType("Long");
		paramWrapper.setParameters(Arrays.asList("fPerson.id"));
		paramWrapper.setOperators(Arrays.asList("="));
		paramWrapper.setValues(Arrays.asList(pId));
		paramWrapper.setRelationship("or");
		paramWrapper.setUpperRelationship("and");

		// Condition Occurrence.
		List<ParameterWrapper> conditionMapList = new ArrayList<ParameterWrapper>();
		conditionMapList.add(paramWrapper);
		ParameterWrapper dateParamWrapper = constructDateParameterWrapper(Arrays.asList("startDate", "endDate"),
				startDate, endDate);
		if (dateParamWrapper != null) {
			conditionMapList.add(dateParamWrapper);
		}

		OmopCondition omopConditionMapper = OmopCondition.getInstance();
		omopConditionMapper.searchWithParams(0, 0, conditionMapList, resources, new ArrayList<String>(), null);

		// device_exposure : DeviceUseStatement
		List<ParameterWrapper> deviceMapList = new ArrayList<ParameterWrapper>();
		deviceMapList.add(paramWrapper);
		dateParamWrapper = constructDateParameterWrapper(
				Arrays.asList("deviceExposureStartDate", "deviceExposureEndDate"), startDate, endDate);
		if (dateParamWrapper != null) {
			deviceMapList.add(dateParamWrapper);
		}

		OmopDeviceUseStatement omopDeviceUseStatementMapper = OmopDeviceUseStatement.getInstance();
		omopDeviceUseStatementMapper.searchWithParams(0, 0, deviceMapList, resources, new ArrayList<String>(), null);

		// drug_exposure : Medication[x]
		List<ParameterWrapper> medicationStatementMapList = new ArrayList<ParameterWrapper>();
		medicationStatementMapList.add(paramWrapper);
		dateParamWrapper = constructDateParameterWrapper(Arrays.asList("drugExposureStartDate", "drugExposureEndDate"),
				startDate, endDate);
		if (dateParamWrapper != null) {
			medicationStatementMapList.add(dateParamWrapper);
		}

		OmopMedicationStatement omopMedicationStatementMapper = OmopMedicationStatement.getInstance();
		omopMedicationStatementMapper.searchWithParams(0, 0, medicationStatementMapList, resources,
				new ArrayList<String>(), null);

		// measurement & observation : Observation
		List<ParameterWrapper> fobservationMapList = new ArrayList<ParameterWrapper>();
		fobservationMapList.add(paramWrapper);
		dateParamWrapper = constructDateParameterWrapper(Arrays.asList("observationDate"), startDate, endDate);
		if (dateParamWrapper != null) {
			fobservationMapList.add(dateParamWrapper);
		}

		OmopObservation omopObservationMapper = OmopObservation.getInstance();
		omopObservationMapper.searchWithParams(0, 0, fobservationMapList, resources, new ArrayList<String>(), null);

		// note : DocumentReference
		List<ParameterWrapper> noteMapList = new ArrayList<ParameterWrapper>();
		noteMapList.add(paramWrapper);
		if (dateParamWrapper != null) {
			noteMapList.add(dateParamWrapper);
		}

		OmopDocumentReference omopDocumentReferenceMapper = OmopDocumentReference.getInstance();
		omopDocumentReferenceMapper.searchWithParams(0, 0, noteMapList, resources, new ArrayList<String>(), null);

		// procedure_occurrence : Procecure
		List<ParameterWrapper> procedureMapList = new ArrayList<ParameterWrapper>();
		procedureMapList.add(paramWrapper);
		dateParamWrapper = constructDateParameterWrapper(Arrays.asList("procedureDate"), startDate, endDate);
		if (dateParamWrapper != null) {
			procedureMapList.add(dateParamWrapper);
		}

		OmopProcedure omopProcedureMapper = OmopProcedure.getInstance();
		omopProcedureMapper.searchWithParams(0, 0, procedureMapList, resources, new ArrayList<String>(), null);

		// * visit_occurrence: : Encounter
		List<ParameterWrapper> visitMapList = new ArrayList<ParameterWrapper>();
		visitMapList.add(paramWrapper);
		dateParamWrapper = constructDateParameterWrapper(Arrays.asList("startDate", "endDate"), startDate, endDate);
		if (dateParamWrapper != null) {
			visitMapList.add(dateParamWrapper);
		}

		OmopEncounter omopEncounterMapper = OmopEncounter.getInstance();
		omopEncounterMapper.searchWithParams(0, 0, visitMapList, resources, new ArrayList<String>(), null);

		// AllergyIntolerance  observationDate
		List<ParameterWrapper> allergyMapList = new ArrayList<ParameterWrapper>();
		allergyMapList.add(paramWrapper);
		dateParamWrapper = constructDateParameterWrapper(Arrays.asList("observationDate"), startDate, endDate);
		if (dateParamWrapper != null) {
			allergyMapList.add(dateParamWrapper);
		}

		OmopAllergyIntolerance omopAllergyIntoleranceMapper = OmopAllergyIntolerance.getInstance();
		omopAllergyIntoleranceMapper.searchWithParams(0, 0, allergyMapList, resources, new ArrayList<String>(), null);

		// Immunization
		List<ParameterWrapper> immunizationMapList = new ArrayList<ParameterWrapper>();
		immunizationMapList.add(paramWrapper);
		dateParamWrapper = constructDateParameterWrapper(Arrays.asList("immunizationDate"), startDate, endDate);
		if (dateParamWrapper != null) {
			immunizationMapList.add(dateParamWrapper);
		}

		OmopImmunization omopImmunizationMapper = OmopImmunization.getInstance();
		omopImmunizationMapper.searchWithParams(0, 0, immunizationMapList, resources, new ArrayList<String>(), null);

	}
}
