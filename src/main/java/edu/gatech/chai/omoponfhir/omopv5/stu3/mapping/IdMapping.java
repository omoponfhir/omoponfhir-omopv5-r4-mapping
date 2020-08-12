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
package edu.gatech.chai.omoponfhir.omopv5.stu3.mapping;

/**
 * ID Mapping Class to manage the IDs between FHIR and OMOP.
 * 
 * @author mc142
 *
 */
public class IdMapping {
	
	public static Long getFHIRfromOMOP(Long omop_id, String resource_name) {
		// We use the same ID for now.
		// TODO: Develop the ID mapping so that we do not reveal native
		//       OMOP ID. If the mapping exists, send it. If not, create a new
		//       mapping.
		
		return omop_id;
	}

	/**
	 * What is OMOP ID for the long part of FHIR ID
	 * @param fhir_id
	 * @return
	 */
	public static Long getOMOPfromFHIR(Long fhir_id, String resource_name) {
		// We use the same ID now.
		// TODO: Develop the ID mapping so that we do not reveal native
		//       OMOP ID.
		
		return fhir_id;
	}
	
	public static void writeOMOPfromFHIR(Long fhir_id) {
		// Placeholder for later to use to store OMOP ID mapping info.
		// This information will be used by getFHIRfromOMOP
		// TODO: Develop mapping creation here.
	}
}
