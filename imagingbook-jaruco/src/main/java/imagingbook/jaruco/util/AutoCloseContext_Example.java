package imagingbook.jaruco.util;

public class AutoCloseContext_Example {

    final AutoCloseContextHandle<Integer> ctx1 = new AutoCloseContextHandle<>();
    final AutoCloseContextHandle<String> ctx2 = new AutoCloseContextHandle<>();

    public void chiefmethod() {
        ctx1.setValue(10);
        ctx2.setValue("Foo");

        try(ctx1; ctx2)
        {
            submethod1();
            submethod2();
        }
    }

    private void submethod1() {
        System.out.println("  submethod1 " + ctx1.getValue());
    }

    private void submethod2() {
        System.out.println("  submethod2 " + ctx2.getValue());
    }


    public static void main(String[] args) {
        new AutoCloseContext_Example().chiefmethod();
    }
}
