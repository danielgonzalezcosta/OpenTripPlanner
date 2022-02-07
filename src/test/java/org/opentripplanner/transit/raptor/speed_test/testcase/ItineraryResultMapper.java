package org.opentripplanner.transit.raptor.speed_test.testcase;

import java.util.Optional;
import org.opentripplanner.model.StopLocation;
import org.opentripplanner.model.plan.Itinerary;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import org.opentripplanner.model.plan.Leg;
import org.opentripplanner.transit.raptor.util.PathStringBuilder;
import org.opentripplanner.util.time.TimeUtils;

/**
 * Map an Itinerary to a result instance. We do this to normalize the Itinerary
 * for the purpose of testing, and serialization of the results.
 * <p/>
 * This way we do not need to change the Itinerary class to fit our needs and
 * we avoid the 'feature envy' anti pattern.
 */
class ItineraryResultMapper {
    private static final Map<String, String> AGENCY_NAMES_SHORT = new HashMap<>();

    private final boolean skipCost;
    private final String testCaseId;

    static {
        AGENCY_NAMES_SHORT.put("Agder flyekspress", "AgderFly");
        AGENCY_NAMES_SHORT.put("Agder Kollektivtrafikk as", "Agder");
        AGENCY_NAMES_SHORT.put("AtB", "AtB");
        AGENCY_NAMES_SHORT.put("Avinor", "Avinor");
        AGENCY_NAMES_SHORT.put("Farte", "Farte");
        AGENCY_NAMES_SHORT.put("FlixBus", "FlexBus");
        AGENCY_NAMES_SHORT.put("Flybussen Norgesbuss", "Flybussen");
        AGENCY_NAMES_SHORT.put("Flytoget", "FLY");
        AGENCY_NAMES_SHORT.put("Flåmsbana", "FLÅ");
        AGENCY_NAMES_SHORT.put("Hedmark Trafikk", "HED");
        AGENCY_NAMES_SHORT.put("Møre og Romsdal fylkeskommune", "M&R");
        AGENCY_NAMES_SHORT.put("NOR-WAY Bussekspress", "N-W");
        AGENCY_NAMES_SHORT.put("Ruter", "RUT");
        AGENCY_NAMES_SHORT.put("SJ AB", "SJ");
        AGENCY_NAMES_SHORT.put("Skyss", "SKY");
        AGENCY_NAMES_SHORT.put("Snelandia", "Snelandia");
        AGENCY_NAMES_SHORT.put("Troms fylkestrafikk", "Troms");
        AGENCY_NAMES_SHORT.put("Unibuss Ekspress", "Unibuss");
        AGENCY_NAMES_SHORT.put("Vestfold Kollektivtrafikk", "VF");
        AGENCY_NAMES_SHORT.put("Vy", "Vy");
        AGENCY_NAMES_SHORT.put("Vy express", "VyEx");
        AGENCY_NAMES_SHORT.put("N/A", "DummyEUR");

        // Old agencies (2019)
        AGENCY_NAMES_SHORT.put("Hedmark Trafikk FKF", "HED");
        AGENCY_NAMES_SHORT.put("Nord-Trøndelag fylkeskommune", "NTrø");
        AGENCY_NAMES_SHORT.put("Nordland fylkeskommune", "Nordld");
        AGENCY_NAMES_SHORT.put("Norgesbuss Ekspress AS", "NorBuss");
        AGENCY_NAMES_SHORT.put("Opplandstrafikk", "OPP");
        AGENCY_NAMES_SHORT.put("Vestfold Kollektivtrafikk as", "VF");
        AGENCY_NAMES_SHORT.put("Østfold fylkeskommune", "ØstFyl");
        AGENCY_NAMES_SHORT.put("Østfold kollektivtrafikk", "ØstKol");
    }

    private ItineraryResultMapper(boolean skipCost, String testCaseId) {
        this.skipCost = skipCost;
        this.testCaseId = testCaseId;
    }

    static Collection<Result> map(final String testCaseId, Collection<org.opentripplanner.model.plan.Itinerary> itineraries, boolean skipCost) {
        var mapper = new ItineraryResultMapper(skipCost, testCaseId);
        return itineraries.stream().map(mapper::map).collect(Collectors.toList());
    }

    private Result map(Itinerary itinerary) {
        Result result = new Result(
                testCaseId,
                itinerary.nTransfers,
                itinerary.durationSeconds,
                itinerary.generalizedCost,
                itinerary.legs.stream().filter(Leg::isWalkingLeg).mapToInt(l -> l.getDistanceMeters().intValue()).sum(),
                TimeUtils.localTime(itinerary.startTime()).toSecondOfDay(),
                TimeUtils.localTime(itinerary.endTime()).toSecondOfDay(),
                details(itinerary)
        );

        for (Leg it : itinerary.legs) {
            if (it.isTransitLeg()) {
                var route = Optional.ofNullable(it.getRoute().getShortName()).orElse(it.getRoute().getLongName());
                result.agencies.add(AGENCY_NAMES_SHORT.getOrDefault(it.getAgency().getName(), it.getAgency().getName()));
                result.modes.add(it.getMode());
                result.routes.add(route);
            }
        }
        return result;
    }

    public static String details(Itinerary itin) {
        PathStringBuilder buf = new PathStringBuilder(Integer::toString, true);

        for (Leg leg : itin.legs) {

            Optional.ofNullable(leg.getFrom().stop).map(ItineraryResultMapper::formatStop).map(id -> buf.sep().stop(id).sep());

            if(leg.isWalkingLeg()) {
                buf.walk((int) leg.getDuration());
            }
            else if(leg.isTransitLeg()) {
                buf.transit(
                        leg.getMode().name() + " " + leg.getRoute().getShortName(),
                        TimeUtils.localTime(leg.getStartTime()).toSecondOfDay(),
                        TimeUtils.localTime(leg.getEndTime()).toSecondOfDay());
            }
        }
        return buf.toString();
    }

    private static String formatStop(StopLocation s) {
        return s.getName() + "(" + s.getId().getId() + ")";
    }
}
