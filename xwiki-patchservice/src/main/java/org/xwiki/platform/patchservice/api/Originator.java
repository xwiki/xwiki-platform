package org.xwiki.platform.patchservice.api;

/**
 * An <tt>Originator</tt> identifies the origin of a patch: the place where the changes occured,
 * and the user who made those changes.
 * 
 * @see RWOriginator
 * @version $Id$
 * @since XWikiPlatform 1.3
 */
public interface Originator extends XmlSerializable
{
    /**
     * Gets the local name of the user who caused this patch.
     * 
     * @return The wiki name of the original author.
     */
    String getAuthor();

    /**
     * Gets the name of the virtual wiki where the patch was created.
     * 
     * @return The original name of the wiki.
     */
    String getWikiId();

    /**
     * Gets an identifier of the host where the patch was created.
     * 
     * @return The ID (e.g. IP address) of the original host.
     */
    String getHostId();
}
