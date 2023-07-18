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
import java.util.Set;

import org.hl7.fhir.r4.model.CodeType;
import org.hl7.fhir.r4.model.UriType;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.IdType;
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
import edu.gatech.chai.omopv5.dba.service.ParameterWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author mfeng45 
 * @version 2.4 
 * This class will implement REST Operations for a CodeSystem resource 
 */
public class CodeSystemResourceProvider implements IResourceProvider {
    private static final Logger logger = LoggerFactory.getLogger(ConceptMapResourceProvider.class);

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

    
    /**
     * TODO: (Id mapping) This method will add a new instance of a resource to the server
     * @param codeSystem CodeSystem that will be saved as a new resource to the server, allowing the server to give that resource an ID and version ID
     * @return MethodOutcome that contains the identity of the created resource 
     */
    @Create() 
    public MethodOutcome createCodeSystem(@ResourceParam CodeSystem codeSystem) {
        validateResource(codeSystem);
        Long id = null;
        try {
            id = getMyMapper().toDbase(codeSystem, null);

        } catch (FHIRException e) {
            e.printStackTrace();
        }
        return new MethodOutcome(new IdType(id));
    }


    /**
     * The delete operation retrieves a specific version of a resource with a given Id. It takes a single Id parameter annotated with an @IdParam annotation 
     * @param theId Id parameter which supplies the Id of the resource to delete
     */
    @Delete()
    public void deleteCodeSystem(@IdParam IdType theId) {
        System.out.println("This is the deleteCodeSystem");
        String FhirId = theId.getValue().substring(11); 
        if (getMyMapper().removeByFhirId(new IdType (FhirId)) <= 0) {
            throw new ResourceNotFoundException(FhirId);
        }
    }


    /**
     * The read operation retrieves a resource by Id
     * @param theId Id parameter which supplies the Id of the resource to read
     * @return CodeSystem that has a matching Id 
     */
    @Read()
    public CodeSystem readCodeSystem(@IdParam IdType theId) {
        String FhirId = theId.getValue().substring(11);
        CodeSystem retVal = myMapper.toFHIR(new IdType(FhirId));
        if (retVal == null) {
            throw new ResourceNotFoundException("The CodeSystem with id " + FhirId + " was not found.");
        }
        System.out.println("The translated id in readCodeSystem is " + FhirId + " the retVal is " + retVal.getName());
        return retVal;
    }
    

    /**
     * The search operation finds a resource by Id 
     * @param theCodeSystemId Id parameter which supplies the Id of the resource to found
     * @param theSort SortSpec preferred by the client 
     * @return Bundle resource that is used as a FHIR transaction
     */
    @Search()
    public IBundleProvider findCodeSystemById(
            @RequiredParam(name = CodeSystem.SP_RES_ID) TokenParam theCodeSystemId,
            @Sort SortSpec theSort,

            @IncludeParam(allow = {"CodeSystem:supplements"}) final Set<Include> theIncludes,
            @IncludeParam(reverse=true) final Set<Include> theReverseIncludes) {

        List<ParameterWrapper> paramList = new ArrayList<ParameterWrapper>();

        if (theCodeSystemId != null) {
            paramList.addAll(getMyMapper().mapParameter(CodeSystem.SP_RES_ID, theCodeSystemId, false));
        }

        String orderParams = getMyMapper().constructOrderParams(theSort);
        MyBundleProvider myBundleProvider = new MyBundleProvider(paramList, theIncludes, theReverseIncludes);
        myBundleProvider.setTotalSize(getTotalSize(paramList));
        myBundleProvider.setPreferredPageSize(preferredPageSize);
        myBundleProvider.setOrderParams(orderParams);
        return myBundleProvider;
    }

    

