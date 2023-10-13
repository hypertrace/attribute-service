package org.hypertrace.core.attribute.service;

import static java.util.Objects.isNull;
import static org.hypertrace.core.attribute.service.constants.AttributeFieldPathConstants.FQN_PATH;
import static org.hypertrace.core.attribute.service.constants.AttributeFieldPathConstants.INTERNAL_PATH;
import static org.hypertrace.core.attribute.service.constants.AttributeFieldPathConstants.KEY_PATH;
import static org.hypertrace.core.attribute.service.constants.AttributeFieldPathConstants.SCOPE_PATH;
import static org.hypertrace.core.attribute.service.constants.AttributeFieldPathConstants.SCOPE_STRING_PATH;
import static org.hypertrace.core.attribute.service.constants.AttributeFieldPathConstants.SOURCE_METADATA_PATH;
import static org.hypertrace.core.attribute.service.constants.AttributeFieldPathConstants.TENANT_ID_PATH;
import static org.hypertrace.core.attribute.service.utils.tenant.TenantUtils.ROOT_TENANT_ID;
import static org.hypertrace.core.attribute.service.validator.AttributeMetadataValidator.validateAndUpdateDeletionFilter;

import com.google.common.collect.Streams;
import com.google.protobuf.ServiceException;
import com.typesafe.config.Config;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Spliterators;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import org.hypertrace.core.attribute.service.converter.AttributeMetadataConverter;
import org.hypertrace.core.attribute.service.converter.AttributeMetadataConverterImpl;
import org.hypertrace.core.attribute.service.delegate.AttributeUpdater;
import org.hypertrace.core.attribute.service.delegate.AttributeUpdaterImpl;
import org.hypertrace.core.attribute.service.model.AttributeMetadataDocKey;
import org.hypertrace.core.attribute.service.model.AttributeMetadataModel;
import org.hypertrace.core.attribute.service.utils.tenant.TenantUtils;
import org.hypertrace.core.attribute.service.v1.AttributeCreateRequest;
import org.hypertrace.core.attribute.service.v1.AttributeMetadata;
import org.hypertrace.core.attribute.service.v1.AttributeMetadataFilter;
import org.hypertrace.core.attribute.service.v1.AttributeScope;
import org.hypertrace.core.attribute.service.v1.AttributeServiceGrpc;
import org.hypertrace.core.attribute.service.v1.AttributeSource;
import org.hypertrace.core.attribute.service.v1.AttributeSourceMetadataDeleteRequest;
import org.hypertrace.core.attribute.service.v1.AttributeSourceMetadataUpdateRequest;
import org.hypertrace.core.attribute.service.v1.Empty;
import org.hypertrace.core.attribute.service.v1.GetAttributesRequest;
import org.hypertrace.core.attribute.service.v1.GetAttributesResponse;
import org.hypertrace.core.attribute.service.v1.UpdateMetadataRequest;
import org.hypertrace.core.attribute.service.v1.UpdateMetadataResponse;
import org.hypertrace.core.attribute.service.validator.AttributeMetadataValidator;
import org.hypertrace.core.documentstore.Collection;
import org.hypertrace.core.documentstore.Datastore;
import org.hypertrace.core.documentstore.DatastoreProvider;
import org.hypertrace.core.documentstore.Document;
import org.hypertrace.core.documentstore.Filter;
import org.hypertrace.core.documentstore.Filter.Op;
import org.hypertrace.core.documentstore.JSONDocument;
import org.hypertrace.core.documentstore.Key;
import org.hypertrace.core.documentstore.Query;
import org.hypertrace.core.documentstore.model.config.DatastoreConfig;
import org.hypertrace.core.documentstore.model.config.TypesafeConfigDatastoreConfigExtractor;
import org.hypertrace.core.grpcutils.context.RequestContext;
import org.hypertrace.core.serviceframework.docstore.metrics.DocStoreMetricsRegistry;
import org.hypertrace.core.serviceframework.spi.PlatformServiceLifecycle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service that implements the fetch operations on Attributes This is an Proto based implementation
 */
public class AttributeServiceImpl extends AttributeServiceGrpc.AttributeServiceImplBase {
  private static final Logger LOGGER = LoggerFactory.getLogger(AttributeServiceImpl.class);

