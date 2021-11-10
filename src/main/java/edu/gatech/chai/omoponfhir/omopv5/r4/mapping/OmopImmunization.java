package edu.gatech.chai.omoponfhir.omopv5.r4.mapping;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.r4.model.Annotation;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.DateTimeType;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Immunization;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Quantity;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Type;
import org.hl7.fhir.r4.model.Immunization.ImmunizationPerformerComponent;
import org.hl7.fhir.r4.model.Immunization.ImmunizationStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.context.ContextLoaderListener;
import org.springframework.web.context.WebApplicationContext;

import ca.uhn.fhir.rest.api.SortSpec;
import ca.uhn.fhir.rest.param.DateParam;
import ca.uhn.fhir.rest.param.DateRangeParam;
import ca.uhn.fhir.rest.param.ReferenceParam;
import ca.uhn.fhir.rest.param.TokenOrListParam;
import ca.uhn.fhir.rest.param.TokenParam;
import edu.gatech.chai.omoponfhir.omopv5.r4.provider.EncounterResourceProvider;
import edu.gatech.chai.omoponfhir.omopv5.r4.provider.ImmunizationResourceProvider;
import edu.gatech.chai.omoponfhir.omopv5.r4.provider.PatientResourceProvider;
import edu.gatech.chai.omoponfhir.omopv5.r4.provider.PractitionerResourceProvider;
import edu.gatech.chai.omoponfhir.omopv5.r4.utilities.CodeableConceptUtil;
import edu.gatech.chai.omoponfhir.omopv5.r4.utilities.DateUtil;
import edu.gatech.chai.omopv5.dba.service.ConceptService;
import edu.gatech.chai.omopv5.dba.service.DrugExposureService;
import edu.gatech.chai.omopv5.dba.service.FImmunizationViewService;
import edu.gatech.chai.omopv5.dba.service.FPersonService;
import edu.gatech.chai.omopv5.dba.service.ParameterWrapper;
import edu.gatech.chai.omopv5.dba.service.ProviderService;
import edu.gatech.chai.omopv5.dba.service.VisitOccurrenceService;
import edu.gatech.chai.omopv5.model.entity.Concept;
import edu.gatech.chai.omopv5.model.entity.DrugExposure;
import edu.gatech.chai.omopv5.model.entity.FImmunizationView;
import edu.gatech.chai.omopv5.model.entity.FPerson;
import edu.gatech.chai.omopv5.model.entity.Provider;
import edu.gatech.chai.omopv5.model.entity.VisitOccurrence;

/**
 * <h1>OMOP to FHIR Immunization Mapping Class</h1> This class maps FHIR
 * Immunication resource from/to OMOP CDM.
 * <p>
 * <b>Note:</b> Giving proper comments in your program makes it more user
 * friendly and it is assumed as a high quality code.
 *
 * @author Myung Choi
 */
public class OmopImmunization extends BaseOmopResource<Immunization, FImmunizationView, FImmunizationViewService> {

	static final Logger logger = LoggerFactory.getLogger(OmopImmunization.class);

	private static OmopImmunization omopImmunization = new OmopImmunization();
	private VisitOccurrenceService visitOccurrenceService;
	private DrugExposureService drugExposureService;
	private ConceptService conceptService;
	private ProviderService providerService;
	private FPersonService fPersonService;

	private final static Long SELF_REPORTED_CONCEPTID = 44787730L;
	private final static Long PHYSICIAN_ADMINISTERED_PROCEDURE = 38000179L;

	private static String _columns = "distinct d.drug_exposure_id as id," + " d.stop_reason as stopReason,"
			+ " d.drug_exposure_start_date as drugExposureStartDate,"
			+ " d.drug_exposure_start_datetime as drugExposureStartDateTime,"
			+ " d.drug_exposure_end_date as drugExposureEndDate,"
			+ " d.drug_exposure_end_datetime as drugExposureEndDateTime," + " d.drug_concept_id as drugConceptId,"
			+ " c.vocabulary_id as vaccineVocabularyId," + " c.concept_code as vaccineConceptCode,"
			+ " c.concept_name as vaccineConceptName," + " d.person_id as persionId," + " fp.family_name as familyName,"
			+ " fp.given1_name as given1Name," + " fp.given2_name as given2Name," + " fp.prefix_name as prefixName,"
			+ " fp.suffix_name as suffixName," + " d.provider_id as providerId," + " pr.provider_name as providerName,"
			+ " d.visit_occurrence_id as visitOccurrenceId," + " d.lot_number as lotName,"
			+ " d.route_concept_id as routeConceptId," + " r.vocabulary_id as routeVocabularyId,"
			+ " r.concept_code as routeConceptCode," + " r.concept_name as routeConceptName," + " d.quantity as quantity,"
			+ " d.sig as sig";

