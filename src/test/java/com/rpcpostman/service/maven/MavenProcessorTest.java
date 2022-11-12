/*
 * MIT License
 *
 * Copyright (c) 2019 everythingbest
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.rpcpostman.service.maven;

import com.rpcpostman.service.GAV;
import org.junit.Test;

public class MavenProcessorTest {

    @Test
    public void testProcess() {

        String key = "user.home";

        System.setProperty(key, "D:\\opsouce_project\\dubbo-postman\\dubbo-postman");

        String nexusUrl = "http://nexus.javamaster.org:8081/nexus/service/local/artifact/maven/redirect";
        String nexusUrlReleases = "http://nexus.javamaster.org:8081/nexus/service/local/repositories/thridparty/content/";

        String fileBasePath = "D:\\opsouce_project\\dubbo-postman\\dubbo-postman";

        Maven processResources = new Maven(nexusUrl, nexusUrlReleases, fileBasePath);

        String g = "org.javamaster.b2c";
        String a = "b2c-archetype-id";
        String v = "1.0-SNAPSHOT";

        GAV gav = new GAV();
        gav.setVersion(v);
        gav.setArtifactID(a);
        gav.setGroupID(g);

        processResources.dependency("test-service", gav);
    }
}
