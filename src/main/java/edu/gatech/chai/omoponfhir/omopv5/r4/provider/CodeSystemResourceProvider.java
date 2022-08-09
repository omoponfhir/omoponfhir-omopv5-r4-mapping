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
//note: imports from Patient
package edu.gatech.chai.omoponfhir.omopv5.r4.provider;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

import org.hl7.fhir.r4.model.CodeType;
import org.hl7.fhir.r4.model.UriType;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.DateTimeType;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Parameters.ParametersParameterComponent;
import org.hl7.fhir.r4.model.Parameters;
import org.springframework.web.context.ContextLoaderListener;
import org.springframework.web.context.WebApplicationContext;


import org.hl7.fhir.r4.model.CodeSystem;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.instance.model.api.IBaseResource;

import ca.uhn.fhir.model.api.Include;
import ca.uhn.fhir.rest.annotation.Create;
import ca.uhn.fhir.rest.annotation.Delete;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.IncludeParam;
import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.annotation.OperationParam;
import ca.uhn.fhir.rest.annotation.OptionalParam;
import ca.uhn.fhir.rest.annotation.Read;
import ca.uhn.fhir.rest.annotation.RequiredParam;
import ca.uhn.fhir.rest.annotation.ResourceParam;
import ca.uhn.fhir.rest.annotation.Search;
import ca.uhn.fhir.rest.annotation.Sort;
import ca.uhn.fhir.rest.annotation.Update;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.api.SortSpec;
import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.param.DateRangeParam;
import ca.uhn.fhir.rest.param.StringParam;
import ca.uhn.fhir.rest.param.TokenParam;
import ca.uhn.fhir.rest.param.UriParam;
import ca.uhn.fhir.rest.server.IResourceProvider;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import edu.gatech.chai.omoponfhir.omopv5.r4.mapping.OmopCodeSystem;
import edu.gatech.chai.omoponfhir.omopv5.r4.utilities.ThrowFHIRExceptions;
import edu.gatech.chai.omopv5.dba.service.ParameterWrapper;

public class CodeSystemResourceProvider implements IResourceProvider {

	private WebApplicationContext myAppCtx;
	private OmopCodeSystem myMapper;
	private int preferredPageSize = 30;

	public CodeSystemResourceProvider() {
		myAppCtx = ContextLoaderListener.getCurrentWebApplicationContext();
		myMapper = new OmopCodeSystem(myAppCtx);

		String pageSizeStr = myAppCtx.getServletContext().getInitParameter("preferredPageSize");
		if (pageSizeStr != null && pageSizeStr.isEmpty() == false) {
			int pageSize = Integer.parseInt(pageSizeStr);
			if (pageSize > 0) {
				preferredPageSize = pageSize;
			}
		}
	}

	/**
	 * The getResourceType method comes from IResourceProvider, and must be
	 * overridden to indicate what type of resource this provider supplies.
	 */
	@Override
	public Class<CodeSystem> getResourceType() {
		return CodeSystem.class;
	}

	public static String getType() {
		return "CodeSystem";
	}

	public OmopCodeSystem getMyMapper() {
		return myMapper;
	}

    public Integer getTotalSize(List<ParameterWrapper> paramList) {
        final Long totalSize;
		if (paramList.size() == 0) {
			totalSize = getMyMapper().getSize();
		} else {
			totalSize = getMyMapper().getSize(paramList);
		}

		return totalSize.intValue();
    }

	/*
	 * This method will add a new instance of a resource to the server 
	 */
    @Create() //TODO
    public MethodOutcome createCodeSystem(@ResourceParam CodeSystem codeSystem) {
        validateResource(codeSystem); //checks to see if the code already exists in the Code System

        Long id = null;
        try {
            id = getMyMapper().toDbase(codeSystem, null);

        } catch (FHIRException e) {
            e.printStackTrace();
        }

        return new MethodOutcome(new IdType(id));
    }


    @Delete()
    public void deleteCodeSystem(@IdParam IdType theId) {
        if (getMyMapper().removeByFhirId(theId) <= 0) {
            throw new ResourceNotFoundException(theId);
        }
    }


	@Read()
    public CodeSystem readCodeSystem(@IdParam IdType theId) {
        CodeSystem retVal = (CodeSystem) getMyMapper().toFHIR(theId); //toFHIR is called for the read operation 
        if (retVal == null) {
            throw new ResourceNotFoundException(theId);
        }
        return retVal;
    }


    @Search()
    public IBundleProvider findCodeSystemById(
		@RequiredParam(name = CodeSystem.SP_RES_ID) TokenParam theCodeSystemId,
		@Sort SortSpec theSort) {

            List<ParameterWrapper> paramList = new ArrayList<ParameterWrapper>();

            if (theCodeSystemId != null) {
                paramList.addAll(getMyMapper().mapParameter(CodeSystem.SP_RES_ID, theCodeSystemId, false));
            }
            String orderParams = getMyMapper().constructOrderParams(theSort);
            MyBundleProvider myBundleProvider = new MyBundleProvider(paramList);
		    myBundleProvider.setTotalSize(getTotalSize(paramList));
		    myBundleProvider.setPreferredPageSize(preferredPageSize);
		    myBundleProvider.setOrderParams(orderParams);
		    return myBundleProvider;
        }

    

