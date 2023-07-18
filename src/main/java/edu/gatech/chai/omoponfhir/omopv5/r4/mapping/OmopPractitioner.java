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
import java.util.Iterator;
import java.util.List;

import org.hl7.fhir.r4.model.Address;
import org.hl7.fhir.r4.model.HumanName;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Practitioner;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.Address.AddressUse;
import org.hl7.fhir.r4.model.Enumerations.AdministrativeGender;
import org.hl7.fhir.exceptions.FHIRException;
import org.springframework.web.context.ContextLoaderListener;
import org.springframework.web.context.WebApplicationContext;

import ca.uhn.fhir.rest.param.StringParam;
import ca.uhn.fhir.rest.param.TokenParam;
import edu.gatech.chai.omoponfhir.omopv5.r4.utilities.AddressUtil;
import edu.gatech.chai.omopv5.dba.service.CareSiteService;
import edu.gatech.chai.omopv5.dba.service.LocationService;
import edu.gatech.chai.omopv5.dba.service.ParameterWrapper;
import edu.gatech.chai.omopv5.dba.service.ProviderService;
import edu.gatech.chai.omopv5.model.entity.CareSite;
import edu.gatech.chai.omopv5.model.entity.Concept;
import edu.gatech.chai.omopv5.model.entity.Location;
import edu.gatech.chai.omopv5.model.entity.Provider;

public class OmopPractitioner extends BaseOmopResource<Practitioner, Provider, ProviderService> implements IResourceMapping<Practitioner, Provider>{

	private static OmopPractitioner omopPractitioner = new OmopPractitioner();
	
	private CareSiteService careSiteService;
	private LocationService locationService;	
	
	public OmopPractitioner(WebApplicationContext context) {
		super(context, Provider.class, ProviderService.class, OmopPractitioner.FHIRTYPE);
		initialize(context);
		
		// Get count and put it in the counts.
		getSize(true);
	}
	
	public OmopPractitioner() {
		super(ContextLoaderListener.getCurrentWebApplicationContext(), Provider.class, ProviderService.class, OmopPractitioner.FHIRTYPE);
		initialize(ContextLoaderListener.getCurrentWebApplicationContext());
	}
	
	private void initialize(WebApplicationContext context) {
		
		careSiteService = context.getBean(CareSiteService.class);
		locationService = context.getBean(LocationService.class);

	}
	
	public static OmopPractitioner getInstance() {
		return omopPractitioner;
	}
	
	public static String FHIRTYPE = "Practitioner";
	
	@Override
	public Practitioner constructFHIR(Long fhirId, Provider omopProvider) {
		Practitioner practitioner = new Practitioner(); //Assuming default active state
		practitioner.setId(new IdType(fhirId));
		
		CareSite omopCareSite = omopProvider.getCareSite();
		
		if(omopProvider.getProviderName() != null && !omopProvider.getProviderName().isBlank()) {
			String fullName = omopProvider.getProviderName(); 

			HumanName fhirName = new HumanName();
			fhirName.setText(fullName);

			String[] fullNamePart = fullName.split(",");
			if (fullNamePart.length > 0) {
				fhirName.setFamily(fullNamePart[0]);
				String [] givens = fullNamePart[1].split(" ");
				for (String given : givens) {
					fhirName.addGiven(given);
				}
			}

			List<HumanName> fhirNameList = new ArrayList<HumanName>();
			fhirNameList.add(fhirName);
			practitioner.setName(fhirNameList);
		}
		
		//TODO: Need practictioner telecom information

		//Set address
		if(omopCareSite != null && omopCareSite.getLocation() != null) {
			Long locationId = omopCareSite.getLocation().getId();
			Location location = locationService.findById(locationId);
			if (location != null) {
				practitioner.addAddress()
				.setUse(AddressUse.WORK) // default to work
				.addLine(location.getAddress1())
				.addLine(location.getAddress2())//WARNING check if mapping for lines are correct
				.setCity(location.getCity())
				.setPostalCode(location.getZip())
				.setState(location.getState());
			}
		}

		//Set gender
		if (omopProvider.getGenderConcept() != null) {
			String gName = omopProvider.getGenderConcept().getConceptName().toLowerCase(); 
			AdministrativeGender gender;
			try {
				gender = AdministrativeGender.fromCode(gName);
				practitioner.setGender(gender);
			} catch (FHIRException e) {
				e.printStackTrace();
			}
		}
		return practitioner;
	}

