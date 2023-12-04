package logic;

import java.util.ArrayList;
import java.util.HashSet;
//import java.util.HashMap;
import java.util.Iterator;
import java.util.TreeSet;

import data.Map;
import view.Gomme;

/**
 * an object Position correspond to a position in the Pacman grid
 */
class Position implements Comparable{
	public int x, y;
	public char dir;

	/**
	 * construct a new Object position corresponding to the position of an entity (ghost or pacman) in the grid
	 * @param x row
	 * @param y column
	 * @param dir direction followed by the entity
	 */
	public Position(int x, int y, char dir) {
		this.x = x;
		this.y = y;
		this.dir = dir;
	}
	
	/**
	 * return the row index
	 * @return the row index
	 */
	int getRow() {
		return this.x;
	}
	
	/**
	 * return the column index
	 * @return the column index
	 */
	int getColumn() {
		return this.y;
	}
	
	/**
	 * return direction (among 'U', 'D', 'L', 'R')
	 * @return
	 */
	char getDirection() {
		return this.dir;
	}

	public String toString() {
		return "(" + this.x + "," + this.y + ") " + this.dir;
	}

	/**
	 * construct a copie of a given position
	 * @param pos
	 */
	public Position clone() {
		return new Position(this.x, this.y, this.dir);
	}
	
	
	/**
	 * used to compare two positions
	 * @return 0 if the two positions are the same
	 */
	public int compareTo(Object o) {
		Position pos = (Position)o;
		int comp = this.x - pos.x;
		if(comp != 0)
			return comp;
		comp = this.y - pos.y;
		if(comp != 0)
			return comp;
		comp = this.dir - pos.dir;
		if(comp != 0)
			return comp;
		return 0; 
	}
}

/**
 * an object BeliefState represents all relevant information about the game.
 */
public class BeliefState implements Comparable{
	private char map[][];
	private ArrayList</*HashMap<String, Position>*/TreeSet<Position>> listPGhost;
	private Position pacmanPos;
	private int nbrOfGommes, nbrOfSuperGommes, score, life;
	private ArrayList<Integer> compteurPeur;
	private Map theMap;
	
	/**
	 * create a new BeliefState object
	 * @param theMap the current version of the Pacman man discribing the position of (super) gums
	 * @param score the current score
	 * @param life the number of remaining lifes for Pacman
	 */
	public BeliefState(Map theMap, int score, int life) {
		this.map = new char[theMap.getNbCases()][theMap.getNbCases()];
		this.pacmanPos = new Position(0,0,'U');
		this.listPGhost = new ArrayList</*HashMap<String, Position>*/TreeSet<Position>>();
		this.nbrOfGommes = 0;
		this.score = score;
		this.compteurPeur = new ArrayList<Integer>();
		this.theMap = theMap;
		this.life = life;
	}
	
	public int compareTo(Object o) {
		BeliefState bs = (BeliefState) o;
		int comp = this.pacmanPos.compareTo(bs.pacmanPos);
		if(comp != 0)
			return comp;
		comp = this.life - bs.life;
		if(comp != 0)
			return comp;
		comp = this.score - bs.score;
		if(comp != 0)
			return comp;
		comp = this.nbrOfGommes - bs.nbrOfGommes;
		if(comp != 0)
			return comp;
		comp = this.nbrOfSuperGommes - bs.nbrOfSuperGommes;
		if(comp != 0)
			return comp;
		for(int[] pos:this.theMap.getGamePosition()) {
			comp = this.map[pos[0]][pos[1]] - bs.map[pos[0]][pos[1]];
			if(comp != 0)
				return comp;
		}
		for(int i = 0; i < this.compteurPeur.size(); i++) {
			comp = this.compteurPeur.get(i) - bs.compteurPeur.get(i);
			if(comp != 0)
				return comp;
		}
		for(int i = 0; i < this.listPGhost.size(); i++) {
			/*HashMap<String, Position>*/TreeSet<Position> posGhost1 = this.listPGhost.get(i), posGhost2 = bs.listPGhost.get(i);
			comp = posGhost1.size() - posGhost2.size();
			if(comp != 0)
				return comp;
			Iterator<Position> iterPos1 = posGhost1.descendingIterator(), iterPos2 = posGhost2.descendingIterator();
			for(Position pos1 = iterPos1.next(), pos2 = iterPos2.next(); iterPos1.hasNext(); pos1 = iterPos1.next(), pos2 = iterPos2.next()) {
				comp = pos1.compareTo(pos2);
				if(comp != 0)
					return comp;
			}
		}
		return 0;
	}
	
	/**
	 * construct a copy of the state
	 * @param toCopy BeliefState object to be copied
	 * @param isDead if true then Pacman is dead and the status of the status should be updated accordingly
	 */

