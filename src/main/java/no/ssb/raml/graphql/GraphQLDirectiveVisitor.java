package no.ssb.raml.graphql;

import com.google.common.collect.ImmutableMap;
import graphql.schema.GraphQLDirective;
import org.raml.v2.api.model.v10.datamodel.TypeDeclaration;

import java.util.Map;

/**
 * Simple visitor that creates a directive out of an annotation.
 */
public class GraphQLDirectiveVisitor extends BaseTypeDeclarationVisitor<GraphQLDirective> {

    private static Map<String, String> nameMap = ImmutableMap.of(
            "types", "link"
    );

    @Override
    public GraphQLDirective visit(TypeDeclaration type) {
        String name = nameMap.getOrDefault(type.name(), type.name());
        return GraphQLDirective.newDirective().name(name).build();
    }
}
