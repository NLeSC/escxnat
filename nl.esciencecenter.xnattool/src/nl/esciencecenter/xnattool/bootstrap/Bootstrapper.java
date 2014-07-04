/*
 * Copyright 2012-2014 Netherlands eScience Center.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License. 
 * You may obtain a copy of the License at the following location:
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 * For the full license, see: LICENSE.txt (located in the root folder of this distribution).
 * ---
 */
// source:

package nl.esciencecenter.xnattool.bootstrap;

// NO EXTERNAL IMPORTS HERE 
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Properties;

import javax.swing.JOptionPane;

/**
 * Universal Bootstrapper class for both Windows, Linux and Mac Os. <br>
 * This class must be able work as a stand-alone class!<br>
 * Cannot reference external classes if not in the same bootstrapper jar !
 * 
 * @author Piter T. de Boer
 */
public class Bootstrapper
{
    public static class BooleanHolder
    {
        public boolean value = false;
    };

    public static final String JAVA_LIBRARY_PATH_PROP = "java.library.path";

    private static final Class<?>[] MAIN_PARAMS_TYPE = {
        String[].class
    };

    /**
     * Static BootStrapper configuration
     */
    public static class BootOptions
    {
        // public String toolName = "bootStrapper";

        public String toolPrefix = "boot";

        // public String toolConfigSubdir = ".ptkrc";

        public String confSubDir = "etc";

        public String libSubDir = "lib";

        public String binSubDir = "bin";

        public String javaVersion = "1.7";

        /**
         * Default library sub-directories explicitly added to bootstrapper classpath. These directories are added as
         * classpath <em>directories</em>.
         */
        public String libSubDirs[] =
        {
                "icons",
                "win32",
                "win64",
                "mac",
                "linux"
        };

        /**
         * Library subdirectories explicitly ignored when scanning the lib/ directory for jar files.
         */
        public String libIgnoreDirs[] =
        {
                "plugins"
        };
    }

    protected static boolean debug = false;

    // ========================================================================
    //
    // ========================================================================

    protected BootOptions bootOptions = new BootOptions();

    public String APPRC_PROP_FILE = "";

    public String APP_INSTALL_PROP = "";

    public String APP_SYSCONFDIR_PROP = "";

    public String APP_INSTALL_LIBDIR_PROP = "";

    public String APP_INSTALL_ENV = "";

    protected ArrayList<URL> classpathUrls = new ArrayList<URL>();

    /**
     * ${APP_PREFIX} install or startup directory (minus ./lib or ./bin) Use URL to be applet and webstart compatible.
     */
    protected URL baseUrl = null;

    public Bootstrapper(BootOptions options)
    {
        if (options == null)
            options = new BootOptions();

        this.bootOptions = options;

        String toolPrefix = bootOptions.toolPrefix;

        if (toolPrefix == null)
            toolPrefix = "env";

        APPRC_PROP_FILE = toolPrefix + ".prop";
        APP_INSTALL_PROP = toolPrefix + ".install";
        APP_SYSCONFDIR_PROP = toolPrefix + ".install.sysconfdir";
        APP_INSTALL_LIBDIR_PROP = toolPrefix + ".install.libdir";
        APP_INSTALL_ENV = toolPrefix.toUpperCase() + "_INSTALL";

    }

    /**
     * Set this before booting to ensure correct base URL environemnt in the case of Applet of Webstart setup.
     * 
     * More secure runtime environments might restrict access only to resources which are a subdirectory of this base
     * URL.
     */
    public void setBaseURL(URL baseurl)
    {
        baseUrl = baseurl;
    }

