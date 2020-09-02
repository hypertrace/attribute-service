# Attribute service

Attribute service provides CRUD operations for the attributes in Hypertrace.

It fetches all attributes relevant to the scope of what is being shown on UI. The concept of Attributes helps build a generic framework of communication between the UI and backend.

## What are Attributes?
An attribute represents a piece of data that's present in Hypertrace that's available for querying. It is a highly generic building block of what queries/show/display/represent in the UI. Attribute metadata gives the type of the data, kind of aggregations allowed on it, services which serve that attribute, etc.

Ex.
```
key: apiName,
value_kind: TYPE_STRING,
groupable : true,
display_name: Endpoint Name,
scope: API_TRACE,
sources: [QS],
type: ATTRIBUTE
```

You can check out structure of attribute [here](https://github.com/hypertrace/attribute-service/blob/main/attribute-service-api/src/main/proto/org/hypertrace/core/attribute/service/v1/attribute_metadata.proto).

### How are attributes created?
An initial list of attributes needed by Hypertrace are seeded from `helm/configs` but they can also be dynamically registered and queried using the APIs of AttributeService.

## How do we use Attribute service

Attribute service is a part of query architecture in Hypertrace and here is the use of it in context of its callers: [hypertrace-graphql](https://github.com/hypertrace/hypertrace-graphql) and [gateway-service](https://github.com/hypertrace/gateway-service). 

| ![space-1.jpg](https://hypertrace-docs.s3.amazonaws.com/HT-query-architecture.png) | 
|:--:| 
| *Hypertrace Query Architecture* |

Attribute service fetches all attributes relevant to the scope of what is being shown on UI from Mongo and where it also stores attributes. All of this metadata assists the UI and backend to perform things in a generic fashion, like
- All String attributes in the UI can have a different look at feel and also different operations that can be supported on them(in the explorer).
- The backend also uses part of the information(like the sources) to figure out where to fetch data from i.e. query-service vs entity-service.

## Building locally
The Attribute service uses gradlew to compile/install/distribute. Gradle wrapper is already part of the source code. To build Attribute Service, run:

```
./gradlew clean build dockerBuildImages
```

## Docker Image Source:
- [DockerHub > Attribute service](https://hub.docker.com/r/hypertrace/attribute-service)




