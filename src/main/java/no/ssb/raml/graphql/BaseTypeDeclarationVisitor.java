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
import org.raml.v2.api.model.v10.datamodel.UnionTypeDeclaration;

class BaseTypeDeclarationVisitor<T> implements TypeDeclarationVisitor<T> {

    @Override
    public T visit(ObjectTypeDeclaration type) {
        throw new UnsupportedOperationException();
    }

    @Override
    public T visit(StringTypeDeclaration type) {
        throw new UnsupportedOperationException();
    }

    @Override
    public T visit(IntegerTypeDeclaration type) {
        throw new UnsupportedOperationException();
    }

    @Override
    public T visit(NumberTypeDeclaration type) {
        throw new UnsupportedOperationException();
    }

    @Override
    public T visit(BooleanTypeDeclaration type) {
        throw new UnsupportedOperationException();
    }

    @Override
    public T visit(ArrayTypeDeclaration type) {
        throw new UnsupportedOperationException();
    }

    @Override
    public T visit(UnionTypeDeclaration type) {
        throw new UnsupportedOperationException();
    }

    @Override
    public T visit(DateTypeDeclaration type) {
        throw new UnsupportedOperationException();

    }

    @Override
    public T visit(TimeOnlyTypeDeclaration type) {
        throw new UnsupportedOperationException();

    }

    @Override
    public T visit(DateTimeOnlyTypeDeclaration type) {
        throw new UnsupportedOperationException();
    }

    @Override
    public T visit(DateTimeTypeDeclaration type) {
        throw new UnsupportedOperationException();
    }
}
