package org.xwiki.platform.patchservice.api;

public interface Originator extends XmlSerializable
{
    String getAuthor();

    String getHostId();

    String getWikiId();
}
