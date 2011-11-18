# camel-spring-amqp

## Introduction

An [Apache Camel](http://camel.apache.org/ "Apache Camel") Component that will natively communicate with a [RabbitMQ](http://www.rabbitmq.com/ "RabbitMQ") broker. 
This is implemented using Spring's AMQP project, so it should ultimately become vendor-agnostic.

## Usage

The simplest Fanout exchange can be defined as:

`spring-amqp:<Exchange Name>`

`spring-amqp:<Exchange Name>:<Queue Name>`

Here a simple message producer will send a message to Exchange Name, and a simple consumer will bind the Exchange Name to the Queue Name.

If you wish to use a routing key, URIs have the structure: 

`spring-amqp:<Exchange Name>:<Queue Name>:<Routing Key>?type=<Exchange Type>`

The routing key is optional, but Queue Name and Exchange Name are required for consumers. Just the Exchange Name is required for producers.

Options to the URI include the exchange type, which defaults to direct if none is specified.

For header based exchanges, the URI is similar but name/value pairs can be specified in place of the routing key. For example:

`spring-amqp:myExchange:qName:cheese=gouda?type=headers`

This example will fetch all messages where a header named "cheese" has the value of "gouda." You can also add additional name/value pairs:

`spring-amqp:myExchange:qName:cheese=gouda&fromage=jack?type=headers`

Which will create a binding for headers where "cheese" has the value of "gouda" AND "fromage" has the value of "jack." You can also choose to create an OR relationship:

`spring-amqp:myExchange:qName:cheese=gouda|fromage=jack?type=headers`

## Additional Settings and Properties

Additional properties can be added to the endpoint as URI parameters. For example, to create a topic exchange that is non-exclusive and not durable:

`spring-amqp:myExchange_10:writeQueue:write.*?type=topic&durable=false&autodelete=true&exclusive=false`

Parameters available include:

<table>
    <tr>
        <td>prefetchCount</td>
        <td>How many messages a consumer should pre-fetch</td>
    </tr>
    <tr>
        <td>concurrentConsumers</td>
        <td>The number of concurrent threads that will consume messages from a queue</td>
    </tr>
    <tr>
        <td>transactional</td>
        <td>Mark messages coming to/from this endpoint as transactional</td>
    </tr>
    <tr>
        <td>autodelete</td>
        <td>Allow this endpoint to be automagically deleted from the broker once it is gone</td>
    </tr>
    <tr>
        <td>durable</td>
        <td>Make messages being produced from this endpoint persistent</td>
    </tr>
    <tr>
        <td>type</td>
        <td>One of the AMQP exchange types: direct, fanout, headers, or topic. Defaults to direct.</td>
    </tr>
    <tr>
        <td>exclusive</td>
        <td>Mark this endpoint as an exclusive point for message exchanges</td>
    </tr>
</table>

## Limitations

 - Transactions are currently not supported

 - TTLs have not yet been implemented

 - Lifecycle events (e.g. stop, shutdown) need to be refined

 - Unit tests require a running AMQP broker. I may end up creating a VM local Qpid instance as an AMQP broker...

 - The component is currently undergoing integration testing but has not yet been tested in a production environment

## License

This package, the Camel Spring AMQP component is licensed under the Mozilla Public License v2.0. See LICENSE for details.
