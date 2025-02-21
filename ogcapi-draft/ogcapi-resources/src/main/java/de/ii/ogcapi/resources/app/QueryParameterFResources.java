/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.resources.app;

import com.github.azahnen.dagger.annotations.AutoBind;
import de.ii.ogcapi.common.domain.QueryParameterF;
import de.ii.ogcapi.foundation.domain.ExtensionConfiguration;
import de.ii.ogcapi.foundation.domain.ExtensionRegistry;
import de.ii.ogcapi.foundation.domain.FormatExtension;
import de.ii.ogcapi.foundation.domain.OgcApiDataV2;
import de.ii.ogcapi.foundation.domain.SchemaValidator;
import de.ii.ogcapi.resources.domain.ResourcesConfiguration;
import de.ii.ogcapi.resources.domain.ResourcesFormatExtension;
import de.ii.ogcapi.styles.domain.StylesConfiguration;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * @langEn Todo
 * @langDe Todo
 * @name fResources
 * @endpoints Resources
 */
@Singleton
@AutoBind
public class QueryParameterFResources extends QueryParameterF {

  @Inject
  public QueryParameterFResources(
      ExtensionRegistry extensionRegistry, SchemaValidator schemaValidator) {
    super(extensionRegistry, schemaValidator);
  }

  @Override
  public String getId() {
    return "fResources";
  }

  @Override
  protected boolean matchesPath(String definitionPath) {
    return definitionPath.equals("/resources");
  }

  @Override
  protected Class<? extends FormatExtension> getFormatClass() {
    return ResourcesFormatExtension.class;
  }

  @Override
  public boolean isEnabledForApi(OgcApiDataV2 apiData) {
    return super.isEnabledForApi(apiData)
        || apiData
            .getExtension(StylesConfiguration.class)
            .map(StylesConfiguration::isResourcesEnabled)
            .orElse(false);
  }

  @Override
  public Class<? extends ExtensionConfiguration> getBuildingBlockConfigurationType() {
    return ResourcesConfiguration.class;
  }
}
