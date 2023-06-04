package org.opentripplanner.street.model.vertex;

import org.opentripplanner.framework.i18n.I18NString;
import org.opentripplanner.routing.graph.Graph;

public class StreetVertexWithElevation extends StreetVertex {

  private final Double elevation;

  public StreetVertexWithElevation(Graph g, String label, double x, double y, I18NString name) {
    this(g, label, x, y, null, name);
  }

  public StreetVertexWithElevation(
    Graph g,
    String label,
    double x,
    double y,
    Double z,
    I18NString name
  ) {
    super(g, label, x, y, name);
    this.elevation = z;
  }

  @Override
  public Double getElevation() {
    return elevation;
  }
}