    @Search(allowUnknownParams=true)
    public IBundleProvider findCodeSystemsByParams(RequestDetails theRequestDetails, 
		@OptionalParam(name = CodeSystem.SP_CODE) TokenParam theCode,
		@OptionalParam(name = CodeSystem.SP_DATE) DateRangeParam theDateRange,
		@OptionalParam(name = CodeSystem.SP_NAME) StringParam theName,
		@OptionalParam(name = CodeSystem.SP_STATUS) TokenParam theStatus,
		@OptionalParam(name = CodeSystem.SP_SYSTEM) StringParam theSystem,
		@OptionalParam(name = CodeSystem.SP_TITLE) StringParam theTitle,
		@OptionalParam(name = CodeSystem.SP_URL) UriParam theUrl,
		@OptionalParam(name = CodeSystem.SP_VERSION) TokenParam theVersion,
		@OptionalParam(name = CodeSystem.SP_CONTENT_MODE) TokenParam theContentMode,
		@Sort SortSpec theSort, 
		@IncludeParam() final Set<Include> theIncludes,
		@IncludeParam(reverse=true) final Set<Include> theReverseIncludes){
		
		List<ParameterWrapper> paramList = new ArrayList<ParameterWrapper>();
		if (theCode != null) {
			paramList.addAll(getMyMapper().mapParameter(CodeSystem.SP_CODE, theCode, false));
		}
		if (theContentMode != null) {
			paramList.addAll(getMyMapper().mapParameter(CodeSystem.SP_CONTENT_MODE, theContentMode, false));
		}
		if (theDateRange != null) {
			paramList.addAll(getMyMapper().mapParameter(CodeSystem.SP_DATE, theDateRange, false));
		}
		if (theName != null) {
			paramList.addAll(getMyMapper().mapParameter(CodeSystem.SP_NAME, theName, false));
		}
        if (theStatus != null) {
			paramList.addAll(getMyMapper().mapParameter(CodeSystem.SP_STATUS, theStatus, false));
		}
        if (theTitle != null) {
			paramList.addAll(getMyMapper().mapParameter(CodeSystem.SP_TITLE, theTitle, false));
		}
        if (theUrl != null) {
			paramList.addAll(getMyMapper().mapParameter(CodeSystem.SP_URL, theUrl, false));
		}
        if (theVersion != null) {
			paramList.addAll(getMyMapper().mapParameter(CodeSystem.SP_VERSION, theVersion, false));
		}

		String orderParams = getMyMapper().constructOrderParams(theSort);
		System.out.println("MYSORT!!! " + orderParams);
		MyBundleProvider myBundleProvider = new MyBundleProvider(paramList);
		myBundleProvider.setTotalSize(getTotalSize(paramList));
		myBundleProvider.setPreferredPageSize(preferredPageSize);
		myBundleProvider.setOrderParams(orderParams);
		return myBundleProvider;
	}



    @Update() 
    public MethodOutcome updateCodeSystem(@IdParam IdType theId, @ResourceParam CodeSystem codeSystem) {
        validateResource(codeSystem);
        Long fhirId = null;
        try {
            fhirId = getMyMapper().toDbase(codeSystem, theId);
        } catch (FHIRException e) {
            e.printStackTrace();
        }
        if (fhirId == null) {
            throw new ResourceNotFoundException(theId);
        }

        return new MethodOutcome();
    }


