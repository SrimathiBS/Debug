import java.util.InputMismatchException;
import java.util.Iterator;
import java.util.Scanner;
import java.util.TreeSet;




//if we should get lift direction before getting pending request so if we enter lift is idle , user should not be able to enter pending requests i made the required change for that , but another change is if user gave up/down direction for lift but didnt give any pending request ,then the lift direction should be saved as idle
//does pending request require direction?isn't it similar to nextfloor treeset ?
//how to end the program or end getting input without enterning -1, -2 anol cuz if there are basement in a building we cant process it right.
//what does INF mean here?
//for variable naming dont say f,T,B anol give correct name like liftFloor,pickupFloor like that


public class LiftAllocationService {
    TreeSet<Request> pendingRequests = new TreeSet<>();
    TreeSet<Integer> nextFloors = new TreeSet<>();

    public void addRequest(Request request) {
        Request existing = pendingRequests.ceiling(request);

        if (existing != null && existing.compareTo(request) == 0) {
            // Merge dir
            for (Direction dir : request.getDirection()) {
                existing.addDirection(dir);
            }
        } else {
            // No existing request for this pickup floor
            pendingRequests.add(request);
        }
    }

    public Integer findNextFloor(Lift lift, Scanner scanner) {
        if (lift.direction == Direction.IDLE) {
            lift.direction = handleIdleLift(lift, scanner);
        }

        while (hasWork()) {
            Integer next;
            if (lift.direction == Direction.UP) {
                next = findNextUpFloor(lift);
                if (next != null && (lift.getCurrentFloor() > next)) {
                    lift.direction = Direction.DOWN;
                }
            } else {
                next = findNextDownFloor(lift);
                if (next != null && (lift.getCurrentFloor() < next)) {
                    lift.direction = Direction.UP;
                }
            }

            if (next != null) {
                return next;
            }

            lift.direction = reverse(lift);//reverse lift direction
            processPickup(lift);
        }
        return null;
    }

    public Direction reverse(Lift lift) {
        return (lift.direction == Direction.UP) ? Direction.DOWN : Direction.UP;
    }

    private Integer findNextUpFloor(Lift lift) {
        Integer destination = nextFloors.higher(lift.currentFloor);
        Integer compatiblePickup = null;

        for (Request request : pendingRequests.tailSet(new Request(lift.currentFloor), false)) {
            if (request.getDirection().contains(Direction.UP)) {
                compatiblePickup = request.getPickupFloor();
                break;
            }
        }
        if (destination != null && compatiblePickup != null) {
            return Math.min(destination, compatiblePickup);
        } else if (destination != null || compatiblePickup != null) {
            return (destination != null) ? destination : compatiblePickup;
        }

        // visit the next pickup even if it only has the opposite direction.
        //Request request = pendingRequests.higher(new Request(lift.currentFloor));
        if (!pendingRequests.isEmpty()) {
            if (pendingRequests.last().getDirection().contains(Direction.DOWN)) {
                lift.direction = Direction.DOWN;   // lift turns around here
                return pendingRequests.last().getPickupFloor();
            } else {
                return null;
            }
        } else return null;

    }

    private Integer findNextDownFloor(Lift lift) {
        Integer destination = nextFloors.lower(lift.currentFloor);
        Integer compatiblePickup = null;

        for (Request request : pendingRequests.headSet(new Request(lift.currentFloor), false).descendingSet()) {
            if (request.getDirection().contains(Direction.DOWN)) {
                compatiblePickup = request.getPickupFloor();
                break;
            }
        }
        if (destination != null && compatiblePickup != null) {
            return Math.max(destination, compatiblePickup);
        } else if (destination != null || compatiblePickup != null) {
            return (destination != null) ? destination : compatiblePickup;
        }
        if (!pendingRequests.isEmpty()) {
            if (pendingRequests.first().getDirection().contains(Direction.UP)) {
                lift.direction = Direction.UP;     // lift turns around here
                return pendingRequests.first().getPickupFloor();
            } else {
                return null;
            }
        } else return null;
    }


