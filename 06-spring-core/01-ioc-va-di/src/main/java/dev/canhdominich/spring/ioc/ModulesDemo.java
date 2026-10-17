package dev.canhdominich.spring.ioc;

import java.io.File;
import java.util.Arrays;
import java.util.List;

public class ModulesDemo {

    public static void main(String[] args) {
        run();
    }

    public static List<String> run() {
        List<String> jars = Arrays.stream(System.getProperty("java.class.path").split(File.pathSeparator))
                .map(path -> new File(path).getName())
                .filter(name -> name.startsWith("spring-"))
                .sorted()
                .toList();
        System.out.println("spring jars on the classpath:");
        jars.forEach(jar -> System.out.println("  " + jar));
        return jars;
    }
}