    /**
     * Checks startup environment and set installation parameters:<br>
     * - Check java 1.6 version.<br>
     * - Add all .jar files from ./lib and ${globus.install}/lib.<br>
     * - Sets skeleton CLASSPATH adding:
     * 
     * <pre>
     * <li> ./    ; APP_INSTALL     ${APP_PREFIX.install} 
     * <li> ./etc ; APP_SYSCONFDIR  ${APP_PREFIX.install.sysconfdir} 
     * <li> ./lib ;                 ${APP_PREFIX.install.libdir} 
     * <li> ./lib/linux ;           ${APP_PREFIX.install.libdir}/linux
     * <li> ./lib/win32 ;           ${APP_PREFIX.install.libdir}/win32
     * <li> ./lib/win64 ;           ${APP_PREFIX.install.libdir}/win32
     * </pre>
     * <p>
     * Bootstrap configuration:<br>
     * 0) Assume default APP_INSTALL using environment variable APP_INSTALL or (if not set) by stripping ./lib or ./bin
     * from startup path.<br>
     * 1) check property "${APP_PREFIX}.install.sysconfdir" for configuration.<br>
     * -1a) if not set, check APP_INSTALL and specify sysconfdir as ${APP_INSTALL}/etc<br>
     * 2) load configuration file from ${APP_PREFIX.install.sysconfigdir}/${APP_PREFIX}rc.prop<br>
     * -2a) when not found, try to load /etc/${APP_PREFIX}rc.prop<br>
     * 3) (re)set installation location taken from ${APP_PREFIX}rc.prop, overriding previous settings or keep defaults
     * (this allows to keep defaults without setting the properties explicitly in vlerc.prop)<br>
     */
    public void checkSetAppEnvironment() throws Exception
    {
        String javaHome = System.getProperty("java.home");
        debugPrintf(" - java home    =%s\n", javaHome);
        String versionStr = System.getProperty("java.version");
        debugPrintf(" - java version =%s\n", versionStr);

        // Warning: doing string compare where int compare should be used:
        if ((versionStr != null) && (versionStr.compareToIgnoreCase(bootOptions.javaVersion) < 0))
        {
            errorPrintf("Bootstrapper: Wrong java version. Need at least '%s' This is:%s\n", bootOptions.javaVersion,
                    versionStr);

            JOptionPane.showMessageDialog(null, "Wrong java version. Need " + bootOptions.javaVersion + " or higher.\n"
                    + "If java " + bootOptions.javaVersion + " is installed "
                    + "set your JAVA_HOME to the right location.\n" + "This version  =" + versionStr + "\n"
                    + "Java location =" + javaHome + "\n"
                    + "The Program will try to continue ...", "Error", JOptionPane.ERROR_MESSAGE);

            // Continue anyway.
        }

        // java property: -DAPP_PREFIX.install=
        String installDir = System.getProperty(APP_INSTALL_PROP);

        // envvar: APP_INSTALL=...
        if (installDir == null)
            installDir = System.getProperty(APP_INSTALL_ENV);

        String urlstr = null;

        // ***
        // Get default installation directory as URL
        // ***

        if ((installDir == null) && (baseUrl == null))
        {
            // ***
            // Important: URI/URL uses forward slashes, native path must use "/" !
            // ***

            // getProtectionDomain???
            URL url = this.getClass().getProtectionDomain().getCodeSource().getLocation();
            debugPrintf(" - source location URL   = %s\n", url);

            // parent dir of code base location
            // URL has FORWARD slashes!
            urlstr = url.toString();

            int len = urlstr.length();
            int i = 0;

            // dirname of path:
            for (i = len - 1; ((i > 0) && (urlstr.charAt(i) != '/')); i--)
            {
                ;
            }
            // nop;

            urlstr = urlstr.substring(0, i); // 0 to position of path seperator

            len = urlstr.length();

            // auto strip 'lib' from path when started directly from ./lib/...
            if (urlstr.endsWith("/" + bootOptions.libSubDir))
            {
                urlstr = urlstr.substring(0, len - 4);
            }

            // auto strip 'lib' from path when started directly from ./lib/...
            else if (urlstr.endsWith("/" + bootOptions.binSubDir))
            {
                urlstr = urlstr.substring(0, len - 4);
            }

            // update installDir
            baseUrl = new URL(urlstr);

        }
        else
        {
            // create URL
            baseUrl = new File(installDir).toURI().toURL(); // unify File
                                                            // location
        }

        // get decoded & unified path using URI class:
        installDir = new URI(baseUrl.toString()).getPath();

        debugPrintf(" - normalized installdir = %s\n", installDir);

        // ***
        // Check APP_PREFIX.install.sysconfdir (if speficied)
        // ***

        String sysconfdir = System.getProperty(APP_SYSCONFDIR_PROP);

        if (sysconfdir == null)
        {
            // NATIVE File seperator: "APP_INSTALL/etc"
            sysconfdir = installDir + File.separator + bootOptions.confSubDir;
        }

        // load vlertc.prop from APP_INSTALL/etc or APP_SYSCONFDIR or
        // APP_PREFIXrc.prop
        Properties appProps = getAppProperties(sysconfdir);

        //
        // ***
        // Check whether ${APP_PREFIX}rc.prop defines new installation
        // directories!
        // ***
        //

        // alternative ${APP_PREFIX}.install directory !
        String val = appProps.getProperty(APP_INSTALL_PROP);

        if (val != null)
            installDir = val;

        // Alternative lib directory ! (/usr/share/lib/${APP_PREFIX})
        String libDir = appProps.getProperty(APP_INSTALL_LIBDIR_PROP);

        // Default to APP_INSTALL/lib
        if (libDir == null)
            libDir = installDir + "/" + bootOptions.libSubDir;

        // Warning: this will change sysConfDir if not the same as specified at
        // startup !
        String sysconfDir = appProps.getProperty(APP_SYSCONFDIR_PROP);

        // default to APP_INSTALL/etc
        if (sysconfDir == null)
            sysconfDir = installDir + "/" + bootOptions.confSubDir;

        // Consistency: make sure System property
        // ${APP_PREFIX}.install.sysconfdir & ${APP_PREFIX}.install
        // matches the one dymically configured sysconfDir !
        // (is rare, this means a misconfigured system!)
        System.setProperty(APP_SYSCONFDIR_PROP, sysconfDir);
        System.setProperty(APP_INSTALL_PROP, installDir);

        debugPrintf(" - installdir  =%s\n", installDir);
        debugPrintf(" - sysconfdir  =%s\n", sysconfDir);
        debugPrintf(" - libdir      =%s\n", libDir);

        val = null;

        // -------------------------------------------------------------------
        /*
         * Current 'skeleton' classpath/ All other classpath directories should start from this skeleton (For example
         * the ./viewers, ./applets, ./icons, directories) For example to load a viewer specify "viewers/myviewer.jar".
         * When started from windows, the ./lib/win32/viewers directory should match also as with ./lib/viewers (for
         * pure java viewers) as with ./lib/win32/viewers) for windows only viewers !.
         * CLASSPATH=$APP_INSTALL:$APP_INSTALL/etc:...
         */
        // ---------------------------------------------------------------------

        // ClassPath directories (defaults):
        // ./
        // ./etc/
        // ./lib/
        // ./lib/icons/
        // ./lib/win32
        // ./lib/win64
        // ./lib/linux

        // Add CLASSPATH Environment (bug: new classloader doesn't respect
        // CLASSPATH)
        String cpstr = System.getenv("CLASSPATH");
        if (cpstr != null)
        {
            // ---
            // Important: Use OS depended path separator here!
            // ---
            String paths[] = cpstr.split(File.pathSeparator);
            if ((paths != null) && (paths.length > 0))
                for (int i = 0; i < paths.length; i++)
                    classpathUrls.add(new URL("file:///" + paths[i]));
        }

        addDirToClasspath(installDir + "/"); // APP_INSTALL
        // add directory structure (without jars!)
        addDirToClasspath(sysconfDir + "/"); // APP_INSTALL/etc

        addDirToClasspath(libDir + "/"); // APP_INSTALL/lib

        for (String subDir : bootOptions.libSubDirs)
            addDirToClasspath(libDir + "/" + subDir);

        // recursive read jars from:
        addJarsToLibUrls(libDir, true, 0);

        addJarToLibUrls(javaHome + "/lib/jfxrt.jar", true);

        // Add java.libary.path
        setJavaLibraryPath(appProps);

    }

