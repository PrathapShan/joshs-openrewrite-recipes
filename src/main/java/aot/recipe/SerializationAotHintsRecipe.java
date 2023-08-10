package aot.recipe;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.TypeUtils;
import org.openrewrite.marker.SearchResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.aot.hint.TypeReference;

import java.io.Serializable;

/*
* At a high level, you will want to extend ScanningRecipe  with these broad steps:
    Define an accumulator class which can store whatever data you want it to so that:
    Override getScanner() to return a visitor which identifies whether the thing you are visiting meets your criteria, and records that result in the accumulator you defined in step 1
    Override generate() to create new source files as necessary. You can instantiate a JavaParser inline with JavaParser.fromJavaVersion() if you need to create a new class from scratch
    Override getVisitor() to perform any other modifications which are necessary

*/
@Data
@EqualsAndHashCode(callSuper = false)
public class SerializationAotHintsRecipe extends Recipe {


    private static Logger log = LoggerFactory.getLogger(SerializationAotHintsRecipe.class);

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new SerializableIsoVisitor();
    }



    static class SerializableIsoVisitor extends JavaVisitor<ExecutionContext> {

        @Override
        public J visitBlock(J.Block block, ExecutionContext executionContext) {
            var parent = getCursor().getParent().getValue();
            var code = """
                      static class Hints implements RuntimeHintsRegistrar {
                                      
                                    @Override
                                    public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
                                        hints.serialization().registerType(TypeReference.of(""));
                                    }
                                }
                    """;
            var seen = executionContext.getMessage("seen", null);
            if (parent instanceof J.ClassDeclaration classDeclaration && seen.equals(classDeclaration.getId())) {
                var markers = classDeclaration.getName().getMarkers();
                var searchResult = markers.findFirst(SearchResult.class);
                if (searchResult.isEmpty()) {
                    log.info("found " + seen);
                    var builder = JavaTemplate.builder(code).javaParser(JavaParser.fromJavaVersion()
                            .classpath("spring-core")
                    );
                    var topLevelImports = new String[]{RuntimeHints.class.getName(),
                            ClassLoader.class.getName(),
                            RuntimeHintsRegistrar.class.getName(),
                            TypeReference.class.getName()};
                    var javaTemplate = builder
                            .imports(topLevelImports)
                            .build();
                    for (var p : topLevelImports)
                        maybeAddImport(p);
                    return javaTemplate.apply(getCursor(), block.getCoordinates().lastStatement());

                }
            }

            return super.visitBlock(block, executionContext);
        }

        @Override
        public J.ClassDeclaration visitClassDeclaration(
                J.ClassDeclaration classDecl,
                ExecutionContext executionContext) {

            var seen = !classDecl.getName().getMarkers().entries().isEmpty();
            var serializable = TypeUtils.isAssignableTo(Serializable.class.getName(), classDecl.getType());
            if (!seen && serializable) {
                executionContext.putMessage("seen", classDecl.getId());
                classDecl = classDecl.withName(SearchResult.found(classDecl.getName(), "the class %s was found".formatted(classDecl.getName())));
            }
            return (J.ClassDeclaration) super.visitClassDeclaration(classDecl, executionContext);
        }


    }


    @Override
    public String getDisplayName() {
        return "RuntimeHints#serialization";
    }

    @Override
    public String getDescription() {
        return "Identifies Spring components that implement {} and registers a {} for it.".formatted(
                Serializable.class.getName(),
                RuntimeHints.class.getName()
        );
    }
}