    public Direction handleIdleLift(Lift lift, Scanner scanner) {

        if (!hasWork()) return null;
        Request reqAtCurrentFloor = pendingRequests.ceiling(new Request(lift.currentFloor));
        if (reqAtCurrentFloor != null && reqAtCurrentFloor.pickupFloor == lift.currentFloor) {

            if (!reqAtCurrentFloor.getDirection().isEmpty()) {
                lift.direction = reqAtCurrentFloor.getDirection().get(0);
            } else return lift.direction;
            System.out.println("\nLift reached floor " + lift.getCurrentFloor() + " ... " + "Lift direction: " + lift.direction);
            System.out.println("Enter destination floors (-1 to finish this list):");
            while (true) {
                int destinationFloor;
                try {
                    destinationFloor = scanner.nextInt();
                } catch (InputMismatchException e) {
                    scanner.nextLine();
                    System.out.println("Enter valid floor from 0 to 8");
                    continue;
                }
                if (destinationFloor == -1) {
                    break;
                }
                try {
                    if (destinationFloor != lift.currentFloor && destinationFloor >= 0 && destinationFloor <= 8) {
                        nextFloors.add(destinationFloor);
                    } else {
                        throw new IllegalArgumentException();
                    }
                } catch (IllegalArgumentException e) {
                    System.out.println("Enter valid floor from 0 to 8");
                }
            }

            processPickup(lift);
            return lift.direction;
        }
        int[] upPossibility = countUp(lift);
        int[] downPossibility = countDown(lift);
        if (upPossibility[0] != downPossibility[0]) {
            return lift.direction = (upPossibility[0] > downPossibility[0]) ? Direction.UP : Direction.DOWN;
        } else {
            return lift.direction = (upPossibility[1] > downPossibility[1]) ? Direction.UP : Direction.DOWN;
        }

    }

    public int[] countUp(Lift lift) {
        int count = 0, size = 0;
        Iterator<Request> upDir = pendingRequests.tailSet(new Request(lift.currentFloor), false).iterator();

        while (upDir.hasNext()) {
            size += 1;
            Request up = upDir.next();
            if (up.getDirection().contains(Direction.DOWN)) {
                count += 1;
            }
        }
        return new int[]{count, size};
    }

    public int[] countDown(Lift lift) {
        int count = 0, size = 0;

        Iterator<Request> downDir = pendingRequests.headSet(new Request(lift.currentFloor), false).descendingSet().iterator();
        while (downDir.hasNext()) {
            size += 1;
            Request down = downDir.next();
            if (down.getDirection().contains(Direction.UP)) {
                count += 1;
            }
        }
        return new int[]{count, size};
    }


    // Read destination input only after the lift reaches a floor.
    public void processLift(Lift lift, Scanner scanner) {
        while (hasWork()) {
            Integer nextFloor = findNextFloor(lift, scanner);
            if (nextFloor == null) {
                break;
            }

            lift.currentFloor = nextFloor;
            System.out.println("\nLift reached floor " + lift.getCurrentFloor() + " ... " + "Lift direction: " + lift.getDirection());

            // Remove the destination that has just been reached before collecting new input.
            nextFloors.remove(lift.getCurrentFloor());
            System.out.println("Enter destination floors (-1 to finish this list):");
            while (true) {
                int destinationFloor;
                try {
                    destinationFloor = scanner.nextInt();
                } catch (InputMismatchException e) {
                    scanner.nextLine();
                    System.out.println("Enter valid floor from 0 to 8");
                    continue;
                }
                if (destinationFloor == -1) {
                    break;
                }
                try {
                    if (destinationFloor != lift.currentFloor && (destinationFloor >= 0 && destinationFloor <= 8)) {
                        nextFloors.add(destinationFloor);
                    } else {
                        throw new IllegalArgumentException();
                    }
                } catch (IllegalArgumentException e) {
                    System.out.println("Enter valid floor from 0 to 8 and destination floor should not be equal to pickup floor");
                }
            }
            processPickup(lift);
        }
        //if no work remaining
        lift.direction = Direction.IDLE;
        System.out.println("\nLift is now idle at floor " + lift.currentFloor);

    }

    // Remove the served direction,(if it is empty remove the request)
    public void processPickup(Lift lift) {
        Request request = new Request(lift.currentFloor);
        Request existing = pendingRequests.ceiling(request);

        if(existing != null && existing.compareTo(request) == 0) {
            if(existing.getDirection().size()==2) {
                // both buttons pressed: serve both in ONE stop only if the lift turns around here,
                // i.e. nothing more to do in the direction the lift is travelling
                if ((lift.direction == Direction.UP && nextFloors.higher(lift.currentFloor) == null && pendingRequests.last().getPickupFloor() <= existing.getPickupFloor()) ||
                        (lift.direction == Direction.DOWN && nextFloors.lower(lift.currentFloor) == null && pendingRequests.first().getPickupFloor() >= existing.getPickupFloor())) {
                    existing.getDirection().clear();
                } else {
                    existing.getDirection().remove(lift.direction);   // serve only the direction the lift is going
                }
            } else existing.getDirection().remove(lift.direction);
            if (existing.getDirection().isEmpty()) {
                pendingRequests.remove(existing);
            }
        }
    }

    private boolean hasWork() {
        return !pendingRequests.isEmpty() || !nextFloors.isEmpty();
    }
}
