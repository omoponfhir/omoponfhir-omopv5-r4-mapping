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
import java.util.List;

import org.hl7.fhir.r4.model.BooleanType;
import org.hl7.fhir.r4.model.CodeSystem;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Enumerations;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Meta;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.CodeSystem.CodeSystemContentMode;
import org.hl7.fhir.r4.model.CodeSystem.ConceptDefinitionComponent;
import org.hl7.fhir.r4.model.Enumerations.PublicationStatus;
import org.hl7.fhir.r4.model.OperationOutcome.IssueSeverity;
import org.hl7.fhir.r4.model.OperationOutcome.IssueType;
import org.hl7.fhir.r4.model.Parameters.ParametersParameterComponent;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.context.ContextLoaderListener;
import org.springframework.web.context.WebApplicationContext;

import ca.uhn.fhir.rest.param.TokenParam;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import ca.uhn.fhir.rest.api.SortSpec;
import edu.gatech.chai.omoponfhir.omopv5.r4.provider.CodeSystemResourceProvider;
import edu.gatech.chai.omopv5.dba.service.ConceptService;
import edu.gatech.chai.omopv5.dba.service.ParameterWrapper;
import edu.gatech.chai.omopv5.dba.service.VocabularyService;
import edu.gatech.chai.omopv5.model.entity.Concept;
import edu.gatech.chai.omopv5.model.entity.Vocabulary;

/**
 * @author mfeng45 
 * @version 1.0 
 * This class will represent a CodeSystem resource and implements FHIR operations 
 */
public class OmopCodeSystem extends BaseOmopResource<CodeSystem, Vocabulary, VocabularyService> {

    private static final Logger logger = LoggerFactory.getLogger(OmopCodeSystem.class);
    private static OmopCodeSystem omopCodeSystem = new OmopCodeSystem();
    private VocabularyService vocabularyService;
    private ConceptService conceptService;

    public OmopCodeSystem(WebApplicationContext context) {
        super(context, Vocabulary.class, VocabularyService.class, CodeSystemResourceProvider.getType());
        initialize(context);

        getSize();
    }

    public OmopCodeSystem() {
        super(ContextLoaderListener.getCurrentWebApplicationContext(), Vocabulary.class, VocabularyService.class, 
            CodeSystemResourceProvider.getType());
        initialize(ContextLoaderListener.getCurrentWebApplicationContext());
    }

    private void initialize(WebApplicationContext context) {
        if (context != null) {
            vocabularyService = context.getBean(VocabularyService.class);
            conceptService = context.getBean(ConceptService.class);
        } else {
            logger.error("context must NOT be null");
        }
        getSize();
    }

    public static OmopCodeSystem getInstance() {
        return omopCodeSystem;
    }


