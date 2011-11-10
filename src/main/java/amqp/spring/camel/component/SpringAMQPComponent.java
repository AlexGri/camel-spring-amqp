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

import java.util.Map;
import org.apache.camel.CamelContext;
import org.apache.camel.Endpoint;
import org.apache.camel.impl.DefaultComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

public class SpringAMQPComponent extends DefaultComponent {
    private static transient final Logger LOG = LoggerFactory.getLogger(SpringAMQPComponent.class);
    
    protected ConnectionFactory connectionFactory;
    protected AmqpTemplate amqpTemplate;
    protected AmqpAdmin amqpAdministration;
    public static final String ROUTING_KEY_HEADER = "ROUTING_KEY";
    
    public SpringAMQPComponent() {
        this.connectionFactory = new CachingConnectionFactory();
    }
    
    public SpringAMQPComponent(CamelContext context) {
        super(context);
        
        //Attempt to load a connection factory from the registry
        if(this.connectionFactory == null) {
            Map<String, ConnectionFactory> factories = context.getRegistry().lookupByType(ConnectionFactory.class);
            if(factories != null && ! factories.isEmpty()) {
                this.connectionFactory = factories.values().iterator().next();
                LOG.info("Found AMQP ConnectionFactory in registry for {}", this.connectionFactory.getHost());
            }
        }
        
        if(this.connectionFactory == null) {
            LOG.error("Cannot find a connection factory!");
        }
    }
    
    public SpringAMQPComponent(ConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    @Override
    protected Endpoint createEndpoint(String uri, String remaining, Map<String, Object> parameters) throws Exception {
        SpringAMQPEndpoint endpoint = new SpringAMQPEndpoint(remaining, getAmqpTemplate(), getAmqpAdministration());
        setProperties(endpoint, parameters);
        return endpoint;
    }

    public ConnectionFactory getConnectionFactory() {
        return connectionFactory;
    }

    public void setConnectionFactory(ConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    public AmqpAdmin getAmqpAdministration() {
        if(this.amqpAdministration == null) {
            //Attempt to load an administration connection from the registry
            Map<String, AmqpAdmin> factories = getCamelContext().getRegistry().lookupByType(AmqpAdmin.class);
            if(factories != null && ! factories.isEmpty()) {
                this.amqpAdministration = factories.values().iterator().next();
                LOG.info("Found AMQP Administrator in registry");
            }
        }
        
        if(this.amqpAdministration == null) {
            //Attempt to construct an AMQP Adminstration instance
            this.amqpAdministration = new RabbitAdmin(this.connectionFactory);
            LOG.info("Created new AMQP Administration instance");
        }
        
        return this.amqpAdministration;
    }

    public void setAmqpAdministration(AmqpAdmin amqpAdministration) {
        this.amqpAdministration = amqpAdministration;
    }

    public AmqpTemplate getAmqpTemplate() {
        if(this.amqpTemplate == null) {
            //Attempt to load an AMQP template from the registry
            Map<String, AmqpTemplate> factories = getCamelContext().getRegistry().lookupByType(AmqpTemplate.class);
            if(factories != null && ! factories.isEmpty()) {
                this.amqpTemplate = factories.values().iterator().next();
                LOG.info("Found AMQP Template in registry");
            }
        }
        
        if(this.amqpTemplate == null) {
            //Attempt to construct an AMQP template
            this.amqpTemplate = new RabbitTemplate(this.connectionFactory);
            LOG.info("Created new AMQP Template");
        }
        
        return this.amqpTemplate;
    }

    public void setAmqpTemplate(AmqpTemplate amqpTemplate) {
        this.amqpTemplate = amqpTemplate;
    }    
}
