package no.ssb.raml.ramlhandler;

import org.raml.v2.api.RamlModelBuilder;
import org.raml.v2.api.RamlModelResult;
import org.raml.v2.api.model.common.ValidationResult;
import org.raml.v2.api.model.v10.api.Library;
import org.raml.v2.api.model.v10.datamodel.TypeDeclaration;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class RAMLJsonSchema04DefinitionBuilder {

    final String resourcePath;
    Map<String, String> schemaByName;

    public RAMLJsonSchema04DefinitionBuilder(String resourcePath) {
        this.resourcePath = resourcePath;
    }

    public RAMLJsonSchema04DefinitionBuilder schemaByName(Map<String, String> schemaByName) {
        this.schemaByName = schemaByName;
        return this;
    }

    public Map<String, String> schemaByName() {
        return schemaByName;
    }

    public Map<String, String> build() {
        RamlModelResult ramlModelResult = new RamlModelBuilder().buildApi(resourcePath);
        if (ramlModelResult.hasErrors()) {
            for (ValidationResult validationResult : ramlModelResult.getValidationResults()) {
                System.err.println(validationResult.getPath());
                System.err.println(validationResult.getMessage());
            }
            throw new RuntimeException("Unable to create RAML Specification");
        }
        Library library = ramlModelResult.getLibrary();


        if (schemaByName == null) {
            schemaByName = new LinkedHashMap<>();
        }

        List<TypeDeclaration> types = library.types();
        Iterator<TypeDeclaration> typeIterator = types.iterator();
        while (typeIterator.hasNext()) {
            TypeDeclaration type = typeIterator.next();
            String schemaJsonStr = type.toJsonSchema();
            schemaByName.put(type.name(), schemaJsonStr);
        }

        return schemaByName;
    }
}
