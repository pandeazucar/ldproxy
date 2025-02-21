/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.common.app.html;

import com.google.common.base.Charsets;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import de.ii.ogcapi.common.domain.LandingPage;
import de.ii.ogcapi.common.domain.OgcApiDatasetView;
import de.ii.ogcapi.foundation.domain.ApiMetadata;
import de.ii.ogcapi.foundation.domain.ExternalDocumentation;
import de.ii.ogcapi.foundation.domain.I18n;
import de.ii.ogcapi.foundation.domain.Link;
import de.ii.ogcapi.foundation.domain.OgcApiDataV2;
import de.ii.ogcapi.foundation.domain.URICustomizer;
import de.ii.ogcapi.html.domain.HtmlConfiguration;
import de.ii.ogcapi.html.domain.ImmutableMapClient;
import de.ii.ogcapi.html.domain.ImmutableStyle;
import de.ii.ogcapi.html.domain.MapClient;
import de.ii.ogcapi.html.domain.NavigationDTO;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public class OgcApiLandingPageView extends OgcApiDatasetView {

  private final LandingPage apiLandingPage;
  public final String mainLinksTitle;
  public final String apiInformationTitle;
  public List<Link> distributionLinks;
  public String dataSourceUrl;
  public String keywords;
  public String keywordsWithQuotes;
  public boolean spatialSearch;
  public String dataTitle;
  public String apiDefinitionTitle;
  public String apiDocumentationTitle;
  public String providerTitle;
  public String licenseTitle;
  public String spatialExtentTitle;
  public String temporalExtentTitle;
  public String dataSourceTitle;
  public String additionalLinksTitle;
  public String expertInformationTitle;
  public String externalDocsTitle;
  public String attributionTitle;
  public String none;
  public boolean isDataset;
  public MapClient mapClient;

  public OgcApiLandingPageView(
      OgcApiDataV2 apiData,
      LandingPage apiLandingPage,
      final List<NavigationDTO> breadCrumbs,
      String urlPrefix,
      HtmlConfiguration htmlConfig,
      boolean noIndex,
      URICustomizer uriCustomizer,
      I18n i18n,
      Optional<Locale> language) {
    super(
        "landingPage.mustache",
        Charsets.UTF_8,
        apiData,
        breadCrumbs,
        htmlConfig,
        noIndex,
        urlPrefix,
        apiLandingPage.getLinks(),
        apiLandingPage.getTitle().orElse(apiData.getId()),
        apiLandingPage.getDescription().orElse(null),
        uriCustomizer,
        apiLandingPage.getExtent(),
        language);
    this.apiLandingPage = apiLandingPage;

    this.spatialSearch = false;
    this.isDataset =
        apiData.isDataset()
            && Objects.nonNull(htmlConfig)
            && Objects.equals(htmlConfig.getSchemaOrgEnabled(), true);

    this.keywords =
        apiData
            .getMetadata()
            .map(ApiMetadata::getKeywords)
            .map(v -> Joiner.on(',').skipNulls().join(v))
            .orElse(null);
    distributionLinks =
        Objects.requireNonNullElse(
            (List<Link>) apiLandingPage.getExtensions().get("datasetDownloadLinks"),
            ImmutableList.of());

    this.dataTitle = i18n.get("dataTitle", language);
    this.apiDefinitionTitle = i18n.get("apiDefinitionTitle", language);
    this.apiDocumentationTitle = i18n.get("apiDocumentationTitle", language);
    this.providerTitle = i18n.get("providerTitle", language);
    this.licenseTitle = i18n.get("licenseTitle", language);
    this.spatialExtentTitle = i18n.get("spatialExtentTitle", language);
    this.temporalExtentTitle = i18n.get("temporalExtentTitle", language);
    this.dataSourceTitle = i18n.get("dataSourceTitle", language);
    this.additionalLinksTitle = i18n.get("additionalLinksTitle", language);
    this.expertInformationTitle = i18n.get("expertInformationTitle", language);
    this.apiInformationTitle = i18n.get("apiInformationTitle", language);
    this.mainLinksTitle = i18n.get("mainLinksTitle", language);
    this.externalDocsTitle = i18n.get("externalDocsTitle", language);
    this.attributionTitle = i18n.get("attributionTitle", language);
    this.none = i18n.get("none", language);
    this.mapClient =
        new ImmutableMapClient.Builder()
            .backgroundUrl(
                Optional.ofNullable(htmlConfig.getLeafletUrl())
                    .or(() -> Optional.ofNullable(htmlConfig.getBasemapUrl())))
            .attribution(
                Optional.ofNullable(htmlConfig.getLeafletAttribution())
                    .or(() -> Optional.ofNullable(htmlConfig.getBasemapAttribution())))
            .bounds(Optional.ofNullable(this.getBbox()))
            .drawBounds(true)
            .isInteractive(false)
            .defaultStyle(new ImmutableStyle.Builder().color("red").build())
            .build();
  }

  public List<Link> getDistributionLinks() {
    return distributionLinks;
  }
  ;

  public Optional<Link> getData() {
    return links.stream()
        .filter(
            link ->
                Objects.equals(link.getRel(), "data")
                    || Objects.equals(link.getRel(), "http://www.opengis.net/def/rel/ogc/1.0/data"))
        .findFirst();
  }

  public List<Link> getTiles() {
    return links.stream()
        .filter(
            link -> link.getRel().startsWith("http://www.opengis.net/def/rel/ogc/1.0/tilesets-"))
        .collect(Collectors.toUnmodifiableList());
  }

  public Optional<Link> getStyles() {
    return links.stream()
        .filter(
            link -> Objects.equals(link.getRel(), "http://www.opengis.net/def/rel/ogc/1.0/styles"))
        .findFirst();
  }

  public Optional<Link> getRoutes() {
    return links.stream()
        .filter(
            link -> Objects.equals(link.getRel(), "http://www.opengis.net/def/rel/ogc/1.0/routes"))
        .findFirst();
  }

  public Optional<Link> getMap() {
    return links.stream().filter(link -> Objects.equals(link.getRel(), "ldp-map")).findFirst();
  }

  public Optional<Link> getApiDefinition() {
    return links.stream().filter(link -> Objects.equals(link.getRel(), "service-desc")).findFirst();
  }

  public Optional<Link> getApiDocumentation() {
    return links.stream().filter(link -> Objects.equals(link.getRel(), "service-doc")).findFirst();
  }

  public Optional<ExternalDocumentation> getExternalDocs() {
    return apiLandingPage.getExternalDocs();
  }

  public Optional<String> getSchemaOrgDataset() {
    return Optional.of(getSchemaOrgDataset(apiData, Optional.empty(), uriCustomizer.copy(), false));
  }

  public boolean getContactInfo() {
    return getMetadata()
        .filter(
            md ->
                md.getContactEmail().isPresent()
                    || md.getContactUrl().isPresent()
                    || md.getContactName().isPresent()
                    || md.getContactPhone().isPresent())
        .isPresent();
  }
}
