package org.hypertrace.core.attribute.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.Iterators;
import com.google.protobuf.ServiceException;
import io.grpc.Context;
import io.grpc.stub.StreamObserver;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import org.hypertrace.core.attribute.service.v1.AggregateFunction;
import org.hypertrace.core.attribute.service.v1.AttributeKind;
import org.hypertrace.core.attribute.service.v1.AttributeMetadata;
import org.hypertrace.core.attribute.service.v1.AttributeMetadataFilter;
import org.hypertrace.core.attribute.service.v1.AttributeScope;
import org.hypertrace.core.attribute.service.v1.AttributeType;
import org.hypertrace.core.attribute.service.v1.Empty;
import org.hypertrace.core.attribute.service.v1.GetAttributesRequest;
import org.hypertrace.core.attribute.service.v1.GetAttributesResponse;
import org.hypertrace.core.documentstore.CloseableIterator;
import org.hypertrace.core.documentstore.Collection;
import org.hypertrace.core.documentstore.Document;
import org.hypertrace.core.documentstore.Filter;
import org.hypertrace.core.documentstore.Query;
import org.hypertrace.core.grpcutils.context.RequestContext;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

public class AttributeServiceImplTest {

  private static final AttributeMetadata MOCK_EVENT_NAME_ATTRIBUTE =
      AttributeMetadata.newBuilder()
          .setFqn("EVENT.name")
          .setId("EVENT.name")
          .setKey("name")
          .setScope(AttributeScope.EVENT)
          .setScopeString(AttributeScope.EVENT.name())
          .setDisplayName("EVENT name")
          .setValueKind(AttributeKind.TYPE_STRING)
          .setGroupable(true)
          .setType(AttributeType.ATTRIBUTE)
          // Add default aggregations. See SupportedAggregationsDecorator
          .addAllSupportedAggregations(List.of(AggregateFunction.DISTINCT_COUNT))
          .build();
  private static final AttributeMetadata MOCK_EVENT_TYPE_ATTRIBUTE =
      AttributeMetadata.newBuilder()
          .setFqn("EVENT.type")
          .setId("EVENT.type")
          .setKey("type")
          .setScope(AttributeScope.EVENT)
          .setScopeString(AttributeScope.EVENT.name())
          .setDisplayName("EVENT type")
          .setValueKind(AttributeKind.TYPE_STRING)
          .setGroupable(true)
          .setType(AttributeType.ATTRIBUTE)
          // Add default aggregations. See SupportedAggregationsDecorator
          .addAllSupportedAggregations(List.of(AggregateFunction.DISTINCT_COUNT))
          .setCustom(true)
          .build();
  private static final AttributeMetadata MOCK_EVENT_DURATION_ATTRIBUTE =
      AttributeMetadata.newBuilder()
          .setFqn("EVENT.duration")
          .setId("EVENT.duration")
          .setKey("duration")
          .setScope(AttributeScope.EVENT)
          .setScopeString(AttributeScope.EVENT.name())
          .setDisplayName("EVENT duration")
          .setGroupable(false)
          .setValueKind(AttributeKind.TYPE_INT64)
          .setType(AttributeType.METRIC)
          // Add default aggregations. See SupportedAggregationsDecorator
          .addAllSupportedAggregations(
              List.of(
                  AggregateFunction.SUM,
                  AggregateFunction.MIN,
                  AggregateFunction.MAX,
                  AggregateFunction.AVG,
                  AggregateFunction.AVGRATE,
                  AggregateFunction.PERCENTILE))
          .build();

