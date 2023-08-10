package aot.recipe;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.openrewrite.ExecutionContext;
import org.openrewrite.ScanningRecipe;
import org.openrewrite.SourceFile;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.TypeUtils;
import org.openrewrite.marker.SearchResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aot.hint.RuntimeHints;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/*
* At a high level, you will want to extend ScanningRecipe  with these broad steps:
    Define an accumulator class which can store whatever data you want it to so that:
    Override getScanner() to return a visitor which identifies whether the thing you are visiting meets your criteria, and records that result in the accumulator you defined in step 1
    Override generate() to create new source files as necessary. You can instantiate a JavaParser inline with JavaParser.fromJavaVersion() if you need to create a new class from scratch
    Override getVisitor() to perform any other modifications which are necessary

*/
@Data
@EqualsAndHashCode(callSuper = false)
public class SerializationAotHintsRecipe extends ScanningRecipe<Set<J.ClassDeclaration>> {


    private static Logger log = LoggerFactory.getLogger(SerializationAotHintsRecipe.class);

    @Override
    public Set<J.ClassDeclaration> getInitialValue(ExecutionContext ctx) {
        return new HashSet<>();
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getScanner(Set<J.ClassDeclaration> acc) {
        return new SerializableIsoVisitor(acc);
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor(Set<J.ClassDeclaration> acc) {
        return super.getVisitor(acc);
    }

    @Override
    public Collection<? extends SourceFile> generate(Set<J.ClassDeclaration> acc, Collection<SourceFile> generatedInThisCycle, ExecutionContext ctx) {
        log.info("running generate(" + acc.getClass().getName() + " " + generatedInThisCycle + ")");
        return super.generate(acc, generatedInThisCycle, ctx);
    }

    static class SerializableIsoVisitor extends JavaVisitor<ExecutionContext> {

        private final Set<J.ClassDeclaration> classDeclarations;

        SerializableIsoVisitor(Set<J.ClassDeclaration> classDeclarations) {
            this.classDeclarations = classDeclarations;
        }

        @Override
        public J.ClassDeclaration visitClassDeclaration(
                J.ClassDeclaration classDecl,
                ExecutionContext executionContext) {

            var newClassDeclaration = (J.ClassDeclaration) super.visitClassDeclaration(classDecl, executionContext);
            var seen = !newClassDeclaration.getMarkers().entries().isEmpty();
            if (seen) {
                return newClassDeclaration;
            }
            var serializable = TypeUtils.isAssignableTo(Serializable.class.getName(), newClassDeclaration.getType());
            if (serializable) {
                var result = classDecl.withName(SearchResult.found(classDecl.getName(), "the class %s was found".formatted(classDecl.getName())));
                log.info(result.print());
                this.classDeclarations.add(result);
                return result;
            }


            return newClassDeclaration;

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
