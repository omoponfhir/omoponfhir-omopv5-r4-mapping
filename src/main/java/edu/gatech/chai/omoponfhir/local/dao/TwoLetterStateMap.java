package edu.gatech.chai.omoponfhir.local.dao;

import java.sql.Connection;
import java.util.List;

import edu.gatech.chai.omoponfhir.local.model.TwoLetterSateMapEntry;


public interface TwoLetterStateMap {
	public Connection connect();
	
	public int save(TwoLetterSateMapEntry mapEntry);
	public void update(TwoLetterSateMapEntry mapEntry);
	public void delete(String stateName);
	public List<TwoLetterSateMapEntry> get();
	public String getTwoLetter(String stateName);
	public String getStateName(String twoLetter);
}