	public BeliefState(BeliefState toCopy, boolean isDead) {
		this(toCopy.theMap, toCopy.score, toCopy.life);
		this.pacmanPos.dir = toCopy.pacmanPos.dir;
		for(int i = 0; i < this.map.length; i++) {
			for(int j = 0; j < this.map[i].length; j++) {
				this.modifyMap(i, j, toCopy.map[i][j]);
			}
		}
		if(!isDead) {
			this.listPGhost.clear();
			for(/*HashMap<String, Position>*/TreeSet<Position> listP: toCopy.listPGhost) {
				//HashMap<String, Position> newListP = new HashMap<String, Position>();
				TreeSet<Position> newListP = new TreeSet<Position>();
				for(Position pos: listP.descendingSet()) {
					newListP.add(/*pos.toString(), */pos.clone());
				}
				this.listPGhost.add(newListP);
			}
			for(int i = 0; i < toCopy.compteurPeur.size(); i++) {
				this.compteurPeur.set(i, toCopy.compteurPeur.get(i));
			}
		}
		else {
			this.life = toCopy.life - 1;
			this.moveTo(this.theMap.getPMY() / this.theMap.getTailleCase(), this.theMap.getPMX() / this.theMap.getTailleCase(), 'U');
		}
	}

	/**
	 * update the status of one square
	 * @param i row of the square
	 * @param j column of the square
	 * @param val value coressponding to the content of the square
	 */
	public void modifyMap(int i, int j, char val) {
		switch(val) {
		case '.': nbrOfGommes++; break;
		case '*': nbrOfGommes++; nbrOfSuperGommes++; break;
		case 'P': this.pacmanPos.x = i;this.pacmanPos.y = j; break;
		case 'F': /*HashMap<String, Position>*/TreeSet<Position> posGhost = new /*HashMap<String, Position>*/TreeSet<Position>(); Position pos = new Position(i, j, 'U'); posGhost.add(/*pos.toString(),*/ pos); this.listPGhost.add(posGhost); this.compteurPeur.add(0); /*this.isBlocked.add(false);*/ break;
		case 'B': this.pacmanPos.x = i;this.pacmanPos.y = j; /*HashMap<String, Position>*/TreeSet<Position> posGhost2 = new /*HashMap<String, Position>*/TreeSet<Position>(); Position pos2 = new Position(i, j, 'U'); posGhost2.add(/*pos2.toString(),*/ pos2); this.listPGhost.add(posGhost2); this.compteurPeur.add(0); /*this.isBlocked.add(false);*/ break;
		}
		this.map[i][j] = val;
	}

	/**
	 * returns the current score
	 * @return current score
	 */
	public int getScore() {
		return this.score;
	}

