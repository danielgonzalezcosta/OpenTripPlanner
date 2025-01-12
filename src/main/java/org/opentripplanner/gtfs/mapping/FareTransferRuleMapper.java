package org.opentripplanner.gtfs.mapping;

import static org.opentripplanner.gtfs.mapping.AgencyAndIdMapper.mapAgencyAndId;
import static org.opentripplanner.gtfs.mapping.AgencyAndIdMapper.mapNullableId;

import java.time.Duration;
import java.util.Collection;
import java.util.Objects;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueStore;
import org.opentripplanner.model.fare.FareTransferRule;

public class FareTransferRuleMapper {

  public final int MISSING_VALUE = -999;

  private final DataImportIssueStore issueStore;
  private final FareProductMapper fareProductMapper;

  public FareTransferRuleMapper(
    FareProductMapper fareProductMapper,
    DataImportIssueStore issueStore
  ) {
    this.fareProductMapper = fareProductMapper;
    this.issueStore = issueStore;
  }

  public Collection<FareTransferRule> map(
    Collection<org.onebusaway.gtfs.model.FareTransferRule> allRules
  ) {
    return allRules.stream().map(this::doMap).filter(Objects::nonNull).toList();
  }

  private FareTransferRule doMap(org.onebusaway.gtfs.model.FareTransferRule rhs) {
    var fareProductId = mapAgencyAndId(rhs.getFareProductId());

    return fareProductMapper
      .getByFareProductId(fareProductId)
      .map(p -> {
        Duration duration = null;
        if (rhs.getDurationLimit() != MISSING_VALUE) {
          duration = Duration.ofSeconds(rhs.getDurationLimit());
        }
        return new FareTransferRule(
          mapNullableId(rhs.getFromLegGroupId()),
          mapNullableId(rhs.getToLegGroupId()),
          rhs.getTransferCount(),
          duration,
          p
        );
      })
      .orElseGet(() -> {
        issueStore.add(
          "UnknownFareProductId",
          "Fare product with id %s referenced by fare transfer rule with id %s not found.".formatted(
              fareProductId,
              rhs.getId()
            )
        );
        return null;
      });
  }
}
