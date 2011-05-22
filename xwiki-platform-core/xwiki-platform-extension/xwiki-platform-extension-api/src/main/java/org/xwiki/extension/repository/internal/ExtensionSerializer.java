package org.xwiki.extension.repository.internal;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;

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
import org.xwiki.extension.Extension;
import org.xwiki.extension.ExtensionDependency;
import org.xwiki.extension.ExtensionId;
import org.xwiki.extension.InvalidExtensionException;
import org.xwiki.extension.LocalExtension;

/**
 * Local repository storage serialization tool.
 * 
 * @version $Id$
 */
public class ExtensionSerializer
{
    private static final String ELEMENT_ID = "id";

    private static final String ELEMENT_VERSION = "version";

    private static final String ELEMENT_TYPE = "type";

    private static final String ELEMENT_DEPENDENCY = "dependency";

    private static final String ELEMENT_INSTALLED = "installed";

    private static final String ELEMENT_NAME = "name";

    private static final String ELEMENT_DESCRIPTION = "description";

    private static final String ELEMENT_WEBSITE = "website";

    private static final String ELEMENT_AUTHORS = "authors";

    private static final String ELEMENT_AAUTHOR = "author";

    private static final String ELEMENT_DEPENDENCIES = "dependencies";

    private static final String ELEMENT_DDEPENDENCY = "dependency";

    private static final String ELEMENT_NAMESPACES = "namespaces";

    private static final String ELEMENT_NNAMESPACE = "namespace";

    /**
     * Used to parse XML descriptor file.
     */
    private DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();