	/**
	 * create all possible states resulting from a given action of Pacman
	 * @param toward describe the action performed by Pacman (PacmanLuncher.UP/DOWN/LEFT/RIGHT)
	 * @return list of possible states that can be the results of the action performed by Pacman
	 */
	public Result extendsBeliefState(String toward) {
		BeliefState stateRemoved = null;
		ArrayList<BeliefState> listAlternativeBeliefState = new ArrayList<BeliefState>();
		BeliefState currentBeliefState = null;
		char currentPos = this.map[this.pacmanPos.x][this.pacmanPos.y];
		switch(toward.charAt(0)) {
		case 'U': if(pacmanPos.x > 0) {
			char nextPos = this.map[this.pacmanPos.x - 1][this.pacmanPos.y];
			if(nextPos != '#') {
				currentBeliefState = this.move(-1, 0, nextPos, 'U');
				if(nextPos == '*') {
					for(int i = 0; i < currentBeliefState.compteurPeur.size(); i++) {
						currentBeliefState.compteurPeur.set(i, Ghost.TIME_PEUR);
					}
				}
			}
			else {
				currentBeliefState = this.move(0, 0, currentPos, 'N');
			}
		} else {
			currentBeliefState = this.move(0, 0, currentPos, 'N');
		} break;
		case 'D': if(this.pacmanPos.x + 1 < this.map.length) {
			char nextPos = this.map[this.pacmanPos.x + 1][this.pacmanPos.y];
			if(nextPos != '#') {
				currentBeliefState = this.move(1, 0, nextPos, 'D');
				if(nextPos == '*') {
					for(int i = 0; i < currentBeliefState.compteurPeur.size(); i++) {
						currentBeliefState.compteurPeur.set(i, Ghost.TIME_PEUR);
					}
				}
			}
			else {
				currentBeliefState = this.move(0, 0, currentPos, 'N');
			}
		}
		else{
			currentBeliefState = this.move(0, 0, currentPos, 'N');
		} break;
		case 'L': if(this.pacmanPos.y > 0) {
			char nextPos = this.map[this.pacmanPos.x][this.pacmanPos.y - 1];
			if(nextPos != '#') {
				currentBeliefState = this.move(0, -1, nextPos, 'L');
				if(nextPos == '*') {
					for(int i = 0; i < currentBeliefState.compteurPeur.size(); i++) {
						currentBeliefState.compteurPeur.set(i, Ghost.TIME_PEUR);
					}
				}
			}
			else {
				currentBeliefState = this.move(0, 0, currentPos, 'N');
			}
		}
		else{
			currentBeliefState = this.move(0, 0, currentPos, 'N');
		} break;
		case 'R': if(this.pacmanPos.y + 1 < this.map[0].length) {
			char nextPos = this.map[this.pacmanPos.x][this.pacmanPos.y + 1];
			if(nextPos != '#') {
				currentBeliefState = this.move(0, 1, nextPos, 'R');
				if(nextPos == '*') {
					for(int i = 0; i < currentBeliefState.compteurPeur.size(); i++) {
						currentBeliefState.compteurPeur.set(i, Ghost.TIME_PEUR);
					}
				}
			}
			else {
				currentBeliefState = this.move(0, 0, currentPos, 'N');
			}
		}
		else {
			currentBeliefState = this.move(0, 0, currentPos, 'N');
		} break;
		}

		boolean dead = false;
		int l = 0;
		for(/*HashMap<String, Position> hmap*/TreeSet<Position> treeSet: this.listPGhost) {
			if(currentBeliefState.compteurPeur.get(l++) == 0 && treeSet.size() == 1) {
				Position pos = treeSet.first();
				if(pos.x == currentBeliefState.pacmanPos.x && pos.y == currentBeliefState.pacmanPos.y) {
					dead = true;
					break;
				}
			}
		}
		if(dead) {
			listAlternativeBeliefState.add(new BeliefState(currentBeliefState, true));
		}
		else {
			listAlternativeBeliefState.add(currentBeliefState);
			for(int k = 0; k < currentBeliefState.compteurPeur.size(); k++) {
				ArrayList<BeliefState> tempListAlternativeBeliefState = new ArrayList<BeliefState>();

				for(int indexBeliefState = 0; indexBeliefState < listAlternativeBeliefState.size(); indexBeliefState++) {
					BeliefState state = listAlternativeBeliefState.get(indexBeliefState); 
					int compteurPeur = state.compteurPeur.get(k);
					if (compteurPeur > 0) {
						state.compteurPeur.set(k, compteurPeur - 2);
					}
					/*HashMap<String, Position>*/TreeSet<Position> posGhost = state.listPGhost.get(k), newPosGhost = new /*HashMap<String, Position>*/TreeSet<Position>();
					Iterator<Position> itPos = posGhost.iterator();
					HashSet<String> hAlternativePos = new HashSet<String>();
					while(itPos.hasNext()) {
						Position posG = itPos.next();
						boolean haveMoved = false;
						if(state.theMap.isVisible(posG.x, posG.y, state.pacmanPos.x, state.pacmanPos.y) && compteurPeur == 0) {
							if(posGhost.size() > 1) {
								Position newPos = posG.clone();
								BeliefState actualBeliefState = new BeliefState(state, false);
								actualBeliefState.listPGhost.get(k).clear();
								boolean isDead = false;
								if(newPos.x > state.pacmanPos.x) {
									newPos.x--;
									newPos.dir = 'U';
									if(newPos.x == state.pacmanPos.x)
										isDead = true;
								}
								else {
									if(newPos.x < state.pacmanPos.x) {
										newPos.x++;
										newPos.dir = 'D';
										if(newPos.x == state.pacmanPos.x)
											isDead = true;
									}
									else {
										if(newPos.y < state.pacmanPos.y) {
											newPos.y++;
											newPos.dir = 'R';
											if(newPos.y == state.pacmanPos.y)
												isDead = true;
										}
										else {
											if(newPos.y > state.pacmanPos.y) {
												newPos.y--;
												newPos.dir = 'L';
												if(newPos.y == state.pacmanPos.y)
													isDead = true;
											}
											else
												isDead = true;
										}
									}
								}
								if(isDead) {
									if(stateRemoved == null)
										stateRemoved = new BeliefState(actualBeliefState, true);
								}
								else {
									actualBeliefState.listPGhost.get(k).add(/*newPos.toString(),*/ newPos.clone());
									if(!hAlternativePos.contains(newPos.toString())) {
										tempListAlternativeBeliefState.add(actualBeliefState);
										hAlternativePos.add(newPos.toString());
									}
								}
							}
							else {
								Position newPos = posG.clone();
								if(newPos.x > state.pacmanPos.x) {
									newPos.x--;
									newPos.dir = 'U';
								}
								else {
									if(newPos.x < state.pacmanPos.x) {
										newPos.x++;
										newPos.dir = 'D';
									}
									else {
										if(newPos.y < state.pacmanPos.y) {
											newPos.y++;
											newPos.dir = 'R';
										}
										else {
											newPos.y--;
											newPos.dir = 'L';
										}
									}
								}
								if(newPos.x == state.pacmanPos.x && newPos.y == state.pacmanPos.y) {
									if(stateRemoved == null)
										stateRemoved = new BeliefState(state, true);
								}
								else{
									newPosGhost.add(/*newPos.toString(),*/ newPos);
								}
							}
							haveMoved = true;
						}
						else {
							ArrayList<int[]> caseAround =  new ArrayList<int[]>();
							ArrayList<Character> moveAround = new ArrayList<Character>();
							boolean rightAvailable = false, leftAvailable = false, upAvailable = false, downAvailable = false;
							if(posG.x > 0 && state.map[posG.x - 1][posG.y] != '#') {
								int[] newPosG = {posG.x - 1, posG.y};
								caseAround.add(newPosG);
								moveAround.add('U');
								upAvailable = true;
							}
							if(posG.x + 1 < state.map.length && state.map[posG.x + 1][posG.y] != '#') {
								int[] newPosG = {posG.x + 1, posG.y};
								caseAround.add(newPosG);
								moveAround.add('D');
								downAvailable = true;
							}
							if(posG.y > 0 && state.map[posG.x][posG.y - 1] != '#') {
								int[] newPosG = {posG.x, posG.y - 1};
								caseAround.add(newPosG);
								moveAround.add('L');
								leftAvailable = true;
							}
							if(posG.y + 1 < state.map[0].length && state.map[posG.x][posG.y + 1] != '#') {
								int[] newPosG = {posG.x, posG.y + 1};
								caseAround.add(newPosG);
								moveAround.add('R');
								rightAvailable = true;
							}

							int m = 0;
							switch (posG.dir) {
							case 'U' :
								if (leftAvailable || rightAvailable) {
									if(downAvailable) {
										caseAround.remove(upAvailable?1:0);
										moveAround.remove(upAvailable?1:0);
									}
									for(int[] newCase: caseAround) {
										Position newPos = new Position(newCase[0], newCase[1], moveAround.get(m++));
										if((newPos.x == state.pacmanPos.x && newPos.y == state.pacmanPos.y) || (posG.x == state.pacmanPos.x && posG.y == state.pacmanPos.y && newPos.x == this.pacmanPos.x && newPos.y == this.pacmanPos.y)) {
											newPos = new Position(this.theMap.getPGhost().get(k)[1] / this.theMap.getTailleCase(),this.theMap.getPGhost().get(k)[0] / this.theMap.getTailleCase(),'U');
											BeliefState actualBeliefState = new BeliefState(state, false);
											actualBeliefState.listPGhost.get(k).clear();
											actualBeliefState.compteurPeur.set(k, 0);
											actualBeliefState.listPGhost.get(k).add(/*newPos.toString(),*/ newPos);
											actualBeliefState.score += Ghost.SCORE_FANTOME;
											if(!hAlternativePos.contains(newPos.toString())) {
												tempListAlternativeBeliefState.add(actualBeliefState);
												hAlternativePos.add(newPos.toString());
											}
										}
										else {
											newPosGhost.add(/*newPos.toString(),*/ newPos);
										}
									}
									haveMoved = true;
								} else if (!upAvailable) {
									for(int[] newCase: caseAround) {
										Position newPos = new Position(newCase[0], newCase[1], moveAround.get(m++));
										if((newPos.x == state.pacmanPos.x && newPos.y == state.pacmanPos.y) || (posG.x == state.pacmanPos.x && posG.y == state.pacmanPos.y && newPos.x == this.pacmanPos.x && newPos.y == this.pacmanPos.y)) {
											newPos = new Position(this.theMap.getPGhost().get(k)[1] / this.theMap.getTailleCase(),this.theMap.getPGhost().get(k)[0] / this.theMap.getTailleCase(),'U');
											BeliefState actualBeliefState = new BeliefState(state, false);
											actualBeliefState.listPGhost.get(k).clear();
											actualBeliefState.compteurPeur.set(k, 0);
											actualBeliefState.listPGhost.get(k).add(/*newPos.toString(),*/ newPos);
											actualBeliefState.score += Ghost.SCORE_FANTOME;
											if(!hAlternativePos.contains(newPos.toString())) {
												tempListAlternativeBeliefState.add(actualBeliefState);
												hAlternativePos.add(newPos.toString());
											}
										}
										else {
											newPosGhost.add(/*newPos.toString(),*/ newPos);
										}
									}
									haveMoved = true;
								}
								break;
							case 'D' :
								if (leftAvailable || rightAvailable) {
									if(upAvailable) {
										caseAround.remove(0);
										moveAround.remove(0);
									}
									for(int[] newCase: caseAround) {
										Position newPos = new Position(newCase[0], newCase[1], moveAround.get(m++));
										if((newPos.x == state.pacmanPos.x && newPos.y == state.pacmanPos.y) || (posG.x == state.pacmanPos.x && posG.y == state.pacmanPos.y && newPos.x == this.pacmanPos.x && newPos.y == this.pacmanPos.y)) {
											newPos = new Position(this.theMap.getPGhost().get(k)[1] / this.theMap.getTailleCase(),this.theMap.getPGhost().get(k)[0] / this.theMap.getTailleCase(),'U');
											BeliefState actualBeliefState = new BeliefState(state, false);
											actualBeliefState.listPGhost.get(k).clear();
											actualBeliefState.compteurPeur.set(k, 0);
											actualBeliefState.listPGhost.get(k).add(/*newPos.toString(),*/ newPos);
											actualBeliefState.score += Ghost.SCORE_FANTOME;
											if(!hAlternativePos.contains(newPos.toString())) {
												tempListAlternativeBeliefState.add(actualBeliefState);
												hAlternativePos.add(newPos.toString());
											}
										}
										else {
											newPosGhost.add(/*newPos.toString(),*/ newPos);
										}
									}
									haveMoved = true;
								} else if (!downAvailable) {
									for(int[] newCase: caseAround) {
										Position newPos = new Position(newCase[0], newCase[1], moveAround.get(m++));
										if((newPos.x == state.pacmanPos.x && newPos.y == state.pacmanPos.y) || (posG.x == state.pacmanPos.x && posG.y == state.pacmanPos.y && newPos.x == this.pacmanPos.x && newPos.y == this.pacmanPos.y)) {
											newPos = new Position(this.theMap.getPGhost().get(k)[1] / this.theMap.getTailleCase(),this.theMap.getPGhost().get(k)[0] / this.theMap.getTailleCase(),'U');
											BeliefState actualBeliefState = new BeliefState(state, false);
											actualBeliefState.listPGhost.get(k).clear();
											actualBeliefState.compteurPeur.set(k, 0);
											actualBeliefState.listPGhost.get(k).add(/*newPos.toString(),*/ newPos);
											actualBeliefState.score += Ghost.SCORE_FANTOME;
											if(!hAlternativePos.contains(newPos.toString())) {
												tempListAlternativeBeliefState.add(actualBeliefState);
												hAlternativePos.add(newPos.toString());
											}
										}
										else {
											newPosGhost.add(/*newPos.toString(),*/ newPos);
										}
									}
									haveMoved = true;
								}
								break;
							case 'L' :
								if (upAvailable || downAvailable) {
									if(rightAvailable) {
										caseAround.remove((upAvailable?1:0)+(downAvailable?1:0)+(leftAvailable?1:0));
										moveAround.remove((upAvailable?1:0)+(downAvailable?1:0)+(leftAvailable?1:0));

									}
									for(int[] newCase: caseAround) {
										Position newPos = new Position(newCase[0], newCase[1], moveAround.get(m++));
										if((newPos.x == state.pacmanPos.x && newPos.y == state.pacmanPos.y) || (posG.x == state.pacmanPos.x && posG.y == state.pacmanPos.y && newPos.x == this.pacmanPos.x && newPos.y == this.pacmanPos.y)) {
											newPos = new Position(this.theMap.getPGhost().get(k)[1] / this.theMap.getTailleCase(),this.theMap.getPGhost().get(k)[0] / this.theMap.getTailleCase(),'U');
											BeliefState actualBeliefState = new BeliefState(state, false);
											actualBeliefState.listPGhost.get(k).clear();
											actualBeliefState.compteurPeur.set(k, 0);
											actualBeliefState.listPGhost.get(k).add(/*newPos.toString(),*/ newPos);
											actualBeliefState.score += Ghost.SCORE_FANTOME;
											if(!hAlternativePos.contains(newPos.toString())) {
												tempListAlternativeBeliefState.add(actualBeliefState);
												hAlternativePos.add(newPos.toString());
											}
										}
										else {
											newPosGhost.add(/*newPos.toString(),*/ newPos);
										}
									}
									haveMoved = true;
								} else if (!leftAvailable) {
									for(int[] newCase: caseAround) {
										Position newPos = new Position(newCase[0], newCase[1], moveAround.get(m++));
										if((newPos.x == state.pacmanPos.x && newPos.y == state.pacmanPos.y) || (posG.x == state.pacmanPos.x && posG.y == state.pacmanPos.y && newPos.x == this.pacmanPos.x && newPos.y == this.pacmanPos.y)) {
											newPos = new Position(this.theMap.getPGhost().get(k)[1] / this.theMap.getTailleCase(),this.theMap.getPGhost().get(k)[0] / this.theMap.getTailleCase(),'U');
											BeliefState actualBeliefState = new BeliefState(state, false);
											actualBeliefState.listPGhost.get(k).clear();
											actualBeliefState.compteurPeur.set(k, 0);
											actualBeliefState.listPGhost.get(k).add(/*newPos.toString(),*/ newPos);
											actualBeliefState.score += Ghost.SCORE_FANTOME;
											if(!hAlternativePos.contains(newPos.toString())) {
												tempListAlternativeBeliefState.add(actualBeliefState);
												hAlternativePos.add(newPos.toString());
											}
										}
										else {
											newPosGhost.add(/*newPos.toString(),*/ newPos);
										}
									}
									haveMoved = true;
								}
								break;
							case 'R' :
								if (upAvailable || downAvailable) {
									if(leftAvailable) {
										caseAround.remove((upAvailable?1:0)+(downAvailable?1:0));
										moveAround.remove((upAvailable?1:0)+(downAvailable?1:0));
									}
									for(int[] newCase: caseAround) {
										Position newPos = new Position(newCase[0], newCase[1], moveAround.get(m++));
										if((newPos.x == state.pacmanPos.x && newPos.y == state.pacmanPos.y) || (posG.x == state.pacmanPos.x && posG.y == state.pacmanPos.y && newPos.x == this.pacmanPos.x && newPos.y == this.pacmanPos.y)) {
											newPos = new Position(this.theMap.getPGhost().get(k)[1] / this.theMap.getTailleCase(),this.theMap.getPGhost().get(k)[0] / this.theMap.getTailleCase(),'U');
											BeliefState actualBeliefState = new BeliefState(state, false);
											actualBeliefState.listPGhost.get(k).clear();
											actualBeliefState.compteurPeur.set(k, 0);
											actualBeliefState.listPGhost.get(k).add(/*newPos.toString(),*/ newPos);
											actualBeliefState.score += Ghost.SCORE_FANTOME;
											if(!hAlternativePos.contains(newPos.toString())) {
												tempListAlternativeBeliefState.add(actualBeliefState);
												hAlternativePos.add(newPos.toString());
											}
										}
										else {
											newPosGhost.add(/*newPos.toString(),*/ newPos);
										}
									}
									haveMoved = true;
								} else if (!rightAvailable) {
									for(int[] newCase: caseAround) {
										Position newPos = new Position(newCase[0], newCase[1], moveAround.get(m++));
										if((newPos.x == state.pacmanPos.x && newPos.y == state.pacmanPos.y) || (posG.x == state.pacmanPos.x && posG.y == state.pacmanPos.y && newPos.x == this.pacmanPos.x && newPos.y == this.pacmanPos.y)) {
											newPos = new Position(this.theMap.getPGhost().get(k)[1] / this.theMap.getTailleCase(),this.theMap.getPGhost().get(k)[0] / this.theMap.getTailleCase(),'U');
											BeliefState actualBeliefState = new BeliefState(state, false);
											actualBeliefState.listPGhost.get(k).clear();
											actualBeliefState.compteurPeur.set(k, 0);
											actualBeliefState.listPGhost.get(k).add(/*newPos.toString(),*/ newPos);
											actualBeliefState.score += Ghost.SCORE_FANTOME;
											if(!hAlternativePos.contains(newPos.toString())) {
												tempListAlternativeBeliefState.add(actualBeliefState);
												hAlternativePos.add(newPos.toString());
											}
										}
										else {
											newPosGhost.add(/*newPos.toString(),*/ newPos);
										}
									}
									haveMoved = true;
								}
								break;
							}	
						}									
						if (!haveMoved) {
							Position newPos = posG.clone();
							switch(posG.dir) {
							case 'U': newPos.x--; break;
							case 'D': newPos.x++; break;
							case 'L': newPos.y--; break;
							case 'R': newPos.y++; break;
							}
							if(compteurPeur > 0) {
								if((newPos.x == state.pacmanPos.x && newPos.y == state.pacmanPos.y) || (posG.x == state.pacmanPos.x && posG.y == state.pacmanPos.y && newPos.x == this.pacmanPos.x && newPos.y == this.pacmanPos.y)) {
									newPos = new Position(this.theMap.getPGhost().get(k)[1] / this.theMap.getTailleCase(),this.theMap.getPGhost().get(k)[0] / this.theMap.getTailleCase(),'U');
									BeliefState actualBeliefState = new BeliefState(state, false);
									actualBeliefState.listPGhost.get(k).clear();
									actualBeliefState.compteurPeur.set(k, 0);
									actualBeliefState.listPGhost.get(k).add(/*newPos.toString(),*/ newPos);
									actualBeliefState.score += Ghost.SCORE_FANTOME;
									if(!hAlternativePos.contains(newPos.toString())) {
										tempListAlternativeBeliefState.add(actualBeliefState);
										hAlternativePos.add(newPos.toString());
									}
								}
								else {
									newPosGhost.add(/*newPos.toString(),*/ newPos);
								}
							}
							else {
								newPosGhost.add(/*newPos.toString(),*/ newPos);
							}
						}
					}
					if(newPosGhost.isEmpty()) {
						listAlternativeBeliefState.remove(indexBeliefState--);
					}
					else {
						state.listPGhost.set(k, newPosGhost);
					}
				}
				listAlternativeBeliefState.addAll(tempListAlternativeBeliefState);
			}
			if(stateRemoved != null) {
				listAlternativeBeliefState.add(stateRemoved);
			}
		}
		return new Result(listAlternativeBeliefState);
	}

