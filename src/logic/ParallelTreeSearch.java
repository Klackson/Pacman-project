package logic;


import java.util.ArrayList;
import java.util.Collections;
import java.util.Objects;
import java.util.TreeMap;
import java.util.concurrent.RecursiveAction;

import static logic.AI.*;

public class ParallelTreeSearch  extends RecursiveAction {
    BeliefState bstate;
    Result result;
    int currentdepth;
    BeliefState originalstate;
    float hvalue = Float.MIN_VALUE;
    String nodetype;

    ParallelTreeSearch (BeliefState bstate, int currentdepth, BeliefState originalstate, TreeMap<BeliefState, Float> memory)
    {
        this.bstate = bstate;
        this.currentdepth = currentdepth;
        this.originalstate = originalstate;
        nodetype="OR";
    }

    ParallelTreeSearch (Result result, int currentdepth, BeliefState originalstate, TreeMap<BeliefState, Float> memory)
    {
        this.result = result;
        this.currentdepth = currentdepth;
        this.originalstate = originalstate;
        nodetype="AND";
    }

    protected void compute (){
        if(Objects.equals(nodetype, "OR")) {
            if (currentdepth == AI.maxdepth || bstate.getLife() == 0) hvalue = heuristic(bstate, originalstate);

            else if (state_in_memory(memory, bstate)) hvalue = get_memory_value(memory, bstate);

            else {
                ArrayList<Result> results = bstate.extendsBeliefState().results;
                ArrayList<ParallelTreeSearch> result_children = new ArrayList<>();

                for (Result result : results) {
                    result_children.add(new ParallelTreeSearch(result, currentdepth, originalstate, memory));
                }

                invokeAll(result_children); // Recursive parallel call

                ArrayList<Float> children_values = new ArrayList<>();
                for (ParallelTreeSearch child : result_children) {
                    children_values.add(child.hvalue);
                }
                hvalue = Collections.max(children_values);
                }
            save_result(memory, bstate, hvalue);
        }

        else{
            ArrayList<BeliefState> states = result.getBeliefStates();
            ArrayList<ParallelTreeSearch> belief_children = new ArrayList<>();

            for(BeliefState bstate : states){
                belief_children.add(new ParallelTreeSearch(bstate, currentdepth+1, originalstate, memory));
            }

            invokeAll(belief_children);

            ArrayList<Float> children_values = new ArrayList<>();
            for (ParallelTreeSearch child : belief_children) {
                children_values.add(child.hvalue);
            }
            hvalue = aggregateValues(children_values);
        }
    }

}

