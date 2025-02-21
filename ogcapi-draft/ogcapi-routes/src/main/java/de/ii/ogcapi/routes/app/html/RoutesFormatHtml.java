/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.routes.app.html;

import com.github.azahnen.dagger.annotations.AutoBind;
import com.google.common.collect.ImmutableList;
import de.ii.ogcapi.foundation.domain.ApiMediaType;
import de.ii.ogcapi.foundation.domain.ApiMediaTypeContent;
import de.ii.ogcapi.foundation.domain.ApiRequestContext;
import de.ii.ogcapi.foundation.domain.ExtensionConfiguration;
import de.ii.ogcapi.foundation.domain.I18n;
import de.ii.ogcapi.foundation.domain.ImmutableApiMediaType;
import de.ii.ogcapi.foundation.domain.ImmutableApiMediaTypeContent;
import de.ii.ogcapi.foundation.domain.OgcApi;
import de.ii.ogcapi.foundation.domain.OgcApiDataV2;
import de.ii.ogcapi.foundation.domain.URICustomizer;
import de.ii.ogcapi.html.domain.HtmlConfiguration;
import de.ii.ogcapi.html.domain.NavigationDTO;
import de.ii.ogcapi.routes.domain.HtmlForm;
import de.ii.ogcapi.routes.domain.HtmlFormDefaults;
import de.ii.ogcapi.routes.domain.ImmutableHtmlFormDefaults;
import de.ii.ogcapi.routes.domain.Routes;
import de.ii.ogcapi.routes.domain.RoutesFormatExtension;
import de.ii.ogcapi.routes.domain.RoutingConfiguration;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.core.MediaType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
@AutoBind
public class RoutesFormatHtml implements RoutesFormatExtension {

  private static final Logger LOGGER = LoggerFactory.getLogger(RoutesFormatHtml.class);
  static final ApiMediaType MEDIA_TYPE =
      new ImmutableApiMediaType.Builder()
          .type(MediaType.TEXT_HTML_TYPE)
          .label("HTML")
          .parameter("html")
          .build();

  private final Schema schemaHtml;
  public static final String SCHEMA_REF_HTML = "#/components/schemas/htmlSchema";
  private final I18n i18n;

  @Inject
  public RoutesFormatHtml(I18n i18n) {
    this.i18n = i18n;
    schemaHtml = new StringSchema().example("<html>...</html>");
  }

  @Override
  public Class<? extends ExtensionConfiguration> getBuildingBlockConfigurationType() {
    return RoutingConfiguration.class;
  }

  @Override
  public boolean isEnabledForApi(OgcApiDataV2 apiData) {
    return apiData
        .getExtension(RoutingConfiguration.class)
        .filter(RoutingConfiguration::isEnabled)
        .map(RoutingConfiguration::getHtml)
        .filter(HtmlForm::isEnabled)
        .isPresent();
  }

  @Override
  public Object getRoutesEntity(Routes routes, OgcApi api, ApiRequestContext requestContext) {
    String rootTitle = i18n.get("root", requestContext.getLanguage());
    String routesTitle = i18n.get("routesTitle", requestContext.getLanguage());

    final URICustomizer uriCustomizer = requestContext.getUriCustomizer().clearParameters();
    final List<NavigationDTO> breadCrumbs =
        new ImmutableList.Builder<NavigationDTO>()
            .add(
                new NavigationDTO(
                    rootTitle,
                    uriCustomizer
                        .copy()
                        .removeLastPathSegments(api.getData().getSubPath().size() + 1)
                        .toString()))
            .add(
                new NavigationDTO(
                    api.getData().getLabel(),
                    uriCustomizer.copy().removeLastPathSegments(1).toString()))
            .add(new NavigationDTO(routesTitle))
            .build();

    HtmlConfiguration htmlConfig = api.getData().getExtension(HtmlConfiguration.class).orElse(null);

    HtmlFormDefaults htmlDefaults =
        api.getData()
            .getExtension(RoutingConfiguration.class)
            .map(RoutingConfiguration::getHtml)
            .flatMap(HtmlForm::getDefaults)
            .orElse(ImmutableHtmlFormDefaults.builder().build());

    RoutesView view =
        new RoutesView(
            api.getData(),
            routes,
            htmlDefaults,
            api.getSpatialExtent(),
            breadCrumbs,
            requestContext.getStaticUrlPrefix(),
            htmlConfig,
            isNoIndexEnabledForApi(api.getData()),
            i18n,
            requestContext.getLanguage());
    return view;
  }

  @Override
  public ApiMediaTypeContent getContent(OgcApiDataV2 apiData, String path) {
    return new ImmutableApiMediaTypeContent.Builder()
        .schema(schemaHtml)
        .schemaRef(SCHEMA_REF_HTML)
        .ogcApiMediaType(MEDIA_TYPE)
        .build();
  }

  @Override
  public ApiMediaType getMediaType() {
    return MEDIA_TYPE;
  }

  private boolean isNoIndexEnabledForApi(OgcApiDataV2 apiData) {
    return apiData
        .getExtension(HtmlConfiguration.class)
        .map(HtmlConfiguration::getNoIndexEnabled)
        .orElse(true);
  }
}
