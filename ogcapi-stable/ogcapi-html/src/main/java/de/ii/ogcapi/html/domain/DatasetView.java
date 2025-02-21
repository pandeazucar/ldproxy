/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.html.domain;

import com.github.mustachejava.util.DecoratedCollection;
import com.google.common.base.Splitter;
import de.ii.xtraplatform.services.domain.GenericView;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;

/**
 * @author zahnen
 */
public class DatasetView extends GenericView {

  public String name;
  public String title;
  public String description;
  public List<String> keywords;
  public String version;
  public String license;
  public String attribution;
  public String url;
  public String metadataUrl;
  public boolean noIndex;
  public List<DatasetView> featureTypes;
  public List<NavigationDTO> breadCrumbs;
  public List<NavigationDTO> formats;
  public String urlPrefix;
  public HtmlConfiguration htmlConfig;

  public DatasetView(
      String template, URI uri, String urlPrefix, HtmlConfiguration htmlConfig, boolean noIndex) {
    this(template, uri, null, urlPrefix, htmlConfig, noIndex);
  }

  public DatasetView(
      String template,
      URI uri,
      Object data,
      String urlPrefix,
      HtmlConfiguration htmlConfig,
      boolean noIndex) {
    super(String.format("/templates/%s", template), uri, data);
    this.keywords = new ArrayList<>();
    this.featureTypes = new ArrayList<>();
    this.urlPrefix = urlPrefix;
    this.htmlConfig = htmlConfig;
    this.noIndex = noIndex;
  }

  public DatasetView(
      String template,
      URI uri,
      String name,
      String title,
      String urlPrefix,
      HtmlConfiguration htmlConfig,
      boolean noIndex) {
    this(template, uri, urlPrefix, htmlConfig, noIndex);
    this.name = name;
    this.title = title;
  }

  public DatasetView(
      String template,
      URI uri,
      String name,
      String title,
      String description,
      String urlPrefix,
      HtmlConfiguration htmlConfig,
      boolean noIndex) {
    this(template, uri, urlPrefix, htmlConfig, noIndex);
    this.name = name;
    this.title = title;
    this.description = description;
  }

  public DatasetView(
      String template,
      URI uri,
      String name,
      String title,
      String description,
      String attribution,
      String urlPrefix,
      HtmlConfiguration htmlConfig,
      boolean noIndex) {
    this(template, uri, urlPrefix, htmlConfig, noIndex);
    this.name = name;
    this.title = title;
    this.description = description;
    this.attribution = attribution;
  }

  public DecoratedCollection<String> getKeywordsDecorated() {
    return new DecoratedCollection<>(keywords);
  }

  public Function<String, String> getQueryWithout() {
    return without -> {
      List<String> ignore = Splitter.on(',').trimResults().omitEmptyStrings().splitToList(without);

      List<NameValuePair> query =
          URLEncodedUtils.parse(getRawQuery().substring(1), StandardCharsets.UTF_8).stream()
              .filter(kvp -> !ignore.contains(kvp.getName().toLowerCase()))
              .collect(Collectors.toList());

      return '?'
          + URLEncodedUtils.format(query, '&', StandardCharsets.UTF_8)
          + (!query.isEmpty() ? '&' : "");
    };
  }

  public String getUrlPrefix() {
    return urlPrefix;
  }

  public boolean hasBreadCrumbs() {
    return breadCrumbs.size() > 1;
  }

  public String getBreadCrumbsList() {
    String result = "";
    for (int i = 0; i < breadCrumbs.size(); i++) {
      NavigationDTO item = breadCrumbs.get(i);
      result +=
          "{ \"@type\": \"ListItem\", \"position\": "
              + (i + 1)
              + ", \"name\": \""
              + item.label
              + "\"";
      if (Objects.nonNull(item.url)) {
        result += ", \"item\": \"" + item.url + "\"";
      }
      result += " }";
      if (i < breadCrumbs.size() - 1) {
        result += ",\n    ";
      }
    }
    return result;
  }

  public String getAttribution() {
    if (Objects.nonNull(htmlConfig.getLeafletAttribution())) {
      return htmlConfig.getLeafletAttribution();
    }
    if (Objects.nonNull(htmlConfig.getOpenLayersAttribution())) {
      return htmlConfig.getOpenLayersAttribution();
    }
    return htmlConfig.getBasemapAttribution();
  }
}
