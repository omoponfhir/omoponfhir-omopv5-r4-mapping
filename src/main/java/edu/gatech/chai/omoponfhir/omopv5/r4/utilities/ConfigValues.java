package edu.gatech.chai.omoponfhir.omopv5.r4.utilities;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ConfigValues {

	@Value("${schema.data}")
    private String dataSchema;

	@Value("${schema.vocabularies}")
	private String vocabSchema;


    public String getDataSchema() {
        return this.dataSchema;
    }

    public void setDataSchema(String dataSchema) {
        this.dataSchema = dataSchema;
    }

    public String getVocabSchema() {
        return this.vocabSchema;
    }

    public void setVocabSchema(String vocabSchema) {
        this.vocabSchema = vocabSchema;
    }
}