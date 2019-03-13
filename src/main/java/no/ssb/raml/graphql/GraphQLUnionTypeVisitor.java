package no.ssb.raml.graphql;

import graphql.schema.GraphQLTypeReference;
import graphql.schema.GraphQLUnionType;
import org.raml.v2.api.model.v10.datamodel.TypeDeclaration;
import org.raml.v2.api.model.v10.datamodel.UnionTypeDeclaration;

public class GraphQLUnionTypeVisitor extends BaseTypeDeclarationVisitor<GraphQLUnionType> {

    @Override
    public GraphQLUnionType visit(UnionTypeDeclaration type) {

        GraphQLUnionType.Builder builder = GraphQLUnionType.newUnionType();

        builder.name(type.name());
        if (type.description() != null) {
            builder.description(type.description().value());
        }

        for (TypeDeclaration subType : type.of()) {
            builder.possibleType(GraphQLTypeReference.typeRef(subType.name()));
        }
        return super.visit(type);
    }
}
