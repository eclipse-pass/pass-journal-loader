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
import java.net.URI;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.dataconservancy.pass.client.PassClient;
import org.dataconservancy.pass.client.PassClientFactory;
import org.dataconservancy.pass.client.fedora.FedoraConfig;
import org.dataconservancy.pass.model.Journal;
import org.dataconservancy.pass.model.PmcParticipation;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author apb@jhu.edu
 */
public class DepositIT {

    private final PassClient client = PassClientFactory.getPassClient();

    private Logger LOG = LoggerFactory.getLogger(DepositIT.class);

    private Process load;

    private final String PASS_BASEURL = System.getProperty("pass.fedora.baseurl",
                                                           "http://localhost:8080/fcrepo/rest/");

    @Test
    public void loadFromFileTest() throws Exception {

        // First, load all three journals using medline data
        load = jar(new File(System.getProperty("nih.loader.jar")))
            .logOutput(LoggerFactory.getLogger("nih-loader"))
            .withEnv("MEDLINE", DepositIT.class.getResource("/medline.txt").getPath())
            .withEnv("PASS_FEDORA_BASEURL", PASS_BASEURL)
            .withEnv("LOG_ORG_DATACONSERVANCY_PASS", "DEBUG")
            .start();

        wait(load);

        // We expect three journals, but no PMC A journals
        assertEquals(3, listJournals().size());
        assertEquals(0, typeA(listJournals()).size());

        load = jar(new File(System.getProperty("nih.loader.jar")))
            .logOutput(LoggerFactory.getLogger("nih-loader"))
            .withEnv("PMC", DepositIT.class.getResource("/pmc-1.csv").getPath())
            .withEnv("PASS_FEDORA_BASEURL", PASS_BASEURL)
            .withEnv("LOG_ORG_DATACONSERVANCY_PASS", "DEBUG")
            .start();

        wait(load);

        // We still expect three journals in the repository, but now two are PMC A
        assertEquals(3, listJournals().size());
        assertEquals(2, typeA(listJournals()).size());

        load = jar(new File(System.getProperty("nih.loader.jar")))
            .logOutput(LoggerFactory.getLogger("nih-loader"))
            .withEnv("PMC", DepositIT.class.getResource("/pmc-2.csv").getPath())
            .withEnv("PASS_FEDORA_BASEURL", PASS_BASEURL)
            .withEnv("LOG_ORG_DATACONSERVANCY_PASS", "DEBUG")
            .start();

        wait(load);

        // The last dataset removed a type A journal, so now we expect only one
        assertEquals(3, listJournals().size());
        assertEquals(1, typeA(listJournals()).size());
    }

    private List<PmcParticipation> typeA(Collection<URI> uris) {
        return uris.stream()
                   .map(uri -> client.readResource(uri, Journal.class))
                   .map(Journal::getPmcParticipation)
                   .filter(Objects::nonNull)
                   .collect(Collectors.toList());
    }

    private Set<URI> listJournals() throws Exception {
        final HttpGet get = new HttpGet(PASS_BASEURL + "journals");
        get.setHeader("Accept", "application/n-triples");
        get.setHeader("Prefer",
                      "return=representation; include=\"http://www.w3.org/ns/ldp#PreferContainment\"; " +
                      "omit=\"http://fedora.info/definitions/v4/repository#ServerManaged\"");

        final Set<URI> URIs = new HashSet<>();

        try (CloseableHttpResponse response = getHttpClient().execute(get)) {
            try (InputStream in = response.getEntity().getContent()) {
                final BufferedReader reader = new BufferedReader(new InputStreamReader(in, UTF_8));
                for (String line = reader.readLine(); line != null; line = reader.readLine()) {
                    final String[] spo = line.split("\\s+");
                    URIs.add(URI.create(Objects.requireNonNull(ntripleUri(spo[2]))));
                }
            }
        }

        return URIs;
    }

    private void wait(Process toWaitFor) throws InterruptedException {
        for (int i = 0; i < 30 && load.isAlive(); i++) {
            System.out.println(".");
            Thread.sleep(1000);
        }
    }

    private static String ntripleUri(String token) {
        final int s = token.indexOf("<");
        final int f = token.indexOf(">");
        if (s != -1 && f != -1) {
            return token.substring(s + 1, f);
        }

        return null;
    }

    private static CloseableHttpClient getHttpClient() {
        final CredentialsProvider provider = new BasicCredentialsProvider();
        final UsernamePasswordCredentials credentials = new UsernamePasswordCredentials(FedoraConfig.getUserName(),
                                                                                        FedoraConfig.getPassword());
        provider.setCredentials(AuthScope.ANY, credentials);

        return HttpClientBuilder.create()
                                .setDefaultCredentialsProvider(provider)
                                .build();
    }

}
