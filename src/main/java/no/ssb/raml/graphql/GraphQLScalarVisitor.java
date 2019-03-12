package no.ssb.raml.graphql;

import graphql.Scalars;
import graphql.scalars.ExtendedScalars;
import graphql.schema.GraphQLList;
import graphql.schema.GraphQLNonNull;
import graphql.schema.GraphQLScalarType;
import graphql.schema.GraphQLType;
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

/**
 * Resolve Scalar types.
 */
class GraphQLScalarVisitor extends BaseTypeDeclarationVisitor<GraphQLScalarType> {

    @Override
    public GraphQLScalarType visit(StringTypeDeclaration type) {
        return Scalars.GraphQLString;
    }

    @Override
    public GraphQLScalarType visit(DateTypeDeclaration type) {
        return ExtendedScalars.Date;
    }

    @Override
    public GraphQLScalarType visit(TimeOnlyTypeDeclaration type) {
        return ExtendedScalars.Time;
    }

    @Override
    public GraphQLScalarType visit(DateTimeOnlyTypeDeclaration type) {
        return ExtendedScalars.DateTime;
    }

    @Override
    public GraphQLScalarType visit(DateTimeTypeDeclaration type) {
        // TODO: Check what type.format() is used for.
        return ExtendedScalars.DateTime;
    }

    @Override
    public GraphQLScalarType visit(IntegerTypeDeclaration type) {
        return Scalars.GraphQLInt;
    }

    @Override
    public GraphQLScalarType visit(NumberTypeDeclaration type) {
        return Scalars.GraphQLFloat;
    }

    @Override
    public GraphQLScalarType visit(BooleanTypeDeclaration type) {
        return Scalars.GraphQLBoolean;
    }
}