  private static final String DOC_STORE_CONFIG_KEY = "document.store";
  private static final String DATA_STORE_TYPE = "dataStoreType";
  private static final String ATTRIBUTE_METADATA_COLLECTION = "attribute_metadata";

  private final Collection collection;
  private final AttributeMetadataValidator validator;
  private final AttributeMetadataConverter converter;
  private final AttributeUpdater updater;

  private static String sourceMetadataPathFor(final AttributeSource source) {
    return String.join(".", SOURCE_METADATA_PATH, source.name());
  }

  /**
   * Initiates with a configuration. The configuration should be production configuration, but for
   * V0 The attributes type data would be stored in the configuration
   */
  public AttributeServiceImpl(Config config, PlatformServiceLifecycle platformServiceLifecycle) {
    Datastore store = initDataStore(config, platformServiceLifecycle);
    this.collection = store.getCollection(ATTRIBUTE_METADATA_COLLECTION);
    this.validator = new AttributeMetadataValidator(config);
    this.converter = new AttributeMetadataConverterImpl();
    this.updater = new AttributeUpdaterImpl(collection);
  }

  AttributeServiceImpl(Collection collection) {
    this.collection = collection;
    this.validator = new AttributeMetadataValidator();
    this.converter = new AttributeMetadataConverterImpl();
    this.updater = new AttributeUpdaterImpl(collection);
  }

  private Datastore initDataStore(
      Config config, PlatformServiceLifecycle platformServiceLifecycle) {
    final Config docStoreConfig = config.getConfig(DOC_STORE_CONFIG_KEY);
    final String dataStoreType = docStoreConfig.getString(DATA_STORE_TYPE);
    final DatastoreConfig datastoreConfig =
        TypesafeConfigDatastoreConfigExtractor.from(docStoreConfig, DATA_STORE_TYPE)
            .hostKey(dataStoreType + ".host")
            .portKey(dataStoreType + ".port")
            .keysForEndpoints(dataStoreType + ".endpoints", "host", "port")
            .authDatabaseKey(dataStoreType + ".authDb")
            .replicaSetKey(dataStoreType + ".replicaSet")
            .databaseKey(dataStoreType + ".database")
            .usernameKey(dataStoreType + ".user")
            .passwordKey(dataStoreType + ".password")
            .applicationNameKey("appName")
            .poolMaxConnectionsKey("maxPoolSize")
            .poolConnectionAccessTimeoutKey("connectionAccessTimeout")
            .poolConnectionSurrenderTimeoutKey("connectionIdleTime")
            .extract();

    final Datastore datastore = DatastoreProvider.getDatastore(datastoreConfig);
    new DocStoreMetricsRegistry(datastore)
        .withPlatformLifecycle(platformServiceLifecycle)
        .monitor();

    return datastore;
  }

  @Override
  public void create(AttributeCreateRequest request, StreamObserver<Empty> responseObserver) {
    Optional<String> tenantIdOptional = RequestContext.CURRENT.get().getTenantId();
    if (tenantIdOptional.isEmpty()) {
      responseObserver.onError(new ServiceException("Tenant id is missing in the request."));
      return;
    }

    final String tenantId = tenantIdOptional.orElseThrow();
    final AttributeMetadataFilter filter =
        AttributeMetadataFilter.newBuilder().setCustom(true).build();

    try {
      validator.validate(
          request, tenantId, () -> collection.total(getQueryForFilter(tenantId, filter)));
      Map<Key, Document> attributeDocs = new HashMap<>();
      for (AttributeMetadata attributeMetadata : request.getAttributesList()) {
        AttributeMetadataModel attributeMetadataModel =
            AttributeMetadataModel.fromDTO(attributeMetadata);
        attributeMetadataModel.setTenantId(tenantId);
        attributeDocs.put(
            new AttributeMetadataDocKey(
                tenantId, attributeMetadataModel.getScopeString(), attributeMetadataModel.getKey()),
            attributeMetadataModel);
      }

      boolean status = collection.bulkUpsert(attributeDocs);
      if (status) {
        responseObserver.onNext(Empty.newBuilder().build());
        responseObserver.onCompleted();
      } else {
        responseObserver.onError(
            new RuntimeException(
                String.format(
                    "Could not bulk insert attributes. AttributeCreateRequest:%s", request)));
      }
    } catch (final Exception e) {
      LOGGER.warn("Could not create attributes with request: " + request, e);
      responseObserver.onError(
          Status.INTERNAL
              .withDescription("Could not create attributes with request: " + request)
              .asException());
    }
  }

