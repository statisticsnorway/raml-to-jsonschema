package no.ssb.raml.graphql;

import graphql.schema.GraphQLCompositeType;
import graphql.schema.GraphQLEnumType;
import graphql.schema.GraphQLInterfaceType;
import graphql.schema.GraphQLList;
import graphql.schema.GraphQLNonNull;
import graphql.schema.GraphQLOutputType;
import graphql.schema.GraphQLScalarType;
import graphql.schema.GraphQLTypeReference;
import org.raml.v2.api.model.v10.datamodel.ArrayTypeDeclaration;
import org.raml.v2.api.model.v10.datamodel.BooleanTypeDeclaration;
import org.raml.v2.api.model.v10.datamodel.DateTimeOnlyTypeDeclaration;
import org.raml.v2.api.model.v10.datamodel.DateTimeTypeDeclaration;
import org.raml.v2.api.model.v10.datamodel.DateTypeDeclaration;
import org.raml.v2.api.model.v10.datamodel.IntegerTypeDeclaration;
import org.raml.v2.api.model.v10.datamodel.NumberTypeDeclaration;
import org.raml.v2.api.model.v10.datamodel.ObjectTypeDeclaration;
import org.raml.v2.api.model.v10.datamodel.StringTypeDeclaration;
import org.raml.v2.api.model.v10.datamodel.TimeOnlyTypeDeclaration;
import org.raml.v2.api.model.v10.datamodel.TypeDeclaration;
import org.raml.v2.api.model.v10.datamodel.UnionTypeDeclaration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;

/**
 * Handles:
 * List, object, non null,
 */
public class GraphQLOutputTypeVisitor extends BaseTypeDeclarationVisitor<GraphQLOutputType> {

    private static final Logger log = LoggerFactory.getLogger(GraphQLOutputTypeVisitor.class);

    private final TypeDeclarationVisitor<GraphQLScalarType> scalarVisitor;
    private final TypeDeclarationVisitor<GraphQLEnumType> enumVisitor;
    private final TypeDeclarationVisitor<GraphQLCompositeType> compositeVisitor;
    private final Set<String> seenTypes = new HashSet<>();

    public GraphQLOutputTypeVisitor() {
        this.scalarVisitor = new GraphQLScalarVisitor();
        this.enumVisitor = new GraphQLEnumVisitor();
        this.compositeVisitor = new GraphQLCompositeTypeVisitor(this, seenTypes);
    }

    @Override
    public GraphQLOutputType visit(ObjectTypeDeclaration type) {
        // TODO: Maybe check after.
        String typeName = type.name();
        if (seenTypes.contains(typeName)) {
            log.debug("Already seen object {}, using type reference", type);
            return GraphQLTypeReference.typeRef(typeName);
        } else {
            seenTypes.add(typeName);
            return compositeVisitor.visit(type);
        }
    }

    @Override
    public GraphQLOutputType visit(StringTypeDeclaration type) {
        if (type.enumValues().isEmpty()) {
             return scalarVisitor.visit(type);
        } else {
            GraphQLEnumType enumType = enumVisitor.visit(type);
            if (seenTypes.contains(enumType.getName())) {
                log.debug("Already seen enum {}, using type reference", type);
                return GraphQLTypeReference.typeRef(enumType.getName());
            } else {
                seenTypes.add(enumType.getName());
                return enumType;
            }
        }
    }

    @Override
    public GraphQLOutputType visit(IntegerTypeDeclaration type) {
        return scalarVisitor.visit(type);
    }

    @Override
    public GraphQLOutputType visit(NumberTypeDeclaration type) {
        return scalarVisitor.visit(type);
    }

    @Override
    public GraphQLOutputType visit(BooleanTypeDeclaration type) {
        return scalarVisitor.visit(type);
    }

    @Override
    public GraphQLOutputType visit(ArrayTypeDeclaration type) {
        return GraphQLList.list(visit(type.items()));
    }

    @Override
    public GraphQLOutputType visit(UnionTypeDeclaration type) {
        String typeName = type.name();
        if (seenTypes.contains(typeName)) {
            log.debug("Already seen union {}, using type reference", type);
            return GraphQLTypeReference.typeRef(typeName);
        } else {
            seenTypes.add(typeName);
            return compositeVisitor.visit(type);
        }
    }

    @Override
    public GraphQLOutputType visit(DateTypeDeclaration type) {
        return scalarVisitor.visit(type);
    }

    @Override
    public GraphQLOutputType visit(TimeOnlyTypeDeclaration type) {
        return scalarVisitor.visit(type);
    }

    @Override
    public GraphQLOutputType visit(DateTimeOnlyTypeDeclaration type) {
        return scalarVisitor.visit(type);
    }

    @Override
    public GraphQLOutputType visit(DateTimeTypeDeclaration type) {
        return scalarVisitor.visit(type);
    }

    @Override
    public GraphQLOutputType visit(TypeDeclaration type) {
        GraphQLOutputType outputType = super.visit(type);
        return type.required() && !(outputType instanceof GraphQLInterfaceType)
                ? GraphQLNonNull.nonNull(outputType)
                : outputType;
    }
}
