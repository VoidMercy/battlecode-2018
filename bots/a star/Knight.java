import bc.*;
import java.util.*;

public class Knight {

	static Unit curUnit;
	static GameController gc;
	static Direction[] directions = Direction.values();
	static HashMap<Integer, HashSet<Integer>> visited = new HashMap<Integer, HashSet<Integer>>();
	static HashMap<Integer, Integer> targets = new HashMap<Integer, Integer>();
	static HashMap<Integer, HashMap<Integer, Integer>> paths = new HashMap<Integer, HashMap<Integer, Integer>>();

	public static void run(GameController gc, Unit curUnit) {

		Knight.curUnit = curUnit;
		Knight.gc = gc;

		if (curUnit.location().isInGarrison()) {
			return;
		}

		Pair nearbyInfo = null;

		//if i dont have a target right now
		if (!targets.containsKey(curUnit.id())) {
			//try to find an enemy target in range
			nearbyInfo = findTarget();
		} else {
			//remove unit if it is already killed
			try {
				gc.unit(targets.get(curUnit.id()));
			} catch (Exception e) {
				//already killed this unit, or it ran away, remove
				targets.remove(curUnit.id());
				nearbyInfo = findTarget();
			}
			
		}

		//didnt sense number of allies and enemies yet
		if (nearbyInfo == null) {
			nearbyInfo = countNearby();
		}

		//if this knight has a target
		if (targets.containsKey(curUnit.id())) {
			//move towards them if my army is stronger
			if (nearbyInfo.friendly >= nearbyInfo.enemy) {
				if (canMove()) {
					move(gc.unit(targets.get(curUnit.id())).location().mapLocation());
				}
				if (canAttack()) {
					tryAttack(targets.get(curUnit.id()));
				}
			} else {
				//otherwise run away!!!
				if (canMove()) {
					move(Player.startingLocation);
				}
			}
		} else {
			//otherwise explore
			if (canMove()) {
				move(Player.enemyLocation);
			}
		}

		//if i couldnt attack my target, attack anyone around me i can attack
		if (canAttack()) {
			attackNearbyEnemies();
		}

	}

	//try to attack a target unit
	public static void tryAttack(int id) {
		if (gc.canAttack(curUnit.id(), id)) {
			gc.attack(curUnit.id(), id);
		}
	}

	public static Pair findTarget() {
		Pair ret = new Pair();
		VecUnit nearby = gc.senseNearbyUnits(curUnit.location().mapLocation(), curUnit.visionRange());
		int tempTarget = -1;
		int smallest = 9999999;
		//find nearest target
		for (int i = 0; i < nearby.size(); i++) {
			Unit temp3 = nearby.get(i);
			if (temp3.team() != gc.team()) {
				ret.enemy++;
				MapLocation temp2 = temp3.location().mapLocation();
				int temp = distance(curUnit.location().mapLocation(), temp2);
				if (temp < smallest) {
					smallest = temp;
					tempTarget = temp3.id();
				}
			}
		}
		//if found a target, set that as target for units around me
		if (smallest != 9999999) {
			for (int i = 0; i < nearby.size(); i++) {
				Unit temp3 = nearby.get(i);
				if (temp3.team() == gc.team()) {
					ret.friendly++;
					targets.put(temp3.id(), tempTarget);
				}
			}
		}
		return ret;
	}

	//count number of nearby enemies and allies
	public static Pair countNearby() {
		Pair ret = new Pair();
		VecUnit nearby = gc.senseNearbyUnits(curUnit.location().mapLocation(), curUnit.visionRange());
		for (int i = 0; i < nearby.size(); i++) {
			Unit temp3 = nearby.get(i);
			if (temp3.team() != gc.team()) {
				ret.enemy++;
			} else {
				ret.friendly++;
			}
		}
		return ret;
	}

	//struct to store two ints lol
	public static class Pair {
		int friendly;
		int enemy;
		Pair() {
			friendly = 0;
			enemy = 0;
		}
	}