    /**
     * This method will construct a FHIR resource from a vocabulary entry in the VOCABULARY table
     * @param fhirId Long value that represents the FHIR id of this resource
     * @param vocabulary Vocabulary reference that the CodeSystem is being created from 
     * @return Codesystem that represents information from a vocabulary and concept entry and other concepts that belong in the specific CodeSystem
     */
    @Override
    public CodeSystem constructFHIR(Long fhirId, Vocabulary vocabulary) { //important for entries in bundles 
        CodeSystem codeSystem = new CodeSystem();
        Concept codeSystemConcept = vocabulary.getVocabularyConcept();
        
        //TODO: maps to the correct id
        codeSystem.setId(new IdType(fhirId));
        
        Calendar calendar = Calendar.getInstance();
        if (codeSystemConcept.getValidStartDate() != null) {
            calendar.setTime(codeSystemConcept.getValidStartDate());
            codeSystem.setDate(calendar.getTime());
        }
        
        Meta metaData = new Meta();
        String version;
        if (vocabulary.getVocabularyVersion() != null) {
            version = vocabulary.getVocabularyVersion();
            metaData.setVersionId(version);
        } else {
            metaData.setVersionId("1");
        }
        metaData.setLastUpdated(calendar.getTime());
        codeSystem.setMeta(metaData);

        String uri;
        if (vocabulary.getVocabularyReference() != null) {
            uri = vocabulary.getVocabularyReference();
            codeSystem.setUrl(uri);
        } else {
            codeSystem.setUrl("none");
        }

        String name;
        if (vocabulary.getId() != null) {
            name = vocabulary.getId();
            codeSystem.setName(name);
        } else {
            codeSystem.setName("none");
        }

        String title = vocabulary.getVocabularyName();
        if (title != null && !(title.isEmpty())) {
            codeSystem.setTitle(title);
        } else {
            codeSystem.setTitle("none");
        }
        
        //static values for status and content-mode 
        PublicationStatus status = Enumerations.PublicationStatus.ACTIVE;
        codeSystem.setStatus(status);

        CodeSystemContentMode contentMode = CodeSystem.CodeSystemContentMode.COMPLETE;
        codeSystem.setContent(contentMode);
        
        //to contain all of the codes in the codeSystem
        List<ConceptDefinitionComponent> theConcept = new ArrayList<ConceptDefinitionComponent>();

        List<ParameterWrapper> params = new ArrayList<ParameterWrapper>();
        params.addAll(mapParameter (CodeSystem.SP_NAME, vocabulary.getId(), false));

        List<Concept> conceptIds = conceptService.searchWithParams(0, 100, params, null); //increase toIndex as needed
        
        for (Concept match: conceptIds) {
            ConceptDefinitionComponent code = new ConceptDefinitionComponent();
            code.setCode(match.getConceptCode());
            code.setDisplay(match.getConceptName());
            theConcept.add(code);
        }
        codeSystem.setConcept(theConcept);

        return codeSystem;
    }

    
    @Override
    public Long toDbase(CodeSystem codeSystem, IdType fhirId) throws FHIRException { //for read operation 
        Long omopId = null;
        Long retval;

        if (fhirId != null) {
            // update
            omopId = fhirId.getIdPartAsLong();
            if (omopId == null) {
                // Invalid fhirId.
                logger.error("Failed to get CodeSystem.id as Long value");
                return null;
            }
        }

        Vocabulary vocabulary = constructOmop(omopId, codeSystem);

        if (vocabulary.getId() != null) {
            retval = getMyOmopService().update(vocabulary).getIdAsLong();
        } else {
            retval = getMyOmopService().create(vocabulary).getIdAsLong();
        }
        return retval;
    }

    @Override
    public void removeDbase(Long id) {
        return; 
    }

    @Override
    public Long removeByFhirId (IdType fhirId) throws FHIRException {
        return null;
    }

    @Override
    public Long getSize() {
        return super.getSize();
    }

    @Override
    public Long getSize(List<ParameterWrapper> mapList) {
        return super.getSize(mapList);
    }

    @Override
    public void searchWithoutParams(int fromIndex, int toIndex, List<IBaseResource> listResources,
            List<String> includes, String sort) {
                super.searchWithoutParams(fromIndex, toIndex, listResources, includes, sort);
    }


    @Override
    public void searchWithParams(int fromIndex, int toIndex, List<ParameterWrapper> mapList, List<IBaseResource> listResources, 
        List<String> includes, String sort) {
            super.searchWithParams(fromIndex, toIndex, mapList, listResources, includes, sort);
    }

