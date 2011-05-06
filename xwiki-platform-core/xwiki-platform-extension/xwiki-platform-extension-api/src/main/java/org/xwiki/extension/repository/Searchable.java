package org.xwiki.extension.repository;

import java.util.List;

import org.xwiki.extension.Extension;

public interface Searchable
{
    List<Extension> search(String str);
}
