/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.collections.domain;

import com.google.common.collect.ImmutableSet;
import de.ii.ogcapi.foundation.domain.ApiExtensionCache;
import de.ii.ogcapi.foundation.domain.HttpMethods;
import de.ii.ogcapi.foundation.domain.OgcApiDataV2;
import de.ii.ogcapi.foundation.domain.OgcApiQueryParameter;
import de.ii.ogcapi.foundation.domain.SchemaValidator;
import io.swagger.v3.oas.models.media.Schema;
import java.util.Set;
import org.immutables.value.Value;

@Value.Immutable
public abstract class QueryParameterTemplateQueryable extends ApiExtensionCache
    implements OgcApiQueryParameter {

  public abstract String getApiId();

  public abstract String getCollectionId();

  public abstract Schema<?> getSchema();

  public abstract SchemaValidator getSchemaValidator();

  @Override
  @Value.Default
  public String getId() {
    return getName() + "_" + getCollectionId();
  }

  @Override
  public abstract String getName();

  @Override
  public abstract String getDescription();

  @Override
  @Value.Default
  public boolean getExplode() {
    return false;
  }

  @Override
  public boolean isApplicable(
      OgcApiDataV2 apiData, String definitionPath, String collectionId, HttpMethods method) {
    return computeIfAbsent(
        this.getClass().getCanonicalName()
            + apiData.hashCode()
            + definitionPath
            + collectionId
            + method.name(),
        () ->
            apiData.getId().equals(getApiId())
                && method == HttpMethods.GET
                && definitionPath.equals("/collections/{collectionId}")
                && collectionId.equals(getCollectionId()));
  }

  @Override
  public boolean isApplicable(OgcApiDataV2 apiData, String definitionPath, HttpMethods method) {
    return computeIfAbsent(
        this.getClass().getCanonicalName() + apiData.hashCode() + definitionPath + method.name(),
        () -> false);
  }

  @Override
  public Schema<?> getSchema(OgcApiDataV2 apiData) {
    return getSchema();
  }

  @Override
  public Set<String> getFilterParameters(
      Set<String> filterParameters, OgcApiDataV2 apiData, String collectionId) {
    if (!isEnabledForApi(apiData)) return filterParameters;

    return ImmutableSet.<String>builder().addAll(filterParameters).add(getId(collectionId)).build();
  }
}
