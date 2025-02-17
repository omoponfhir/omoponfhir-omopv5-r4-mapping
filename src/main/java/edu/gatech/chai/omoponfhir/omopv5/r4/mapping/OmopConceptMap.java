package edu.gatech.chai.omoponfhir.omopv5.r4.mapping;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.hl7.fhir.r4.model.BooleanType;
import org.hl7.fhir.r4.model.CodeType;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.ConceptMap;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.Parameters.ParametersParameterComponent;
import org.hl7.fhir.exceptions.FHIRException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.context.ContextLoaderListener;
import org.springframework.web.context.WebApplicationContext;

import edu.gatech.chai.omoponfhir.omopv5.r4.utilities.CodeableConceptUtil;
import edu.gatech.chai.omopv5.dba.service.ConceptRelationshipService;
import edu.gatech.chai.omopv5.dba.service.ConceptService;
import edu.gatech.chai.omopv5.dba.service.ParameterWrapper;
import edu.gatech.chai.omopv5.model.entity.Concept;
import edu.gatech.chai.omopv5.model.entity.ConceptRelationship;

public class OmopConceptMap extends BaseOmopResource<ConceptMap, ConceptRelationship, ConceptRelationshipService> {

	private static final Logger logger = LoggerFactory.getLogger(OmopConceptMap.class);
	private static OmopConceptMap omopConceptMap = new OmopConceptMap();
	private ConceptService conceptService;
	
	public OmopConceptMap(WebApplicationContext context) {
		super(context, ConceptRelationship.class, ConceptRelationshipService.class, OmopConceptMap.FHIRTYPE);
		initialize(context);
	}

	public OmopConceptMap() {
		super(ContextLoaderListener.getCurrentWebApplicationContext(), ConceptRelationship.class, ConceptRelationshipService.class, OmopConceptMap.FHIRTYPE);
		initialize(ContextLoaderListener.getCurrentWebApplicationContext());
	}
	
	private void initialize(WebApplicationContext context) {
		conceptService = context.getBean(ConceptService.class);		
		
		// Get count and put it in the counts. 
		// We haven't created read here yet. So, do not get counts.
		// getSize();
	}
	
	public static String FHIRTYPE = "ConceptMap";

	@Override
	public Long toDbase(ConceptMap fhirResource, IdType fhirId) throws FHIRException {
		return null;
	}

	@Override
	public List<ParameterWrapper> mapParameter(String parameter, Object value, boolean or) {
		return null;
	}

	@Override
	public ConceptMap constructFHIR(Long fhirId, ConceptRelationship entity) {
		return null;
	}

	@Override
	public ConceptRelationship constructOmop(Long omopId, ConceptMap fhirResource) {
		return null;
	}

