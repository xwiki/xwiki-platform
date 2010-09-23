package org.xwiki.extension.repository.internal.aether.configuration;

import java.io.File;

import org.xwiki.component.annotation.ComponentRole;

@ComponentRole
public interface AetherConfiguration
{
    File getLocalRepository();
}
