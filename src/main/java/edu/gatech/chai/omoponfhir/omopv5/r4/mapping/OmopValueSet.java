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
import java.util.List;
import java.util.Date;

import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.ValueSet;
import org.hl7.fhir.r4.model.Enumerations;
import org.hl7.fhir.r4.model.Enumerations.PublicationStatus;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.springframework.web.context.ContextLoaderListener;
import org.springframework.web.context.WebApplicationContext;

import ca.uhn.fhir.rest.api.SortSpec;
import edu.gatech.chai.omoponfhir.omopv5.r4.provider.CodeSystemResourceProvider;
import edu.gatech.chai.omoponfhir.omopv5.r4.provider.ValueSetResourceProvider;
import edu.gatech.chai.omopv5.dba.service.ConceptRelationshipService;
import edu.gatech.chai.omopv5.dba.service.ConceptService;
import edu.gatech.chai.omopv5.dba.service.ParameterWrapper;
import edu.gatech.chai.omopv5.dba.service.RelationshipService;
import edu.gatech.chai.omopv5.model.entity.Concept;
import edu.gatech.chai.omopv5.model.entity.ConceptRelationship;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OmopValueSet extends BaseOmopResource<ValueSet, ConceptRelationship, ConceptRelationshipService> {
    private static final Logger logger = LoggerFactory.getLogger(OmopValueSet.class);
	private static OmopValueSet omopValueSet = new OmopValueSet();
	private ConceptService conceptService;
    private ConceptRelationshipService conceptRelationshipService;
    private RelationshipService relationshipService;


    public OmopValueSet(WebApplicationContext context) {
        super(context, ConceptRelationship.class, ConceptRelationshipService.class, CodeSystemResourceProvider.getType());
        initialize(context);
        getSize();
    }

    public OmopValueSet() {
        super(ContextLoaderListener.getCurrentWebApplicationContext(), ConceptRelationship.class, ConceptRelationshipService.class, 
        ValueSetResourceProvider.getType());
    initialize(ContextLoaderListener.getCurrentWebApplicationContext());
    }

    private void initialize(WebApplicationContext context) {
        if (context != null) {
            conceptService = context.getBean(ConceptService.class);
            conceptRelationshipService = context.getBean(ConceptRelationshipService.class);
            relationshipService = context.getBean(RelationshipService.class);
        } else {
            logger.error("context must NOT be null");
        }
    }

    public static OmopValueSet getInstance() {
		return omopValueSet;
	}

    // @Override //constructs fhir with includes and revincludes 
    // public ValueSet constructResource(Long fhirId, ConceptRelationship entity, List<String> includes) {
    //     return null; 
	// }

    @Override
    public ValueSet constructFHIR(Long fhirId, ConceptRelationship conceptRelationship) {
        ValueSet valueSet = new ValueSet();
        ConceptRelationship cr = conceptRelationship.getId();
        valueSet.setId(new IdType(210000000000L));
        
        valueSet.setUrl("URl test");
        valueSet.setName("name of the valueset");
        valueSet.setTitle("title of the valueset");
        valueSet.setStatus(PublicationStatus.ACTIVE);
        valueSet.setDescription("description");

        return valueSet;
    }


    
    @Override
    public Long toDbase(ValueSet fhirResource, IdType fhirId) throws FHIRException {
        Long omopId = null;

		if (fhirId != null) {
			// update
			Long fhirIdLong = fhirId.getIdPartAsLong();
			if (fhirIdLong == null) {
				// Invalid fhirId.
				logger.error("Failed to get ValueSet.id as Long value");
				return null;
			}
			omopId = IdMapping.getOMOPfromFHIR(fhirIdLong, CodeSystemResourceProvider.getType());
		}

        // ConceptRelationship conceptRelationship = new ConceptRelationship();
        //concept_realtionship?? 

		// Long omopRecordId = null;
		// if (concept.getId() != null) {
		// 	omopRecordId = conceptService.update(concept).getId();
		// } else {
		// 	omopRecordId = conceptService.create(concept).getId();
		// }
		// Long fhirRecordId = IdMapping.getFHIRfromOMOP(omopRecordId, CodeSystemResourceProvider.getType());
		// return fhirRecordId;
        return null;
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
        case ValueSet.SP_IDENTIFIER:
            // String idValue = ((TokenParam) value).getValue();
            // String idSystemValue = ((TokenParam) value).getSystem();
            paramWrapper.setParameterType("Integer");
            paramWrapper.setParameters(Arrays.asList("conceptId"));
            paramWrapper.setOperators(Arrays.asList("="));
            // paramWrapper.setValues(Arrays.asList("%" + idSystemValue + ":%:%" + idValue + "%"));
            paramWrapper.setRelationship("or");
            mapList.add(paramWrapper);
            break;
        case ValueSet.SP_CODE: //just examples, add more later 
        case ValueSet.SP_DESCRIPTION:
        default: 
            mapList = null;
        }
        return mapList; 
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
    public ConceptRelationship constructOmop(Long omopId, ValueSet fhirResource) {
        // TODO Auto-generated method stub
        return null;
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
    
}