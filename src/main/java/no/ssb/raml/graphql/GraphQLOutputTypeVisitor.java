package no.ssb.raml.graphql;

import graphql.schema.GraphQLCompositeType;
import graphql.schema.GraphQLEnumType;
import graphql.schema.GraphQLNonNull;
import graphql.schema.GraphQLOutputType;
import org.raml.v2.api.model.v10.datamodel.ObjectTypeDeclaration;
import org.raml.v2.api.model.v10.datamodel.StringTypeDeclaration;
import org.raml.v2.api.model.v10.datamodel.TypeDeclaration;
import org.raml.v2.api.model.v10.datamodel.UnionTypeDeclaration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles:
 * List, object, non null,
 */
public class GraphQLOutputTypeVisitor extends GraphQLTypeReferenceVisitor {

    private static final Logger log = LoggerFactory.getLogger(GraphQLOutputTypeVisitor.class);

    private final TypeDeclarationVisitor<GraphQLCompositeType> compositeVisitor;
    private final TypeDeclarationVisitor<GraphQLEnumType> enumVisitor;

    public GraphQLOutputTypeVisitor() {
        this.compositeVisitor = new GraphQLCompositeTypeVisitor(new GraphQLTypeReferenceVisitor());
        this.enumVisitor = new GraphQLEnumVisitor();
    }

    @Override
    public GraphQLOutputType visit(StringTypeDeclaration type) {
        return type.enumValues().isEmpty() ? enumVisitor.visit(type) : super.visit(type);
    }

    @Override
    public GraphQLOutputType visit(UnionTypeDeclaration type) {
        return compositeVisitor.visit(type);
    }

    @Override
    public GraphQLOutputType visit(ObjectTypeDeclaration type) {
        return compositeVisitor.visit(type);
    }

    @Override
    public GraphQLOutputType visit(TypeDeclaration type) {
        GraphQLOutputType outputType = super.visit(type);
        if (outputType instanceof GraphQLNonNull) {
            return (GraphQLOutputType) ((GraphQLNonNull) outputType).getWrappedType();
        } else {
            return outputType;
        }
    }
}
