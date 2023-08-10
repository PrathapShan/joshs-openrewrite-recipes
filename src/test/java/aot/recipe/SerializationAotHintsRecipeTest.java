package aot.recipe;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import java.io.Serializable;

import static org.openrewrite.java.Assertions.java;

class SerializationAotHintsRecipeTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new SerializationAotHintsRecipe());
    }

    @Test
    void serializable() {

        var before = """
                    package com.yourorg;
                        
                    class FooBar  implements  %s {
                    }
                """.formatted(Serializable.class.getName());

        var after = """
                package com.yourorg;

                class /*~~(the class FooBar was found)~~>*/FooBar  implements  java.io.Serializable {
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