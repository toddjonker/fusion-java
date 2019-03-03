// Copyright (c) 2012-2014 Amazon.com, Inc.  All rights reserved.

package com.amazon.fusion;

import com.amazon.ion.*;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static com.amazon.fusion.GlobalState.FUSION_SOURCE_EXTENSION;

/**
 * A {@link ModuleRepository} that loads its modules through {@link ClassLoader} resources.
 * A particular Ion manifest resource is used as the <i>root</i> of the repository, this
 * resource can be thought of as similar to the repository directory root in a
 * {@link FileSystemModuleRepository}.
 *
 * <br/><br/>
 * The manifest format is a single Ion <tt>struct</tt>
 * in the following form:
 * <pre>
 *  {
 *      version: 1,
 *      root: "/some/root/path",
 *      modules: [
 *          "/a/good/module1",
 *          "/a/fun/module2"
 *      ]
 *  }
 * </pre>
 *
 * <br/><br/>
 * The <i>module</i> names <b>must</b> be absolute.
 *
 * <br/><br/>
 * The <i>root</i> prefix is joined with <tt>/src</tt> which is then joined with
 * the <i>module</i> name with the <tt>.fusion</tt> suffix to generate a module's
 * {@link ClassLoader} resource path.  So in the above example the following resource paths
 * map to the modules defined:
 *
 * <ul>
 *     <li><tt>/some/root/path/src/a/good/module1.fusion</tt></li>
 *     <li><tt>/some/root/path/src/a/fun/module2.fusion</tt></li>
 * </ul>
 */
final class ClassloaderResourceRepository
    extends ModuleRepository
{
    private static final String VERSION_FIELD = "version";
    private static final String ROOT_FIELD = "root";
    private static final String MODULES_FIELD = "modules";


    private final String myIdentity;
    private final Class<?> myResourceClass;
    private final Map<String, String> myModulePathToResourcePaths;

    /**
     * @param ion The {@link IonSystem} to use for reading the manifest.
     * @param resourceClass The {@link Class} to use to find resources.
     * @param manifestPath The resource path to the Ion based manifest that describes the
     *                     locations of the Fusion module sources.
     *
     * @see Class#getResource(String)
     */
    ClassloaderResourceRepository(IonSystem ion, Class<?> resourceClass, String manifestPath)
        throws FusionException
    {
        myIdentity = "classloader::[" + resourceClass.getName() + "," + manifestPath + "]";
        myResourceClass = resourceClass;

        try(InputStream manifestIn = resourceClass.getResourceAsStream(manifestPath))
        {
            if (manifestIn == null)
            {
                throw new FusionException("No classloader manifest found: " + manifestPath);
            }
            IonLoader loader = ion.getLoader();
            IonDatagram manifestDatagram = loader.load(manifestIn);
            if (manifestDatagram.size() != 1)
            {
                throw new FusionException(
                    "Expected a single Ion value for repository manifest: " + manifestPath);
            }

            IonStruct manifest = check(IonStruct.class, manifestDatagram.get(0));
            IonInt version = check(IonInt.class, manifest.get(VERSION_FIELD));
            if (version.isNullValue() || version.intValue() != 1)
            {
                throw new FusionException("Expected version 1 for repository manifest: " + version);
            }

            String root = checkStr(manifest.get(ROOT_FIELD));

            IonList modules = check(IonList.class, manifest.get(MODULES_FIELD));
            Map<String, String> modulePathToResourcePaths = new HashMap<>();
            for (IonValue moduleValue : modules)
            {
                String modulePath = checkStr(moduleValue);
                if (!ModuleIdentity.isValidAbsoluteModulePath(modulePath))
                {
                    throw new FusionException(
                        "Repository manifest module path is not absolute: " + modulePath);
                }
                String resourcePath = root + "/src" + modulePath + FUSION_SOURCE_EXTENSION;
                modulePathToResourcePaths.put(modulePath, resourcePath);
            }
            myModulePathToResourcePaths = Collections.unmodifiableMap(modulePathToResourcePaths);
        }
        catch (IOException e)
        {
            throw new FusionException("I/O Error", e);
        }
    }

    private static <T extends IonValue> T check(Class<T> clazz, IonValue value)
        throws FusionException
    {   if (!clazz.isInstance(value) || value.isNullValue())
        {
            throw new FusionException(
                "Repository manifest expected " + clazz.getName() + ", got " + value);
        }
        return clazz.cast(value);
    }

    private static String checkStr(IonValue value)
        throws FusionException
    {
        IonText text = check(IonText.class, value);
        return text.stringValue();
    }

    @Override
    String identify()
    {
        return myIdentity;
    }

    @Override
    public String toString()
    {
        return "[ClassloaderResourceRepository " + identify() + "]";
    }


    @Override
    ModuleLocation locateModule(Evaluator eval, ModuleIdentity id)
        throws FusionException
    {
        String path = id.absolutePath();
        String resourcePath = myModulePathToResourcePaths.get(path);
        if (resourcePath == null)
        {
            // no resource in manifest
            return null;
        }

        final URL url = myResourceClass.getResource(resourcePath);
        if (url == null)
        {
            // cannot find the resource
            return null;
        }

        ModuleLocation loc = new InputStreamModuleLocation()
        {
            // TODO replace this with a proper source name
            private final SourceName myName = SourceName.forDisplay(url.toString());

            @Override
            SourceName sourceName()
            {
                return myName;
            }

            @Override
            InputStream open()
                throws IOException
            {
                return url.openStream();
            }
        };
        return loc;
    }


    @Override
    void collectModules(Predicate<ModuleIdentity> selector,
                        Consumer<ModuleIdentity>  results)
        throws FusionException
    {
        for (Map.Entry<String, String> entry : myModulePathToResourcePaths.entrySet())
        {
            String modulePath = entry.getKey();
            String resourcePath = entry.getValue();

            ModuleIdentity id = ModuleIdentity.forAbsolutePath(modulePath);
            if (selector.test(id))
            {
                results.accept(id);
            }
        }
    }
}
