package no.ssb.raml.graphql;

import graphql.schema.GraphQLCompositeType;
import graphql.schema.GraphQLInterfaceType;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLOutputType;
import graphql.schema.GraphQLUnionType;
import org.raml.v2.api.model.v10.datamodel.ObjectTypeDeclaration;
import org.raml.v2.api.model.v10.datamodel.UnionTypeDeclaration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;

class GraphQLCompositeTypeVisitor extends BaseTypeDeclarationVisitor<GraphQLCompositeType> {

    private static final Logger log = LoggerFactory.getLogger(GraphQLCompositeTypeVisitor.class);

    private final TypeDeclarationVisitor<GraphQLObjectType> objectVisitor;
    private final TypeDeclarationVisitor<GraphQLUnionType> unionVisitor;
    private final TypeDeclarationVisitor<GraphQLInterfaceType> interfaceVisitor;

    private final Set<String> interfaces;

    GraphQLCompositeTypeVisitor(TypeDeclarationVisitor<GraphQLOutputType> fieldVisitor) {
        this.interfaces = new HashSet<>();
        this.objectVisitor = new GraphQLObjectTypeVisitor(fieldVisitor, this.interfaces);
        this.unionVisitor = new GraphQLUnionTypeVisitor();
        this.interfaceVisitor = new GraphQLInterfaceTypeVisitor(fieldVisitor);
    }

    @Override
    public GraphQLCompositeType visit(ObjectTypeDeclaration type) {
        // Return as interface if we have seen this type as such.
        String typeName = type.name();
        if (interfaces.contains(typeName)) {
            log.info("converting {} ({}) as interface", typeName, type.hashCode());
            return interfaceVisitor.visit(type);
        } else {
            return objectVisitor.visit(type);
        }
    }

    @Override
    public GraphQLCompositeType visit(UnionTypeDeclaration type) {
        return unionVisitor.visit(type);
    }
}
