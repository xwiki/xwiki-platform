package org.xwiki.extension.jar.internal.handler;

import java.net.URI;

import org.xwiki.classloader.URIClassLoader;

public class ExtensionURLClassLoader extends URIClassLoader
{
    private String namespace;

    public ExtensionURLClassLoader(URI[] uris, ClassLoader parent, String wiki)
    {
        super(uris, parent);
        
        this.namespace = wiki;
    }

    public String getWiki()
    {
        return namespace;
    }
}
