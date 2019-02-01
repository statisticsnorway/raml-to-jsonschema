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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

public class JsonSchemaHandler {

    private static final String DEFINITION_TAG = "definitions";
    private static final String PROPERTIES_TAG = "properties";
    private static final String LINK_TAG = "(Link.types)";

    private final static Logger logger = Logger.getLogger(JsonSchemaHandler.class.getName());

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
        String sourceJson = "";

        if (jsonSchemaDocumentObject instanceof LinkedHashMap) {
            jsonSchemaDocument = oMapper.convertValue(jsonSchemaDocumentObject, LinkedHashMap.class);
        }

        //get all the definitions from Json schema
        Object jsonSchemaDefinitionsObject = jsonSchemaDocument.get(DEFINITION_TAG);
        if (jsonSchemaDefinitionsObject instanceof LinkedHashMap) {
            jsonSchemaDefinitions = oMapper.convertValue(jsonSchemaDocument.get(DEFINITION_TAG),
                    LinkedHashMap.class);
        }

        //get json file form temporary location
        Path jsonFileLocation = DirectoryUtils.resolveRelativeFolderPath(jsonFilesPath.toString(), jsonSchema04.getKey() + ".json");

        if (jsonFileLocation.toFile().getAbsoluteFile().exists()) {
            sourceJson = DirectoryUtils.readFileContent(jsonFileLocation);
            Object sourceJsonDocumentObject = Configuration.defaultConfiguration().jsonProvider().parse(sourceJson);

            if (sourceJsonDocumentObject instanceof LinkedHashMap) {
                sourceJsonDocument = oMapper.convertValue(sourceJsonDocumentObject, LinkedHashMap.class);
            }

            //merge domain level properties(Role: displayName, description etc)
            mergeDomainLevelProperties(modifiedJsonSchema, jsonSchemaDefinitions, sourceJsonDocument);

            //merge properties for all the domain mentioned in the uses section in raml file
            mergePropertiesFromRamlUses(modifiedJsonSchema, jsonSchemaDocument, sourceJsonDocument, jsonFilesPath);

        } else {
            logger.log(Level.WARNING, "Raml file {0} cannot to be converted to json file {1}. " + "A complete json" +
                            " schema with set of all properties cannot be created! ",
                    new Object[]{jsonSchema04.getKey() + ".raml", jsonSchema04.getKey() + ".json"});
        }

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

        Object schemaDefinitionsObject = null;
        String jsonPath = "";

        List<Object> types = new ArrayList(jsonDomainProperties.keySet());

        if (!types.isEmpty() && types.size() > 0) {
            schemaDefinitionsObject = jsonSchemaDefinitions.get(types.get(0));
            jsonDomainProperties = (LinkedHashMap) jsonDomainProperties.get(types.get(0));
            jsonPath = "$..definitions." + types.get(0);

            if (schemaDefinitionsObject instanceof LinkedHashMap) {
                jsonSchemaDomainProperties = oMapper.convertValue(schemaDefinitionsObject, LinkedHashMap.class);
            }
            LinkedHashMap<Object, Object> finalJsonSchemaDomainProperties = jsonSchemaDomainProperties;
            jsonDomainProperties.forEach((property, value) -> {
                if (!finalJsonSchemaDomainProperties.containsKey(property) &&
                        !(property.toString().equalsIgnoreCase("example") ||
                                (property.toString().equalsIgnoreCase("examples")))) {
                    finalJsonSchemaDomainProperties.put(property, value);
                }
            });
            modifiedJsonSchema.set(jsonPath, jsonSchemaDomainProperties);
        }
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

        String jsonContent = "";

        // get plain json for the required Json schema ( Role, Agent etc)
        Path jsonFileLocation = DirectoryUtils.resolveRelativeFolderPath(jsonFilesPath.toString(), dependentSchema + ".json");
        if (jsonFileLocation.toFile().getAbsoluteFile().exists()) {
            jsonContent = DirectoryUtils.readFileContent(jsonFileLocation);
        } else {
            logger.log(Level.WARNING, "Cannot find json file {0}. " + "A complete json schema with set of" +
                    " all properties cannot be created! ", new Object[]{dependentSchema + ".json"});
        }

        LinkedHashMap<Object, Object> jsonDocument = new LinkedHashMap();
        LinkedHashMap<Object, Object> jsonProperties = new LinkedHashMap();
        LinkedHashMap<Object, Object> jsonSchemaProperties = new LinkedHashMap();
        LinkedHashMap<Object, Object> schemaDefinitions = new LinkedHashMap();
        LinkedHashMap<Object, Object> types = new LinkedHashMap();
        LinkedHashMap<Object, Object> resolvedJsonProperties = new LinkedHashMap();