	/**
	 * create all possible states resulting from all possible actions of Pacman
	 * @return a plan, which is a list of belief states, on per set of actions resulting to the same belief states
	 */
	public Plans extendsBeliefState() {
		Plans plans = new Plans();
		if(this.life <= 0)
			return plans;
		ArrayList<String> listNull = new ArrayList<String>();
		if(pacmanPos.x > 0) {
			char nextPos = this.map[this.pacmanPos.x - 1][this.pacmanPos.y];
			if(nextPos != '#') {
				ArrayList<String> listUp = new ArrayList<String>();
				listUp.add(PacManLauncher.UP);
				plans.addPlan(this.extendsBeliefState(PacManLauncher.UP), listUp);
			}
			else {
				listNull.add(PacManLauncher.UP);
			}
		}
		if(this.pacmanPos.x + 1 < this.map.length) {
			char nextPos = this.map[this.pacmanPos.x + 1][this.pacmanPos.y];
			if(nextPos != '#') {
				ArrayList<String> listDown = new ArrayList<String>();
				listDown.add(PacManLauncher.DOWN);
				plans.addPlan(this.extendsBeliefState(PacManLauncher.DOWN), listDown);
			}
			else {
				listNull.add(PacManLauncher.DOWN);
			}
		}
		if(this.pacmanPos.y > 0) {
			char nextPos = this.map[this.pacmanPos.x][this.pacmanPos.y - 1];
			if(nextPos != '#') {
				ArrayList<String> listLeft = new ArrayList<String>();
				listLeft.add(PacManLauncher.LEFT);
				plans.addPlan(this.extendsBeliefState(PacManLauncher.LEFT), listLeft);
			}
			else {
				listNull.add(PacManLauncher.LEFT);
			}
		}
		if(this.pacmanPos.y + 1 < this.map[0].length) {
			char nextPos = this.map[this.pacmanPos.x][this.pacmanPos.y + 1];
			if(nextPos != '#') {
				ArrayList<String> listRight = new ArrayList<String>();
				listRight.add(PacManLauncher.RIGHT);
				plans.addPlan(this.extendsBeliefState(PacManLauncher.RIGHT), listRight);
			}
			else {
				listNull.add(PacManLauncher.RIGHT);
			}
		}
		if(listNull.size() > 0)
			plans.addPlan(this.extendsBeliefState(listNull.get(0)), listNull);
		return plans;
	}