	@Override
	public Long toDbase(Practitioner practitioner, IdType fhirId) throws FHIRException {
		
		// If we have match in identifier, then we can update or create since
		// we have the patient. If we have no match, but fhirId is not null,
		// then this is update with fhirId. We need to do another search.
		Long omopId = null;
		if (fhirId != null) {
			// Search for this ID.
			omopId = IdMapping.getOMOPfromFHIR(fhirId.getIdPartAsLong(), OmopPractitioner.FHIRTYPE);
		}

		List<Identifier> identifiers = practitioner.getIdentifier();
		Provider alreadyIdentifiedProvider = null;
		for (Identifier identifier : identifiers) {
			if (identifier.getValue().isEmpty() == false) {
				String providerSourceValue = identifier.getValue();

				// See if we have existing patient
				// with this identifier.
				List<Provider> identifierProviders = getMyOmopService().searchByColumnString("providerSourceValue", providerSourceValue);
				if (identifierProviders != null && !identifierProviders.isEmpty()) {
					alreadyIdentifiedProvider = identifierProviders.get(0);
				}
				if (alreadyIdentifiedProvider != null) {
					omopId = alreadyIdentifiedProvider.getId();
					break;
				}
			}
		}
		
		Provider omopProvider = constructOmop(omopId, practitioner);
		
		Long omopRecordId = null;
		if (omopProvider.getId() != null) {
			omopRecordId = getMyOmopService().update(omopProvider).getId();
		} else {
			omopRecordId = getMyOmopService().create(omopProvider).getId();
		}
		return IdMapping.getFHIRfromOMOP(omopRecordId, OmopPractitioner.FHIRTYPE);
	}
	
//	@Override
//	public Long getSize() {
//		return providerService.getSize(Provider.class);
//	}
//
//	public Long getSize(Map<String, List<ParameterWrapper>> map) {
//		return providerService.getSize(Provider.class, map);
//	}
	
	public Location searchAndUpdateLocation (Address address, Location location) {
		if (address == null) return null;
		
		List<StringType> addressLines = address.getLine();
		if (!addressLines.isEmpty()) {
			String line1 = addressLines.get(0).getValue();
			String line2 = null;
			if (address.getLine().size() > 1)
				line2 = address.getLine().get(1).getValue();
			String zipCode = address.getPostalCode();
			String city = address.getCity();
			String state = address.getState();
			
			Location existingLocation = locationService.searchByAddress(line1, line2, city, state, zipCode);
			if (existingLocation != null) {
				return existingLocation;
			} else {
				// We will return new Location. But, if Location is provided,
				// then we update the parameters here.
				if (location != null) {
					location.setAddress1(line1);
					if (line2 != null)
						location.setAddress2(line2);
					location.setZip(zipCode);
					location.setCity(city);
					location.setState(state);
				} else {
					return new Location (line1, line2, city, state, zipCode);
				}
			}			
		}
		
		return null;
	}
	
	public CareSite searchAndUpdateCareSite(Address address) {
		Location location = AddressUtil.searchAndUpdate(locationService, address, null);
		if(location == null) return null;
		CareSite careSite = careSiteService.searchByLocation(location);
		if(careSite != null) {
			return careSite;
		} else {
			careSite = new CareSite();
			careSite.setLocation(location);
			careSiteService.create(careSite);
			return careSite;
		}
	}
	
