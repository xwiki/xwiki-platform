package org.xwiki.localization.jar.internal;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

public final class JARUtils
{
    public static URL toJARURL(File jarFile) throws MalformedURLException
    {
        return new URL("jar:" + jarFile.toURI() + "!/");
    }
}