	/**
	 * remove from a list of states all the states where a given ghost is not (possibly) at a given position provided as input
	 * @param listBeliefState list of state to be updated
	 * @param gId Id of the ghost
	 * @param posG actual position of the ghost
	 */
	public static void filter(ArrayList<BeliefState> listBeliefState, int gId, Position posG) {
		ArrayList<BeliefState> copy = (ArrayList<BeliefState>)listBeliefState.clone();
		for(int i = 0; i < listBeliefState.size(); i++) {
			BeliefState state = listBeliefState.get(i);
			if(!state.listPGhost.get(gId).contains(posG)) {
				listBeliefState.remove(i);
				i--;
			}
		}
	}

	/**
	 * move the Pacman at a given position
	 * @param i number of rows added to the current position of Pacman
	 * @param j number of columns added to the current position of Pacman
	 * @param nextPos content of the new position of Pacman
	 * @param move direction followed by Pacman ('U', 'D', 'L', 'R')
	 * @return the state resulting from the action of Pacman
	 */
	public BeliefState move(int i, int j, char nextPos, char move) {
		BeliefState nextBeliefState = new BeliefState(this, false);
		if(nextBeliefState.map[nextBeliefState.pacmanPos.x][nextBeliefState.pacmanPos.y] == 'B')
			nextBeliefState.map[nextBeliefState.pacmanPos.x][nextBeliefState.pacmanPos.y] = 'F';
		else
			nextBeliefState.map[nextBeliefState.pacmanPos.x][nextBeliefState.pacmanPos.y] = 'O';
		nextBeliefState.pacmanPos.x += i;
		nextBeliefState.pacmanPos.y += j;
		nextBeliefState.pacmanPos.dir = move;
		if(nextPos == '*' || nextPos == '.') {
			nextBeliefState.nbrOfGommes--;
			nextBeliefState.score += Gomme.SCORE_GOMME;
			if(nextPos == '*') {
				nextBeliefState.nbrOfSuperGommes--;
				for(int k = 0; k < nextBeliefState.compteurPeur.size(); k++) {
					nextBeliefState.compteurPeur.set(k, Ghost.TIME_PEUR);
				}
			}
		}
		if(nextBeliefState.map[nextBeliefState.pacmanPos.x][nextBeliefState.pacmanPos.y] == 'F')
			nextBeliefState.map[nextBeliefState.pacmanPos.x][nextBeliefState.pacmanPos.y] = 'B';
		else
			nextBeliefState.map[nextBeliefState.pacmanPos.x][nextBeliefState.pacmanPos.y] = 'P';
		return nextBeliefState;
	}