    /**
     * Load local extension descriptor.
     * 
     * @param repository the repository
     * @param descriptor the descriptor content
     * @return the parsed local extension descriptor
     * @throws InvalidExtensionException error when trying to parse extension descriptor
     */
    public DefaultLocalExtension loadDescriptor(DefaultLocalExtensionRepository repository, InputStream descriptor)
        throws InvalidExtensionException
    {
        DocumentBuilder documentBuilder;
        try {
            documentBuilder = this.documentBuilderFactory.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            throw new InvalidExtensionException("Failed to create new DocumentBuilder", e);
        }

        Document document;
        try {
            document = documentBuilder.parse(descriptor);
        } catch (Exception e) {
            throw new InvalidExtensionException("Failed to parse descriptor", e);
        }

        Element extensionElement = document.getDocumentElement();

        // Mandatory fields

        Node idNode = extensionElement.getElementsByTagName(ELEMENT_ID).item(0);
        Node versionNode = extensionElement.getElementsByTagName(ELEMENT_VERSION).item(0);
        Node typeNode = extensionElement.getElementsByTagName(ELEMENT_TYPE).item(0);

        DefaultLocalExtension localExtension =
            new DefaultLocalExtension(repository,
                new ExtensionId(idNode.getTextContent(), versionNode.getTextContent()), typeNode.getTextContent());

        // Optional fields

        Node dependencyNode = getNode(extensionElement, ELEMENT_DEPENDENCY);
        if (dependencyNode != null) {
            localExtension.setDependency(Boolean.valueOf(dependencyNode.getTextContent()));
        }
        Node enabledNode = getNode(extensionElement, ELEMENT_INSTALLED);
        if (enabledNode != null) {
            localExtension.setInstalled(Boolean.valueOf(enabledNode.getTextContent()));
        }
        Node nameNode = getNode(extensionElement, ELEMENT_NAME);
        if (nameNode != null) {
            localExtension.setName(nameNode.getTextContent());
        }
        Node descriptionNode = getNode(extensionElement, ELEMENT_DESCRIPTION);
        if (descriptionNode != null) {
            localExtension.setDescription(descriptionNode.getTextContent());
        }
        Node websiteNode = getNode(extensionElement, ELEMENT_WEBSITE);
        if (websiteNode != null) {
            localExtension.setWebsite(websiteNode.getTextContent());
        }

        // Authors
        NodeList authorsNodes = extensionElement.getElementsByTagName(ELEMENT_AUTHORS);
        if (authorsNodes.getLength() > 0) {
            NodeList authors = authorsNodes.item(0).getChildNodes();
            for (int i = 0; i < authors.getLength(); ++i) {
                Node authorsNode = authors.item(i);

                localExtension.addAuthor(authorsNode.getTextContent());
            }
        }

        // Dependencies
        NodeList dependenciesNodes = extensionElement.getElementsByTagName(ELEMENT_DEPENDENCIES);
        if (dependenciesNodes.getLength() > 0) {
            NodeList dependenciesNodeList = dependenciesNodes.item(0).getChildNodes();
            for (int i = 0; i < dependenciesNodeList.getLength(); ++i) {
                Node dependency = dependenciesNodeList.item(i);

                if (dependency.getNodeName().equals(ELEMENT_DDEPENDENCY)) {
                    Node dependencyIdNode = getNode(dependency, ELEMENT_ID);
                    Node dependencyVersionNode = getNode(dependency, ELEMENT_VERSION);

                    localExtension.addDependency(new LocalExtensionDependency(dependencyIdNode.getTextContent(),
                        dependencyVersionNode.getTextContent()));
                }
            }
        }

        // Namespaces
        NodeList namespacesNodes = extensionElement.getElementsByTagName(ELEMENT_NAMESPACES);
        if (namespacesNodes.getLength() > 0) {
            NodeList namespaces = namespacesNodes.item(0).getChildNodes();
            for (int i = 0; i < namespaces.getLength(); ++i) {
                Node namespaceNode = namespaces.item(i);

                localExtension.addNamespace(namespaceNode.getTextContent());
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

    public void saveDescriptor(LocalExtension extension, FileOutputStream fos) throws ParserConfigurationException,
        TransformerException, IOException
    {
        DocumentBuilder documentBuilder = this.documentBuilderFactory.newDocumentBuilder();
        Document document = documentBuilder.newDocument();

        Element extensionElement = document.createElement("extension");
        document.appendChild(extensionElement);

        addElement(document, extensionElement, ELEMENT_ID, extension.getId().getId());
        addElement(document, extensionElement, ELEMENT_VERSION, extension.getId().getVersion());
        addElement(document, extensionElement, ELEMENT_TYPE, extension.getType());

        addElement(document, extensionElement, ELEMENT_DEPENDENCY, String.valueOf(extension.isDependency()));
        addElement(document, extensionElement, ELEMENT_INSTALLED, String.valueOf(extension.isInstalled()));
        addElement(document, extensionElement, ELEMENT_NAME, extension.getName());
        addElement(document, extensionElement, ELEMENT_DESCRIPTION, extension.getDescription());
        addElement(document, extensionElement, ELEMENT_WEBSITE, extension.getWebSite());

        addAuthors(document, extensionElement, extension);
        addDependencies(document, extensionElement, extension);
        addNamespaces(document, extensionElement, extension);

        TransformerFactory transfac = TransformerFactory.newInstance();
        Transformer trans = transfac.newTransformer();
        trans.setOutputProperty(OutputKeys.INDENT, "yes");

        DOMSource source = new DOMSource(document);
        Result result = new StreamResult(fos);
        trans.transform(source, result);
    }

    private void addElement(Document document, Element parentElement, String elementName, String elementValue)
    {
        Element element = document.createElement(elementName);
        element.setTextContent(elementValue);

        parentElement.appendChild(element);
    }

    private void addDependencies(Document document, Element parentElement, Extension extension)
    {
        if (extension.getDependencies() != null && !extension.getDependencies().isEmpty()) {
            Element dependenciesElement = document.createElement(ELEMENT_DEPENDENCIES);
            parentElement.appendChild(dependenciesElement);

            for (ExtensionDependency dependency : extension.getDependencies()) {
                Element dependencyElement = document.createElement(ELEMENT_DDEPENDENCY);
                dependenciesElement.appendChild(dependencyElement);

                addElement(document, dependencyElement, ELEMENT_ID, dependency.getId());
                addElement(document, dependencyElement, ELEMENT_VERSION, dependency.getVersion());
            }
        }
    }

    private void addNamespaces(Document document, Element parentElement, LocalExtension extension)
    {
        addCollection(document, parentElement, extension.getNamespaces(), ELEMENT_NNAMESPACE, ELEMENT_NAMESPACES);
    }

    private void addAuthors(Document document, Element parentElement, LocalExtension extension)
    {
        addCollection(document, parentElement, extension.getAuthors(), ELEMENT_AAUTHOR, ELEMENT_AUTHORS);
    }

    private void addCollection(Document document, Element parentElement, Collection<String> elements,
        String elementName, String elementRoot)
    {
        if (elements != null && !elements.isEmpty()) {
            Element wikisElement = document.createElement(elementRoot);
            parentElement.appendChild(wikisElement);

            for (String element : elements) {
                addElement(document, wikisElement, elementName, element);
            }
        }
    }
}
