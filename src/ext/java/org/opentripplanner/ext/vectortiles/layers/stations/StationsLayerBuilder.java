package org.opentripplanner.ext.vectortiles.layers.stations;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;
import org.opentripplanner.api.mapping.PropertyMapper;
import org.opentripplanner.ext.vectortiles.VectorTilesResource;
import org.opentripplanner.framework.geometry.GeometryUtils;
import org.opentripplanner.framework.i18n.I18NString;
import org.opentripplanner.inspector.vector.LayerBuilder;
import org.opentripplanner.inspector.vector.LayerParameters;
import org.opentripplanner.transit.model.site.Station;
import org.opentripplanner.transit.service.TransitService;

public class StationsLayerBuilder extends LayerBuilder<Station> {

  static Map<MapperType, BiFunction<TransitService, Locale, PropertyMapper<Station>>> mappers = Map.of(
    MapperType.Digitransit,
    DigitransitStationPropertyMapper::create
  );
  private final TransitService transitService;
  private final String defaultZoomLevels;

  public StationsLayerBuilder(
    TransitService transitService,
    LayerParameters<VectorTilesResource.LayerType> layerParameters,
    Locale locale
  ) {
    super(
      mappers.get(MapperType.valueOf(layerParameters.mapper())).apply(transitService, locale),
      layerParameters.name(),
      layerParameters.expansionFactor()
    );
    this.transitService = transitService;
    this.defaultZoomLevels = layerParameters.defaultZoomLevels();
  }

  protected List<Geometry> getGeometries(Envelope query, int z) {
    return transitService
      .getStations()
      .stream()
      .filter(station ->
        LayerBuilder.isWithinZoomBounds(
          Optional
            .ofNullable(station.getDescription())
            .map(I18NString::toString)
            .orElse(defaultZoomLevels),
          z
        )
      )
      .map(station -> {
        Coordinate coordinate = station.getCoordinate().asJtsCoordinate();
        Point point = GeometryUtils.getGeometryFactory().createPoint(coordinate);
        point.setUserData(station);
        return point;
      })
      .collect(Collectors.toList());
  }

  enum MapperType {
    Digitransit,
  }
}
