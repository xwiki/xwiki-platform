package org.xwiki.extension.repository;

import java.util.List;

import org.xwiki.component.annotation.ComponentRole;

@ComponentRole
public interface ExtensionRepositorySource
{
    List<ExtensionRepositoryId> getExtensionRepositories();
}
