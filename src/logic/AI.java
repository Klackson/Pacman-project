package logic;

import java.util.*;

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

	public static String[] pacmanactions = new String[]{"UP","LEFT","RIGHT","DOWN"};
	
	// We want our memory to work on the most general case as possible, and since results can be much more diverse than beliefstates, we use belifstates as keys to our memory
	public static TreeMap<String,Float> memory = new TreeMap<String, Float>();
	
	// Parameters to fiddle with

	static int memory_refresh_rate = 2;
	static int maxdepth = 3;
	static String aggregate_method = "mean";
	static int value_per_life = 15;
	static int death_penatly = 1000;

	static final int turnback_penalty = 98;

	static int number_of_moves = 0;

	static final float max_expand=10;
	
	
	public static float heuristic (BeliefState bstate) {
		if (bstate.getLife()==0) return 0;
		// Very  primitive heuristic, we only consider current score and wether pacman is alive or dead
		float hscore = 0;
		hscore+= bstate.getScore();
		//hscore+= bstate.getLife() * value_per_life; No point in that as long as we only have a single life
		if (bstate.getLife()==0) hscore -= death_penatly;

		return hscore;
	}
	
	
	// This method will define what function we use in order to compute the values from several children (originally leaves)
	public static float aggregateValues(ArrayList<Float> values) {
		if(values.isEmpty())return 0;
		// Mean version
		if (aggregate_method=="mean") {
			float sum= 0;
			for(Float value : values) {
				sum+=value;
			}
			float v = sum/values.size();
			return v;
		}
		
		else if (aggregate_method=="min"){
			return Collections.min(values);
		}
		return 0;
	}

	public static boolean opposite_direction(String direction1, char direction2){
        return (direction1 == "UP" && direction2== 'D') ||
                (direction1 == "DOWN" && direction2 == 'U') ||
                (direction1 == "RIGHT" && direction2 == 'L') ||
                (direction1 == "LEFT" && direction2 == 'R');
    }
	
	
	// As we save values based on beliefstates and not on results, had to rework this function to work on  beliefstates
	public static float treesearch(BeliefState bstate, int currentdepth) {
		float childvalue;
		ArrayList<BeliefState> beliefchildren;
		ArrayList<Float> children_values;
		float nbchildren;
		float expand_proba;
		Result result;
		
		// If we reach a leaf, stop expanding and return the heuristic value
		if(currentdepth == maxdepth) return heuristic(bstate);
		
		float child_value;
		float max_actionvalue = Float.MIN_VALUE;

		Plans plans = bstate.extendsBeliefState();
		for(int i=0; i<plans.size(); i++){
			result = plans.getResult(i);
			beliefchildren = result.getBeliefStates();

			nbchildren = beliefchildren.size();
			expand_proba = max_expand / nbchildren;

			children_values = new ArrayList<Float>();

			for(BeliefState beliefchild : beliefchildren){
				if(Math.random()> expand_proba)continue;

				if(memory.containsKey(beliefchild.toString())) {
					// If this beliefstate has already been considered, output the already known answer
					child_value =  memory.get(beliefchild.toString());
				}
				else if(beliefchild.getLife()==0){
					child_value = heuristic(beliefchild);
					memory.put(beliefchild.toString(), child_value);
				}
				else {
					child_value = treesearch(beliefchild, currentdepth + 1);
					memory.put(beliefchild.toString(), child_value); // Obviously, save that result in the memory
				}
				children_values.add(child_value);
			}

			float actionvalue = aggregateValues(children_values);
			/*if (opposite_direction(plans.getAction(i).get(0), bstate.getPacmanPos().getDirection() )) {
				actionvalue -= turnback_penalty;
				System.out.println("U-turn detected");
			}*/
			if(actionvalue > max_actionvalue) max_actionvalue = actionvalue;
		}

		return max_actionvalue;
	}
	
	/**
	 * function that compute the next action to do (among UP, DOWN, LEFT, RIGHT)
	 * @param beliefState the current belief-state of the agent
	 * @param depth the depth of the search (size of the largest sequence of action checked)
	 * @return a string describing the next action (among PacManLauncher.UP/DOWN/LEFT/RIGHT)
	 */
	public static String findNextMove(BeliefState beliefState) {
		if(number_of_moves%memory_refresh_rate==0) memory = new TreeMap<String, Float>(); //Tried reseting memory at every new search to reduce containsKey runtime : not a good idea
		Plans plans = beliefState.extendsBeliefState();
		
		float max_utility = Integer.MIN_VALUE;
		float plan_utility;
		String chosen_action = PacManLauncher.LEFT;
		ArrayList<Float> belief_utilities;
		
		// We are going to expand all possible beliefstates possible after each action and select the action with the best aggregate utility
		for(int i=0; i<plans.size(); i++){
			Result plan_result = plans.getResult(i);
			String plan_action = plans.getAction(i).get(0);

			belief_utilities = new ArrayList<Float>();
			
			for(BeliefState bstate : plan_result.getBeliefStates()) {
				belief_utilities.add(treesearch(bstate, 0));
			}			
			plan_utility = aggregateValues(belief_utilities);

			if (opposite_direction(plan_action, beliefState.getPacmanPos().getDirection() )) {
				plan_utility -= turnback_penalty;
			}
			
			if(plan_utility > max_utility){
				max_utility = plan_utility;
				chosen_action = plan_action;
			}
		}
		number_of_moves++;
		return chosen_action;
	}
}