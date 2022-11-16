package org.hypertrace.core.attribute.service;

import static java.util.Collections.emptyList;
import static org.hypertrace.core.grpcutils.client.GrpcClientRequestContextUtil.executeInTenantContext;
import static org.hypertrace.core.grpcutils.client.GrpcClientRequestContextUtil.executeWithHeadersContext;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.google.common.collect.ImmutableList;
import io.grpc.Channel;
import io.grpc.ManagedChannelBuilder;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.hypertrace.core.attribute.service.v1.AggregateFunction;
import org.hypertrace.core.attribute.service.v1.AttributeCreateRequest;
import org.hypertrace.core.attribute.service.v1.AttributeDefinition;
import org.hypertrace.core.attribute.service.v1.AttributeKind;
import org.hypertrace.core.attribute.service.v1.AttributeMetadata;
import org.hypertrace.core.attribute.service.v1.AttributeMetadataFilter;
import org.hypertrace.core.attribute.service.v1.AttributeScope;
import org.hypertrace.core.attribute.service.v1.AttributeServiceGrpc;
import org.hypertrace.core.attribute.service.v1.AttributeServiceGrpc.AttributeServiceBlockingStub;
import org.hypertrace.core.attribute.service.v1.AttributeSource;
import org.hypertrace.core.attribute.service.v1.AttributeSourceMetadata;
import org.hypertrace.core.attribute.service.v1.AttributeSourceMetadataDeleteRequest;
import org.hypertrace.core.attribute.service.v1.AttributeSourceMetadataUpdateRequest;
import org.hypertrace.core.attribute.service.v1.AttributeType;
import org.hypertrace.core.attribute.service.v1.GetAttributesRequest;
import org.hypertrace.core.grpcutils.client.RequestContextClientCallCredsProviderFactory;
import org.hypertrace.core.serviceframework.IntegrationTestServerUtil;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Integration test for AttributeService */
public class AttributeServiceTest {
  private static final String SPAN_ID_ATTR = "EVENT.id";
  private static final String SPAN_NAME_ATTR = "EVENT.name";
  private static final String TRACE_METRIC_ATTR = "TRACE.duration";

  private static final String TEST_TENANT_ID = "test-tenant-id";
  private static final String ROOT_TENANT_ID = "__root";

  private final AttributeMetadata spanNameAttr =
      AttributeMetadata.newBuilder()
          .setId(SPAN_NAME_ATTR)
          .setFqn(SPAN_NAME_ATTR)
          .setScopeString(AttributeScope.EVENT.name())
          .setKey("name")
          .setType(AttributeType.ATTRIBUTE)
          .setValueKind(AttributeKind.TYPE_STRING)
          .setDisplayName("Span Name")
          .addSources(AttributeSource.QS)
          .build();
  private final AttributeMetadata spanIdAttr =
      AttributeMetadata.newBuilder()
          .setId(SPAN_ID_ATTR)
          .setFqn(SPAN_ID_ATTR)
          .setScopeString(AttributeScope.EVENT.name())
          .setKey("id")
          .setType(AttributeType.ATTRIBUTE)
          .setValueKind(AttributeKind.TYPE_STRING)
          .setDisplayName("Span Id")
          .addSources(AttributeSource.QS)
          .build();

  private final AttributeMetadata traceDurationMillis =
      AttributeMetadata.newBuilder()
          .setId(TRACE_METRIC_ATTR)
          .setFqn(TRACE_METRIC_ATTR)
          .setScopeString(AttributeScope.TRACE.name())
          .setKey("duration")
          .setType(AttributeType.METRIC)
          .setValueKind(AttributeKind.TYPE_INT64)
          .setDisplayName("Duration")
          .addSources(AttributeSource.QS)
          .build();

  private final Map<String, String> requestHeaders = Map.of("x-tenant-id", TEST_TENANT_ID);
  private final Map<String, String> systemRequestHeaders = Map.of("x-tenant-id", ROOT_TENANT_ID);

  private static AttributeServiceBlockingStub stub;

