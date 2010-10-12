package test;

import org.testng.annotations.Test;

public class AppTest  {
    @Test
    public void test1() {
        System.out.println("test1");
    }

    @Test
    public void test2() {
        System.out.println("simulated test failure");
        throw new AssertionError();
    }
}
