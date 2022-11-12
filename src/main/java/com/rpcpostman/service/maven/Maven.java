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
import com.rpcpostman.util.LogResultPrintStream;
import lombok.Cleanup;
import lombok.SneakyThrows;
import org.apache.maven.cli.MavenCli;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * @author everythingbest
 * 处理从nexus下载api.jar
 * 然后通过embededmaven下载这个api.jar的依赖
 */
public class Maven {

    private final static Logger logger = LoggerFactory.getLogger(Maven.class);

    private final String nexusUrl;
    private final String fileBasePath;
    private final String nexusPathReleases;

    public Maven(String nexusUrl, String nexusPathReleases, String fileBasePath) {
        this.nexusUrl = nexusUrl;
        this.nexusPathReleases = nexusPathReleases;
        this.fileBasePath = fileBasePath;
    }

    public void dependency(String serviceName, GAV gav) {
        String versionDirName = gav.getVersion().replaceAll("\\.", "_");
        String serviceDirName = versionDirName + "_" + serviceName;
        @Cleanup
        LogResultPrintStream resultPrintStream = new LogResultPrintStream(new ByteArrayOutputStream());
        String pomPath = downPomAndJar(serviceDirName, gav.getGroupID(), gav.getArtifactID(), gav.getVersion(), resultPrintStream);
        logger.info("构建完成10%...");
        mavenCopyDependencies(pomPath, resultPrintStream);
    }

    private String downPomAndJar(String serviceName, String groupId, String artifactId, String version, LogResultPrintStream resultPrintStream) {

        resultPrintStream.println("准备下载api.jar文件...");
        resultPrintStream.println("groupId:" + groupId);
        resultPrintStream.println("artifactId:" + artifactId);
        resultPrintStream.println("version:" + version);

        String jarUrl = buildNexusUrl(groupId, artifactId, version, "jar");
        resultPrintStream.println("api.jar的url:" + jarUrl);

        String pomUrl = buildNexusUrl(groupId, artifactId, version, "pom");
        resultPrintStream.println("构建api.jar的pom.xml文件的url:" + pomUrl);

        String basePath = fileBasePath + "/" + serviceName;

        File file = new File(basePath);
        if (file.exists()) {
            file.delete();
        }

        file.mkdirs();
        String jarPath = fileBasePath + "/" + serviceName + "/lib";
        File libfile = new File(jarPath);

        if (libfile.exists()) {

            file.delete();
        }

        libfile.mkdirs();
        resultPrintStream.println("api.jar下载路径:" + jarPath);
        resultPrintStream.println("pom.xml的下载路径:" + basePath);

        resultPrintStream.println("开始下载:" + artifactId + ".jar文件");

        doDownLoadFile(jarUrl, jarPath, artifactId + ".jar");
        resultPrintStream.println("下载:" + artifactId + ".jar文件成功");
        resultPrintStream.println("开始下载:" + artifactId + "的pom.xml文件");
        doDownLoadFile(pomUrl, basePath, "pom.xml");
        resultPrintStream.println("下载:" + artifactId + "的pom.xml文件成功");

        return basePath;
    }

    private void mavenCopyDependencies(String pomPath, LogResultPrintStream resultPrintStream) {

        MavenCli cli = new MavenCli();

        resultPrintStream.println("处理api.jar的所有依赖,通过执行maven命令: 'mvn dependency:copy-dependencies"
                + " -DoutputDirectory=./lib -DexcludeScope=provided -U'");

        resultPrintStream.println("开始执行maven命令");
        System.setProperty("maven.multiModuleProjectDirectory", "./");

        int result = cli.doMain(new String[]{
                "dependency:copy-dependencies",
                "-DoutputDirectory=./lib",
                "-DexcludeScope=provided ",
                "-U"}, pomPath, resultPrintStream, resultPrintStream);


        boolean success = (result == 0);

        logger.info("构建完成100%,构建结果:{}", success);
        if (!success) {
            byte[] logByteArray = resultPrintStream.getLogByteArray();
            throw new RuntimeException(new String(logByteArray, StandardCharsets.UTF_8));
        }
    }

    @SneakyThrows
    private void doDownLoadFile(String baseUrl, String filePath, String fileName) {

        URL httpUrl = new URL(baseUrl);

        HttpURLConnection conn = (HttpURLConnection) httpUrl.openConnection();
        conn.setDoInput(true);
        conn.setDoOutput(true);
        conn.connect();

        InputStream inputStream = conn.getInputStream();
        BufferedInputStream bis = new BufferedInputStream(inputStream);

        //判断文件的保存路径后面是否以/结尾
        if (!filePath.endsWith("/")) {

            filePath += "/";

        }

        FileOutputStream fileOut = new FileOutputStream(filePath + fileName);
        BufferedOutputStream bos = new BufferedOutputStream(fileOut);

        byte[] buf = new byte[4096];
        int length = bis.read(buf);
        //保存文件
        while (length != -1) {
            bos.write(buf, 0, length);
            length = bis.read(buf);
        }

        bos.close();
        bis.close();
        conn.disconnect();
    }

    String buildNexusUrl(String groupId, String artifactId, String version, String type) {
        String upperV = version.trim().toUpperCase();
        String suffixUrl;

        if (upperV.endsWith("SNAPSHOT")) {
            suffixUrl = "?r=" + "snapshots&g=" + groupId + "&a=" + artifactId + "&v=" + version + "&e=" + type;
            return nexusUrl + suffixUrl;
        } else {
            suffixUrl = groupId.replace(".", "/") + "/" + artifactId + "/" + version + "/" +
                    artifactId + "-" + version + "." + type;
            return nexusPathReleases + suffixUrl;
        }
    }

}