  @BeforeAll
  public static void setup() {
    System.out.println("Testing the Attribute E2E Test");
    IntegrationTestServerUtil.startServices(new String[] {"attribute-service"});
    Channel channel = ManagedChannelBuilder.forAddress("localhost", 9012).usePlaintext().build();
    stub =
        AttributeServiceGrpc.newBlockingStub(channel)
            .withCallCredentials(
                RequestContextClientCallCredsProviderFactory.getClientCallCredsProvider().get());
  }

  @AfterAll
  public static void teardown() {
    IntegrationTestServerUtil.shutdownServices();
  }

  @BeforeEach
  public void setupMethod() {
    executeWithHeadersContext(
        requestHeaders, () -> stub.delete(AttributeMetadataFilter.getDefaultInstance()));
  }

  @Test
  public void testCreateInvalidUseHeaders() {
    Assertions.assertThrows(RuntimeException.class, () -> testCreateInvalid(true));
  }

  @Test
  public void testCreateInvalidUseTenantId() {
    Assertions.assertThrows(RuntimeException.class, () -> testCreateInvalid(false));
  }

  private void testCreateInvalid(boolean useRequestHeaders) {
    AttributeCreateRequest attributeCreateRequest =
        AttributeCreateRequest.newBuilder()
            .addAttributes(
                AttributeMetadata.newBuilder()
                    .setId(SPAN_NAME_ATTR)
                    .setFqn(SPAN_NAME_ATTR)
                    .setKey("name")
                    .setType(AttributeType.ATTRIBUTE)
                    .setValueKind(AttributeKind.TYPE_STRING)
                    .setDisplayName("Span Name")
                    .addSources(AttributeSource.QS)
                    .build())
            .build();
    if (useRequestHeaders) {
      executeWithHeadersContext(requestHeaders, () -> stub.create(attributeCreateRequest));
    } else {
      executeInTenantContext(TEST_TENANT_ID, () -> stub.create(attributeCreateRequest));
    }
  }

  @Test
  public void testCheckValidAttributeMetadata() {
    AttributeMetadata expectedAttributeMetadata =
        AttributeMetadata.newBuilder()
            .setFqn("name-1")
            .setValueKind(AttributeKind.TYPE_STRING)
            .setKey("key-1")
            .setDisplayName("displayname-1")
            .setScope(AttributeScope.EVENT)
            .setMaterialized(false)
            .setUnit("unit-1")
            .setType(AttributeType.ATTRIBUTE)
            .addAllLabels(List.of("label-1", "label-2"))
            .addAllSupportedAggregations(List.of(AggregateFunction.SUM, AggregateFunction.AVG))
            .setOnlyAggregationsAllowed(true)
            .addSources(AttributeSource.EDS)
            .setId("EVENT.key-1")
            .setGroupable(true)
            .setDefinition(AttributeDefinition.newBuilder().setSourcePath("sourcepath-1"))
            .setScopeString(AttributeScope.EVENT.name())
            .setCustom(true)
            .build();

    AttributeCreateRequest request =
        AttributeCreateRequest.newBuilder().addAttributes(expectedAttributeMetadata).build();
    executeWithHeadersContext(requestHeaders, () -> stub.create(request));

    List<AttributeMetadata> attributeMetadataList =
        executeWithHeadersContext(
                requestHeaders,
                () ->
                    stub.getAttributes(
                        GetAttributesRequest.newBuilder()
                            .setFilter(AttributeMetadataFilter.getDefaultInstance())
                            .build()))
            .getAttributesList();
    assertEquals(List.of(expectedAttributeMetadata), attributeMetadataList);
  }

