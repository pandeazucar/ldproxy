/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.collections.queryables.app;

import com.github.azahnen.dagger.annotations.AutoBind;
import com.google.common.collect.ImmutableList;
import de.ii.ogcapi.collections.domain.EndpointSubCollection;
import de.ii.ogcapi.collections.domain.ImmutableOgcApiResourceData;
import de.ii.ogcapi.collections.queryables.domain.QueryablesConfiguration;
import de.ii.ogcapi.features.core.domain.CollectionPropertiesFormat;
import de.ii.ogcapi.features.core.domain.CollectionPropertiesQueriesHandler;
import de.ii.ogcapi.features.core.domain.CollectionPropertiesType;
import de.ii.ogcapi.features.core.domain.ImmutableQueryInputCollectionProperties;
import de.ii.ogcapi.features.core.domain.JsonSchemaCache;
import de.ii.ogcapi.foundation.domain.ApiEndpointDefinition;
import de.ii.ogcapi.foundation.domain.ApiMediaTypeContent;
import de.ii.ogcapi.foundation.domain.ApiOperation;
import de.ii.ogcapi.foundation.domain.ApiRequestContext;
import de.ii.ogcapi.foundation.domain.ExtensionConfiguration;
import de.ii.ogcapi.foundation.domain.ExtensionRegistry;
import de.ii.ogcapi.foundation.domain.FormatExtension;
import de.ii.ogcapi.foundation.domain.HttpMethods;
import de.ii.ogcapi.foundation.domain.ImmutableApiEndpointDefinition;
import de.ii.ogcapi.foundation.domain.OgcApi;
import de.ii.ogcapi.foundation.domain.OgcApiDataV2;
import de.ii.ogcapi.foundation.domain.OgcApiPathParameter;
import de.ii.ogcapi.foundation.domain.OgcApiQueryParameter;
import de.ii.xtraplatform.auth.domain.User;
import io.dropwizard.auth.Auth;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
@AutoBind
public class EndpointQueryables extends EndpointSubCollection /* implements ConformanceClass */ {

  private static final Logger LOGGER = LoggerFactory.getLogger(EndpointQueryables.class);

  private static final List<String> TAGS = ImmutableList.of("Discover data collections");

  private final CollectionPropertiesQueriesHandler queryHandler;
  private final JsonSchemaCache schemaCache;

  @Inject
  public EndpointQueryables(
      ExtensionRegistry extensionRegistry, CollectionPropertiesQueriesHandler queryHandler) {
    super(extensionRegistry);
    this.queryHandler = queryHandler;
    this.schemaCache = new SchemaCacheQueryables();
  }

  /* TODO wait for updates on Features Part n: Schemas
  @Override
  public List<String> getConformanceClassUris(OgcApiDataV2 apiData) {
      return ImmutableList.of("http://www.opengis.net/spec/ogcapi-features-n/0.0/conf/queryables");
  }
  */

  @Override
  public Class<? extends ExtensionConfiguration> getBuildingBlockConfigurationType() {
    return QueryablesConfiguration.class;
  }

  @Override
  public List<? extends FormatExtension> getFormats() {
    if (formats == null)
      formats = extensionRegistry.getExtensionsForType(CollectionPropertiesFormat.class);
    return formats;
  }

  @Override
  protected ApiEndpointDefinition computeDefinition(OgcApiDataV2 apiData) {
    ImmutableApiEndpointDefinition.Builder definitionBuilder =
        new ImmutableApiEndpointDefinition.Builder()
            .apiEntrypoint("collections")
            .sortPriority(ApiEndpointDefinition.SORT_PRIORITY_QUERYABLES);
    String subSubPath = "/queryables";
    String path = "/collections/{collectionId}" + subSubPath;
    List<OgcApiPathParameter> pathParameters = getPathParameters(extensionRegistry, apiData, path);
    Optional<OgcApiPathParameter> optCollectionIdParam =
        pathParameters.stream().filter(param -> param.getName().equals("collectionId")).findAny();
    if (!optCollectionIdParam.isPresent()) {
      LOGGER.error(
          "Path parameter 'collectionId' missing for resource at path '"
              + path
              + "'. The resource will not be available.");
    } else {
      final OgcApiPathParameter collectionIdParam = optCollectionIdParam.get();
      final boolean explode = collectionIdParam.isExplodeInOpenApi(apiData);
      final List<String> collectionIds =
          (explode) ? collectionIdParam.getValues(apiData) : ImmutableList.of("{collectionId}");
      for (String collectionId : collectionIds) {
        final List<OgcApiQueryParameter> queryParameters =
            getQueryParameters(extensionRegistry, apiData, path, collectionId);
        final String operationSummary =
            "retrieve the queryables of the feature collection '" + collectionId + "'";
        Optional<String> operationDescription =
            Optional.of(
                "The Queryables resources identifies the properties that can be "
                    + "referenced in filter expressions to select specific features that meet the criteria identified in the filter. "
                    + "The response is a JSON Schema document that describes a single JSON object where each property is a queryable.\n\n"
                    + "Note: The queryables schema does not specify a schema of any object that can be retrieved from the API.\n\n"
                    + "The descriptive metadata (title and description of the property) as well as the schema information (data type and "
                    + "constraints like a list of allowed values or minimum/maxmimum values are provided to support clients to construct"
                    + "meaningful queries for the data.");
        String resourcePath = "/collections/" + collectionId + subSubPath;
        ImmutableOgcApiResourceData.Builder resourceBuilder =
            new ImmutableOgcApiResourceData.Builder()
                .path(resourcePath)
                .pathParameters(pathParameters);
        Map<MediaType, ApiMediaTypeContent> responseContent =
            collectionId.startsWith("{")
                ? getContent(apiData, Optional.empty(), subSubPath, HttpMethods.GET)
                : getContent(apiData, Optional.of(collectionId), subSubPath, HttpMethods.GET);
        ApiOperation.getResource(
                apiData,
                resourcePath,
                false,
                queryParameters,
                ImmutableList.of(),
                responseContent,
                operationSummary,
                operationDescription,
                Optional.empty(),
                getOperationId("getQueryables", collectionId),
                TAGS)
            .ifPresent(
                operation -> resourceBuilder.putOperations(HttpMethods.GET.name(), operation));
        definitionBuilder.putResources(resourcePath, resourceBuilder.build());
      }
    }

    return definitionBuilder.build();
  }

  @GET
  @Path("/{collectionId}/queryables")
  public Response getQueryables(
      @Auth Optional<User> optionalUser,
      @Context OgcApi api,
      @Context ApiRequestContext requestContext,
      @Context UriInfo uriInfo,
      @PathParam("collectionId") String collectionId) {

    CollectionPropertiesQueriesHandler.QueryInputCollectionProperties queryInput =
        new ImmutableQueryInputCollectionProperties.Builder()
            .from(getGenericQueryInput(api.getData()))
            .collectionId(collectionId)
            .type(CollectionPropertiesType.QUERYABLES)
            .schemaCache(schemaCache)
            .build();

    return queryHandler.handle(
        CollectionPropertiesQueriesHandler.Query.COLLECTION_PROPERTIES, queryInput, requestContext);
  }
}
