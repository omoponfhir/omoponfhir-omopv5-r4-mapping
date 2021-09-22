package edu.gatech.chai.omoponfhir.omopv5.r4.provider;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Immunization;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.hl7.fhir.r4.model.OperationOutcome.IssueSeverity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.context.ContextLoader;
import org.springframework.web.context.WebApplicationContext;

import ca.uhn.fhir.model.primitive.IdDt;
import ca.uhn.fhir.rest.annotation.Create;
import ca.uhn.fhir.rest.annotation.Delete;
import ca.uhn.fhir.rest.annotation.IdParam;
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
import ca.uhn.fhir.rest.param.DateRangeParam;
import ca.uhn.fhir.rest.param.ReferenceParam;
import ca.uhn.fhir.rest.param.TokenOrListParam;
import ca.uhn.fhir.rest.param.TokenParam;
import ca.uhn.fhir.rest.server.IResourceProvider;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import edu.gatech.chai.omoponfhir.omopv5.r4.mapping.OmopImmunization;
import edu.gatech.chai.omoponfhir.omopv5.r4.utilities.StaticValues;

public class ImmunizationResourceProvider implements IResourceProvider {
	private static final Logger logger = LoggerFactory.getLogger(ImmunizationResourceProvider.class);

	private WebApplicationContext myAppCtx;
	private OmopImmunization myMapper;
	private int preferredPageSize = 30;

	public ImmunizationResourceProvider() {
		myAppCtx = ContextLoader.getCurrentWebApplicationContext();
		myMapper = new OmopImmunization(myAppCtx);
		
		String pageSizeStr = myAppCtx.getServletContext().getInitParameter("preferredPageSize");
		if (pageSizeStr != null && pageSizeStr.isEmpty() == false) {
			int pageSize = Integer.parseInt(pageSizeStr);
			if (pageSize > 0) {
				preferredPageSize = pageSize;
			}
		}
	}
	
	public static String getType() {
		return "Immunization";
	}

    public OmopImmunization getMyMapper() {
    	return myMapper;
    }

	private Integer getTotalSize(String queryString, List<String> parameterList, List<String> valueList) {
		final Long totalSize = getMyMapper().getSize(queryString, parameterList, valueList);
			
		return totalSize.intValue();
	}

	@Override
	public Class<? extends IBaseResource> getResourceType() {
		return Immunization.class;
	}

	/**
	 * The "@Create" annotation indicates that this method implements "create=type", which adds a 
	 * new instance of a resource to the server.
	 */
	@Create()
	public MethodOutcome createImmunization(@ResourceParam Immunization theImmunization) {
		validateResource(theImmunization);
		
		Long id=null;
		try {
			id = myMapper.toDbase(theImmunization, null);
		} catch (FHIRException e) {
			e.printStackTrace();
		}
		
		if (id == null) {
			OperationOutcome outcome = new OperationOutcome();
			CodeableConcept detailCode = new CodeableConcept();
			detailCode.setText("Failed to create entity.");
			outcome.addIssue().setSeverity(IssueSeverity.FATAL).setDetails(detailCode);
			throw new UnprocessableEntityException(StaticValues.myFhirContext, outcome);
		}

		return new MethodOutcome(new IdDt(id));
	}

	@Delete()
	public void deleteMedicationRequest(@IdParam IdType theId) {
		if (myMapper.removeByFhirId(theId) <= 0) {
			throw new ResourceNotFoundException(theId);
		}
	}


	@Update()
	public MethodOutcome updateUmmunization(@IdParam IdType theId, @ResourceParam Immunization theImmunization) {
		validateResource(theImmunization);
		
		Long fhirId=null;
		try {
			fhirId = myMapper.toDbase(theImmunization, theId);
		} catch (FHIRException e) {
			e.printStackTrace();
		}

		if (fhirId == null) {
			throw new ResourceNotFoundException(theId);
		}

		return new MethodOutcome();
	}

	@Read()
	public Immunization readImmunization(@IdParam IdType theId) {
		Immunization retval = (Immunization) getMyMapper().toFHIR(theId);
		if (retval == null) {
			throw new ResourceNotFoundException(theId);
		}
			
		return retval;
	}
	
