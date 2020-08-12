# Upgrade nodes for FHIR STU3 to R4

`import org.hl7.fhir.r3.model.Device.DeviceUdiComponent becomes import org.hl7.fhir.r4.model.Device.DeviceUdiComponent;`
`import org.hl7.fhir.dstu3.model.CodeableConcept; becomes import org.hl7.fhir.r4.model.CodeableConcept;`
`import org.hl7.fhir.dstu3.model.Coding; becomes import org.hl7.fhir.r4.model.Coding;`
`import org.hl7.fhir.dstu3.model.IdType; becomes import org.hl7.fhir.r4.model.IdType;`
`import org.hl7.fhir.dstu3.model.Patient; becomes import org.hl7.fhir.r4.model.Patient;`
`import org.hl7.fhir.dstu3.model.Reference; becomes import org.hl7.fhir.r4.model.Reference;`
`import org.hl7.fhir.dstu3.model.Resource; becomes import org.hl7.fhir.r4.model.Resource;`