  @Test
  public void testFindAll() {
    RequestContext requestContext = mock(RequestContext.class);
    when(requestContext.getTenantId()).thenReturn(Optional.of("test-tenant-id"));
    Context ctx = Context.current().withValue(RequestContext.CURRENT, requestContext);

    Context previous = ctx.attach();
    try {
      Collection collection =
          mockCollectionReturningDocuments(
              createMockDocument(
                  "__root",
                  "name",
                  AttributeScope.EVENT,
                  AttributeType.ATTRIBUTE,
                  AttributeKind.TYPE_STRING),
              createMockDocument(
                  "__root",
                  "duration",
                  AttributeScope.EVENT,
                  AttributeType.METRIC,
                  AttributeKind.TYPE_INT64));
      StreamObserver<AttributeMetadata> responseObserver = mock(StreamObserver.class);

      AttributeServiceImpl attributeService = new AttributeServiceImpl(collection);

      attributeService.findAll(Empty.newBuilder().build(), responseObserver);

      ArgumentCaptor<Query> queryCaptor = ArgumentCaptor.forClass(Query.class);
      verify(collection, times(1)).search(queryCaptor.capture());

      Filter filter = queryCaptor.getValue().getFilter();
      Assertions.assertEquals("tenant_id", filter.getFieldName());
      Assertions.assertEquals(List.of("__root", "test-tenant-id"), filter.getValue());

      ArgumentCaptor<AttributeMetadata> attributeMetadataArgumentCaptor =
          ArgumentCaptor.forClass(AttributeMetadata.class);
      verify(responseObserver, times(2)).onNext(attributeMetadataArgumentCaptor.capture());

      List<AttributeMetadata> attributeMetadataList =
          attributeMetadataArgumentCaptor.getAllValues();
      Assertions.assertEquals(2, attributeMetadataList.size());

      Assertions.assertEquals(MOCK_EVENT_NAME_ATTRIBUTE, attributeMetadataList.get(0));
      Assertions.assertEquals(MOCK_EVENT_DURATION_ATTRIBUTE, attributeMetadataList.get(1));

      verify(responseObserver, times(1)).onCompleted();
      verify(responseObserver, never()).onError(any(Throwable.class));
    } finally {
      ctx.detach(previous);
    }
  }

  @Test
  public void testFindAllNoTenantId() {
    RequestContext requestContext = mock(RequestContext.class);
    when(requestContext.getTenantId()).thenReturn(Optional.empty());
    Context ctx = Context.current().withValue(RequestContext.CURRENT, requestContext);

    Context previous = ctx.attach();
    try {
      Collection collection = mock(Collection.class);
      StreamObserver<AttributeMetadata> responseObserver = mock(StreamObserver.class);

      AttributeServiceImpl attributeService = new AttributeServiceImpl(collection);

      attributeService.findAll(Empty.newBuilder().build(), responseObserver);

      verify(collection, never()).search(any(Query.class));
      verify(responseObserver, never()).onNext(any(AttributeMetadata.class));
      verify(responseObserver, never()).onCompleted();
      verify(responseObserver, times(1)).onError(any(ServiceException.class));
    } finally {
      ctx.detach(previous);
    }
  }

