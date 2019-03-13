package no.ssb.raml.graphql;

import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLInterfaceType;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLOutputType;
import org.raml.v2.api.model.v10.datamodel.ObjectTypeDeclaration;
import org.raml.v2.api.model.v10.datamodel.TypeDeclaration;

public class GraphQLInterfaceTypeVisitor extends BaseTypeDeclarationVisitor<GraphQLInterfaceType> {

    private final TypeDeclarationVisitor<GraphQLOutputType> fieldVisitor;

    public GraphQLInterfaceTypeVisitor(TypeDeclarationVisitor<GraphQLOutputType> fieldVisitor) {
        this.fieldVisitor = fieldVisitor;
    }

    @Override
    public GraphQLInterfaceType visit(ObjectTypeDeclaration type) {
        GraphQLInterfaceType.Builder newInterface = GraphQLInterfaceType.newInterface();
        newInterface.typeResolver(env -> (GraphQLObjectType) env.getSchema().getType(type.name()));
        newInterface.name(type.name());
        for (TypeDeclaration property : type.properties()) {
            GraphQLFieldDefinition.Builder fieldDefinition = GraphQLFieldDefinition.newFieldDefinition();
            fieldDefinition.name(property.name());
            GraphQLOutputType graphQLType = fieldVisitor.visit(property);
            fieldDefinition.type(graphQLType);
            newInterface.field(fieldDefinition);
        }
        return newInterface.build();
    }
}
