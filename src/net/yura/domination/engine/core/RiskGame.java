// Yura Mamyrin, Group D

package net.yura.domination.engine.core;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.StringTokenizer;
import java.util.Vector;

import net.yura.domination.engine.ColorUtil;
import net.yura.domination.engine.RiskObjectOutputStream;
import net.yura.domination.engine.RiskUtil;
import net.yura.domination.engine.translation.MapTranslator;
import net.yura.domination.engine.translation.TranslationBundle;

/**
 * <p> Risk Game Main Class </p>
 * @author Yura Mamyrin
 */

public final class RiskGame implements Serializable { // transient

	private static final long serialVersionUID = 8L;
	/**
	 * these are a costant 
	 */
	public final static String SAVE_VERSION = String.valueOf(serialVersionUID);
	/**
	 * costant String Version Network
	 */
	public final static String NETWORK_VERSION = "12";
	/**
	 * costant Max Players of Integer Types
	 */
	public final static int MAX_PLAYERS = 6;
	/**
	 * costant about Type Continent
	 */
	public final static Continent ANY_CONTINENT = new Continent("any","any", 0, 0);

	/**
	 * Costant int State New Game integer Type Level 0
	 */
	public final static int STATE_NEW_GAME        = 0;
	/**
	 * Costant int State Trade Card integer Type Level 1
	 */
	public final static int STATE_TRADE_CARDS     = 1;
	/**
	 * Costant int State Place about Armies integer Type Level 1
	 */
	public final static int STATE_PLACE_ARMIES    = 2;

	/**
	 * Costant int State Attacking integer Type Level 3
	 */
	public final static int STATE_ATTACKING       = 3;
	/**
	 * Costant int State State Rolling Type Level 4
	 */
	public final static int STATE_ROLLING         = 4;
	/**
	 * Costant int State Battle Type Level 5
	 */
	public final static int STATE_BATTLE_WON      = 5;
	/**
	 * Costant int State Fortifyng Type Level 6
	 */
	public final static int STATE_FORTIFYING      = 6;
	/**
	 * Costant int State about and Turn 7
	 */
	public final static int STATE_END_TURN        = 7;
	/**
	 * Costant int State the End (Game Over) Type Level 8
	 */
	public final static int STATE_GAME_OVER       = 8;
	/**
	 * Costant int Selection Type Level 9
	 */
	public final static int STATE_SELECT_CAPITAL  = 9;
	/**
	 * Costant int Defend youself Type Level 10
	 */
	public final static int STATE_DEFEND_YOURSELF = 10;

	/**
	 * Costant int State Modality Domination Type Level 0
	 */

	public final static int MODE_DOMINATION     = 0;
	/**
	 * Costant int State Mode Capital Type Level 1
	 */
	public final static int MODE_CAPITAL        = 1;
	/**
	 * Costant int State Mode Secret Mission Type Level 3
	 */
	public final static int MODE_SECRET_MISSION = 3;
	/**
	 * Costant int State Increase Card Type Level 0
	 */

	public final static int CARD_INCREASING_SET = 0;
	/**
	 * Costant int State Card Fixed Type Level 1
	 */
	public final static int CARD_FIXED_SET = 1;
	/**
	 * Costant int State Card Italian Like Type Level 2
	 */
	public final static int CARD_ITALIANLIKE_SET = 2;

	/**
	 * Costant int State MAXCARD Type Level 5
	 */

	public final static int MAX_CARDS = 5;

/*

//	public final static int MODE_DOMINATION_2   = 1;

gameState:

nogame	(-1 in gui)		(current possible commands are: newgame, loadgame, closegame, savegame)

9 - select capital		(current possible commands are: capital)
10 - defend yourself!		(current possible commands are: roll)
0 - new game just created	(current possible commands are: newplayer, delplayer, startgame)
1 - trade cards			(current possible commands are: showcards, trade, notrade)
2 - placing new armies		(current possible commands are: placearmies, autoplace)
3 - attacking 			(current possible commands are: attack endattack)
4 - rolling			(current possible commands are: roll retreat)
5 - you have won!		(current possible commands are: move)
6 - fortifying			(current possible commands are: movearmy nomove)
7 - endturn			(current possible commands are: endgo)
8 - game is over		(current possible commands are: continue)

gameMode:
0 - WORLD DOMINATION RISK	- 3 to 6 players
//1 - WORLD DOMINATION RISK	- 2 player
2 - CAPITAL RISK		- 3 to 6 players
3 - SECRET MISSION RISK	- 3 to 6 players

playerType:
0 - human
1 - AI (Easy)
2 - AI (Hard)
3 - AI (Crap)

transient - A keyword in the Java programming language that indicates that a field is not part of the serialized form of an object. When an object is serialized, the values of its transient fields are not included in the serial representation, while the values of its non-transient fields are included.

*/














	// ---------------------------------------
	// THIS IS THE GAME INFO FOR Serialization
	// ---------------------------------------

	private Random r; // mmm, not sure where this should go, may stop cheeting when its here

	// cant use URL as that stores full URL to the map file on the disk,
	// and if the risk install dir changes the saves dont work
	private String mapfile;
	private String cardsfile;
	private int setup;

	private Vector Players;
	private Country[] Countries;
	private Continent[] Continents;
	private Vector Cards,usedCards;
	private Vector Missions;

	private Player currentPlayer;
	private int gameState;
	private int cardState;
	private int mustmove;
	private boolean capturedCountry;
	private boolean tradeCap;
	private int gameMode;

	private Country attacker;
	private Country defender;

	private int attackerDice;
	private int defenderDice;

	private transient int battleRounds;

	private String ImagePic;
	private String ImageMap;
	private String previewPic;

	private Map properties;

	private Vector replayCommands;
	private int maxDefendDice;
	private int cardMode;

	private boolean runmaptest=false;
	private boolean recycleCards=false;


	/**
	 * Creates a new RiskGame
	 */
	public RiskGame() throws Exception {

            //try {
            
		setCardsfile("default");
		//}
		//catch (Exception e) {
		//	RiskUtil.printStackTrace(e);
		//}

		setup=0; // when setup reaches the number of players it goes into normal mode

		Players = new Vector();

		currentPlayer=null;
		gameState=STATE_NEW_GAME;
		cardState=0;

		replayCommands = new Vector();

		//System.out.print("New Game created\n"); // testing

		//simone=true;//false;

		r = new Random();
	}

	public void addCommand(String a) {

		replayCommands.add(a);

	}

	public Vector getCommands() {

		return replayCommands;

	}

	public void setCommands(Vector replayCommands) {
		this.replayCommands = replayCommands;
	}

	public int getMaxDefendDice() {
		return maxDefendDice;
	}

	/**
	 * This adds a player to the game
	 * @param type Type of game (i.e World Domination, Secret Mission, Capital)
	 * @param name Name of player
	 * @param color Color of player
	 * @return boolean Returns true if the player is added, returns false if the player can't be added.
	 */
	public boolean addPlayer(int type, String name, int color, String a) {
		if (gameState==STATE_NEW_GAME ) { // && !name.equals("neutral") && !(color==Color.gray)
                        int size = Players.size();
			for (int c=0; c< size ; c++) {
				if (( name.equals(((Player)Players.elementAt(c)).getName() )) || (color ==  ((Player)Players.elementAt(c)).getColor() )) return false;
			}

			//System.out.print("Player added. Type: " +type+ "\n"); // testing
			Player player = new Player(type, name, color , a);
			Players.add(player);
			return true;
		}
		else return false;
	}

	/**
	 * This deletes a player in the game
	 * @param name Name of the player
	 * @return boolean Returns true if the player is deleted, returns false if the player cannot be deleted
	 */
	public boolean delPlayer(String name) {
		if (gameState==STATE_NEW_GAME) {

			int n=-1;
                        int size =  Players.size() ;
			for (int c=0; c< size ; c++) {
				if (name.equals( ((Player)Players.elementAt(c)).getName() )) n=c;
			}
			if (n==-1) {
				//System.out.print("Error: No player found\n"); // testing
				return false;
			}
			else {
				Players.removeElementAt(n);
				Players.trimToSize();
				//System.out.print("Player removed\n"); // testing
				return true;
			}
		}
		else return false;

	}

	/**
	 * Starts the game Risk
	 * @param mode This represents the moce of the game: normal, 2 player, capital or mission
	 */
        public void startGame1(){
            if (Countries==null) { return; }

			if (gameMode==MODE_SECRET_MISSION && Missions.size() < Players.size() ) { return; }

			int armies = ( 10 - Players.size() ) *  Math.round( Countries.length * 0.12f );

			// System.out.print("armies="+ armies +"\n");
			//
			//if (gameMode==1) { // 2 player mode
			//	Player player = new Player(3, "neutral", Color.gray , "all" );
			//	Players.add(player);
			//}
			//
			//System.out.print("Game Started\n"); // testing
                        int size = Players.size() ;
			for (int c=0; c< size ; c++) {
				((Player)Players.elementAt(c)).addArmies(armies);
			}

        }
        public void startGame2(){
            try {

					loadCards(false);

				}
				catch (Exception e) {

					if (runmaptest) {

						//System.out.println("LOAD FILE ERROR: " + e.getMessage() + "\n(This normally means you have selected the wrong set of cards for this map)"); // testing
						//RiskUtil.printStackTrace(e);
						System.out.println("LOAD FILE ERROR: " + e.getMessage() + "\n(This normally means you have selected the wrong set of cards for this map)");

					}

					return;
				}
        }
	public void startGame(int mode, int card, boolean recycle, boolean threeDice) throws Exception {

		if (gameState==STATE_NEW_GAME) { //  && ((mapfile !=null && cardsfile !=null) || () )

			gameMode=mode;
			cardMode=card;

			recycleCards = recycle;
			maxDefendDice = threeDice?3:2;

			// 2 player human crap
			//if ( gameMode==1 && ( !(((Player)Players.elementAt(0)).getType()==0) || !(((Player)Players.elementAt(1)).getType()==0) ) ) { return; }


			// check if things need to be loaded, maybe already loaded, then these will be null
			if (mapfile!=null && cardsfile!=null) {


				//try {

				loadMap();

				//}
				//catch (Exception e) {
				//	RiskUtil.printStackTrace(e);
				//	return;
				//}

				startGame2();


				if (runmaptest) {

					//try {
					testMap(); // testing maps
					//}
					//catch (Exception e) {
					//	RiskUtil.printStackTrace(e);
					//	return;
					//}
				}

			}
                        startGame1();
			
			gameState=STATE_PLACE_ARMIES;
			capturedCountry=false;
			tradeCap=false;

		}

	}

