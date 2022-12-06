package org.hypertrace.core.attribute.service.delegate;

import java.io.IOException;
import org.hypertrace.core.attribute.service.v1.UpdateMetadataRequest;
import org.hypertrace.core.attribute.service.v1.UpdateMetadataResponse;
import org.hypertrace.core.grpcutils.context.RequestContext;

public interface AttributeUpdater {
  UpdateMetadataResponse update(final UpdateMetadataRequest request, final RequestContext context)
      throws IOException;
}
