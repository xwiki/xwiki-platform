package org.xwiki.extension.repository.internal;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

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
import org.xwiki.extension.Extension;
import org.xwiki.extension.ExtensionDependency;
import org.xwiki.extension.ExtensionId;
import org.xwiki.extension.LocalExtension;

public class DefaultLocalExtensionSerializer
{
    private DefaultLocalExtensionRepository repository;

    private DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();

    public DefaultLocalExtensionSerializer(DefaultLocalExtensionRepository repository)
    {
        this.repository = repository;
    }

    public DefaultLocalExtension loadDescriptor(InputStream descriptor) throws ParserConfigurationException, SAXException,
        IOException
    {
        DocumentBuilder documentBuilder = this.documentBuilderFactory.newDocumentBuilder();
        Document document = documentBuilder.parse(descriptor);

        Element extensionElement = document.getDocumentElement();

        // Mandatory fields

        Node idNode = extensionElement.getElementsByTagName("id").item(0);
        Node versionNode = extensionElement.getElementsByTagName("version").item(0);
        Node typeNode = extensionElement.getElementsByTagName("type").item(0);

        DefaultLocalExtension localExtension =
            new DefaultLocalExtension(repository,
                new ExtensionId(idNode.getTextContent(), versionNode.getTextContent()), typeNode.getTextContent());


        // Optional fields

        Node dependencyNode = getNode(extensionElement, "dependency");
        if (dependencyNode != null) {
            localExtension.setDependency(Boolean.valueOf(dependencyNode.getTextContent()));
        }
        Node enabledNode = getNode(extensionElement, "installed");
        if (enabledNode != null) {
            localExtension.setInstalled(Boolean.valueOf(enabledNode.getTextContent()));
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
            NodeList dependenciesNodeList = dependenciesNodes.item(0).getChildNodes();
            for (int i = 0; i < dependenciesNodeList.getLength(); ++i) {
                Node dependency = dependenciesNodeList.item(i);

                if (dependency.getNodeName().equals("dependency")) {
                    Node dependencyIdNode = getNode(dependency, "id");
                    Node dependencyVersionNode = getNode(dependency, "version");

                    localExtension.addDependency(new LocalExtensionDependency(dependencyIdNode.getTextContent(),
                        dependencyVersionNode.getTextContent()));
                }
            }
        }

        // Namespaces
        NodeList namespacesNodes = extensionElement.getElementsByTagName("namespaces");
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

        addElement(document, extensionElement, "id", extension.getId().getId());
        addElement(document, extensionElement, "version", extension.getId().getVersion());
        addElement(document, extensionElement, "type", extension.getType());

        addElement(document, extensionElement, "dependency", String.valueOf(extension.isDependency()));
        addElement(document, extensionElement, "installed", String.valueOf(extension.isInstalled()));
        addElement(document, extensionElement, "description", extension.getDescription());
        addElement(document, extensionElement, "author", extension.getAuthor());
        addElement(document, extensionElement, "website", extension.getWebSite());

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
            Element dependenciesElement = document.createElement("dependencies");
            parentElement.appendChild(dependenciesElement);

            for (ExtensionDependency dependency : extension.getDependencies()) {
                Element dependencyElement = document.createElement("dependency");
                dependenciesElement.appendChild(dependencyElement);

                addElement(document, dependencyElement, "id", dependency.getId());
                addElement(document, dependencyElement, "version", dependency.getVersion());
            }
        }
    }

    private void addNamespaces(Document document, Element parentElement, LocalExtension extension)
    {
        if (extension.getNamespaces() != null) {
            Element wikisElement = document.createElement("namespaces");
            parentElement.appendChild(wikisElement);

            for (String namespace : extension.getNamespaces()) {
                addElement(document, wikisElement, "namespace", namespace);
            }
        }
    }
}
