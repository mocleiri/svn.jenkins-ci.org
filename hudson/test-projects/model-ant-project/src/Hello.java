public final class Hello {
    public static void main(String[] args) {
        System.out.println("Hello, world!");
    }

    // dummy method to make FindBugs report some problems
    private void unusedMethod() {
        String x = null;
        System.out.println("foo".toString()+(x.toString()));
    }
}