	private static String _from = "drug_exposure d join concept c on d.drug_concept_id = c.concept_id"
			+ " join concept_relationship cr on d.drug_concept_id = cr.concept_id_2"
			+ " join Concept c2 on cr.concept_id_1 = c2.concept_id" + " join person p on d.person_id = p.person_id"
			+ " join f_person fp on d.person_id = fp.person_id" + " left join provider pr on d.provider_id = pr.provider_id"
			+ " left join visit_occurrence v on d.visit_occurrence_id = v.visit_occurrence_id"
			+ " left join concept r on d.route_concept_id = r.concept_id";

	private String _where = "c2.vocabulary_id = 'CVX'";

	public OmopImmunization(WebApplicationContext context) {
		super(context, FImmunizationView.class, FImmunizationViewService.class, ImmunizationResourceProvider.getType());
		initialize(context);

		// String sizeSql = "select count(distinct d) from " + _from + " where " + _where;
		// getSize(sizeSql, null, null);
		getSize();
	}

	public OmopImmunization() {
		super(ContextLoaderListener.getCurrentWebApplicationContext(), FImmunizationView.class,
		FImmunizationViewService.class, ImmunizationResourceProvider.getType());
		initialize(ContextLoaderListener.getCurrentWebApplicationContext());
	}

	private void initialize(WebApplicationContext context) {
		if (context != null) {
			visitOccurrenceService = context.getBean(VisitOccurrenceService.class);
			conceptService = context.getBean(ConceptService.class);
			providerService = context.getBean(ProviderService.class);
			fPersonService = context.getBean(FPersonService.class);
		}
	}

	public static OmopImmunization getInstance() {
		return OmopImmunization.omopImmunization;
	}

	@Override
	public Long toDbase(Immunization fhirResource, IdType fhirId) throws FHIRException {
		Long omopId = null;
		DrugExposure drugExposure = null;
		if (fhirId != null) {
			omopId = fhirId.getIdPartAsLong();
		}

		drugExposure = constructDrugExposure(omopId, fhirResource);

		Long retOmopId = null;
		if (omopId == null) {
			retOmopId = drugExposureService.create(drugExposure).getId();
		} else {
			retOmopId = drugExposureService.update(drugExposure).getId();
		}
		return retOmopId;
	}

	@Override
	public List<ParameterWrapper> mapParameter(String parameter, Object value, boolean or) {
		List<ParameterWrapper> mapList = new ArrayList<ParameterWrapper>();
		ParameterWrapper paramWrapper = new ParameterWrapper();
		if (or)
			paramWrapper.setUpperRelationship("or");
		else
			paramWrapper.setUpperRelationship("and");

		switch (parameter) {
			case Immunization.SP_RES_ID:
				String immunizationId = ((TokenParam) value).getValue();
				paramWrapper.setParameterType("Long");
				paramWrapper.setParameters(Arrays.asList("id"));
				paramWrapper.setOperators(Arrays.asList("="));
				paramWrapper.setValues(Arrays.asList(immunizationId));
				paramWrapper.setRelationship("or");
				mapList.add(paramWrapper);
				break;

				case Immunization.SP_VACCINE_CODE:
				String system = ((TokenParam) value).getSystem();
				String code = ((TokenParam) value).getValue();
	//			System.out.println("\n\n\n\n\nSystem:"+system+"\n\ncode:"+code+"\n\n\n\n\n");
				if ((system == null || system.isEmpty()) && (code == null || code.isEmpty()))
					break;
	
				String omopVocabulary = "None";
				if (system != null && !system.isEmpty()) {
					try {
						omopVocabulary = OmopCodeableConceptMapping.omopVocabularyforFhirUri(system);
					} catch (FHIRException e) {
						e.printStackTrace();
					}
				}
	
				paramWrapper.setParameterType("String");
				if ("None".equals(omopVocabulary) && code != null && !code.isEmpty()) {
					paramWrapper.setParameters(Arrays.asList("immunizationConcept.conceptCode"));
					paramWrapper.setOperators(Arrays.asList("="));
					paramWrapper.setValues(Arrays.asList(code));
				} else if (!"None".equals(omopVocabulary) && (code == null || code.isEmpty())) {
					paramWrapper.setParameters(Arrays.asList("immunizationConcept.vocabularyId"));
					paramWrapper.setOperators(Arrays.asList("="));
					paramWrapper.setValues(Arrays.asList(omopVocabulary));
				} else {
					paramWrapper.setParameters(Arrays.asList("immunizationConcept.vocabularyId", "immunizationConcept.conceptCode"));
					paramWrapper.setOperators(Arrays.asList("=", "="));
					paramWrapper.setValues(Arrays.asList(omopVocabulary, code));
				}
				paramWrapper.setRelationship("and");
				mapList.add(paramWrapper);
				break;
			case Immunization.SP_DATE:
				DateRangeParam dateRangeParam = ((DateRangeParam) value);
				DateUtil.constructParameterWrapper(dateRangeParam, "immunizationDate", paramWrapper, mapList);
				break;
			case "Patient:" + Patient.SP_RES_ID:
				addParamlistForPatientIDName(parameter, (String) value, paramWrapper, mapList);
				break;
			case "Patient:" + Patient.SP_NAME:
				addParamlistForPatientIDName(parameter, (String) value, paramWrapper, mapList);
				break;
			case "Patient:" + Patient.SP_IDENTIFIER:
				addParamlistForPatientIDName(parameter, (String) value, paramWrapper, mapList);
				break;				
			default:
				mapList = null;
		}

		return mapList;
	}

