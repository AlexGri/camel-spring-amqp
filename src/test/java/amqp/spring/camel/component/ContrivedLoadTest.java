/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/. */

package amqp.spring.camel.component;

//Not a super-great load test, but something to get the ball rolling

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import javax.annotation.Resource;
import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.Handler;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.spi.Synchronization;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
@Component
public class ContrivedLoadTest {
    private static transient final Logger LOG = LoggerFactory.getLogger(ContrivedLoadTest.class);
    
    @Resource
    protected ProducerTemplate template;
    @Resource
    protected CamelContext camelContext;
    
    @Before
    public void resetEndpoints() throws Exception {
    }
    
    @Test
    public void testSynchronous() throws Exception {
        final int messageCount = 1000;
        int received = 0;
        ScheduledExecutorService executorService = new ScheduledThreadPoolExecutor(messageCount);
        List<Future<String>> futures = new ArrayList<Future<String>>();
        
        long startTime = System.currentTimeMillis();
        for(int i=0; i < messageCount; ++i)
            futures.add(executorService.submit(new SynchronousRequestor(this.template)));
        LOG.info("Time to submit synchronous messages: {}", (System.currentTimeMillis() - startTime) / 1000.0f);

        for(Future<String> future : futures) {
            String response = future.get();
            if("RESPONSE".equals(response)) ++received;
        }
        LOG.info("Time to receive synchronous messages: {}", (System.currentTimeMillis() - startTime) / 1000.0f);
        
        Assert.assertEquals(messageCount, received);
    }
    
    @Test
    public void testAsynchronous() throws Exception {
        final int messageCount = 1000;
        int received = 0;
        List<Future<String>> futures = new ArrayList<Future<String>>();
        
        long startTime = System.currentTimeMillis();
        for(int i=0; i < messageCount; ++i)
            futures.add(this.template.asyncRequestBody("direct:sync", "HELLO WORLD", String.class));
        LOG.info("Time to submit asynchronous messages: {}", (System.currentTimeMillis() - startTime) / 1000.0f);

        for(Future<String> future : futures) {
            String response = future.get();
            if("RESPONSE".equals(response)) ++received;
        }
        LOG.info("Time to receive asynchronous messages: {}", (System.currentTimeMillis() - startTime) / 1000.0f);
        
        Assert.assertEquals(messageCount, received);
    }
    
    @Handler
    public void handle(Exchange exchange) {
        try {
            Thread.sleep(1000);
            exchange.getOut().setBody("RESPONSE");
        } catch(InterruptedException e) {
            LOG.error("Error when attempting to sleep in handler", e);
        }
    }
    
    private static class SynchronousRequestor implements Callable<String> {
        private ProducerTemplate template;
        
        public SynchronousRequestor(ProducerTemplate template) {
            this.template = template;
        }
        
        @Override
        public String call() throws Exception {
            return template.requestBody("direct:sync", "HELLO WORLD", String.class);
        }
    }
}
