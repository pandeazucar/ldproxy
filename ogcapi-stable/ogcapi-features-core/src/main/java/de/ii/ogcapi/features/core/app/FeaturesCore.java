/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.features.core.app;

import com.github.azahnen.dagger.annotations.AutoBind;
import com.google.common.collect.ImmutableList;
import de.ii.ogcapi.features.core.domain.FeaturesCoreConfiguration;
import de.ii.ogcapi.features.core.domain.ItemTypeSpecificConformanceClass;
import de.ii.ogcapi.foundation.domain.OgcApiDataV2;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
@AutoBind
public class FeaturesCore implements ItemTypeSpecificConformanceClass {

  private static final Logger LOGGER = LoggerFactory.getLogger(FeaturesCore.class);

  @Inject
  FeaturesCore() {}

  @Override
  public List<String> getConformanceClassUris(OgcApiDataV2 apiData) {
    ImmutableList.Builder<String> builder = new ImmutableList.Builder<>();

    if (isItemTypeUsed(apiData, FeaturesCoreConfiguration.ItemType.feature))
      builder.add("http://www.opengis.net/spec/ogcapi-features-1/1.0/conf/core");

    if (isItemTypeUsed(apiData, FeaturesCoreConfiguration.ItemType.record))
      builder.add(
          "http://www.opengis.net/spec/ogcapi-features-1/1.0/conf/core",
          "http://www.opengis.net/spec/ogcapi-records-1/0.0/conf/records-api");

    return builder.build();
  }
}
