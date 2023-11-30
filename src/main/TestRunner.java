package main;

import org.junit.runner.JUnitCore;
import org.junit.runner.Request;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;

import java.util.LinkedList;
import java.util.List;

public class TestRunner {
    public static void main(String[] args) throws ClassNotFoundException {
        String classname = null;
        List<String> tests = new LinkedList<>();
        for (String arg : args) {
            if (arg.startsWith("-class=")) {
                classname = arg.replace("-class=", "");
            } else if (arg.startsWith("-test=")) {
                tests.add(arg.replace("-test=", ""));
            }
        }
        if (classname == null)
            return;
        if (tests.isEmpty())
            return;
        final JUnitCore junit = new JUnitCore();

        for (String test : tests) {
            Request torun = Request.method(Class.forName(classname), test);
            Result result = junit.run(torun);

            for (Failure failure : result.getFailures()) {
                System.out.println(failure.toString());
            }
            // System.out.println(result.wasSuccessful());
        }
    }
}