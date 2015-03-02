/* This program is free software: you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public License
 as published by the Free Software Foundation, either version 3 of
 the License, or (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>. */

package org.opentripplanner.routing.spt;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.graph.Vertex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Note that turn restrictions make all searches multi-state; however turn restrictions do not apply when walking.
 * The turn restriction handling should be done in the SPT implementation, independent of the dominance function.
 * It essentially splits each vertex into N vertices depending on the incoming edge being taken.
 */
public class MultiStateShortestPathTree extends ShortestPathTree {
	
    private static final Logger LOG = LoggerFactory.getLogger(MultiStateShortestPathTree.class);

    private Map<Vertex, List<State>> stateSets;

    public void dump() {
        Multiset<Integer> histogram = HashMultiset.create();
        int statesCount = 0;
        int maxSize = 0;
        for (Map.Entry<Vertex, List<State>> kv : stateSets.entrySet()) {
            List<State> states = kv.getValue();
            int size = states.size();
            histogram.add(size);
            statesCount += size;
            if (size > maxSize) {
                maxSize = size;
            }
        }
        LOG.info("SPT: vertices: " + stateSets.size() + " states: total: "
                + statesCount + " per vertex max: " + maxSize + " avg: "
                + (statesCount * 1.0 / stateSets.size()));
        List<Integer> nStates = new ArrayList<Integer>(histogram.elementSet());
        Collections.sort(nStates);
        for (Integer nState : nStates) {
            LOG.info(nState + " states: " + histogram.count(nState) + " vertices.");
        }
    }

    public MultiStateShortestPathTree (RoutingRequest options, DominanceFunction dominanceFunction) {
        super(options, dominanceFunction);
        stateSets = new IdentityHashMap<Vertex, List<State>>();
    }

    public Set<Vertex> getVertices() {
        return stateSets.keySet();
    }

    @Override
    public boolean add(State newState) {
        Vertex vertex = newState.getVertex();
        List<State> states = stateSets.get(vertex);
        
        // if the vertex has no states, add one and return
        if (states == null) {
            states = new ArrayList<State>();
            stateSets.put(vertex, states);
            states.add(newState);
            return true;
        }
        
        // if the vertex has any states that dominate the new state, don't add the state
        // if the new state dominates any old states, remove them
        Iterator<State> it = states.iterator();
        while (it.hasNext()) {
            State oldState = it.next();
            // order is important, because in the case of a tie
            // we want to reject the new state
            if (dominanceFunction.dominates(oldState, newState))
                return false;
            if (dominanceFunction.dominates(newState, oldState))
                it.remove();
        }
        
        // any states remaining are co-dominant with the new state
        states.add(newState);
        return true;
    }

    @Override
    public State getState(Vertex dest) {
        Collection<State> states = stateSets.get(dest);
        if (states == null)
            return null;
        State ret = null;
        // TODO are we only checking path parser acceptance when we fetch states via this specific method?
        for (State s : states) {
            if ((ret == null || s.weight < ret.weight) && s.isFinal() && s.allPathParsersAccept()) {
                ret = s;
            }
        }
        return ret;
    }

    @Override
    public List<State> getStates(Vertex dest) {
        return stateSets.get(dest);
    }

    @Override
    public int getVertexCount() {
        return stateSets.keySet().size();
    }

    /**
     * Check that a state coming out of the queue is still in the Pareto-optimal set for this vertex, 
     * which indicates that it has not been ruled out as a state on an optimal path. Many shortest 
     * path algorithms will decrease the key of an entry in the priority queue when it is updated, or
     * remove it when it is dominated.
     * 
     * When the Fibonacci heap was replaced with a binary heap, the decrease-key operation was 
     * removed for the same reason: both improve theoretical run time complexity, at the cost of 
     * high constant factors and more complex code.
     * 
     * So there can be dominated (useless) states in the queue. When they come out we want to 
     * ignore them rather than spend time branching out from them.
     */
    @Override
    public boolean visit(State state) {
        boolean ret = false;
        for (State s : stateSets.get(state.getVertex())) {
            if (s == state) {
                ret = true;
                break;
            }
        }
        return ret;
    }

    public String toString() {
        return "MultiSPT(" + this.stateSets.size() + " vertices)";
    }

    @Override
    public Collection<State> getAllStates() {
        ArrayList<State> allStates = new ArrayList<State>();
        for (List<State> stateSet : stateSets.values()) {
            allStates.addAll(stateSet);
        }
        return allStates;
    }

}