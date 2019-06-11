package no.ssb.raml.graphql;

import graphql.schema.GraphQLDirective;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLInterfaceType;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLOutputType;
import graphql.schema.GraphQLTypeReference;
import org.raml.v2.api.model.v10.datamodel.ObjectTypeDeclaration;
import org.raml.v2.api.model.v10.datamodel.TypeDeclaration;
import org.raml.v2.api.model.v10.declarations.AnnotationRef;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Set;

public class GraphQLObjectTypeVisitor extends BaseTypeDeclarationVisitor<GraphQLObjectType> {

    private static final Logger log = LoggerFactory.getLogger(GraphQLObjectTypeVisitor.class);

    private final TypeDeclarationVisitor<GraphQLOutputType> fieldVisitor;
    private final TypeDeclarationVisitor<GraphQLInterfaceType> interfaceVisitor;
    private final GraphQLDirectiveVisitor directiveVisitor;
    private final Set<String> interfaces;

    public GraphQLObjectTypeVisitor(TypeDeclarationVisitor<GraphQLOutputType> fieldVisitor, Set<String> interfaces) {
        this.fieldVisitor = fieldVisitor;
        this.interfaces = interfaces;
        this.interfaceVisitor = new GraphQLInterfaceTypeVisitor(fieldVisitor);
        this.directiveVisitor = new GraphQLDirectiveVisitor();
    }

    @Override
    public GraphQLObjectType visit(ObjectTypeDeclaration type) {
        GraphQLObjectType.Builder builder = GraphQLObjectType.newObject();
        builder.name(type.name());

        if (type.description() != null) {
            builder.description(type.description().value());
        }

        // Check interface.
        List<TypeDeclaration> parentTypes = type.parentTypes();
        if (!parentTypes.isEmpty()) {
            for (TypeDeclaration parentType : parentTypes) {
                String interfaceName = parentType.name();
                if ("object".equals(interfaceName)) {
                    continue;
                }
                if (interfaces.add(interfaceName)) {
                    builder.withInterface(interfaceVisitor.visit(parentType));
                } else {
                    builder.withInterface(GraphQLTypeReference.typeRef(interfaceName));
                }
                // TODO: Ask for an annotation.
                builder.withDirective(GraphQLDirective.newDirective().name("domain").build());
            }
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
            builder.field(fieldDefinition);
        }
        return builder.build();

    }
}
