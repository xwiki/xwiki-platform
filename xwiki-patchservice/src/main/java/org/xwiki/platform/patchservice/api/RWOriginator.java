package org.xwiki.platform.patchservice.api;

public interface RWOriginator extends Originator
{
    void setAuthor(String author);

    void setHostId(String hostId);

    void setWikiId(String  wikiId);
}