	public static boolean canAttack() {
		return curUnit.attackHeat() < 10;
	}

	//attack nearest enemy found
	public static void attackNearbyEnemies() {
		VecUnit nearbyUnits = getNearby(curUnit.location().mapLocation(), (int) curUnit.attackRange());
		for (int i = 0; i < nearbyUnits.size(); i++) {
			Unit unit = nearbyUnits.get(i);
			//if can attack this enemy unit
			if (unit.team() != gc.team() && gc.isAttackReady(curUnit.id()) && gc.canAttack(curUnit.id(), unit.id())) {
				gc.attack(curUnit.id(), unit.id());
				return;
			}
		}
	}

	public static boolean canMove() {
		return curUnit.movementHeat() < 10;
	}

	//pathing
	//move towards target location
	public static void move(MapLocation target) {
		//a*
		int movingTo = doubleHash(curUnit.location().mapLocation(), target);
		if (!paths.containsKey(movingTo)) {
			HashSet<Integer> closedList = new HashSet<Integer>();
			HashMap<Integer, Integer> gScore = new HashMap<Integer, Integer>();
			HashMap<Integer, Integer> fScore = new HashMap<Integer, Integer>();
			HashMap<Integer, Integer> fromMap = new HashMap<Integer, Integer>();
			PriorityQueue<Integer> openList = new PriorityQueue<Integer>(11, new Comparator<Integer>() {
			public int compare(Integer nodeA, Integer nodeB) {
				return Integer.compare(fScore.get(nodeA), fScore.get(nodeB));
			}
		});

			MapLocation curLoc = curUnit.location().mapLocation();

			int startHash = hash(curLoc);

			gScore.put(startHash, 0);
			fScore.put(startHash, manDistance(curLoc, target));
			openList.offer(startHash);

			int goal = hash(target);

			while (!openList.isEmpty()) {
				int current = openList.poll();

				int tempY = current % 69;
				int tempX = (current - tempY) / 69;
				curLoc = new MapLocation(Planet.Earth, tempX, tempY);
				
				//System.out.println("Node im on " + print(current));

				closedList.add(current);

				//iterate through neighbors
				for (int i = 0; i < directions.length; i++) {
					int neighbor = hash(curLoc.add(directions[i]));
					if (neighbor == goal) {
						fromMap.put(neighbor, current);
						HashMap<Integer, Integer> path = new HashMap<Integer, Integer>();
						HashMap<Integer, Integer> path2 = new HashMap<Integer, Integer>();
						int next = goal;

						int prev = -1;
						ArrayList<Integer> before = new ArrayList<Integer>();
						before.add(next);
						while (fromMap.containsKey(next)) {
							//System.out.println(print(next));
							//path.put(next, prev);
							prev = next;
							next = fromMap.get(prev);
							before.add(next);
							//paths.put(doubleHash(prev, next), path);
							//paths.put(doubleHash(next, prev), path);
							//TODO put in between paths... a b c d e needs bc, bd, cd 
							path.put(next, prev);
							path2.put(prev, next);
						}
						int temp = before.size();
						for (int j = 0; j < temp; j++) {
							for (int a = 0; a < j; a++) {
								paths.put(doubleHash(before.get(j), before.get(a)), path);
								paths.put(doubleHash(before.get(a), before.get(j)), path2);
							}
						}
						
						break;
					}
					if (checkPassable(curLoc.add(directions[i]))) {
						if (closedList.contains(neighbor)) {
							continue;
						}

						int tentG = gScore.get(current) + 1;

						boolean contains = openList.contains(neighbor);
						if (!contains || tentG < gScore.get(neighbor)) {
							gScore.put(neighbor, tentG);
							fScore.put(neighbor, tentG + manDistance(neighbor, hash(target.getX(), target.getY())));

							if (contains) {
								openList.remove(neighbor);
							}

							openList.offer(neighbor);
							//System.out.println("Add: " + print(neighbor));
							fromMap.put(neighbor, current);
						}
					}
				}
			}
		}
		//System.out.println(hash(curUnit.location().mapLocation()));
		//System.out.println(Arrays.asList(paths.get(movingTo)));
		//System.out.println(paths.get(movingTo).containsKey(hash(curUnit.location().mapLocation())));

		int toMove = paths.get(movingTo).get(hash(curUnit.location().mapLocation()));

		int y = toMove % 69;
		int x = (toMove - y) / 69;
		
		MapLocation next = new MapLocation(Planet.Earth, x, y);
		Direction temp = curUnit.location().mapLocation().directionTo(next);
		if (gc.canMove(curUnit.id(), temp) && canMove()) {
			gc.moveRobot(curUnit.id(), temp);
		} else {
			System.out.println("Darn");
		}
	}

