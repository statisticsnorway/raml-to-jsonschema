package no.ssb.raml.graphql;

import graphql.scalars.ExtendedScalars;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLList;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLOutputType;
import graphql.schema.GraphQLSchema;
import graphql.schema.GraphQLType;
import graphql.schema.idl.RuntimeWiring;
import graphql.schema.idl.SchemaPrinter;
import org.raml.v2.api.RamlModelBuilder;
import org.raml.v2.api.RamlModelResult;
import org.raml.v2.api.model.v10.api.Library;
import org.raml.v2.api.model.v10.datamodel.ObjectTypeDeclaration;
import org.raml.v2.api.model.v10.datamodel.TypeDeclaration;

import java.io.FileReader;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class RamlToGraphQLSchemaConverter {

    public static void main(String[] args) throws IOException {
        Path root = Paths.get("/home/hadrien/Projects/SSB/gsim-raml-schema/schemas/");
        List<TypeDeclaration> models = new ArrayList<>();
        Files.walkFileTree(root, new FileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                try (FileReader fileReader = new FileReader(file.toFile())) {
                    RamlModelResult modelResult = new RamlModelBuilder().buildApi(
                            fileReader,
                            file.toAbsolutePath().toString()
                    );
                    Library library = modelResult.getLibrary();
                    if (library != null) {
                        models.addAll(library.types());
                    }
                    return FileVisitResult.CONTINUE;
                }
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

        RuntimeWiring.newRuntimeWiring()
                .scalar(ExtendedScalars.DateTime)
                .scalar(ExtendedScalars.Date)
                .scalar(ExtendedScalars.Time);

        GraphQLOutputTypeVisitor visitor = new GraphQLOutputTypeVisitor();

        SchemaPrinter printer = new SchemaPrinter(SchemaPrinter.Options.defaultOptions()
                .includeScalarTypes(true)
                .includeDirectives(true)
                .includeExtendedScalarTypes(true)
        );

        GraphQLObjectType.Builder query = GraphQLObjectType.newObject().name("Query");

        Set<GraphQLType> types = new HashSet<>();

        // TODO: Find a better way to detect interfaces.
        Set<String> interfaces = new HashSet<>();
        for (TypeDeclaration model : models) {
            if (model instanceof ObjectTypeDeclaration) {
                ObjectTypeDeclaration objectTypeDeclaration = (ObjectTypeDeclaration) model;
                for (TypeDeclaration parentType : objectTypeDeclaration.parentTypes()) {
                    interfaces.add(parentType.name());
                }
            }
        }
        models.removeIf(typeDeclaration -> {
            return interfaces.contains(typeDeclaration.name());
        });

        for (TypeDeclaration typeDeclaration : models) {
            GraphQLOutputType type = visitor.visit(typeDeclaration);
            types.add(type);
            query.field(GraphQLFieldDefinition.newFieldDefinition()
                    .name(type.getName() + "ById")
                    .type(GraphQLList.list(type))
                    .build());
        }

        schema.additionalTypes(types);
        schema.additionalType(ExtendedScalars.DateTime);
        schema.additionalType(ExtendedScalars.Date);
        schema.additionalType(ExtendedScalars.Time);
        schema.query(query);
        System.out.println(printer.print(schema.build()));
    }


}
