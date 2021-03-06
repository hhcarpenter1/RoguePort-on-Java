import java.util.concurrent.ThreadLocalRandom;
import java.awt.Point;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class Level {
	protected final int MAXROOMS = 9;
	protected final int numDungeons = 4;
	protected int numLevel;
	protected int numR;
	/* Tells you what to put down after you leave a space */
	protected char last;
	protected boolean inside;
	protected boolean door;
	protected char[][] floor;
	protected boolean[][] isSeen;
	protected boolean[] rb;
	protected Rm[] rs;
	protected String itemNames;
	protected int itemX = 0;
	protected int itemY = 0;
	protected final char STAIR = '%';
	protected Point stairs;
	protected final char AMULET = ',';
	protected Point amulet;
	private ArrayList<Enemy> enemies;
	private ArrayList<Item> items;
	private Map<Character, Item> itemPos; // Maps each item to an item Position
	protected Player play;
	protected int hits;
	protected String narration;
	protected String narration2;
	protected boolean onStairs;

	public class Rm {
		int x1, x2, y1, y2, w, h, goldVal;
		boolean isDark;

		public Rm() {
			w = ThreadLocalRandom.current().nextInt(4, 25 + 1);
			h = ThreadLocalRandom.current().nextInt(4, 6 + 1);
			isDark = false;

			if (ThreadLocalRandom.current().nextInt(1, 100 + 1) < 5)
				isDark = true;

		}

		private void set(int x, int y) {
			x1 = x;
			y1 = y;
			x2 = x1 + w - 1;
			y2 = y1 + h - 1;
		}
	}

	public Level(int nL, Player pp) {
		// starts at 0 so amulet is on 25
		numLevel = nL;
		numR = ThreadLocalRandom.current().nextInt(5, 8 + 1);
		isSeen = new boolean[24][80];
		floor = new char[24][80];
		rb = new boolean[9];
		rs = new Rm[9];
		play = pp;

		/* set everything to 0, which means empty */
		for (int y = 0; y < 24; y++)
			for (int x = 0; x < 80; x++)
				floor[y][x] = ' ';

		makeRooms();
		doors();
		placeGold();

		enemies = new ArrayList<Enemy>();
		items = new ArrayList<Item>();

		itemPos = new HashMap<Character, Item>();

		for (int i = 0; i < 6; i++) {
			makeItem();
			makeEnemy();
		}

		// initialize to false
		for (int y = 0; y < 24; y++)
			for (int x = 0; x < 80; x++)
				isSeen[y][x] = false;

		/*
		 * Spawn Player and Light up its room. Use %|/ 3 to find the room
		 */
		spawnP();
		last = '.';
		hits = 0;

		/*
		 * Spawn stair for current level Should be above player It would be
		 * place everything. Then fit player
		 */
		this.stairs = findS();
		floor[stairs.y][stairs.x] = STAIR;

		narration = "";
		narration2 = "";

		onStairs = false;

		/* What we have been waiting for! */
		if (numLevel == numDungeons) {
			amulet = findS();
			floor[amulet.y][amulet.x] = ',';
			System.out.println("The Amulet has spawned at x = " + amulet.x + " y = " + amulet.y);
		}
	}

	public void removeP() {
		for (int y = 0; y < floor.length; y++) {
			for (int x = 0; x < floor[y].length; x++) {
				if (floor[y][x] == play.val)
					floor[y][x] = '.';
			}
		}
	}

	public void ascend(Player pp) {
		for (int y = 0; y < floor.length; y++) {
			for (int x = 0; x < floor[y].length; x++) {
				if (floor[y][x] == '@')
					floor[y][x] = '.';
			}
		}

		play = pp;
		spawnP();
		seeRm(getCurRoom(play));
		last = '.';
		hits = 0;

		narration = "";
		narration2 = "";

		onStairs = false;
	}

	/*
	 * Connects all rooms
	 */
	private void doors() {
		boolean[] connect = new boolean[9];
		for (int i = 0; i < MAXROOMS - 1; i++) {
			if (i % 3 < 2 && rb[i] && rb[i + 1]) {
				conn(i, i + 1, 'r');
				connect[i] = connect[i + 1] = true;
			} else if (i % 3 == 0 && rb[i] && rb[i + 2]) {
				conn(i, i + 2, 'r');
				connect[i] = connect[i + 2] = true;
			}
			if (i / 3 < 2 && rb[i] && rb[i + 3]) {
				conn(i, i + 3, 'd');
				connect[i] = connect[i + 3] = true;
			} else if (i / 3 == 0 && rb[i] && rb[i + 6]) {
				conn(i, i + 6, 'd');
				connect[i] = connect[i + 6] = true;
			}
		}
		for (int i = 0; i < connect.length; i++) {
			if (connect[i])
				rb[i] = true;
			else
				rb[i] = false;
		}
	}

	/*
	 * Connects two rooms
	 */
	private void conn(int r1, int r2, char direc) {
		int turn;

		if (direc == 'r') {
			/* the x coords are already set by the room of course! */
			/* And d means door */
			int d1y = ThreadLocalRandom.current().nextInt(rs[r1].y1 + 1, rs[r1].y2);
			int d2y = ThreadLocalRandom.current().nextInt(rs[r2].y1 + 1, rs[r2].y2);
			turn = ((rs[r2].x1 + rs[r1].x2 + 2) / 2);
			// turn = ThreadLocalRandom.current().nextInt(rs[r2].x2+1,
			// rs[r2].x1);

			floor[d1y][rs[r1].x2] = '+';
			floor[d2y][rs[r2].x1] = '+';
			/* Left Side */
			for (int x = rs[r1].x2 + 1; x <= turn; x++) {
				floor[d1y][x] = '#';
			}
			/* Find top door */
			int top = d1y;
			int bottom = d2y;
			if (d1y < d2y) {
				top = d2y;
				bottom = d1y;
			}
			/* Move Up */
			for (int up = bottom; up <= top; up++) {
				floor[up][turn] = '#';
			}
			/* Right Side */
			for (int x = turn; x < rs[r2].x1; x++) {
				floor[d2y][x] = '#';
			}
		} else if (direc == 'd') {
			int d1x = ThreadLocalRandom.current().nextInt(rs[r1].x1 + 1, rs[r1].x2);
			int d2x = ThreadLocalRandom.current().nextInt(rs[r2].x1 + 1, rs[r2].x2);
			turn = ((rs[r2].y1 - rs[r1].y2) / 2) + rs[r1].y2;
			// turn = ThreadLocalRandom.current().nextInt(rs[r2].x2+1,
			// rs[r2].x1);

			/* Doors */
			floor[rs[r1].y2][d1x] = '+';
			floor[rs[r2].y1][d2x] = '+';
			/* Top */
			for (int y = rs[r1].y2 + 1; y <= turn; y++) {
				floor[y][d1x] = '#';
			}
			/* Find Right Door */
			int left = d1x;
			int right = d2x;
			if (d1x > d2x) {
				left = d2x;
				right = d1x;
			}
			/* Move Right */
			for (int x = left; x <= right; x++) {
				floor[turn][x] = '#';
			}
			/* Bottom */
			for (int y = turn; y < rs[r2].y1; y++) {
				floor[y][d2x] = '#';
			}
		} else {
			System.out.println("Oops! There is an error in conn().");
		}
	}

	/**
	 * Place gold in each room
	 */
	private void placeGold() {
		Point temp = new Point();

		// 50% chance of spawning
		for (int i = 0; i < rs.length; i++) {
			if (ThreadLocalRandom.current().nextBoolean() && rb[i]) {
				// random gold value
				rs[i].goldVal = ThreadLocalRandom.current().nextInt(1, 50 + 1);

				char c = '0';

				while (c != '.') {
					temp.x = ThreadLocalRandom.current().nextInt(rs[i].x1 + 1, rs[i].x2);
					temp.y = ThreadLocalRandom.current().nextInt(rs[i].y1 + 1, rs[i].y2);
					c = floor[temp.y][temp.x];

					floor[temp.y][temp.x] = '*';
				}
			}
		}
	}

	protected void spawnP() {
		play.p = findS();
		floor[play.p.y][play.p.x] = play.val;
		inside = true;

		if (!getCurRoom(play).isDark) {
			seeRm(getCurRoom(play));
		} else {
			// only draw outline of the room and show squares around player
			for (int i = 0; i < 3; i++) {
				for (int j = 0; j < 3; j++) {
					isSeen[(play.p.y) + i - 1][(play.p.x) + j - 1] = true;
				}
			}
		}
	}

	/*
	 * Finds a random empty square inside a room
	 */
	protected Point findS() {
		Point rd = new Point();
		/* Find a room that exists */
		int roomN = ThreadLocalRandom.current().nextInt(0, 8 + 1);
		while (!rb[roomN])
			roomN = ThreadLocalRandom.current().nextInt(0, 8 + 1);

		Rm r = rs[roomN];
		char c = '0';
		while (c != '.') {
			rd.x = ThreadLocalRandom.current().nextInt(r.x1 + 1, r.x2);
			rd.y = ThreadLocalRandom.current().nextInt(r.y1 + 1, r.y2);
			c = floor[rd.y][rd.x];
		}
		return rd;
	}

	/*
	 * Need to store the Item Point Public not protected
	 */
	private void makeItem() {
		ArrayList<String> itemStrings = new ArrayList<String>();
		for (Item i : items) {
			itemStrings.add(i.name);
		}

		Item i = new Item();
		Point spot = findS();
		i.generateItem();
		itemY = spot.y;
		itemX = spot.x;

		if (!itemStrings.contains(itemStrings))
			items.add(i);
		floor[itemY][itemX] = i.getBoardName();
		char c = floor[itemY][itemX];

		itemNames = i.getPickUpMessage();
		itemPos.put(c, i);
	}

	private void makeEnemy() {
		Enemy e = new Enemy(numLevel, play.level);
		e.p = findS();
		enemies.add(e);
		floor[e.p.y][e.p.x] = e.val;
	}

	/* I loosely implemented this in move when an Enemy is killed. */
	public void removeUnit(Unit u) {
	}

	/*
	 * grid: x: 0__25#27__52#54__79 y: 0__6#8__14#16__22#
	 */
	private void makeRooms() {
		for (int i = 0; i < numR; i++) {
			int roomN = ThreadLocalRandom.current().nextInt(0, 8 + 1);
			while (rb[roomN])
				roomN = ThreadLocalRandom.current().nextInt(0, 8 + 1);

			Rm r = new Rm();
			int x = ThreadLocalRandom.current().nextInt((roomN % 3) * 27, ((roomN % 3) * 27) + 25 - r.w + 1);
			int y = ThreadLocalRandom.current().nextInt((roomN / 3) * 8, ((roomN / 3) * 8) + 6 - r.h + 1);
			r.set(x, y);

			rs[roomN] = drawRoom(r);
			rb[roomN] = true;
		}
	}

	private Rm drawRoom(Rm r) {
		// Top and Bottom
		for (int w = r.x1; w <= r.x2; w++) {
			floor[r.y1][w] = '-';
			floor[r.y2][w] = '-';
		}
		// Inside and Sides
		for (int h = r.y1 + 1; h < r.y2; h++) {
			floor[h][r.x1] = '|';
			for (int w = r.x1 + 1; w < r.x2; w++) {
				floor[h][w] = '.';
			}
			floor[h][r.x2] = '|';
		}
		return r;
	}

	/**
	 * returns board as viewed by player
	 */
	public char[][] getFloor() {
		char[][] pfloor = new char[24][80];
		for (int y = 0; y < floor.length; y++) {
			for (int x = 0; x < floor[y].length; x++) {
				if (isSeen[y][x]) {
					pfloor[y][x] = floor[y][x];
				}
			}
		}
		hideEnemy(pfloor);

		return pfloor;
	}

	public int throwItem(int itemNum, Player u, int[] dir) {
		narration = "";
		Point a = u.p;
		if (play.inventory.getInventorySpace() > itemNum) {
			if (floor[a.y + dir[1]][a.x + dir[0]] != '.') {
				narration = "Something is in the way. You need to choose a different spot for throwing.";
			} else {
				Item item = play.items.get(itemNum);
				narration = "You" + item.getDropMessage();
				floor[a.y + dir[1]][a.x + dir[0]] = item.getBoardName();
				play.items.remove(item);
				play.inventory.removeItem(item);
			}
		} else {
			narration = "The index is too large for the inventory's size.";
		}
		return 7;
	}

	// Updates the player's stats after equiping or consuming an item
	public int updatePlayerStatsAfterEquip(int itemNum) {
		narration = "";
		if (play.inventory.getInventorySpace() > itemNum) {
			Item item = play.items.get(itemNum);
			play.useItem(item);
			narration = "You" + item.getUseMessage();
			play.items.remove(item);
			play.inventory.removeItem(item);
		} else {
			narration = "The index is too large for the inventory's size.";
		}

		return 6;
	}

	public int printInventory() {
		narration = "";
		if (play.items.size() != 0) {
			narration += "Current Inventory: ";
		} else {
			narration = "Nothing stored in Inventory";
		}

		int count = 0;
		for (Item i : play.items) {
			identifyScrollHelper(i, count);
			count++;
		}

		return 5;
	}

	public void identifyScrollHelper(Item i, int count) { // Helper Method for
															// Move Unit
		if (play.idenitfyWeapon == true && (i.getItemType().equalsIgnoreCase("Weapon"))) {
			narration += i.getItemName() + " ";
		}
		if (play.identifyScroll == true && (i.getItemType().equalsIgnoreCase("Scroll"))) {
			narration += i.name + " ";
		}
		if (play.identifyPotion == true && (i.getItemType().equalsIgnoreCase("Potions"))) {
			narration += i.getItemName() + " ";
		}
		if (play.foodDetection == true && (i.getItemType().equalsIgnoreCase("Food"))) {
			narration += i.getItemName() + " ";
		}
		if (play.identifyArmor == true && (i.getItemType().equalsIgnoreCase("Armor"))) {
			narration += i.getItemName() + " ";
		}
		if (play.identifyWandRing == true
				&& (i.getItemType().equalsIgnoreCase("Ring") || i.getItemType().equalsIgnoreCase("Wand"))) {
			narration += i.getItemName() + " ";
		}

		else {
			narration += i.typeItem + " ";
			narration += count;

		}
		if (count != play.items.size() - 1) {
			narration += ", ";
		}
	}

	public boolean boundsCheck(Point a, int[] dir) { // Helper Method for
														// MoveUnit
		return ((a.x + dir[0]) < 0 || (a.y + dir[1]) < 0 || (a.y + dir[1]) > 23 || (a.x + dir[0]) > 79);
	}

	public void inventoryAction(char c, int[] dir, Point a) // Helper methord
															// for moveUnit
	{
		if (play.inventory.addItem(itemPos.get(c)) == false) {
			narration = "Can't add item. Inventory is full.";
			floor[a.y][a.x] = last;
			last = c;
			a.setLocation(a.x + dir[0], a.y + dir[1]);
			floor[a.y][a.x] = '@';
		} else {
			Item temp = itemPos.get(c);
			narration = "You" + " " + itemPos.get(c).getPickUpMessage();
			identifyScrollHelper2(temp);
			play.items.add(itemPos.get(c));
			floor[a.y + dir[1]][a.x + dir[0]] = '.';
		}
	}

	/*
	 * Return Narration String Returns: 0 = moved 1 = fighting 2 = cant move
	 * Call new method moveAllMonsters();
	 */
	public int moveUnit(Player u, int[] dir) {
		onStairs = false;
		Point a = u.p;
		/* Bounds check */
		if (boundsCheck(a, dir)) {
			return 2;
		}
		char c = floor[a.y + dir[1]][a.x + dir[0]];

		if (u.hunger <= 0) {
			u.dead = true;
			return 12;
		}

		if (c == AMULET) {
			narration = "";
			floor[a.y + dir[1]][a.x + dir[0]] = '@';
			floor[a.y][a.x] = last;
			u.p = new Point(a.x + dir[0], a.y + dir[1]);
			narration = "You collected the Amulet!";
			last = '.';
			return 26;
		} else if (Character.isLetter(c)) {
			for (Enemy e : enemies) {
				if (e.p.x == a.x + dir[0] && e.p.y == a.y + dir[1]) {
					fight(e);
					break;
				}
			}

			if (u.dead)
				return 12;
			return 1;
		} else if (validMove(c)) {
			if (isItem(c)) {
				inventoryAction(c, dir, a);
				return 3;
			}

			floor[a.y][a.x] = last;
			last = c;
			a.setLocation(a.x + dir[0], a.y + dir[1]);
			floor[a.y][a.x] = '@';

			// shows everything if room is not dark, shows surrounding area if
			// it is dark
			if (isInRoom(u) && !(getCurRoom(u).isDark)) {
				seeRm(getCurRoom(u));

			} else {// otherwise room is dark
				makeDark(getCurRoom(u));

			}
			// quick fix, make every dark room dark again instead of the one
			// you've just left
			for (int i = 0; i < rs.length; i++) {
				if (rb[i] && rs[i].isDark) {
					makeDark(rs[i]);
				}
			}

			// shows squares around player
			for (int i = 0; i < 3; i++) {
				for (int j = 0; j < 3; j++) {
					isSeen[a.y + i - 1][a.x + j - 1] = true;
				}
			}
			if (foundGold(c, u)) {
				return 3;
			}

			narration = "";
			narration = play.move();
			if (isHungry()) {
				return 9;
			} else
				return 0;
		} else if (c == STAIR) {
			onStairs = true;
			if (numLevel < numDungeons) {
				for (int i = 0; i < floor.length; i++) // Gets rid of @'s evil
														// twin while going up
				{
					for (int j = 0; j < floor[i].length; j++) {
						if (floor[i][j] == '@') {
							floor[i][j] = '.';
						}
					}

				}
			}

			return 6;
		}

		return 2;
	}

	private boolean isHungry() { // Helper Method for moveUnit
		return (narration.equalsIgnoreCase("You should probably stop by the Gizmo.")
				|| narration.equals("You are starving!"));
	}

	private boolean foundGold(char c, Player u) // Helper Method for moveUnit
	{
		if (c == '*' && getCurRoom(u).goldVal != 0) {
			u.gold += getCurRoom(u).goldVal;
			narration = "You've picked up " + getCurRoom(u).goldVal + " gold";
			getCurRoom(u).goldVal = 0;
			if (isInRoom(u)) {
				last = '.';
			} else {
				last = '#';
			}

			return true;
		} else {
			return false;
		}

	}

	private void identifyScrollHelper2(Item temp) {
		if (play.idenitfyWeapon && (temp.getItemType().equalsIgnoreCase("Weapon"))) {
			narration += " You identified " + temp.getItemName();
		}
		if (play.identifyScroll && (temp.getItemType().equalsIgnoreCase("Scroll"))) {
			narration += " You identified " + temp.getItemName();
		}
		if (play.identifyPotion && (temp.getItemType().equalsIgnoreCase("Potions"))) {
			narration += " You identified " + temp.getItemName();
		}
		if (play.foodDetection && (temp.getItemType().equalsIgnoreCase("Food"))) {
			narration += " You identified " + temp.getItemName();
		}
		if (play.identifyArmor && (temp.getItemType().equalsIgnoreCase("Armor"))) {
			narration += " You identified " + temp.getItemName();
		}
		if (play.identifyWandRing
				&& (temp.getItemType().equalsIgnoreCase("Ring") || temp.getItemType().equalsIgnoreCase("Wand"))) {
			narration += " You identified " + temp.getItemName();
		}
	}

	/*
	 * Error in GamePlay: need to have a String array So that we can have two
	 * narrations to space through
	 */
	public String fight(Enemy e) {
		play.fight = e.fight = true;
		String n = "";
		int patk = play.attack();
		e.hp -= patk;
		narration = "You hit the " + e.name + " for " + patk + " damage!";
		if (e.hp <= 0) {
			floor[e.p.y][e.p.x] = e.lastChar;
			play.xp += e.xp;
			narration = "You defeated the " + e.name + "!";
			narration2 = "";
			enemies.remove(e);
			play.fight = false;
		} else {
			/* fight back */
			Random r = new Random();
			int armorLuck = 0;
			if (play.armor >= 1) {
				armorLuck = r.nextInt(play.armor);
			} else {
				play.armor = 1;
				armorLuck = r.nextInt(play.armor);

			}

			int eatk = e.getDMG() - armorLuck;
			if (eatk >= 1) {

				play.hp -= eatk;
				play.armor -= 1;
			} else {
				eatk = 0;

			}

			narration2 = "The " + e.name + " attacked you for " + eatk + " damage!";

			if (play.hp <= 0) {
				play.dead = true;
				narration2 = "Sorry. You died!";
			}
		}
		e.chase = true;
		return n;
	}

	private boolean validMove(char c) {
		return (c == '.' || c == '#' || c == '+' || c == '*' || isItem(c));
	}

	private boolean isItem(char c) {
		return (c == ':' || c == '!' || c == '/' || c == ']' || c == '?' || c == ')' || c == '=');
	}

	/**
	 * checks if unit is currently in a room
	 */
	private boolean isInRoom(Unit u) {
		Point a = u.p;
		for (int i = 0; i < rs.length; i++) {
			if (rb[i] && a.x >= rs[i].x1 && a.x <= rs[i].x2 && a.y >= rs[i].y1 && a.y <= rs[i].y2) {
				return true;
			}
		}
		return false;
	}

	/**
	 * check which room the unit is in
	 */
	protected Rm getCurRoom(Unit u) {
		Point a = u.p;
		for (int i = 0; i < rs.length; i++) {
			if (rb[i] && a.x >= rs[i].x1 && a.x <= rs[i].x2 && a.y >= rs[i].y1 && a.y <= rs[i].y2) {
				return rs[i];
			}
		}
		return new Rm();
	}

	/**
	 * hides the floor unless it is within vision range of the player
	 */
	private void makeDark(Rm curRoom) {
		for (int x = (curRoom.x1 + 1); x < curRoom.x2; x++) {
			for (int y = (curRoom.y1 + 1); y < curRoom.y2; y++) {
				isSeen[y][x] = false;
			}
		}
	}

	/**
	 * hide enemies in rooms that player aren't in
	 */
	private void hideEnemy(char[][] playerFloor) {
		for (int i = 0; i < enemies.size(); i++) {
			Point a = (enemies.get(i)).p;
			if (isSeen[a.y][a.x]) {
				boolean seePlayer = false;
				for (int j = 0; j < 3; j++) {
					for (int k = 0; k < 3; k++) {
						if (floor[a.y + j - 1][a.x + k - 1] == play.val) {
							seePlayer = true;

						}
					}
				}
				if (seePlayer) {
					// do nothing
				} else if (isInRoom(play) && getCurRoom(play).equals(getCurRoom(enemies.get(i)))) {
					// do nothing
				} else {
					playerFloor[a.y][a.x] = enemies.get(i).lastChar;
				}
			}
		}
	}

	/**
	 * Move random enemy
	 */
	protected void moveEnemy(int[] previous) {
		for (Enemy e : enemies) {
			int x1, x2, y1, y2;
			char c = '|';
			x1 = x2 = e.p.x;
			y1 = y2 = e.p.y;
			if (e.chase) {
				/* If chase, then move to last spot of player */
				x2 = play.p.x - previous[0];
				y2 = play.p.y - previous[1];
			} else if (ThreadLocalRandom.current().nextBoolean()) {
				/*
				 * Otherwise move randomly. 50% chance. int[] leftdown = { -1,
				 * -1 };
				 */
				int tries = 0;
				while (tries < 9 && !validMove(c)) {
					int move = 1;
					if (ThreadLocalRandom.current().nextBoolean())
						move = -1;
					if (ThreadLocalRandom.current().nextBoolean()) {
						x2 = e.p.x + move;
						y2 = e.p.y;
					} else {
						x2 = e.p.x;
						y2 = e.p.y + move;
					}
					c = floor[y2][x2];
					tries++;
				}
			}

			if (validMove(c) && !e.fight && e.type.contains('M')) {
				floor[y1][x1] = e.lastChar;
				e.lastChar = floor[y2][x2];
				e.p.setLocation(x2, y2);
				floor[y2][x2] = e.val;
			}
		}
	}

	protected void seeRm(Rm r) {
		for (int y = r.y1; y <= r.y2; y++) {
			for (int x = r.x1; x <= r.x2; x++) {
				isSeen[y][x] = true;
			}
		}
	}

	/* Accounts for 0 indexed dungeon array */
	public String stats() {
		int x = this.numLevel + 1;
		return "Level: " + x + play.pStats();
	}
}