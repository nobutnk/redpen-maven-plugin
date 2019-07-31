package com.nobutnk.redpen.maven.plugin;

import static org.junit.Assert.fail;

import org.apache.maven.plugin.MojoExecutionException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class RedpenMojoTest {

    private RedpenMojo mojo = new RedpenMojo();
    
    @Rule
    public TemporaryFolder outputFolder = new TemporaryFolder();
    
    @Before
    public void setUp() throws Exception {
        mojo.setConfigFileName("src/test/resources/redpen-conf-ja.xml");
        mojo.setInputFormat("asciidoc");
        mojo.setLanguage("ja");
        mojo.setLimit(1);
        mojo.setResultFormat("plain2");
        mojo.setOutputDirectory(outputFolder.getRoot());
        mojo.setResultFileName("redpen-result.txt");
    }
    
    @Test
    public void test_executeWithError() throws MojoExecutionException {
        mojo.setInputFile("src/test/resources");
        try {
            mojo.execute();
            fail("error");
        } catch (MojoExecutionException e) {
            
        }
    }
    
    @Test
    public void test_executeWithoutError() throws MojoExecutionException {
        mojo.setInputFile("src/test/resources/noerror");
        try {
            mojo.execute();
        } catch (MojoExecutionException e) {
            fail("error");
        }
    }
}
