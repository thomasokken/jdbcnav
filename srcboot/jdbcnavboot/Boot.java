///////////////////////////////////////////////////////////////////////////////
// JDBC Navigator - A Free Database Browser and Editor
// Copyright (C) 2001-2023  Thomas Okken
//
// This program is free software; you can redistribute it and/or modify
// it under the terms of the GNU General Public License, version 2,
// as published by the Free Software Foundation.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
///////////////////////////////////////////////////////////////////////////////

package jdbcnavboot;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.jar.*;
import java.lang.reflect.*;

/**
 * This class creates a custom class loader, and loads the rest of the JDBC
 * Navigator application through it. This is necessary in order to be able to
 * load JDBC Drivers through a custom class loader: java.sql.DriverManager
 * refuses to use drivers that were loaded by a different class loader than the
 * one that loaded the class that calls getConnection().
 * <br>
 * In order to make the whole sleight-of-hand work, we must prevent the default
 * class loader from loading the application; we accomplish this by hiding its
 * classes in an extra top-level directory "foo" in the jar file. This way,
 * jdbcnav.Main is stored as foo/jdbcnav/Main.class, and the default class
 * loader won't find it; our class loader recognizes class names starting with
 * "jdbcnav", prepends "foo", and uses getResourceAsStream() to fetch them.
 */
public class Boot {
    // String representations of directories and zip files that the user
    // has requested to be added to the path. We use this to filter out
    // duplicates.
    private static TreeSet<String> classPath = new TreeSet<String>();

    // List of JarFiles and File objects (representing directories) to search,
    // and the corresponding URL prefixes
    private static ArrayList<Object> activeHandles = new ArrayList<Object>();
    private static ArrayList<String> activeUrls = new ArrayList<String>();
    private static SneakyClassLoader sneakyClassLoader;

    private static String fileSep = System.getProperty("file.separator");
    private static String pathSep = System.getProperty("path.separator");

    public static void main(String[] args) {
        // Make sure we don't use SneakyClassLoader to load stuff that the
        // default class loader is handling already (just a performance
        // optimization, this, to prevent us from searching jars and
        // directories where we're guaranteed not to find anything anyway).
        String cp = System.getProperty("java.class.path");
        StringTokenizer tok = new StringTokenizer(cp, pathSep);
        while (tok.hasMoreTokens()) {
            String item = tok.nextToken();
            File file = new File(item);
            if (file.isDirectory()) {
                if (item.endsWith(fileSep))
                    item = item.substring(0, item.length() - fileSep.length());
                classPath.add(item);
            } else if (file.isFile())
                classPath.add(item);
        }

        sneakyClassLoader = new SneakyClassLoader();
        Exception ex = null;
        try {
            Class mainClass = Class.forName("jdbcnav.Main", true,
                                                    sneakyClassLoader);
            Method m = mainClass.getMethod("main",
                                        new Class[] { args.getClass() });
            m.invoke(null, new Object[] { args });
        } catch (ClassNotFoundException e) {
            // From Class.forName()
            ex = e;
        } catch (NoSuchMethodException e) {
            // From Class.getMethod()
            ex = e;
        } catch (IllegalAccessException e) {
            // From Method.invoke()
            ex = e;
        } catch (InvocationTargetException e) {
            // From Method.invoke()
            ex = e;
        }
        if (ex != null)
            ex.printStackTrace(System.err);
    }

