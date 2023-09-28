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
import java.util.Date;
import java.util.List;

import org.hl7.fhir.r4.model.Attachment;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.DocumentReference;
import org.hl7.fhir.r4.model.DocumentReference.DocumentReferenceContentComponent;
import org.hl7.fhir.r4.model.DocumentReference.DocumentReferenceContextComponent;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.Enumerations.DocumentReferenceStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.exceptions.FHIRException;
import org.springframework.web.context.ContextLoaderListener;
import org.springframework.web.context.WebApplicationContext;

import ca.uhn.fhir.rest.param.DateRangeParam;
import ca.uhn.fhir.rest.param.ReferenceParam;
import ca.uhn.fhir.rest.param.TokenParam;
import edu.gatech.chai.omoponfhir.omopv5.r4.utilities.CodeableConceptUtil;
import edu.gatech.chai.omoponfhir.omopv5.r4.model.MyDocumentReference;
import edu.gatech.chai.omoponfhir.omopv5.r4.utilities.DateUtil;
import edu.gatech.chai.omoponfhir.omopv5.r4.utilities.ThrowFHIRExceptions;
import edu.gatech.chai.omopv5.dba.service.ConceptService;
import edu.gatech.chai.omopv5.dba.service.FPersonService;
import edu.gatech.chai.omopv5.dba.service.NoteService;
import edu.gatech.chai.omopv5.dba.service.ParameterWrapper;
import edu.gatech.chai.omopv5.dba.service.ProviderService;
import edu.gatech.chai.omopv5.dba.service.VisitOccurrenceService;
import edu.gatech.chai.omopv5.model.entity.Concept;
import edu.gatech.chai.omopv5.model.entity.FPerson;
import edu.gatech.chai.omopv5.model.entity.Note;
import edu.gatech.chai.omopv5.model.entity.Provider;
import edu.gatech.chai.omopv5.model.entity.VisitOccurrence;

/***
 * DocumentReference
 * 
 * @author mc142
 * 
 *         maps to Note table
 *
 */
public class OmopDocumentReference extends BaseOmopResource<DocumentReference, Note, NoteService> {
	private static final Logger logger = LoggerFactory.getLogger(OmopDocumentReference.class);

	private static OmopDocumentReference omopDocumentReference = new OmopDocumentReference();
	private ConceptService conceptService;
	private FPersonService fPersonService;
	private ProviderService providerService;
	private VisitOccurrenceService visitOccurrenceService;

	public OmopDocumentReference() {
		super(ContextLoaderListener.getCurrentWebApplicationContext(), Note.class, NoteService.class,
				OmopDocumentReference.FHIRTYPE);
		initialize(ContextLoaderListener.getCurrentWebApplicationContext());
	}

	public OmopDocumentReference(WebApplicationContext context) {
		super(context, Note.class, NoteService.class, OmopDocumentReference.FHIRTYPE);
		initialize(context);
		
		// Get count and put it in the counts.
		getSize(true);
	}

	private void initialize(WebApplicationContext context) {
		conceptService = context.getBean(ConceptService.class);
		fPersonService = context.getBean(FPersonService.class);
		providerService = context.getBean(ProviderService.class);
		visitOccurrenceService = context.getBean(VisitOccurrenceService.class);
	}

	public static OmopDocumentReference getInstance() {
		return OmopDocumentReference.omopDocumentReference;
	}

	public static String FHIRTYPE = "DocumentReference";

	@Override
	public Long toDbase(DocumentReference fhirResource, IdType fhirId) throws FHIRException {
		Long omopId = null;
		if (fhirId != null) {
			// Update
			omopId = IdMapping.getOMOPfromFHIR(fhirId.getIdPartAsLong(), OmopDocumentReference.FHIRTYPE);
		}
		
		Note note = constructOmop(omopId, fhirResource);
		
		Long OmopRecordId = null;
		if (omopId == null) {
			OmopRecordId = getMyOmopService().create(note).getId();
		} else {
			OmopRecordId = getMyOmopService().update(note).getId();
		}
		
		return IdMapping.getFHIRfromOMOP(OmopRecordId, OmopDocumentReference.FHIRTYPE);
	}

