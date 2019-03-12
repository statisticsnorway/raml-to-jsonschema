package no.ssb.raml.graphql;

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

interface TypeDeclarationVisitor<T> {
    T visit(ObjectTypeDeclaration type);

    T visit(StringTypeDeclaration type);

    T visit(IntegerTypeDeclaration type);

    T visit(NumberTypeDeclaration type);

    T visit(BooleanTypeDeclaration type);

    T visit(ArrayTypeDeclaration type);

    T visit(UnionTypeDeclaration type);

    T visit(DateTypeDeclaration type);

    T visit(TimeOnlyTypeDeclaration type);

    T visit(DateTimeOnlyTypeDeclaration type);

    T visit(DateTimeTypeDeclaration type);

    default T visit(TypeDeclaration type) {
        if (type instanceof ObjectTypeDeclaration) {
            return visit((ObjectTypeDeclaration) type);
        } else if (type instanceof StringTypeDeclaration) {
            return visit((StringTypeDeclaration) type);
        } else if (type instanceof IntegerTypeDeclaration) {
            return visit((IntegerTypeDeclaration) type);
        } else if (type instanceof NumberTypeDeclaration) {
            return visit((NumberTypeDeclaration) type);
        } else if (type instanceof BooleanTypeDeclaration) {
            return visit((BooleanTypeDeclaration) type);
        } else if (type instanceof DateTypeDeclaration) {
            return visit((DateTypeDeclaration) type);
        } else if (type instanceof TimeOnlyTypeDeclaration) {
            return visit((TimeOnlyTypeDeclaration) type);
        } else if (type instanceof DateTimeOnlyTypeDeclaration) {
            return visit((DateTimeOnlyTypeDeclaration) type);
        } else if (type instanceof DateTimeTypeDeclaration) {
            return visit((DateTimeTypeDeclaration) type);
        } else if (type instanceof ArrayTypeDeclaration) {
            return visit((ArrayTypeDeclaration) type);
        } else if (type instanceof UnionTypeDeclaration) {
            return visit((UnionTypeDeclaration) type);
        } else {
            throw new IllegalArgumentException("unsupported type " + type.getClass());
        }
    }
}
