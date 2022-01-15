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
package edu.gatech.chai.omoponfhir.omopv5.r4.model;

import java.util.ArrayList;
import java.util.List;

import org.hl7.fhir.r4.model.BackboneElement;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.StringType;

import ca.uhn.fhir.model.api.annotation.Block;
import ca.uhn.fhir.model.api.annotation.Child;
import ca.uhn.fhir.model.api.annotation.Description;
import ca.uhn.fhir.model.api.annotation.Extension;
import ca.uhn.fhir.model.api.annotation.ResourceDef;
import ca.uhn.fhir.util.ElementUtil;

@ResourceDef(name = "Patient", profile = "http://hl7.org/fhir/us/core/StructureDefinition/us-core-patient")
public class USCorePatient extends Patient {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@Description(shortDefinition = "Concepts classifying the person into a named category of humans sharing common history, traits, geographical origin or nationality")
	@Extension(url = "http://hl7.org/fhir/us/core/StructureDefinition/us-core-race", isModifier = false, definedLocally = true)
	@Child(name = "race", min = 0, max = 1)
	private Race myRace;

	@Description(shortDefinition = "Concepts classifying the person into a named category of humans sharing common history, traits, geographical origin or nationality")
	@Extension(url = "http://hl7.org/fhir/us/core/StructureDefinition/us-core-ethnicity", isModifier = false, definedLocally = true)
	@Child(name = "ethnicity", min = 0, max = 1)
	private Ethnicity myEthnicity;

	
	public Race getRace() {
		if (myRace == null) {
			myRace = new Race();
		} 
		
		return myRace;
	}
	
	public void setRace(Race myRace) {
		this.myRace = myRace;
	}
	
	public Ethnicity getEthnicity() {
		if (myEthnicity == null) {
			myEthnicity = new Ethnicity();
		}
		
		return myEthnicity;
	}
	
	public void setEthnicity(Ethnicity myEthnicity) {
		this.myEthnicity = myEthnicity;
	}
	
	@Override
	public boolean isEmpty() {
        return super.isEmpty() && ElementUtil.isEmpty(myRace, myEthnicity);
	}

	/**
	 * This "block definition" defines an extension type with multiple child
	 * extensions. It is referenced by the field myRace above.
	 */
	@Block
	public static class Race extends BackboneElement {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		/**
		 * This is a primitive datatype extension
		 */
		@Description(shortDefinition = "The 5 race category codes according to the OMB Standards")
		@Extension(url = "ombCategory", isModifier = false, definedLocally = true)
		@Child(name = "category", min = 0, max = 5)
		private List<Coding> myCategory;

		public List<Coding> getCategory() {
			if (myCategory == null) {
				myCategory = new ArrayList<Coding>();
			}
			
			return myCategory;
		}

		public void setCategory(List<Coding> myCategory) {
			this.myCategory = myCategory;
		}

		@Description(shortDefinition = "Plain text representation of the race concept(s)")
		@Extension(url = "text", isModifier = false, definedLocally = true)
		@Child(name = "text", min = 1, max = 1)
		private StringType myText;

		public StringType getText() {
			if (myText == null) {
				myText = new StringType();
			}
			
			return myText;
		}

		public void setText(StringType myText) {
			this.myText = myText;
		}

		@Override
		public boolean isEmpty() {
			return ElementUtil.isEmpty(myCategory);
		}

		@Override
		public Race copy() {
			Race copy = new Race();
			copy.myCategory = this.myCategory;
			copy.myText = this.myText;

			return copy;
		}

	}
	
	/**
	 * This "block definition" defines an extension type with multiple child
	 * extensions. It is referenced by the field myRace above.
	 */
	@Block
	public static class Ethnicity extends BackboneElement {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		/**
		 * This is a primitive datatype extension
		 */
		@Description(shortDefinition = "The 2 ethnicity category codes according to the OMB Standards")
		@Extension(url = "ombCategory", isModifier = false, definedLocally = true)
		@Child(name = "category", min = 0, max = 1)
		private List<Coding> myCategory;

		public List<Coding> getCategory() {
			if (myCategory == null) {
				myCategory = new ArrayList<Coding>();
			}
			
			return myCategory;
		}

		public void setCategory(List<Coding> myCategory) {
			this.myCategory = myCategory;
		}

		@Description(shortDefinition = "Plain text representation of the ethnicity concept(s)")
		@Extension(url = "text", isModifier = false, definedLocally = true)
		@Child(name = "text", min = 1, max = 1)
		private StringType myText;

		public StringType getText() {
			if (myText == null) {
				myText = new StringType();
			}
			
			return myText;
		}

		public void setText(StringType myText) {
			this.myText = myText;
		}

		@Override
		public boolean isEmpty() {
			return ElementUtil.isEmpty(myCategory);
		}

		@Override
		public Ethnicity copy() {
			Ethnicity copy = new Ethnicity();
			copy.myCategory = this.myCategory;
			copy.myText = this.myText;
			
			return copy;
		}

	}
}