	/**
	 * this code is used to check if the borders in the map file are ok
	 */
	public void testMap() throws Exception {

		//System.out.print("Starting map test...\n");

		for (int c=0; c< Countries.length ; c++) {

			Country c1 = Countries[c];
			Vector c1neighbours = (Vector)c1.getNeighbours();
                         int size = c1neighbours.size();
			if (c1neighbours.contains(c1)) { System.err.println("Error: "+c1.getName()+" neighbours with itself"); }

			for (int a=0; a< size ; a++) {

				Country c2 = (Country)c1neighbours.elementAt(a);
				Vector c2neighbours = (Vector)c2.getNeighbours();

				boolean ok=false;

				for (int b=0; b< size ; b++) {

					Country c3 = (Country)c2neighbours.elementAt(b);

					if ( c1 == c3 ) { ok=true; }

				}

				if (ok==false) {
					throw new Exception("Borders error with: " + Countries[c].getName() + " ("+Countries[c].getColor()+") and " + ((Country)c1neighbours.elementAt(a)).getName() +" ("+((Country)c1neighbours.elementAt(a)).getColor()+")" ); // Display
				}

			}
		}

		//System.out.print("End map test.\n");

	}

	/**
	 * Sets the current player in the game
	 * @return Player Returns the current player in the game
	 */
	public Player setCurrentPlayer(int c) {
		currentPlayer = (Player)Players.get(c);
		return currentPlayer;

	}

	/**
	 * Gets the current player in the game
	 * @return String Returns the name of a randomly picked player from the set of players
	 */
	public int getRandomPlayer() {
		return r.nextInt( Players.size() );
	}

	/**
	 * Checks whether the player deserves a card during at the end of their go
	 * @return String Returns the name of the card if deserves a card, else else returns empty speech-marks
	 */
	public String getDesrvedCard() {
		//check to see if the player deserves a new risk card
		if (capturedCountry==true && Cards.size() > 0) {

			Card c = (Card)Cards.elementAt( r.nextInt(Cards.size()) );
			if (c.getCountry() == null) return Card.WILDCARD;
			else return ( (Country)c.getCountry() ).getColor()+"";
		}
		else {
			return "";
		}
	}

	public boolean isCapturedCountry() {
		return capturedCountry;
	}

	/**
	 * Ends a player's go
	 * @return Player Returns the next player
	 */
        public void endGo1(){
            	for (int c=0; c< Continents.length ; c++) {

					if ( Continents[c].isOwned(currentPlayer) ) {
						currentPlayer.addArmies( Continents[c].getArmyValue() );
                                        }
                }
        }
        public void endGo2(){
            int size = Players.size();
             for (int c=0; c< size ; c++) {
					if ( currentPlayer==((Player)Players.elementAt(c)) && Players.size()==(c+1) ) {
						currentPlayer=(Player)Players.elementAt(0);
						c=Players.size();
					}
					else if ( currentPlayer==((Player)Players.elementAt(c)) && Players.size() !=(c+1) ) {
						currentPlayer=(Player)Players.elementAt(c+1);
						c=Players.size();
					}
				}
         }
        public void endGo3(){
            if (getSetupDone() && gameMode==2 && currentPlayer.getCapital() == null) { // capital risk setup not finished
				gameState=STATE_SELECT_CAPITAL;
			}
			else if ( canTrade()==false ) { // ie the initial setup has not been compleated or there are no cards that can be traded
				gameState=STATE_PLACE_ARMIES;
			}
			else { // there are cards that can be traded
				gameState=STATE_TRADE_CARDS;
			}
        }
        public void endGo4(){
            if ( currentPlayer.getNoTerritoriesOwned() < 9 ) {
					currentPlayer.addArmies(3);
				}
				else {
					currentPlayer.addArmies( currentPlayer.getNoTerritoriesOwned() / 3 );
				}
        }
	public void endGo() {

		if (gameState==STATE_END_TURN) {

			//System.out.print("go ended\n"); // testing

			// work out who is the next player

			while (true) {
                                         endGo2();
				

				if (!getSetupDone()) { break; }

				// && (currentPlayer.getType() != 3)

				else if ( currentPlayer.getNoTerritoriesOwned() > 0       ) {break; }

			}

			//System.out.print("Curent Player: " + currentPlayer.getName() + "\n"); // testing

			if ( getSetupDone() && !(gameMode==2 && currentPlayer.getCapital() == null) ) { // ie the initial setup has been compleated

				workOutEndGoStats( currentPlayer );
				currentPlayer.nextTurn();

				// add new armies for the Territories Owned
				endGo4();

				// add new armies for the Continents Owned
                                endGo1();
			

			}
                          endGo3();
			

			capturedCountry=false;
			tradeCap=false;

			

		}
		else {
                    System.out.println("lala "+gameState);

		}
	}

	/**
	 * Trades a set of cards
	 * @param card1 First card to trade
	 * @param card2 Second card to trade
	 * @param card3 Third card to trade
	 * @return int Returns the number of armies gained from the trade, returning 0 if the trade is unsuccessful
	 */
        public int trade1(){
          
            if (gameState!=STATE_TRADE_CARDS) return 0;

		if (tradeCap && currentPlayer.getCards().size() < MAX_CARDS )
			System.out.println("trying to do a trade when less then 5 cards and tradeCap is on");
            return 0;
        }
        public int trade2(){
            if (cardMode==CARD_INCREASING_SET) {
                int armies = 0;
			cardState=armies;
		}
            return 0;
        }
	public int trade(Card card1, Card card2, Card card3) {
		trade1();

		int armies = getTradeAbsValue( card1.getName(), card2.getName(), card3.getName(), cardMode);

		if (armies <= 0) return 0;

		trade2();

		currentPlayer.tradeInCards(card1, card2, card3);

		//Return the cards to the deck
		List used = getUsedCards();
		used.add(card1);
		used.add(card2);
		used.add(card3);

		recycleUsedCards();

		currentPlayer.addArmies(armies);

		// if tradeCap you must trade to redude your cards to 4 or fewer cards
		// but once your hand is reduced to 4, 3 or 2 cards, you must stop trading
		if ( !canTrade() || (tradeCap && currentPlayer.getCards().size() < MAX_CARDS ) ) {
			gameState=STATE_PLACE_ARMIES;
			tradeCap=false;
		}

		return armies;
	}

	/**
	 * Returns the trading value of the given cards, without taking into account
	 * the territories associated to the cards.
	 * @param c1 The name of the type of the first card.
	 * @param c2 The name of the type of the second card.
	 * @param c3 The name of the type of the third card.
	 * @return 0 in case of invalid combination of cards.
	 */
        public int getTradeAbsValue1(){
            String c1 = null;
            String c3 = null;
            if (!c1.equals(Card.WILDCARD)) { String n4 = c3; c3 = c1; c1 = n4; }
            String c2 = null;
		if (!c2.equals(Card.WILDCARD)) { String n4 = c3; c3 = c2; c2 = n4; }
		if (!c1.equals(Card.WILDCARD)) { String n4 = c2; c2 = c1; c1 = n4; }
            return 0;
        }
        public int getTradeAbsValue2(){
            if (cardMode == CARD_INCREASING_SET) {
			if (
					c1.equals(Card.WILDCARD) ||
							(c1.equals(c2) && c1.equals(c3)) ||
							(!c1.equals(c2) && !c1.equals(c3) && !c2.equals(c3))
					) {
                            System.out.println("error");
                            
			}
		}
            return 0;
        }
        public int getTradeAbsValue3(){
            if ((c1.equals(c2) || c1.equals(Card.WILDCARD)) && c2.equals(c3)) {
                boolean size = c3.equals(Card.INFANTRY);
				while(size) {
                                    
					break;
				}
                    boolean size1 = c3.equals(Card.CAVALRY);            
				while (size1) {
                                  
					break;
				}
                                boolean size2 = c3.equals(Card.CANNON);
				while(size2) {
                                 
					break;
				}
                                boolean size3 = c1.equals( Card.WILDCARD );
				while (size3) { // (c1.equals( Card.WILDCARD ))
                                   
					break;// Incase someone puts 3 wildcards into his set
				}
			}
            return 0;
        }
        public int getTradeAbsValue4(){
            if (
					(c1.equals(Card.WILDCARD) && c2.equals(Card.WILDCARD)) ||
							(!c1.equals(c2) && !c2.equals(c3) && !c1.equals(c3))
					) {
                System.out.print("");
              
			}
            return 0;
        }
        public int getTradeAbsValue5(){
            if (c1.equals(c2) && c1.equals(c3)) {
				// All equal
                                boolean size = c1.equals(Card.CAVALRY);
				while (size) {
                                    
					break;
				}
                                boolean size1 = c1.equals(Card.INFANTRY);
				while(size1) {
                                  
					break;
				}
                                boolean size2 = c1.equals(Card.CANNON);
				while (size2) {
					
					break;
				}
                                boolean size3 = c1.equals( Card.WILDCARD );
				while (size3){ // (c1.equals( Card.WILDCARD ))
					 // Incase someone puts 3 wildcards into his set
					break;
				}
			}
            return 0;
        }
        public int getTradeAbsValue6(){
            if (!c1.equals(c2) && !c2.equals(c3) && !c1.equals(c3) && !c1.equals(Card.WILDCARD)) {
                            System.out.print("");
			}
			//All the same w/1 wildcard
			else if (c1.equals(Card.WILDCARD) && c2.equals(c3)) {
           System.out.print("");
			}
			//2 wildcards, or a wildcard and two different
			else {
                System.out.print("");
			}
            return 0;
        }
	public int getTradeAbsValue() {
		int armies=0;

		// we shift all wildcards to the front
		getTradeAbsValue1();

		getTradeAbsValue2();
		if (cardMode == CARD_FIXED_SET) {
                    getTradeAbsValue3();
			// ALL THE SAME or 'have 1 wildcard and 2 the same'
			
			// ALL CARDS ARE DIFFERENT (can have 1 wildcard) or 2 wildcards and a 3rd card
			getTradeAbsValue4();
		}
		else { // (cardMode==CARD_ITALIANLIKE_SET)
                    getTradeAbsValue5();
			getTradeAbsValue6();
			
		}
		return armies;
	}

