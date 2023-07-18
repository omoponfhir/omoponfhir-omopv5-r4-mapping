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
package edu.gatech.chai.omoponfhir.omopv5.r4.provider;

import java.util.ArrayList;
import java.util.List;
import java.util.Date;

import org.hl7.fhir.r4.model.CodeType;
import org.hl7.fhir.r4.model.UriType;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.DateTimeType;
import org.hl7.fhir.r4.model.IdType;
import org.springframework.web.context.ContextLoaderListener;
import org.springframework.web.context.WebApplicationContext;


import org.hl7.fhir.r4.model.ValueSet;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.instance.model.api.IBaseResource;

import ca.uhn.fhir.rest.annotation.Create;
import ca.uhn.fhir.rest.annotation.Delete;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.annotation.OperationParam;
import ca.uhn.fhir.rest.annotation.OptionalParam;
import ca.uhn.fhir.rest.annotation.Read;
import ca.uhn.fhir.rest.annotation.RequiredParam;
import ca.uhn.fhir.rest.annotation.ResourceParam;
import ca.uhn.fhir.rest.annotation.Search;
import ca.uhn.fhir.rest.annotation.Update;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.param.DateParam;
import ca.uhn.fhir.rest.param.QuantityParam;
import ca.uhn.fhir.rest.param.StringParam;
import ca.uhn.fhir.rest.param.TokenParam;
import ca.uhn.fhir.rest.param.UriParam;
import ca.uhn.fhir.rest.server.IResourceProvider;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import edu.gatech.chai.omoponfhir.omopv5.r4.mapping.OmopValueSet;
import edu.gatech.chai.omoponfhir.omopv5.r4.utilities.ThrowFHIRExceptions;
import edu.gatech.chai.omopv5.dba.service.ParameterWrapper;

/**
 * @author mfeng45 
 * @version 2.0 
 * This class will implement REST Operations for a ValueSet resource 
 */
public class ValueSetResourceProvider implements IResourceProvider {

	private WebApplicationContext myAppCtx;
	private OmopValueSet myMapper;
	private int preferredPageSize = 30;

	public ValueSetResourceProvider() {
		myAppCtx = ContextLoaderListener.getCurrentWebApplicationContext();
		myMapper = new OmopValueSet(myAppCtx);

		String pageSizeStr = myAppCtx.getServletContext().getInitParameter("preferredPageSize");
		if (pageSizeStr != null && pageSizeStr.isEmpty() == false) {
			int pageSize = Integer.parseInt(pageSizeStr);
			if (pageSize > 0) {
				preferredPageSize = pageSize;
			}
		}
	}

	@Override
	public Class<ValueSet> getResourceType() {
		return ValueSet.class;
	}

	public static String getType() {
		return "ValueSet";
	}