    /*
	 * $lookup operation for a codeSystem - return type? 
	 */
    @Operation(name = "$lookup", idempotent = true)
    public Parameters codeSystemLookupOperation(
		RequestDetails theRequestDetails,
		@OperationParam(name = "system") UriType theSystem, 
		@OperationParam(name = "code") CodeType theCode,
		@OperationParam(name = "version") StringType theVersion,
		@OperationParam(name = "coding") Coding theCoding, 
		@OperationParam(name = "date") DateTimeType theDate, 
		@OperationParam(name = "displayLanguage") CodeType theDisplayLanguage,
		@OperationParam(name = "property") CodeType theProperty) {

			String code = null;
			if (theCode != null) code = theCode.getValue();

			String system = null;
			if (theSystem != null) system = theSystem.getValue();

			String version = null;
			if (theVersion != null) version = theVersion.getValue();

			String coding = null;
			if (theCoding != null) coding = theCoding.getSystem();

			Date date = null;
			if (theDate != null) date = theDate.getValue();

			String displayLanguage = null;
			if (theDisplayLanguage != null) displayLanguage = theDisplayLanguage.getValue();

			String property = null;
			if (theProperty != null) property = theProperty.getValue();
		

			Parameters responseParameter = new Parameters();
				
			ParametersParameterComponent nameParameter = new ParametersParameterComponent();
			nameParameter.setName("name");
			nameParameter.setValue(new StringType(""));
			responseParameter.addParameter(nameParameter);

			ParametersParameterComponent versionParameter = new ParametersParameterComponent();
			versionParameter.setName("version");
			versionParameter.setValue(new StringType(""));
			responseParameter.addParameter(versionParameter);

			ParametersParameterComponent displayParameter = new ParametersParameterComponent();
			displayParameter.setName("display");
			displayParameter.setValue(new StringType(""));
			responseParameter.addParameter(displayParameter);

			//designation paremeter 
			List<ParametersParameterComponent> listOfDesignation = new ArrayList<>();
			ParametersParameterComponent designationLanguageParameter = new ParametersParameterComponent();
			designationLanguageParameter.setName("language");
			designationLanguageParameter.setValue(new CodeType());
			ParametersParameterComponent designationUseParameter = new ParametersParameterComponent();
			designationUseParameter.setName("use");
			designationUseParameter.setValue(new Coding("","",""));
			ParametersParameterComponent designationValueParameter = new ParametersParameterComponent();
			designationValueParameter.setName("value");
			designationValueParameter.setValue(new StringType(""));
			
			listOfDesignation.add(designationLanguageParameter);
			listOfDesignation.add(designationUseParameter);
			listOfDesignation.add(designationValueParameter);
			
			ParametersParameterComponent designationParameter = new ParametersParameterComponent();
			designationParameter.setPart(listOfDesignation);

			responseParameter.addParameter(designationParameter);

			//property 
			List<ParametersParameterComponent> listOfProperty = new ArrayList<>();
			ParametersParameterComponent propertyCodeParameter = new ParametersParameterComponent();
			propertyCodeParameter.setName("code");
			propertyCodeParameter.setValue(new CodeType());

			listOfProperty.add(propertyCodeParameter);

			ParametersParameterComponent propertyParemeter = new ParametersParameterComponent();
			propertyParemeter.setPart(listOfProperty);

			responseParameter.addParameter(propertyParemeter);

			//property subproperty 
			List<ParametersParameterComponent> listOfPropertySub = new ArrayList<>();
			ParametersParameterComponent propertySubCodeParameter = new ParametersParameterComponent();
			propertySubCodeParameter.setName("code");
			propertySubCodeParameter.setValue(new CodeType());
			ParametersParameterComponent propertySubDescriptionParameter = new ParametersParameterComponent();
			propertySubDescriptionParameter.setName("value");
			propertySubDescriptionParameter.setValue(new StringType(""));

			listOfProperty.add(propertySubCodeParameter);
			listOfProperty.add(propertySubDescriptionParameter);
			
			ParametersParameterComponent propertySubParameter = new ParametersParameterComponent();
			propertySubParameter.setPart(listOfPropertySub);

			responseParameter.addParameter(propertySubParameter);

			return responseParameter;
		}


	// @Operation(name = "$validate-code", idempotent = true)
	// public IBundleProvider codeSystemValidateCodeOperation() {
	// 	return null;
	// }

	// @Operation(name = "$subsumes", idempotent = true)
	// public ParameterWrapper codeSystemSubsumesOperation(
	// 	RequestDetails theRequestDetails, 
	// 	@OperationParam(name = "codeA") CodeType theCodeA,
	// 	@OperationParam(name = "codeB") CodeType theCodeB,
	// 	@OperationParam(name = "system") UriType theSystem, 
	// 	@OperationParam(name = "version") StringType theVersion,
	// 	@OperationParam(name = "codingA") Coding theCodingA, 
	// 	@OperationParam(name = "codingB") Coding theCodingB) {
	// 		return null; 
	// 	}

	// @Operation(name = "$find-matches", idempotent = true)


	//TODO - only if there are required items in omop cdm 
	/**
	 * Checks if the concept.code exists in concept.concept_code
	 * 
	 * @param theCodeSystem The Code System to validate
	 */
	private void validateResource(CodeSystem theCodeSystem) {
		// if (theCodeSystem.getCode().equals("Something already in the code system")) { //TODO: parse through other codes in the Concept table
		// 	OperationOutcome outcome = new OperationOutcome();
		// 	CodeableConcept detailCode = new CodeableConcept();
		// 	detailCode.setText("Code already exists in the concept_code field in Concept, Codes can't exist in more than one Code System.");
		// 	outcome.addIssue().setSeverity(IssueSeverity.FATAL).setDetails(detailCode);
		// 	throw new UnprocessableEntityException(FhirContext.forR4(), outcome);
		// }
	}

	class MyBundleProvider extends OmopFhirBundleProvider {

		public MyBundleProvider(List<ParameterWrapper> paramList) {
			super(paramList);
			setPreferredPageSize(preferredPageSize);
		}


		@Override
		public List<IBaseResource> getResources(int fromIndex, int toIndex) {
			List<IBaseResource> retv = new ArrayList<IBaseResource>();
			List<String> includes = new ArrayList<String>();

			System.out.println("SORT!!!!!! " + orderParams);
			if (paramList.size() == 0) {
				getMyMapper().searchWithoutParams(fromIndex, toIndex, retv, includes, null);
			} else {
				getMyMapper().searchWithParams(fromIndex, toIndex, paramList, retv, includes, null);
			}
			return retv;
		}

	}
}