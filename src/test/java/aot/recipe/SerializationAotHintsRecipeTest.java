package aot.recipe;

import org.junit.jupiter.api.Test;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import java.io.Serializable;

import static org.openrewrite.java.Assertions.java;

class SerializationAotHintsRecipeTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new SerializationAotHintsRecipe())
                .parser(JavaParser
                        .fromJavaVersion()
                        .classpath("spring-core"));
    }

    @Test
    void serializable() {

        var before = """
                package com.yourorg;

                class FooBar  implements  %s {

                    void foo (){ }
                }
                """.formatted(Serializable.class.getName());

        var after = """
                package com.yourorg;
                
                import org.springframework.aot.hint.RuntimeHints;
                import org.springframework.aot.hint.RuntimeHintsRegistrar;
                import org.springframework.aot.hint.TypeReference;
                import org.springframework.context.annotation.ImportRuntimeHints;
                                
                @ImportRuntimeHints(FooBar.FooBarHints.class)class /*~~(the class FooBar was found)~~>*/FooBar  implements  java.io.Serializable {

                    void foo (){ }

                    static class FooBarHints implements RuntimeHintsRegistrar {

                        @Override
                        public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
                            hints.serialization().registerType(TypeReference.of(""));
                        }
                    }
                }
                """;
        rewriteRun(java(before, after));
    }

/*
    @Test
    void doesNotChangeExistingHello() {
        rewriteRun(
                java(
                        """
                            package com.yourorg;
                
                            class FooBar {
                                public String hello() { return ""; }
                            }
                        """
                )
        );
    }

    @Test
    void doesNotChangeOtherClasses() {
        rewriteRun(
                java(
                        """
                            package com.yourorg;
                
                            class Bash {
                            }
                        """
                )
        );
    }
*/

}