  @Test
  public void testGetAllAttributeMetadata() {
    AttributeMetadata expectedAttributeMetadata1 =
        AttributeMetadata.newBuilder()
            .setFqn("name-1")
            .setValueKind(AttributeKind.TYPE_STRING)
            .setKey("key-1")
            .setDisplayName("displayname-1")
            .setScope(AttributeScope.EVENT)
            .setMaterialized(false)
            .setUnit("unit-1")
            .setType(AttributeType.ATTRIBUTE)
            .addAllLabels(List.of("label-1", "label-2"))
            .addAllSupportedAggregations(List.of(AggregateFunction.SUM, AggregateFunction.AVG))
            .setOnlyAggregationsAllowed(true)
            .addSources(AttributeSource.EDS)
            .setId("EVENT.key-1")
            .setGroupable(true)
            .setDefinition(AttributeDefinition.newBuilder().setSourcePath("sourcepath-1"))
            .setScopeString(AttributeScope.EVENT.name())
            .setCustom(true)
            .build();

    AttributeMetadata expectedAttributeMetadata2 =
        AttributeMetadata.newBuilder()
            .setFqn("name-2")
            .setValueKind(AttributeKind.TYPE_STRING)
            .setKey("key-2")
            .setDisplayName("displayname-2")
            .setScope(AttributeScope.EVENT)
            .setMaterialized(false)
            .setUnit("unit-2")
            .setType(AttributeType.ATTRIBUTE)
            .addAllLabels(List.of("label-3", "label-4"))
            .addAllSupportedAggregations(List.of(AggregateFunction.SUM, AggregateFunction.AVG))
            .setOnlyAggregationsAllowed(true)
            .addSources(AttributeSource.EDS)
            .setId("EVENT.key-2")
            .setGroupable(true)
            .setDefinition(AttributeDefinition.newBuilder().setSourcePath("sourcepath-2"))
            .setScopeString(AttributeScope.EVENT.name())
            .build();

    AttributeCreateRequest request1 =
        AttributeCreateRequest.newBuilder().addAttributes(expectedAttributeMetadata1).build();
    executeWithHeadersContext(requestHeaders, () -> stub.create(request1));

    AttributeCreateRequest request2 =
        AttributeCreateRequest.newBuilder().addAttributes(expectedAttributeMetadata2).build();
    executeWithHeadersContext(systemRequestHeaders, () -> stub.create(request2));

    List<AttributeMetadata> attributeMetadataList =
        executeWithHeadersContext(
                requestHeaders, () -> stub.getAttributes(GetAttributesRequest.newBuilder().build()))
            .getAttributesList();
    assertEquals(
        List.of(expectedAttributeMetadata1, expectedAttributeMetadata2), attributeMetadataList);
  }

  @Test
  public void testGetCustomAttributeMetadata() {
    AttributeMetadata expectedAttributeMetadata1 =
        AttributeMetadata.newBuilder()
            .setFqn("name-1")
            .setValueKind(AttributeKind.TYPE_STRING)
            .setKey("key-1")
            .setDisplayName("displayname-1")
            .setScope(AttributeScope.EVENT)
            .setMaterialized(false)
            .setUnit("unit-1")
            .setType(AttributeType.ATTRIBUTE)
            .addAllLabels(List.of("label-1", "label-2"))
            .addAllSupportedAggregations(List.of(AggregateFunction.SUM, AggregateFunction.AVG))
            .setOnlyAggregationsAllowed(true)
            .addSources(AttributeSource.EDS)
            .setId("EVENT.key-1")
            .setGroupable(true)
            .setDefinition(AttributeDefinition.newBuilder().setSourcePath("sourcepath-1"))
            .setScopeString(AttributeScope.EVENT.name())
            .setCustom(true)
            .build();

    AttributeMetadata expectedAttributeMetadata2 =
        AttributeMetadata.newBuilder()
            .setFqn("name-2")
            .setValueKind(AttributeKind.TYPE_STRING)
            .setKey("key-2")
            .setDisplayName("displayname-2")
            .setScope(AttributeScope.EVENT)
            .setMaterialized(false)
            .setUnit("unit-2")
            .setType(AttributeType.ATTRIBUTE)
            .addAllLabels(List.of("label-3", "label-4"))
            .addAllSupportedAggregations(List.of(AggregateFunction.SUM, AggregateFunction.AVG))
            .setOnlyAggregationsAllowed(true)
            .addSources(AttributeSource.EDS)
            .setId("EVENT.key-2")
            .setGroupable(true)
            .setDefinition(AttributeDefinition.newBuilder().setSourcePath("sourcepath-2"))
            .setScopeString(AttributeScope.EVENT.name())
            .build();

    AttributeCreateRequest request1 =
        AttributeCreateRequest.newBuilder().addAttributes(expectedAttributeMetadata1).build();
    executeWithHeadersContext(requestHeaders, () -> stub.create(request1));

    AttributeCreateRequest request2 =
        AttributeCreateRequest.newBuilder().addAttributes(expectedAttributeMetadata2).build();
    executeWithHeadersContext(systemRequestHeaders, () -> stub.create(request2));

    List<AttributeMetadata> attributeMetadataList =
        executeWithHeadersContext(
                requestHeaders,
                () ->
                    stub.getAttributes(
                        GetAttributesRequest.newBuilder()
                            .setFilter(AttributeMetadataFilter.newBuilder().setCustom(true))
                            .build()))
            .getAttributesList();
    assertEquals(List.of(expectedAttributeMetadata1), attributeMetadataList);
  }