    String resolve(String parent, String rel_path, BooleanHolder bool)
    {
        String path = null;

        if (bool != null)
            bool.value = false; // default

        if (rel_path.startsWith("/"))
        {
            path = rel_path;
        }

        // Arg: Windows hack, detect 'C:/blah'
        else if (rel_path.charAt(1) == ':')
        {
            path = rel_path;
        }
        else if (rel_path.startsWith(bootOptions.libSubDir))
        {
            // Replace "./lib" with "${${APP_PREFIX}.install.libdir} !
            path = parent + "/" + rel_path.substring(4, rel_path.length());
        }
        else
        {
            // relative path (without "lib" prefix), but starting from LIBDIR !
            path = parent + "/" + rel_path;

            if (bool != null)
                bool.value = true;
        }

        return path;
    }

    public void setJavaLibraryPath(Properties props)
    {
        // check system:
        String java_library_path = System.getProperty(JAVA_LIBRARY_PATH_PROP);

        if (java_library_path == null)
            java_library_path = "";
        else
        {
            // use platform specific path seperator !
            java_library_path += File.pathSeparator;
        }

        String val = props.getProperty(JAVA_LIBRARY_PATH_PROP);
        if (val != null)
            java_library_path += val;

        // Setting the java.library.path here still doesn't work !
        // Keeping the code, anyway.
        // java.library.path or LD_LIBRARY_PATH has to be specified
        // at JVM startup. :-(.

        System.setProperty(JAVA_LIBRARY_PATH_PROP, java_library_path);

        debugPrintf(">>>" + JAVA_LIBRARY_PATH_PROP + "=%s\n", java_library_path);
    }

