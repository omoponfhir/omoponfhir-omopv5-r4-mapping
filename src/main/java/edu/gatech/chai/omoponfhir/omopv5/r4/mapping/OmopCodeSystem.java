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


import org.hl7.fhir.r4.model.CodeSystem;
import org.hl7.fhir.r4.model.CodeType;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Enumerations;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Meta;
import org.hl7.fhir.r4.model.CodeSystem.CodeSystemContentMode;
import org.hl7.fhir.r4.model.CodeSystem.ConceptDefinitionComponent;
import org.hl7.fhir.r4.model.Enumerations.PublicationStatus;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.context.ContextLoaderListener;
import org.springframework.web.context.WebApplicationContext;

import ca.uhn.fhir.rest.param.StringParam;
import ca.uhn.fhir.rest.param.TokenParam;
import ca.uhn.fhir.rest.annotation.Metadata;
import ca.uhn.fhir.rest.api.SortSpec;
import edu.gatech.chai.omoponfhir.omopv5.r4.provider.CodeSystemResourceProvider;
import edu.gatech.chai.omopv5.dba.service.ConceptService;
import edu.gatech.chai.omopv5.dba.service.ParameterWrapper;
import edu.gatech.chai.omopv5.dba.service.VocabularyService;
import edu.gatech.chai.omopv5.model.entity.Concept;
import edu.gatech.chai.omopv5.model.entity.Vocabulary;

public class OmopCodeSystem extends BaseOmopResource<CodeSystem, Vocabulary, VocabularyService> {

    private static final Logger logger = LoggerFactory.getLogger(OmopCodeSystem.class);
	private static OmopCodeSystem omopCodeSystem = new OmopCodeSystem();
	private VocabularyService vocabularyService;
    private ConceptService conceptService;

    public OmopCodeSystem(WebApplicationContext context) {
        super(context, Vocabulary.class, VocabularyService.class, CodeSystemResourceProvider.getType());
        initialize(context);

        getSize(); //get Count and put it in the counts 
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
        

        //get count
        getSize();
    }

    public static OmopCodeSystem getInstance() {
		return omopCodeSystem;
	}
    
    
    //constructs fhir and then instantiates the reference links 
    // @Override
    // public CodeSystem constructResource(Long fhirId, Vocabulary entity, List<String> includes) {
    //     return null; 
	// }


    //TODO
    @Override
    public CodeSystem constructFHIR(Long fhirId, Vocabulary vocabulary) { //important for entries in bundles 
        CodeSystem codeSystem = new CodeSystem();
        Concept codeSystemConcept = vocabulary.getVocabularyConcept();
        
        
        // codeSystem.setId(new IdType(fhirId));
        try {
            codeSystem.setId(new IdType(fhirId));
        } catch (Exception e) {
            e.printStackTrace();
            codeSystem.setId(new IdType(2000000001L));
        }

        Calendar calendar = Calendar.getInstance();
        if (codeSystemConcept.getValidStartDate() != null) {
            calendar.setTime(codeSystemConcept.getValidStartDate());
            codeSystem.setDate(calendar.getTime());
        }
        // String codeSystemSource = codeSystemConcept.
        Meta metaData = new Meta();
        
        String version;
        if (vocabulary.getVocabularyVersion() != null) {
            version = vocabulary.getVocabularyVersion();
            metaData.setVersionId(version);
        } else {
            metaData.setVersionId("1");
        }

        // metaData.setVersionId("1");
        // Calendar c = Calendar.getInstance(); //todo change this to the correct valid_start_date 
        metaData.setLastUpdated(calendar.getTime());
        // metaData.setSource("source of the codeSystem");
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
        
        // Date date = codeSystemConcept.getValidStartDate();
        
        
        PublicationStatus status = Enumerations.PublicationStatus.ACTIVE;
        codeSystem.setStatus(status);

        CodeSystemContentMode contentMode = CodeSystem.CodeSystemContentMode.COMPLETE;
        codeSystem.setContent(contentMode);
        

        //find concept in concept table for the code, then use vocabulary_id to find codeSystem
        // String vocabId = vocabulary.getId();
        // String conceptCode = codeSystemConcept.getConceptCode();

        // List<CodeSystem> entities = getMyOmopService().searchWithoutParams(fromIndex, toIndex, sort);

		// List<Concepts> entities = searchWithoutParams(fromIndex, toIndex, listResources, includes, sort);
        
        List<ConceptDefinitionComponent> theConcept = new ArrayList<ConceptDefinitionComponent>();
        ConceptDefinitionComponent code = new ConceptDefinitionComponent();
        // String conceptName = codeSystemConcept.getConceptName();
        // String conceptDisplay = codeSystemConcept.getConceptClassId();
        // String concpetCode = codeSystemConcept.getConceptCode();
        code.setCode("Code");
        code.setDisplay("Display");
        code.setDefinition("Definition");
        theConcept.add(code);
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

    /*
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
                paramWrapper.setParameters(Arrays.asList("invalidReason")); //TODO find where to set the parameter 
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

    /*
     * @OperationParam(name = "system") UriType theSystem, 
		@OperationParam(name = "code") CodeType theCode,
		@OperationParam(name = "version") StringType theVersion,
		@OperationParam(name = "coding") Coding theCoding, 
		@OperationParam(name = "date") DateTimeType theDate, 
		@OperationParam(name = "displayLanguage") CodeType theDisplayLanguage,
		@OperationParam(name = "property") CodeType theProperty)
     */
    public void lookUp(List<IBaseResource> resources, Long patientId, String code, String system, String version, String coding, Date date, String displayLanguage, String property) {
        return; 
    }

}