package logic;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.TreeSet;

import view.Gomme;


/**
 * class used to represent plan. It will provide for a given set of results an action to perform in each result
 */
class Plans{
	ArrayList<Result> results;
	ArrayList<ArrayList<String>> actions;
	
	/**
	 * construct an empty plan
	 */
	public Plans() {
		this.results = new ArrayList<Result>();
		this.actions = new ArrayList<ArrayList<String>>();
	}
	
	/**
	 * add a new pair of belief-state and corresponding (equivalent) actions 
	 * @param beliefBeliefState the belief state to add
	 * @param action a list of alternative actions to perform. Only one of them is chosen but their results should be similar
	 */
	public void addPlan(Result beliefBeliefState, ArrayList<String> action) {
		this.results.add(beliefBeliefState);
		this.actions.add(action);
	}
	
	/**
	 * return the number of belief-states/actions pairs
	 * @return the number of belief-states/actions pairs
	 */
	public int size() {
		return this.results.size();
	}
	
	/**
	 * return one of the belief-state of the plan
	 * @param index index of the belief-state
	 * @return the belief-state corresponding to the index
	 */
	public Result getResult(int index) {
		return this.results.get(index);
	}
	
	/**
	 * return the list of actions performed for a given belief-state
	 * @param index index of the belief-state
	 * @return the set of actions to perform for the belief-state corresponding to the index
	 */
	public ArrayList<String> getAction(int index){
		return this.actions.get(index);
	}
}

/**
 * class used to represent a transition function i.e., a set of possible belief states the agent may be in after performing an action
 */
class Result{
	private ArrayList<BeliefState> beliefStates;

	/**
	 * construct a new result
	 * @param states the set of states corresponding to the new belief state
	 */
	public Result(ArrayList<BeliefState> states) {
		this.beliefStates = states;
	}

	/**
	 * returns the number of belief states
	 * @return the number of belief states
	 */
	public int size() {
		return this.beliefStates.size();
	}

	/**
	 * return one of the belief state
	 * @param index the index of the belief state to return
	 * @return the belief state to return
	 */
	public BeliefState getBeliefState(int index) {
		return this.beliefStates.get(index);
	}
	
	/**
	 * return the list of belief-states
	 * @return the list of belief-states
	 */
	public ArrayList<BeliefState> getBeliefStates(){
		return this.beliefStates;
	}
}


/**
 * class implement the AI to choose the next move of the Pacman
 */
public class AI{
	
	public HashMap<Result,Float> memory; 
	
	// Parameters to fiddle with
	public static int maxdepth = 5;
	public static String aggregate_method = "mean";
	public static int value_per_life = 15;
	public static int death_penatly = 10;
	
	public static int heuristic (Result result) {
		
		// Very  primitive heuristic, we only consider current score
		int hscore = 0;
		for(BeliefState bstate : result.getBeliefStates()) {
			hscore+= bstate.getScore();
			//hscore+= bstate.getLife() * value_per_life; No point in that as long as we only have a single life
			if (bstate.getLife()==0) hscore -= death_penatly;
		}

		return hscore;
	}
	
	public static float aggregateValues(ArrayList<Float> values) {
		
		// Mean version
		if (aggregate_method=="mean") {
			float sum=0;
			for(float value : values) {
				sum+=value;
			}
			return sum/values.size();
		}
		else if (aggregate_method=="min"){
			float min = Integer.MAX_VALUE;
			for (float value : values){
				if(value < min) min = value;
			}
			return min;
		}
		return 0;
	}
	
	public static float treesearch(Result result, int currentdepth) {
		//should this work with results or belief states ?
		
		if(currentdepth == maxdepth) return heuristic(result);
		
		ArrayList<Float> children_values = new ArrayList<Float>();
		
		for(BeliefState bstate : result.getBeliefStates()) {
			
			ArrayList<Result> results = bstate.extendsBeliefState().results;
			for(Result child : results){
				children_values.add( treesearch(child, currentdepth+1) );
			}
			
		}

		return aggregateValues(children_values);
	}
	
	/**
	 * function that compute the next action to do (among UP, DOWN, LEFT, RIGHT)
	 * @param beliefState the current belief-state of the agent
	 * @param depth the depth of the search (size of the largest sequence of action checked)
	 * @return a string describing the next action (among PacManLauncher.UP/DOWN/LEFT/RIGHT)
	 */
	public static String findNextMove(BeliefState beliefState) {

		Plans plans = beliefState.extendsBeliefState();

		float max_utility = Integer.MIN_VALUE;
		float plan_utility;
		String chosen_action = PacManLauncher.LEFT;

		for(int i=0; i<plans.size(); i++){
			plan_utility = treesearch(plans.getResult(i), 0);
			if(plan_utility > max_utility){
				max_utility = plan_utility;
				chosen_action = plans.getAction(i).get(0);
			}
		}

		return chosen_action;
	}
}