    /**
     * Check for ${APP_PROP}rc.prop in SYSCONFDIR. - ${APP_INSTALL}/etc/${APP_PREFIX}rc.prop -
     * ${APP_SYSCONFDIR}/etc/${APP_PREFIX}rc.prop - /etc/${APP_PREFIX}rc.prop
     * 
     * @returns property set if loaded or EMPTY property set when failed !
     * 
     */
    public Properties getAppProperties(String sysconfdir)
    {
        Properties props = new Properties();

        // use NATIVE File.sepator !
        // Default $APP_INSTALL/etc/${APP_PREFIX}rc.prop
        String apprcprop = sysconfdir + "/" + APPRC_PROP_FILE;

        try
        {
            File rcfile = new File(apprcprop);
            FileInputStream inpfs;

            if (rcfile.exists() == true)
            {
                inpfs = new FileInputStream(rcfile);
                props.load(inpfs);
            }
            else
            {
                // last resort: Check /etc/vletrc.prop under unix style path
                // :/etc/
                rcfile = new File("/etc/" + APPRC_PROP_FILE);
                if (rcfile.exists() == true)
                {
                    inpfs = new FileInputStream(rcfile);
                    props.load(inpfs);
                }
            }
        }
        catch (FileNotFoundException e)
        {
            errorPrintf("***Exception:%s", e);
            e.printStackTrace();
        }
        catch (IOException e)
        {
            errorPrintf("***Exception:%s", e);
            e.printStackTrace();
        }

        return props;
    }

    public File getPath(String filePath, boolean isDir, boolean mustExist) throws Exception
    {
        // if dir is a remote url, FILE will complain:

        File javaFile = new File(filePath);

        // do some sanity checks:

        if (isDir)
        {
            if ((javaFile.exists() == false) || (javaFile.isDirectory() == false))
            {
                if (mustExist)
                {
                    JOptionPane.showMessageDialog(null,
                            "Cannot find directory:'" + javaFile + "' "
                                    + "Installation might be corrupt or misconfigured. Trying to continue...",
                            "Error",
                            JOptionPane.ERROR_MESSAGE);
                }
                // continue!
                errorPrintf("***Error: Can't resolve Directory:%s\n", javaFile);
            }
        }
        else
        {
            if ((javaFile.exists() == false) && (javaFile.isFile() == false))
            {
                if (mustExist)
                {
                    JOptionPane.showMessageDialog(null,
                            "Cannot find file:'" + javaFile + "' "
                                    + "Installation might be corrupt or misconfigured. Trying to continue...",
                            "Error",
                            JOptionPane.ERROR_MESSAGE);
                }
                // continue!
                errorPrintf("***Error: Can't resolve File:%s\n", javaFile);
            }
        }

        try
        {
            // resolve File.
            return javaFile.getCanonicalFile();
        }
        catch (IOException e)
        {
            if (mustExist)
            {
                throw new Exception("IOException: Failed to get the canonical path of of " + javaFile);
            }
            else
            {
                errorPrintf("***Error: getCanonicalFile() Failed for directory:%s\n", javaFile);
                return null;
            }
        }
    }

    public void addDirToClasspath(String dir)
    {
        // Use URL convention for directory URLs/Paths:

        if (dir.endsWith("/") == false)
            dir = dir + "/";

        debugPrintf(" - adding directory:%s\n", dir);

        try
        {
            this.classpathUrls.add(new URL("file:///" + dir));
        }
        catch (MalformedURLException e)
        {
            errorPrintf("***Error: Exception:%s", e);
            e.printStackTrace();
        }
    }

    public boolean addJarToLibUrls(String jarPath, boolean mustExist) throws Exception
    {
        java.io.File jarFile = getPath(jarPath, false, mustExist);

        if (mustExist)
        {
            if ((jarFile == null) || (jarFile.exists() == false))
            {
                errorPrintf("***Error: can not find mandatory jar file:%s\n", jarFile);
                return false;
            }
        }

        if (jarFile == null)
        {
            debugPrintf(" - warning: jarFile is null for:%s\n" + jarPath);
            return false;
        }

        URL url = jarFile.toURI().toURL();
        if (url == null)
        {
            errorPrintf("***Error: URL of jarFile is null for:%s\n" + jarPath);
            return false;
        }

        this.classpathUrls.add(url);
        debugPrintf(" - adding jarurl:%s\n", url);
        return true;
    }