  @Test
  public void testGetSystemAttributeMetadata() {
    AttributeMetadata expectedAttributeMetadata1 =
        AttributeMetadata.newBuilder()
            .setFqn("name-1")
            .setValueKind(AttributeKind.TYPE_STRING)
            .setKey("key-1")
            .setDisplayName("displayname-1")
            .setScope(AttributeScope.EVENT)
            .setMaterialized(false)
            .setUnit("unit-1")
            .setType(AttributeType.ATTRIBUTE)
            .addAllLabels(List.of("label-1", "label-2"))
            .addAllSupportedAggregations(List.of(AggregateFunction.SUM, AggregateFunction.AVG))
            .setOnlyAggregationsAllowed(true)
            .addSources(AttributeSource.EDS)
            .setId("EVENT.key-1")
            .setGroupable(true)
            .setDefinition(AttributeDefinition.newBuilder().setSourcePath("sourcepath-1"))
            .setScopeString(AttributeScope.EVENT.name())
            .setCustom(true)
            .build();

    AttributeMetadata expectedAttributeMetadata2 =
        AttributeMetadata.newBuilder()
            .setFqn("name-2")
            .setValueKind(AttributeKind.TYPE_STRING)
            .setKey("key-2")
            .setDisplayName("displayname-2")
            .setScope(AttributeScope.EVENT)
            .setMaterialized(false)
            .setUnit("unit-2")
            .setType(AttributeType.ATTRIBUTE)
            .addAllLabels(List.of("label-3", "label-4"))
            .addAllSupportedAggregations(List.of(AggregateFunction.SUM, AggregateFunction.AVG))
            .setOnlyAggregationsAllowed(true)
            .addSources(AttributeSource.EDS)
            .setId("EVENT.key-2")
            .setGroupable(true)
            .setDefinition(AttributeDefinition.newBuilder().setSourcePath("sourcepath-2"))
            .setScopeString(AttributeScope.EVENT.name())
            .build();

    AttributeCreateRequest request1 =
        AttributeCreateRequest.newBuilder().addAttributes(expectedAttributeMetadata1).build();
    executeWithHeadersContext(requestHeaders, () -> stub.create(request1));

    AttributeCreateRequest request2 =
        AttributeCreateRequest.newBuilder().addAttributes(expectedAttributeMetadata2).build();
    executeWithHeadersContext(systemRequestHeaders, () -> stub.create(request2));

    List<AttributeMetadata> attributeMetadataList =
        executeWithHeadersContext(
                requestHeaders,
                () ->
                    stub.getAttributes(
                        GetAttributesRequest.newBuilder()
                            .setFilter(AttributeMetadataFilter.newBuilder().setCustom(false))
                            .build()))
            .getAttributesList();
    assertEquals(List.of(expectedAttributeMetadata2), attributeMetadataList);
  }

