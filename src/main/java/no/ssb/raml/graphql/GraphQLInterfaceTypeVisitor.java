package no.ssb.raml.graphql;

import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLInterfaceType;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLOutputType;
import org.raml.v2.api.model.v10.datamodel.ObjectTypeDeclaration;
import org.raml.v2.api.model.v10.datamodel.TypeDeclaration;
import org.raml.v2.api.model.v10.declarations.AnnotationRef;

public class GraphQLInterfaceTypeVisitor extends BaseTypeDeclarationVisitor<GraphQLInterfaceType> {

    private final TypeDeclarationVisitor<GraphQLOutputType> fieldVisitor;
    private final GraphQLDirectiveVisitor directiveVisitor;

    public GraphQLInterfaceTypeVisitor(TypeDeclarationVisitor<GraphQLOutputType> fieldVisitor) {
        this.fieldVisitor = fieldVisitor;
        this.directiveVisitor = new GraphQLDirectiveVisitor();
    }

    @Override
    public GraphQLInterfaceType visit(ObjectTypeDeclaration type) {
        GraphQLInterfaceType.Builder newInterface = GraphQLInterfaceType.newInterface();
        newInterface.typeResolver(env -> (GraphQLObjectType) env.getSchema().getType(type.name()));

        newInterface.name(type.name());

        if (type.description() != null) {
            newInterface.description(type.description().value());
        }

        // TODO: Factorize.
        for (TypeDeclaration property : type.properties()) {
            GraphQLFieldDefinition.Builder fieldDefinition = GraphQLFieldDefinition.newFieldDefinition();
            fieldDefinition.name(property.name());

            if (property.description() != null) {
                fieldDefinition.description(property.description().value());
            }

            // Add simple annotations.
            for (AnnotationRef annotation : property.annotations()) {
                fieldDefinition.withDirective(directiveVisitor.visit(annotation.annotation()));
            }

            GraphQLOutputType graphQLType = fieldVisitor.visit(property);
            fieldDefinition.type(graphQLType);
            newInterface.field(fieldDefinition);
        }
        return newInterface.build();
    }
}
