/*
 * Copyright 2017 Johns Hopkins University
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.dataconservancy.pass.loader.journal.integration;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.dataconservancy.pass.loader.journal.integration.JarRunner.jar;
import static org.junit.Assert.assertEquals;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.Set;

import org.dataconservancy.pass.client.fedora.FedoraConfig;

import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author apb@jhu.edu
 */
public class DepositIT {

    Logger LOG = LoggerFactory.getLogger(DepositIT.class);

    Process load;

    final String PASS_BASEURL = System.getProperty("pass.fedora.baseurl",
            "http://localhost:8080/fcrepo/rest/");

    @Test
    public void loadFromFileTest() throws Exception {

        final String loc = DepositIT.class.getResource("/data.csv").getPath();

        System.out.println("csv loader jar is: " + System.getProperty("csv.loader.jar"));

        load = jar(new File(System.getProperty("csv.loader.jar",
                "../pass-journal-loader-csv/target/pass-journal-loader-csv-0.0.1-SNAPSHOT-exe.jar").toString()))
                        .logOutput(LoggerFactory.getLogger("csv-loader"))
                        .withEnv("file", loc)
                        .withEnv("pass.fedora.baseurl", PASS_BASEURL)
                        .withEnv("LOG.org.dataconservancy.pass", "DEBUG")
                        .start();

        for (int i = 0; i < 30 && load.isAlive(); i++) {
            System.out.println(".");
            Thread.sleep(1000);
        }

        final HttpGet get = new HttpGet(PASS_BASEURL + "journals");
        get.setHeader("Accept", "application/n-triples");
        get.setHeader("Prefer",
                "return=representation; include=\"http://www.w3.org/ns/ldp#PreferContainment\"; omit=\"http://fedora.info/definitions/v4/repository#ServerManaged\"");

        final Set<String> URIs = new HashSet<>();

        try (CloseableHttpResponse response = getHttpClient().execute(get)) {
            try (InputStream in = response.getEntity().getContent()) {
                final BufferedReader reader = new BufferedReader(new InputStreamReader(in, UTF_8));
                for (String line = reader.readLine(); line != null; line = reader.readLine()) {
                    final String[] spo = line.split("\\s+");
                    URIs.add(ntripleUri(spo[2]));
                }
            }
        }

        assertEquals(1, URIs.size());
    }

    static String ntripleUri(String token) {
        final int s = token.indexOf("<");
        final int f = token.indexOf(">");
        if (s != -1 && f != -1) {
            return token.substring(s + 1, f);
        }

        return null;
    }

    static CloseableHttpClient getHttpClient() {
        final CredentialsProvider provider = new BasicCredentialsProvider();
        final UsernamePasswordCredentials credentials = new UsernamePasswordCredentials(FedoraConfig.getUserName(),
                FedoraConfig.getPassword());
        provider.setCredentials(AuthScope.ANY, credentials);

        return HttpClientBuilder.create()
                .setDefaultCredentialsProvider(provider)
                .build();
    }

}