  @Test
  public void testCreateCallWithHeadersWithTenantIdHeader() {
    AttributeCreateRequest attributeCreateRequest =
        AttributeCreateRequest.newBuilder()
            .addAttributes(spanNameAttr)
            .addAttributes(spanIdAttr)
            .build();
    executeWithHeadersContext(requestHeaders, () -> stub.create(attributeCreateRequest));

    List<String> attributeMetadataList =
        executeWithHeadersContext(
                requestHeaders,
                () ->
                    stub.getAttributes(
                        GetAttributesRequest.newBuilder()
                            .setFilter(AttributeMetadataFilter.getDefaultInstance())
                            .build()))
            .getAttributesList()
            .stream()
            .map(AttributeMetadata::getId)
            .sorted()
            .collect(Collectors.toList());
    assertEquals(attributeMetadataList, List.of(spanIdAttr.getId(), spanNameAttr.getId()));
  }

  @Test
  public void testCreateCallWithHeadersWithNoTenantIdHeader_shouldThrowException() {
    Assertions.assertThrows(
        RuntimeException.class,
        () -> {
          Map<String, String> headers = Map.of("a1", "v1");
          AttributeCreateRequest attributeCreateRequest =
              AttributeCreateRequest.newBuilder()
                  .addAttributes(spanNameAttr)
                  .addAttributes(spanIdAttr)
                  .build();
          executeWithHeadersContext(headers, () -> stub.create(attributeCreateRequest));
        });
  }

  @Test
  public void testUpdateAndDeleteSourceMetadataForAttributeUseHeaders() {
    testUpdateAndDeleteSourceMetadataForAttribute(true);
  }

  @Test
  public void testUpdateAndDeleteSourceMetadataForAttributeUseTenantId() {
    testUpdateAndDeleteSourceMetadataForAttribute(false);
  }

  private void testUpdateAndDeleteSourceMetadataForAttribute(boolean useRequestHeaders) {
    AttributeCreateRequest attributeCreateRequest =
        AttributeCreateRequest.newBuilder()
            .addAttributes(spanNameAttr)
            .addAttributes(spanIdAttr)
            .build();
    if (useRequestHeaders) {
      executeWithHeadersContext(requestHeaders, () -> stub.create(attributeCreateRequest));
    } else {
      executeInTenantContext(TEST_TENANT_ID, () -> stub.create(attributeCreateRequest));
    }

    AttributeSourceMetadataUpdateRequest request =
        AttributeSourceMetadataUpdateRequest.newBuilder()
            .setFqn(SPAN_NAME_ATTR)
            .setSource(AttributeSource.EDS)
            .putAllSourceMetadata(Map.of(SPAN_NAME_ATTR, "attributes.SPAN_NAME"))
            .build();

    List<AttributeMetadata> attributeMetadataList;
    if (useRequestHeaders) {
      executeWithHeadersContext(requestHeaders, () -> stub.updateSourceMetadata(request));
      attributeMetadataList =
          ImmutableList.copyOf(
              executeWithHeadersContext(
                  requestHeaders,
                  () ->
                      stub.getAttributes(
                              GetAttributesRequest.newBuilder()
                                  .setFilter(
                                      AttributeMetadataFilter.newBuilder().addFqn(SPAN_NAME_ATTR))
                                  .build())
                          .getAttributesList()));
    } else {
      executeInTenantContext(TEST_TENANT_ID, () -> stub.updateSourceMetadata(request));
      attributeMetadataList =
          ImmutableList.copyOf(
              executeInTenantContext(
                  TEST_TENANT_ID,
                  () ->
                      stub.getAttributes(
                              GetAttributesRequest.newBuilder()
                                  .setFilter(
                                      AttributeMetadataFilter.newBuilder().addFqn(SPAN_NAME_ATTR))
                                  .build())
                          .getAttributesList()));
    }

    assertEquals(1, attributeMetadataList.size());
    Map<String, AttributeSourceMetadata> attributeSourceMetadataMap =
        attributeMetadataList.get(0).getMetadataMap();
    Assertions.assertFalse(attributeSourceMetadataMap.isEmpty());
    assertEquals(
        request.getSourceMetadataMap(),
        attributeSourceMetadataMap.get(AttributeSource.EDS.name()).getSourceMetadataMap());

    if (useRequestHeaders) {
      executeWithHeadersContext(
          requestHeaders,
          () ->
              stub.deleteSourceMetadata(
                  AttributeSourceMetadataDeleteRequest.newBuilder()
                      .setFqn(SPAN_NAME_ATTR)
                      .setSource(AttributeSource.EDS)
                      .build()));
      attributeMetadataList =
          ImmutableList.copyOf(
              executeWithHeadersContext(
                  requestHeaders,
                  () ->
                      stub.getAttributes(
                              GetAttributesRequest.newBuilder()
                                  .setFilter(
                                      AttributeMetadataFilter.newBuilder().addFqn(SPAN_NAME_ATTR))
                                  .build())
                          .getAttributesList()));
    } else {
      executeInTenantContext(
          TEST_TENANT_ID,
          () ->
              stub.deleteSourceMetadata(
                  AttributeSourceMetadataDeleteRequest.newBuilder()
                      .setFqn(SPAN_NAME_ATTR)
                      .setSource(AttributeSource.EDS)
                      .build()));
      attributeMetadataList =
          ImmutableList.copyOf(
              executeInTenantContext(
                  TEST_TENANT_ID,
                  () ->
                      stub.getAttributes(
                              GetAttributesRequest.newBuilder()
                                  .setFilter(
                                      AttributeMetadataFilter.newBuilder().addFqn(SPAN_NAME_ATTR))
                                  .build())
                          .getAttributesList()));
    }

    assertEquals(1, attributeMetadataList.size());
    attributeSourceMetadataMap = attributeMetadataList.get(0).getMetadataMap();
    Assertions.assertTrue(attributeSourceMetadataMap.isEmpty());
  }

