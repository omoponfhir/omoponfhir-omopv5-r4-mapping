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
package edu.gatech.chai.omoponfhir.local.dao;

import java.sql.Connection;
import java.util.List;

import edu.gatech.chai.omoponfhir.local.model.FhirOmopVocabularyMapEntry;

public interface FhirOmopVocabularyMap {
	public Connection connect();
	
	public int save(FhirOmopVocabularyMapEntry conceptMapEntry);
	public void update(FhirOmopVocabularyMapEntry conceptMapEntry);
	public void delete(String omopConceptCodeName);
	public List<FhirOmopVocabularyMapEntry> get();
	public String getOmopVocabularyFromFhirSystemName(String fhirSystemName);
	public String getFhirSystemNameFromOmopVocabulary(String omopConceptCodeName);

}
