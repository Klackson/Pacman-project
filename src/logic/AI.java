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

	// We want our memory to work on the most general case as possible, and since results can be much more diverse than beliefstates, we use belifstates as keys to our memory
	public static TreeMap<String,Float> memory = new TreeMap<String, Float>();
	
	// Parameters to fiddle with

	static final int memory_refresh_rate = 3; // This value HAS to be below maxdepth otherwise pacman literally won't see ghosts coming
	static final int maxdepth = 4;
	static final String aggregate_method = "mean";
	static final float death_penatly = 1000;
	static final float turnback_penalty = 150;
	static final float max_expand = 5;
	static final int gom_distance_weight = 10;


	static int number_of_moves = 0;

	public static float heuristic (BeliefState bstate, int original_nbgoms) {
		//if (bstate.getLife()==0) return 0;
		// Very  primitive heuristic, we only consider current score and wether pacman is alive or dead
		float hscore = 0;
		hscore+= bstate.getScore();
		//hscore-= (float) bstate.getNbrOfGommes();

		if (bstate.getLife()==0) hscore -= death_penatly;
		hscore -= gom_distance_weight * nearest_gom_distance(bstate, original_nbgoms);

		return hscore;
	}
	
	
	// This method will define what function we use in order to compute the values from several children (originally leaves)
	public static float aggregateValues(ArrayList<Float> values) {
		if(values.isEmpty())return 0;
		// Mean version
		if (Objects.equals(aggregate_method, "mean")) {
			float sum= 0;
			for(Float value : values) {
				sum+=value;
			}
            return sum/values.size();
		}
		
		else if (Objects.equals(aggregate_method, "min")){
			return Collections.min(values);
		}
		return 0;
	}

	// Checks if two directions are opposite
	// Is used as a local heuristic to penalize U-turns
	public static boolean opposite_direction(String direction1, char direction2){
        return (Objects.equals(direction1, "UP") && direction2== 'D') ||
                (Objects.equals(direction1, "DOWN") && direction2 == 'U') ||
                (Objects.equals(direction1, "RIGHT") && direction2 == 'L') ||
                (Objects.equals(direction1, "LEFT") && direction2 == 'R');
    }

	public static boolean same_direction(String direction1, char direction2){
		return (Objects.equals(direction1, "UP") && direction2== 'U') ||
				(Objects.equals(direction1, "DOWN") && direction2 == 'D') ||
				(Objects.equals(direction1, "RIGHT") && direction2 == 'R') ||
				(Objects.equals(direction1, "LEFT") && direction2 == 'L');
	}

	// Checks for the distance to the nearest existing gom
	// Is used in the heuristic to incentivize going towards goms
	public static int nearest_gom_distance(BeliefState bstate, int original_nbgoms){
		int current_nbgoms = bstate.getNbrOfGommes();

		if(current_nbgoms==0) return -1000; // -10000 is actually a big bonus (since we substract this result in the heuristic) and we give if the map is completed

		if(current_nbgoms < original_nbgoms) return -1;

		Position pacmanpos = bstate.getPacmanPosition();
		int pacmanx = pacmanpos.x; int pacmany = pacmanpos.y;

		char[][] map = bstate.getMap();

		int max_search_distance = 25;
		int i; int k; int j;
		for(k=1; k<= max_search_distance; k++){
			for(i=-k; i<=k; i++){
				for(j=-k; j<=k; j++){
					if(pacmanx + i >= 25 ||
					pacmanx + i < 0 ||
					pacmany + j >= 25 ||
					pacmany +j < 0)continue;
					if( map[pacmanx + i][pacmany + j] =='.') {
						return Math.abs(i) + Math.abs(j);
					}
				}
			}
		}
		System.out.println("gom not found. Move "+ number_of_moves);
		return 2 * max_search_distance;
	}
	
	
	// As we save values based on beliefstates and not on results, had to rework this function to work on  beliefstates
	public static float treesearch(BeliefState bstate, int currentdepth, int original_nbgoms) {

		// If we reach a leaf, stop expanding and return the heuristic value
		if(currentdepth == maxdepth) return heuristic(bstate, original_nbgoms);

		ArrayList<BeliefState> beliefchildren;
		ArrayList<Float> children_values;
		float nbchildren;
		float expand_proba;
		Result result;

		float child_value;
		float max_actionvalue = Float.MIN_VALUE;
		float actionvalue;

		Plans plans = bstate.extendsBeliefState();

		for(int i=0; i<plans.size(); i++){

			if (opposite_direction(plans.getAction(i).get(0), bstate.getPacmanPos().getDirection()) && plans.getAction(i).size()==1) continue;

			result = plans.getResult(i);
			beliefchildren = result.getBeliefStates();

			nbchildren = beliefchildren.size();
			expand_proba = max_expand / nbchildren;

			children_values = new ArrayList<Float>();

			for(BeliefState beliefchild : beliefchildren){
				// To ease the computation burden, we don't compute all of the expanded beliefstates
				// We randomly skip some of them depending on how many there are.
				// The expected number of children expanded is close to the minimum between the number of children and the parameter max_expand.
				if(Math.random()> expand_proba)continue;

				if(beliefchild.getLife()==0){
					child_value = heuristic(beliefchild, original_nbgoms);
					//memory.put(beliefchild.toString(), child_value);
				}

				else if(memory.containsKey(beliefchild.toString())) {
					// If this beliefstate has already been considered, output the already known answer
					child_value =  memory.get(beliefchild.toString());
				}

				else {
					child_value = treesearch(beliefchild, currentdepth + 1, original_nbgoms);
					memory.put(beliefchild.toString(), child_value); // Obviously, save that result in the memory
				}
				children_values.add(child_value);
			}

			if(children_values.isEmpty()){
				actionvalue = heuristic(bstate, original_nbgoms);
			}

			else actionvalue = aggregateValues(children_values);

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
		if(number_of_moves % memory_refresh_rate==0) memory = new TreeMap<String, Float>();
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
				belief_utilities.add(treesearch(bstate, 0, bstate.getNbrOfGommes()));
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