    /**
     * The search operation finds a resource by all or none parameters listed below 
     * @param theRequestDetails Contains details about the operation that is beginning, including details about the request type, URL, etc. Note that the RequestDetails has a generic Map 
	 * 		(see RequestDetails.getUserData()) that can be used to store information and state to be passed between methods in the consent service.
     * @param theCode A code defined in the code system Type: token Path: CodeSystem.concept.code
     * @param theDateRange The code system publication date Type: date Path: CodeSystem.date
     * @param theName Computationally friendly name of the code system Type: string Path: CodeSystem.name
     * @param theStatus The current status of the code system Type: token Path: CodeSystem.status
     * @param theSystem The system for any codes defined by this code system (same as 'url') Type: uri Path: CodeSystem.url
     * @param theTitle The human-friendly name of the code system Type: string Path: CodeSystem.title
     * @param theUrl The uri that identifies the code system Type: uri Path: CodeSystem.url
     * @param theVersion The business version of the code system Type: token Path: CodeSystem.version
     * @param theContentMode  not-present | example | fragment | complete | supplement Type: token Path: CodeSystem.content
     * @param theSort SortSpec preferred by the client 
     * @param theIncludes Includes specific linked resources as contained resources 
     * @param theReverseIncludes Rev_includes for the resource  
     * @return Bundle resource that is used as a FHIR transaction
     */
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
        @IncludeParam(allow = {"CodeSystem:supplements"}) final Set<Include> theIncludes,
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

