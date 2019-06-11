package no.ssb.raml.graphql;

import com.google.common.base.Strings;
import graphql.schema.GraphQLEnumType;
import graphql.schema.GraphQLList;
import graphql.schema.GraphQLNonNull;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLOutputType;
import graphql.schema.GraphQLScalarType;
import graphql.schema.GraphQLTypeReference;
import graphql.schema.GraphQLUnionType;
import org.raml.v2.api.model.v10.common.Annotable;
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
import org.raml.v2.api.model.v10.datamodel.TypeInstance;
import org.raml.v2.api.model.v10.datamodel.TypeInstanceProperty;
import org.raml.v2.api.model.v10.datamodel.UnionTypeDeclaration;
import org.raml.v2.api.model.v10.declarations.AnnotationRef;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;


public class GraphQLTypeReferenceVisitor extends BaseTypeDeclarationVisitor<GraphQLOutputType> {

    private static final Logger log = LoggerFactory.getLogger(GraphQLTypeReferenceVisitor.class);

    private final TypeDeclarationVisitor<GraphQLScalarType> scalarVisitor;
    private final GraphQLEnumVisitor enumVisitor;
    private Set<String> enums = new HashSet<>();
    private Set<String> unions = new HashSet<>();

    public GraphQLTypeReferenceVisitor() {
        this.scalarVisitor = new GraphQLScalarVisitor();
        this.enumVisitor = new GraphQLEnumVisitor();
    }

    public static Optional<AnnotationRef> getAnnotation(Annotable type, String name) {
        for (AnnotationRef annotation : type.annotations()) {
            TypeDeclaration annotationType = annotation.annotation();
            if (name.equals(annotationType.name())) {
                return Optional.of(annotation);
            }
        }
        return Optional.empty();
    }

    private static Integer forList(
            final List<String> strings,
            BiFunction<CharSequence, CharSequence, String> function
    ) {
        int result = 0;

        if (strings == null || strings.size() < 2) {
            return 0;
        }

        for (int i = 0; i < strings.size() - 1; i++) {
            String prefix = function.apply(strings.get(i), strings.get(i + 1));
            result = result == 0 ?
                    prefix.length() :
                    Math.min(prefix.length(), result);
        }

        return result;
    }

    @Override
    public GraphQLOutputType visit(TypeDeclaration type) {
        // Adding field annotations.
        // Ugly because the GSIM model is not using the RAML type system
        // IE. type is defined in the annotation.
        Optional<AnnotationRef> linksAnnotation = getAnnotation(type, "types");
        GraphQLOutputType outputType;
        if (linksAnnotation.isPresent()) {
            return createLinkReference(type, linksAnnotation.get());
        } else {
            outputType = super.visit(type);
        }
        return type.required() ? GraphQLNonNull.nonNull(outputType) : outputType;
    }

    public GraphQLOutputType createLinkReference(TypeDeclaration property, AnnotationRef annotationRef) {

        // Extract the types
        TypeInstance instance = annotationRef.structuredValue();

        GraphQLOutputType outputType;
        TypeInstanceProperty annotationProperty = instance.properties().get(0);
        if (annotationProperty.isArray()) {

            String unionName = property.name();
            List<TypeInstance> values = annotationProperty.values();
            List<String> typeNames = new ArrayList<>();

            if (values.size() > 1) {

                // If more than one type, create union.
                GraphQLUnionType.Builder unionType = GraphQLUnionType.newUnionType()
                        .name(unionName)
                        .typeResolver(env -> (GraphQLObjectType) env.getSchema().getType(unionName));

                if (!unions.contains(unionName)) {
                    unions.add(unionName);

                    for (TypeInstance value : values) {
                        String possibleType = (String) value.value();
                        typeNames.add(possibleType);
                        unionType.possibleType(GraphQLTypeReference.typeRef(possibleType));
                    }

                    Integer largestPrefix = forList(typeNames, Strings::commonPrefix);
                    Integer largestSuffix = forList(typeNames, Strings::commonSuffix);
                    unionType.description(String.format("" +
                                    "Automatically generated because of anonymous type in RAML\n" +
                                    "Please consider defining your own union types or use an interface type.\n" +
                                    "The list of types are %s%s%s.",
                            typeNames,
                            largestPrefix > 0 ? ".\nCommon prefix: " + typeNames.get(0).substring(0, largestPrefix) : "",
                            largestSuffix > 0 ? ".\nCommon suffix: " + typeNames.get(0).substring(typeNames.get(0).length() - largestSuffix) : ""
                    ));
                    outputType = unionType.build();
                } else {
                    outputType = GraphQLTypeReference.typeRef(unionName);
                }
            } else {
                outputType = GraphQLTypeReference.typeRef((String) values.get(0).value());
            }
        } else {
            String typeName = (String) annotationProperty.value().value();
            outputType = GraphQLTypeReference.typeRef(typeName);
        }

        if (property instanceof ArrayTypeDeclaration) {
            outputType = GraphQLList.list(outputType);
        }
        if (property.required()) {
            outputType = GraphQLNonNull.nonNull(outputType);
        }
        return outputType;
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