	@Override
	public List<ParameterWrapper> mapParameter(String parameter, Object value, boolean or) {
		List<ParameterWrapper> mapList = new ArrayList<ParameterWrapper>();
		ParameterWrapper paramWrapper = new ParameterWrapper();
        if (or) paramWrapper.setUpperRelationship("or");
        else paramWrapper.setUpperRelationship("and");

		switch (parameter) {
		case DocumentReference.SP_RES_ID:
			String documentReferenceId = ((TokenParam) value).getValue();
			paramWrapper.setParameterType("Long");
			paramWrapper.setParameters(Arrays.asList("id"));
			paramWrapper.setOperators(Arrays.asList("="));
			paramWrapper.setValues(Arrays.asList(documentReferenceId));
			paramWrapper.setRelationship("or");
			mapList.add(paramWrapper);
			break;
		case DocumentReference.SP_ENCOUNTER:
			Long fhirId = ((ReferenceParam) value).getIdPartAsLong();
			Long omopVisitOccurrenceId = IdMapping.getOMOPfromFHIR(fhirId, OmopDocumentReference.FHIRTYPE);
			
			if (omopVisitOccurrenceId != null) {
				paramWrapper.setParameterType("Long");
				paramWrapper.setParameters(Arrays.asList("visitOccurrence.id"));
				paramWrapper.setOperators(Arrays.asList("="));
				paramWrapper.setValues(Arrays.asList(String.valueOf(omopVisitOccurrenceId)));
				paramWrapper.setRelationship("or");
				mapList.add(paramWrapper);
			}
			break;
		case DocumentReference.SP_DATE:
			DateRangeParam dateRangeParam = ((DateRangeParam) value);
			DateUtil.constructParameterWrapper(dateRangeParam, "noteDate", paramWrapper, mapList);			
			break;
		case DocumentReference.SP_TYPE:
			String system = ((TokenParam) value).getSystem();
			String code = ((TokenParam) value).getValue();
			
			if ((system == null || system.isEmpty()) && (code == null || code.isEmpty()))
				break;
			
			String omopVocabulary = "None";
			if (system != null && !system.isEmpty()) {
				try {
					omopVocabulary = OmopCodeableConceptMapping.omopVocabularyforFhirUri(system);
					// DocumentReference is mapped from Note table, which uses Note Type concept code.
					// If this is LOINC, we can try to convert this to Note Type.
					if ("LOINC".equals(omopVocabulary)) {
						// Get concept id.
						Concept loincConcept = CodeableConceptUtil.getOmopConceptWithOmopVacabIdAndCode(conceptService, omopVocabulary, code);
						if (loincConcept != null) {
							Long omopConceptId = OmopNoteTypeMapping.getOmopConceptIdFor(loincConcept.getId());
							if (!omopConceptId.equals(0L)) {
								// We found the mapping. Use this to compare with concept id.
								paramWrapper.setParameterType("Long");
								paramWrapper.setParameters(Arrays.asList("noteTypeConcept.id"));
								paramWrapper.setOperators(Arrays.asList("="));
								paramWrapper.setValues(Arrays.asList(String.valueOf(omopConceptId)));
								paramWrapper.setRelationship("and");
								mapList.add(paramWrapper);
								break;
							}
						} else {
							logger.warn("Provided LOINC code (" + code + ") could not be found from Concept table");
						}
					}
				} catch (FHIRException e) {
					e.printStackTrace();
				}
			} 
			
			paramWrapper.setParameterType("String");
			if ("None".equals(omopVocabulary) && code != null && !code.isEmpty()) {
				paramWrapper.setParameters(Arrays.asList("noteTypeConcept.conceptCode"));
				paramWrapper.setOperators(Arrays.asList("="));
				paramWrapper.setValues(Arrays.asList(code));
			} else if (!"None".equals(omopVocabulary) && (code == null || code.isEmpty())) {
				paramWrapper.setParameters(Arrays.asList("noteTypeConcept.vocabularyId"));
				paramWrapper.setOperators(Arrays.asList("="));
				paramWrapper.setValues(Arrays.asList(omopVocabulary));				
			} else {
				paramWrapper.setParameters(Arrays.asList("noteTypeConcept.vocabularyId", "noteTypeConcept.conceptCode"));
				paramWrapper.setOperators(Arrays.asList("=","="));
				paramWrapper.setValues(Arrays.asList(omopVocabulary, code));
			}
			paramWrapper.setRelationship("and");
			mapList.add(paramWrapper);
			break;
		case "Patient:" + Patient.SP_RES_ID:
			addParamlistForPatientIDName(parameter, (String)value, paramWrapper, mapList);
			break;
		case "Patient:" + Patient.SP_NAME:
			addParamlistForPatientIDName(parameter, (String)value, paramWrapper, mapList);
			break;
		default:
			mapList = null;
		}
		
        return mapList;
	}