	@Search()
	public IBundleProvider findImmunizationById(
			@RequiredParam(name = Immunization.SP_RES_ID) TokenParam theImmunizationId
			) {
		List<String> parameterList = new ArrayList<String> ();
		List<String> valueList = new ArrayList<String> ();
		String whereStatement = "";

		if (theImmunizationId != null) {
			whereStatement += getMyMapper().mapParameter (Immunization.SP_RES_ID, theImmunizationId, parameterList, valueList);
		}
				
		whereStatement = whereStatement.trim();
		
		String searchSql = getMyMapper().constructSearchSql(whereStatement);
		String sizeSql = getMyMapper().constructSizeSql(whereStatement);
		
		MyBundleProvider myBundleProvider = new MyBundleProvider(parameterList, valueList, searchSql);
		myBundleProvider.setTotalSize(getTotalSize(sizeSql, parameterList, valueList));
		myBundleProvider.setPreferredPageSize(preferredPageSize);

		return myBundleProvider;
	}

	@Search()
	public IBundleProvider findImmunizationsssByParams(
			@OptionalParam(name = Immunization.SP_VACCINE_CODE) TokenOrListParam theVaccineOrVCodes,
			@OptionalParam(name = Immunization.SP_DATE) DateRangeParam theDateRangeParam,
			@OptionalParam(name = Immunization.SP_PATIENT) ReferenceParam thePatient,
			@Sort SortSpec theSort
			) {
		List<String> parameterList = new ArrayList<String> ();
		List<String> valueList = new ArrayList<String> ();

		String whereStatement = "";
				
		if (theVaccineOrVCodes != null) {
			String newWhere = getMyMapper().mapParameter(Immunization.SP_VACCINE_CODE, theVaccineOrVCodes, parameterList, valueList);
			if (newWhere != null && !newWhere.isEmpty()) {
				whereStatement = "(" + newWhere + ")";
			}
		}
		
		if (thePatient != null) {
			String newWhere = getMyMapper().mapParameter(Immunization.SP_PATIENT, thePatient, parameterList, valueList);
			if (newWhere != null && !newWhere.isEmpty()) {
				whereStatement = whereStatement.isEmpty() ? newWhere : whereStatement + " and " + newWhere;
			}
		}

		if (theDateRangeParam != null) {
			String newWhere = getMyMapper().mapParameter(Immunization.SP_DATE, theDateRangeParam, parameterList, valueList);
			if (newWhere != null && !newWhere.isEmpty()) {
				whereStatement = whereStatement.isEmpty() ? newWhere : whereStatement + " and " + newWhere; 
			}
		}
		
		whereStatement = whereStatement.trim();
		
		String searchSql = getMyMapper().constructSearchSql(whereStatement);
		String sizeSql = getMyMapper().constructSizeSql(whereStatement);
		String orderParams = getMyMapper().constructOrderParams(theSort);

		MyBundleProvider myBundleProvider = new MyBundleProvider(parameterList, valueList, searchSql);
		myBundleProvider.setTotalSize(getTotalSize(sizeSql, parameterList, valueList));
		myBundleProvider.setPreferredPageSize(preferredPageSize);
		myBundleProvider.setOrderParams(orderParams);
		
		logger.debug("I am HERE : " + searchSql + " " + orderParams);
		return myBundleProvider;
	}
	
	private void validateResource(Immunization theMedication) {
		// TODO: implement validation method
	}

	class MyBundleProvider extends OmopFhirBundleProvider implements IBundleProvider {
		public MyBundleProvider(List<String> parameterList, List<String> valueList, String searchSql) {
			super(parameterList, valueList, searchSql);
			setPreferredPageSize (preferredPageSize);
		}

		@Override
		public List<IBaseResource> getResources(int fromIndex, int toIndex) {
			List<IBaseResource> retv = new ArrayList<IBaseResource>();

			String finalSql = searchSql + " " + orderParams;
			logger.debug("Final SQL: " + finalSql);
			getMyMapper().searchWithSql(searchSql, parameterList, valueList, fromIndex, toIndex, orderParams, retv);

			return retv;
		}		
	}
}
