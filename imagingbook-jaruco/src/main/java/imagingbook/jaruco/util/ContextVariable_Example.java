package imagingbook.jaruco.util;

/**
 * Simple (yet still unnecessary) mechanism for passing values from
 * outer methods to inner methods with temporal scope.
 * No clean scoping possible!
 */
public class ContextVariable_Example {

    // Variables are invalidated when used in try-with-resource

    // context variables are declared at the object level, so they
    // accessible to all methods:
    final ContextVariable<Integer> ctx1 = new ContextVariable<>();
    final ContextVariable<String> ctx2 = new ContextVariable<>();

    // -------------------------------------------------------------------

    public void chiefmethod() {

        ctx1.set(10);
        ctx2.set("Foo");

        try(ctx1; ctx2) {
            submethod1();
            ctx2.set("Bar");
            submethod2();
        }   // ctx1 amd ctx2 are invalidated

        submethod3();
    }

    // -------------------------------------------------------------------

    private void submethod1() {
        System.out.println("  submethod1 " + ctx1.get());
        System.out.println("  submethod1 " + ctx2.get());
        ctx1.set(99);
    }

    private void submethod2() {
        System.out.println("  submethod2 " + ctx1.get());
        System.out.println("  submethod2 " + ctx2.get());
    }

    private void submethod3() {
        try {
            System.out.println("  submethod3 " + ctx2.get()); // exception
        } catch (Exception e) {System.out.println("context invalid!");}
    }

    // -------------------------------------------------------------------

    public static void main(String[] args) {
        new ContextVariable_Example().chiefmethod();
    }
}
