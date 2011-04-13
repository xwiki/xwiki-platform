package org.xwiki.extension.repository;

import org.xwiki.extension.Extension;
import org.xwiki.extension.ExtensionCollectException;

public interface ExtensionCollector
{
    void addExtension(Extension extension) throws ExtensionCollectException;
}
