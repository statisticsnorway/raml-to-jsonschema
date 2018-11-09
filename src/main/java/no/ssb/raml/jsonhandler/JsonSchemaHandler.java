package no.ssb.raml.jsonhandler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import no.ssb.raml.utils.DirectoryUtils;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class JsonSchemaHandler {

    private static final String DEFINITION_TAG = "definitions";
    private static final String PROPERTIES_TAG = "properties";

    /**
     * To add missing properties in JsonSchema
     *
     * @param jsonSchema04: JsonSchema where properties needs to be merged\
     * @return
     */
    public DocumentContext addMissingJsonPropertiesInSchema(Map.Entry<String, String> jsonSchema04, Path jsonFilesPath) {

        DocumentContext modifiedJsonSchema = JsonPath.using(Configuration.defaultConfiguration()).parse(jsonSchema04.getValue());

        Object jsonSchemaDocumentObject = Configuration.defaultConfiguration().jsonProvider().parse(jsonSchema04.getValue());

        LinkedHashMap<Object, Object> jsonSchemaDocument = new LinkedHashMap();
        LinkedHashMap<Object, Object> jsonSchemaDefinitions = new LinkedHashMap();
        LinkedHashMap<Object, Object> sourceJsonDocument = new LinkedHashMap();

        ObjectMapper oMapper = new ObjectMapper();

        if (jsonSchemaDocumentObject instanceof LinkedHashMap) {
            jsonSchemaDocument = oMapper.convertValue(jsonSchemaDocumentObject, LinkedHashMap.class);
        }

        //get all the definitions from Json schema
        Object jsonSchemaDefinitionsObject = jsonSchemaDocument.get(DEFINITION_TAG);
        if (jsonSchemaDefinitionsObject instanceof LinkedHashMap) {
            jsonSchemaDefinitions = oMapper.convertValue(jsonSchemaDocument.get(DEFINITION_TAG),
                    LinkedHashMap.class);
        }

        Path jsonFileLocation = DirectoryUtils.resolveRelativeFolderPath(jsonFilesPath.toString(), jsonSchema04.getKey() + ".json");

        String sourceJson = DirectoryUtils.readFileContent(jsonFileLocation);

        Object sourceJsonDocumentObject = Configuration.defaultConfiguration().jsonProvider().parse(sourceJson);
        if (sourceJsonDocumentObject instanceof LinkedHashMap) {
            sourceJsonDocument = oMapper.convertValue(sourceJsonDocumentObject, LinkedHashMap.class);
        }

        //merge domain level properties(Role: displayName, description etc)
        mergeDomainLevelProperties(modifiedJsonSchema, jsonSchemaDefinitions, sourceJsonDocument);

        //merge properties for all the domain mentioned in the uses section in raml file
        mergePropertiesFromRamlUses(modifiedJsonSchema, jsonSchemaDocument, sourceJsonDocument, jsonFilesPath);

        //merge properties for all the definitions mentioned in JsonSchema
        mergePropertiesInJsonSchemaDefinitions(modifiedJsonSchema, jsonSchemaDocument, jsonSchemaDefinitions, jsonFilesPath);

        return modifiedJsonSchema;
    }

    /**
     * Merge properties for all the definitions present in the JsonSchema
     *
     * @param modifiedJsonSchema
     * @param jsonSchemaDocument
     * @param jsonSchemaDefinitions
     */
    public void mergePropertiesInJsonSchemaDefinitions(DocumentContext modifiedJsonSchema,
                                                              Map<Object, Object> jsonSchemaDocument,
                                                              Map<Object, Object> jsonSchemaDefinitions,
                                                              Path jsonFilesPath) {
        //parse each and every definition from JsonSchema
        jsonSchemaDefinitions.forEach((definition, value) -> parseProperties(modifiedJsonSchema, jsonSchemaDocument,
                                                                             definition.toString(), jsonFilesPath));
    }

    /**
     * Merge all the properties of domains mentions in the Uses section in raml
     *
     * @param modifiedJsonSchema
     * @param jsonSchemaDocument
     * @param sourceJsonDocument
     */
    public void mergePropertiesFromRamlUses(DocumentContext modifiedJsonSchema,
                                                   Map<Object, Object> jsonSchemaDocument,
                                                   Map<Object, Object> sourceJsonDocument,
                                            Path jsonFilesPath) {

        LinkedHashMap<Object, Object> jsonUses = new LinkedHashMap();
        if (sourceJsonDocument.containsKey("uses")) {
            Object value = sourceJsonDocument.get("uses");
            if (!(value instanceof String)) {
                jsonUses = JsonPath.read(sourceJsonDocument, "uses");
            }
        }

        Iterator jsonUsesListIterator = jsonUses.keySet().iterator();

        while (jsonUsesListIterator.hasNext()) {
            String domainObject = (String) jsonUsesListIterator.next();
            parseProperties(modifiedJsonSchema, jsonSchemaDocument, domainObject, jsonFilesPath);
        }

    }

    /**
     * Merge properties at top level in domain json schema structure
     *
     * @param modifiedJsonSchema:    merged jsonSchema after merging properties
     * @param jsonSchemaDefinitions: properties of each definition iin JsonSchema04
     * @param sourceJsonDocument:    properties of definition in plain Json
     */
    public void mergeDomainLevelProperties(DocumentContext modifiedJsonSchema,
                                                  Map<Object, Object> jsonSchemaDefinitions,
                                                  Map<Object, Object> sourceJsonDocument) {
        LinkedHashMap<Object, Object> jsonDomainProperties = new LinkedHashMap();
        LinkedHashMap<Object, Object> jsonSchemaDomainProperties = new LinkedHashMap();

        Object typesObject = sourceJsonDocument.get("types");
        ObjectMapper oMapper = new ObjectMapper();

        if (typesObject instanceof LinkedHashMap) {
            jsonDomainProperties = oMapper.convertValue(typesObject, LinkedHashMap.class);
        }

        List<Object> types = new ArrayList(jsonDomainProperties.keySet());
        Object schemaDefinitionsObject = jsonSchemaDefinitions.get(types.get(0));

        if (schemaDefinitionsObject instanceof LinkedHashMap) {
            jsonSchemaDomainProperties = oMapper.convertValue(schemaDefinitionsObject, LinkedHashMap.class);
        }

        jsonDomainProperties = (LinkedHashMap) jsonDomainProperties.get(types.get(0));
        LinkedHashMap<Object, Object> finalJsonSchemaDomainProperties = jsonSchemaDomainProperties;
        jsonDomainProperties.forEach((property, value) -> {
            if (!finalJsonSchemaDomainProperties.containsKey(property)) {
                finalJsonSchemaDomainProperties.put(property, value);
            }

        });
        String jsonPath = "$..definitions." + types.get(0);
        modifiedJsonSchema.set(jsonPath, jsonSchemaDomainProperties);
    }

    /**
     * To parse properties from the Source Json ( plain json ) and Target Json ( Json schema)
     *
     * @param modifiedJsonSchema
     * @param jsonSchemaDocument
     * @param dependentSchema
     */
    public void parseProperties(DocumentContext modifiedJsonSchema, Map<Object, Object> jsonSchemaDocument,
                                       String dependentSchema, Path jsonFilesPath) {
        ObjectMapper oMapper = new ObjectMapper();

        // get plain json for the required Json schema ( Role, Agent etc)
        Path jsonFileLocation = DirectoryUtils.resolveRelativeFolderPath(jsonFilesPath.toString(), dependentSchema + ".json");

        if (jsonFileLocation.toFile().exists()) {
            String jsonContent = "";

            LinkedHashMap<Object, Object> jsonDocument = new LinkedHashMap();
            LinkedHashMap<Object, Object> jsonProperties = new LinkedHashMap();
            LinkedHashMap<Object, Object> jsonSchemaProperties = new LinkedHashMap();
            LinkedHashMap<Object, Object> schemaDefinitions = new LinkedHashMap();

            jsonContent = DirectoryUtils.readFileContent(jsonFileLocation);

            Object jsonObject = Configuration.defaultConfiguration().jsonProvider().parse(jsonContent);
            if (jsonObject instanceof LinkedHashMap) {
                jsonDocument = oMapper.convertValue(jsonObject, LinkedHashMap.class);
            }

            //get domain name for which merging is to be performed
            String domainName = jsonSchemaDocument.get("$ref").toString().substring(jsonSchemaDocument.
                    get("$ref").toString().lastIndexOf('/') + 1);

            Object definitionsObject = jsonSchemaDocument.get(DEFINITION_TAG);
            if (definitionsObject instanceof LinkedHashMap) {
                schemaDefinitions = oMapper.convertValue(definitionsObject, LinkedHashMap.class);
            }

            if (schemaDefinitions.containsKey(dependentSchema)) {
                domainName = dependentSchema;
            }

            LinkedHashMap<Object, Object> types = JsonPath.read(jsonDocument, "$.types."
                    + dependentSchema);
            if (types.containsKey(PROPERTIES_TAG) && (!(types.get(PROPERTIES_TAG) instanceof String) ||
                    !types.get(PROPERTIES_TAG).equals(""))) {
                jsonProperties = JsonPath.read(types, PROPERTIES_TAG);
            }

            LinkedHashMap<Object, Object> domain = JsonPath.read(jsonSchemaDocument, "$.definitions."
                    + domainName);
            if (domain.containsKey(PROPERTIES_TAG)) {
                jsonSchemaProperties = JsonPath.read(domain, PROPERTIES_TAG);
            }
            mergeJson(modifiedJsonSchema, jsonProperties, jsonSchemaProperties, domainName);
        }

    }

    /**
     * Merge properties from plain Json to Json schema
     *
     * @param mergedJsonSchema:     Merged Json schema
     * @param jsonProperties:       map containing list of all the properties
     * @param jsonSchemaProperties: map containing list of properties where missing properties to be added
     * @param domainName:           domain for which the properties needs to be merged
     */
    public void mergeJson(DocumentContext mergedJsonSchema, Map<Object, Object> jsonProperties,
                                 Map<Object, Object> jsonSchemaProperties, String domainName) {
        jsonProperties.forEach((property, value) -> {
            Object propertyObject = property.toString().replaceAll("[?]", "");
            if (jsonSchemaProperties.containsKey(propertyObject)) {
                LinkedHashMap<Object, Object> schemaProperties = new LinkedHashMap();
                LinkedHashMap<Object, Object> plainJsonProperties = new LinkedHashMap();
                ObjectMapper oMapper = new ObjectMapper();
                Object jsonPropertyObject = jsonSchemaProperties.get(propertyObject);
                if (jsonPropertyObject instanceof LinkedHashMap) {
                    schemaProperties = oMapper.convertValue(jsonPropertyObject, LinkedHashMap.class);
                }

                if (value instanceof LinkedHashMap) {
                    plainJsonProperties = oMapper.convertValue(value, LinkedHashMap.class);
                }

                LinkedHashMap<Object, Object> finalSchemaProperties = schemaProperties;
                plainJsonProperties.forEach((jsonProperty, jsonPropertyValue) -> {
                    if (!finalSchemaProperties.containsKey(jsonProperty)) {
                        finalSchemaProperties.put(jsonProperty, jsonPropertyValue);
                        String jsonPath = "$..definitions." + domainName + ".properties." + propertyObject;
                        mergedJsonSchema.set(jsonPath, finalSchemaProperties);
                    }

                });
            }

        });
    }

}
