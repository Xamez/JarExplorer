package fr.xamez.jarexplorer;

import java.io.File;
import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;

public class JarExplorer {

    private final static String JAR_FILE_PREFIX = "jar:";
    private final static String JAR_FILE_SUFFIX = "!/";

    private final static String CLASS_FILE_EXTENSION = ".class";

    private final URL fileURL;
    private final URL jarFileURL;
    private final JarURLConnection connection;
    private final JarFile jarFile;
    private final URLClassLoader classLoader;

    public JarExplorer(File file) throws IOException {
        this.fileURL = file.toURI().toURL();
            this.jarFileURL = new URL(JAR_FILE_PREFIX + this.fileURL + JAR_FILE_SUFFIX);
        this.connection = getConnection();
        this.jarFile = connection.getJarFile();
        this.classLoader = getClassLoader();
    }

    public JarExplorer(String path) throws IOException {
        this(new File(path));
    }

    public JarFile getJarFile() {
        return jarFile;
    }

    private JarURLConnection getConnection() throws IOException {
        return (JarURLConnection) jarFileURL.openConnection();
    }

    private URLClassLoader getClassLoader() {
        final URL[] classLoaderUrls = new URL[]{fileURL};
        return new URLClassLoader(classLoaderUrls);
    }

    public Manifest getManifest() {
        try {
            return this.connection.getManifest();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public List<JarEntry> getPackages() {
        return jarFile.stream().filter(ZipEntry::isDirectory).collect(Collectors.toList());
    }

    public Optional<Class<?>> getClassByName(String className, boolean simpleName) {
        return getClasses().stream()
                .filter(clazz -> simpleName ?
                        clazz.getSimpleName().equals(className) : clazz.getName().equals(className))
                .findFirst();
    }

    public Optional<Class<?>> getClassByName(String className) {
        return getClassByName(className, false);
    }

    public Optional<Class<?>> getClassByName(String packageName, String className, boolean simpleName) {
        return getClasses(packageName).stream()
                .filter(clazz -> simpleName ?
                        clazz.getSimpleName().equals(className) : clazz.getName().equals(className))
                .findFirst();
    }

    public Optional<Class<?>> getClassByName(String packageName, String className) {
        return getClassByName(packageName, className, false);
    }

    public Set<Class<?>> getClasses(String packageName) {
        return jarFile.stream()
                .filter(jarEntry -> jarEntry.getName().startsWith(packageName) &&
                                    jarEntry.getName().endsWith(CLASS_FILE_EXTENSION))
                .map(jarEntry -> {
                    final String className = jarEntry.getName()
                                                     .replace(CLASS_FILE_EXTENSION, "")
                                                     .replace("/", ".");
                    try {
                        return classLoader.loadClass(className);
                    } catch (ClassNotFoundException e) {
                        throw new RuntimeException(e);
                    }
                })
                .collect(Collectors.toSet());
    }

    public Set<Class<?>> getClasses() {
        return getClasses("");
    }

}