	public String constructSearchSql(String whereStatement) {
		String searchSql = "select distinct d from " + _from + " where " + _where;
		if (whereStatement != null && !whereStatement.isEmpty()) {
			searchSql += " and " + whereStatement;
		}

		return searchSql;
	}

	public String constructSizeSql(String whereStatement) {
		String searchSql = "select count(distinct d) from " + _from + " where " + _where;
		if (whereStatement != null && !whereStatement.isEmpty()) {
			searchSql += " and " + whereStatement;
		}

		return searchSql;
	}

	public String mapParameter(String parameter, Object value, List<String> parameterList, List<String> valueList) {
		String whereStatement = "";

		switch (parameter) {
			case Immunization.SP_RES_ID:
				String immunizationId = ((TokenParam) value).getValue();
				whereStatement = "d.id = @drugExposureId";
				parameterList.add("drugExposureId");
				valueList.add(immunizationId);
				break;

			case Immunization.SP_VACCINE_CODE:
				List<TokenParam> codes = ((TokenOrListParam) value).getValuesAsQueryTokens();

				int i = 0;
				for (TokenParam code : codes) {
					String systemValue = code.getSystem();
					String codeValue = code.getValue();

					if ((systemValue == null || systemValue.isEmpty()) && (codeValue == null || codeValue.isEmpty())) {
						// This is not searchable. So, skip this.
						continue;
					}

					String omopVocabulary = "None";
					if (systemValue != null && !systemValue.isEmpty()) {
						// Find OMOP vocabulary_id for this system. If not found,
						// put empty so that we can search it by code only (if provided).
						try {
							omopVocabulary = fhirOmopVocabularyMap.getOmopVocabularyFromFhirSystemName(systemValue);
						} catch (FHIRException e) {
							e.printStackTrace();
							systemValue = "";
						}
					}

					if (systemValue != null && !systemValue.isEmpty() && codeValue != null && !codeValue.isEmpty()) {
						String vId = "c.vocabularyId = " + "@vaccineCodeSystem" + i;
						String cCode = "c.conceptCode = " + "@vaccineCodeCode" + i;
						String statement = "(" + vId + " and " + cCode + ")";

						whereStatement = (whereStatement == null || whereStatement.isEmpty()) ? statement
								: whereStatement + " or " + statement;
						parameterList.add("vaccineCodeSystem" + i);
						valueList.add(omopVocabulary);
						parameterList.add("vaccineCodeCode" + i);
						valueList.add(codeValue);
					} else if ((systemValue == null || systemValue.isEmpty()) && codeValue != null
							&& !codeValue.isEmpty()) {
						String statement = "c.conceptCode = " + "@vaccineCodeCode" + i;
						whereStatement = (whereStatement == null || whereStatement.isEmpty()) ? statement
								: whereStatement + " or " + statement;
						parameterList.add("vaccineCodeCode" + i);
						valueList.add(codeValue);
					} else if ((codeValue == null || codeValue.isEmpty()) && systemValue != null
							&& !systemValue.isEmpty()) {
						String statement = "c.vocabularyId = " + "@vaccineCodeSystem" + i;
						whereStatement = (whereStatement == null || whereStatement.isEmpty()) ? statement
								: whereStatement + " or " + statement;
						parameterList.add("vaccineCodeSystem" + i);
						valueList.add(omopVocabulary);
					} else {
						continue; // no system or code
					}

					i++;
				}
				break;

			case Immunization.SP_DATE:
				DateRangeParam dateRangeParam = ((DateRangeParam) value);

				DateParam lowerDateParam = dateRangeParam.getLowerBound();
				DateParam upperDateParam = dateRangeParam.getUpperBound();
				if (lowerDateParam != null && upperDateParam != null) {
					// case 1
					String lowerSqlOperator = DateUtil.getSqlOperator(lowerDateParam.getPrefix());
					String upperSqlOperator = DateUtil.getSqlOperator(upperDateParam.getPrefix());

					whereStatement = "d.drugExposureStartDate " + lowerSqlOperator + " @drugExposureStartDate and "
							+ "d.drugExposureEndDate " + upperSqlOperator + " @drugExposureEndDate";
					parameterList.add("drugExposureStartDate");
					valueList.add(String.valueOf(lowerDateParam.getValue().getTime()));
					parameterList.add("drugExposureEndDate");
					valueList.add(String.valueOf(upperDateParam.getValue().getTime()));
				} else if (lowerDateParam != null && upperDateParam == null) {
					String lowerSqlOperator = DateUtil.getSqlOperator(lowerDateParam.getPrefix());

					whereStatement = "d.drugExposureStartDate " + lowerSqlOperator + " @drugExposureStartDate";
					parameterList.add("drugExposureStartDate");
					valueList.add(String.valueOf(lowerDateParam.getValue().getTime()));
				} else {
					String upperSqlOperator = DateUtil.getSqlOperator(upperDateParam.getPrefix());

					whereStatement = "d.drugExposureEndDate " + upperSqlOperator + " @drugExposureEndDate";
					parameterList.add("drugExposureEndDate");
					valueList.add(String.valueOf(upperDateParam.getValue().getTime()));
				}
				break;

			case Immunization.SP_PATIENT:
				ReferenceParam patientReference = ((ReferenceParam) value);
				Long fhirPatientId = patientReference.getIdPartAsLong();
				String omopPersonIdString = String.valueOf(fhirPatientId);

				whereStatement = "p.id = @patient";
				parameterList.add("patient");
				valueList.add(omopPersonIdString);

				break;

			default:

		}

		return whereStatement;
	}