	public static String print(int hash) {
		int asdf = hash % 69;
		int asdfg = (hash - asdf) / 69;
		return Integer.toString(asdf) + " " + Integer.toString(asdfg);
	}

	public static int doubleHash(int x1, int y1, int x2, int y2) {
		return (69 * x1) + y1 + ((69 * x2) + y2) * 10000;
	}

	public static int doubleHash(int hash1, int hash2) {
		int y1 = hash1 % 69;
		int x1 = (hash1 - y1) / 69;
		int y2 = hash2 % 69;
		int x2 = (hash2 - y2) / 69;
		return (69 * x1) + y1 + ((69 * x2) + y2) * 10000;
	}

	public static int doubleHash(MapLocation first, MapLocation second) {
		int x1 = first.getX(), y1 = first.getY(), x2 = second.getX(), y2 = second.getY();
		return (69 * x1) + y1 + ((69 * x2) + y2) * 10000;
	}

	public static boolean checkPassable(MapLocation test) {
		if (test.getX() >= Player.gridX || test.getY() >= Player.gridY || test.getX() < 0 || test.getY() < 0) {
			return false;
		}
		return Player.planetMap.isPassableTerrainAt(test) == 1;
	}

	public static int hash(int x, int y) {
		return 69 * x + y;
	}

	public static int hash(MapLocation loc) {
		return 69 * loc.getX() + loc.getY();
	}

	public static int distance(MapLocation first, MapLocation second) {
		int x1 = first.getX(), y1 = first.getY(), x2 = second.getX(), y2 = second.getY();
		return (x1 - x2) * (x1 - x2) + (y1 - y2) * (y1 - y2);
	}

	public static int manDistance(MapLocation first, MapLocation second) {
		int x1 = first.getX(), y1 = first.getY(), x2 = second.getX(), y2 = second.getY();
		return (x2 - x1) + (y2 - y1);
	}

	public static int manDistance(int hash1, int hash2) {
		int y1 = hash1 % 69;
		int x1 = (hash1 - y1) / 69;
		int y2 = hash2 % 69;
		int x2 = (hash2 - y2) / 69;
		return (x2 - x1) + (y2 - y1);
	}

	public static int distance(int hash1, int hash2) {
		int y1 = hash1 % 69;
		int x1 = (hash1 - y1) / 69;
		int y2 = hash2 % 69;
		int x2 = (hash2 - y2) / 69;
		return (x1 - x2) * (x1 - x2) + (y1 - y2) * (y1 - y2);
	}

	//senses nearby units and updates RobotPlayer.map with detected units
	public static VecUnit getNearby(MapLocation maploc, int radius) {
		VecUnit nearby = gc.senseNearbyUnits(maploc, radius);
		for (int i = 0; i < nearby.size(); i++) {
			Unit unit = nearby.get(i);
			MapLocation temp = unit.location().mapLocation();
			Player.map[temp.getX()][temp.getY()] = unit;
		}
		return nearby;
	}

}