	public OmopValueSet getMyMapper() {
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
	 * TODO: This method will add a new instance of a resource to the server 
	 */
    @Create()
    public MethodOutcome createValueSet(@ResourceParam ValueSet valueSet) {
        validateResource(valueSet); //TODO - determine if we need to validate the resource 

        Long id = null;
        try {
            id = getMyMapper().toDbase(valueSet, null);
        } catch (FHIRException e) {
            e.printStackTrace();
        }

        return new MethodOutcome(new IdType(id));
    }

    @Delete()
    public void deleteValueSet(@IdParam IdType theId) {
        if (getMyMapper().removeByFhirId(theId) <= 0) {
            throw new ResourceNotFoundException(theId);
        }
    }


	@Read()
    public ValueSet readValueSet(@IdParam IdType theId) {
        ValueSet retVal = (ValueSet) getMyMapper().toFHIR(theId); 
        if (retVal == null) {
            throw new ResourceNotFoundException(theId);
        }
        return retVal;
    }


    @Search()
    public IBundleProvider findValueSetById(
        @RequiredParam(name = ValueSet.SP_RES_ID) TokenParam theValueSetId) {
        	
			List<ParameterWrapper> paramList = new ArrayList<ParameterWrapper>();
            if (theValueSetId != null) {
                paramList.addAll(getMyMapper().mapParameter(ValueSet.SP_RES_ID, theValueSetId, false));
            }

            MyBundleProvider myBundleProvider = new MyBundleProvider(paramList);
		    myBundleProvider.setTotalSize(getTotalSize(paramList));
		    myBundleProvider.setPreferredPageSize(preferredPageSize);
		    return myBundleProvider;
        }
    


    @Search(allowUnknownParams=true)
    public IBundleProvider findValueSetsByParams(RequestDetails theRequestDetails, 
			@OptionalParam(name = ValueSet.SP_IDENTIFIER) TokenParam theValueSetIdentifier,
			@OptionalParam(name = ValueSet.SP_CODE) TokenParam theCode,
			@OptionalParam(name = ValueSet.SP_CONTEXT) TokenParam theContext,
			@OptionalParam(name = ValueSet.SP_CONTEXT_QUANTITY) QuantityParam theContextQuantity,
			@OptionalParam(name = ValueSet.SP_CONTEXT_TYPE) TokenParam theContextType,
			@OptionalParam(name = ValueSet.SP_DATE) DateParam theDate,
			@OptionalParam(name = ValueSet.SP_DESCRIPTION) StringParam theDescription,
			@OptionalParam(name = ValueSet.SP_EXPANSION) UriParam theExpansion,
            @OptionalParam(name = ValueSet.SP_JURISDICTION) TokenParam theJurisdiction,
            @OptionalParam(name = ValueSet.SP_NAME) StringParam theName,
            @OptionalParam(name = ValueSet.SP_PUBLISHER) StringParam thePublisher,
			@OptionalParam(name = ValueSet.SP_REFERENCE) StringParam theReference,
            @OptionalParam(name = ValueSet.SP_STATUS) TokenParam theStatus,
            @OptionalParam(name = ValueSet.SP_TITLE) StringParam theTitle,
            @OptionalParam(name = ValueSet.SP_URL) UriParam theUrl,
            @OptionalParam(name = ValueSet.SP_VERSION) TokenParam theVersion) {
		
		    List<ParameterWrapper> paramList = new ArrayList<ParameterWrapper>();

			if (theValueSetIdentifier != null) {
				paramList.addAll(getMyMapper().mapParameter(ValueSet.SP_IDENTIFIER, theValueSetIdentifier, false));
			}
			if (theCode != null) {
				paramList.addAll(getMyMapper().mapParameter(ValueSet.SP_CODE, theCode, false));
			}
			if (theContext != null) {
				paramList.addAll(getMyMapper().mapParameter(ValueSet.SP_CONTEXT, theContext, false));
			}
			if (theContextQuantity != null) {
				paramList.addAll(getMyMapper().mapParameter(ValueSet.SP_CONTEXT_QUANTITY, theContextQuantity, false));
			}
			if (theContextType != null) {
				paramList.addAll(getMyMapper().mapParameter(ValueSet.SP_CONTEXT_TYPE, theContextType, false));
			}
			if (theDate != null) {
				paramList.addAll(getMyMapper().mapParameter(ValueSet.SP_DATE, theDate, false));
			}
			if (theDescription != null) {
				paramList.addAll(getMyMapper().mapParameter(ValueSet.SP_DESCRIPTION, theDescription, false));
			}
			if (theExpansion != null) {
				paramList.addAll(getMyMapper().mapParameter(ValueSet.SP_EXPANSION, theExpansion, false));
			}
			if (theJurisdiction != null) {
				paramList.addAll(getMyMapper().mapParameter(ValueSet.SP_JURISDICTION, theJurisdiction, false));
			}
			if (theName != null) {
				paramList.addAll(getMyMapper().mapParameter(ValueSet.SP_NAME, theName, false));
			}
			if (thePublisher != null) {
				paramList.addAll(getMyMapper().mapParameter(ValueSet.SP_PUBLISHER, thePublisher, false));
			}
			if (theReference != null) {
				paramList.addAll(getMyMapper().mapParameter(ValueSet.SP_REFERENCE, theReference, false));
			}
			if (theStatus != null) {
				paramList.addAll(getMyMapper().mapParameter(ValueSet.SP_STATUS, theStatus, false));
			}
			if (theTitle != null) {
				paramList.addAll(getMyMapper().mapParameter(ValueSet.SP_TITLE, theTitle, false));
			}
			if (theUrl != null) {
				paramList.addAll(getMyMapper().mapParameter(ValueSet.SP_URL, theUrl, false));
			}
			if (theVersion != null) {
				paramList.addAll(getMyMapper().mapParameter(ValueSet.SP_VERSION, theVersion, false));
			}

			MyBundleProvider myBundleProvider = new MyBundleProvider(paramList);
		    myBundleProvider.setTotalSize(getTotalSize(paramList));
		    myBundleProvider.setPreferredPageSize(preferredPageSize);
		    return myBundleProvider;	
	}




    @Update() 
    public MethodOutcome updateValueSet(@IdParam IdType theId, @ResourceParam ValueSet valueSet) {
        validateResource(valueSet);
        Long fhirId = null;
        try {
            fhirId = getMyMapper().toDbase(valueSet, theId);
        } catch (FHIRException e) {
            e.printStackTrace();
        }
        if (fhirId == null) {
            throw new ResourceNotFoundException(theId);
        }

        return new MethodOutcome();
    }


    
    
	/** 
	 * Used to create a simple collection of codes suitable for use for data entry or validation
	 * @param url a canonical reference to a value set
	 * @param valueSetVersion used to identify a specific version of the value set to be used when generating the expansion
	 * @param context the context of the value set 
	 * @param contextDirection if a context is provided, a context direction may also be provided
	 * @param filter a text filter that is applied to restrict the codes that are returned
	 * @param date is the date for which the expansion should be generated
	 * @return ValueSet
	 */
	@Operation(name = "$expand", idempotent = true)
    public ValueSet ValueSetExpandOperation(
		RequestDetails theRequestDetails, 
		@IdParam IdType theValueSetId, 
		@OperationParam(name = "url") UriType theUrl,
		@OperationParam(name = "valueSetVersion") StringType theValueSetVersion,
		@OperationParam(name = "context") UriType theContext, 
		@OperationParam(name = "contextDirection") CodeType theContextDirection, 
		@OperationParam(name = "filter") StringType theFilter,
		@OperationParam(name = "date") DateTimeType theDate) {
			
			if (theValueSetId == null) {
				ThrowFHIRExceptions.unprocessableEntityException("ValueSet Id must be present");
			}

			String url = null;
			if (theUrl != null) url = theUrl.getValue();

			String valueSetVersion = null;
			if (theValueSetVersion != null) valueSetVersion = theValueSetVersion.getValue();

			String filter = null;
			if (theFilter != null) filter = theFilter.getValue();

			Date date = null;
			if (theDate != null) date = theDate.getValue();

			// TODO: implement expand

        	return null;
		}


    // $validate-code operation 
    //TODO
	// @Operation(name = "$validate-code", idempotent = true)
    // public List<ParameterWrapper> ValueSetValidateCodeOperation (
    //     RequestDetails theRequestDetails, 
	// 	// @IdParam IdType theValueSetId, 
	// 	@OperationParam(name = "url") UriType theUrl,
    //     @OperationParam(name = "context") UriType theContext, 
	// 	@OperationParam(name = "valueSet") ValueSet theValueSet, 
	// 	@OperationParam(name = "valueSetVersion") StringType theValuseSetVersion,
	// 	@OperationParam(name = "code") CodeType theCode, 
    //     @OperationParam(name = "system") UriType theUri,
    //     @OperationParam(name = "systemVersion") StringType theSystemVersion,  
	// 	@OperationParam(name = "display") StringType theDisplay, 
	// 	@OperationParam(name = "coding") CodingType theCoding,
	// 	@OperationParam(name = "codeableConcpet") CodableConceptType theCodeableConcept) {
    //         return null;
    //     }
	

	
	/**
	 * TODO: only add this if it is needed 
	 * @param theValueSet The Code System to validate
	 */
	private void validateResource(ValueSet theValueSet) {
		return;
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

			if (paramList.size() == 0) {
				getMyMapper().searchWithoutParams(fromIndex, toIndex, retv, includes, null);
			} else {
				getMyMapper().searchWithParams(fromIndex, toIndex, paramList, retv, includes, null);
			}

			return retv;
		}
    }
}