  @Test
  public void testFindAttributesWithInternalFlag() {
    RequestContext requestContext = mock(RequestContext.class);
    when(requestContext.getTenantId()).thenReturn(Optional.of("test-tenant-id"));
    Context ctx = Context.current().withValue(RequestContext.CURRENT, requestContext);

    Context previous = ctx.attach();
    try {
      Collection collection =
          mockCollectionReturningDocuments(
              createMockDocument(
                  "__root",
                  "name",
                  AttributeScope.EVENT,
                  AttributeType.ATTRIBUTE,
                  AttributeKind.TYPE_STRING),
              createMockDocument(
                  "__root",
                  "duration",
                  AttributeScope.EVENT,
                  AttributeType.METRIC,
                  AttributeKind.TYPE_INT64));
      StreamObserver<AttributeMetadata> responseObserver = mock(StreamObserver.class);
      AttributeServiceImpl attributeService = new AttributeServiceImpl(collection);

      List<String> fqnList = List.of("EVENT.name", "EVENT.id");
      List<String> keyList = List.of("name", "startTime", "duration");

      AttributeMetadataFilter attributeMetadataFilter =
          AttributeMetadataFilter.newBuilder()
              .addAllFqn(fqnList)
              .addAllKey(keyList)
              .addAllScope(
                  List.of(AttributeScope.TRACE, AttributeScope.EVENT, AttributeScope.BACKEND))
              .addScopeString("OTHER")
              .setInternal(true)
              .build();

      List<String> allScopes =
          List.of(
              "OTHER",
              AttributeScope.TRACE.name(),
              AttributeScope.EVENT.name(),
              AttributeScope.BACKEND.name());

      attributeService.findAttributes(attributeMetadataFilter, responseObserver);

      ArgumentCaptor<Query> queryCaptor = ArgumentCaptor.forClass(Query.class);
      verify(collection, times(1)).search(queryCaptor.capture());

      Filter filter = queryCaptor.getValue().getFilter();
      // The structure of the filters is an and(^) filter chain that looks like this:
      // ((((tenant_id ^ fqn) ^ (scope_string | scope)) ^ key) ^ internal)
      Assertions.assertEquals(Filter.Op.AND, filter.getOp());
      Assertions.assertEquals(Filter.Op.EQ, filter.getChildFilters()[1].getOp());
      Assertions.assertEquals("internal", filter.getChildFilters()[1].getFieldName());
      Assertions.assertEquals(true, filter.getChildFilters()[1].getValue());

      Filter innerFilter = filter.getChildFilters()[0];
      Assertions.assertEquals("key", innerFilter.getChildFilters()[1].getFieldName());
      Assertions.assertEquals(keyList, innerFilter.getChildFilters()[1].getValue());

      Assertions.assertEquals(Filter.Op.AND, innerFilter.getChildFilters()[0].getOp());
      Filter scopeFilter = innerFilter.getChildFilters()[0].getChildFilters()[1];
      Assertions.assertEquals(Filter.Op.OR, scopeFilter.getOp());

      Assertions.assertEquals(Filter.Op.IN, scopeFilter.getChildFilters()[0].getOp());
      Assertions.assertEquals("scope_string", scopeFilter.getChildFilters()[0].getFieldName());
      Assertions.assertEquals(allScopes, scopeFilter.getChildFilters()[0].getValue());

      Assertions.assertEquals(Filter.Op.IN, scopeFilter.getChildFilters()[1].getOp());
      Assertions.assertEquals("scope", scopeFilter.getChildFilters()[1].getFieldName());
      Assertions.assertEquals(allScopes, scopeFilter.getChildFilters()[1].getValue());

      Assertions.assertEquals(
          Filter.Op.AND, innerFilter.getChildFilters()[0].getChildFilters()[0].getOp());
      Assertions.assertEquals(
          Filter.Op.IN,
          innerFilter.getChildFilters()[0].getChildFilters()[0].getChildFilters()[1].getOp());
      Assertions.assertEquals(
          "fqn",
          innerFilter.getChildFilters()[0].getChildFilters()[0].getChildFilters()[1]
              .getFieldName());
      Assertions.assertEquals(
          fqnList,
          innerFilter.getChildFilters()[0].getChildFilters()[0].getChildFilters()[1].getValue());

      Assertions.assertEquals(
          Filter.Op.IN,
          innerFilter.getChildFilters()[0].getChildFilters()[0].getChildFilters()[0].getOp());
      Assertions.assertEquals(
          List.of("__root", "test-tenant-id"),
          innerFilter.getChildFilters()[0].getChildFilters()[0].getChildFilters()[0].getValue());

      ArgumentCaptor<AttributeMetadata> attributeMetadataArgumentCaptor =
          ArgumentCaptor.forClass(AttributeMetadata.class);
      verify(responseObserver, times(2)).onNext(attributeMetadataArgumentCaptor.capture());

      List<AttributeMetadata> attributeMetadataList =
          attributeMetadataArgumentCaptor.getAllValues();
      Assertions.assertEquals(2, attributeMetadataList.size());

      Assertions.assertEquals(MOCK_EVENT_NAME_ATTRIBUTE, attributeMetadataList.get(0));
      Assertions.assertEquals(MOCK_EVENT_DURATION_ATTRIBUTE, attributeMetadataList.get(1));

      verify(responseObserver, times(1)).onCompleted();
      verify(responseObserver, never()).onError(any(Throwable.class));
    } finally {
      ctx.detach(previous);
    }
  }

