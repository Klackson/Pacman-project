package logic;


import java.util.ArrayList;
import java.util.TreeMap;
import java.util.concurrent.RecursiveAction;

import static logic.AI.*;

public class ParallelTreeSearch  extends RecursiveAction
{
    static final int maxdepth = 4;
    BeliefState bstate;
    int currentdepth;
    BeliefState originalstate;
    float hvalue = Float.MIN_VALUE;

    ParallelTreeSearch (BeliefState bstate, int currentdepth, BeliefState originalstate, TreeMap<BeliefState, Float> memory)
    {
        this.bstate = bstate;
        this.currentdepth = currentdepth;
        this.originalstate = originalstate;
    }

    protected void compute (){
        if (currentdepth == maxdepth || bstate.getLife()==0) hvalue = heuristic(bstate, originalstate);

        else if(memory.containsKey(bstate)) hvalue = memory.get(bstate);

        else{
            ArrayList<Result> results = bstate.extendsBeliefState().results;
            for (Result result : results){
                ArrayList<BeliefState> beliefchildren= result.getBeliefStates();

                ArrayList<ParallelTreeSearch> TreeChildren = new ArrayList<>();
                for(BeliefState beliefchild : beliefchildren){
                    TreeChildren.add(new ParallelTreeSearch(beliefchild, currentdepth+1, originalstate, memory));
                }

                invokeAll(TreeChildren);
                ArrayList<Float> children_values = new ArrayList<>();
                for(ParallelTreeSearch child : TreeChildren){
                    children_values.add(child.hvalue);
                }
                float actionvalue = aggregateValues(children_values);
                if(actionvalue > hvalue) hvalue = actionvalue;
            }
            memory.put(bstate, hvalue);
        }
    }
}