	public boolean canTrade() {
		return getBestTrade(currentPlayer.getCards(), null) > 0;
	}

	/**
	 * Find the best (highest) trade
	 * Simple greedy search using the various valid combinations
	 * @param cards
	 * @return
	 */
        public int getBestTrade1(){
            for (Card card : cards) {
			List<Card> cardType = cardTypes.get(card.getName());
			if (cardType == null) {
				cardType = new ArrayList<Card>();
				cardTypes.put(card.getName(), cardType);
			}
			cardType.add(card);
		}
            return 0;
        }
        public int getBestTrade2(){
            Object bestResult = null;
            RiskGame carda = getCard(cardTypes, Card.CANNON);
			if (carda == null) {
				carda = getCard(cardTypes, Card.WILDCARD);
			}
            RiskGame cardb = getCard(cardTypes, Card.CAVALRY);
			if (cardb == null) {
				cardb = getCard(cardTypes, Card.WILDCARD);
			}
            RiskGame cardc = getCard(cardTypes, Card.INFANTRY);
			if (cardc == null) {
				cardc = getCard(cardTypes, Card.WILDCARD);
			}
            RiskGame bestValue = getTradeAbsValue( carda.getName(), cardb.getName(), cardc.getName(), getCardMode());
			if (bestValue > 0) {
                
				if (bestResult == null) {
					return 2;
				}
				
			}
            return 0;
        }
                public int getBestTrade3(){
            Object carda = null;
            Object cardb = null;
            Object cardc = null;
			if (entry.getKey().equals(Card.WILDCARD)) {
                int wildCardCount = 0;
				if (wildCardCount >= 3) {
					carda = wildCards.get(0);
					cardb = wildCards.get(1);
					cardc = wildCards.get(2);
				}
        }
            return 0;
        
        }
       public int getBestTrade4(){
            int val = getTradeAbsValue( carda.getName(), cardb.getName(), cardc.getName(), getCardMode());
            int bestValue = 0;
				if (val > bestValue) {
					bestValue = val;
                Object bestResult = null;
					if (bestResult == null) {
						return bestValue;
					}
					
				}
            return 0;
        }
       public int getBestTrade5(){
            int wildCardCount = 0;
            List<Card> cardList = entry.getValue();
             Object carda = null;
            Object cardb = null;
            Object cardc = null;
           if (cardList.size() + wildCardCount >= 3) {
					carda = cardList.get(0);
					cardb = cardList.size()>1?cardList.get(1):wildCards.get(0);
					cardc = cardList.size()>2?cardList.get(2):wildCards.get(2-cardList.size());
				}
            return 0;
       }
	public int getBestTrade(List<Card> cards, Card[] bestResult) {
		Map<String, List<Card>> cardTypes = new HashMap<String, List<Card>>();
                getBestTrade1();
		
		Card carda = null;
		
		int bestValue = 0;
		if (cardTypes.size() >= 3) {
		getBestTrade2();
		}
		
		
		for (Map.Entry<String, List<Card>> entry : cardTypes.entrySet()) {
                   
			if(carda == null){
                     getBestTrade3();
				
				getBestTrade5();
			}
                     
			if (carda != null) {
				 getBestTrade4();
			}
		}
		return bestValue;
	}

	private Card getCard(Map<String, List<Card>> cardTypes, String name) {
		List<Card> type = cardTypes.get(name);
		if (type != null) {
			return type.get(0);
		}
		return null;
	}

	public int getNewCardState() {

		if (cardState < 4) {
			return cardState+4;
		}
		else if (cardState < 12) {
			return cardState+2;
		}
		else if (cardState < 15) {
			return cardState+3;
		}
		else {
			return cardState+5;
		}

	}

	/**
	 * checks if a set of cards can be traded
	 * @param card1 First card to trade
	 * @param card2 Second card to trade
	 * @param card3 Third card to trade
	 * @return boolean true if they can be traded false if they can not
	 */
	public boolean checkTrade(Card card1, Card card2, Card card3) {
		return getTradeAbsValue( card1.getName(), card2.getName(), card3.getName(), cardMode) > 0;
	}

	/**
	 * Ends the trading phase by checking if the player has less than 5 cards
	 * @return boolean Returns true if the player has ended the trade phase, returns false if the player cannot end the trade phase
	 */
	public boolean endTrade() {
		if (canEndTrade()) {
			gameState=STATE_PLACE_ARMIES;
			if (tradeCap) {
				System.err.println("endTrade worked when tradeCap was true");
			}
			return true;
		}
		return false;
	}

