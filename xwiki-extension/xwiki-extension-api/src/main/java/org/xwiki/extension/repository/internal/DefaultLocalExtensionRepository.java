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
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.component.logging.AbstractLogEnabled;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.extension.Extension;
import org.xwiki.extension.ExtensionDependency;
import org.xwiki.extension.ExtensionId;
import org.xwiki.extension.ExtensionManagerConfiguration;
import org.xwiki.extension.InstallException;
import org.xwiki.extension.LocalExtension;
import org.xwiki.extension.ResolveException;
import org.xwiki.extension.UninstallException;
import org.xwiki.extension.repository.ExtensionRepositoryId;
import org.xwiki.extension.repository.LocalExtensionRepository;

//TODO: make it threadsafe bulletproof
@Component
public class DefaultLocalExtensionRepository extends AbstractLogEnabled implements LocalExtensionRepository,
    Initializable
{
    @Requirement
    private ExtensionManagerConfiguration configuration;

    private ExtensionRepositoryId repositoryId;

    private File rootFolder;

    private Map<String, LocalExtension> extensions = new ConcurrentHashMap<String, LocalExtension>();

    private Map<String, Set<String>> backwardDependenciesMap = new ConcurrentHashMap<String, Set<String>>();

    public void initialize() throws InitializationException
    {
        this.rootFolder = this.configuration.getLocalRepository();

        this.repositoryId = new ExtensionRepositoryId("local", "xwiki", this.rootFolder.toURI());

        loadExtensions();
    }

    private void loadExtensions()
    {
        File rootFolder = getRootFolder();

        if (rootFolder.exists()) {
            FilenameFilter descriptorFilter = new FilenameFilter()
            {
                public boolean accept(File dir, String name)
                {
                    return name.endsWith(".xed");
                }
            };

            for (File child : rootFolder.listFiles(descriptorFilter)) {
                if (!child.isDirectory()) {
                    try {
                        LocalExtension localExtension = loadDescriptor(child);

                        addLocalExtension(localExtension);
                    } catch (Exception e) {
                        getLogger().warn("Failed to load extensoin from file [" + child + "] in local repository", e);
                    }
                }
            }
        } else {
            rootFolder.mkdirs();
        }
    }

    private void removeLocalExtension(LocalExtension localExtension)
    {
        this.backwardDependenciesMap.remove(localExtension.getId());
        this.extensions.remove(localExtension.getId());
        for (ExtensionDependency dependency : localExtension.getDependencies()) {
            Set<String> backwardDependencies = this.backwardDependenciesMap.get(dependency.getId());

            if (backwardDependencies != null) {
                backwardDependencies.remove(localExtension.getId());
            }
        }
    }

    private void addLocalExtension(LocalExtension localExtension)
    {
        for (ExtensionDependency dependency : localExtension.getDependencies()) {
            Set<String> backwardDependencies = this.backwardDependenciesMap.get(dependency.getId());

            if (backwardDependencies == null) {
                backwardDependencies = new HashSet<String>();
                this.backwardDependenciesMap.put(dependency.getId(), backwardDependencies);
            }

            backwardDependencies.add(localExtension.getId());
        }

        this.extensions.put(localExtension.getId(), localExtension);
    }

    public File getRootFolder()
    {
        return this.rootFolder;
    }

    // Repository

    public Extension resolve(ExtensionId extensionId) throws ResolveException
    {
        LocalExtension localExtension = getLocalExtension(extensionId.getId());

        if (localExtension == null
            || (extensionId.getVersion() != null && !localExtension.getVersion().equals(extensionId.getVersion()))) {
            throw new ResolveException("Can't find extension [" + extensionId + "]");
        }

        return localExtension;
    }

    public boolean exists(ExtensionId extensionId)
    {
        LocalExtension localExtension = getLocalExtension(extensionId.getId());

        if (localExtension == null
            || (extensionId.getVersion() != null && !localExtension.getVersion().equals(extensionId.getVersion()))) {
            return false;
        }

        return true;
    }

    public ExtensionRepositoryId getId()
    {
        return this.repositoryId;
    }

    // LocalRepository

    public List<LocalExtension> getLocalExtensions()
    {
        return new ArrayList<LocalExtension>(this.extensions.values());
    }

    public LocalExtension getLocalExtension(String id)
    {
        LocalExtension extension = this.extensions.get(id);

        return extension != null ? extension : null;
    }

    private LocalExtension createExtension(Extension extension, boolean dependency)
    {
        DefaultLocalExtension localExtension = new DefaultLocalExtension(this, extension);

        localExtension.setDependency(dependency);

        localExtension.setFile(getFile(localExtension.getId(), localExtension.getVersion(), localExtension.getType()));

        return localExtension;
    }

    public int countExtensions()
    {
        return this.extensions.size();
    }

    public List< ? extends LocalExtension> getExtensions(int nb, int offset)
    {
        return getLocalExtensions().subList(offset, offset + nb);
    }

    public LocalExtension installExtension(Extension extension, boolean dependency) throws InstallException
    {
        LocalExtension localExtension = getLocalExtension(extension.getId());

        if (localExtension == null || !extension.getVersion().equals(localExtension.getVersion())) {
            localExtension = createExtension(extension, dependency);

            try {
                extension.download(localExtension.getFile());
                saveDescriptor(localExtension);
                addLocalExtension(localExtension);
            } catch (Exception e) {
                // TODO: clean

                throw new InstallException("Failed to download extension [" + extension + "]", e);
            }
        }

        return localExtension;
    }

    public void uninstallExtension(LocalExtension localExtension) throws UninstallException
    {
        localExtension.getFile().delete();
        getDescriptorFile(localExtension.getId(), localExtension.getVersion()).delete();

        LocalExtension existingExtension = getLocalExtension(localExtension.getId());

        if (existingExtension == localExtension) {
            removeLocalExtension(localExtension);
        }
    }

    private void saveDescriptor(LocalExtension extension) throws ParserConfigurationException, TransformerException,
        IOException
    {
        DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
        Document document = documentBuilder.newDocument();

        Element extensionElement = document.createElement("extension");
        document.appendChild(extensionElement);

        addElement(document, extensionElement, "id", extension.getId());
        addElement(document, extensionElement, "version", extension.getVersion());
        addElement(document, extensionElement, "type", extension.getType());

        addElement(document, extensionElement, "dependency", String.valueOf(extension.isDependency()));
        addElement(document, extensionElement, "enabled", String.valueOf(extension.isEnabled()));
        addElement(document, extensionElement, "description", extension.getDescription());
        addElement(document, extensionElement, "author", extension.getAuthor());
        addElement(document, extensionElement, "website", extension.getWebSite());

        addDependencies(document, extensionElement, extension);

        TransformerFactory transfac = TransformerFactory.newInstance();
        Transformer trans = transfac.newTransformer();
        trans.setOutputProperty(OutputKeys.INDENT, "yes");

        DOMSource source = new DOMSource(document);
        File file = getDescriptorFile(extension.getId(), extension.getVersion());
        FileOutputStream fos = new FileOutputStream(file);
        try {
            Result result = new StreamResult(fos);
            trans.transform(source, result);
        } finally {
            fos.close();
        }
    }

    private File getFile(String id, String version, String type)
    {
        return new File(getRootFolder(), getFileName(id, version, type));
    }

    private File getDescriptorFile(String id, String version)
    {
        return new File(getRootFolder(), getFileName(id, version, "xed"));
    }

    private String getFileName(String id, String version, String extension)
    {
    	String fileName = id + "-" + version + "." + extension;
    	try {
			return URLEncoder.encode(fileName, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			// Should never happen

			return fileName;
		}
    }

    private void addElement(Document document, Element parentElement, String elementName, String elementValue)
    {
        Element element = document.createElement(elementName);
        element.setTextContent(elementValue);

        parentElement.appendChild(element);
    }

    private void addDependencies(Document document, Element parentElement, LocalExtension extension)
    {
        Element dependenciesElement = document.createElement("dependencies");
        parentElement.appendChild(dependenciesElement);

        for (ExtensionDependency dependency : extension.getDependencies()) {
            Element dependencyElement = document.createElement("dependency");
            dependenciesElement.appendChild(dependencyElement);

            addElement(document, dependencyElement, "id", dependency.getId());
            addElement(document, dependencyElement, "version", dependency.getVersion());
        }
    }

    private LocalExtension loadDescriptor(File descriptor) throws ParserConfigurationException, SAXException,
        IOException
    {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();
        Document doc = db.parse(descriptor);

        Element extensionElement = doc.getDocumentElement();

        // Mandatory fields

        Node idNode = extensionElement.getElementsByTagName("id").item(0);
        Node versionNode = extensionElement.getElementsByTagName("version").item(0);
        Node typeNode = extensionElement.getElementsByTagName("type").item(0);

        DefaultLocalExtension localExtension =
            new DefaultLocalExtension(this, idNode.getTextContent(), versionNode.getTextContent(),
                typeNode.getTextContent());

        localExtension.setFile(getFile(localExtension.getId(), localExtension.getVersion(), localExtension.getType()));

        // Optional fields

        Node dependencyNode = getNode(extensionElement, "dependency");
        if (dependencyNode != null) {
            localExtension.setDependency(Boolean.valueOf(dependencyNode.getTextContent()));
        }
        Node enabledNode = getNode(extensionElement, "enabled");
        if (enabledNode != null) {
            localExtension.setEnabled(Boolean.valueOf(enabledNode.getTextContent()));
        }
        Node descriptionNode = getNode(extensionElement, "description");
        if (descriptionNode != null) {
            localExtension.setDescription(descriptionNode.getTextContent());
        }
        Node authorNode = getNode(extensionElement, "author");
        if (authorNode != null) {
            localExtension.setAuthor(authorNode.getTextContent());
        }
        Node websiteNode = getNode(extensionElement, "website");
        if (websiteNode != null) {
            localExtension.setWebsite(websiteNode.getTextContent());
        }

        // Dependencies

        NodeList dependenciesNodes = extensionElement.getElementsByTagName("dependencies");
        if (dependenciesNodes.getLength() > 0) {
            NodeList dependencies = dependenciesNodes.item(0).getChildNodes();
            for (int i = 0; i < dependencies.getLength(); ++i) {
                Node dependency = dependencies.item(i);

                if (dependency.getNodeName().equals("dependency")) {
                    Node dependencyIdNode = getNode(dependency, "id");
                    Node dependencyVersionNode = getNode(dependency, "version");

                    localExtension.addDependency(new LocalExtensionDependency(dependencyIdNode.getTextContent(),
                        dependencyVersionNode.getTextContent()));
                }
            }
        }

        return localExtension;
    }

    private Node getNode(Node parentNode, String elementName)
    {
        NodeList children = parentNode.getChildNodes();
        for (int i = 0; i < children.getLength(); ++i) {
            Node node = children.item(i);

            if (node.getNodeName().equals(elementName)) {
                return node;
            }
        }

        return null;
    }

    private Node getNode(Element parentElement, String elementName)
    {
        NodeList children = parentElement.getElementsByTagName(elementName);

        return children.getLength() > 0 ? children.item(0) : null;
    }

    public List<LocalExtension> getBackwardDependencies(String id) throws ResolveException
    {
        if (getLocalExtension(id) == null) {
            throw new ResolveException("Extension [" + id + "] does not exists");
        }

        Set<String> backwardDependencyNames = this.backwardDependenciesMap.get(id);

        List<LocalExtension> backwardDependencies;
        if (backwardDependencyNames != null) {
            backwardDependencies = new ArrayList<LocalExtension>(backwardDependencyNames.size());
            for (String extensionId : backwardDependencyNames) {
                backwardDependencies.add(getLocalExtension(extensionId));
            }
        } else {
            backwardDependencies = Collections.<LocalExtension> emptyList();
        }

        return backwardDependencies;
    }
}