    /**
     * mapParameter: This maps the FHIR parameter to OMOP column name.
     * Create parameter map, which will be used later to construct predicate. The
         * predicate construction should depend on the DB schema. Therefore, we should
         * let our mapper to do any necessary mapping on the parameter(s). If the FHIR
         * parameter is not mappable, the mapper should return null, which will be
         * skipped when predicate is constructed.
     * 
     * @param parameter FHIR parameter name.
     * @param value     FHIR value for the parameter
     * @return returns ParameterWrapper class, which contains OMOP column name and
     *         value with operator.
    */
    @Override
    public List<ParameterWrapper> mapParameter(String parameter, Object value, boolean or) {
        List<ParameterWrapper> mapList = new ArrayList<ParameterWrapper>();
        ParameterWrapper paramWrapper = new ParameterWrapper();
        if (or) {
            paramWrapper.setUpperRelationship("or");
        }
        else {
            paramWrapper.setUpperRelationship("and");
        }
        switch (parameter) {
            case CodeSystem.SP_RES_ID:
                String id = ((TokenParam) value).getValue();
                paramWrapper.setParameterType("Long");
                paramWrapper.setParameters(Arrays.asList("id"));
                paramWrapper.setOperators(Arrays.asList("="));
                paramWrapper.setValues(Arrays.asList(id));
                paramWrapper.setRelationship("or");
                mapList.add(paramWrapper);
                break;
            case CodeSystem.SP_CODE:
                String code = (String) value;
                paramWrapper.setParameterType("String");
                paramWrapper.setParameters(Arrays.asList("conceptName"));
                paramWrapper.setOperators(Arrays.asList("like"));
                paramWrapper.setValues(Arrays.asList(code));
                paramWrapper.setRelationship("or");
                mapList.add(paramWrapper);
                break;
            case CodeSystem.SP_DATE: 
                String date = (String) value;
                paramWrapper.setParameterType("String");
                paramWrapper.setParameters(Arrays.asList("validStartDate"));
                paramWrapper.setOperators(Arrays.asList("like"));
                paramWrapper.setValues(Arrays.asList(date));
                paramWrapper.setRelationship("or");
                mapList.add(paramWrapper);
                break;
            case CodeSystem.SP_NAME:
                String name = (String) value;
                paramWrapper.setParameterType("String");
                paramWrapper.setParameters(Arrays.asList("vocabularyId"));
                paramWrapper.setOperators(Arrays.asList("like"));
                paramWrapper.setValues(Arrays.asList(name));
                paramWrapper.setRelationship("or");
                mapList.add(paramWrapper);
                break;
            case CodeSystem.SP_STATUS:
                paramWrapper.setParameterType("String");
                paramWrapper.setParameters(Arrays.asList("invalidReason"));
                paramWrapper.setOperators(Arrays.asList("="));
                paramWrapper.setValues(Arrays.asList("active"));
                paramWrapper.setRelationship("or");
                mapList.add(paramWrapper);
                break;
            case CodeSystem.SP_SYSTEM:
                String system = (String) value;
                paramWrapper.setParameterType("String");
                paramWrapper.setParameters(Arrays.asList("vocabularyReference")); 
                paramWrapper.setOperators(Arrays.asList("like"));
                paramWrapper.setValues(Arrays.asList(system));
                paramWrapper.setRelationship("or");
                mapList.add(paramWrapper);
                break;
            case CodeSystem.SP_TITLE:
                String title = (String) value;
                paramWrapper.setParameterType("String");
                paramWrapper.setParameters(Arrays.asList("vocabularyName")); 
                paramWrapper.setOperators(Arrays.asList("like"));
                paramWrapper.setValues(Arrays.asList(title));
                paramWrapper.setRelationship("or");
                mapList.add(paramWrapper);
                break;
            case CodeSystem.SP_URL:
                String url = (String) value;
                paramWrapper.setParameterType("String");
                paramWrapper.setParameters(Arrays.asList("vocabularyReference")); 
                paramWrapper.setOperators(Arrays.asList("like"));
                paramWrapper.setValues(Arrays.asList(url));
                paramWrapper.setRelationship("or");
                mapList.add(paramWrapper);
                break;
            case CodeSystem.SP_VERSION:
                String version = (String) value;
                paramWrapper.setParameterType("Long");
                paramWrapper.setParameters(Arrays.asList("vocabularyVersion")); 
                paramWrapper.setOperators(Arrays.asList("="));
                paramWrapper.setValues(Arrays.asList(version));
                paramWrapper.setRelationship("or");
                mapList.add(paramWrapper);
                break;
            case CodeSystem.SP_CONTENT_MODE:
                paramWrapper.setParameterType("String");
                paramWrapper.setParameters(Arrays.asList("invalidReason")); //TODO: find where to set the parameter 
                paramWrapper.setOperators(Arrays.asList("like"));
                paramWrapper.setValues(Arrays.asList("null"));
                paramWrapper.setRelationship("or");
                mapList.add(paramWrapper);
                break;
            default: 
                mapList = null;
        }
        return mapList; 
    }

    /**
     * TODO: constructs OMOP concept and vocabulary from a CodeSystem
     * @param omopId Long value representing the OMOP CDM Id 
     * @param codeSystem CodeSystem FHIR resource reference
     * @return Vocabulary that represents the CodeSystem
     */
    @Override
    public Vocabulary constructOmop(Long omopId, CodeSystem codeSystem) {
        //things to update: Concept, Vocabulary 

        Concept concept;
        Vocabulary vocabulary; 

        if (omopId != null) {
            vocabulary = getMyOmopService().findById(omopId);
            // concept = conceptService.findById(id);
        } else {
            vocabulary = new Vocabulary();
        }

        // if (concept == null) {
        //     concept = new Concept();
        // }
        // //set id 
        // Long id = 2100000000L;
        // concept.setId(id);
        // //set name 
        // String title = codeSystem.getTitle();
        // concept.setConceptName(title);
        // //set domain
        // concept.setDomainId("Metadata");
        // //set vocabulary id
        // concept.setVocabularyId("Vocabulary");
        // //set concept class id 
        // concept.setConceptClassId("Metadata");
        // //set concept code
        // Iterator<ConceptDefinitionComponent> codeIterator = codeSystem.getConcept().iterator();
        // if (codeIterator.hasNext()) {
        //     ConceptDefinitionComponent nextCode = codeIterator.next();
        //     if (!nextCode.getCode().isEmpty()) {
        //         concept.setConceptCode(nextCode.getCode());
        //     }
        // }
        // //set start date 
        // Calendar calendar = Calendar.getInstance();
        // if (codeSystem.getDate() != null) {
        //     calendar.setTime(codeSystem.getDate());
        //     // concept.setValidStartDate(codeSystem.getDate()); //has to be a date 
        // }
        // //set end date 
        // Calendar endDateCalendar = Calendar.getInstance();
        // endDateCalendar.set(2099, 12, 31);
        // concept.setValidEndDate(endDateCalendar.getTime());
        return null;

    }
    