  @Override
  public void updateSourceMetadata(
      AttributeSourceMetadataUpdateRequest request, StreamObserver<Empty> responseObserver) {
    Optional<String> tenantId = RequestContext.CURRENT.get().getTenantId();
    if (tenantId.isEmpty()) {
      responseObserver.onError(new ServiceException("Tenant id is missing in the request."));
      return;
    }

    try {
      // Fetch attributes by FQN
      Iterator<Document> documents =
          collection.search(getQueryByTenantIdAndFQN(tenantId.get(), request.getFqn()));
      // For each attribute matching the FQN update the source metadata
      boolean status =
          StreamSupport.stream(Spliterators.spliteratorUnknownSize(documents, 0), false)
              .map(Document::toJson)
              .map(
                  attrMetadataDoc -> {
                    try {
                      AttributeMetadata metadata =
                          AttributeMetadataModel.fromJson(attrMetadataDoc).toDTO();
                      boolean response =
                          collection.updateSubDoc(
                              AttributeMetadataDocKey.from(tenantId.get(), metadata),
                              sourceMetadataPathFor(request.getSource()),
                              new JSONDocument(request.getSourceMetadataMap()));
                      if (!response) {
                        LOGGER.warn(
                            "Error updating source metadata for attribute:{}, request:{}",
                            metadata,
                            request);
                      }
                      return response;
                    } catch (IOException ex) {
                      LOGGER.warn(
                          "Unable to convert this Json String to AttributeMetadata : {}",
                          attrMetadataDoc);
                      return false;
                    }
                  })
              .reduce(true, (b1, b2) -> b1 && b2);
      if (status) {
        responseObserver.onNext(Empty.newBuilder().build());
        responseObserver.onCompleted();
      } else {
        responseObserver.onError(
            new RuntimeException(
                String.format("Error updating source metadata for request:%s", request)));
      }
    } catch (Exception e) {
      LOGGER.error("Error finding attributes with filter:" + request, e);
      responseObserver.onError(e);
    }
  }

  @Override
  public void delete(
      final AttributeMetadataFilter request, final StreamObserver<Empty> responseObserver) {
    final AttributeMetadataFilter modifiedRequest = validateAndUpdateDeletionFilter(request);

    Optional<String> tenantId = RequestContext.CURRENT.get().getTenantId();
    if (tenantId.isEmpty()) {
      responseObserver.onError(new ServiceException("Tenant id is missing in the request."));
      return;
    }

    try {
      Iterator<Document> documents =
          collection.search(getQueryForFilter(tenantId.get(), modifiedRequest));
      boolean status =
          StreamSupport.stream(Spliterators.spliteratorUnknownSize(documents, 0), false)
              .map(Document::toJson)
              .map(
                  attrMetadataDoc -> {
                    try {
                      AttributeMetadata metadata =
                          AttributeMetadataModel.fromJson(attrMetadataDoc).toDTO();
                      boolean response =
                          collection.delete(AttributeMetadataDocKey.from(tenantId.get(), metadata));
                      if (!response) {
                        LOGGER.warn(
                            "Error updating source metadata for attribute:{}, request:{}",
                            metadata,
                            modifiedRequest);
                      }
                      return response;
                    } catch (IOException ex) {
                      LOGGER.warn(
                          "Unable to convert this Json String to AttributeMetadata : {}",
                          attrMetadataDoc);
                      return false;
                    }
                  })
              .reduce(true, (b1, b2) -> b1 && b2);
      if (status) {
        responseObserver.onNext(Empty.newBuilder().build());
        responseObserver.onCompleted();
      } else {
        responseObserver.onError(
            new RuntimeException(
                String.format(
                    "Error deleting attribute metadata for request:%s", modifiedRequest)));
      }
    } catch (final Exception e) {
      LOGGER.warn("Error deleting attribute metadata for request: " + request, e);
      responseObserver.onError(
          Status.INTERNAL
              .withDescription(
                  String.format("Error deleting attribute metadata for request: %s", request))
              .asException());
    }
  }

