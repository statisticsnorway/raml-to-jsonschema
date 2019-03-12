package no.ssb.raml.graphql;

import graphql.schema.GraphQLCompositeType;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLOutputType;
import graphql.schema.GraphQLTypeReference;
import graphql.schema.GraphQLUnionType;
import org.raml.v2.api.model.v10.datamodel.ObjectTypeDeclaration;
import org.raml.v2.api.model.v10.datamodel.UnionTypeDeclaration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

class GraphQLCompositeTypeVisitor extends BaseTypeDeclarationVisitor<GraphQLCompositeType> {

    private static final Logger log = LoggerFactory.getLogger(GraphQLCompositeTypeVisitor.class);

    private final TypeDeclarationVisitor<GraphQLObjectType> objectVisitor;
    private final TypeDeclarationVisitor<GraphQLUnionType> unionVisitor;

    GraphQLCompositeTypeVisitor(TypeDeclarationVisitor<GraphQLOutputType> rootVisitor, Set<String> seenTypes) {
        this.objectVisitor = new GraphQLObjectTypeVisitor(rootVisitor, seenTypes);
        this.unionVisitor = new GraphQLUnionTypeVisitor(rootVisitor);
    }

    @Override
    public GraphQLCompositeType visit(ObjectTypeDeclaration type) {
        return objectVisitor.visit(type);
    }

    @Override
    public GraphQLCompositeType visit(UnionTypeDeclaration type) {
        return unionVisitor.visit(type);
    }
}
