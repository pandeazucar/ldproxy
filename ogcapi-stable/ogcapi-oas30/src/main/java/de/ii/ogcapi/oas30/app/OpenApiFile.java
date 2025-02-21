/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.oas30.app;

import com.github.azahnen.dagger.annotations.AutoBind;
import de.ii.ogcapi.common.domain.ApiDefinitionFormatExtension;
import de.ii.ogcapi.foundation.domain.ApiMediaType;
import de.ii.ogcapi.foundation.domain.ApiMediaTypeContent;
import de.ii.ogcapi.foundation.domain.ApiRequestContext;
import de.ii.ogcapi.foundation.domain.ExtensionConfiguration;
import de.ii.ogcapi.foundation.domain.FormatNotSupportedException;
import de.ii.ogcapi.foundation.domain.ImmutableApiMediaType;
import de.ii.ogcapi.foundation.domain.ImmutableApiMediaTypeContent;
import de.ii.ogcapi.foundation.domain.OgcApiDataV2;
import de.ii.ogcapi.oas30.domain.Oas30Configuration;
import de.ii.xtraplatform.openapi.domain.OpenApiViewerResource;
import io.swagger.v3.oas.models.media.Schema;
import java.text.MessageFormat;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
@AutoBind
public class OpenApiFile implements ApiDefinitionFormatExtension {

  private static Logger LOGGER = LoggerFactory.getLogger(OpenApiFile.class);
  private static ApiMediaType MEDIA_TYPE =
      new ImmutableApiMediaType.Builder().type(MediaType.WILDCARD_TYPE).build();

  private final ExtendableOpenApiDefinition openApiDefinition;
  private final OpenApiViewerResource openApiViewerResource;

  @Inject
  public OpenApiFile(
      ExtendableOpenApiDefinition openApiDefinition, OpenApiViewerResource openApiViewerResource) {
    this.openApiDefinition = openApiDefinition;
    this.openApiViewerResource = openApiViewerResource;
  }

  @Override
  public ApiMediaType getMediaType() {
    return MEDIA_TYPE;
  }

  // always active, if OpenAPI 3.0 is active, since this is needed for the HTML output
  @Override
  public Class<? extends ExtensionConfiguration> getBuildingBlockConfigurationType() {
    return Oas30Configuration.class;
  }

  @Override
  public ApiMediaTypeContent getContent(OgcApiDataV2 apiData, String path) {
    if (path.equals("/api")) return null;

    return new ImmutableApiMediaTypeContent.Builder()
        .schema(new Schema())
        .schemaRef("#/components/schemas/any")
        .ogcApiMediaType(MEDIA_TYPE)
        .build();
  }

  @Override
  public String getPathPattern() {
    // This is a special case and we need to override the default pattern to support
    // auxiliary files accessed by the Swagger UI HTML output
    return "^/api/[^/]+$";
  }

  @Override
  public Response getApiDefinitionResponse(OgcApiDataV2 apiData, ApiRequestContext requestContext) {
    throw new FormatNotSupportedException(
        MessageFormat.format(
            "The requested media type {0} cannot be generated.",
            requestContext.getMediaType().type()));
  }

  @Override
  public Response getApiDefinitionFile(
      OgcApiDataV2 apiData, ApiRequestContext apiRequestContext, String file) {
    LOGGER.debug("FILE {}", file);

    if (openApiViewerResource == null) {
      throw new NullPointerException(
          "The object to retrieve auxiliary files for the HTML API documentation is null, but should not be null.");
    }

    // TODO: this also returns a 200 with an entity for non-existing files, but there is no way to
    // identify it here to throw a NotFoundException()
    Response response = openApiViewerResource.getFile(file);
    return response;
  }
}