	public Parameters translateConcept(List<Coding> codings, String targetUri, String targetSystem) throws Exception {
		Parameters retVal = new Parameters();
		
		if (codings == null || codings.isEmpty()) return retVal;

		List<ConceptRelationship> conceptRealationships = new ArrayList<ConceptRelationship>();
		for (Coding coding : codings) {
			String system = coding.getSystem();
			String code = coding.getCode();
			String display = coding.getDisplay();

			String omopSrcVocab = "None";
			if (system != null && !system.isBlank()) {
				omopSrcVocab =fhirOmopVocabularyMap.getOmopVocabularyFromFhirSystemName(system);
			}

			String omopTargetVocab = "None";
			if (targetSystem != null && !targetSystem.isBlank()) {
				omopTargetVocab =fhirOmopVocabularyMap.getOmopVocabularyFromFhirSystemName(targetSystem);
			}

			List<Concept> omopSrcConcepts = new ArrayList<Concept>();
			// get OMOP concept for the source code.
			if (code != null && !code.isBlank()) {
				// Find concept_id for source coding.
				if (!"None".equals(omopSrcVocab)) {
					Concept omopSrcConcept = CodeableConceptUtil.getOmopConceptWithOmopVacabIdAndCode(conceptService, omopSrcVocab, code);
					if (omopSrcConcept != null) {
						omopSrcConcepts.add(omopSrcConcept);
					}
				} else {
					omopSrcConcepts = CodeableConceptUtil.getOmopConceptsWithOmopCode(conceptService, code);
				}
			} else if (display != null && !display.isBlank()) {
				// We do not have code. But, we have display. Use this to find the equivalent. 
				if ("None".equals(omopSrcVocab)) {
					omopSrcConcepts = CodeableConceptUtil.getOmopConceptsWithOmopConceptName(conceptService, display);
				} else {
					omopSrcConcepts = CodeableConceptUtil.getOmopConceptsWithOmopVocabIdAndtName(conceptService, omopSrcVocab, display);
				}
			}

			String relationshipId = null;
			if (!"None".equals(omopSrcVocab) && !"None".equals(omopTargetVocab)) {
				relationshipId = omopSrcVocab+" % "+omopTargetVocab+" eq";
			}

			String relationshipId2 = "Alias of";
	
			for (Concept omopSrcConcept : omopSrcConcepts) {
				if (relationshipId != null) {
					List<ParameterWrapper> params = new ArrayList<ParameterWrapper>();
					ParameterWrapper paramConceptId1 = new ParameterWrapper(
							"Long",
							Arrays.asList("concept1"),
							Arrays.asList("="),
							Arrays.asList(String.valueOf(omopSrcConcept.getId())),
							"or"
							);
					params.add(paramConceptId1);
					
					ParameterWrapper paramRelationshipId = new ParameterWrapper(
							"String",
							Arrays.asList("relationshipId"),
							Arrays.asList("like"),
							Arrays.asList(relationshipId),
							"or"
							);
					params.add(paramRelationshipId);
					
					conceptRealationships = getMyOmopService().searchWithParams(0, 0, params, null);
				}

				// look for another type of equivalent relationship. 
				// - NVDRS/SUDORS "Alias of"
				List<ParameterWrapper> params = new ArrayList<ParameterWrapper>();
				ParameterWrapper paramConceptId1 = new ParameterWrapper(
					"Long",
					Arrays.asList("concept1"),
					Arrays.asList("="),
					Arrays.asList(String.valueOf(omopSrcConcept.getId())),
					"or"
					);
				params.add(paramConceptId1);
				
				ParameterWrapper paramRelationshipId = new ParameterWrapper(
						"String",
						Arrays.asList("relationshipId"),
						Arrays.asList("="),
						Arrays.asList(relationshipId2),
						"or"
						);

				params.add(paramRelationshipId);

				// Add to the concept relationships.
				conceptRealationships.addAll(getMyOmopService().searchWithParams(0, 0, params, null));
			}
		}

		if (conceptRealationships.isEmpty()) {
			return retVal;
		}
		
		ParametersParameterComponent parameter = retVal.addParameter();
		parameter.setName("result");
		parameter.setValue(new BooleanType(true));
		
		parameter = retVal.addParameter();
		parameter.setName("match");
		
		ParametersParameterComponent partParameter = parameter.addPart();
		partParameter.setName("equivalence");
		partParameter.setValue(new CodeType("equivalent"));
		
		for (ConceptRelationship conceptRealationship: conceptRealationships) {
			// We found the mapping. Populate this information in Parameters resource.
			// concept_id_2 is the target concept.
			partParameter = parameter.addPart();
			partParameter.setName("concept");
			
			Concept targetConcept2 = conceptRealationship.getConcept2();
			Concept targetConcept = conceptService.findById(targetConcept2.getId());
			
			logger.debug("$translate: target concept obtained with vocabulary_id="+targetConcept.getVocabularyId());
			Coding targetCoding = CodeableConceptUtil.getCodingFromOmopConcept(targetConcept, getFhirOmopVocabularyMap());
			partParameter.setValue(targetCoding);
		}
		
		return retVal;
	}
}
