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

                class /*~~(the class FooBar was found)~~>*/FooBar  implements  java.io.Serializable {

                    void foo (){ }

                    static class Hints implements org.springframework.aot.hint.RuntimeHintsRegistrar {

                        @Override
                        public void registerHints(org.springframework.aot.hint.RuntimeHints hints, java.lang.ClassLoader classLoader) {
                            hints.serialization().registerType(org.springframework.aot.hint.TypeReference.of(""));
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