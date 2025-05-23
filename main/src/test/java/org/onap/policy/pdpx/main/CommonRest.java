/*-
 * ============LICENSE_START=======================================================
 * ONAP
 * ================================================================================
 * Copyright (C) 2019, 2021 AT&T Intellectual Property. All rights reserved.
 * Modifications Copyright (C) 2019, 2023-2024 Nordix Foundation.
 * ================================================================================
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
 *
 * SPDX-License-Identifier: Apache-2.0
 * ============LICENSE_END=========================================================
 */

package org.onap.policy.pdpx.main;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Invocation;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.MediaType;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.net.ssl.SSLContext;
import org.glassfish.jersey.client.authentication.HttpAuthenticationFeature;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.onap.policy.common.utils.network.NetworkUtil;
import org.onap.policy.common.utils.resources.ResourceUtils;
import org.onap.policy.common.utils.security.SelfSignedKeyStore;
import org.onap.policy.pdpx.main.rest.XacmlPdpStatisticsManager;
import org.onap.policy.pdpx.main.startstop.Main;
import org.onap.policy.pdpx.main.startstop.XacmlPdpActivator;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Common base class for REST service tests.
 */
public class CommonRest {

    /**
     * Full path to the config file.
     */
    public static final String CONFIG_FILE;

    /**
     * Path corresponding to {@link #CONFIG_FILE}.
     */
    private static final Path CONFIG_PATH;

    /**
     * Contents read from the "standard" config file, which still contains ${xxx}
     * place-holders.
     */
    private static final String STD_CONFIG;

    /**
     * Port that was last allocated for the server.
     */
    protected static int port;

    /**
     * "Main" that was last started.
     */
    private static Main main;

    /**
     * Records the "alive" state of the activator while it's temporarily updated by
     * various junit tests. The {@link #tearDown()} method restores the "alive" state back
     * to this value.
     */
    private boolean activatorWasAlive;

    static {
        try {
            File file = new File(ResourceUtils.getFilePath4Resource("parameters/XacmlPdpConfigParameters_Std.json"));
            STD_CONFIG = Files.readString(file.toPath());

            file = new File(file.getParentFile(), "Test_XacmlPdpConfigParameters.json");
            file.deleteOnExit();

            CONFIG_FILE = file.getAbsolutePath();
            CONFIG_PATH = new File(CONFIG_FILE).toPath();

        } catch (IOException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    /**
     * Configures system properties and creates a JSON config file.
     *
     * @throws Exception if an error occurs
     */
    @BeforeAll
    public static void setUpBeforeClass() throws Exception {
        System.setProperty("javax.net.ssl.keyStore", new SelfSignedKeyStore().getKeystoreName());
        System.setProperty("javax.net.ssl.keyStorePassword", SelfSignedKeyStore.KEYSTORE_PASSWORD);

        System.setProperty("org.eclipse.jetty.util.log.class", "org.eclipse.jetty.util.log.StdErrLog");
        System.setProperty("org.eclipse.jetty.LEVEL", "OFF");

        writeJsonConfig();

        final String[] xacmlPdpConfigParameters = {"-c", CommonRest.CONFIG_FILE};
        main = new Main(xacmlPdpConfigParameters);

        // start xacml rest controller
        XacmlPdpActivator.getCurrent().enableApi();

        if (!NetworkUtil.isTcpPortOpen("localhost", port, 20, 1000L)) {
            throw new IllegalStateException("server is not listening on port " + port);
        }
    }

    /**
     * Stops the "Main".
     */
    @AfterAll
    public static void tearDownAfterClass() {
        stopMain();
    }

    /**
     * Resets the statistics.
     */
    @BeforeEach
    public void setUp() {
        activatorWasAlive = XacmlPdpActivator.getCurrent().isAlive();
        XacmlPdpStatisticsManager.getCurrent().resetAllStatistics();
    }

    /**
     * Restores the "alive" status of the activator.
     */
    @AfterEach
    public void tearDown() {
        markActivator(activatorWasAlive);
    }

    /**
     * Stops the "main".
     */
    protected static void stopMain() {
        main.shutdown();
    }

    /**
     * Writes a JSON config file, substituting an allocated port number for occurrences of
     * "${port}".
     *
     * @throws IOException if the config file cannot be created
     */
    public static void writeJsonConfig() throws IOException {
        port = NetworkUtil.allocPort();

        String config = STD_CONFIG.replace("${port}", String.valueOf(port));
        Files.writeString(CONFIG_PATH, config);

    }

    /**
     * Sends an HTTPS request to an endpoint of the PDP's REST API.
     *
     * @param endpoint target endpoint
     * @return a request builder
     * @throws Exception if an error occurs
     */
    protected Invocation.Builder sendHttpsRequest(final String endpoint) throws Exception {
        // always trust the certificate
        final SSLContext sc = SSLContext.getInstance("TLSv1.2");
        sc.init(null, NetworkUtil.getAlwaysTrustingManager(), new SecureRandom());

        // always trust the host name
        final ClientBuilder clientBuilder =
            ClientBuilder.newBuilder().sslContext(sc).hostnameVerifier((host, session) -> true);

        final Client client = clientBuilder.build();
        final HttpAuthenticationFeature feature = HttpAuthenticationFeature.basic("healthcheck", "zb!XztG34");
        client.register(feature);

        final WebTarget webTarget = client.target("https://localhost:" + port + "/policy/pdpx/v1/" + endpoint);

        return webTarget.request(MediaType.APPLICATION_JSON);
    }

    /**
     * Mark the activator as dead, but leave its REST server running.
     */
    protected void markActivatorDead() {
        markActivator(false);
    }

    /**
     * Changes the internal "alive" status of the activator to a new value.
     *
     * @param newAlive the new "alive" status
     */
    private void markActivator(boolean newAlive) {
        Object manager = ReflectionTestUtils.getField(XacmlPdpActivator.getCurrent(), "serviceManager");
        AtomicBoolean running = (AtomicBoolean) ReflectionTestUtils
            .getField(Objects.requireNonNull(manager), "running");
        Objects.requireNonNull(running).set(newAlive);
    }
}
