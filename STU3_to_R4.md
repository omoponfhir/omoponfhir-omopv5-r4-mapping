# Upgrade nodes for FHIR STU3 to R4

`import org.hl7.fhir.r3.model.Device.DeviceUdiComponent becomes import org.hl7.fhir.r4.model.Device.DeviceUdiComponent;`
`import org.hl7.fhir.dstu3.model.CodeableConcept; becomes import org.hl7.fhir.r4.model.CodeableConcept;`
`import org.hl7.fhir.dstu3.model.Coding; becomes import org.hl7.fhir.r4.model.Coding;`
`import org.hl7.fhir.dstu3.model.IdType; becomes import org.hl7.fhir.r4.model.IdType;`
`import org.hl7.fhir.dstu3.model.Patient; becomes import org.hl7.fhir.r4.model.Patient;`
`import org.hl7.fhir.dstu3.model.Reference; becomes import org.hl7.fhir.r4.model.Reference;`
`import org.hl7.fhir.dstu3.model.Resource; becomes import org.hl7.fhir.r4.model.Resource;`
`import org.hl7.fhir.dstu3.model.BooleanType; becomes import org.hl7.fhir.r4.model.BooleanType;`
`import org.hl7.fhir.dstu3.model.CodeType; becomes import org.hl7.fhir.r4.model.CodeType;`
`import org.hl7.fhir.dstu3.model.ConceptMap; becomes import org.hl7.fhir.r4.model.ConceptMap;`
`import org.hl7.fhir.dstu3.model.Parameters; becomes import org.hl7.fhir.r4.model.Parameters;`
`import org.hl7.fhir.dstu3.model.Parameters.ParametersParameterComponent; becomes import org.hl7.fhir.r4.model.Parameters.ParametersParameterComponent;`
`import org.hl7.fhir.dstu3.model.codesystems.AdministrativeGender; becomes import org.hl7.fhir.dstu3.model.codesystems.AdministrativeGender;`
`import org.hl7.fhir.dstu3.model.codesystems.OrganizationType; becomes import org.hl7.fhir.r4.model.codesystems.OrganizationType;`
`import org.hl7.fhir.dstu3.model.codesystems.V3ActCode; becomes import org.hl7.fhir.r4.model.codesystems.V3ActCode;`
`import org.hl7.fhir.dstu3.model.codesystems.ObservationCategory; becomes import org.hl7.fhir.r4.model.codesystems.ObservationCategory;`
`import org.hl7.fhir.dstu3.model.codesystems.ConditionCategory; becomes import org.hl7.fhir.r4.model.codesystems.ConditionCategory;`
`import org.hl7.fhir.dstu3.model.Condition; becomes import org.hl7.fhir.r4.model.Condition;`

## OmopCondition
SP asserted-date is removed. Potentially changed to recorded date?
SP abatement_boolean is removed
SP Context is removed. Potentially change to encounter
`getSourceConceptId` changes to `getConditionSourceConcept` for 5.3.1
`getConceptId` becomes `getConditionConcept` for 5.3.1
`setConceptId` becomes `setConditionConcept` for 5.3.1
`getStartDate` becomes `getConditionStartDate` for 5.3.1
`setStartDate` becomes `setConditionStartDate` for 5.3.1
`getEndDate` becomes `getConditionEndDate` for 5.3.1
`setEndDate` becomes `setConditionEndDate` for 5.3.1
`getTypeConceptId` becomes `getConditionTypeConcept` for 5.3.1
`setTypeConceptId` becomes `setConditionTypeConcept` for 5.3.1

## OmopConceptMap
`getConcept2`  becomes `getConceptId2` for 5.3.1

## MedicationStatement
`dosage.setDose(quantity);` becomes 
```
Dosage.DosageDoseAndRateComponent tempComponent = new Dosage.DosageDoseAndRateComponent();
tempComponent.setDose(quantity);
dosage.addDoseAndRate(tempComponent);
```
`setTaken` is removed
`getReasonNotTaken` is removed, use `getStatusReason` instead
	note that you may have to parse though the 
`dosage.getDoseSimpleQuantity();` becomes 
```
List<Dosage.DosageDoseAndRateComponent> dosesAndRates = dosage.getDoseAndRate();
for(Dosage.DosageDoseAndRateComponent doseAndRate : dosesAndRates){
	if(doseAndRate.hasDoseQuantity()){
		qty=doseAndRate.getDoseQuantity();
	}
}
```