        MyBundleProvider myBundleProvider = new MyBundleProvider(paramList, theIncludes, theReverseIncludes);
        myBundleProvider.setTotalSize(getTotalSize(paramList));
        myBundleProvider.setPreferredPageSize(preferredPageSize);
        myBundleProvider.setOrderParams(orderParams);
        return myBundleProvider;
    }



    /**
     * The update operation updates a specific resource instance (using its ID), and optionally accepts a version ID as well (which can be used to detect version conflicts)
     * @param theId Id parameter which supplies the Id of the resource to read
     * @param codeSystem CodeSystem to be updated 
     * @return MethodOutcome that contains the identity of the created resource 
     */
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


    /**
     * FHIR operation $lookup on CodeSystem
     * Given a code/system, or a Coding, get additional details about the concept, including definition, 
     * status, designations, and properties. One of the products of this operation is a full decomposition 
     * of a code from a structured terminology
            * When invoking this operation, a client SHALL provide both a system and a code, either using the 
            * system+code parameters, or in the coding parameter. Other parameters are optional.
     * @param theRequestDetails Contains details about the operation that is beginning, including details about the request type, URL, etc. Note that the RequestDetails has a generic Map 
	 * 		(see RequestDetails.getUserData()) that can be used to store information and state to be passed between methods in the consent service.
     * @param theSystem String OMOP reference from vocabulary.vocabulary_reference to FHIR resource CodeSystem.Url
     * @param theCode String OMOP reference from concept.concept_code to FHIR resource CodeSystem.concept.code 
     * @return Parameters that represent information from the underlying codeSystem definitions through out parameters determined by the in parameters 
     */
    @Operation(name = "$lookup", idempotent = true)
    public Parameters codeSystemLookupOperation(
        RequestDetails theRequestDetails,
        @OperationParam(name = "system") UriType theSystem, 
        @OperationParam(name = "code") CodeType theCode) {

            String mappingRequestUrl = theRequestDetails.getCompleteUrl();
            System.out.println(mappingRequestUrl);

            String code = null;
            if (theCode != null) code = theCode.getValue();

            String system = null;
            if (theSystem != null) system = theSystem.getValue();

            Parameters retval = new Parameters();
            retval = myMapper.lookUp(code, system);
            return retval;
        }


    
    /**
     * FHIR operation $validate-code on CodeSystem
	 * Validate that a coded value is in the code system. If the operation is not called at the instance level, 
	 * one of the parameters "url" or "codeSystem" must be provided. The operation returns a result (true / false), 
	 * an error message, and the recommended display for the code.
            * When invoking this operation, a client SHALL provide one (and only one) of the parameters 
            * (code+system, coding, or codeableConcept). Other parameters (including version and display) are 
            * optional.
     * @param theRequestDetails Contains details about the operation that is beginning, including details about the request type, URL, etc. Note that the RequestDetails has a generic Map 
	 * 		(see RequestDetails.getUserData()) that can be used to store information and state to be passed between methods in the consent service.
     * @param theCodeSystemId String OMOP reference from vocabulary.vocabulary_id to FHIR resource CodeSystem.name
     * @param theCode String OMOP reference from concept.concept_code to FHIR resource CodeSystem.concept.code 
     * @param theVersion String OMOP reference from vocabulary.vocabulary_version to FHIR resource CodeSystem.version
     * @param theDisplay String OMOP reference from concept.concept_name to FHIR resource CodeSystem.concept.display
     * @return Parameters that represent the results of the validation through out parameters determined by the in parameters 
     */
    @Operation(name = "$validate-code", idempotent = true)
    public Parameters codeSystemValidateCodeOperation(
        RequestDetails theRequestDetails,
        @IdParam IdType theCodeSystemId,
        @OperationParam(name = "code") CodeType theCode, 
        @OperationParam(name = "version") StringType theVersion,
        @OperationParam(name = "display") StringType theDisplay) {

        String code = "default";
        String version = "default"; 
        String display = "default"; 
        String id = "default"; 
        if (theCodeSystemId != null)
            {id = theCodeSystemId.getValueAsString();}
        if (theCode != null)
            {code = theCode.getValueAsString();}
        if (theVersion != null)
            {version = theVersion.getValueAsString();}
        if (theDisplay != null)
            {display = theDisplay.getValueAsString();}

        Parameters retval = new Parameters();
        retval = myMapper.validateCode(id, code, version, display);
        return retval;
    }

	
    
    /** 
     * The subsumes operation will test the relationship between code A and code B given the semantics of subsumption in the underlying CodeSystem
     * @param codeA the "A" code that is to be tested. If a code is provided, a system must be provided
     * @param codeB the "B" code that is to be tested. If a code is provided, a system must be provided
     * @param theSystem the code system in which subsumption testing is to be performed 
     * @return Parameters that represents the subsumption relationship between code A and code B
     */
    @Operation(name = "$subsumes", idempotent = true)
    public Parameters codeSystemSubsumesOperation(
     RequestDetails theRequestDetails, 
     @OperationParam(name = "codeA") CodeType theCodeA,
     @OperationParam(name = "codeB") CodeType theCodeB,
     @OperationParam(name = "system") UriType theSystem) {
        String codeA = "default";
        String codeB = "default"; 
        String system = "default";  
        if (theCodeA != null)
            {codeA = "CodeSystem/" + theCodeA.getValueAsString();}
        if (theCodeB != null)
            {codeB = "CodeSystem/" + theCodeB.getValueAsString();}
        if (theSystem != null)
            {system = theSystem.getValueAsString();}

        Parameters retval = new Parameters();
        retval = myMapper.subsumes(codeA, codeB, system);
        return retval;
     }

 
    /**
     * TODO - only if there are required items in OMOP CDM 
     * @param theCodeSystem The Code System to validate
     */
    private void validateResource(CodeSystem theCodeSystem) {
        // if (theCodeSystem.getCode().equals("Something already in the code system")) {
        //  OperationOutcome outcome = new OperationOutcome();
        //  CodeableConcept detailCode = new CodeableConcept();
        //  detailCode.setText("Code already exists in the concept_code field in Concept, Codes can't exist in more than one Code System.");
        //  outcome.addIssue().setSeverity(IssueSeverity.FATAL).setDetails(detailCode);
        //  throw new UnprocessableEntityException(FhirContext.forR4(), outcome);
        // }
    }

    class MyBundleProvider extends OmopFhirBundleProvider {
        Set<Include> theIncludes; 
        Set<Include> theReverseIncludes; 

        public MyBundleProvider(List<ParameterWrapper> paramList, Set<Include> theIncludes,
                Set<Include> theReverseIncludes) {
            super(paramList);
            setPreferredPageSize(preferredPageSize);
            this.theIncludes = theIncludes; 
            this.theReverseIncludes = theReverseIncludes; 
        }

        @Override
        public List<IBaseResource> getResources(int fromIndex, int toIndex) {
            List<IBaseResource> retv = new ArrayList<IBaseResource>();
            
            List<String> includes = new ArrayList<String>();
            if (theIncludes.contains(new Include("CodeSystem:supplements"))) {
				includes.add("CodeSystem:supplements");
			}

            System.out.println("SORT!!!!!! " + orderParams);
            if (paramList.size() == 0) {
                getMyMapper().searchWithoutParams(fromIndex, toIndex, retv, includes, orderParams);
            } else {
                getMyMapper().searchWithParams(fromIndex, toIndex, paramList, retv, includes, orderParams);
            }
            return retv;
        }
    }
}