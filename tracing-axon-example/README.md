# Tracing Axon Springboot Example

This is an example SpringBoot application using the Tracing Axon extension.
It uses the tracing gateways to provide spans across your messages. 

## How to run

### Preparation

You will need `docker` and `docker-compose` to run this example.

Please run:

```bash 
docker-compose -f ./tracing-axon-example/docker-compose.yaml up -d
```

This will start Jaeger Tracing.

Now build the application by running:

```bash
mvn clean package -f ./tracing-axon-example 
``` 

### Running example application
 
You can start the application by running `java -jar ./tracing-axon-example/target/axon-tracing-example.jar`.

You can access the jaeger ui on [http://localhost:16686](http://localhost:16686).
You should see the span created there looking at the Service with name `tracingAxonExample`.