    @Override
    public String constructOrderParams(SortSpec theSort) {
        if (theSort == null) {return null;}

        String direction;
        if (theSort.getOrder() != null) {
            direction = theSort.getOrder().toString();
        } else {
            direction = "ASC";
        }
        String orderParam = new String();
        orderParam = "id" + direction;
        return orderParam;
    }

   /**
    * FHIR operation $validate-code on CodeSystem
    * Validate that a coded value is in the code system. If the operation is not called at the instance level, 
    * one of the parameters "url" or "codeSystem" must be provided. The operation returns a result (true / false), 
    * an error message, and the recommended display for the code.
            * When invoking this operation, a client SHALL provide one (and only one) of the parameters 
            * (code+system, coding, or codeableConcept). Other parameters (including version and display) are 
            * optional.
    * @param id String OMOP reference from vocabulary.vocabulary_id to FHIR resource CodeSystem.name
    * @param code String OMOP reference from concept.concept_code to FHIR resource CodeSystem.concept.code 
    * @param version String OMOP reference from vocabulary.vocabulary_version to FHIR resource CodeSystem.version
    * @param display String OMOP reference from concept.concept_name to FHIR resource CodeSystem.concept.display
    * @return Parameters that represent the results of the validation through out parameters determined by the in parameters 
    */ 
    public Parameters validateCode(String id, String code, String version, String display) {
        String correctDisplay = "";
        boolean result = false; 
        Parameters retVal = new Parameters();

        List<ParameterWrapper> params = new ArrayList<ParameterWrapper>();
        params.addAll(mapParameter (CodeSystem.SP_NAME, id.substring(11), false));
        List<Concept> conceptIds = conceptService.searchWithParams(0, 100, params, null);

        ParametersParameterComponent displayParameter = new ParametersParameterComponent();
        displayParameter.setName("display");

        //there should only be one vocabulary that matches the details in the request 
        for (Concept c: conceptIds) {
            System.out.println("These are all the different concepts: " + c.getConceptName());
            if (c.getConceptCode().equals(code)) {
                correctDisplay = c.getConceptName();
                if (c.getConceptName().equals(display)) {
                    result = true;
                }
            }
            displayParameter.setValue(new StringType(correctDisplay)); 
        }
        //HTTP 400 Bad Request if the CodeSystem defined in the request does not exist
        if (conceptIds.size() == 0) {
            logger.error("$validate-code: trying to validate a code in which the CodeSystem does not exist.");
            OperationOutcome outcome = new OperationOutcome();
            CodeableConcept detailCode = new CodeableConcept();
            detailCode.setText("The CodeSystem " + id.substring(11) + " does not exist");
            outcome.addIssue().setSeverity(IssueSeverity.ERROR).setDetails(detailCode).setCode(IssueType.INVALID).setId("exception");
            throw new InvalidRequestException("This is an invalid request", outcome);
        }
        //HTTP 400 Bad Request if the code defined in the request does not exist in the CodeSystem
        else if (correctDisplay.isEmpty()) {
            logger.error("$validate-code: trying to validate a code that does not exist in the CodeSystem");
            OperationOutcome outcome = new OperationOutcome();
            CodeableConcept detailCode = new CodeableConcept();
            detailCode.setText("The code " + code + " is not found");
            outcome.addIssue().setSeverity(IssueSeverity.ERROR).setDetails(detailCode).setCode(IssueType.NOTFOUND).setId("exception");
            throw new InvalidRequestException("This is an invalid request", outcome);
        }

        

        retVal.addParameter(displayParameter);
        ParametersParameterComponent resultParameter = new ParametersParameterComponent();
        resultParameter.setName("result");
        resultParameter.setValue(new BooleanType(result));
        retVal.addParameter(resultParameter);

        //Error details, if result = false. If this is provided when result = true, the message carries hints and warnings
        ParametersParameterComponent messageParameter = new ParametersParameterComponent();
        messageParameter.setName("message");
        if (!result) {
            messageParameter.setValue(new StringType("The display " + display + " is incorrect"));
        } else {
            messageParameter.setValue(new StringType("The display " + display + " is correct for the " + id.substring(11) + " CodeSystem")); //todo - add appropriate hints and warnings 
        }
        retVal.addParameter(messageParameter);
        
        return retVal;
    }