  @Override
  public void deleteSourceMetadata(
      AttributeSourceMetadataDeleteRequest request, StreamObserver<Empty> responseObserver) {
    Optional<String> tenantId = RequestContext.CURRENT.get().getTenantId();
    if (tenantId.isEmpty()) {
      responseObserver.onError(new ServiceException("Tenant id is missing in the request."));
      return;
    }

    try {
      // Fetch attributes by FQN
      Iterator<Document> documents =
          collection.search(getQueryByTenantIdAndFQN(tenantId.get(), request.getFqn()));
      // For each attribute matching the FQN update the source metadata
      boolean status =
          StreamSupport.stream(Spliterators.spliteratorUnknownSize(documents, 0), false)
              .map(Document::toJson)
              .map(
                  attrMetadataDoc -> {
                    try {
                      AttributeMetadata metadata =
                          AttributeMetadataModel.fromJson(attrMetadataDoc).toDTO();
                      boolean response =
                          collection.deleteSubDoc(
                              AttributeMetadataDocKey.from(tenantId.get(), metadata),
                              sourceMetadataPathFor(request.getSource()));
                      if (!response) {
                        LOGGER.warn(
                            "Error updating source metadata for attribute:{}, request:{}",
                            metadata,
                            request);
                      }
                      return response;
                    } catch (IOException ex) {
                      LOGGER.warn(
                          "Unable to convert this Json String to AttributeMetadata : {}",
                          attrMetadataDoc);
                      return false;
                    }
                  })
              .reduce(true, (b1, b2) -> b1 && b2);
      if (status) {
        responseObserver.onNext(Empty.newBuilder().build());
        responseObserver.onCompleted();
      } else {
        responseObserver.onError(
            new RuntimeException(
                String.format("Error updating source metadata for request:%s", request)));
      }
    } catch (Exception e) {
      LOGGER.error("Error finding attributes with filter:" + request, e);
      responseObserver.onError(e);
    }
  }

  @Override
  public void findAttributes(
      AttributeMetadataFilter request, StreamObserver<AttributeMetadata> responseObserver) {
    Optional<String> tenantId = RequestContext.CURRENT.get().getTenantId();
    if (tenantId.isEmpty()) {
      responseObserver.onError(new ServiceException("Tenant id is missing in the request."));
      return;
    }

    try {
      Iterator<Document> documents = collection.search(getQueryForFilter(tenantId.get(), request));
      sendResult(documents, responseObserver);
    } catch (Exception e) {
      LOGGER.error("Error finding attributes with filter:" + request, e);
      responseObserver.onError(e);
    }
  }

  @Override
  public void findAll(Empty request, StreamObserver<AttributeMetadata> responseObserver) {
    Optional<String> tenantId = RequestContext.CURRENT.get().getTenantId();
    if (tenantId.isEmpty()) {
      responseObserver.onError(new ServiceException("Tenant id is missing in the request."));
      return;
    }

    try {
      // query with filter on Tenant id
      Query query = new Query();
      query.setFilter(getTenantIdInFilter(TenantUtils.getTenantHierarchy(tenantId.get())));

      Iterator<Document> documents = collection.search(query);
      sendResult(documents, responseObserver);
    } catch (Exception e) {
      LOGGER.error("Error finding all attributes", e);
      responseObserver.onError(e);
    }
  }

  @Override
  public void getAttributes(
      GetAttributesRequest request, StreamObserver<GetAttributesResponse> responseObserver) {
    String tenantId = RequestContext.CURRENT.get().getTenantId().orElse(null);
    if (isNull(tenantId)) {
      responseObserver.onError(new ServiceException("Tenant id is missing in the request."));
      return;
    }

    List<AttributeMetadata> attributes =
        Streams.stream(collection.search(this.getQueryForFilter(tenantId, request.getFilter())))
            .map(converter::convert)
            .flatMap(Optional::stream)
            .collect(Collectors.toUnmodifiableList());

    responseObserver.onNext(
        GetAttributesResponse.newBuilder().addAllAttributes(attributes).build());
    responseObserver.onCompleted();
  }

