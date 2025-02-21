/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.sorting.app;

import com.github.azahnen.dagger.annotations.AutoBind;
import com.google.common.collect.ImmutableList;
import de.ii.ogcapi.collections.domain.EndpointSubCollection;
import de.ii.ogcapi.collections.domain.ImmutableOgcApiResourceData;
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
import de.ii.ogcapi.sorting.domain.SortingConfiguration;
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

/**
 * @langEn The sortables resources identifies the properties that can be referenced in the 'sortby'
 *     parameter to order the features of the collection in the response to a query. The response is
 *     returned as a JSON Schema document that describes a single JSON object where each property is
 *     a sortable. Note that the sortables schema does not specify a schema of any object that can
 *     be retrieved from the API. JSON Schema is used for the sortables to have a consistent
 *     approach for describing schema information and JSON Schema is/will be used in other parts of
 *     OGC API Features to describe schemas for GeoJSON feature content including in OpenAPI
 *     documents.
 * @langDe TODO
 * @name sortables
 * @path /collections/{collectionId}/sortables
 * @format {@link de.ii.ogcapi.features.core.domain.CollectionPropertiesFormat}
 */
@Singleton
@AutoBind
public class EndpointSortables extends EndpointSubCollection /* implements ConformanceClass */ {

  private static final Logger LOGGER = LoggerFactory.getLogger(EndpointSortables.class);

  private static final List<String> TAGS = ImmutableList.of("Discover data collections");

  private final CollectionPropertiesQueriesHandler queryHandler;
  private final JsonSchemaCache schemaCache;

  @Inject
  public EndpointSortables(
      ExtensionRegistry extensionRegistry, CollectionPropertiesQueriesHandler queryHandler) {
    super(extensionRegistry);
    this.queryHandler = queryHandler;
    this.schemaCache = new SchemaCacheSortables();
  }

  /* TODO wait for updates on Features Part n: Schemas
  @Override
  public List<String> getConformanceClassUris(OgcApiDataV2 apiData) {
      return ImmutableList.of("http://www.opengis.net/spec/ogcapi-features-n/0.0/conf/queryables");
  }
  */

  @Override
  public Class<? extends ExtensionConfiguration> getBuildingBlockConfigurationType() {
    return SortingConfiguration.class;
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
            .sortPriority(ApiEndpointDefinition.SORT_PRIORITY_SORTABLES);
    String subSubPath = "/sortables";
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
        // TODO summary and description needs to be updated to describe the Sortables resource, not
        // the Queryables resource
        final List<OgcApiQueryParameter> queryParameters =
            getQueryParameters(extensionRegistry, apiData, path, collectionId);
        final String operationSummary =
            "retrieve the sortables of the feature collection '" + collectionId + "'";
        Optional<String> operationDescription =
            Optional.of(
                "The sortables resources identifies the properties that can be"
                    + "referenced in the 'sortby' parameter to order the features of the collection in the response to a query."
                    + "The response is returned as a JSON Schema document that describes a single JSON object where each property is a sortable.\n\n"
                    + "Note that the sortables schema does not specify a schema of any object that can be retrieved from the API.\n\n"
                    + "JSON Schema is used for the sortables to have a consistent approach for describing schema information and JSON Schema "
                    + "is/will be used in other parts of OGC API Features to describe schemas for GeoJSON feature content including in OpenAPI documents.");
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
                getOperationId("getSortables", collectionId),
                TAGS)
            .ifPresent(
                operation -> resourceBuilder.putOperations(HttpMethods.GET.name(), operation));
        definitionBuilder.putResources(resourcePath, resourceBuilder.build());
      }
    }

    return definitionBuilder.build();
  }

  @GET
  @Path("/{collectionId}/sortables")
  public Response getSortables(
      @Auth Optional<User> optionalUser,
      @Context OgcApi api,
      @Context ApiRequestContext requestContext,
      @Context UriInfo uriInfo,
      @PathParam("collectionId") String collectionId) {

    CollectionPropertiesQueriesHandler.QueryInputCollectionProperties queryInput =
        new ImmutableQueryInputCollectionProperties.Builder()
            .from(getGenericQueryInput(api.getData()))
            .collectionId(collectionId)
            .type(CollectionPropertiesType.SORTABLES)
            .schemaCache(schemaCache)
            .build();

    return queryHandler.handle(
        CollectionPropertiesQueriesHandler.Query.COLLECTION_PROPERTIES, queryInput, requestContext);
  }
}
