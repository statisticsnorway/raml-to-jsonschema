package no.ssb.raml.graphql;

import graphql.Scalars;
import graphql.language.EnumTypeDefinition;
import graphql.scalars.ExtendedScalars;
import graphql.scalars.datetime.DateTimeScalar;
import graphql.schema.GraphQLEnumType;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLInterfaceType;
import graphql.schema.GraphQLList;
import graphql.schema.GraphQLNonNull;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLOutputType;
import graphql.schema.GraphQLSchema;
import graphql.schema.GraphQLType;
import graphql.schema.GraphQLTypeReference;
import graphql.schema.idl.RuntimeWiring;
import org.raml.v2.api.RamlModelBuilder;
import org.raml.v2.api.RamlModelResult;
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

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class RamlToGraphQLSchemaConverter {

    public static void main(String[] args) throws IOException {
        Path root = Paths.get("/home/hadrien/Projects/SSB/gsim-raml-schema/schemas");
        List<TypeDeclaration> models = new ArrayList<>();
        Files.walkFileTree(root, new FileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                RamlModelResult modelResult = new RamlModelBuilder().buildApi(file.toFile());
                models.addAll(modelResult.getLibrary().types());
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                return FileVisitResult.TERMINATE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                return FileVisitResult.CONTINUE;
            }
        });

        GraphQLSchema.Builder schema = GraphQLSchema.newSchema();
        RuntimeWiring.newRuntimeWiring().scalar(ExtendedScalars.DateTime);
        RuntimeWiring.newRuntimeWiring().scalar(ExtendedScalars.Date);
        RuntimeWiring.newRuntimeWiring().scalar(ExtendedScalars.Time);

        Map<String, GraphQLInterfaceType> interfaces = new LinkedHashMap<>();
        Map<String, GraphQLObjectType> objects = new LinkedHashMap<>();

        ObjectVisitor objectVisitor = new ObjectVisitor();
        InterfaceVisitor interfaceVisitor = new InterfaceVisitor();


        for (TypeDeclaration typeDeclaration : models) {
            System.out.println(typeDeclaration.name());
            for (TypeDeclaration parentType : typeDeclaration.parentTypes()) {
                if (parentType instanceof ObjectTypeDeclaration) {

                    ObjectTypeDeclaration objectTypeDeclaration = (ObjectTypeDeclaration) parentType;
                    GraphQLInterfaceType.Builder visit = interfaceVisitor.visit(objectTypeDeclaration);

                    System.out.println(visit.build().toString());
                }
            }
        }
    }

    static interface TypeDeclarationVisitor<T> {
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

    static class BaseTypeDeclarationVisitor<T> implements TypeDeclarationVisitor<T> {

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

    static class ObjectVisitor extends BaseTypeDeclarationVisitor<GraphQLObjectType.Builder> {

        FieldVisitor fieldVisitor = new FieldVisitor();

        @Override
        public GraphQLObjectType.Builder visit(ObjectTypeDeclaration type) {
            GraphQLObjectType.Builder object = GraphQLObjectType.newObject();
            object.name(type.name());
            for (TypeDeclaration property : type.properties()) {
                GraphQLFieldDefinition.Builder fieldDefinition = GraphQLFieldDefinition.newFieldDefinition();
                fieldDefinition.name(property.name());
                GraphQLType graphQLType = fieldVisitor.visit(property);
                fieldDefinition.type((GraphQLOutputType) graphQLType);
                object.field(fieldDefinition);
            }
            return object;
        }
    }


    static class InterfaceVisitor extends BaseTypeDeclarationVisitor<GraphQLInterfaceType.Builder> {

        FieldVisitor fieldVisitor = new FieldVisitor();

        @Override
        public GraphQLInterfaceType.Builder visit(ObjectTypeDeclaration type) {
            GraphQLInterfaceType.Builder newInterface = GraphQLInterfaceType.newInterface();
            newInterface.name(type.name());
            for (TypeDeclaration property : type.properties()) {
                GraphQLFieldDefinition.Builder fieldDefinition = GraphQLFieldDefinition.newFieldDefinition();
                fieldDefinition.name(property.name());
                GraphQLType graphQLType = fieldVisitor.visit(property);
                fieldDefinition.type((GraphQLOutputType) graphQLType);
                newInterface.field(fieldDefinition);
            }
            return newInterface;
        }
    }

    static class EnumVisitor extends BaseTypeDeclarationVisitor<GraphQLEnumType> {

        private String enumName(StringTypeDeclaration type) {
            String fieldName = type.name();
            return fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1);
        }

        @Override
        public GraphQLEnumType visit(StringTypeDeclaration type) {
            GraphQLEnumType.Builder enumBuilder = GraphQLEnumType.newEnum();
            enumBuilder.name(enumName(type));
            for (String enumValue : type.enumValues()) {
                enumBuilder.definition(EnumTypeDefinition.newEnumTypeDefinition().name(enumValue).build());
            }
            return enumBuilder.build();
        }
    }

    /**
     * Resolve Scalar types.
     */
    static class FieldVisitor extends BaseTypeDeclarationVisitor<GraphQLType> {

        EnumVisitor enumVisitor = new EnumVisitor();

        @Override
        public GraphQLType visit(ObjectTypeDeclaration type) {
            return GraphQLTypeReference.typeRef(type.name());
        }

        @Override
        public GraphQLType visit(ArrayTypeDeclaration type) {
            return GraphQLList.list(visit(type.items()));
        }

        @Override
        public GraphQLType visit(StringTypeDeclaration type) {
            return type.enumValues().isEmpty() ? Scalars.GraphQLString : enumVisitor.visit(type);
        }

        @Override
        public GraphQLType visit(DateTypeDeclaration type) {
            return ExtendedScalars.Date;
        }

        @Override
        public GraphQLType visit(TimeOnlyTypeDeclaration type) {
            return ExtendedScalars.Time;
        }

        @Override
        public GraphQLType visit(DateTimeOnlyTypeDeclaration type) {
            return ExtendedScalars.DateTime;
        }

        @Override
        public GraphQLType visit(DateTimeTypeDeclaration type) {
            // TODO: Check what type.format() is used for.
            return ExtendedScalars.DateTime;
        }

        @Override
        public GraphQLType visit(IntegerTypeDeclaration type) {
            return Scalars.GraphQLInt;
        }

        @Override
        public GraphQLType visit(NumberTypeDeclaration type) {
            return Scalars.GraphQLFloat;
        }

        @Override
        public GraphQLType visit(BooleanTypeDeclaration type) {
            return Scalars.GraphQLBoolean;
        }

        @Override
        public GraphQLType visit(TypeDeclaration type) {
            GraphQLType graphQLType = super.visit(type);
            return type.required() ? GraphQLNonNull.nonNull(graphQLType) : graphQLType;
        }
    }

}