        Object jsonObject = Configuration.defaultConfiguration().jsonProvider().parse(jsonContent);
        if (jsonObject instanceof LinkedHashMap) {
            jsonDocument = oMapper.convertValue(jsonObject, LinkedHashMap.class);
        }

        if (jsonDocument.containsKey("types")) {
            Object typesObject = JsonPath.read(jsonDocument, "$.types");
            if (typesObject instanceof LinkedHashMap) {
                types = oMapper.convertValue(typesObject, LinkedHashMap.class);
            }
        }

        if (types.size() > 0) {
            Object schemaType = JsonPath.read(jsonDocument, "$.types." + dependentSchema);
            types = oMapper.convertValue(schemaType, LinkedHashMap.class);
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

        if (types.containsKey(PROPERTIES_TAG) && (!(types.get(PROPERTIES_TAG) instanceof String) ||
                !types.get(PROPERTIES_TAG).equals(""))) {
            jsonProperties = JsonPath.read(types, PROPERTIES_TAG);
        }

        LinkedHashMap<Object, Object> domain = JsonPath.read(jsonSchemaDocument, "$.definitions." + domainName);
        if (domain.containsKey(PROPERTIES_TAG)) {
            jsonSchemaProperties = JsonPath.read(domain, PROPERTIES_TAG);
        }

        if (jsonProperties != null) {
            resolvedJsonProperties = resolveJsonLinks(oMapper.convertValue(jsonProperties, ConcurrentHashMap.class));
        }

        mergeJson(modifiedJsonSchema, jsonProperties, jsonSchemaProperties, resolvedJsonProperties, domainName);
    }

    private LinkedHashMap<Object, Object> resolveJsonLinks(ConcurrentHashMap<Object, Object> jsonProperties) {
        ObjectMapper oMapper = new ObjectMapper();
        AtomicReference<ConcurrentHashMap<Object, Object>> propertyValues = new AtomicReference<>(new ConcurrentHashMap<>());

        jsonProperties.forEach((key, value) -> {
            AtomicBoolean isInvalidPropertyValue = new AtomicBoolean(false);
            LinkedHashMap<Object, Object> keyValues = oMapper.convertValue(value, LinkedHashMap.class);
            keyValues.forEach((k, v) -> {
                if (v == null || v == "") {
                    System.err.println("Property " + k + " in " + key + " is not defined!!");
                    isInvalidPropertyValue.set(true);
                }
            });

            if(!isInvalidPropertyValue.get()){
                propertyValues.set(oMapper.convertValue(value, ConcurrentHashMap.class));
                propertyValues.get().forEach((property, propertyValue) -> {
                    LinkedHashMap<Object, Object> linkedObject = new LinkedHashMap<>();
                    if (property.equals(LINK_TAG)) {
                        LinkedHashMap<Object, Object> linkedPropertyType = new LinkedHashMap<>();
                        ArrayList<String> linkedProperties = (ArrayList) propertyValue;
                        LinkedHashMap<Object, Object> linkedProperty = new LinkedHashMap<>();
                        linkedProperties.forEach((linkProperty) -> {
                            linkedPropertyType.put("type", "null");
                            linkedProperty.put(linkProperty, linkedPropertyType);
                            linkedObject.put("type", "object");
                            linkedObject.put("properties", linkedProperty);
                            String keyStr = "_link_property_" + key.toString().replaceAll("[?]", "");
                            propertyValues.get().remove(property);
                            jsonProperties.put(key, propertyValues);
                            jsonProperties.put(keyStr, linkedObject);
                        });
                    }
                });

            }
        });

        return oMapper.convertValue(jsonProperties, LinkedHashMap.class);
    }

    /**
     * Merge properties from plain Json to Json schema
     *
     * @param mergedJsonSchema       :     Merged Json schema
     * @param jsonProperties         :       map containing list of all the properties
     * @param jsonSchemaProperties   : map containing list of properties where missing properties to be added
     * @param resolvedJsonProperties
     * @param domainName             :           domain for which the properties needs to be merged
     */
    public DocumentContext mergeJson(DocumentContext mergedJsonSchema, Map<Object, Object> jsonProperties,
                                     Map<Object, Object> jsonSchemaProperties, LinkedHashMap<Object, Object> resolvedJsonProperties, String domainName) {
        resolvedJsonProperties.forEach((property, value) -> {
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
            } else {
                String jsonPath = "$..definitions." + domainName + ".properties";
                mergedJsonSchema.put(jsonPath, propertyObject.toString(), value);
            }
        });
        return mergedJsonSchema;
    }

}
