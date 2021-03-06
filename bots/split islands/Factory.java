import bc.*;
import java.util.*;

public class Factory {

	static Unit curUnit;
	static MapLocation curLoc;
	static GameController gc;
	static HashSet<Integer> workers = new HashSet<Integer>();
	static Direction[] directions = Direction.values();

	public static void run(Unit curUnit) {

		Factory.curUnit = curUnit;
		Factory.curLoc = curUnit.location().mapLocation();

		if (Player.timesReachedTarget >= 3) {
            return;
        }

		VecUnitID garrison = curUnit.structureGarrison();
		for (int i = 0; i < garrison.size(); i++) {
			//unload units
			for (int a = 0; a < directions.length; a++) {
				if (gc.canUnload(curUnit.id(), directions[a])) {
					gc.unload(curUnit.id(), directions[a]);
					Unit newUnit = gc.senseUnitAtLocation(curLoc.add(directions[a]));
					int newId = newUnit.id();
					Player.parentWorker.put(newId, Player.parentWorker.get(curUnit.id()));
					switch (newUnit.unitType()) {
						case Healer:
							Healer.run(gc, newUnit);
							break;
						case Knight:
							Knight.run(gc, newUnit);
							break;
						case Mage:
							Mage.run(gc, newUnit);
							break;
						case Ranger:
							Ranger.run(gc, newUnit);
							break;
						case Worker:
							Worker.run(newUnit);
					}
					break;
				}
			}
		}

		Player.currentIncome -= 4;
		//produce unit if no rockets have been started AND rockets can be built
		if (Player.prevBlocked < 15 && gc.canProduceRobot(curUnit.id(), UnitType.Ranger) && (gc.karbonite() > 40 || gc.researchInfo().getLevel(UnitType.Rocket) == 0)) {
			if (Player.split[Player.parentWorker.get(curUnit.id())]) {
				int around = 0;
				for (int i = 0; i < directions.length; i++) {
					MapLocation test = curLoc;
					boolean res = onMap(test);
					if (!res || res && !Player.gotoable[Player.parentWorker.get(curUnit.id())][test.getX()][test.getY()]) {
						around++;
					}
				}

				VecUnit nearby = gc.senseNearbyUnitsByTeam(curLoc, 2, Player.myTeam);
				for (int i = 0; i < nearby.size(); i++) {
					UnitType temp = nearby.get(i).unitType();
					around++;
				}

				if (around <= 5) {
					if (Worker.numWorkers == 0) {
	                	gc.produceRobot(curUnit.id(), UnitType.Worker);
		            } else if (gc.researchInfo().getLevel(UnitType.Healer) >= 1 && Player.numRangers > 4*Player.numHealers) {
						gc.produceRobot(curUnit.id(), UnitType.Healer);
						Player.numHealers++;
					} else {
						gc.produceRobot(curUnit.id(), UnitType.Ranger);
						Player.numRangers++;
					}
				}
			} else {
				if (Worker.numWorkers == 0 && gc.karbonite() >= 25) {
	                gc.produceRobot(curUnit.id(), UnitType.Worker);
	            } else if (gc.researchInfo().getLevel(UnitType.Healer) >= 1 && Player.numRangers > 4*Player.numHealers) {
					gc.produceRobot(curUnit.id(), UnitType.Healer);
					Player.numHealers++;
				} else {
					gc.produceRobot(curUnit.id(), UnitType.Ranger);
					Player.numRangers++;
				}
			}
		}

		return;
	}

	public static int hash(MapLocation loc) {
		return 69 * loc.getX() + loc.getY();
	}

	public static boolean onMap(MapLocation test) {
        int x = test.getX();
        int y = test.getY();
        return x >= 0 && y >= 0 && x < Player.gridX && y < Player.gridY;
    }
}