	@Override
	public Note constructOmop(Long omopId, DocumentReference fhirResource) {
		Note note = null;
		if (omopId == null) {
			// Create
			note = new Note();
		} else {
			// Update
			note = getMyOmopService().findById(omopId);
			if (note == null) {
				throw new FHIRException(fhirResource.getId() + " does not exist");
			}
		}
		
		// get type
		CodeableConcept typeCodeableConcept = fhirResource.getType();
		Coding loincCoding = null;
		Concept typeOmopConcept = null;
		Concept typeFhirConcept = null;
		if (typeCodeableConcept != null && !typeCodeableConcept.isEmpty()) {
			for (Coding coding: typeCodeableConcept.getCoding()) {
				try {
					typeFhirConcept = CodeableConceptUtil.getOmopConceptWithFhirConcept(conceptService, coding);
				} catch (FHIRException e) {
					typeFhirConcept = null;
					e.printStackTrace();
				}
				if ("http://loinc.org".equals(coding.getSystem()) ||
						"urn:oid:2.16.840.1.113883.6.1".equals(coding.getSystem())) {
					loincCoding = coding;
					break;
				}
			}
			
			if (typeFhirConcept == null) {
				ThrowFHIRExceptions.unprocessableEntityException("The type codeableconcept is not recognized");
			}
			
			if (loincCoding != null) {
				// We found loinc coding. See if we can convert to Note Type concept.
				Long typeOmopConceptId = OmopNoteTypeMapping.getOmopConceptIdFor(typeFhirConcept.getId());
				typeOmopConcept = conceptService.findById(typeOmopConceptId);
			}
			
			if (typeOmopConcept == null) {
				// We couldn't get Note Type concept. Just use the fhirConcept as is.
				typeOmopConcept = typeFhirConcept;
			}
			
			note.setNoteTypeConcept(typeOmopConcept);
		} else {
			ThrowFHIRExceptions.unprocessableEntityException("The type codeableconcept cannot be null");
		}
		
		// Get patient
		Reference subject = fhirResource.getSubject();
		if (subject == null || subject.isEmpty()) {
			ThrowFHIRExceptions.unprocessableEntityException("Subject(Patient) must be provided");
		}
		
		if (subject.getReferenceElement().getResourceType().equals(OmopPatient.FHIRTYPE)) {
			// get patient ID.
			Long patientId = subject.getReferenceElement().getIdPartAsLong();
			Long omopPersonId = IdMapping.getOMOPfromFHIR(patientId, OmopPatient.FHIRTYPE);
			FPerson fPerson = fPersonService.findById(omopPersonId);
			if (fPerson == null) {
				ThrowFHIRExceptions.unprocessableEntityException("Patient does not exist");
			}
			
			note.setFPerson(fPerson);
		} else {
			ThrowFHIRExceptions.unprocessableEntityException("Only Patient is supported for subject");
		}
		
		// get indexed.
		Date indexedDate = fhirResource.getDate();
		if (indexedDate != null) {
			note.setNoteDate(indexedDate);
			
//			SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");
//			note.setNoteDateTime(timeFormat.format(indexedDate));
			note.setNoteDateTime(indexedDate);
		}
		
		// get author.
		Reference authorReference = fhirResource.getAuthorFirstRep();
		if (authorReference != null && !authorReference.isEmpty()) {
			if (authorReference.getReferenceElement().getResourceType().equals(OmopPractitioner.FHIRTYPE)) {
				Long practitionerId = authorReference.getReferenceElement().getIdPartAsLong();
				Long omopProviderId = IdMapping.getOMOPfromFHIR(practitionerId, OmopPractitioner.FHIRTYPE);
				Provider provider = providerService.findById(omopProviderId);
				if (provider != null) {
					note.setProvider(provider);
				} else {
					ThrowFHIRExceptions.unprocessableEntityException("Author practitioner does not exist");
				}
			} else {
				ThrowFHIRExceptions.unprocessableEntityException("Only practitioner is supported for author");
			}
		}
		
		// get encounter.
		DocumentReferenceContextComponent context = fhirResource.getContext();
		if (context != null && !context.isEmpty()) {
			Reference encounterReference = context.getEncounterFirstRep();
//			TODO in the future, we want to deal with all of these encounters isntead of just the first
			if (encounterReference != null && !encounterReference.isEmpty()) {
				Long encounterId = encounterReference.getReferenceElement().getIdPartAsLong();
				Long omopVisitOccurrenceId = IdMapping.getOMOPfromFHIR(encounterId, OmopEncounter.FHIRTYPE);
				VisitOccurrence visitOccurrence = visitOccurrenceService.findById(omopVisitOccurrenceId);
				if (visitOccurrence != null) {
					note.setVisitOccurrence(visitOccurrence);
				} else {
					ThrowFHIRExceptions.unprocessableEntityException("context.encounter does not exist");
				}
			}
		}
		
		// get content.
		String note_text = new String();
		List<DocumentReferenceContentComponent> contents = fhirResource.getContent();
		for (DocumentReferenceContentComponent content: contents) {
			Attachment attachment = content.getAttachment();
			if (attachment == null || attachment.isEmpty()) {
				ThrowFHIRExceptions.unprocessableEntityException("content.attachment cannot be empty");
			}
			
			String contentType = attachment.getContentType();
			if (contentType != null && !contentType.isEmpty()) {
				if (!"text/plain".equals(contentType)) {
					ThrowFHIRExceptions.unprocessableEntityException("content.attachment must be text/plain");
				}
				
				byte[] data = attachment.getData();
				if (data == null) {
					data = attachment.getHash();
				}
				
				if (data == null) {
					ThrowFHIRExceptions.unprocessableEntityException("content.attachment.data or hash must exist");
				}
				
				// get text.
				String data_text = new String(data);
				note_text = note_text.concat(data_text);
			} else {
				ThrowFHIRExceptions.unprocessableEntityException("content.attachment.contentType must be specified as text/plain");
			}
		}
		
		if (note_text.isEmpty()) {
			ThrowFHIRExceptions.unprocessableEntityException("content.attachment.data and hash data seems to be empty");
		}
		
		note.setNoteText(note_text);
		
		// default for non-null fields
		note.setNoteClassConcept(new Concept(0L));
		note.setEncodingConcept(new Concept(0L));
		note.setLanguageConcept(new Concept(0L));
		
		return note;
	}
	
