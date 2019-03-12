package no.ssb.raml.graphql;

import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLInterfaceType;
import graphql.schema.GraphQLOutputType;
import graphql.schema.GraphQLTypeReference;
import org.raml.v2.api.model.v10.datamodel.ObjectTypeDeclaration;
import org.raml.v2.api.model.v10.datamodel.TypeDeclaration;

import java.util.LinkedHashMap;
import java.util.Map;

public class GraphQLInterfaceTypeVisitor extends BaseTypeDeclarationVisitor<GraphQLInterfaceType> {

    private final TypeDeclarationVisitor<GraphQLOutputType> rootVisitor;
    private final Map<String, GraphQLInterfaceType> interfaces = new LinkedHashMap<>();

    public GraphQLInterfaceTypeVisitor(TypeDeclarationVisitor<GraphQLOutputType> rootVisitor) {
        this.rootVisitor = rootVisitor;
    }

    public GraphQLInterfaceType getInterface(ObjectTypeDeclaration type) {
        return interfaces.get(type.name());
    }

    @Override
    public GraphQLInterfaceType visit(ObjectTypeDeclaration type) {
        GraphQLInterfaceType.Builder newInterface = GraphQLInterfaceType.newInterface();
        newInterface.typeResolver(env -> null);
        newInterface.name(type.name());
        for (TypeDeclaration property : type.properties()) {
            GraphQLFieldDefinition.Builder fieldDefinition = GraphQLFieldDefinition.newFieldDefinition();
            fieldDefinition.name(property.name());
            GraphQLOutputType graphQLType = rootVisitor.visit(property);
            fieldDefinition.type(graphQLType);
            newInterface.field(fieldDefinition);
        }
        return newInterface.build();
    }
}
