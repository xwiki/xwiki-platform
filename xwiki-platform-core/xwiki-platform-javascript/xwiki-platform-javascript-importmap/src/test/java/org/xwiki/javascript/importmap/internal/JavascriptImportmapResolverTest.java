package org.xwiki.javascript.importmap.internal;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.xwiki.extension.repository.CoreExtensionRepository;
import org.xwiki.extension.repository.InstalledExtensionRepository;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.webjars.WebJarsUrlFactory;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Test of {@link JavascriptImportmapResolver}.
 *
 * @version $Id$
 * @since 17.10.0RC1
 */
@ComponentTest
class JavascriptImportmapResolverTest
{
    @InjectMockComponents
    private JavascriptImportmapResolver javascriptImportmapResolver;

    @MockComponent
    private InstalledExtensionRepository installedExtensionRepository;

    @MockComponent
    private CoreExtensionRepository coreExtensionRepository;

    @MockComponent
    private WebJarsUrlFactory webJarsUrlFactory;

    @Test
    void cacheIsUsedUntilCleared()
    {
        this.javascriptImportmapResolver.getBlock();
        // Get installed extension is used as a proxy to verify if the resolution logic is called.
        verify(this.installedExtensionRepository).getInstalledExtensions();
        this.javascriptImportmapResolver.getBlock();
        verify(this.installedExtensionRepository).getInstalledExtensions();
        this.javascriptImportmapResolver.clearCache();
        this.javascriptImportmapResolver.getBlock();
        verify(this.installedExtensionRepository, times(2)).getInstalledExtensions();
    }

    // TODO: add more tests
}