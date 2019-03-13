package no.ssb.raml.graphql;

import graphql.language.EnumTypeDefinition;
import graphql.language.EnumValueDefinition;
import graphql.schema.GraphQLEnumType;
import org.raml.v2.api.model.v10.datamodel.StringTypeDeclaration;

import java.util.HashSet;
import java.util.Set;

class GraphQLEnumVisitor extends BaseTypeDeclarationVisitor<GraphQLEnumType> {

    public static String enumName(StringTypeDeclaration type) {
        String fieldName = type.name();
        String suffix = !type.name().toLowerCase().endsWith("type") ? "Type" : "";
        return fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1) + suffix;
    }

    @Override
    public GraphQLEnumType visit(StringTypeDeclaration type) {
        GraphQLEnumType.Builder enumBuilder = GraphQLEnumType.newEnum();
        enumBuilder.name(enumName(type));

        enumBuilder.definition(EnumTypeDefinition.newEnumTypeDefinition().build());

        if (type.description() != null) {
            enumBuilder.description(type.description().value());
        }

        for (String enumValue : type.enumValues()) {
            enumBuilder.value(enumValue);
        }
        return enumBuilder.build();
    }
}