  @Test
  public void testFindAttributesWithNoInternalFlag() {
    RequestContext requestContext = mock(RequestContext.class);
    when(requestContext.getTenantId()).thenReturn(Optional.of("test-tenant-id"));
    Context ctx = Context.current().withValue(RequestContext.CURRENT, requestContext);

    Context previous = ctx.attach();
    try {
      Collection collection =
          mockCollectionReturningDocuments(
              createMockDocument(
                  "__root",
                  "name",
                  AttributeScope.EVENT,
                  AttributeType.ATTRIBUTE,
                  AttributeKind.TYPE_STRING),
              createMockDocument(
                  "__root",
                  "duration",
                  AttributeScope.EVENT,
                  AttributeType.METRIC,
                  AttributeKind.TYPE_INT64));
      StreamObserver<AttributeMetadata> responseObserver = mock(StreamObserver.class);
      AttributeServiceImpl attributeService = new AttributeServiceImpl(collection);

      List<String> fqnList = List.of("EVENT.name", "EVENT.id");
      List<String> keyList = List.of("name", "startTime", "duration");

      AttributeMetadataFilter attributeMetadataFilter =
          AttributeMetadataFilter.newBuilder()
              .addAllFqn(fqnList)
              .addAllKey(keyList)
              .addAllScope(
                  List.of(AttributeScope.TRACE, AttributeScope.EVENT, AttributeScope.BACKEND))
              .addScopeString("OTHER")
              .build();

      List<String> allScopes =
          List.of(
              "OTHER",
              AttributeScope.TRACE.name(),
              AttributeScope.EVENT.name(),
              AttributeScope.BACKEND.name());

      attributeService.findAttributes(attributeMetadataFilter, responseObserver);

      ArgumentCaptor<Query> queryCaptor = ArgumentCaptor.forClass(Query.class);
      verify(collection, times(1)).search(queryCaptor.capture());

      Filter filter = queryCaptor.getValue().getFilter();
      // The structure of the filters is an and(^) filter chain that looks like this:
      // (((tenant_id ^ fqn) ^ (scope_string | scope)) ^ key)
      Assertions.assertEquals(Filter.Op.AND, filter.getOp());
      Assertions.assertEquals(Filter.Op.IN, filter.getChildFilters()[1].getOp());
      Assertions.assertEquals("key", filter.getChildFilters()[1].getFieldName());
      Assertions.assertEquals(keyList, filter.getChildFilters()[1].getValue());

      Assertions.assertEquals(Filter.Op.AND, filter.getChildFilters()[0].getOp());
      Filter scopeFilter = filter.getChildFilters()[0].getChildFilters()[1];
      Assertions.assertEquals(Filter.Op.OR, scopeFilter.getOp());

      Assertions.assertEquals(Filter.Op.IN, scopeFilter.getChildFilters()[0].getOp());
      Assertions.assertEquals("scope_string", scopeFilter.getChildFilters()[0].getFieldName());
      Assertions.assertEquals(allScopes, scopeFilter.getChildFilters()[0].getValue());
      Assertions.assertEquals(Filter.Op.IN, scopeFilter.getChildFilters()[1].getOp());
      Assertions.assertEquals("scope", scopeFilter.getChildFilters()[1].getFieldName());
      Assertions.assertEquals(allScopes, scopeFilter.getChildFilters()[1].getValue());

      Assertions.assertEquals(
          Filter.Op.AND, filter.getChildFilters()[0].getChildFilters()[0].getOp());
      Assertions.assertEquals(
          Filter.Op.IN,
          filter.getChildFilters()[0].getChildFilters()[0].getChildFilters()[1].getOp());
      Assertions.assertEquals(
          Filter.Op.IN,
          filter.getChildFilters()[0].getChildFilters()[0].getChildFilters()[0].getOp());
      Assertions.assertEquals(
          List.of("__root", "test-tenant-id"),
          filter.getChildFilters()[0].getChildFilters()[0].getChildFilters()[0].getValue());

      ArgumentCaptor<AttributeMetadata> attributeMetadataArgumentCaptor =
          ArgumentCaptor.forClass(AttributeMetadata.class);
      verify(responseObserver, times(2)).onNext(attributeMetadataArgumentCaptor.capture());
      List<AttributeMetadata> attributeMetadataList =
          attributeMetadataArgumentCaptor.getAllValues();
      Assertions.assertEquals(2, attributeMetadataList.size());
      Assertions.assertEquals(MOCK_EVENT_NAME_ATTRIBUTE, attributeMetadataList.get(0));
      Assertions.assertEquals(MOCK_EVENT_DURATION_ATTRIBUTE, attributeMetadataList.get(1));
      verify(responseObserver, times(1)).onCompleted();
      verify(responseObserver, never()).onError(any(Throwable.class));
    } finally {
      ctx.detach(previous);
    }
  }

  @Test
  public void testGetAttributes() {
    RequestContext requestContext = mock(RequestContext.class);
    when(requestContext.getTenantId()).thenReturn(Optional.of("test-tenant-id"));
    Context.current()
        .withValue(RequestContext.CURRENT, requestContext)
        .run(
            () -> {
              AttributeServiceImpl attributeService =
                  new AttributeServiceImpl(
                      mockCollectionReturningDocuments(
                          createMockDocument(
                              "__root",
                              "name",
                              AttributeScope.EVENT,
                              AttributeType.ATTRIBUTE,
                              AttributeKind.TYPE_STRING),
                          createMockDocument(
                              "__root",
                              "duration",
                              AttributeScope.EVENT,
                              AttributeType.METRIC,
                              AttributeKind.TYPE_INT64)));

              StreamObserver<GetAttributesResponse> mockObserver = mock(StreamObserver.class);
              attributeService.getAttributes(
                  GetAttributesRequest.newBuilder()
                      .setFilter(
                          AttributeMetadataFilter.newBuilder()
                              .addKey(MOCK_EVENT_NAME_ATTRIBUTE.getKey())
                              .addKey(MOCK_EVENT_DURATION_ATTRIBUTE.getKey()))
                      .build(),
                  mockObserver);

              verify(mockObserver, times(1))
                  .onNext(
                      GetAttributesResponse.newBuilder()
                          .addAttributes(MOCK_EVENT_NAME_ATTRIBUTE)
                          .addAttributes(MOCK_EVENT_DURATION_ATTRIBUTE)
                          .build());

              verify(mockObserver, times(1)).onCompleted();
              verify(mockObserver, never()).onError(any());
            });
  }