	/**
	 * move the Pacman at a given position
	 * @param i number of rows added to the current position of Pacman
	 * @param j number of columns added to the current position of Pacman
	 * @param move direction followed by Pacman ('U', 'D', 'L', 'R')
	 * @return true if Pacman is dead after performing the move
	 */
	public boolean move(int i, int j, char move) {
		if(this.map[this.pacmanPos.x + i][this.pacmanPos.y + j] != '#') {
			if(this.map[this.pacmanPos.x][this.pacmanPos.y] == 'B')
				this.map[this.pacmanPos.x][this.pacmanPos.y] = 'F';
			else
				this.map[this.pacmanPos.x][this.pacmanPos.y] = 'O';
			this.pacmanPos.x += i;
			this.pacmanPos.y += j;
			this.pacmanPos.dir = move;
			int l = 0;
			char nextPos = this.map[this.pacmanPos.x][this.pacmanPos.y];
			if(nextPos != 'O' && nextPos != 'F') {
				this.nbrOfGommes--;
				this.score += Gomme.SCORE_GOMME;
				if(nextPos == '*') {
					this.nbrOfSuperGommes--;
					for(int k = 0; k < this.compteurPeur.size(); k++) {
						this.compteurPeur.set(k, Ghost.TIME_PEUR);
					}
				}
			}
			if(nextPos == 'F')
				this.map[this.pacmanPos.x][this.pacmanPos.y] = 'B';
			else
				this.map[this.pacmanPos.x][this.pacmanPos.y] = 'P';
			for(/*HashMap<String, Position>*/TreeSet<Position> treeSet: this.listPGhost) {
				if(this.compteurPeur.get(l++) == 0 && treeSet.size() == 1) {
					Position pos = treeSet.first();
					if(pos.x == this.pacmanPos.x && pos.y == this.pacmanPos.y)
						return true;
				}
			}
		}
		else {
			this.pacmanPos.dir = 'N';
		}
		return false;
	}

