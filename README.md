# Generic Async ID Client
A generic async client implementation for client services such as [Flurry Unique id generation service](https://github.com/BazuSports/flurry). It's based on a producer-consumer pattern. A thread pool constantly fetches new ids and keeps them in a local queue.

## Compatibility
- Java 6+
- Supported clients
-- Flurry v0.2.0-beta

## Dependency
```xml
<dependency>
    <groupId>com.hubrick.client</groupId>
    <artifactId>id-client</artifactId>
    <version>1.0.0</version>
</dependency>
```
## Features
- Flurry
- - Id collision protection (If the servers are misconfiguration and have the same workerId a collision can occur. The client checks the configuration on startup.)
- - Automated reconnection to servers (If a server was down and shows up again the client will autocratically reconnect and start to consume the ids again)

## How to use

## How to use

###Create an instance
```java
public IdClient createClient() {
	return new FlurryIdClient.Builder()
		.addServiceEndpoint("localhost", 9090)
		.addServiceEndpoint("localhost", 9091)
		.addServiceEndpoint("localhost", 9092)
		.withQueueSize(1000)
		.withThreadPoolSize(10)
		.withWaitOnFailMillis(1000)
		.build();
    }
```

###Consume ids
```java
public void someMethod(IdClient idClient) {
	try {
		System.out.println("Retrieved id in a blocking way. ID: " + idClient.getId());	
		System.out.println("Retrieved id in a blocking way wit timeout. ID: " + idClient.getId(100));	
	} catch (InterruptedException e) {
		// Should not happen
	}
	
	System.out.println("Retrieved id in a non blocking way. ID: " + idClient.getIdNonBlocking());	
}
```
## Exceptions
 Name                               | Description
 ---------------------------------- | --------------------------------------------------------------------------------------
 IdClientException                  | Base exception
 ConnectionException                | When the client is unable to connect to the id server

## License
Apache License, Version 2.0


