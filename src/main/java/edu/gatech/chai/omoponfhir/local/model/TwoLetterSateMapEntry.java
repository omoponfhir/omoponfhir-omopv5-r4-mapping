package edu.gatech.chai.omoponfhir.local.model;

public class TwoLetterSateMapEntry {
	private String stateName;
	private String twoLetter;
	
	public TwoLetterSateMapEntry() {
		
	}
	
	public String getStateName() {
		return this.stateName;
	}
	
	public void setStateName(String stateName) {
		this.stateName = stateName;
	}
	
	public String getTwoLetter() {
		return this.twoLetter;
	}
	
	public void setTwoLetter(String twoLetter) {
		this.twoLetter = twoLetter;
	}
}