  @Test
  public void testFindByFilterUseHeaders() {
    testFindByFilter(true);
  }

  @Test
  public void testFindByFilterUseTenantId() {
    testFindByFilter(false);
  }

  private void testFindByFilter(boolean useRequestHeaders) {
    createSampleAttributes(useRequestHeaders, spanNameAttr, spanIdAttr, traceDurationMillis);

    {
      List<AttributeMetadata> attributeMetadataList =
          useRequestHeaders
              ? executeWithHeadersContext(
                      requestHeaders,
                      () ->
                          stub.getAttributes(
                              GetAttributesRequest.newBuilder()
                                  .setFilter(AttributeMetadataFilter.getDefaultInstance())
                                  .build()))
                  .getAttributesList()
              : executeInTenantContext(
                      TEST_TENANT_ID,
                      () ->
                          stub.getAttributes(
                              GetAttributesRequest.newBuilder()
                                  .setFilter(AttributeMetadataFilter.getDefaultInstance())
                                  .build()))
                  .getAttributesList();
      List<String> attributeMetadataIdList =
          attributeMetadataList.stream()
              .map(AttributeMetadata::getId)
              .sorted()
              .collect(Collectors.toList());
      assertEquals(
          List.of(spanIdAttr.getId(), spanNameAttr.getId(), traceDurationMillis.getId()),
          attributeMetadataIdList);
    }

    {
      List<AttributeMetadata> attributeMetadataList =
          useRequestHeaders
              ? executeWithHeadersContext(
                      requestHeaders,
                      () ->
                          stub.getAttributes(
                              GetAttributesRequest.newBuilder()
                                  .setFilter(
                                      AttributeMetadataFilter.newBuilder()
                                          .addFqn(spanNameAttr.getFqn()))
                                  .build()))
                  .getAttributesList()
              : executeInTenantContext(
                      TEST_TENANT_ID,
                      () ->
                          stub.getAttributes(
                              GetAttributesRequest.newBuilder()
                                  .setFilter(
                                      AttributeMetadataFilter.newBuilder()
                                          .addFqn(spanNameAttr.getFqn()))
                                  .build()))
                  .getAttributesList();
      List<String> attributeMetadataIdList =
          attributeMetadataList.stream().map(AttributeMetadata::getId).collect(Collectors.toList());
      assertEquals(List.of(spanNameAttr.getId()), attributeMetadataIdList);
    }

    {
      List<AttributeMetadata> attributeMetadataList =
          useRequestHeaders
              ? executeWithHeadersContext(
                      requestHeaders,
                      () ->
                          stub.getAttributes(
                              GetAttributesRequest.newBuilder()
                                  .setFilter(
                                      AttributeMetadataFilter.newBuilder()
                                          .addScopeString(spanNameAttr.getScopeString())
                                          .addKey(spanNameAttr.getKey()))
                                  .build()))
                  .getAttributesList()
              : executeInTenantContext(
                      TEST_TENANT_ID,
                      () ->
                          stub.getAttributes(
                              GetAttributesRequest.newBuilder()
                                  .setFilter(
                                      AttributeMetadataFilter.newBuilder()
                                          .addScopeString(spanNameAttr.getScopeString())
                                          .addKey(spanNameAttr.getKey()))
                                  .build()))
                  .getAttributesList();
      List<String> attributeMetadataIdList =
          attributeMetadataList.stream().map(AttributeMetadata::getId).collect(Collectors.toList());
      assertEquals(List.of(spanNameAttr.getId()), attributeMetadataIdList);
    }
  }