	/**
	 * mapParameter: This maps the FHIR parameter to OMOP column name.
	 * 
	 * @param parameter
	 *            FHIR parameter name.
	 * @param value
	 *            FHIR value for the parameter
	 * @return returns ParameterWrapper class, which contains OMOP attribute name
	 *         and value with operator.
	 */
	public List<ParameterWrapper> mapParameter(String parameter, Object value, boolean or) {
		List<ParameterWrapper> mapList = new ArrayList<ParameterWrapper>();
		ParameterWrapper paramWrapper = new ParameterWrapper();
        if (or) paramWrapper.setUpperRelationship("or");
        else paramWrapper.setUpperRelationship("and");

		switch (parameter) {
		case Practitioner.SP_RES_ID:
			String practitionerId = ((TokenParam) value).getValue();
			paramWrapper.setParameterType("Long");
			paramWrapper.setParameters(Arrays.asList("id"));
			paramWrapper.setOperators(Arrays.asList("="));
			paramWrapper.setValues(Arrays.asList(practitionerId));
			paramWrapper.setRelationship("or");
			mapList.add(paramWrapper);
			break;
		case Practitioner.SP_FAMILY:
			// This is family name, which is string. use like.
			String familyString;
			if (((StringParam) value).isExact())
				familyString = ((StringParam) value).getValue();
			else
				familyString = "%"+((StringParam) value).getValue()+"%";
			paramWrapper.setParameterType("String");
			paramWrapper.setParameters(Arrays.asList("providerName"));
			paramWrapper.setOperators(Arrays.asList("like"));
			paramWrapper.setValues(Arrays.asList(familyString));
			paramWrapper.setRelationship("or");
			mapList.add(paramWrapper);
			break;
		case Practitioner.SP_GIVEN:
			String givenString;
			if (((StringParam) value).isExact())
				givenString = ((StringParam) value).getValue();
			else
				givenString = "%"+((StringParam) value).getValue()+"%";
			paramWrapper.setParameterType("String");
			paramWrapper.setParameters(Arrays.asList("providerName"));
			paramWrapper.setOperators(Arrays.asList("like"));
			paramWrapper.setValues(Arrays.asList(givenString));
			paramWrapper.setRelationship("or");
			mapList.add(paramWrapper);
			break;
		case Patient.SP_GENDER:
			//Not sure whether we should just search the encoded concept, or the source concept as well. Doing both for now.
			String genderValue = ((TokenParam) value).getValue();
			Long genderLongCode = null;
			//Setting the value to omop concept NULL if we cannot find an omopId
			try {
				genderLongCode = OmopConceptMapping.omopForAdministrativeGenderCode(genderValue);
			} catch (FHIRException e) {
				genderLongCode = OmopConceptMapping.ADMIN_NULL.getOmopConceptId();
			}
			paramWrapper.setParameterType("Long");
			paramWrapper.setParameters(Arrays.asList("genderSourceConcept", "genderSourceValue"));
			paramWrapper.setOperators(Arrays.asList("="));
			paramWrapper.setValues(Arrays.asList(genderLongCode.toString(),genderLongCode.toString()));
			paramWrapper.setRelationship("or");
			mapList.add(paramWrapper);
			break;
		default:
			mapList = null;
		}
		return mapList;
	}

	@Override
	public Provider constructOmop(Long omopId, Practitioner practitioner) {
		Provider omopProvider = null;

		if (omopId != null) {
			omopProvider = getMyOmopService().findById(omopId);
			if (omopProvider == null) {
				throw new FHIRException(practitioner.getId() + " does not exist");
			}
		} else {
			omopProvider = new Provider();
		}
		
		String providerSourceValue = null;

		//Set name
		Iterator<HumanName> practitionerIterator = practitioner.getName().iterator();
		if(practitionerIterator.hasNext()) {
			HumanName next = practitionerIterator.next();
			if (next.getText() != null) {
				omopProvider.setProviderName(next.getText());
			} else {
				String familyName = next.getFamily();
				String given = new String();
				List<StringType> givens = next.getGiven();
				for (StringType givenFhir : givens) {
					given = given.concat(" " + givenFhir.getValue());
				}

				String nameString = new String();
				if (familyName != null && !familyName.isBlank()) {
					nameString = familyName + ", " + given;
				} else {
					nameString = given;
				}

				if (!nameString.isBlank()) {
					omopProvider.setProviderName(nameString);
				}
			}
		}
		
		//Set address
		List<Address> addresses = practitioner.getAddress();
		Location retLocation = null;
		if (addresses != null && !addresses.isEmpty()) {
			Address address = addresses.get(0);

			// Now see if we have a caresite for this address. 
			CareSite careSite = searchAndUpdateCareSite(practitioner.getAddress().get(0));
			if (careSite == null)  {
				retLocation = AddressUtil.searchAndUpdate(locationService, address, null);
				if (retLocation != null) {
					careSite = new CareSite();
					careSite.setLocation(retLocation);
					careSiteService.create(careSite);
				}	
			}

			omopProvider.setCareSite(careSite);
		}
		
		//Set gender concept
		if (practitioner.getGender() != null) {
			String genderCode = practitioner.getGender().toCode();
			try {
				Concept genderConcept = new Concept(OmopConceptMapping.omopForAdministrativeGenderCode(genderCode));
				omopProvider.setGenderConcept(genderConcept);
			} catch (FHIRException e) {
				e.printStackTrace();
			}
		}
				
		Identifier identifier = practitioner.getIdentifierFirstRep();
		if (identifier.getValue() != null && !identifier.getValue().isEmpty()) {
			providerSourceValue = identifier.getValue();
			omopProvider.setProviderSourceValue(providerSourceValue);
		}

		return omopProvider;
	}
}
