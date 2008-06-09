package org.xwiki.platform.patchservice.api;

import org.w3c.dom.Element;

import com.xpn.xwiki.XWikiException;

/**
 * A helper class for managing implementations for different {@link Operation} implementations.
 * 
 * @version $Id$
 * @since XWikiPlatform 1.3
 */
public interface OperationFactory
{
    /**
     * Retrieve a new {@link RWOperation} object implementing a specific operation type.
     * 
     * @param type The operation type, should be one of the constants defined in {@link Operation}.
     * @return An object implementing that operation type.
     * @throws XWikiException If no class is registered for that operation type, or if the
     *             registered class cannot be instantiated.
     */
    RWOperation newOperation(String type) throws XWikiException;

    /**
     * Load an Operation object from an XML export.
     * 
     * @param e The XML DOM Element containing an exported {@link Operation}.
     * @return An Operation object.
     * @throws XWikiException If the XML does not contain a valid object, or the object cannot be
     *             properly created.
     */
    Operation loadOperation(Element e) throws XWikiException;
}
