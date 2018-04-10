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

package org.dataconservancy.pass.loader.journal.nih;

import static org.dataconservancy.pass.client.util.ConfigUtil.getSystemProperty;
import static org.dataconservancy.pass.loader.journal.nih.NihTypeAReader.readJournals;

import java.io.FileReader;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;

import org.dataconservancy.pass.client.fedora.FedoraPassClient;

import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultRedirectStrategy;
import org.apache.http.impl.client.HttpClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main method for csv loader executable.
 *
 * @author apb@jhu.edu
 */
public class Main {

    static final Logger LOG = LoggerFactory.getLogger(Main.class);

    private static final String DEFAULT_JOURNAL_LIST_URL =
            "http://www.ncbi.nlm.nih.gov/pmc/front-page/NIH_PA_journal_list.csv";

    public static void main(String[] args) throws Exception {

        final LoaderEngine loader = new LoaderEngine(new FedoraPassClient());

        LogUtil.adjustLogLevels();

        loader.load(readJournals(getReader()));
        LOG.info("done!");

    }

    @SuppressWarnings("resource")
    private static Reader getReader() throws Exception {

        final String fileName = getSystemProperty("file", null);
        if (fileName != null) {
            LOG.info("Reading from file " + fileName);
            return new FileReader(fileName);
        }

        final CloseableHttpClient client = HttpClientBuilder.create().setRedirectStrategy(
                new DefaultRedirectStrategy()).build();

        final String url = getSystemProperty("url", DEFAULT_JOURNAL_LIST_URL);
        LOG.info("Reading from URL " + url);
        final HttpGet get = new HttpGet(url);

        final CloseableHttpResponse response = client.execute(get);

        if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
            return new InputStreamReader(response.getEntity().getContent(), StandardCharsets.ISO_8859_1);
        }

        throw new RuntimeException("Bad http response to " + url + ": " + response.getStatusLine());
    }
}
