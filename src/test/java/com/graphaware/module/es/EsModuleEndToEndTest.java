package com.graphaware.module.es;

import com.graphaware.module.es.util.CustomClassLoading;
import com.graphaware.module.es.util.PassThroughProxyHandler;
import com.graphaware.module.es.wrapper.IGenericWrapper;
import com.graphaware.test.integration.NeoServerIntegrationTest;
import org.eclipse.jetty.http.HttpStatus;
import org.elasticsearch.node.Node;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.Proxy;
import java.net.MalformedURLException;
import java.util.concurrent.Executors;

import static org.elasticsearch.node.NodeBuilder.nodeBuilder;
import static org.junit.Assert.assertEquals;

public class EsModuleEndToEndTest extends NeoServerIntegrationTest {

    private static final Logger LOG = LoggerFactory.getLogger(EsModuleEndToEndTest.class);

    private static String ELASTICSEARCH_URL = "http://localhost:9200";
    private static Node ELASTICSEARCH_NODE;
    private IGenericWrapper indexWrapper;

    @Override
    protected String neo4jConfigFile() {
        return "neo4j-es.properties";
    }

    @Override
    public void setUp() throws IOException, InterruptedException {
        final String classpath = System.getProperty("classpath");
        LOG.warn("classpath: " + classpath);
        try
        {
            CustomClassLoading loader = new CustomClassLoading(classpath);
            Class<Object> loadedClass = (Class<Object>) loader.loadClass("com.graphaware.module.es.wrapper.ESWrapper");
            indexWrapper = (IGenericWrapper) Proxy.newProxyInstance(this.getClass().getClassLoader(),
                    new Class[]
                            {
                                    IGenericWrapper.class
                            },
                    new PassThroughProxyHandler(loadedClass.newInstance()));
            indexWrapper.startLocalClient();
        }
        catch (Exception ex)
        {
            LOG.warn("Error while creating and starting client", ex);
        }

        super.setUp();
        ELASTICSEARCH_NODE = nodeBuilder().node();
        ELASTICSEARCH_NODE.start();
    }

    @Override
    public void tearDown() throws IOException, InterruptedException {
        super.tearDown();

        ELASTICSEARCH_NODE.close();
    }

    @Test
    public void testIntegration() {
        httpClient.executeCypher(baseUrl(), "CREATE (c:Car {name:'Tesla Model S'})");

        String response = httpClient.get(ELASTICSEARCH_URL + "/_cluster/health?pretty=true", HttpStatus.OK_200);

        assertEquals("", response);
    }
}