  @Override
  public void updateMetadata(
      final UpdateMetadataRequest request,
      final StreamObserver<UpdateMetadataResponse> responseObserver) {
    try {
      final UpdateMetadataResponse response = updater.update(request, RequestContext.CURRENT.get());
      responseObserver.onNext(response);
      responseObserver.onCompleted();
    } catch (final Exception e) {
      LOGGER.error("Error updating attribute metadata", e);
      responseObserver.onError(e);
    }
  }

  private void sendResult(
      Iterator<Document> documents, StreamObserver<AttributeMetadata> responseObserver) {
    while (documents.hasNext()) {
      converter.convert(documents.next()).ifPresent(responseObserver::onNext);
    }
    responseObserver.onCompleted();
  }

  private Query getQueryForFilter(
      String tenantId, AttributeMetadataFilter attributeMetadataFilter) {
    List<String> scopeFilterList =
        Stream.concat(
                attributeMetadataFilter.getScopeStringList().stream(),
                attributeMetadataFilter.getScopeList().stream().map(AttributeScope::name))
            .collect(Collectors.toUnmodifiableList());
    List<String> keyFilterRequest = attributeMetadataFilter.getKeyList();
    List<String> fqnFilterRequest = attributeMetadataFilter.getFqnList();
    List<Filter> andFilters = new ArrayList<>();

    andFilters.add(getTenantIdInFilter(TenantUtils.getTenantHierarchy(tenantId)));

    if (fqnFilterRequest != null && !fqnFilterRequest.isEmpty()) {
      andFilters.add(new Filter(Filter.Op.IN, FQN_PATH, fqnFilterRequest));
    }

    if (!scopeFilterList.isEmpty()) {
      andFilters.add(
          new Filter(Filter.Op.IN, SCOPE_STRING_PATH, scopeFilterList)
              .or(new Filter(Filter.Op.IN, SCOPE_PATH, scopeFilterList)));
    }

    if (!keyFilterRequest.isEmpty()) {
      andFilters.add(new Filter(Filter.Op.IN, KEY_PATH, keyFilterRequest));
    }

    if (attributeMetadataFilter.hasInternal()) {
      Filter internalFilter =
          new Filter(Op.EQ, INTERNAL_PATH, attributeMetadataFilter.getInternal());
      if (!attributeMetadataFilter.getInternal()) {
        // For backwards compatibility, treat an attribute missing internal attribute as external
        internalFilter = internalFilter.or(new Filter(Op.NOT_EXISTS, INTERNAL_PATH, null));
      }
      andFilters.add(internalFilter);
    }

    if (attributeMetadataFilter.hasCustom()) {
      andFilters.add(
          attributeMetadataFilter.getCustom()
              ? getTenantIdEqFilter(tenantId)
              : getTenantIdEqFilter(ROOT_TENANT_ID));
    }

    Filter queryFilter = new Filter();
    if (!andFilters.isEmpty()) {
      queryFilter = andFilters.remove(0);
      // and add the remaining ones
      for (Filter filter : andFilters) {
        queryFilter = queryFilter.and(filter);
      }
    }

    Query query = new Query();
    query.setFilter(queryFilter);
    return query;
  }

  private Query getQueryByTenantIdAndFQN(String tenantId, String fqn) {
    Filter queryFilter = new Filter();
    queryFilter.setOp(Filter.Op.AND);
    queryFilter.setChildFilters(
        new Filter[] {getTenantIdEqFilter(tenantId), new Filter(Filter.Op.EQ, FQN_PATH, fqn)});
    Query query = new Query();
    query.setFilter(queryFilter);
    return query;
  }

  /**
   * Method to apply the tenant id equals filter.
   *
   * @param tenantId The tenant id.
   */
  private Filter getTenantIdEqFilter(String tenantId) {
    return new Filter(Filter.Op.EQ, TENANT_ID_PATH, tenantId);
  }

  /**
   * Method to get a filter which applies the "tenant id in <list>" filter.
   *
   * @param tenantIds The tenant id.
   */
  private Filter getTenantIdInFilter(List<String> tenantIds) {
    return new Filter(Filter.Op.IN, TENANT_ID_PATH, tenantIds);
  }
}