  @Test
  public void testGetCustomAttributes() {
    RequestContext requestContext = mock(RequestContext.class);
    final String TEST_TENANT_ID = "test-tenant-id";
    when(requestContext.getTenantId()).thenReturn(Optional.of(TEST_TENANT_ID));
    Context.current()
        .withValue(RequestContext.CURRENT, requestContext)
        .run(
            () -> {
              AttributeServiceImpl attributeService =
                  new AttributeServiceImpl(
                      mockCollectionReturningDocuments(
                          createMockDocument(
                              TEST_TENANT_ID,
                              "type",
                              AttributeScope.EVENT,
                              AttributeType.ATTRIBUTE,
                              AttributeKind.TYPE_STRING)));

              StreamObserver<GetAttributesResponse> mockObserver = mock(StreamObserver.class);
              attributeService.getAttributes(
                  GetAttributesRequest.newBuilder()
                      .setFilter(AttributeMetadataFilter.newBuilder().setCustom(true))
                      .build(),
                  mockObserver);

              verify(mockObserver, times(1))
                  .onNext(
                      GetAttributesResponse.newBuilder()
                          .addAttributes(MOCK_EVENT_TYPE_ATTRIBUTE)
                          .build());

              verify(mockObserver, times(1)).onCompleted();
              verify(mockObserver, never()).onError(any());
            });
  }

  @Test
  public void testFindAttributesNoTenantId() {
    RequestContext requestContext = mock(RequestContext.class);
    when(requestContext.getTenantId()).thenReturn(Optional.empty());
    Context ctx = Context.current().withValue(RequestContext.CURRENT, requestContext);

    Context previous = ctx.attach();
    try {
      Collection collection = mock(Collection.class);
      StreamObserver<AttributeMetadata> responseObserver = mock(StreamObserver.class);

      AttributeServiceImpl attributeService = new AttributeServiceImpl(collection);

      attributeService.findAttributes(
          AttributeMetadataFilter.newBuilder().build(), responseObserver);

      verify(collection, never()).search(any(Query.class));
      verify(responseObserver, never()).onNext(any(AttributeMetadata.class));
      verify(responseObserver, never()).onCompleted();
      verify(responseObserver, times(1)).onError(any(ServiceException.class));
    } finally {
      ctx.detach(previous);
    }
  }

  private Collection mockCollectionReturningDocuments(Document... documents) {
    Collection collection = mock(Collection.class);
    when(collection.search(any(Query.class)))
        .thenReturn(convertToCloseableIterator(Iterators.forArray(documents)));
    return collection;
  }

  private Document createMockDocument(
      String tenantId, String key, AttributeScope scope, AttributeType type, AttributeKind kind) {
    Document document = mock(Document.class);

    StringBuilder sb = new StringBuilder();
    sb.append("{\"fqn\":\"")
        .append(scope.name())
        .append(".")
        .append(key)
        .append("\",\"scope\":\"")
        .append(scope.name())
        .append("\",\"key\":\"")
        .append(key)
        .append("\",\"type\":\"")
        .append(type.name())
        .append("\",\"value_kind\":\"")
        .append(kind.name())
        .append("\",\"tenant_id\":\"")
        .append(tenantId)
        .append("\",\"display_name\":\"")
        .append(scope.name())
        .append(" ")
        .append(key)
        .append("\"}");

    when(document.toJson()).thenReturn(sb.toString());
    return document;
  }

  private <T> CloseableIterator<T> convertToCloseableIterator(Iterator<T> iterator) {
    return new CloseableIterator<>() {
      @Override
      public void close() {}

      @Override
      public boolean hasNext() {
        return iterator.hasNext();
      }

      @Override
      public T next() {
        return iterator.next();
      }
    };
  }
}