	public String constructOrderParams(SortSpec theSort) {
		if (theSort == null)
			return null;

		String direction;

		if (theSort.getOrder() != null)
			direction = theSort.getOrder().toString();
		else
			direction = "ASC";

		String orderParam = new String();

		if (theSort.getParamName().equals(Immunization.SP_VACCINE_CODE)) {
			orderParam = "d.drugConcept.conceptCode " + direction;
		} else if (theSort.getParamName().equals(Immunization.SP_DATE)) {
			orderParam = "d.drugExposureStartDate " + direction;
		} else if (theSort.getParamName().equals(Immunization.SP_PATIENT)) {
			orderParam = "d.person.id " + direction;
		} else {
			orderParam = "d.id " + direction;
		}

		String orderParams = orderParam;

		if (theSort.getChain() != null) {
			orderParams = orderParams.concat("," + constructOrderParams(theSort.getChain()));
		}

		return orderParams;
	}

	@Override
	public Immunization constructFHIR(Long fhirId, FImmunizationView entity) {
		Immunization immunization = new Immunization();
		immunization.setId(new IdType(fhirId));

		// Set patient
		Reference patientReference = new Reference(new IdType(PatientResourceProvider.getType(), entity.getFPerson().getId()));
		patientReference.setDisplay(entity.getFPerson().getNameAsSingleString());
		immunization.setPatient(patientReference);

		// status - set to stopped if we have stop reason. Otherwise, we just set it to
		if (entity.getImmunizationStatus() != null && !entity.getImmunizationStatus().isEmpty()) {
			immunization.setStatus(ImmunizationStatus.NOTDONE);
			// get status stop reason to coding.text
			immunization.setStatusReason((new CodeableConcept()).setText(entity.getImmunizationStatus()));
		} else {
			immunization.setStatus(ImmunizationStatus.COMPLETED);
		}

		// date
		immunization.setOccurrence(new DateTimeType(entity.getImmunizationDate()));

		// vaccine code
		CodeableConcept vaccineCodeable = CodeableConceptUtil.getCodeableConceptFromOmopConcept(entity.getImmunizationConcept(),
				getFhirOmopVocabularyMap());
		immunization.setVaccineCode(vaccineCodeable);

		// performer
		Provider provider = entity.getProvider();
		if (provider != null) {
			Reference performerReference = new Reference(
					new IdType(PractitionerResourceProvider.getType(), entity.getProvider().getId()));
			ImmunizationPerformerComponent perf = new ImmunizationPerformerComponent(performerReference);
			immunization.setPerformer(Arrays.asList(perf));
		}

		// encounter
		VisitOccurrence visitOccurrence = entity.getVisitOccurrence();
		if (visitOccurrence != null) {
			Reference encounterReference = new Reference(
					new IdType(EncounterResourceProvider.getType(), entity.getVisitOccurrence().getId()));
			immunization.setEncounter(encounterReference);
		}

		// lot number
		String lotNumber = entity.getLotNumber();
		if (lotNumber != null && !lotNumber.isEmpty()) {
			immunization.setLotNumber(lotNumber);
		}

		// route
		Concept routeConcept = entity.getRouteConcept();
		if (routeConcept != null) {
			CodeableConcept routeCodeable = CodeableConceptUtil.getCodeableConceptFromOmopConcept(routeConcept,
					getFhirOmopVocabularyMap());
			immunization.setRoute(routeCodeable);
		}

		// quantity
		Double quantity = entity.getQuantity();
		if (quantity != null && !quantity.isInfinite() && !quantity.isNaN()) {
			immunization.setDoseQuantity(new Quantity(quantity));
		}

		// sig
		String sig = entity.getImmunizationNote();
		if (sig != null && !sig.isEmpty()) {
			Annotation annotation = new Annotation();
			annotation.setText(sig);
			immunization.setNote(Arrays.asList(annotation));
		}

		return immunization;
	}