	/**
	 * move the Pacman at a given position
	 * @param i new row position
	 * @param j new culumn position
	 * @param move direction of the pacman
	 */
	public void moveTo(int i, int j, char move) {
		if(this.map[this.pacmanPos.x][this.pacmanPos.y] == 'B')
			this.map[this.pacmanPos.x][this.pacmanPos.y] = 'F';
		else
			this.map[this.pacmanPos.x][this.pacmanPos.y] = 'O';
		this.pacmanPos.x = i;
		this.pacmanPos.y = j;
		this.pacmanPos.dir = move;
		if(this.map[this.pacmanPos.x][this.pacmanPos.y] == 'F')
			this.map[this.pacmanPos.x][this.pacmanPos.y] = 'B';
		else
			this.map[this.pacmanPos.x][this.pacmanPos.y] = 'P';
	}

	/**
	 * move one of the ghost to a given position
	 * @param i number of rows added to the current position of the ghost
	 * @param j number of columns added to the current position of the ghost
	 * @param k Id of the ghost
	 * @param dir direction followed by the ghost ('U', 'D', 'L', 'R')
	 * @return true if the move performed by the ghost kill Pacman
	 */
	public boolean moveGhost(int i, int j, int k, char dir) {
		Position posGhost = this.listPGhost.get(k).first();

		int compteurPeur = this.compteurPeur.get(k);
		if(compteurPeur > 0) {
			Position posPcopy = this.pacmanPos.clone();
			switch(posPcopy.dir) {
			case 'U': posPcopy.x++; break;
			case 'D': posPcopy.x--; break;
			case 'L': posPcopy.y++; break;
			case 'R': posPcopy.y--; break;
			}
			if(posGhost.x == this.pacmanPos.x && posGhost.y == this.pacmanPos.y && posPcopy.x == posGhost.x + i && posPcopy.y == posGhost.y + j) {
				Integer[] initPosG = this.theMap.getPGhost().get(k);
				this.moveGhostTo(initPosG[1] / this.theMap.getTailleCase(), initPosG[0] / this.theMap.getTailleCase(), k, 'U'/*, true*/);
				this.score += Ghost.SCORE_FANTOME;
				return false;
			}
			else
				this.compteurPeur.set(k, compteurPeur - 2);
		}
		this.listPGhost.get(k).clear();
		posGhost.x += i;
		posGhost.y += j;
		posGhost.dir = dir;
		this.listPGhost.get(k).add(/*posGhost.toString(),*/ posGhost);
		if(posGhost.x == this.pacmanPos.x && posGhost.y == this.pacmanPos.y) {
			if(this.compteurPeur.get(k) == 0) {
				this.life--;
				this.moveTo(this.theMap.getPMY() / this.theMap.getTailleCase(),this.theMap.getPMX() / this.theMap.getTailleCase(), 'U');
				for(int l = 0; l < this.theMap.getPGhost().size(); l++) {
					Integer[] initPosG = this.theMap.getPGhost().get(l);
					this.moveGhostTo(initPosG[1] / this.theMap.getTailleCase(), initPosG[0] / this.theMap.getTailleCase(), l, 'U'/*, false*/);
				}
				return true;
			}
			else {
				Integer[] initPosG = this.theMap.getPGhost().get(k);
				this.moveGhostTo(initPosG[1] / this.theMap.getTailleCase(), initPosG[0] / this.theMap.getTailleCase(), k, 'U'/*, false*/);
				this.score += Ghost.SCORE_FANTOME;
				return false;
			}
		}
		else
			return false;
	}

