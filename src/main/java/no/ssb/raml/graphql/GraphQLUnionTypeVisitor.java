package no.ssb.raml.graphql;

import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLOutputType;
import graphql.schema.GraphQLType;
import graphql.schema.GraphQLTypeReference;
import graphql.schema.GraphQLUnionType;
import org.raml.v2.api.model.v10.datamodel.ObjectTypeDeclaration;
import org.raml.v2.api.model.v10.datamodel.TypeDeclaration;
import org.raml.v2.api.model.v10.datamodel.UnionTypeDeclaration;

import static java.lang.String.format;

public class GraphQLUnionTypeVisitor extends BaseTypeDeclarationVisitor<GraphQLUnionType> {

    final TypeDeclarationVisitor<GraphQLOutputType> rootVisitor;

    public GraphQLUnionTypeVisitor(TypeDeclarationVisitor<GraphQLOutputType> rootVisitor) {
        this.rootVisitor = rootVisitor;
    }

    @Override
    public GraphQLUnionType visit(UnionTypeDeclaration type) {

        GraphQLUnionType.Builder builder = GraphQLUnionType.newUnionType();

        builder.name(type.name());
        builder.description(type.description().toString());

        for (TypeDeclaration subType : type.of()) {
            if (!(subType instanceof ObjectTypeDeclaration)) {
                throw new IllegalArgumentException(format(
                        "subtype %s (%s) of union %s was not an object",
                        subType.name(), subType.getClass(), type.name()
                ));
            }

            GraphQLType graphQlType = rootVisitor.visit(subType);
            if (graphQlType instanceof GraphQLTypeReference) {
                builder.possibleType((GraphQLTypeReference) graphQlType);
            } else if (graphQlType instanceof GraphQLObjectType) {
                builder.possibleType((GraphQLObjectType) graphQlType);
            } else {
                throw new IllegalArgumentException(format(
                        "GraphQL subtype %s (%s) was neither of type GraphQLObjectType nor GraphQLTypeReference",
                        graphQlType.getName(), graphQlType.getClass()
                ));
            }

        }
        return super.visit(type);
    }
}
