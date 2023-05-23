package org.opentripplanner.ext.vectortiles.layers.stops;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.opentripplanner.api.mapping.PropertyMapper;
import org.opentripplanner.ext.vectortiles.VectorTilesResource;
import org.opentripplanner.framework.i18n.I18NString;
import org.opentripplanner.inspector.vector.LayerBuilder;
import org.opentripplanner.inspector.vector.LayerParameters;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.transit.service.TransitService;

public class StopsLayerBuilder extends LayerBuilder<RegularStop> {

  static Map<MapperType, BiFunction<TransitService, Locale, PropertyMapper<RegularStop>>> mappers = Map.of(
    MapperType.Digitransit,
    DigitransitStopPropertyMapper::create
  );
  private final TransitService transitService;
  private final String defaultZoomLevels;

  public StopsLayerBuilder(
    TransitService transitService,
    LayerParameters<VectorTilesResource.LayerType> layerParameters,
    Locale locale
  ) {
    super(
      mappers.get(MapperType.valueOf(layerParameters.mapper())).apply(transitService, locale),
      layerParameters.name(),
      layerParameters.expansionFactor()
    );
    this.defaultZoomLevels = layerParameters.defaultZoomLevels();
    this.transitService = transitService;
  }

  protected List<Geometry> getGeometries(Envelope query, int z) {
    return transitService
      .findRegularStop(query)
      .stream()
      .filter(stop ->
        LayerBuilder.isWithinZoomBounds(
          Optional
            .ofNullable(stop.getDescription())
            .map(I18NString::toString)
            .orElse(defaultZoomLevels),
          z
        )
      )
      .filter(stop -> !transitService.getPatternsForStop(stop).isEmpty())
      .map(stop -> {
        Geometry point = stop.getGeometry();

        point.setUserData(stop);

        return point;
      })
      .collect(Collectors.toList());
  }

  enum MapperType {
    Digitransit,
  }
}
