/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License version 2.0 (the "License"). You can obtain a copy of the
 * License at http://mozilla.org/MPL/2.0/.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Original Code is camel-spring-amqp.
 * 
 * The Initial Developer of the Original Code is Bluelock, LLC.
 * Copyright (c) 2007-2011 Bluelock, LLC. All Rights Reserved.
 */
package amqp.spring.camel.component;

import java.io.IOException;
import java.util.Map.Entry;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.impl.DefaultConsumer;
import org.apache.camel.impl.DefaultExchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.MessageConverter;

public class SpringAMQPConsumer extends DefaultConsumer {
    private static transient final Logger LOG = LoggerFactory.getLogger(SpringAMQPProducer.class);
    
    protected SpringAMQPEndpoint endpoint;
    private ThreadPoolExecutor consumers;
    private Queue queue;
    private Binding binding;
    
    public SpringAMQPConsumer(SpringAMQPEndpoint endpoint, Processor processor) {
        super(endpoint, processor);
        this.endpoint = endpoint;
    }

    @Override
    public void start() throws Exception {
        super.start();
        
        org.springframework.amqp.core.Exchange exchange = SpringAMQPProducer.createAMQPExchange(this.endpoint);
        this.endpoint.amqpAdministration.declareExchange(exchange);
        LOG.info("Declared exchange {}", exchange.getName());

        this.queue = new Queue(this.endpoint.queueName, this.endpoint.durable, this.endpoint.exclusive, this.endpoint.autodelete);
        this.endpoint.getAmqpAdministration().declareQueue(queue);
        LOG.info("Declared queue {}", this.queue.getName());
        
        this.binding = BindingBuilder.bind(this.queue).to(exchange).with(this.endpoint.routingKey).noargs();
        this.endpoint.getAmqpAdministration().declareBinding(binding);
        LOG.info("Declared binding {}", this.binding.getRoutingKey());
        
        BlockingQueue<Runnable> threadQueue = new LinkedBlockingQueue(this.endpoint.concurrentConsumers);
        this.consumers = new ThreadPoolExecutor(this.endpoint.concurrentConsumers, this.endpoint.concurrentConsumers, Long.MAX_VALUE, TimeUnit.MILLISECONDS, threadQueue);
        
        for(int i = 0; i < this.endpoint.concurrentConsumers; ++i)
            this.consumers.execute(new RabbitMQConsumerTask((RabbitTemplate) this.endpoint.getAmqpTemplate()));
    }

    @Override
    public void stop() throws Exception {
        if(this.consumers != null) {
            LOG.info("Shutting down {} consumers", endpoint.concurrentConsumers);
            this.consumers.shutdown();
            try {
                this.consumers.awaitTermination(60, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                LOG.error("Waited 60 seconds for termination of all consumers but timed out. Forcing the end!");
                this.consumers.shutdownNow();
            }
            this.consumers.purge();
            this.consumers.getQueue().clear();
        }

        if(this.endpoint.amqpAdministration != null && this.binding != null) {
            this.endpoint.amqpAdministration.removeBinding(this.binding);
            LOG.info("Removed binding {}", this.binding.getRoutingKey());
        }
        
        if(this.endpoint.amqpAdministration != null && this.queue != null) {
            this.endpoint.amqpAdministration.deleteQueue(this.queue.getName());
            LOG.info("Deleted queue {}", this.queue.getName());
        }
        
        super.stop();
    }

    //We have to ask the RabbitMQ Template for converters, the interface doesn't have a way to get MessageConverter
    class RabbitMQConsumerTask implements Runnable {
        private RabbitTemplate template;
        
        public RabbitMQConsumerTask(RabbitTemplate template) {
            this.template = template;
        }
        
        @Override
        public void run() {
            try {
                LOG.info("Prepared consumer for {}", endpoint.queueName);

                while(isRunAllowed()) {
                    Message message = this.template.receive(endpoint.queueName);
                    
                    if(message == null) {
                        LOG.error("Received invalid message, cannot process!");
                        continue;
                    }
                    
                    LOG.trace("Received message for routing key {}", message.getMessageProperties().getReceivedRoutingKey());
                    
                    MessageConverter msgConverter = this.template.getMessageConverter();
                    Object body = msgConverter.fromMessage(message);
                    
                    Exchange exchange = new DefaultExchange(endpoint, endpoint.getExchangePattern());
                    exchange.getIn().setBody(body);
                    for(Entry<String, Object> headerEntry : message.getMessageProperties().getHeaders().entrySet())
                        exchange.getIn().setHeader(headerEntry.getKey(), headerEntry.getValue());
                    
                    getProcessor().process(exchange);
                }
            } catch (IOException e) {
                LOG.error("Error when attempting to speak with RabbitMQ", e);
            } catch (InterruptedException e) {
                LOG.warn("Thread was interrupted while waiting for message consumption", e);
            } catch (Exception e) {
                LOG.warn("General exception during Camel handoff, Processor returned error", e);
            }
        }
        
    }
}
