package org.xwiki.extension.repository.xwiki;

import org.junit.Assert;
import org.junit.Test;

public class UriBuilderTest
{
    @Test
    public void testBuildWithUTF8()
    {
        UriBuilder uriBuilder = new UriBuilder("http://base", "{toto}");

        uriBuilder.queryParam("param", "é");

        Assert.assertEquals("http://base/%C3%A9?param=%C3%A9", uriBuilder.build("é").toString());
    }
}