	public DrugExposure constructDrugExposure(Long omopId, Immunization fhirResource) {
		DrugExposure drugExposure = null;
		if (omopId != null) {
			// Update
			drugExposure = drugExposureService.findById(omopId);
			if (drugExposure == null) {
				throw new FHIRException(fhirResource.getId() + " does not exist");
			}
		} else {
			// Create
			List<Identifier> identifiers = fhirResource.getIdentifier();
			for (Identifier identifier : identifiers) {
				if (identifier.isEmpty())
					continue;
				String identifierValue = identifier.getValue();
				List<DrugExposure> results = drugExposureService.searchByColumnString("drugSourceValue",
						identifierValue);
				if (!results.isEmpty()) {
					drugExposure = results.get(0);
					omopId = drugExposure.getId();
					break;
				}
			}

			if (drugExposure == null) {
				drugExposure = new DrugExposure();
				// Add the source column.
				Identifier identifier = fhirResource.getIdentifierFirstRep();
				if (!identifier.isEmpty()) {
					drugExposure.setDrugSourceValue(identifier.getValue());
				}
			}
		}

		// Set patient.
		Reference patientReference = fhirResource.getPatient();
		if (patientReference == null)
			throw new FHIRException("Patient must exist.");

		Long omopFPersonId = patientReference.getReferenceElement().getIdPartAsLong();

		FPerson fPerson = fPersonService.findById(omopFPersonId);
		if (fPerson == null)
			throw new FHIRException("Patient/" + omopFPersonId + " is not valid");

		drugExposure.setFPerson(fPerson);
		drugExposure.setDrugTypeConcept(new Concept(OmopImmunization.PHYSICIAN_ADMINISTERED_PROCEDURE));

		// status
		ImmunizationStatus status = fhirResource.getStatus();
		if (status != null && status == ImmunizationStatus.NOTDONE) {
			CodeableConcept statusReason = fhirResource.getStatusReason();
			String stopReason = "";
			if (!statusReason.isEmpty()) {
				if (statusReason.getText() != null && !statusReason.getText().isEmpty()) {
					drugExposure.setStopReason(statusReason.getText());
				} else {
					List<Coding> statusReasonCodings = statusReason.getCoding();
					for (Coding statusReasonCoding : statusReasonCodings) {
						stopReason += statusReasonCoding.getSystem() + ":" + statusReasonCoding.getCode();
					}
					drugExposure.setStopReason(stopReason);
				}
			}
		}

		// vaccine code to drug concept
		CodeableConcept vaccineCode = fhirResource.getVaccineCode();
		if (vaccineCode.isEmpty()) {
			throw new FHIRException("vaccineCode cannot be empty");
		}

		Concept drugConcept = null;
		for (Coding vaccineCodeCoding : vaccineCode.getCoding()) {
			drugConcept = CodeableConceptUtil.getOmopConceptWithFhirConcept(conceptService, vaccineCodeCoding);
			if (drugConcept != null)
				break;
		}

		if (drugConcept == null)
			drugConcept = new Concept(0L);
		drugExposure.setDrugConcept(drugConcept);
		drugExposure.setDrugSourceValue(
				vaccineCode.getCodingFirstRep().getSystem() + ":" + vaccineCode.getCodingFirstRep().getCode());

		// date. we only have one date.
		Type occurrence = fhirResource.getOccurrence();
		if (occurrence == null) {
			throw new FHIRException("date cannot be null");
		}

		DateTimeType startDate = null;
		if (occurrence instanceof DateTimeType) {
			startDate = (DateTimeType) occurrence;
		} else {
			throw new FHIRException("occurrence must be datetime type");
		}
		
		drugExposure.setDrugExposureStartDate(startDate.getValue());
		drugExposure.setDrugExposureStartDateTime(startDate.getValue());
		drugExposure.setDrugExposureEndDate(startDate.getValue());
		
		// performer
		ImmunizationPerformerComponent performer = fhirResource.getPerformerFirstRep();
		if (!performer.isEmpty()) {
			Reference performerActorReference = performer.getActor();
			if (!performerActorReference.isEmpty()) {
				Long performerId = performerActorReference.getReferenceElement().getIdPartAsLong();
				Provider provider = providerService.findById(performerId);
				if (provider == null) {
					throw new FHIRException("performer (" + performerId + ") does not exist");
				}
			
				drugExposure.setProvider(new Provider(performerId));
			}
		}
		
		// encounter
		Reference encounterReference = fhirResource.getEncounter();
		if (!encounterReference.isEmpty()) {
			Long encounterId = encounterReference.getReferenceElement().getIdPartAsLong();
			VisitOccurrence visitOccurrence = visitOccurrenceService.findById(encounterId);
			if (visitOccurrence == null) {
				throw new FHIRException("encounter (" + encounterId + ") does not exist");
			}
			
			drugExposure.setVisitOccurrence(visitOccurrence);
		}
		
		// lotNumber
		String lotNumber = fhirResource.getLotNumber();
		if (lotNumber != null && !lotNumber.isEmpty()) {
			drugExposure.setLotNumber(lotNumber);
		}
		
		// route
		CodeableConcept routeCode = fhirResource.getRoute();
		if (!routeCode.isEmpty()) {
			Concept routeConcept = null;
			for (Coding routeCodeCoding : routeCode.getCoding()) {
				routeConcept = CodeableConceptUtil.getOmopConceptWithFhirConcept(conceptService, routeCodeCoding);
				if (routeConcept != null) break;
			}
			
			if (routeConcept == null) {
				drugExposure.setRouteConcept(new Concept (0L));
			}
			
			drugExposure.setRouteSourceValue(routeCode.getCodingFirstRep().getSystem()+":"+routeCode.getCodingFirstRep().getCode()+":"+routeCode.getCodingFirstRep().getDisplay());
		}
		
		// doseQuantity
		Quantity doseQuantity = fhirResource.getDoseQuantity();
		if (!doseQuantity.isEmpty()) {
			drugExposure.setQuantity(doseQuantity.getValue().doubleValue());
		}
		
		// note
		List<Annotation> note = fhirResource.getNote();
		String sigText = "";
		for (Annotation noteAnnotation : note) {
			sigText += noteAnnotation.getText() + " ";
		}
		
		if (!sigText.isEmpty() ) {
			drugExposure.setSig(sigText.trim());
		}
		
		return drugExposure;	
	}

	@Override
	public FImmunizationView constructOmop(Long omopId, Immunization fhirResource) {
		// this is a view. So, it's read-only
		return null;
	}

}
