/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.jackrabbit.osgi;

import static org.junit.Assert.assertEquals;
import static org.ops4j.pax.exam.CoreOptions.bundle;
import static org.ops4j.pax.exam.CoreOptions.frameworkProperty;
import static org.ops4j.pax.exam.CoreOptions.junitBundles;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.systemProperties;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;
import static org.ops4j.pax.exam.CoreOptions.vmOption;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.MalformedURLException;
import java.net.URISyntaxException;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.CoreOptions;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.options.DefaultCompositeOption;
import org.ops4j.pax.exam.options.SystemPropertyOption;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;
import org.ops4j.pax.exam.util.PathUtils;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.Version;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class OSGiIT {

    @Configuration
    public Option[] configuration() throws IOException, URISyntaxException {
        return CoreOptions.options(
                junitBundles(),
                mavenBundle("org.osgi", "org.osgi.service.log", "1.3.0"),
                mavenBundle("org.apache.felix", "org.apache.felix.scr", "2.0.12"),
                mavenBundle("org.apache.felix", "org.apache.felix.jaas", "1.0.2"),
                mavenBundle("org.osgi", "org.osgi.dto", "1.0.0"),
                mavenBundle("org.apache.felix", "org.apache.felix.configadmin", "1.8.16"),
                mavenBundle("org.apache.felix", "org.apache.felix.fileinstall", "3.2.6"),
                mavenBundle("org.slf4j", "slf4j-api", "1.7.36"),
                mavenBundle("commons-logging", "commons-logging", "1.2"),
                mavenBundle("ch.qos.logback", "logback-core", "1.2.13"),
                mavenBundle("ch.qos.logback", "logback-classic", "1.2.13"),
                frameworkProperty("repository.home").value("target"),
                systemProperties(
                        systemProperty("logback.configurationFile")
                                .value("file:" + PathUtils.getBaseDir() + "/src/test/resources/logback-test.xml"),
                        new SystemPropertyOption("felix.fileinstall.dir").value(getConfigDir())),
                jarBundles(),
                jpmsOptions());
    }

    private Option jpmsOptions(){
        DefaultCompositeOption composite = new DefaultCompositeOption();
        if (Version.parseVersion(System.getProperty("java.specification.version")).getMajor() > 1){
            if (java.nio.file.Files.exists(java.nio.file.FileSystems.getFileSystem(URI.create("jrt:/")).getPath("modules", "java.se.ee"))){
                composite.add(vmOption("--add-modules=java.se.ee"));
            }
            composite.add(vmOption("--add-opens=java.base/jdk.internal.loader=ALL-UNNAMED"));
            composite.add(vmOption("--add-opens=java.base/java.lang=ALL-UNNAMED"));
            composite.add(vmOption("--add-opens=java.base/java.lang.invoke=ALL-UNNAMED"));
            composite.add(vmOption("--add-opens=java.base/java.io=ALL-UNNAMED"));
            composite.add(vmOption("--add-opens=java.base/java.net=ALL-UNNAMED"));
            composite.add(vmOption("--add-opens=java.base/java.nio=ALL-UNNAMED"));
            composite.add(vmOption("--add-opens=java.base/java.util=ALL-UNNAMED"));
            composite.add(vmOption("--add-opens=java.base/java.util.jar=ALL-UNNAMED"));
            composite.add(vmOption("--add-opens=java.base/java.util.regex=ALL-UNNAMED"));
            composite.add(vmOption("--add-opens=java.base/java.util.zip=ALL-UNNAMED"));
            composite.add(vmOption("--add-opens=java.base/sun.nio.ch=ALL-UNNAMED"));
        }
        return composite;
    }

    private String getConfigDir(){
        return new File(new File("src", "test"), "config").getAbsolutePath();
    }

    private Option jarBundles() throws MalformedURLException {
        DefaultCompositeOption composite = new DefaultCompositeOption();
        for (File bundle : new File("target", "test-bundles").listFiles()) {
            if (bundle.getName().endsWith(".jar") && bundle.isFile()) {
                composite.add(bundle(bundle.toURI().toURL().toString()));
            }
        }
        return composite;
    }

    @Inject
    private BundleContext context;

    @Test
    public void bundleStates() {
        for (Bundle bundle : context.getBundles()) {
            assertEquals(
                String.format("Bundle %s not active. have a look at the logs", bundle.toString()), 
                Bundle.ACTIVE, bundle.getState());
        }
    }

    @Test
    public void listBundles() {
        for (Bundle bundle : context.getBundles()) {
            System.out.println(bundle);
        }
    }

    @Test
    public void listServices() throws InvalidSyntaxException {
        for (ServiceReference<?> reference
                : context.getAllServiceReferences(null, null)) {
            System.out.println(reference);
        }
    }
}