    public static void addClassPathItem(String item) {
        if (item.equals("CLASSPATH")) {
            addEnvClassPath();
            return;
        }
        if (item.endsWith(fileSep))
            item = item.substring(0, item.length() - fileSep.length());
        if (classPath.contains(item))
            return;
        classPath.add(item);
        File file = new File(item);
        String url = null;
        if (file.isFile()) {
            try {
                JarFile jarFile = new JarFile(file);
                activeHandles.add(jarFile);
                String path = file.getAbsolutePath();
                path = tr(path, fileSep, "/");
                if (!path.startsWith("/"))
                    // Windows -- no leading slash if path starts with
                    // a drive letter. We add the slash because "file:"
                    // URLs need it.
                    path = "/" + path;
                url = "jar:file:" + path + "!";
                activeUrls.add(url);
            } catch (IOException e) {}
        } else if (file.isDirectory()) {
            activeHandles.add(file);
            String path = file.getAbsolutePath();
            path = tr(path, fileSep, "/");
            if (!path.startsWith("/"))
                // Windows -- no leading slash if path starts with
                // a drive letter. We add the slash because "file:"
                // URLs need it.
                path = "/" + path;
            url = "file:" + path;
            activeUrls.add(url);
        }

        // Preload driver classes for JDBC 4.0 drivers
        if (url != null) {
            BufferedReader reader = null;
            try {
                reader = new BufferedReader(new InputStreamReader(new URL(url + "/META-INF/services/java.sql.Driver").openStream(), "UTF-8"));
                String line;
                while ((line = reader.readLine()) != null) {
                    int hash = line.indexOf('#');
                    if (hash != -1)
                        line = line.substring(0, hash);
                    line = line.trim();
                    if (line.length() > 0)
                        try {
                            Class.forName(line, true, sneakyClassLoader);
                        } catch (ClassNotFoundException e) {}
                }
            } catch (IOException e) {
                // Ignore
            } finally {
                if (reader != null)
                    try {
                        reader.close();
                    } catch (IOException e) {}
            }
        }
    }

    private static void addEnvClassPath() {
        String cp = null;
        try {
            // I'd just call System.getenv() directly, but the deprecation
            // warnings are too annoying...
            Method getenv = System.class.getMethod("getenv",
                                                new Class[] { String.class });
            cp = (String) getenv.invoke(null, new Object[] { "CLASSPATH" });
        } catch (NoSuchMethodException e) {
            // Won't happen
        } catch (IllegalAccessException e) {
            // Won't happen
        } catch (InvocationTargetException e) {
            // The JDK < 1.5 version of System.getenv() throws an Error
        }
        if (cp != null) {
            StringTokenizer tok = new StringTokenizer(cp, pathSep);
            while (tok.hasMoreTokens())
                addClassPathItem(tok.nextToken());
        }
    }

    private static class SneakyClassLoader extends ClassLoader {
        protected Class findClass(String name) throws ClassNotFoundException {
            IOException ioe = null;
            Class c;

            // Look for it in our sneaky path in the application jar file
            try {
                c = findClassInFoo(name);
                if (c != null)
                    return c;
            } catch (IOException e) {
                if (ioe == null)
                    ioe = e;
            }

            // Look for it in our "classpath"
            for (Iterator<Object> iter = activeHandles.iterator(); iter.hasNext();) {
                Object handle = iter.next();
                try {
                    if (handle instanceof JarFile)
                        c = findClassInJar((JarFile) handle, name);
                    else
                        c = findClassInDir((File) handle, name);
                    if (c != null)
                        return c;
                } catch (IOException e) {
                    if (ioe == null)
                        ioe = e;
                }
            }

            // Still nothing. The end.
            String message = "Class " + name + " not found.";
            if (ioe == null)
                throw new ClassNotFoundException(message);
            else
                throw new ClassNotFoundException(message, ioe);
        }

        private Class findClassInFoo(String name) throws IOException {
            if (!name.startsWith("jdbcnav."))
                return null;
            String resName = "/foo/" + tr(name, ".", "/") + ".class";
            InputStream is = getClass().getResourceAsStream(resName);
            if (is == null)
                return null;
            try {
                byte[] bytes = streamToBytes(is, -1);
                return defineClass(name, bytes, 0, bytes.length);
            } finally {
                try {
                    is.close();
                } catch (IOException e) {}
            }
        }

        private Class findClassInDir(File dir, String name) throws IOException {
            String fileName = dir.getPath() + fileSep
                                + tr(name, ".", fileSep) + ".class";
            File file = new File(fileName);
            if (!file.isFile())
                return null;
            InputStream is = null;
            try {
                is = new FileInputStream(file);
                int size = (int) file.length();
                byte[] bytes = streamToBytes(is, size);
                return defineClass(name, bytes, 0, bytes.length);
            } finally {
                if (is != null)
                    try {
                        is.close();
                    } catch (IOException e) {}
            }
        }

