package org.xwiki.extension.test;

import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

import org.xwiki.extension.Extension;
import org.xwiki.extension.ExtensionCollectException;
import org.xwiki.extension.ExtensionId;
import org.xwiki.extension.ResolveException;
import org.xwiki.extension.repository.ExtensionCollector;
import org.xwiki.extension.repository.ExtensionRepository;
import org.xwiki.extension.repository.ExtensionRepositoryId;
import org.xwiki.extension.repository.internal.DefaultLocalExtension;
import org.xwiki.extension.repository.internal.DefaultLocalExtensionSerializer;

public class ResourceExtensionRepository implements ExtensionRepository
{
    private DefaultLocalExtensionSerializer extensionSerializer;

    private ExtensionRepositoryId repositoryId;

    private ClassLoader classLoader;

    private String baseResource;

    private Map<ExtensionId, ResourceExtension> extension = new HashMap<ExtensionId, ResourceExtension>();

    public ResourceExtensionRepository(ClassLoader classLoader, String baseResource)
    {
        this.extensionSerializer = new DefaultLocalExtensionSerializer(null);

        this.repositoryId = new ExtensionRepositoryId("resources", "resources", null);

        this.classLoader = classLoader;
        this.baseResource = baseResource;
    }

    InputStream getResourceAsStream(String name) throws UnsupportedEncodingException
    {
        return this.classLoader.getResourceAsStream(this.baseResource + URLEncoder.encode(name, "UTF-8"));
    }

    InputStream getResourceAsStream(ExtensionId extensionId, String type) throws UnsupportedEncodingException
    {
        return getResourceAsStream(extensionId.getId() + '-' + extensionId.getVersion() + '.' + type);
    }

    public ExtensionRepositoryId getId()
    {
        return this.repositoryId;
    }

    public Extension resolve(ExtensionId extensionId) throws ResolveException
    {
        InputStream descriptor;
        try {
            descriptor = getResourceAsStream(extensionId, "xed");
        } catch (UnsupportedEncodingException e) {
            throw new ResolveException("Invalid extension id [" + extensionId + "]", e);
        }

        if (descriptor == null) {
            throw new ResolveException("Extension [" + extensionId + "] not found");
        }

        try {
            DefaultLocalExtension localExtension = this.extensionSerializer.loadDescriptor(descriptor);

            ResourceExtension resourceExtension = new ResourceExtension(this, localExtension);

            this.extension.put(resourceExtension.getId(), resourceExtension);

            return resourceExtension;
        } catch (Exception e) {
            throw new ResolveException("Failed to parse descriptor for extension [" + extensionId + "]", e);
        }
    }

    public boolean exists(ExtensionId extensionId)
    {
        // TODO
        return false;
    }

    public int countExtensions()
    {
        // TODO
        return 0;
    }

    public void collectExtensions(ExtensionCollector collector) throws ExtensionCollectException
    {
        // TODO        
    }
}