	@Override
	public DocumentReference constructResource(Long fhirId, Note entity, List<String> includes) {
		DocumentReference documentReference = constructFHIR(fhirId, entity);
		
		if (!includes.isEmpty()) {
			if (includes.contains("DocumentReference:patient") || includes.contains("DocumentReference:subject")) {
				if (documentReference.hasSubject()) {
					Long patientFhirId = documentReference.getSubject().getReferenceElement().getIdPartAsLong();
					Patient patient = OmopPatient.getInstance().constructFHIR(patientFhirId, entity.getFPerson());
					documentReference.getSubject().setResource(patient);
				}
			}
			if (includes.contains("DocumentReference:encounter")) {
				if (documentReference.hasContext()) {
					DocumentReferenceContextComponent documentContext = documentReference.getContext();
					if (documentContext.hasEncounter()) {
//						TODO later on, we want to deal with all enounters instead of just the first
						Long encounterFhirId = documentContext.getEncounterFirstRep().getReferenceElement().getIdPartAsLong();
						Encounter encounter = OmopEncounter.getInstance().constructFHIR(encounterFhirId, entity.getVisitOccurrence());
						documentContext.getEncounterFirstRep().setResource(encounter);
					}
				}
			}
		}
		
		return documentReference;
	}

	@Override
	public DocumentReference constructFHIR(Long fhirId, Note entity) {
		MyDocumentReference documentReference = new MyDocumentReference();

		documentReference.setId(new IdType(fhirId));

		// status: hard code to current.
		documentReference.setStatus(DocumentReferenceStatus.CURRENT);

		// type: map OMOP's Note Type concept to LOINC code if possible.
		Concept omopTypeConcept = entity.getNoteTypeConcept();
		CodeableConcept typeCodeableConcept = null;
		if ("Note Type".equals(omopTypeConcept.getVocabularyId())) {
			Long loincConceptId = OmopNoteTypeMapping.getLoincConceptIdFor(omopTypeConcept.getId());
			logger.debug("origin:"+omopTypeConcept.getId()+" loinc:"+loincConceptId);
			try {
				if (loincConceptId != 0L) {
					// We found lonic code for this. Find this concept and create FHIR codeable
					// concept.
					Concept loincConcept = conceptService.findById(loincConceptId);
					typeCodeableConcept = CodeableConceptUtil.getCodeableConceptFromOmopConcept(loincConcept);
				}
			} catch (FHIRException e) {
				e.printStackTrace();
				typeCodeableConcept = null;
			}
		}
		
		if (typeCodeableConcept == null) {
			try {
				typeCodeableConcept = CodeableConceptUtil.getCodeableConceptFromOmopConcept(omopTypeConcept);
			} catch (FHIRException e) {
				e.printStackTrace();
				typeCodeableConcept = null;
			}
		}
		
		// If type CodeableConcept is still null,
		if (typeCodeableConcept == null) return null;

		// Set the type now.
		documentReference.setType(typeCodeableConcept);
		
		// Set Subject
		FPerson fPerson = entity.getFPerson();
		Reference patientReference = new Reference(new IdType(OmopPatient.FHIRTYPE, IdMapping.getFHIRfromOMOP(fPerson.getId(), OmopPatient.FHIRTYPE)));
		patientReference.setDisplay(fPerson.getNameAsSingleString());
		documentReference.setSubject(patientReference);
		
		// Set created time
//		Date createdDate = entity.getNoteDate();
//		String createdTime = entity.getTime();
//		Date createdDateTime = null;
		Date createdDate = entity.getNoteDate();
		Date createdDateTime = entity.getNoteDateTime();;
		if (createdDate != null) {
//			if (createdDateTime != null)
//				createdDateTime = DateUtil.constructDateTime(createdDate, createdTime);
//			else
			if (createdDateTime == null)
				createdDateTime = DateUtil.constructDateTime(createdDate, null);
		}
		
		if (createdDateTime != null) {
			documentReference.setDate(createdDateTime);
		}
		
		// Set author 
		Provider provider = entity.getProvider();
		if (provider != null) {
			Reference practitionerReference = new Reference (new IdType(OmopPractitioner.FHIRTYPE, IdMapping.getFHIRfromOMOP(provider.getId(), OmopPractitioner.FHIRTYPE)));
			practitionerReference.setDisplay(provider.getProviderName());
			documentReference.addAuthor(practitionerReference);
		}
		
		// Set content now.
		String noteText = entity.getNoteText();
		if (noteText != null && !noteText.isEmpty()) {
			Attachment attachment = new Attachment();
			attachment.setContentType("text/plain");
			attachment.setLanguage("en-US");
			
			// Convert data to base64
			attachment.setData(noteText.getBytes());
			
			DocumentReferenceContentComponent documentReferenceContentComponent = new DocumentReferenceContentComponent(attachment);
			documentReference.addContent(documentReferenceContentComponent);
		}
		
		// Set context if visitOccurrence exists.
		VisitOccurrence visitOccurrence = entity.getVisitOccurrence();
		if (visitOccurrence != null) {
			Reference encounterReference = new Reference(new IdType(OmopEncounter.FHIRTYPE, IdMapping.getFHIRfromOMOP(visitOccurrence.getId(), OmopEncounter.FHIRTYPE)));
			DocumentReferenceContextComponent documentReferenceContextComponent = new DocumentReferenceContextComponent();
			documentReferenceContextComponent.addEncounter(encounterReference);
			documentReferenceContextComponent.setSourcePatientInfo(patientReference);
			
			documentReference.setContext(documentReferenceContextComponent);
		}
		
		return documentReference;
	}
}