package no.ssb.raml.graphql;

import graphql.language.EnumTypeDefinition;
import graphql.schema.GraphQLEnumType;
import org.raml.v2.api.model.v10.datamodel.StringTypeDeclaration;

class GraphQLEnumVisitor extends BaseTypeDeclarationVisitor<GraphQLEnumType> {

    private String enumName(StringTypeDeclaration type) {
        String fieldName = type.name();
        return fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1);
    }

    @Override
    public GraphQLEnumType visit(StringTypeDeclaration type) {
        GraphQLEnumType.Builder enumBuilder = GraphQLEnumType.newEnum();
        enumBuilder.name(enumName(type));
        for (String enumValue : type.enumValues()) {
            enumBuilder.definition(EnumTypeDefinition.newEnumTypeDefinition().name(enumValue).build());
        }
        return enumBuilder.build();
    }
}