        private Class findClassInJar(JarFile jar, String name)
                                                            throws IOException {
            String entryName = tr(name, ".", "/") + ".class";
            JarEntry entry = jar.getJarEntry(entryName);
            if (entry == null)
                return null;
            InputStream is = null;
            try {
                is = jar.getInputStream(entry);
                int size = (int) entry.getSize();
                byte[] bytes = streamToBytes(is, size);
                return defineClass(name, bytes, 0, bytes.length);
            } finally {
                if (is != null)
                    try {
                        is.close();
                    } catch (IOException e) {}
            }
        }

        protected URL findResource(String name) {
            if (!name.startsWith("/"))
                name = "/" + name;
            return findNextResource(name, new IntHolder(-1));
        }

        protected Enumeration<URL> findResources(String name) {
            if (!name.startsWith("/"))
                name = "/" + name;
            return new ResourceEnumeration(name);
        }

        private URL findNextResource(String name, IntHolder index) {
            // Try the sneaky "foo" path in the main application first
            if (index.value == -1) {
                index.value++;
                if (name.startsWith("/jdbcnav/")) {
                    URL url = getClass().getResource("/foo" + name);
                    if (url != null)
                        return url;
                }
            }

            if (name.startsWith("/foo/jdbcnav/"))
                // We were re-entered; no point in looking for the sneaky "foo"
                // path in the remaining classpath...
                return null;

            // Scan the remaining classpath
            while (index.value < activeHandles.size()) {
                Object handle = activeHandles.get(index.value);
                String urlPrefix = activeUrls.get(index.value);
                index.value++;
                if (handle instanceof JarFile) {
                    JarFile jar = (JarFile) handle;
                    JarEntry entry = jar.getJarEntry(name.substring(1));
                    if (entry != null) {
                        try {
                            return new URL(urlPrefix + name);
                        } catch (MalformedURLException e) {
                            // Should not happen
                            return null;
                        }
                    }
                } else {
                    File dir = (File) handle;
                    String fileName = dir.getPath() + tr(name, "/", fileSep);
                    if (new File(fileName).exists()) {
                        try {
                            return new URL(urlPrefix + name);
                        } catch (MalformedURLException e) {
                            // Should not happen
                            return null;
                        }
                    }
                }
            }

            // Not found. The end.
            return null;
        }

        private class IntHolder {
            public int value;
            public IntHolder(int value) {
                this.value = value;
            }
        }

        private class ResourceEnumeration implements Enumeration<URL> {
            private IntHolder index;
            private String name;
            private boolean finished;
            private URL next;
            public ResourceEnumeration(String name) {
                index = new IntHolder(-1);
                this.name = name;
                finished = false;
                next = null;
            }
            public boolean hasMoreElements() {
                if (finished)
                    return false;
                if (next != null)
                    return true;
                next = findNextResource(name, index);
                if (next == null)
                    finished = true;
                return !finished;
            }
            public URL nextElement() {
                if (finished)
                    throw new NoSuchElementException();
                if (next != null) {
                    URL ret = next;
                    next = null;
                    return ret;
                }
                URL ret = findNextResource(name, index);
                if (ret == null) {
                    finished = true;
                    throw new NoSuchElementException();
                } else
                    return ret;
            }
        }
    }

    private static String tr(String value, String from, String to) {
        StringBuffer buf = new StringBuffer();
        StringTokenizer tok = new StringTokenizer(value, from, true);
        while (tok.hasMoreTokens()) {
            String t = tok.nextToken();
            if (t.equals(from))
                buf.append(to);
            else
                buf.append(t);
        }
        return buf.toString();
    }

    private static byte[] streamToBytes(InputStream is, int size)
                                                        throws IOException {
        if (size == -1) {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            while ((n = is.read(buf)) > 0)
                bos.write(buf, 0, n);
            return bos.toByteArray();
        } else {
            byte[] buf = new byte[size];
            int bytesLeft = size;
            int n;
            while ((n = is.read(buf, size - bytesLeft, bytesLeft)) > 0)
                bytesLeft -= n;
            return buf;
        }
    }
}