    /**
     * FHIR operation $lookup on CodeSystem
     * TODO: add OperationOutcomes for bad requests (look at validate-code to see how this is done)
     * Given a code/system, or a Coding, get additional details about the concept, including definition, 
     * status, designations, and properties. One of the products of this operation is a full decomposition 
     * of a code from a structured terminology
            * When invoking this operation, a client SHALL provide both a system and a code, either using the 
            * system+code parameters, or in the coding parameter. Other parameters are optional.
     * @param code String OMOP reference from concept.concept_code to FHIR resource CodeSystem.concept.code 
     * @param system String OMOP reference from vocabulary.vocabulary_reference to FHIR resource CodeSystem.Url
     * @return Parameters that represent information from the underlying codeSystem definitions through out parameters determined by the in parameters 
     */
    public Parameters lookUp(String code, String system) {
        System.out.println("The code is " + code);
        System.out.println("The system is " + system);

        //setting the out parameters for the response   
        Parameters responseParameter = new Parameters();
                
        ParametersParameterComponent nameParameter = new ParametersParameterComponent();
        nameParameter.setName("name");
        ParametersParameterComponent versionParameter = new ParametersParameterComponent();
        versionParameter.setName("version");
        ParametersParameterComponent displayParameter = new ParametersParameterComponent();
        displayParameter.setName("display");
        ParametersParameterComponent abstractParameter = new ParametersParameterComponent();
        abstractParameter.setName("abstract");
        List<ParametersParameterComponent> listOfDesignation = new ArrayList<>();
        ParametersParameterComponent designationValueParameter = new ParametersParameterComponent();
        ParametersParameterComponent designationParameter = new ParametersParameterComponent();
        designationParameter.setPart(listOfDesignation);
        designationValueParameter.setName("value");
    
        List<ParameterWrapper> params = new ArrayList<ParameterWrapper>();

        params.addAll(mapParameter (CodeSystem.SP_URL, system, false));
        List<Vocabulary> vocabulary = vocabularyService.searchWithParams(0, 100, params, null);
        for (Vocabulary v: vocabulary) {
            System.out.println("These are the vocabs: " + v.getId());
            List<ParameterWrapper> param = new ArrayList<ParameterWrapper>();
            //TODO: somehow map the system from OMOP (ex. from: "http://loinc.org/downloads/loinc" to: "http://loinc.org")
            param.addAll(mapParameter (CodeSystem.SP_NAME, v.getId(), false)); 
            
            //these are all the concepts in a CodeSystem
            List<Concept> conceptIds = conceptService.searchWithParams(0, 100, param, null);
            for (Concept c: conceptIds) {
                System.out.println("These are the concepts: " + c.getConceptCode());
                if (c.getConceptCode().equals(code) && v.getVocabularyReference().equals(system)) {
                    nameParameter.setValue(new StringType(v.getId()));
                    responseParameter.addParameter(nameParameter);

                    versionParameter.setValue(new StringType(v.getVocabularyVersion()));
                    responseParameter.addParameter(versionParameter);

                    displayParameter.setValue(new StringType(c.getConceptName()));
                    responseParameter.addParameter(displayParameter);

                    if (c.getStandardConcept() != null && c.getStandardConcept().compareTo('S') == 0) {
                        abstractParameter.setValue(new StringType("true"));
                    } else {
                        abstractParameter.setValue(new StringType("false"));
                    }
                    responseParameter.addParameter(abstractParameter);

                    designationValueParameter.setValue(new StringType(c.getConceptName()));
                    listOfDesignation.add(designationValueParameter);
                    responseParameter.addParameter(designationParameter);
                }
            }
        }
        return responseParameter;
    }

}