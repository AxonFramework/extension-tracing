# Axon Framework - Tracing Extension 
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.axonframework.extensions.tracing/axon-tracing/badge.svg)](https://maven-badges.herokuapp.com/maven-central/org.axonframework.extensions.tracing/axon-tracing/)
![Build Status](https://github.com/AxonFramework/extension-tracing/workflows/Tracing%20Extension/badge.svg?branch=master)
[![SonarCloud Status](https://sonarcloud.io/api/project_badges/measure?project=AxonFramework_extension-tracing&metric=alert_status)](https://sonarcloud.io/dashboard?id=AxonFramework_extension-tracing)

Axon Framework is a framework for building evolutionary, event-driven microservice systems,
 based on the principles of Domain Driven Design, Command-Query Responsibility Segregation (CQRS) and Event Sourcing.

As such it provides you the necessary building blocks to follow these principles. 
Building blocks like Aggregate factories and Repositories, Command, Event and Query Buses and an Event Store.
The framework provides sensible defaults for all of these components out of the box.

This set up helps you create a well structured application without having to bother with the infrastructure.
The main focus can thus become your business functionality.

This repository provides an extension to the Axon Framework: Tracing.
It provides functionality to trace command, event and query messages flowing through your Axon application by providing 
 a specific implementation of the `CommandGateway`, `QueryGateway`, `MessageDispatchInterceptor` and
 `MessageHandlerInterceptor`.
The [Open Tracing](https://opentracing.io/) standard is used to provide the tracing capabilities,
 which thus allows you to use several Open Tracing implementations. 
  
For more information on anything Axon, please visit our website, [http://axoniq.io](http://axoniq.io).

## Getting started

The [AxonIQ Library](https://library.axoniq.io) contains a section for the guides of all the Axon Framework extensions.
The Spring AOT extension guide can be found [here](https://library.axoniq.io/home/guides/axon-framework.html).

## Receiving help

Are you having trouble using the extension? 
We'd like to help you out the best we can!
There are a couple of things to consider when you're traversing anything Axon:

* Checking the [reference guide](https://library.axoniq.io/axon-framework-reference/) should be your first stop,
  as the majority of possible scenarios you might encounter when using Axon should be covered there.
* If the Reference Guide does not cover a specific topic you would've expected,
  we'd appreciate if you could post a [new thread/topic on our library fourms describing the problem](https://discuss.axoniq.io/c/26).
* There is a [forum](https://discuss.axoniq.io/) to support you in the case the reference guide did not sufficiently answer your question.
Axon Framework and Server developers will help out on a best effort basis.
Know that any support from contributors on posted question is very much appreciated on the forum.
* Next to the forum we also monitor Stack Overflow for any questions which are tagged with `axon`.

## Feature requests and issue reporting

We use GitHub's [issue tracking system](https://github.com/AxonFramework/extension-tracing/issues) for new feature 
request, extension enhancements and bugs. 
Prior to filing an issue, please verify that it's not already reported by someone else.

When filing bugs:
* A description of your setup and what's happening helps us figuring out what the issue might be
* Do not forget to provide version you're using
* If possible, share a stack trace, using the Markdown semantic ```

When filing features:
* A description of the envisioned addition or enhancement should be provided
* (Pseudo-)Code snippets showing what it might look like help us understand your suggestion better 
* If you have any thoughts on where to plug this into the framework, that would be very helpful too
* Lastly, we value contributions to the framework highly. So please provide a Pull Request as well!
 
## Credits

---