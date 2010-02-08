package test;

import org.python.core.PySystemState;
import org.python.core.PyType;
import org.python.util.PythonInterpreter;

/**
 * Simple Java Wrapper around JythonConsole Code (written in Jython).
 * <p/>
 * This should enable JythonConsole to be embedded in a Java application.
 */
public class App {
    public static void main(String[] args) throws Exception {
        PySystemState.initialize();
        PythonInterpreter pyi = new PythonInterpreter();
        // you can pass the python.path to java to avoid hardcoding this
        // java -Dpython.path=/path/to/jythonconsole-0.0.6 EmbedExample
        // pyi.exec("sys.path.append(r'/path/to/jythonconsole-0.0.6/')");
        pyi.exec("from java.lang import Runnable");
        
        pyi.exec("class MyRunnable(Runnable):\n    def run(o):\n        print 'hello';");

        PyType t = (PyType)pyi.get("MyComparable");
        Class<?> cl = t.getProxyType();

        System.out.println(cl);
        Object o = cl.newInstance();
        ((Runnable)o).run();
    }

}