    public boolean addJarsToLibUrls(String libDir, boolean recurse, int dirLevel) throws Exception
    {
        debugPrintf(" > [%2d] Scanning directory for jar files: %s\n", dirLevel, libDir);

        File dir = getPath(libDir, true, true);

        if ((dir == null) || (dir.exists() == false) || (dir.isDirectory() == false))
        {
            // ignore faulty configurations.
            errorPrintf(" > [%2d] ***Error: lib directory does not exists or is unreadable:%s\n", dirLevel, dir);
            return false;
        }

        try
        {
            File[] files = dir.listFiles();
            for (int i = 0; i < files.length; i++)
            {
                String fileName = files[i].getName();
                String filepath = files[i].getPath();

                // Debug("Checking:"+filepath);
                // skip ./lib/plugins (but only onlevel 0)
                if ((dirLevel == 0) && (fileName.compareToIgnoreCase("plugins") == 0))
                {
                    debugPrintf(" > [%2d] - Ignoring custom plugin directory:%s\n", dirLevel, filepath);
                }
                else if (filepath.endsWith(".jar") == true)
                {
                    // USe toURI().toURL() to normalize file URLs !
                    URL url = files[i].toURI().toURL();
                    this.classpathUrls.add(url);
                    debugPrintf(" > [%2d] - Adding jarurl:%s\n", dirLevel, url);

                }
                else if ((files[i].isDirectory() == true) && (recurse = true))
                {
                    addJarsToLibUrls(files[i].getPath(), recurse, dirLevel + 1);
                }
            }
        }
        catch (IOException e)
        {
            throw new Exception("BootrapException: Error during startup processing, directory=" + libDir, e);
        }

        return true;
    }

    public void launch(String launchClass, String[] launchArgs) throws Exception
    {
        // set the whole APP environment:
        this.checkSetAppEnvironment();

        URL[] urlJars = new URL[this.classpathUrls.size()];
        urlJars = this.classpathUrls.toArray(urlJars);

        // ClassLoader parent=ClassLoader.getSystemClassLoader();
        // Keep current classloader so current classpath which was used
        // to load the bootloader is respected:
        ClassLoader parent = Thread.currentThread().getContextClassLoader();

        // context class loader:
        URLClassLoader loader = new URLClassLoader(urlJars, parent);
        Thread.currentThread().setContextClassLoader(loader);

        // start main class
        try
        {
            Class<?> mainClass = loader.loadClass(launchClass);

            Method mainMethod = mainClass.getMethod("main", MAIN_PARAMS_TYPE);

            mainMethod.invoke(null, new Object[] {
                launchArgs
            });
        }
        catch (ClassNotFoundException e)
        {
            throw new Exception("ClassNotFoundException:" + "Class '" + launchClass + "'.\n" + e, e);
        }
        catch (NoSuchMethodException e)
        {
            throw new Exception("NoSuchMethodException: main() method not found in class '" + launchClass + "'\n"
                    + e.getMessage(), e);
        }
        catch (InvocationTargetException e)
        {
            throw new Exception("InvocationTargetException:" + e, e);
        }
        catch (IllegalAccessException e)
        {
            throw new Exception("IllegalAccessException:" + e, e);
        }
    }

    public static void main(String args[])
    {
        // --------
        // Pre Boot
        // --------

        if ((args == null) || (args.length < 1))
        {
            printUsage();
            return;
        }

        BootOptions options = new BootOptions();

        // First argument MUST be starting class:
        String startclass = args[0];
        String newargs[] = new String[args.length - 1];

        // when run with -Ddebug=<whatever>, show debug:
        debug = (System.getProperty("debug") != null);

        if (!debug)
            debug = (System.getProperty("DEBUG") != null);

        for (int i = 0; i < newargs.length; i++)
        {
            String arg = args[i + 1];

            if (arg.compareTo("-debug") == 0)
                debug = true;

            // pass argument to launch class
            newargs[i] = args[i + 1];
        }

        // ----
        // BOOT
        // ----

        Bootstrapper boot = new Bootstrapper(options);

        try
        {
            boot.launch(startclass, newargs);
        }
        catch (Exception e)
        {
            errorPrintf("Failed to launch:'%s'\nException=%s\n", startclass, e);
            e.printStackTrace();
        }
    }

    // ========================================================================
    // Logging/Messages/Etc/
    // =========================================================================

    public static void printUsage()
    {
        System.err.println("Usage: java [JVM options] -jar bootstrapper.jar [bootstrapper options] <main class> [application options]");
    }

    public static void errorPrintf(String format, Object... args)
    {
        System.err.printf(format, args);
    }

    public static void debugPrintf(String format, Object... args)
    {
        if (debug == true)
            System.err.printf("Bootstrapper:DEBUG:" + format, args);
    }

    public static void messagePrintln(String msg)
    {
        System.out.println("Bootstrapper:" + msg);
    }

}
