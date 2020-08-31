# Attribute-service

Service to provide CRUD operations for the attributes in Hypertrace.

## What are Attributes?
An attribute represents a piece of data that's present in Hypertrace
that's available for querying. Attribute metadata gives the type of the
data, kind of aggregations allowed on it, services which serve that attribute, etc.
See `attribute-service-api/src/main/proto/org/hypertrace/core/attribute/service/v1/attribute_metadata.proto`
for the structure of an attribute.

### How are attributes created?
An initial list of attributes needed by Hypertrace are seeded from `helm/configs` but they
can also be dynamically registered and queried using the APIs of AttributeService.

## How Hypertrace uses Attribute service?
Below is the high level schematic diagram(of the push based model) of different components involved in attribute service:

| ![space-1.jpg](https://imagizer.imageshack.com/v2/xq90/924/xN2Top.png) | 
|:--:| 
| *Attribute service schematic representation* |



| ![space-1.jpg](https://imagizer.imageshack.com/v2/xq90/924/ankE6z.png) | 
|:--:| 
| *All flows involving Attribute service* |


## Building locally
The Attribute service uses gradlew to compile/install/distribute. Gradle wrapper is already part of the source code. To build Attribute Service, run:

```
./gradlew clean build dockerBuildImages
```

## Docker Image Source:
- [DockerHub > Attribute service](https://hub.docker.com/r/hypertrace/attribute-service)




