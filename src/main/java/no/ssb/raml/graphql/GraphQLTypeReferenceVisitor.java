package no.ssb.raml.graphql;

import graphql.schema.GraphQLEnumType;
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

import java.util.HashSet;
import java.util.Set;


public class GraphQLTypeReferenceVisitor extends BaseTypeDeclarationVisitor<GraphQLOutputType> {

    private final TypeDeclarationVisitor<GraphQLScalarType> scalarVisitor;
    private final GraphQLEnumVisitor enumVisitor;
    private Set<String> enums = new HashSet<>();

    public GraphQLTypeReferenceVisitor() {
        this.scalarVisitor = new GraphQLScalarVisitor();
        this.enumVisitor = new GraphQLEnumVisitor();
    }

    @Override
    public GraphQLOutputType visit(TypeDeclaration type) {
        GraphQLOutputType outputType = super.visit(type);
        return type.required() ? GraphQLNonNull.nonNull(outputType) : outputType;
    }

    @Override
    public GraphQLOutputType visit(ObjectTypeDeclaration type) {
        return GraphQLTypeReference.typeRef(type.name());
    }

    @Override
    public GraphQLOutputType visit(UnionTypeDeclaration type) {
        return GraphQLTypeReference.typeRef(type.name());
    }

    @Override
    public GraphQLOutputType visit(ArrayTypeDeclaration type) {
        return GraphQLList.list(visit(type.items()));
    }

    @Override
    public GraphQLOutputType visit(StringTypeDeclaration type) {
        if (type.enumValues().isEmpty()) {
            return scalarVisitor.visit(type);
        } else {
            GraphQLEnumType enumType = enumVisitor.visit(type);
            if (enums.contains(enumType.getName())) {
                return GraphQLTypeReference.typeRef(enumType.getName());
            } else {
                enums.add(enumType.getName());
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
}