  @Test
  void testUnknownScopeCreateReadDelete() {
    final AttributeMetadata otherNameAttr =
        AttributeMetadata.newBuilder()
            .setId("OTHER.name")
            .setFqn("some fqn")
            .setScopeString("OTHER")
            .setKey("name")
            .setType(AttributeType.ATTRIBUTE)
            .setValueKind(AttributeKind.TYPE_STRING)
            .setDisplayName("Other Name")
            .addSources(AttributeSource.QS)
            .build();

    final AttributeMetadataFilter otherScopeFilter =
        AttributeMetadataFilter.newBuilder().addScopeString("OTHER").build();
    executeWithHeadersContext(
        requestHeaders,
        () ->
            stub.create(
                AttributeCreateRequest.newBuilder()
                    .addAllAttributes(List.of(spanNameAttr, otherNameAttr))
                    .build()));

    List<String> attributeMetadataIdList =
        executeWithHeadersContext(
                requestHeaders,
                () ->
                    stub.getAttributes(
                        GetAttributesRequest.newBuilder().setFilter(otherScopeFilter).build()))
            .getAttributesList()
            .stream()
            .map(AttributeMetadata::getId)
            .collect(Collectors.toList());

    assertEquals(List.of(otherNameAttr.getId()), attributeMetadataIdList);

    attributeMetadataIdList =
        executeWithHeadersContext(
                requestHeaders,
                () ->
                    stub.getAttributes(
                        GetAttributesRequest.newBuilder()
                            .setFilter(AttributeMetadataFilter.newBuilder().addScopeString("EVENT"))
                            .build()))
            .getAttributesList()
            .stream()
            .map(AttributeMetadata::getId)
            .collect(Collectors.toList());

    assertEquals(List.of(spanNameAttr.getId()), attributeMetadataIdList);

    attributeMetadataIdList =
        executeWithHeadersContext(
                requestHeaders,
                () ->
                    stub.getAttributes(
                        GetAttributesRequest.newBuilder()
                            .setFilter(
                                AttributeMetadataFilter.newBuilder().addScope(AttributeScope.EVENT))
                            .build()))
            .getAttributesList()
            .stream()
            .map(AttributeMetadata::getId)
            .collect(Collectors.toList());

    assertEquals(List.of(spanNameAttr.getId()), attributeMetadataIdList);

    executeWithHeadersContext(requestHeaders, () -> stub.delete(otherScopeFilter));

    assertEquals(
        emptyList(),
        executeWithHeadersContext(
                requestHeaders,
                () ->
                    stub.getAttributes(
                        GetAttributesRequest.newBuilder().setFilter(otherScopeFilter).build()))
            .getAttributesList());
  }

