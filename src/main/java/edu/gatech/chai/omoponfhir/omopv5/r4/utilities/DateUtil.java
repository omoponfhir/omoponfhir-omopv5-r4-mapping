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
package edu.gatech.chai.omoponfhir.omopv5.r4.utilities;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import ca.uhn.fhir.rest.param.DateParam;
import ca.uhn.fhir.rest.param.DateRangeParam;
import ca.uhn.fhir.rest.param.ParamPrefixEnum;
import edu.gatech.chai.omopv5.dba.service.ParameterWrapper;

public class DateUtil {
	public static Date constructDateTime(Date date, String time) {
		DateFormat dateOnlyFormat = new SimpleDateFormat("yyyy/MM/dd");
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd hh:mm:ss");
		
		String timeString = "00:00:00";
		if (time != null && !time.isEmpty()) {
			timeString = time;
		}
		
		String dateTimeString = dateOnlyFormat.format(date) + " " + timeString;
		Date dateTime = null;
		try {
			dateTime = dateFormat.parse(dateTimeString);
		} catch (ParseException e) {
			e.printStackTrace();
			return null;
		}
		
		return dateTime;
	}
	
	public static String getSqlOperator (ParamPrefixEnum apiOperator) {
		String sqlOperator = null;
		if (apiOperator.equals(ParamPrefixEnum.GREATERTHAN)) {
			sqlOperator = ">";
		} else if (apiOperator.equals(ParamPrefixEnum.GREATERTHAN_OR_EQUALS)) {
			sqlOperator = ">=";
		} else if (apiOperator.equals(ParamPrefixEnum.LESSTHAN)) {
			sqlOperator = "<";
		} else if (apiOperator.equals(ParamPrefixEnum.LESSTHAN_OR_EQUALS)) {
			sqlOperator = "<=";
		} else if (apiOperator.equals(ParamPrefixEnum.NOT_EQUAL)) {
			sqlOperator = "!=";
		} else {
			sqlOperator = "=";
		}

		return sqlOperator;
	}
	
	public static void constructParameterWrapper(DateRangeParam dateRangeParam, String dateColumn, ParameterWrapper paramWrapper, List<ParameterWrapper> mapList) {
		// There are 3 possible cases. 
		// case 1 is range.
		// case 2 is lower bound and no upper bound
		// case 3 is upper bound and no lower bound
		
		DateParam lowerDateParam = dateRangeParam.getLowerBound();
		DateParam upperDateParam = dateRangeParam.getUpperBound();
		if (lowerDateParam != null && upperDateParam != null) {
			// case 1
			String lowerSqlOperator = DateUtil.getSqlOperator(lowerDateParam.getPrefix());
			String upperSqlOperator = DateUtil.getSqlOperator(upperDateParam.getPrefix());
			
			paramWrapper.setParameterType("Date");
			paramWrapper.setParameters(Arrays.asList(dateColumn, dateColumn));
			paramWrapper.setOperators(Arrays.asList(lowerSqlOperator, upperSqlOperator));
			paramWrapper.setValues(Arrays.asList(String.valueOf(lowerDateParam.getValue().getTime()),
					String.valueOf(upperDateParam.getValue().getTime())));
			paramWrapper.setRelationship("and");
		} else if (lowerDateParam != null && upperDateParam == null) {
			String lowerSqlOperator = DateUtil.getSqlOperator(lowerDateParam.getPrefix());
			
			paramWrapper.setParameterType("Date");
			paramWrapper.setParameters(Arrays.asList(dateColumn));
			paramWrapper.setOperators(Arrays.asList(lowerSqlOperator));
			paramWrapper.setValues(Arrays.asList(String.valueOf(lowerDateParam.getValue().getTime())));
			paramWrapper.setRelationship("and");
		} else {
			String upperSqlOperator = DateUtil.getSqlOperator(upperDateParam.getPrefix());
			
			paramWrapper.setParameterType("Date");
			paramWrapper.setParameters(Arrays.asList(dateColumn));
			paramWrapper.setOperators(Arrays.asList(upperSqlOperator));
			paramWrapper.setValues(Arrays.asList(String.valueOf(upperDateParam.getValue().getTime())));
			paramWrapper.setRelationship("and");
		}
		
		mapList.add(paramWrapper);
	}
}
