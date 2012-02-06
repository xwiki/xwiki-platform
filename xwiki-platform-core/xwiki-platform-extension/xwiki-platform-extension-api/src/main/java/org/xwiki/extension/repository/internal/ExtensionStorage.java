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
package org.xwiki.extension.repository.internal;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.extension.ExtensionId;
import org.xwiki.extension.InvalidExtensionException;

/**
 * Manipulate the extension filesystem repository storage.
 * 
 * @version $Id$
 */
public class ExtensionStorage
{
    /**
     * Logging tool.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(ExtensionStorage.class);

    /**
     * The extension of the descriptor files.
     */
    private static final String DESCRIPTOR_EXT = "xed";

    /**
     * The extension of the descriptor files prefixed with dot.
     */
    private static final String DESCRIPTOR_SUFFIX = "." + DESCRIPTOR_EXT;

    /**
     * The repository.
     */
    private DefaultLocalExtensionRepository repository;

    /**
     * Used to read/write in the repository storage itself.
     */
    private ExtensionSerializer extensionSerializer;

    /**
     * @see #getRootFolder()
     */
    private File rootFolder;

    /**
     * Used to lookup the extension serializer.
     */
    private ComponentManager componentManager;

    /**
     * @param repository the repository
     * @param rootFolder the repository folder
     * @param componentManager used to lookup needed components
     * @throws ComponentLookupException can't find ExtensionSerializer
     */
    public ExtensionStorage(DefaultLocalExtensionRepository repository, File rootFolder,
        ComponentManager componentManager) throws ComponentLookupException
    {
        this.repository = repository;
        this.rootFolder = rootFolder;
        this.componentManager = componentManager;

        this.extensionSerializer = this.componentManager.lookup(ExtensionSerializer.class);
    }

    /**
     * @return the repository folder
     */
    public File getRootFolder()
    {
        return this.rootFolder;
    }

    /**
     * Load extension from repository storage.
     */
    protected void loadExtensions()
    {
        // Load local extension from repository

        if (this.rootFolder.exists()) {
            FilenameFilter descriptorFilter = new FilenameFilter()
            {
                @Override
                public boolean accept(File dir, String name)
                {
                    return name.endsWith(DESCRIPTOR_SUFFIX);
                }
            };

            for (File child : this.rootFolder.listFiles(descriptorFilter)) {
                if (!child.isDirectory()) {
                    try {
                        DefaultLocalExtension localExtension = loadDescriptor(child);

                        repository.addLocalExtension(localExtension);
                    } catch (Exception e) {
                        LOGGER.warn("Failed to load extension from file [" + child + "] in local repository", e);
                    }
                }
            }
        } else {
            this.rootFolder.mkdirs();
        }
    }

    /**
     * Local extension descriptor from a file.
     * 
     * @param descriptor the descriptor file
     * @return the extension descriptor
     * @throws InvalidExtensionException error when trying to load extension descriptor
     */
    private DefaultLocalExtension loadDescriptor(File descriptor) throws InvalidExtensionException
    {
        FileInputStream fis;
        try {
            fis = new FileInputStream(descriptor);
        } catch (FileNotFoundException e) {
            throw new InvalidExtensionException("Failed to open descriptor for reading", e);
        }

        try {
            DefaultLocalExtension localExtension = this.extensionSerializer.loadDescriptor(this.repository, fis);

            localExtension.setDescriptorFile(descriptor);
            localExtension.setFile(getFile(descriptor, DESCRIPTOR_EXT, localExtension.getType()));

            if (!localExtension.getFile().getFile().exists()) {
                throw new InvalidExtensionException("Failed to load local extension [" + descriptor + "]: ["
                    + localExtension.getFile() + "] file does not exists");
            }

            return localExtension;
        } finally {
            try {
                fis.close();
            } catch (IOException e) {
                LOGGER.error("Failed to close stream for file [" + descriptor + "]", e);
            }
        }
    }

    /***
     * Update the extension descriptor in the filesystem repository.
     * 
     * @param extension the local extension descriptor to save
     * @throws ParserConfigurationException error when trying to save the descriptor
     * @throws TransformerException error when trying to save the descriptor
     * @throws IOException error when trying to save the descriptor
     */
    protected void saveDescriptor(DefaultLocalExtension extension) throws ParserConfigurationException,
        TransformerException, IOException
    {
        File file = extension.getDescriptorFile();

        if (file == null) {
            file = getNewDescriptorFile(extension.getId());
            extension.setDescriptorFile(file);
        }

        FileOutputStream fos = new FileOutputStream(file);

        try {
            this.extensionSerializer.saveDescriptor(extension, fos);
        } finally {
            fos.close();
        }
    }

    /**
     * @param id the extension identifier
     * @param type the extension type
     * @return the file containing the extension
     */
    protected File getNewExtensionFile(ExtensionId id, String type)
    {
        return new File(getRootFolder(), getFilePath(id, type));
    }

    /**
     * @param id the extension identifier
     * @return the file containing the extension descriptor
     */
    private File getNewDescriptorFile(ExtensionId id)
    {
        return new File(getRootFolder(), getFilePath(id, DESCRIPTOR_EXT));
    }

    /**
     * @param baseFile the extension file
     * @param baseType the type of the extension
     * @param type the type of the file to get
     * @return the extension descriptor file
     */
    private File getFile(File baseFile, String baseType, String type)
    {
        String baseName = getBaseName(baseFile.getName(), baseType);

        return new File(baseFile.getParent(), baseName + '.' + encode(type));
    }

    /**
     * @param fileName the name of the file of the provided type
     * @param type the type of the file
     * @return the base name which is the name without the typed extension
     */
    private String getBaseName(String fileName, String type)
    {
        return fileName.substring(0, fileName.length() - encode(type).length() - 1);
    }

    /**
     * @param name the file or directory name to encode
     * @return the encoding name
     */
    private String encode(String name)
    {
        String encoded;
        try {
            encoded = URLEncoder.encode(name, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            // Should never happen

            encoded = name;
        }

        return encoded;
    }

    /**
     * Get file path in the local extension repository.
     * 
     * @param id the extension id
     * @param fileExtension the file extension
     * @return the encoded file path
     */
    private String getFilePath(ExtensionId id, String fileExtension)
    {
        String encodedId = encode(id.getId());
        String encodedVersion = encode(id.getVersion().toString());
        String encodedType = encode(fileExtension);

        return encodedId + File.separator + encodedVersion + File.separator + encodedId + '-' + encodedVersion + '.'
            + encodedType;
    }

    /**
     * Remove extension from storage.
     * 
     * @param extension extension to remove
     * @throws IOException error when deleting the extension
     */
    protected void removeExtension(DefaultLocalExtension extension) throws IOException
    {
        File descriptorFile = extension.getDescriptorFile();

        if (descriptorFile == null) {
            throw new IOException("Exception does not exists");
        }

        descriptorFile.delete();

        DefaultLocalExtensionFile extensionFile = extension.getFile();

        extensionFile.getFile().delete();
    }
}
