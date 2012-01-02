/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.xwiki.extension.test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Properties;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.io.IOUtils;
import org.reflections.Reflections;
import org.reflections.scanners.ResourcesScanner;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;
import org.reflections.util.FilterBuilder;
import org.reflections.vfs.Vfs;

import com.google.common.base.Predicates;

/**
 * Generate package based on information found in <code>packagefile.properties</code> files from the resources.
 * 
 * @version $Id$
 */
public class ExtensionPackager
{
    public static final String PACKAGEFILE_PACKAGE = "packagefile";

    public static final String PACKAGEFILE_DESCRIPTOR = "packagefile.properties";

    private File workingDirectory;

    private File defaultDirectory;

    public ExtensionPackager(File workingDirectory, File defaultDirectory)
    {
        this.workingDirectory = workingDirectory;
        this.defaultDirectory = defaultDirectory;
    }

    public void generateExtensions() throws IOException
    {
        Set<URL> urls = ClasspathHelper.forPackage(PACKAGEFILE_PACKAGE);

        if (!urls.isEmpty()) {
            Reflections reflections =
                new Reflections(new ConfigurationBuilder().setScanners(new ResourcesScanner()).setUrls(urls)
                    .filterInputsBy(new FilterBuilder.Include(FilterBuilder.prefix(PACKAGEFILE_PACKAGE))));

            Set<String> descriptors = reflections.getResources(Predicates.equalTo(PACKAGEFILE_DESCRIPTOR));

            for (String descriptor : descriptors) {
                String classPackageFolder =
                    descriptor.substring(0, descriptor.length() - PACKAGEFILE_DESCRIPTOR.length());
                generateExtension(classPackageFolder, getClass().getClassLoader().getResource(descriptor));
            }
        }
    }

    public void generateExtension(String classPackageFolder, URL descriptorUrl) throws IOException
    {
        String descriptorUrlStr = descriptorUrl.toString();
        String descriptorFolderURL =
            descriptorUrlStr.substring(0, descriptorUrlStr.length() - PACKAGEFILE_DESCRIPTOR.length());

        InputStream descriptorStream = descriptorUrl.openStream();

        Properties descriptorProperties = new Properties();
        descriptorProperties.load(descriptorStream);

        String type = descriptorProperties.getProperty("type");
        if (type == null) {
            type = "zip";
        }
        String id = descriptorProperties.getProperty("id");
        if (id == null) {
            id = descriptorFolderURL.substring(0, descriptorFolderURL.length() - 1);
            id = id.substring(id.lastIndexOf('/') + 1);
        }
        String version = descriptorProperties.getProperty("version");
        if (version == null) {
            version = "1.0";
        }
        File packageFile;
        String directory = descriptorProperties.getProperty("directory");
        String fileName = descriptorProperties.getProperty("fileName");
        if (directory == null) {
            if (fileName == null) {
                packageFile =
                    new File(this.defaultDirectory, URLEncoder.encode(id + '-' + version + '.' + type, "UTF-8"));
            } else {
                packageFile = new File(this.defaultDirectory, fileName);
            }
        } else {
            if (fileName == null) {
                fileName = URLEncoder.encode(id + '-' + version + '.' + type, "UTF-8");
            }

            packageFile = new File(this.workingDirectory, directory);
            packageFile = new File(packageFile, fileName);
        }

        // generate

        // Make sure the folder exists
        packageFile.getParentFile().mkdirs();

        FileOutputStream fos = new FileOutputStream(packageFile);
        try {
            ZipOutputStream zos;
            if (type.equals("jar")) {
                Manifest manifest = new Manifest();
                manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
                zos = new JarOutputStream(fos, manifest);
            } else {
                zos = new ZipOutputStream(fos);
            }

            try {
                for (Vfs.File resourceFile : Vfs.fromURL(new URL(descriptorFolderURL)).getFiles()) {
                    if (!resourceFile.getRelativePath().equals(PACKAGEFILE_DESCRIPTOR)) {
                        addZipEntry(classPackageFolder, resourceFile, zos, type);
                    }
                }
            } finally {
                zos.close();
            }
        } finally {
            fos.close();
        }
    }

    private void addZipEntry(String classPackageFolder, Vfs.File resourceFile, ZipOutputStream zos, String type)
        throws IOException
    {
        String zipPath;
        if (type.equals("jar") && resourceFile.getName().endsWith(".class")) {
            zipPath = classPackageFolder + resourceFile.getRelativePath();
        } else {
            zipPath = resourceFile.getRelativePath();
        }

        ZipEntry entry = new ZipEntry(zipPath);
        zos.putNextEntry(entry);

        InputStream resourceStream = resourceFile.openInputStream();
        try {
            IOUtils.copy(resourceStream, zos);
        } finally {
            resourceStream.close();
        }

        zos.closeEntry();
    }
}
