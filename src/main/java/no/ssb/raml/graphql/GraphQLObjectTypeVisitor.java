package no.ssb.raml.graphql;

import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLInterfaceType;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLOutputType;
import graphql.schema.GraphQLTypeReference;
import org.raml.v2.api.model.v10.datamodel.ObjectTypeDeclaration;
import org.raml.v2.api.model.v10.datamodel.TypeDeclaration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Set;

public class GraphQLObjectTypeVisitor extends BaseTypeDeclarationVisitor<GraphQLObjectType> {

    private static final Logger log = LoggerFactory.getLogger(GraphQLObjectTypeVisitor.class);

    private final TypeDeclarationVisitor<GraphQLOutputType> rootVisitor;
    private final TypeDeclarationVisitor<GraphQLInterfaceType> interfaceVisitor;
    private final Set<String> seenTypes;

    public GraphQLObjectTypeVisitor(TypeDeclarationVisitor<GraphQLOutputType> rootVisitor, Set<String> seenTypes) {
        this.rootVisitor = rootVisitor;
        this.interfaceVisitor = new GraphQLInterfaceTypeVisitor(rootVisitor);
        this.seenTypes = seenTypes;
    }

    @Override
    public GraphQLObjectType visit(ObjectTypeDeclaration type) {
        GraphQLObjectType.Builder builder = GraphQLObjectType.newObject();
        builder.name(type.name());

        if (type.description() != null) {
            builder.description(type.description().toString());
        }

        // Check interface.
        List<TypeDeclaration> parentTypes = type.parentTypes();
        if (!parentTypes.isEmpty()) {
            for (TypeDeclaration parentType : parentTypes) {

                // Ignore root object type.
                if ("object".equals(parentType.name())) {
                    continue;
                }

                String typeName = parentType.name();
                if (seenTypes.contains(typeName)) {
                    log.debug("Already seen the interface {}, using type reference", type);
                    builder.withInterface(GraphQLTypeReference.typeRef(typeName));
                } else {
                    seenTypes.add(typeName);
                    GraphQLInterfaceType interfaceType = interfaceVisitor.visit(parentType);
                    builder.withInterface(interfaceType);
                }
            }
        }

        // TODO: Factorize.
        for (TypeDeclaration property : type.properties()) {
            GraphQLFieldDefinition.Builder fieldDefinition = GraphQLFieldDefinition.newFieldDefinition();
            fieldDefinition.name(property.name());
            GraphQLOutputType graphQLType = rootVisitor.visit(property);
            fieldDefinition.type(graphQLType);
            builder.field(fieldDefinition);
        }
        return builder.build();

    }
}
