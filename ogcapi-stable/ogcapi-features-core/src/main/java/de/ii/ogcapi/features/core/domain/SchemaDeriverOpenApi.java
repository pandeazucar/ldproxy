/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.features.core.domain;

import de.ii.xtraplatform.codelists.domain.Codelist;
import de.ii.xtraplatform.features.domain.FeatureSchema;
import de.ii.xtraplatform.features.domain.SchemaBase.Type;
import de.ii.xtraplatform.features.domain.SchemaConstraints;
import de.ii.xtraplatform.features.domain.SchemaDeriver;
import de.ii.xtraplatform.geometries.domain.SimpleFeatureGeometry;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.BooleanSchema;
import io.swagger.v3.oas.models.media.DateSchema;
import io.swagger.v3.oas.models.media.DateTimeSchema;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.NumberSchema;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public abstract class SchemaDeriverOpenApi extends SchemaDeriver<Schema<?>> {

  protected final String label;
  protected final Optional<String> description;

  public SchemaDeriverOpenApi(
      String label, Optional<String> description, List<Codelist> codelists) {
    super(codelists);
    this.label = label;
    this.description = description;
  }

  @Override
  protected Optional<String> getPropertyName(Schema<?> property) {
    return Optional.ofNullable(property.getName());
  }

  @Override
  protected boolean isPropertyRequired(Schema<?> property) {
    return Objects.equals(property.getNullable(), false);
  }

  @Override
  protected Map<String, Schema<?>> getNestedProperties(Schema<?> property) {
    Map<String, Schema<?>> nestedProperties = new LinkedHashMap<>();

    if (Objects.nonNull(property.getProperties())) {
      property.getProperties().forEach(nestedProperties::put);
    }

    return nestedProperties;
  }

  @Override
  protected abstract Schema<?> buildRootSchema(
      FeatureSchema schema,
      Map<String, Schema<?>> properties,
      Map<String, Schema<?>> definitions,
      List<String> requiredProperties);

  @Override
  protected Schema<?> buildObjectSchema(
      FeatureSchema schema, Map<String, Schema<?>> properties, List<String> requiredProperties) {
    if (schema.getObjectType().filter(ot -> Objects.equals(ot, "Link")).isPresent()) {
      return new Schema<>().name(schema.getName()).$ref("#/components/schemas/Link");
    }

    Schema<?> objectSchema = new ObjectSchema().name(schema.getName());

    if (schema.getLabel().isPresent()) {
      objectSchema.title(schema.getLabel().get());
    }
    if (schema.getDescription().isPresent()) {
      objectSchema.description(schema.getDescription().get());
    }

    objectSchema.properties(new LinkedHashMap<>());
    objectSchema.getProperties().putAll(properties);
    objectSchema.required(requiredProperties);

    return objectSchema;
  }

  @Override
  protected Schema<?> getSchemaForLiteralType(
      Type type, Optional<String> label, Optional<String> description, Optional<String> unit) {
    Schema<?> valueSchema;
    switch (type) {
      case INTEGER:
        valueSchema = new IntegerSchema();
        break;
      case FLOAT:
        valueSchema = new NumberSchema();
        break;
      case BOOLEAN:
        valueSchema = new BooleanSchema();
        break;
      case DATETIME:
        valueSchema = new DateTimeSchema();
        break;
      case DATE:
        valueSchema = new DateSchema();
        break;
      case STRING:
      default:
        valueSchema = new StringSchema();
        break;
    }

    label.ifPresent(valueSchema::title);
    description.ifPresent(valueSchema::description);

    return valueSchema;
  }

  @Override
  protected Schema<?> getSchemaForGeometry(FeatureSchema schema) {
    Schema<?> oapiSchema;
    switch (schema.getGeometryType().orElse(SimpleFeatureGeometry.ANY)) {
      case POINT:
        oapiSchema =
            new Schema<>()
                .$ref(
                    "https://raw.githubusercontent.com/opengeospatial/ogcapi-features/master/core/openapi/ogcapi-features-1.yaml#/components/schemas/pointGeoJSON");
        break;
      case MULTI_POINT:
        oapiSchema =
            new Schema<>()
                .$ref(
                    "https://raw.githubusercontent.com/opengeospatial/ogcapi-features/master/core/openapi/ogcapi-features-1.yaml#/components/schemas/multipointGeoJSON");
        break;
      case LINE_STRING:
        oapiSchema =
            new Schema<>()
                .$ref(
                    "https://raw.githubusercontent.com/opengeospatial/ogcapi-features/master/core/openapi/ogcapi-features-1.yaml#/components/schemas/linestringGeoJSON");
        break;
      case MULTI_LINE_STRING:
        oapiSchema =
            new Schema<>()
                .$ref(
                    "https://raw.githubusercontent.com/opengeospatial/ogcapi-features/master/core/openapi/ogcapi-features-1.yaml#/components/schemas/multilinestringGeoJSON");
        break;
      case POLYGON:
        oapiSchema =
            new Schema<>()
                .$ref(
                    "https://raw.githubusercontent.com/opengeospatial/ogcapi-features/master/core/openapi/ogcapi-features-1.yaml#/components/schemas/polygonGeoJSON");
        break;
      case MULTI_POLYGON:
        oapiSchema =
            new Schema<>()
                .$ref(
                    "https://raw.githubusercontent.com/opengeospatial/ogcapi-features/master/core/openapi/ogcapi-features-1.yaml#/components/schemas/multipolygonGeoJSON");
        break;
      case GEOMETRY_COLLECTION:
      case ANY:
      default:
        oapiSchema =
            new Schema<>()
                .$ref(
                    "https://raw.githubusercontent.com/opengeospatial/ogcapi-features/master/core/openapi/ogcapi-features-1.yaml#/components/schemas/geometryGeoJSON");
        break;
      case NONE:
        oapiSchema = null;
        break;
    }
    return oapiSchema;
  }

  @Override
  protected Schema<?> withName(Schema<?> oapiSchema, String propertyName) {
    oapiSchema.setName(propertyName);
    return oapiSchema;
  }

  @Override
  protected Schema<?> withRequired(Schema<?> oapiSchema) {
    oapiSchema.nullable(false);
    return oapiSchema;
  }

  @Override
  protected Schema<?> withConstraints(
      Schema<?> oapiSchema,
      SchemaConstraints constraints,
      FeatureSchema property,
      List<Codelist> codelists) {
    if (oapiSchema instanceof ArraySchema) {
      if (constraints.getMinOccurrence().isPresent()) {
        oapiSchema.minItems(constraints.getMinOccurrence().get());
      }
      if (constraints.getMaxOccurrence().isPresent()) {
        oapiSchema.maxItems(constraints.getMaxOccurrence().get());
      }
    }

    Schema<?> result = oapiSchema;
    if (constraints.getRequired().isPresent() && constraints.getRequired().get()) {
      result = withRequired(result);
    }

    if (!constraints.getEnumValues().isEmpty()) {
      // if enum is specified in the configuration, it wins over codelist
      boolean isString =
          property.isArray()
              ? property.getValueType().orElse(Type.UNKNOWN) != Type.INTEGER
              : property.getType() != Type.INTEGER;
      if (isString) {
        ((Schema<String>) result).setEnum(constraints.getEnumValues());
      } else {
        ((Schema<Integer>) result)
            .setEnum(
                constraints.getEnumValues().stream()
                    .map(Integer::parseInt)
                    .collect(Collectors.toList()));
      }
    } else if (constraints.getCodelist().isPresent()) {
      Optional<Codelist> codelist =
          codelists.stream()
              .filter(cl -> cl.getId().equals(constraints.getCodelist().get()))
              .findAny();
      if (codelist.isPresent() && !codelist.get().getData().getFallback().isPresent()) {
        boolean isString =
            property.isArray()
                ? property.getValueType().orElse(Type.UNKNOWN) != Type.INTEGER
                : property.getType() != Type.INTEGER;
        Set<String> values = codelist.get().getData().getEntries().keySet();
        if (isString) {
          ((Schema<String>) result).setEnum(new ArrayList<>(values));
        } else {
          ((Schema<Integer>) result)
              .setEnum(values.stream().map(Integer::parseInt).collect(Collectors.toList()));
        }
      }
    }
    if (constraints.getRegex().isPresent()) {
      result.setPattern(constraints.getRegex().get());
    }
    if (constraints.getMin().isPresent()) {
      result.setMinimum(BigDecimal.valueOf(constraints.getMin().get()));
    }
    if (constraints.getMax().isPresent()) {
      result.setMaximum(BigDecimal.valueOf(constraints.getMax().get()));
    }

    return oapiSchema;
  }

  @Override
  protected Schema<?> withRefWrapper(Schema<?> schema, String objectType) {
    return schema;
  }

  @Override
  protected Schema<?> withArrayWrapper(Schema<?> oapiSchema) {
    return new ArraySchema()
        .items(oapiSchema)
        .name(oapiSchema.getName())
        .description(oapiSchema.getDescription());
  }
}
