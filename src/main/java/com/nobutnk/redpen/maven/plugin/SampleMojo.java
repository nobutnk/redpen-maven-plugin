package com.nobutnk.redpen.maven.plugin;

import java.io.File;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * Sample
 */
@Mojo(name = "sample", threadSafe = true, defaultPhase = LifecyclePhase.COMPILE)
public class SampleMojo extends AbstractMojo {
    /**
     * Location of the file.
     */
    @Parameter(property = "project.build.directory", required = true)
    private File outputDirectory;

    @Override
    public void execute() throws MojoExecutionException {
        getLog().info("sample plugin start!");
        getLog().debug("project.build.directory is " + outputDirectory.getAbsolutePath());
    }
}