	/**
	 * move one of the ghost to a given position
	 * @param i new row position of the ghost
	 * @param j new column position of the ghost
	 * @param k Id of the ghost
	 * @param dir direction followed by the ghost ('U', 'D', 'L', 'R')
	 */
	public void moveGhostTo(int i, int j, int k, char dir) {
		Position posGhost = this.listPGhost.get(k).first();
		this.listPGhost.get(k).clear();
		posGhost.x = i;
		posGhost.y = j;
		this.compteurPeur.set(k, 0);
		posGhost.dir = dir;
		this.listPGhost.get(k).add(/*posGhost.toString(),*/ posGhost);
	}

	public String toString() {
		String s = new String();
		for(int i = 0; i < this.map.length; i++) {
			for(int j = 0; j < this.map[0].length; j++) {
				s += this.map[i][j];
			}
			s += '\n';
		}
		s += "Pacman (" + this.pacmanPos.x + ", " + this.pacmanPos.y + ", " + this.pacmanPos.dir + ") "+ this.score +"\n";
		for(int i = 0; i < this.listPGhost.size(); i++) {
			s += "Ghost " + i + " (" + this.listPGhost.get(i).size() + ") [" + this.compteurPeur.get(i) + "]";
			Iterator<Position> itPos = this.listPGhost.get(i).iterator();
			while(itPos.hasNext()) {
				Position posG = itPos.next();
				s += "(" + posG.x + ", " + posG.y + ") " + posG.dir + " ";
			}
			s += "\n";
		}
		return s;
	}

	/**
	 * return the position of one of the ghost
	 * @param i Id of the ghost
	 * @return the position of the ghost
	 */
	public Position getPGhost(int i) {
		return this.listPGhost.get(i).first();
	}

	/**
	 * return Pacman position
	 * @return the position of Pacman
	 */
	public Position getPacmanPos() {
		return this.pacmanPos;
	}
	
	/**
	 * return the number of remaining lifes
	 * @return the number of remaining lifes
	 */
	public int getLife() {
		return this.life;
	}
	
	/**
	 * return the number of remaining gums in the map
	 * @return the number of remaining gums in the map
	 */
	public int getNbrOfGommes() {
		return this.nbrOfGommes;
	}
	
	/**
	 * return the number of remaining super gums in the map
	 * @return the number of remaining super gums in the map
	 */
	public int getNbrOfSuperGommes() {
		return this.nbrOfSuperGommes;
	}
	
	/**
	 * return the number of ghosts
	 * @return number of ghosts
	 */
	public int getNbrOfGhost() {
		return this.compteurPeur.size();
	}
	
	public int getCompteurPeur(int i) {
		return this.compteurPeur.get(i);
	}
	
	public char getMap(int i, int j) {
		return this.map[i][j];
	}
	
	public Position getPacmanPosition() {
		return this.pacmanPos;
	}
	
	public TreeSet<Position> getGhostPositions(int i){
		return this.listPGhost.get(i);
	}
}