  @Test
  public void testDeleteByFilterUseHeaders() {
    testDeleteByFilter(true);
  }

  @Test
  public void testDeleteByFilterUseTenantId() {
    testDeleteByFilter(false);
  }

  private void testDeleteByFilter(boolean useRequestHeaders) {
    createSampleAttributes(useRequestHeaders, spanNameAttr, spanIdAttr, traceDurationMillis);
    {
      List<AttributeMetadata> attributeMetadataList;
      if (useRequestHeaders) {
        executeWithHeadersContext(
            requestHeaders, () -> stub.delete(AttributeMetadataFilter.getDefaultInstance()));
        attributeMetadataList =
            executeWithHeadersContext(
                    requestHeaders,
                    () ->
                        stub.getAttributes(
                            GetAttributesRequest.newBuilder()
                                .setFilter(AttributeMetadataFilter.getDefaultInstance())
                                .build()))
                .getAttributesList();
      } else {
        executeInTenantContext(
            TEST_TENANT_ID, () -> stub.delete(AttributeMetadataFilter.getDefaultInstance()));
        attributeMetadataList =
            executeInTenantContext(
                    TEST_TENANT_ID,
                    () ->
                        stub.getAttributes(
                            GetAttributesRequest.newBuilder()
                                .setFilter(AttributeMetadataFilter.getDefaultInstance())
                                .build()))
                .getAttributesList();
      }
      assertEquals(emptyList(), attributeMetadataList);
    }

    createSampleAttributes(useRequestHeaders, spanNameAttr, spanIdAttr, traceDurationMillis);
    {
      List<AttributeMetadata> attributeMetadataList;
      if (useRequestHeaders) {
        executeWithHeadersContext(
            requestHeaders,
            () ->
                stub.delete(
                    AttributeMetadataFilter.newBuilder()
                        .addScopeString(spanNameAttr.getScopeString())
                        .addKey(spanNameAttr.getKey())
                        .build()));
        attributeMetadataList =
            executeWithHeadersContext(
                    requestHeaders,
                    () ->
                        stub.getAttributes(
                            GetAttributesRequest.newBuilder()
                                .setFilter(AttributeMetadataFilter.getDefaultInstance())
                                .build()))
                .getAttributesList();
      } else {
        executeInTenantContext(
            TEST_TENANT_ID,
            () ->
                stub.delete(
                    AttributeMetadataFilter.newBuilder()
                        .addScopeString(spanNameAttr.getScopeString())
                        .addKey(spanNameAttr.getKey())
                        .build()));
        attributeMetadataList =
            executeInTenantContext(
                    TEST_TENANT_ID,
                    () ->
                        stub.getAttributes(
                            GetAttributesRequest.newBuilder()
                                .setFilter(AttributeMetadataFilter.getDefaultInstance())
                                .build()))
                .getAttributesList();
      }
      List<String> attributeMetadataIdList =
          attributeMetadataList.stream().map(AttributeMetadata::getId).collect(Collectors.toList());
      assertEquals(2, attributeMetadataIdList.size());
      Assertions.assertTrue(
          attributeMetadataIdList.containsAll(
              Arrays.asList(spanIdAttr.getId(), traceDurationMillis.getId())));
    }
  }

  private void createSampleAttributes(
      boolean useRequestHeaders, AttributeMetadata... attributeMetadata) {
    AttributeCreateRequest attributeCreateRequest =
        AttributeCreateRequest.newBuilder()
            .addAllAttributes(Arrays.asList(attributeMetadata))
            .build();
    if (useRequestHeaders) {
      executeWithHeadersContext(requestHeaders, () -> stub.create(attributeCreateRequest));
    } else {
      executeInTenantContext(TEST_TENANT_ID, () -> stub.create(attributeCreateRequest));
    }
  }
}