	public boolean canEndTrade() {
		if (gameState==STATE_TRADE_CARDS) {
			//in italian rules there isn't a limit to the number of risk cards that you can hold in your hand.
			if (cardMode == CARD_ITALIANLIKE_SET || currentPlayer.getCards().size() < MAX_CARDS) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Places an army on the Country
	 * @param t Country that the player wants to add armies to
	 * @param n Number of armies the player wants to add to the country
	 * @return boolean Returns true if the number of armies are added the country, returns false if the armies cannot be added to the territory
	 */
        public int placeArmy1(){
            boolean size = NoEmptyCountries();
            while ( size ) { // no empty country are found
						t.addArmy();
						currentPlayer.loseExtraArmy(1);
               
						break;
						//System.out.print("army placed in: " + t.getName() + "\n"); // testing
					}
            return 0;
        }
        public int placeArmy2(){
            int n = 0;
            if (n != 1) return 0;
				// if it has the player as a owner
				if ( t.getOwner()==currentPlayer ) {

					placeArmy1();

				}
				// if there is no owner
				else if ( t.getOwner()==null ) {

					t.setOwner(currentPlayer);
					currentPlayer.newCountry(t);
					t.addArmy();
					currentPlayer.loseExtraArmy(1);
               
                //System.out.print("country taken and army placed in: " + t.getName() + "\n"); // testing
				}
            return 0;

        }
        public int placeArmy3(){
            boolean n = false;
            if ( t.getOwner()==currentPlayer && currentPlayer.getExtraArmies() >=n ) {

					currentPlayer.currentStatistic.addReinforcements(n);

					t.addArmies(n);
					currentPlayer.loseExtraArmy(n);
                //System.out.print("army placed in: " + t.getName() + "\n"); // testing
         

				}
            return 0;
        }
        public int placeArmy4(){
            if (getSetupDone() ) { // ie the initial setup has been compleated
                                        int size = currentPlayer.getExtraArmies();
					while ( size == 0 ) { gameState=STATE_ATTACKING;
						break;
					}
					while(!( size == 0 )) { gameState=STATE_PLACE_ARMIES;
						break;
					}
				}
				else { // initial setup is not compleated
                int size = currentPlayer.getExtraArmies();
					while (size==0) {
						setup++; // another player has finished initial setup
						break;
					}

					gameState=STATE_END_TURN;

				}

				if ( checkPlayerWon() ) {
                 System.out.print("");
				}
            return 0;
        }
	public int placeArmy(Country t, int n) {

		int done=0;

		if ( gameState==STATE_PLACE_ARMIES ) {

			if ( !getSetupDone() ) { // ie the initial setup has not been compleated
				placeArmy2();
			}
			else { // initial setup is completed
                                placeArmy3();
				// if it has the player as a owner
				
			}

			if (done==1) {
                                  placeArmy4();
				

			}

		}
		return done;

	}

	/**
	 * Automatically places an army on an unoccupied country
	 * @return int Returns the country id which an army was added to
	 */
        public int getRandomCountry2() {
            if ( NoEmptyCountries() ) {
				List countries = currentPlayer.getTerritoriesOwned();
				return ((Country)countries.get( r.nextInt(countries.size()) )).getColor();
			}
			else {
				// find a empty country
				int a = r.nextInt(Countries.length);
				
				for (int c=a; c < Countries.length ; c++) {
                                    getRandomCountry1();
					
				}
			}
            return 0;
            
        }
        public int getRandomCountry1() {
            
            int c = 0;
            boolean done = false;
            if ( Countries[c].getOwner() == null ) {
						return Countries[c].getColor();
					}
					else if ( c == Countries.length-1 && !done ) {
						c = -1;
						done = true;
					}
					else if ( c == Countries.length-1 && done ) {
						System.out.println("error");
					}
            return 0;
        }
	public int getRandomCountry() {
		if (gameState==STATE_PLACE_ARMIES) {
			getRandomCountry2();
		}
		throw new IllegalStateException();
	}

	/**
	 * Attacks one country and another
	 * @param t1 Attacking country
	 * @param t2 Defending country
	 * @return int[] Returns an array which determines if the player is allowed to roll dice
	 */
	public boolean attack(Country t1, Country t2) {

		boolean result=false;

		if (gameState==STATE_ATTACKING) {

			if (
					t1!=null &&
							t2!=null &&
							t1.getOwner()==currentPlayer &&
							t2.getOwner()!=currentPlayer &&
							t1.isNeighbours(t2) &&
							// t2.isNeighbours(t1) && // not needed as there is code to check this
							t1.getArmies() > 1
					) {

				currentPlayer.currentStatistic.addAttack();
				((Player)t2.getOwner()).currentStatistic.addAttacked();

				result=true;

				attacker=t1;
				defender=t2;
				battleRounds = 0;
				gameState=STATE_ROLLING;
				//System.out.print("Attacking "+t2.getName()+" ("+t2.getArmies()+") with "+t1.getName()+" ("+t1.getArmies()+").\n"); // testing
			}
		}
		return result;
	}

	/**
	 * Ends the attacking phase
	 * @return boolean Returns true if the player ended the attacking phase, returns false if the player cannot end the attacking phase
	 */
	public boolean endAttack() {

		if (gameState==STATE_ATTACKING) { // if we were in the attack phase

			// YURA:TODO check if there are any countries with more then 1 amy, maybe even check that a move can be made

			gameState=STATE_FORTIFYING; // go to move phase
			//System.out.print("Attack phase ended\n");
			return true;
		}
		return false;
	}

	/**
	 * Rolls the attackersdice
	 * @param dice1 Number of dice to be used by the attacker
	 * @return boolean Return if the roll was successful
	 */
	public boolean rollA(int dice1) {

		if (gameState==STATE_ROLLING) { // if we were in the attacking phase

			if ( attacker.getArmies() > 4 ) {
				if (dice1<=0 || dice1>3) return false;
			}
			else {
				if (dice1<=0 || dice1> (attacker.getArmies()-1) ) return false;
			}

			attackerDice = dice1; // 5 2 0

			// System.out.print("NUMBER OF DICE: " + dice1 + " or " + attackerDice.length + "\n");

			currentPlayer=defender.getOwner();
			gameState=STATE_DEFEND_YOURSELF;
			return true;

		}
		else return false;

	}

	public boolean rollD(int dice2) {

		if (gameState==STATE_DEFEND_YOURSELF) { // if we were in the defending phase

			if ( defender.getArmies() > maxDefendDice ) {
				if (dice2<=0 || dice2>maxDefendDice) return false;
			}
			else {
				if (dice2<=0 || dice2> (defender.getArmies()) ) return false;
			}

			currentPlayer=attacker.getOwner();

			defenderDice = dice2; // 4 3

			return true;

		}
		else return false;

	}

	public int getAttackerDice() {
		return attackerDice;
	}

	public int getDefenderDice() {
		return defenderDice;
	}

	/**
	 * Get the number of rolls that have taken place in the current attack
	 * @return
	 */
	public int getBattleRounds() {
		return battleRounds;
	}

	/**
	 * Rolls the defenders dice
	 * @param attackerResults The results for the attacker
	 * @param defenderResults The results for the defender
	 * @return int[] Returns an array which will determine the results of the attack
	 */
        public int[] battle4(){
            ((Player)attacker.getOwner()).currentStatistic.addCountriesWon();
				((Player)defender.getOwner()).currentStatistic.addCountriesLost();
            int[] result = null;

				result[5]=attacker.getArmies()-1;

				capturedCountry=true;

				Player lostPlayer=(Player)defender.getOwner();

				lostPlayer.lostCountry(defender);
				currentPlayer.newCountry(defender);

				defender.setOwner( (Player)attacker.getOwner() );
				result[3]=1;
				gameState=STATE_BATTLE_WON;
				mustmove=attackerResults.length;

				result[4]=mustmove;


				// if the player has been eliminated
				if ( lostPlayer.getNoTerritoriesOwned() == 0) {
                                               battle3();
				

				}

            return null;
            
        }
        public int[] battle3(){
            int[] result = null;
         	result[3]=2;
                                   Player lostPlayer=(Player)defender.getOwner();
					currentPlayer.addPlayersEliminated(lostPlayer);
                                        int size = lostPlayer.getCards().size();
					while (size > 0) {

						//System.out.print("Hes got a card .. i must take it!\n");
						currentPlayer.giveCard( lostPlayer.takeCard() );

					}

					// in italian rules there is no limit to the number of cards you can hold
					// if winning the other players cards gives you 6 or more cards you must immediately trade
					 size = currentPlayer.getCards().size();
                                        while ( cardMode!=CARD_ITALIANLIKE_SET && size > MAX_CARDS) {
						// gameState=STATE_BATTLE_WON;
						tradeCap=true;
						break;
					}
               return null;
        }
        public int[] battle2(){
            int[] attackerResults = null;
            int c = 0;
            int[] defenderResults = null;
            if (attackerResults[c] > defenderResults[c]) {
					defender.looseArmy();
					defender.getOwner().currentStatistic.addCasualty();
					attacker.getOwner().currentStatistic.addKill();
                int[] result = null;
					result[2]++;
				}
				else {
					attacker.looseArmy();
					attacker.getOwner().currentStatistic.addCasualty();
					defender.getOwner().currentStatistic.addKill();
                int[] result = null;
					result[1]++;
				}
            return null;
        }
        public int[] battle1(){
            int[] attackerResults = null;
            for (int aResult:attackerResults) {
				attacker.getOwner().currentStatistic.addDice(aResult);
			}
            int[] defenderResults = null;
			for (int aResult:defenderResults) {
				defender.getOwner().currentStatistic.addDice(aResult);
			}
            return null;
        }
	public int[] battle(int[] attackerResults, int[] defenderResults) {

		int[] result = new int[6];
		result[0]=0; // worked or not
		result[1]=0; // no of armies attacker lost
		result[2]=0; // no of armies defender lost
		result[3]=0; // did you win
		result[4]=0; // min move
		result[5]=0; // max move

		if (gameState==STATE_DEFEND_YOURSELF) { // if we were in the defending phase
			battleRounds++;

			battle1();

			// battle away!
                        int size = Math.min(attackerResults.length, defenderResults.length);
			for (int c=0; c< size ; c++) {

				battle2();

			}

			// if all the armies have been defeated
			if (defender.getArmies() == 0) {
                                 battle4();
				
			}
			else if (attacker.getArmies() == 1) {
				gameState=STATE_ATTACKING;
				//System.out.print("Retreating (FORCED)\n");
				currentPlayer.currentStatistic.addRetreat();
			}
			else { gameState=STATE_ROLLING; }

			defenderDice = 0;
			attackerDice = 0;
			result[0]=1;

		}

		return result;
	}

	/**
	 * Moves a number of armies from the attacking country to defending country
	 * @param noa Number of armies to be moved
	 * @return 1 or 2 if you can move the number of armies across (2 if you won the game), returns 0 if you cannot
	 */
	public int moveArmies(int noa) {

		if (gameState==STATE_BATTLE_WON && mustmove>0 && noa>= mustmove && noa<attacker.getArmies() ) {

			attacker.removeArmies(noa);
			defender.addArmies(noa);

			gameState=tradeCap?STATE_TRADE_CARDS:STATE_ATTACKING;

			attacker=null;
			defender=null;
			mustmove=0;

			return checkPlayerWon()?2:1;
		}
		return 0;
	}

	/**
	 * Moves all of armies from the attacking country to defending country
	 * @return int Return trues if you can move the number of armies across, returns false if you cannot
	 */
	public int moveAll() {

		if (gameState==STATE_BATTLE_WON && mustmove>0) {

			return attacker.getArmies() - 1;

		}
		return -1;

	}

	public int getMustMove() {
		return mustmove;
	}

	/**
	 * Retreats from attacking a country
	 * @return boolean Returns true if you can retreat, returns false if you cannot
	 */
	public boolean retreat() {

		if (gameState==STATE_ROLLING) { // if we were in the attacking phase

			currentPlayer.currentStatistic.addRetreat();

			gameState=STATE_ATTACKING; // go to attack phase
			//System.out.print("Retreating\n");

			attacker=null;
			defender=null;

			return true;
		}
		return false;
	}

	/**
	 * Moves armies from one country to an adjacent country and goes to the end phase
	 * @param t1 Country where the armies are moving from
	 * @param t2 Country where the armies are moving to
	 * @param noa Number of Armies to move
	 * @return boolean Returns true if the tactical move is allowed, returns false if the tactical move is not allowed
	 */
	public boolean moveArmy(Country t1, Country t2, int noa) {
		if (gameState==STATE_FORTIFYING) {

			// do they exist //check if they belong to the player //check if they are neighbours //check if there are enough troops in country1
			if (
					t1!=null &&
							t2!=null &&
							t1.getOwner()==currentPlayer &&
							t2.getOwner()==currentPlayer &&
							t1.isNeighbours(t2) &&
							// t2.isNeighbours(t1) && // not needed as there is code to check this
							t1.getArmies() > noa &&
							noa > 0
					) {

				t1.removeArmies(noa);
				t2.addArmies(noa);
				gameState=STATE_END_TURN;

				checkPlayerWon();

				//System.out.println("Armies Moved. "+gameState); // testing
				return true;

			}
		}
		return false;

	}

	/**
	 * Choosing not to use the tactical move and moves to the end phase
	 * @return boolean Returns true if you are in the right phase to use the tactical move and returns false otherwise
	 */
	public boolean noMove() {

		if (gameState==STATE_FORTIFYING) { // if we were in the move phase
			gameState=STATE_END_TURN; // go to end phase

			//System.out.print("No Move.\n"); // testing
			return true;
		}
		else return false;

	}

	public void workOutEndGoStats(Player p) {

		int countries = p.getNoTerritoriesOwned();
		int armies = p.getNoArmies();
		int continents = getNoContinentsOwned(p);
		int conectedEmpire = getConnectedEmpire(p).size();
		int cards = p.getCards().size();

		p.currentStatistic.endGoStatistics(countries, armies, continents, conectedEmpire, cards);

	}

	public List getConnectedEmpire(Player p) {

		Vector t = (Vector)p.getTerritoriesOwned().clone();

		Vector a = new Vector();
		Vector b = new Vector();

		while ( t.isEmpty() == false ) {

			Country country = ((Country)t.remove(0));

			a.add( country );

			getConnectedEmpire( t, a, country.getNeighbours() , p );

			if (a.size() > b.size() ) {
				b = a;
			}

			a = new Vector();

		}

		return b;

	}

	/**
	 * Finds the largest number of connected territories owned by a single player
	 * @param t Vector of territories owned by a single player (volatile)
	 * @param a Vector of adjacent territories
	 * @param n Vector of territories owned by a single player (non-volatile)
	 * @param p The current player
	 */
	public void getConnectedEmpire(List t, List a, List n, Player p) {
                    int size = n.size() ;
		for (int i = 0; i < size ; i++) {

			if ( ((Country)n.get(i)).getOwner() == p && t.contains( n.get(i) ) ) {

				Country country = (Country)n.get(i);
				t.remove( country );
				a.add( country );

				getConnectedEmpire( t, a, country.getNeighbours(), p);


			}
		}

	}

	/**
	 * Sets the capital for a player - ONLY FOR CAPITAL RISK
	 * @param c The capital country
	 * @return boolean Returns true if the country is set as the capital, returns false otherwise
	 */
	public boolean setCapital(Country c) {

		if (gameState== STATE_SELECT_CAPITAL && gameMode == 2 && c.getOwner()==currentPlayer && currentPlayer.getCapital()==null) {

			currentPlayer.setCapital(c);
                        int size = Cards.size();
			for (int b=0; b< size ; b++) {

				if ( c== ((Card)Cards.elementAt(b)).getCountry() ) {
					Cards.removeElementAt(b);
					//System.out.print("card removed because it is a capital\n");
				}

			}

			gameState=STATE_END_TURN;

			return true;

		}
		return false;

	}

	/**
	 * Check if a player has won the game
	 * @return boolean Returns true if the player has won the game, returns false otherwise
	 */
        
        public boolean checkPlayerWon1(){
           
            Object size = m.getContinent1();
            Object size1 = m.getContinent2();
            Object size2 = m.getContinent3();
            boolean size4 = checkPlayerOwnesContinentForMission(size,1);
            boolean size5 = checkPlayerOwnesContinentForMission(size1,2);
            boolean size6 =  checkPlayerOwnesContinentForMission(size2,3);       
            while (
					(size !=null) && // this means its a continent mission

							size4 &&
							size5 &&
							size6

					) {

                // yay you have won
            
				break;

			}
            return true;
        }
        public boolean checkPlayerWon2(){
             int size = currentPlayer.getNoTerritoriesOwned();
            int size1 = m.getNoofcountries();
            int size2 = m.getNoofarmies();
            Object size5 = m.getPlayer();
        int size4 = (Player)m.getPlayer();

            while (
					 size1 != 0 && size2 != 0 && // check if this card has a value for capture teretories
							( size5 == null || (size4 ==0 || (Player)size5 == currentPlayer ) &&
							size1 <= size // do you have that number of countries captured
					)) {

			
                               
				for (int c=0; c< size ; c++) {
                                checkPlayerWon6();
				break;
			}
            return false;
        }   return false;
}
        public boolean checkPlayerWon3(){
            	int capitalcount=0;

			if ( currentPlayer==((Country)currentPlayer.getCapital()).getOwner() ) {
                               int size = Players.size();
				for (int c=0; c< size ; c++) {
                                   boolean size3 = ((Vector)currentPlayer.getTerritoriesOwned()).contains((Country)((Player)Players.elementAt(c)).getCapital());
                                       
					while ( size3 ) {
						capitalcount++;
						break;
					}

				}

			}

			if ( capitalcount==Players.size() ) {
                       System.out.print("");
			}
            return false;
                        
        }
        public boolean checkPlayerWon4(){
            Mission m = currentPlayer.getMission();
            Vector size2 = (Vector)currentPlayer.getPlayersEliminated();
            Object size = m.getPlayer();
          int size3 = (Player)size;
            boolean size4 = (size2).contains( size );
			while(
					size !=null && // check is this is indeed a Elim Player card
							size != currentPlayer && // check if its not the current player u need to eliminate
							(size3 ==0 && // chack if that player has been eliminated
							size4) //check if it was you who eliminated them
					) {

                // yay you have won
              

				break;
			}
			checkPlayerWon2();
                        checkPlayerWon1();
                        return false;
        }
        public boolean checkPlayerWon5(){
            int won;
            for (int c=0; c< Continents.length ; c++) {

			if ( Continents[c].isOwned(currentPlayer) ) {
				won++;
			}

		}
		if (won == Continents.length ) {

             
                System.out.print("The Game Is Over, "+currentPlayer.getName()+" has won!\n");

		}
                return false;
        }
        
        public boolean checkPlayerWon6(){
                int size3 = ((Country)((Vector)currentPlayer.getTerritoriesOwned()).elementAt(c)).getArmies();
            boolean n;
            int size2;
					while ( size3 >= size2)
					{n++;
						break;
					}
            

				
				
            return false;
        
}
	public boolean checkPlayerWon() {

		boolean result=false;

		// check if the player has won
		
		checkPlayerWon5();

	// check if the player has won capital risk!
		if (getSetupDone() && gameMode==MODE_CAPITAL && currentPlayer.getCapital() !=null ) {

		checkPlayerWon3();

		}
		// check if the player has won mission risk!
		else if (getSetupDone() && gameMode==MODE_SECRET_MISSION ) {

			checkPlayerWon4();
			

		}

		if (result==true) {
			gameState=STATE_GAME_OVER;
		}

		return result;

	}

	private boolean checkPlayerOwnesContinentForMission(Continent c,int n) {

		if ( ANY_CONTINENT.equals(c) ) {

			return (getNoContinentsOwned(currentPlayer) >=n);

		}
		else if (c!=null) {

			return c.isOwned(currentPlayer);

		}
		else {

			return true;

		}

	}

	public boolean canContinue() {

		if (gameState==STATE_GAME_OVER && gameMode != MODE_DOMINATION && gameMode != 1) {

			int oldGameMode=gameMode;
			gameMode=MODE_DOMINATION;
			boolean playerWon = checkPlayerWon();
			gameMode=oldGameMode;

			return !playerWon; // we CAN continue if someone has NOT won

		}
		return false;

	}

	public boolean continuePlay() {

		if (canContinue()) {

			gameMode=MODE_DOMINATION;

			if (tradeCap==true) { gameState=STATE_TRADE_CARDS; }
			else if ( currentPlayer.getExtraArmies()==0 ) { gameState=STATE_ATTACKING; }
			else { gameState=STATE_PLACE_ARMIES; }

			return true;

		}
		return false;
	}

	public int getClosestCountry(int x, int y) {
		Country closestCountryCanvas = null;
		int closestDistance = Integer.MAX_VALUE;

		for (int index=0; index < Countries.length; index++) {
			int distance = Countries[index].getDistanceTo(x,y);
			if (distance < closestDistance) {
				// we have a country closer to the point (x,y)
				closestCountryCanvas = Countries[index];
				closestDistance = distance;
			}
		}
		return closestCountryCanvas.getColor();
	}

	/**
	 * Loads the map
	 * @throws Exception There was a error
	 */
	public void loadMap() throws Exception {
		loadMap(true, null);
	} 
        public void loadMap1(){
            MapTranslator.setMap( mapfile );

		

		Vector Countries;
		Vector Continents;
            boolean cleanLoad = false;
		if (cleanLoad) {
			Countries = new Vector();
			Continents = new Vector();
		}
		else {
			Countries = new Vector(Arrays.asList(this.Countries));
			Continents = new Vector(Arrays.asList(this.Continents));
		}
        }
         public void loadMap2(){
            String input = null;
             if (input.charAt(0)=='[' && input.charAt( input.length()-1 )==']') {
                 System.out.print("Something beggining with [ and ending with ] found\n"); // testing
               
				}
				else {
                  System.out.print("");
}
         }
         public void loadMap5(){
             if (mode.equals("continents")) {
					//System.out.print("Adding continents\n"); // testing

					String id=st.nextToken(); //System.out.print(name+"\n"); // testing

					// get translation
					String name = MapTranslator.getTranslatedMapName(id).replaceAll( "_", " ");

					int noa=Integer.parseInt( st.nextToken() ); //System.out.print(noa+"\n"); // testing
					int color=ColorUtil.getColor( st.nextToken() ); //System.out.print(color.toString()+"\n"); // testing

					while(color==0) {

						// there was no check for null b4 here, but now we need this for the map editor
						color = getRandomColor();
						break;

					}

					while( st.hasMoreTokens() ) { System.out.println("unknown item found in map file: "+ st.nextToken() ); }
                 boolean cleanLoad = false;

					while(cleanLoad) {
						Continent continent = new Continent(id, name, noa, color);
						Continents.add(continent);
						break;
					}

				}
         }
         public void loadMap3(){
             
         
            if(mode.equals("files")) {
               
					//System.out.print("Adding files\n"); // testing
                      loadMap5();
				
				if (mode.equals("countries")) {
					//System.out.print("Adding countries\n"); // testing

					int color = Integer.parseInt(st.nextToken());
					String id=st.nextToken(); //System.out.print(name+"\n"); // testing

					// get translation
					String name = MapTranslator.getTranslatedMapName(id).replaceAll( "_", " ");

					int continent = Integer.parseInt(st.nextToken());
					int x = Integer.parseInt(st.nextToken());
					int y = Integer.parseInt(st.nextToken());

					while( st.hasMoreTokens() ) { System.out.println("unknown item found in map file: " );
}
                     int countryCount = 0;
					while ( ++countryCount != color ) { System.out.println("unexpected number found in map file: "+color ); }

					Country country = null;
                     boolean cleanLoad = false;
					while (cleanLoad) {
						country = new Country();
						Countries.add(country);
						((Continent)Continents.elementAt( continent - 1 )).addTerritoriesContained(country);
						break;
					}
					while(!cleanLoad){
						country = (Country)Countries.get(color -1);
						break;
					}

					country.setColor(color);
					country.setContinent((Continent)Continents.elementAt( continent - 1 ));
					country.setIdString(id);
					country.setName(name);
					country.setX(x);
					country.setY(y);
				}
         }
         }
         public void loadMap4(){
            String mode = null;
             if (mode.equals("borders")) {
					//System.out.print("Adding borders\n"); // testing

					int country=Integer.parseInt( st.nextToken() ); //System.out.print(country+"\n"); // testing
					while (st.hasMoreElements()) {
						((Country)Countries.elementAt( country - 1 )).addNeighbour( ((Country)Countries.elementAt( Integer.parseInt(st.nextToken()) - 1 )) );
					}


				}
				else if (mode.equals("newsection")) {

					mode = input.substring(1, input.length()-1); // set mode to the name of the section
                                        boolean size = mode.equals("files"); 
					while (size) {
						//System.out.print("Section: files found\n"); // testing
						ImagePic=null;
						ImageMap=null;
						break;
					}
					while(!size) {
						System.out.println("unknown section found in map file: "+mode);
					}

				}
         }
	public void loadMap(boolean cleanLoad, BufferedReader bufferin) throws Exception {

		MapTranslator.setMap( mapfile );

	

		Vector Countries = null;
		Vector Continents = null;
		loadMap1();

		int mapVer = 1;
		//System.out.print("Starting Load Map...\n");
		
		if (bufferin == null) {
			bufferin=RiskUtil.readMap( RiskUtil.openMapStream(mapfile) );
		}

		String input = bufferin.readLine();
		

		while(input != null) {

			if (input.equals("") || input.charAt(0)==';') {
				// do nothing
				System.out.print("Nothing\n"); // testing
			}
			else {
				//System.out.print("Something found\n"); // testing
                               loadMap2();
				
                               loadMap3();
			       loadMap4();
				
				// we are not in any section
				if (input.startsWith("ver ")) {
					mapVer = Integer.parseInt(input.substring(4, input.length()));
				}
//				else if (input.equals("test")) {
//
//				}
//				else if (input.startsWith("name ")) {
//
//				}
				// we should NOT throw errors on unknown items, as new items may be added to new versions of the map format
				//else {
				//	throw new Exception("unknown item found in map file: "+input);
				//}
			}

			input = bufferin.readLine(); // get next line
		}
		bufferin.close();

		int gameVer = getVersion();
		if (gameVer > mapVer) {
			System.err.println(mapfile + " too old, ver " + mapVer + ". game saved with ver " + gameVer);
		}

		if (cleanLoad) {
			this.Countries = (Country[])Countries.toArray( new Country[Countries.size()] );
			this.Continents = (Continent[])Continents.toArray( new Continent[Continents.size()] );
		}
		//System.out.print("Map Loaded\n");
	}

	/**
	 * Sets the filename of the map file
	 * @param f The name of the new file
	 * @return boolean Return trues if missions are supported
	 * @throws Exception The file cannot be found
	 */
        public boolean setMapfile4(){
            Object mode = null;
            if (input.charAt(0)=='[' && input.charAt( input.length()-1 )==']') {
					mode="newsection";
				}
                                 setMapfile2();
				
                                if ("borders".equals(mode)) {

               System.out.print("");

				}
				else if ("newsection".equals(mode)) {

					mode = input.substring(1, input.length()-1); // set mode to the name of the section

				}
                                return false;
        }
        public boolean setMapfile3(){
            	int space = input.indexOf(' ');
                   boolean size = input.equals("test");
					while(size) {

						runmaptest = true;
						break;

					}
					//else if (input.startsWith("name ")) {
					//	mapName = input.substring(5,input.length());
					//}
					//else if (input.startsWith("ver ")) {
					//        ver = Integer.parseInt( input.substring(4,input.length()) );
					//}
					while (space >= 0) {
						String key = input.substring(0,space);
						String value = input.substring(space+1);

						properties.put(key, value);
						break;
					}
                                        return false;
        }
        public boolean setMapfile2(){
            Object mode = null;
            if ("files".equals(mode)) {

					if ( input.startsWith("pic ") ) { ImagePic = input.substring(4); }

					else if ( input.startsWith("prv ") ) { previewPic = input.substring(4); }

					else if ( input.startsWith("crd ") ) { 
  System.out.print("");
}

				}
            return false;
        }
        public boolean setMapfile1(){
            boolean yesmap = false;
            	if ( yesmap==false ) { System.err.println("error with map file"); }
            boolean yescards = false;
		if ( yescards==false ) {  System.err.println("cards file not specified in map file"); }
            return false;
        }
	public boolean setMapfile(String f) throws Exception {

		if (f.equals("default")) {
			f = defaultMap;
		}

		BufferedReader bufferin=RiskUtil.readMap( RiskUtil.openMapStream(f) );
/*
		File file;

		if (f.equals("default")) {
			file = new File("maps/" + defaultMap);
		}
		else {
			file = new File("maps/" + f);
		}

		FileReader filein = new FileReader(file);

		BufferedReader bufferin = new BufferedReader(filein);
*/

		runmaptest = false;
		previewPic = null;
		properties = new HashMap();

		String input = bufferin.readLine();
		String mode = null;

		boolean returnvalue = false;
		

		while(input != null) {
			if (input.equals("") || input.charAt(0)==';') {
                            System.out.println("..");

			}
			else {

				setMapfile4();
				if (mode == null) {

				setMapfile3();
					// else unknown section
				}
			}

			input = bufferin.readLine(); // get next line
		}
                  setMapfile1();
            
        
	

		mapfile = f;
		bufferin.close();

		return returnvalue;
	}

	/**
	 * we need to call this if we do not want to reload data from disk when we start a game
	 */
	public void setMemoryLoad() {

		mapfile = null;
		cardsfile = null;

		ImagePic = null;
		ImageMap = null;
	}

	public void setupNewMap() {

		Countries = new Country[0];
		Continents = new Continent[0];

		Cards = new Vector();
		usedCards = new Vector();
		Missions = new Vector();

		properties = new HashMap();

		runmaptest = false;
		previewPic=null;

		setMemoryLoad();

	}

	public void setCountries(Country[] a) {
		Countries = a;
	}
	public void setContinents(Continent[] a) {
		Continents = a;
	}

	/**
	 * Loads the cards
	 * @throws Exception There was a error
	 */
        public void loadCards9(){
           
					int s1 = Integer.parseInt(st.nextToken());
					Player p = null;

					loadCards3();

					int noc = Integer.parseInt(st.nextToken());
					int noa = Integer.parseInt(st.nextToken());

					String s4 = st.nextToken();
					String s5 = st.nextToken();
					String s6 = st.nextToken();

					String missioncode=s1+"-"+noc+"-"+noa+"-"+s4+"-"+s5+"-"+s6;
            boolean rawLoad;
					String description=rawLoad?null:MapTranslator.getTranslatedMissionName(missioncode);

					while (description==null) {
						description="";
						while (st.hasMoreElements()) {
							description = description +("".equals(description)?"":" ")+ st.nextToken();
						}
						break;
					}

					while (p!=null && !rawLoad) {

				loadCards4();
					}

				loadCards7();
        }
        public void loadCards8(){
            String mode;
          
            if (mode.equals("cards")) {
					//System.out.print("Adding cards\n"); // testing
                                       loadCards2();
					 //System.out.print(name+"\n"); // testing

				

					loadCards5();

				}
        }
         public void loadCards7(){
            boolean rawLoad;
            int s1;
             	if ( rawLoad || s1 <= Players.size() ) {

						//System.out.print(description+"\n"); // testing
						Mission mission = new Mission(p, noc, noa, c1, c2, c3, description);
						Missions.add(mission);
					}
					else {
						System.out.print("NOT adding this mission as it refures to an unused player\n"); // testing
					}
                
         }
        public void loadCards6(){
            String mode = input.substring(1, input.length()-1); // set mode to the name of the section
            switch (mode) {
            //System.out.print("Section: cards found\n"); // testing
                case "cards":
                    break;
            //System.out.print("Section: missions found\n"); // testing
                case "missions":
                    break;
                default:
                    System.err.println("unknown section found in cards file: "+mode);
                    break;
            }
        }
        public void loadCards5(){
                   if ( st.hasMoreTokens() ) { System.err.println("unknown item found in cards file: "+ st.nextToken() ); } 
                }
        public void loadCards4(){
                  		String name = p.getName();

						String color = "color."+ColorUtil.getStringForColor( p.getColor() );
						java.util.ResourceBundle trans = TranslationBundle.getBundle();
						try { // in Java 1.4 no if (trans.containsKey(color))
							name = trans.getString(color)+" "+name;
						}
						catch (Exception ex) {
                                                System.err.println("error");
                                                }
            String s1;

						String oldkey ="PLAYER"+s1;
						String newkey = "{"+oldkey+"}";
            String description;                int size = description.indexOf(newkey);
						while (size >= 0) {
							// DefaultCards_XX.properties uses this format
							description = RiskUtil.replaceAll(description, newkey, name );
							break;
						}
                                                int size1 = description.indexOf(oldkey);
						while (size1 >= 0) {
							// many maps still have this format for missions
							description = RiskUtil.replaceAll(description, oldkey, name );
							break;
						}
                                              
						while ((!(size1 >= 0))&& (!(size >= 0))){
							System.err.println("newkey: "+newkey+" and oldkey: "+oldkey+" not found in mission: "+description);
						}


						break;  
                }
        public void loadCards3(){
            int s1 = 0;
            int size = Players.size();
                    while (s1==0 || s1>size ) {
               
						break;
					}
					while (!(s1==0 || s1>size )) {
						p = (Player)Players.elementAt( s1-1 );
					}
                }
        public void loadCards2(){
                    	if (name.equals(Card.WILDCARD)) {
						Card card = new Card(name, null);
						Cards.add(card);
					}
					else if ( name.equals(Card.CAVALRY) || name.equals(Card.INFANTRY) || name.equals(Card.CANNON) ) {
						int country=Integer.parseInt( st.nextToken() );

						//System.out.print( Countries[ country - 1 ].getName() +"\n"); // testing
						Card card = new Card(name, Countries[ country - 1 ]);
						Cards.add(card);
					}
					else {
						System.err.println("unknown item found in cards file: "+name);
					}
                }
        public void loadCards1(){
            String input = null;
                    if (input.charAt(0)=='[' && input.charAt( input.length()-1 )==']') {
                        System.out.print("Something beggining with [ and ending with ] found\n"); // testing
                   
				}
				else {  System.out.print("");
}
                }
	public void loadCards(boolean rawLoad) throws Exception {

		

		Cards = new Vector();
		usedCards = new Vector();
		Missions = new Vector();

		//System.out.print("Starting load cards and missions...\n");

		BufferedReader bufferin=RiskUtil.readMap( RiskUtil.openMapStream(cardsfile) );

		String input = bufferin.readLine();
		String mode = "none";

		while(input != null) {

			if (input.equals("") || input.charAt(0)==';') {
				// do nothing
				System.out.print("Nothing\n"); // testing
			}
			else {

				//System.out.print("Something found\n"); // testing

				loadCards1();

				loadCards8();
				 if (mode.equals("missions")) {
					//System.out.print("Adding Mission\n"); // testing

					//boolean add=true;
                                    loadCards9();

				}
				else if (mode.equals("newsection")) {

		               loadCards6();

				}
				else {

					System.err.println("unknown item found in cards file: "+input);

				}

			}

			input = bufferin.readLine(); // get next line

		}
		bufferin.close();


		//System.out.print("Cards and missions loaded.\n");

	}

	


	/**
	 * Sets the filename of the cards file
	 * @param f The name of the new file
	 * @return boolean Return trues if missions are supported
	 * @throws Exception The file cannot be found
	 */
        private boolean setCardsfile2(){
            String input;
            String mode;
            if (mode.equals("newsection")) {

					mode = input.substring(1, input.length()-1); // set mode to the name of the section

					if (mode.equals("cards")) {

                                         System.out.print("");

					}
					else if (mode.equals("missions")) {

                                            System.out.print("");

					}
				}
            return false;
        }
        private boolean setCardsfile1(){
            String input;
            if (input.charAt(0)=='[' && input.charAt( input.length()-1 )==']') {
                System.out.print("");
				}
				else {  System.out.print("");
}
            return false;
        }
	private boolean setCardsfile(String f) throws Exception {


		


		if (f.equals("default")) {
			f = defaultCards;
		}

		BufferedReader bufferin=RiskUtil.readMap(RiskUtil.openMapStream(f) );


/*

		File file;

		if (f.equals("default")) {
			file = new File("maps/" + defaultCards);
		}
		else {
			file = new File("maps/" + f);
		}


		FileReader filein = new FileReader(file);

		BufferedReader bufferin = new BufferedReader(filein);


*/

		String input = bufferin.readLine();
		

		boolean yesmissions=false;
		boolean yescards=false;

		while(input != null) {

			if (input.equals("") || input.charAt(0)==';') {
                      System.out.print("..");
			}
			else {

				setCardsfile1();



				setCardsfile2();


			}


			input = bufferin.readLine(); // get next line

		}


		if ( yescards==false ) { throw new Exception("error with cards file"); }

		cardsfile = f;
		bufferin.close();

		MapTranslator.setCards( f );

		return yesmissions;

	}

	/**
	 * Shuffles the countries
	 */
	public List shuffleCountries() {

		// we create a COPY of the Countries array, so that we do not mess up the real one
		List oldCountries = new Vector( Arrays.asList( Countries ) );

		//Vector newCountries = new Vector();
		//while(oldCountries.size() > 0) {
		//	int a = r.nextInt(oldCountries.size()) ;
		//	newCountries.add ( oldCountries.remove(a) );
		//}
		//return newCountries;

		Collections.shuffle(oldCountries);
		return oldCountries;

	}

	/**
	 * Creates a new game
	 * @return RiskGame Returns the new game created

	public static RiskGame newGame() {
	RiskGame game = new RiskGame();
	//System.out.print("Game State: "+game.getState()+"\n"); // testing
	return game;
	}
	 */

	/**
	 * Loads a saved game
	 * @param file The saved game's filename
	 * @return Riskgame Return the saved game object if it loads, returns null if it doe not load
	 */
	public static RiskGame loadGame(String file) throws Exception {
		RiskGame game = null;
		//try {
		InputStream filein = RiskUtil.getLoadFileInputStream(file);
		ObjectInputStream objectin = new ObjectInputStream(filein);
		game = (RiskGame) objectin.readObject();
		objectin.close();

		//XMLDecoder d = new XMLDecoder( new BufferedInputStream( new FileInputStream(file)));
		//game = (RiskGame)d.readObject();
		//d.close();

		//}
		//catch (Exception e) {
		//System.out.println(e.getMessage());
		//}
		return game;
	}



	/**
	 * Closes the current game
	 * @return Riskgame Returns the game, which is already set to null
	 * /
	public static RiskGame closeGame() {
	RiskGame game = null;
	return game;
	}
	 */

	/**
	 * Saves the current game to a file
	 * @param file The filename of the save
	 * @return boolean Return trues if you saved, returns false if you cannot
	 */
	public void saveGame(OutputStream file) throws Exception { //added RiskGame parameter g, so remember to change in parser

		ObjectOutputStream out = new RiskObjectOutputStream(file);
		out.writeObject(this);
		//out.flush(); not needed if we do a close
		out.close();

		//XMLEncoder e = new XMLEncoder( new BufferedOutputStream( new FileOutputStream(file)));
		//e.writeObject(this);
		//e.close();
	}

	/**
	 * Gets the state of the game
	 * @return int Returns the game state
	 */
	public int getState() {
		return gameState;
	}

	/**
	 * Checks if there are any empty countries
	 * @return boolean Return trues if no empty countries, returns false otherwise
	 */
	public boolean NoEmptyCountries() {

		// find out if there are any empty countries

		Country empty=null;


		for (int c=0; c< Countries.length ; c++) {

			if ( Countries[c].getOwner() == null ) {
				empty = Countries[c];
				c=Countries.length;
			}

		}
		if (empty != null ) {
			return false;
		}
		else {
			return true;
		}

	}

	/**
	 * Checks if the set up is completely
	 * @return boolean Return trues if the set up is complete, returns false otherwise
	 */
	public boolean getSetupDone() {
		return setup == Players.size();
	}

	/**
	 * get the value od the trade-cap
	 * @return boolean Return trues if tradecap is true and false otherwise
	 */
	public boolean getTradeCap() {
		return tradeCap;
	}

	/**
	 * Gets the game mode
	 * @return int Return the game mode
	 */
	public int getGameMode() {
		return gameMode;
	}

	/**
	 * Gets the current player
	 * @return player Return the current player
	 */
	public Player getCurrentPlayer() {
		return currentPlayer;
	}

	/**
	 * Gets all the players
	 * @return Vector Return all the players
	 */
	public Vector getPlayers() {
		return Players;
	}

	/**
	 * Gets all the players
	 * @return Vector Return all the players
	 */
	public Vector getPlayersStats() {
            int size = Players.size() ;
		for (int c=0; c< size; c++) {
			workOutEndGoStats( (Player)Players.elementAt(c) );
		}

		return Players;
	}

	/**
	 * Gets the attacking country
	 * @return Country the attacking country
	 */
	public Country getAttacker() {
		return attacker;
	}

	/**
	 * Gets the defending country
	 * @return Country the defending country
	 */
	public Country getDefender() {
		return defender;
	}

	/**
	 * Gets the ImagePic
	 * @return URL ImagePic
	 */
	public String getImagePic() {
		return ImagePic;
	}

	public String getPreviewPic() {
		return previewPic;
	}
	public void setPreviewPic(String prv) {
		previewPic = prv;
	}

	public Map getProperties() {
		return properties;
	}

	int getIntProperty(String name, int defaultValue) {
		Object value = properties.get(name);
		if (value!=null) {
			return Integer.parseInt( String.valueOf(value) );
		}
		return defaultValue;
	}
	void setIntProperty(String name, int value, int defaultValue) {
		if (value == defaultValue) {
			properties.remove(name);
		}
		else {
			properties.put(name, String.valueOf(value));
		}
	}

	public int getCircleSize() {
		return getIntProperty("circle",20);
	}
	public void setCircleSize(int a) {
		setIntProperty("circle",a,20);
	}

	public int getVersion() {
		return getIntProperty("ver",1);
	}
	public void setVersion(int newVersion) {
		setIntProperty("ver",newVersion,1);
	}

	/**
	 * can return the name or null
	 */
	public String getMapName() {
		return (String) properties.get("name");
	}
	public void setMapName(String name) {
		if (name==null) {
			properties.remove("name");
		}
		else {
			properties.put("name", name);
		}
	}

	/**
	 * Gets the ImageMap
	 * @return URL ImageMap
	 */
	public String getImageMap() {
		return ImageMap;
	}

	public String getCardsFile() {
		return cardsfile; //.getFile().substring( cardsfile.getFile().lastIndexOf("/")+1 );
	}

	public String getMapFile() {
		return mapfile; //.getFile().substring( mapfile.getFile().lastIndexOf("/")+1 );
	}

	public Vector getCards() {
		return Cards;
	}
	public Vector getUsedCards() {
		return usedCards;
	}

	/**
	 * Rolls a certain number of dice
	 * @param nod Number of dice you want to roll
	 * @return int[] Returns an array which was the results of the roll, ordered from highest to lowest
	 */
	public int[] rollDice(int nod) {

		int[] dice = new int[nod];

		for (int j=0; j<nod; j++) {
			dice[j]=r.nextInt( 6 );
		}

		// NOW SORT THEM, biggest at the beggining
		for (int i=0; i<nod-1; i++) {
			int temp, pos=i;

			for(int j=i+1; j<nod; j++)
				if(dice[j]>dice[pos])
					pos=j;
			temp = dice[i];
			dice[i] = dice[pos];
			dice[pos] = temp;
		}

/*
System.out.print("After sorting, the dice are:\n");

String str="[";
if(dice.length>0) {
str+=(dice[0]+1);
for(int i=1; i<dice.length; i++)
str+="|"+(dice[i]+1);
}
System.out.print(str+"]\n");
*/
		return dice;

	}

	/**
	 * Gets the number of continents which are owned by a player
	 * @param p The player you want to find continents for
	 * @return int Return the number of continents a player owns
	 */
	public int getNoContinentsOwned(Player p) {

		int total=0;

		for (int c=0; c< Continents.length ; c++) {

			if ( Continents[c].isOwned(p) ) {
				total++;
			}

		}
		return total;
	}

	/**
	 * Gets a country
	 * @param name The name of the country
	 * @return Country Return the country you are looking for, if it exists. Otherwise returns null
	 *
	// * @deprecated

	public Country getCountry(String name) {

	for (int c=0; c< Countries.length ; c++) {

	if ( name.equals(Countries.[c].getName()) ) {
	return Countries[c];
	}

	}
	System.out.println( "ERROR: Country not found: " + name );
	return null;

	}
	 */


	/**
	 * Tries to find a country by its name.
	 * This function should only be used if a user has entered the name manually!
	 *
	 * @param name The name of the country
	 * @return Country Return the country you are looking for, if it exists. Otherwise returns null

	public Country getCountryByName(String name) {

	for (int c=0; c< Countries.length ; c++) {

	if ( name.equals(Countries[c].getName()) ) {
	return Countries[c];
	}

	}
	System.out.println( "ERROR: Country not found: " + name );
	return null;

	}//public Country getCountryByName(String name)
	 */


	/**
	 * returns the country with the given color (ID)
	 */
	public Country getCountryInt(int color) {

		if (color <= 0 || color > Countries.length ) { return null; }
		else return Countries[color-1];

	}



	/**
	 * returns the country with the given color (ID)
	 * the string is converted to an int value

	 public Country getCountryInt(String strId)
	 {
	 int nId = -1;
	 try {
	 nId = Integer.parseInt( strId);
	 } catch( NumberFormatException e) {
	 System.out.println( "ERROR: Can't convert number \"" + strId + "\" to a number." );
	 return null;
	 }

	 return getCountryInt(nId);
	 }//public Country getCountryInt(String nId)
	 */


	/**
	 * Gets a cards
	 * @return Card Return the card you are looking for, if it exists. Otherwise returns null
	 */
        public Card[] getCards1(){
            int a;
            String name;
            switch (a) {
                case 0:
            {
                String name1;
                name = name1;
            }
                    break;
                case 1:
            {
                String name2;
                name = name2;
            }
                    break;
                default:
            {
                String name3;
                name = name3;
            }
                    break;
            }
        }
	public Card[] getCards(String name1,String name2,String name3) {

		Card[] c = new Card[3];

		Vector playersCards = new Vector( currentPlayer.getCards() );

		for (int a=0;a<3;a++) {

			String name;

			getCards1(); // if (a==2)
                          int size = playersCards.size();
			for (int b=0; b< size; b++) {

				if (name.equals(Card.WILDCARD) && name.equals( ((Card)playersCards.elementAt(b)).getName() ) ) {
					c[a] = (Card) playersCards.remove(b);
					continue;
				}
				else if ( (Country)((Card)playersCards.elementAt(b)).getCountry() != null && name.equals( ((Country)((Card)playersCards.elementAt(b)).getCountry()).getColor()+"" ) ) {
					c[a] = (Card) playersCards.remove(b);
					continue;
				}

			}

		}

		return c;

	}

	public Card findCardAndRemoveIt(String name) {

		int cardIndex = -1;
                int size = Cards.size() ; 
		for (int c=0; c< size ; c++) {
			Card theCard = ((Card)Cards.elementAt(c));

			// if we are looking for a wildcard, and this card is also a wildcard
			if (name.equals(Card.WILDCARD) && name.equals( theCard.getName() ) ) {
				cardIndex = c;
				break;
			}
			// if we are not looking for a wildcard and the card matches the country
			else if (theCard.getCountry() != null && name.equals( String.valueOf( theCard.getCountry().getColor() ) ) ) {
				cardIndex = c;
				break;
			}

		}

		// find the card and remove it
		Card theCard = (Card)Cards.remove(cardIndex);
		Cards.trimToSize(); // not sure if this is needed

		recycleUsedCards();

		return theCard;

	}

	/**
	 * This method should be called after:
	 * <ul>
	 * <li>a card was removed from normal cards
	 * <li>a card was added to the used cards
	 * </ul>
	 */
	private boolean recycleUsedCards() {
		// if we have removed the last card, and we want to reuse our cards, then we add all the used ones into the current cards vector
		Vector used = getUsedCards();
		if (Cards.isEmpty() && recycleCards && !used.isEmpty()) {
			Cards.addAll(used);
			used.clear();
			return true;
		}
		return false;
	}

	/**
	 * Gets a cards
	 * @param s The number you want to parse
	 * @return int The number you wanted
	 * @throws NumberFormatException You cannot parse the string
	 */
	public static int getNumber(String s) {
		try {
			return Integer.parseInt(s);
		}
		catch (NumberFormatException e) {
			return -1;
		}
	}

	/**
	 * Gets the number of players in the game
	 * @return int Return the number of number of players
	 */
	public int getNoPlayers() {
		return Players.size();
	}

	/**
	 * Gets the countries in the game
	 * @return Vector Return the Countries in the current game
	 */
	public Country[] getCountries() {

		return Countries;
	}

	/**
	 * Gets the continents in the game
	 * @return Vector Return the Continents in the current game
	 */
	public Continent[] getContinents() {
		return Continents;
	}

	/**
	 * Gets the number of countries in the game
	 * @return int Return the number of countries in the current game
	 */
	public int getNoCountries() {
		return Countries.length;
	}

	public int getNoContinents() {

		return Continents.length;

	}

	/**
	 * Gets the allocated Missions in the game
	 * @return Vector Return the Missions in the current game
	 */
	public Vector getMissions() {
		return Missions;
	}

	/**
	 * Gets the number of Missions in the game
	 * @return int Return the number of Missions in the game
	 */
	public int getNoMissions() {
		return Missions.size();
	}

	public int getNoCards() {
		return Cards.size();
	}

	public boolean isRecycleCards() {
		return recycleCards;
	}

	/**
	 * Set the Default Map and Cards File
	 */
	public static void setDefaultMapAndCards(String a,String b) {

		defaultMap=a;
		defaultCards=b;

		// not needed as is reset each time a new RiskGame object is created
		//net.yura.domination.engine.translation.MapTranslator.setMap( a );
		//net.yura.domination.engine.translation.MapTranslator.setCards( b );

	}


	public static String getDefaultMap() {
		return defaultMap;
	}
	public static String getDefaultCards() {
		return defaultCards;
	}

	/**
	 * @return the current Card Mode
	 */
	public int getCardMode() {
		return cardMode;
	}

	public static int getRandomColor() {

		return HSBtoRGB( (float)Math.random(), 0.5F, 1.0F );

	}

	/**
	 * copy and paste from
	 * @see java.awt.Color#HSBtoRGB(float, float, float)
	 */
	public static int HSBtoRGB(float hue, int saturation, float brightness) {
		int r = 0, g = 0, b = 0;
		if (saturation == 0) {
			r = g = b = (int) (brightness * 255.0f + 0.5f);
		} else {
			
			
			System.out.print("");
			
		}
		return 0xff000000 | (r << 16) | (g << 8) | (b << 0);
	}

	public int getNoAttackDice() {
		if ( attacker.getArmies() > 4 ) { return 3; }
		else { return attacker.getArmies()-1; }
	}
	public int getNoDefendDice() {
		if ( defender.getArmies() > maxDefendDice ) { return maxDefendDice; }
		else { return defender.getArmies(); }
	}

	public void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
		in.defaultReadObject();
		this.r = new Random();
		if (this.mapfile != null && gameState!=STATE_NEW_GAME) {
			try {
				loadMap(false, null);
			}
			catch (Exception e1) {
                             
				// stupid fix for android 1.6
				System.err.println("Avoid sensitive informat");
				
			}
		}
	}

	void setCardMode(int cardMode) {
		this.cardMode = cardMode;
	}

	public Player getPlayer(String name) {
		for (Player player: (List<Player>)Players) {
			if (player.getName().equals(name)) {
				return player;
			}
		}
		return null;
	}

}
