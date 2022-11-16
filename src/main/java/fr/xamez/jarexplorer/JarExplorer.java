package fr.xamez.jarexplorer;

import java.io.File;
import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;

public class JarExplorer {

    private final URL fileURL;
    private final URL jarFileURL;
    private final JarURLConnection connection;
    private final JarFile jarFile;
    private final URLClassLoader classLoader;

    public JarExplorer(File file) throws IOException {
        this.fileURL = file.toURI().toURL();
        this.jarFileURL = new URL("jar:" + this.fileURL + "!/");
        this.connection = getConnection();
        this.jarFile = connection.getJarFile();
        this.classLoader = getClassLoader();
    }

    public JarExplorer(String path) throws IOException {
        this(new File(path));
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

    public List<Class<?>> getClasses() {
        return jarFile.stream()
                .filter(jarEntry -> jarEntry.getName().endsWith(".class"))
                .map(jarEntry -> {
                        String className = jarEntry.getName().replace(".class", "").replace("/", ".");
                        try {
                            return classLoader.loadClass(className);
                        } catch (ClassNotFoundException e) {
                            throw new RuntimeException(e);
                        }
                    })
                .collect(Collectors.toList());
    }

    public List<Class<?>> getClasses(String packageName) {
        return jarFile.stream()
                .filter(jarEntry -> jarEntry.getName().startsWith(packageName) && jarEntry.getName().endsWith(".class"))
                .map(jarEntry -> {
                        final String className = jarEntry.getName().replace(".class", "").replace("/", ".");
                        try {
                            return classLoader.loadClass(className);
                        } catch (ClassNotFoundException e) {
                            throw new RuntimeException(e);
                        }
                    })
                .collect(Collectors.toList());
    }

    public JarFile getJarFile() {
        return jarFile;
    }

}
