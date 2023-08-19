package aot.recipe;


import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.*;
import org.openrewrite.internal.lang.NonNull;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.tree.J;

// Making your recipe immutable helps make them idempotent and eliminates a variety of possible bugs.
// Configuring your recipe in this way also guarantees that basic validation of parameters will be done for you by rewrite.
// Also note: All recipes must be serializable. This is verified by RewriteTest.rewriteRun() in your tests.
@Value
@EqualsAndHashCode(callSuper = false)
public class SayHelloRecipe extends Recipe {


    @Option(displayName = "Fully Qualified Class Name",
            description = "A fully qualified class name indicating which class to add a hello() method to.",
            example = "com.yourorg.FooBar")
    @NonNull
    String fullyQualifiedClassName;

    // All recipes must be serializable. This is verified by RewriteTest.rewriteRun() in your tests.
    @JsonCreator
    public SayHelloRecipe(@NonNull @JsonProperty("fullyQualifiedClassName") String fullyQualifiedClassName) {
        this.fullyQualifiedClassName = fullyQualifiedClassName;
    }

    @Override
    public String getDisplayName() {
        return "Say Hello";
    }

    @Override
    public String getDescription() {
        return "Adds a \"hello\" method to the specified class.";
    }


    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        // getVisitor() should always return a new instance of the visitor to avoid any state leaking between cycles
        return new SayHelloVisitor();
    }

    public class SayHelloVisitor extends JavaIsoVisitor<ExecutionContext> {


        private final String newHelloMethodBody = """
                public String hello(){ return "Hello, from #{}!"; }
                """;

        private final JavaTemplate template = JavaTemplate.builder(this.newHelloMethodBody).build();


        @Override
        public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext executionContext) {
            // TODO: Filter out classes that don't match the fully qualified name

            var notAMatch = classDecl.getType() == null || !classDecl.getType().getFullyQualifiedName().equals(fullyQualifiedClassName);
            if (notAMatch) {
                return classDecl;
            }

            var hasHelloMethodAlready = classDecl.getBody()
                    .getStatements()
                    .stream()
                    .filter(statement -> statement instanceof J.MethodDeclaration)
                    .map(J.MethodDeclaration.class::cast)
                    .anyMatch(method -> method.getName().getSimpleName().equals("hello"));
            if (hasHelloMethodAlready)
                return classDecl;

            // TODO: Add a `hello()` method to classes that need it
            var cursor = new Cursor(getCursor(), classDecl.getBody());
            var lastStatementCoordinates = classDecl.getBody().getCoordinates().lastStatement();
            classDecl = classDecl.withBody(this.template.apply(cursor, lastStatementCoordinates, fullyQualifiedClassName));

            return classDecl;
        }
    }
    // TODO: Override getVisitor() to return a JavaIsoVisitor to perform the refactoring
}