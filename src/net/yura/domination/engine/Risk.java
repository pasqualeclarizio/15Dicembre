// Yura Mamyrin, Group D

package net.yura.domination.engine;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.Observable;
import java.util.Observer;
import java.util.Properties;
import java.util.Random;
import java.util.ResourceBundle;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.yura.domination.engine.ai.AIManager;
import net.yura.domination.engine.core.Card;
import net.yura.domination.engine.core.Country;
import net.yura.domination.engine.core.Mission;
import net.yura.domination.engine.core.Player;
import net.yura.domination.engine.core.RiskGame;
import net.yura.domination.engine.p2pclient.ChatClient;
import net.yura.domination.engine.p2pserver.ChatArea;
import net.yura.domination.engine.translation.TranslationBundle;

/**
 * <p> Main Risk Class </p>
 * @author Yura Mamyrin
 */
public class Risk extends Thread {

    private static final int DEFAULT_SHOW_DICE_SLEEP = 1000;
    private static final int DEFAULT_ROLL_DICE_SLEEP = 500;

    private static int SHOW_DICE_SLEEP = DEFAULT_SHOW_DICE_SLEEP;
    private static int ROLL_DICE_SLEEP = DEFAULT_ROLL_DICE_SLEEP;

    private static final Logger logger = Logger.getLogger(Risk.class.getName());

    protected RiskController controller;
    protected RiskGame game;

    OnlineRisk onlinePlayClient;
    private ChatArea p2pServer;

    private int port;

    protected String myAddress;

    // crashes on a mac too much
    //private SealedObject Undo;
    private ByteArrayOutputStream Undo = new ByteArrayOutputStream();

    protected boolean unlimitedLocalMode;
    private boolean autoplaceall;
    private boolean battle;
    private boolean replay;

    protected final List inbox;

    protected ResourceBundle resb;
    protected Properties riskconfig;


    public Risk(String b,String c) {
        this();

        RiskGame.setDefaultMapAndCards(b,c);
    }
    /**
     * these are a three types of Level Array's String
     */
    public static final String[] types = new String[] { "human","ai easy","ai easy","ai easy","ai average","ai average" };
    /**
     * Array of String about player
     */
    public static final String[] names = new String[] { "player","bob","fred","ted","yura","lala"};
    /**
     * Array of String about colors
     */
    public static final String[] colors = new String[] { "cyan","green","magenta","red","blue","yellow"};

    public Risk() {
        // default Android value does not work
        // 10,000 gives StackOverflowError on android on default map
        // 100,000 gives StackOverflowError on android on the map "The Keep"
        // 1,000,000 gives StackOverflowError on android on the map "The Keep" if you place all the troops
        // 10,000,000 very rarly gives crash
        // 100,000,000 crashes on "Castle in the Sky" map on Android (CURRENT VALUE)
        // 1,000,000,000 still crashes on "Castle in the Sky" (also crashes 32bit java SE)
        // 10,000,000,000 still crashes on "Castle in the Sky" (also crashes 32bit java SE)
        // 100,000,000,000 still crashes on "Castle in the Sky" (also crashes 32bit java SE)
        // 1,000,000,000,000 crashes the whole Android JVM, FUCK FUCK FUCK
        super(null,null,"DOMINATION-GAME-THREAD", 100000000 );

        resb = TranslationBundle.getBundle();

        try {
            String newName = System.getProperty("user.name");

            if (newName==null || "".equals(newName.trim())) {
                throw new Exception("bad user name");
            }
            else {
                for (int c=0;c<names.length;c++) {
                    if (names[c].equals(newName)) {
                        throw new Exception("name already in use");
                    }
                }
            }
            names[0] = newName;
        }
        catch(Throwable th) {
            System.out.println("error");
        }

        riskconfig = new Properties();

        riskconfig.setProperty("default.port","4444");
        riskconfig.setProperty("default.host","localhost");
        riskconfig.setProperty("default.map", RiskGame.getDefaultMap() );
        riskconfig.setProperty("default.cards", RiskGame.getDefaultCards() );
        riskconfig.setProperty("default.autoplaceall","false");
        riskconfig.setProperty("default.recyclecards","true");
        riskconfig.setProperty("ai.wait", String.valueOf(AIManager.getWait()) );
        int lenght = names.length;
        for (int c=0;c<lenght;c++) {
            riskconfig.setProperty("default.player"+(c+1)+".type",types[c]);
            riskconfig.setProperty("default.player"+(c+1)+".color",colors[c]);
            riskconfig.setProperty("default.player"+(c+1)+".name", names[c] );
        }

        try {
            riskconfig.load( RiskUtil.openStream("game.ini") );
        }
        catch (Exception ex) {
            System.out.println("error");
        }

        AIManager.setWait( Integer.parseInt( riskconfig.getProperty("ai.wait") ) );

        myAddress = createRandomUniqueAddress();

        RiskGame.setDefaultMapAndCards( riskconfig.getProperty("default.map") , riskconfig.getProperty("default.cards") );
        port = Integer.parseInt( riskconfig.getProperty("default.port") );

        battle = false;
        replay = false;

        controller = new RiskController();

        inbox = new java.util.Vector();
        this.start();

    }

    static String createRandomUniqueAddress() {

        String randomString = "#"+String.valueOf( Math.round(Math.random()*Long.MAX_VALUE) );

        try {
            //if (RiskUtil.checkForNoSandbox()) {
            try {
                return InetAddress.getLocalHost().getHostName() + randomString;
            }
            //else {
            catch(Throwable th) {
                return "sandbox" + randomString;
            }
        }
        catch (Exception e) { // if network has not been setup
            return "nonet" + randomString;
        }
    }

    public String getRiskConfig(String a) {
        return riskconfig.getProperty(a);
    }

    private class GameCommand implements Runnable {
        /**
         * UI_COMMAND
         */
        public static final int UI_COMMAND = 1;
        /**
         * NETWORK_COMMAND
         */
        public static final int NETWORK_COMMAND = 2;
        /**
         * TYPE
         */
        final int type;
        /**
         * COMMAND
         */
        final String command;
        /**
         * NOTIFIER
         */
        Object notifier;

        public GameCommand(int t,String c) {
            type = t;
            command = c;
        }

        public void run() {
            if (type == GameCommand.UI_COMMAND) {
                processFromUI(command);
            }
            else if (type == GameCommand.NETWORK_COMMAND) {
                inGameParser(command);
            }
        }

        public String toString() {
            return (type == UI_COMMAND ? "UI" : "NETWORK") + " " + command;
        }
    }

    /**
     * This parses the string, calls the relavant method and displays the correct error messages
     * @param m The string needed for parsing
     */
    public void parser(String m) {
        addToInbox( new GameCommand(GameCommand.UI_COMMAND, m ) );
    }

    public void parserFromNetwork(String m) {
        addToInbox( new GameCommand(GameCommand.NETWORK_COMMAND, m ) );
    }
    private void addToInbox(Runnable m) {
        synchronized(inbox) {
            inbox.add(m);
            inbox.notify();
        }
    }

    boolean running = true;
    public void run() {
        Runnable message = null;
        try {
            while (running) {
                synchronized (inbox) {
                    boolean emp = inbox.isEmpty();
                    try {
                        while (emp) {
                            if (!running) return;
                            inbox.wait();
                        }
                    } catch (InterruptedException e) {
                        // TODO fix this, this is totally wrong
                        System.err.println("InterruptedException in " + getName());
                    }
                    message = (Runnable) inbox.remove(0);
                }
                message.run();
            }
        } catch (Exception ex) {
            logger.log(Level.WARNING, "ERROR processing " + message, ex);
        }
    }
    private void processFromUI(String message) {
        String output = null;
        processFromUI5(message);
        processFromUI8(message);
        // take no action
        processFromUI9(message);
        // out of game commands
        if (game==null) { // if no game
            noGameParser(message);
        }
        // IN GAME COMMANDS
        else {
            StringTokenizer StringT = new StringTokenizer(message);
            String input=StringT.nextToken();
            // CLOSE GAME
            processFromUI10(input,output,StringT);
            // SAVE GAME
            processFromUI16(input,output,StringT);
            // REPLAY A GAME FROM THE GAME FILE
            try {
                processFromUI15(input, output, StringT);
            }catch (Exception e) {
                System.out.println("error");
            }
            processFromUI13(output,message);
        }

    }
    private void processFromUI16(String input,String output,StringTokenizer StringT) {
        boolean eq = input.equals("savegame");
        while(eq) {
            processFromUI12(output,StringT);
            processFromUI14(output,StringT);
            break;
        }
    }
    private void processFromUI15(String input, String output, StringTokenizer StringT) throws Exception {
        boolean eq = input.equals("replay");
        while(eq) {
            processFromUI4(output,StringT);
            break;
        }
    }
    private void processFromUI14(String output,StringTokenizer StringT) {
        int s = StringT.countTokens();
        while (!(s >= 1))
        { output=RiskUtil.replaceAll(resb.getString( "core.error.syntax"), "{0}", "savegame filename");
            break;
        }
    }
    private void processFromUI13(String output, String message) {
        if ( onlinePlayClient == null ) {
            inGameParser( myAddress+" "+message );
            output=null;

        }
        else {
            // send to network
            onlinePlayClient.sendUserCommand( message );
            output=null;

        }
        processFromUI2(output,message);
    }
    private void processFromUI12(String output,StringTokenizer StringT) {
        int s = StringT.countTokens();
        while (s >= 1) {
            processFromUI11(output,StringT);
            break;
        }
    }
    private void processFromUI11(String output,StringTokenizer StringT) {
        if ( unlimitedLocalMode ) {
            String filename = RiskUtil.getAtLeastOne(StringT);
            try {
                RiskUtil.saveFile(filename,game);
                output=resb.getString( "core.save.saved");
            }
            catch (Exception ex) {
                logger.log(Level.WARNING, "error saving game to file: "+filename,ex);

                output=resb.getString( "core.save.error.unable")+" "+ex;
                showMessageDialog(output);
            }
        }
        else {
            output = resb.getString( "core.save.error.unable" );
        }
    }
    private void processFromUI10(String input,String output, StringTokenizer StringT) {
        boolean eq = input.equals("closegame");
        while (eq) {
            processFromUI6(output, StringT);
            processFromUI7(output,StringT);
            break;
        }
    }
    private void processFromUI9(String message) {
        if (message.startsWith("rem ")) {
            controller.sendMessage(">" + message, false, false );
            getInput();
        }
    }
    private void processFromUI8(String message) {
        // Show version
        if (message.equals("ver")) {
            controller.sendMessage(">" + message, false, false );
            controller.sendMessage(RiskUtil.GAME_NAME+" Game Engine [Version " + RiskUtil.RISK_VERSION + "]", false, false );
            getInput();
        }
    }
    private void processFromUI7(String output, StringTokenizer StringT) {
        boolean b = !StringT.hasMoreTokens();
        while (b) {
            output=RiskUtil.replaceAll( resb.getString( "core.error.syntax"), "{0}", "closegame");
            break;
        }
    }
    private void processFromUI6(String output, StringTokenizer StringT) {
        boolean b = !StringT.hasMoreTokens();
        while (b) {
            closeGame();
            output=resb.getString("core.close.closed");
            break;
        }
    }
    private void processFromUI5(String message) {
        if ( message.trim().length()==0 ) {
            controller.sendMessage(">", false, false );
            getInput();
        }
    }
    private void processFromUI3(String output) throws Exception {
        if ( unlimitedLocalMode ) {
            saveGameToUndoObject();
            game = new RiskGame();
            replay = true;
            replay = false;
            output="replay of game finished";
        }
        else {
            output="can only replay local games";
        }
    }
    private void processFromUI4(String output, StringTokenizer StringT) throws Exception {
        boolean b = StringT.hasMoreTokens();
        while(b ==false ) {
            processFromUI3(output);
            break;
        }
    }
    private void processFromUI2(String output, String message) {
        while (output!=null) {
            controller.sendMessage("game>" + message, false, false);
            controller.sendMessage(output, false, false);
            getInput();
        }
    }
    private void noGameParser2(StringTokenizer StringT, String output) {
        boolean b = StringT.hasMoreTokens();
        if (b==false) {
            try {
                game = new RiskGame();
                unlimitedLocalMode = true;
                controller.newGame(true);
                setupPreviews( doesMapHaveMission() );
                output=resb.getString( "core.newgame.created");
            }
            catch (Exception e) {
                //RiskUtil.printStackTrace(e);
                output=resb.getString( "core.newgame.error") + " " + e.toString();
            }
        }
        else { output=RiskUtil.replaceAll(resb.getString( "core.error.syntax"), "{0}", "newgame"); }
    }
    private void noGameParser3() throws Exception {
        while (game == null) {
            throw new Exception("no game");
        }
    }
    private void noGameParser4(Player player, String output) {
        while ( player != null ) {
            // the game is saved
            saveGameToUndoObject();
            output=output+ System.getProperty("line.separator") + resb.getString( "core.loadgame.currentplayer") + " " + player.getName();
            break;
        }
    }
    private void noGameParser5() {
        int i = game.getState();
        while (i==RiskGame.STATE_NEW_GAME) {
            controller.newGame(true);
            setupPreviews( doesMapHaveMission() );
            break;
        }
    }
    private void noGameParser6() {
        int value = game.getState();
        while (!(value ==RiskGame.STATE_NEW_GAME) ){
            controller.startGame(unlimitedLocalMode);
            break;
        }
    }
    private void noGameParser13(String input,String output,StringTokenizer StringT) {
        if (input.equals("newgame")) {
            noGameParser2(StringT,output);
        }
    }
    private void noGameParser14(StringTokenizer StringT, String output) {
        if (StringT.countTokens() >= 1) {
            // this is not needed here as u can only get into this bit of code if game == null
            //if (game == null) {
            String filename = RiskUtil.getAtLeastOne(StringT);

            try {
                game = RiskGame.loadGame(filename);
                unlimitedLocalMode = true;
                output = resb.getString("core.loadgame.loaded");
                noGameParser3();
                Player player = game.getCurrentPlayer();
                noGameParser4(player, output);
                noGameParser5();
                noGameParser6();
            } catch (Exception ex) {
                logger.log(Level.WARNING, "error loading game from file: " + filename, ex);

                output = resb.getString("core.loadgame.error.load") + " " + ex;
                showMessageDialog(output);
            }
        }
        else { output=RiskUtil.replaceAll(resb.getString( "core.error.syntax"), "{0}", "loadgame filename"); }
    }
    private void noGameParser16(String input,StringTokenizer StringT, String output) {
        // LOAD GAME
        if (input.equals("loadgame")) {
            noGameParser14(StringT,output);
        }
    }
    private void noGameParser17(StringTokenizer StringT, String output) throws Exception{
        if (StringT.countTokens() == 1) {
            try {
                onlinePlayClient = new ChatClient( this, myAddress, StringT.nextToken(), port );
                // CREATE A GAME
                game = new RiskGame();
                unlimitedLocalMode = false;
                controller.newGame(false);
                setupPreviews( doesMapHaveMission() );
                output=resb.getString( "core.join.created");

            }
            catch (UnknownHostException e) {
                game = null;
                output=resb.getString( "core.join.error.unknownhost");
            }
            catch (ConnectException e) {
                game = null;
                output=resb.getString( "core.join.error.connect");
            }
            if (game==null) {
                showMessageDialog(output);
            }
        }
        else { output=RiskUtil.replaceAll(resb.getString( "core.error.syntax"), "{0}", "join server"); }
    }
    private void noGameParser19(String input,StringTokenizer StringT,String output) throws Exception{
        if (input.equals("join")) {
            noGameParser17(StringT,output);
        }
        // NEW SERVER
        if (input.equals("startserver")) {
            noGameParser12(StringT, output);
        }
    }
    private void noGameParser20(String input,StringTokenizer StringT,String output) {
        if (input.equals("killserver")) {
            noGameParser10(StringT,output);
        }
        else { // if there is no game and the command was unknown
            output=resb.getString( "core.loadgame.nogame");
        }
    }
    private void noGameParser(String message) {

        StringTokenizer StringT = new StringTokenizer( message );

        String input = StringT.nextToken();
        String output = null;

        controller.sendMessage(">" + message, false, false );
        noGameParser13(input,output,StringT);
        noGameParser16(input,StringT,output);
        noGameParser19(input,StringT,output);
        // KILL SERVER
        noGameParser20(input,StringT,output);
        controller.sendMessage(output, false, true );
        getInput();
    }
    private void noGameParser12(StringTokenizer StringT,String output) {
        if (StringT.hasMoreTokens()==false) {
            noGameParser11(output);
        }
        else { output=RiskUtil.replaceAll(resb.getString( "core.error.syntax"), "{0}", "startserver"); }
    }
    private void noGameParser11(String output) {
        if ( p2pServer == null ) {
            // CREATE A SERVER
            try {
                p2pServer = new ChatArea(controller,port);
                output=resb.getString( "core.startserver.started");
                controller.serverState(true);
            }
            catch(Exception e) {
                p2pServer = null;
                output=resb.getString( "core.startserver.error")+" "+e;
                showMessageDialog(output);
            }
        }
        else {
            output=resb.getString( "core.startserver.error");
        }
    }
    private void noGameParser7() throws IOException {
        if (p2pServer != null) {
            p2pServer.closeSocket();
            p2pServer=null;
        }
    }
    private void noGameParser10(StringTokenizer StringT, String output) {
        if (StringT.hasMoreTokens()==false) {
            noGameParser9(output);
        }
        else { output=RiskUtil.replaceAll(resb.getString( "core.error.syntax"), "{0}", "killserver"); }
    }
    private void noGameParser9(String output) {
        if ( p2pServer != null ) {
            try {
                noGameParser7();
                output=resb.getString( "core.killserver.killed");
                controller.serverState(false);
            }
            catch (Exception e) {
                output=resb.getString( "core.killserver.error")+" "+e.getMessage();
            }

        }
        else {
            output=resb.getString( "core.killserver.noserver");
        }
    }
    private String inGameParser2(List leavers, String id) {
        int size = leavers.size();
        String s = null;
        for (int c=0; c< size ; c++) {
            if ( !((Player)leavers.get(c)).getAddress().equals(id) ) {

                s = ((Player)leavers.get(c)).getAddress();
                break;
            }
        }
        return s;
    }
    private void inGameParser3(Player patc) {
        int value = game.getState();
        while(!(value == RiskGame.STATE_NEW_GAME )) {

            patc.setType( Player.PLAYER_AI_CRAP );
            break;
        }
    }
    private Player inGameParser4(Player patc, int c) {
        Player p = patc;
        if ( game.delPlayer( patc.getName() ) ) {
            c--;
            controller.delPlayer( patc.getName() );
            p = null;
        }
        return p;
    }
    private Player inGameParser5(Player patc, int c) {
        Player p = null;
        int value = game.getState();
        while(value == RiskGame.STATE_NEW_GAME ) {
            // should never return false
            p = inGameParser4(patc,c);
            break;
        }
        return p;
    }
    private Player inGameParser6(Player patc, int c, String output) {
        Player p = null;
        int value = patc.getType();
        while (value  == Player.PLAYER_HUMAN ) {
           
            p = inGameParser5(patc,c);
            inGameParser3(patc);
            break;
        }
        return p;
    }
    private void inGameParser7(Player patc,String newPlayerAddress) {
        if (patc!=null) {
            while(newPlayerAddress!=null) {
                patc.setAddress( newPlayerAddress );
                break;
            }
        }
    }
    private void inGameParser8(List leavers, String id, String newPlayerAddress, String output) {
        int size = leavers.size();
        for (int c=0; c < size; c++) {

            Player patc = ((Player)leavers.get(c));
            if ( patc.getAddress().equals(id) ) {
                patc = inGameParser6(patc,c,output);
                inGameParser7(patc,newPlayerAddress);
            }

        }
    }
    private void inGameParser9(StringTokenizer StringT,String Addr) {
        if (Addr.equals("LEAVE")) {
            String id = StringT.nextToken();
            String output = "someone has gone: ";
            List leavers = game.getPlayers();
            String newPlayerAddress=null;
            newPlayerAddress = inGameParser2(leavers,id);
            inGameParser8(leavers,id,newPlayerAddress,output);
        }
    }
    private void inGameParser10(String Addr, String message, StringTokenizer StringT) {
        if (!Addr.equals("ERROR")) {
            game.addCommand(message);
        }
        if (Addr.equals("ERROR")) { // server has sent us a error
            String Pname = StringT.nextToken();
            boolean value = StringT.hasMoreTokens();
            while (value) {
                Pname = Pname +" "+ StringT.nextToken();
            }
            showMessageDialog(Pname);
        }
    }
    private void inGameParser11(int attSize, int defSize, StringTokenizer StringT, String output, int[] att, int[] def) {
        for (int c=0; c< attSize ; c++) {
            att[c] = RiskGame.getNumber(StringT.nextToken());
            output = output + " " + (att[c]+1);
        }
        for (int c=0; c< defSize ; c++) {
            def[c] = RiskGame.getNumber(StringT.nextToken());
            
        }
    }
    private String inGameParser12(int n,String output) {
        String out = null;
        if (n > 0) {
            if (n > 3) { n=3; }
            out = output + RiskUtil.replaceAll(resb.getString( "core.dice.attackagain"), "{0}", "" + n);
        }
        else {
            out = output + resb.getString( "core.dice.noattackagain");
        }
        return out;
    }
    private String inGameParser13(int result[], String output) {
        String s = null;
        if ( result[4] == result[5] ) {
            int noa = game.moveAll();
            int ma = game.moveArmies( noa );
            //Moved {0} armies to captured country.
            s = output + RiskUtil.replaceAll(resb.getString( "core.dice.armiesmoved"), "{0}", String.valueOf(noa) );
            if (ma==2) {
                s=output + whoWon();
            }
        }
        else {
            //How many armies do you wish to move? ({0} to {1})
            s=output + RiskUtil.replaceAll(RiskUtil.replaceAll(resb.getString( "core.dice.howmanyarmies")
                    , "{0}", String.valueOf(result[4]) )
                    , "{1}", String.valueOf(result[5]) );
        }
        return s;
    }
    private String inGameParser14(int[] result, String output) {
        String s = null;
        if (result[3]==0) {
            int n=((Country)game.getAttacker()).getArmies()-1;
            s=output + System.getProperty("line.separator") + resb.getString( "core.dice.notdefeated") + " ";
            s = inGameParser12(n,output);
        }
        else {
            s=output + System.getProperty("line.separator") + resb.getString( "core.dice.defeated") + " ";
            s = inGameParser13(result,output);
        }
        return s;
    }
    private void inGameParser15(int[] att,int[] def) {
        if ( battle ) {
            controller.showDiceResults( att, def );
            try{ Thread.sleep(SHOW_DICE_SLEEP); }
            catch(InterruptedException e){
                System.out.println("error");
            }

        }
    }
    private void inGameParser16(String Addr, StringTokenizer StringT) {
        String output = null;
        if (Addr.equals("DICE")) {
            int attSize = RiskGame.getNumber(StringT.nextToken());
            int defSize = RiskGame.getNumber(StringT.nextToken());
            output=resb.getString( "core.dice.rolling") + System.getProperty("line.separator") + resb.getString( "core.dice.results");
            int att[] = new int[ attSize ];
            output = output + " " + resb.getString( "core.dice.attacker");
            int def[] = new int[ defSize ];
            output = output + " " + resb.getString( "core.dice.defender");
            inGameParser11(attSize,defSize,StringT,output,att,def);
            output = output + System.getProperty("line.separator");
            int result[] = game.battle( att, def );
            if ( result[0]==1 ) {
                output = output + RiskUtil.replaceAll(RiskUtil.replaceAll(resb.getString( "core.dice.result")
                        , "{0}", String.valueOf(result[2]) ) //defeated
                        , "{1}", String.valueOf(result[1]) );//lost
                output = inGameParser14(result,output);
                inGameParser15(att,def);
            }
            else { output=resb.getString( "core.dice.error.unabletoroll"); }
        }
    }
    private void inGameParser17(String Addr, StringTokenizer StringT) {
        String output = null;
        boolean needInput = true;
        if (Addr.equals("PLAYER")) { // a server command
            int index = Integer.parseInt( StringT.nextToken() );
            Player p = game.setCurrentPlayer( index );
            controller.sendMessage("Game started", false, false);
            output=RiskUtil.replaceAll(resb.getString( "core.player.randomselected"), "{0}", p.getName());
            if ( game.getGameMode()==RiskGame.MODE_SECRET_MISSION || autoplaceall==true ) {
                needInput=false;
            }
            else {
                saveGameToUndoObject();
            }
        }
    }
    private void inGameParser18(String name, Card card) {
        if ( showHumanPlayerThereInfo() ) {
            String cardName;
            if (name.equals(Card.WILDCARD)) {
                cardName = name;
            }
            else {
                cardName = card.getName() + " " + game.getCountryInt( Integer.parseInt(name) ).getName();
            }
            controller.sendMessage("You got a new card: \"" + cardName +"\"", false , false);
        }
    }
    private void inGameParser19(String Addr, StringTokenizer StringT) {
        String output = null;
        if (Addr.equals("CARD")) {
            if ( StringT.hasMoreTokens() ) {
                String name = StringT.nextToken();
                Card card = game.findCardAndRemoveIt( name );
                ((Player)game.getCurrentPlayer()).giveCard( card );
                inGameParser18(name,card);
            }
            Player newplayer = game.endGo();
            output = RiskUtil.replaceAll(resb.getString( "core.player.newselected"), "{0}", newplayer.getName());
            saveGameToUndoObject();

        }
    }
    private void inGameParser20(String Addr, StringTokenizer StringT) {
        String output = null;
        if (Addr.equals("PLACE")) { // a server command
            Country c = game.getCountryInt( Integer.parseInt( StringT.nextToken() ) );
            game.placeArmy( c ,1);
            controller.sendMessage( RiskUtil.replaceAll( resb.getString( "core.place.oneplacedin"), "{0}", c.getName()) , false, false); // Display
            output=resb.getString( "core.place.autoplaceok");
        }
    }
    private void inGameParser21(String Addr, StringTokenizer StringT) {
        String output = null;
        if (Addr.equals("PLACEALL")) { // a server command
            int value = game.getNoCountries();
            for (int c=0; c< value; c++) {
                Country t = game.getCountryInt( Integer.parseInt( StringT.nextToken() ) );
                game.placeArmy( t ,1);
                controller.sendMessage( RiskUtil.replaceAll(RiskUtil.replaceAll( resb.getString("core.place.getcountry")
                        , "{0}", ((Player)game.getCurrentPlayer()).getName())
                        , "{1}", t.getName()) // Display
                        , false, false);
                game.endGo();
            }
            saveGameToUndoObject();
            controller.sendMessage("Auto place all successful.", false, false);
            //New player selected: {0}.
            output= RiskUtil.replaceAll( resb.getString( "core.player.newselected"), "{0}", ((Player)game.getCurrentPlayer()).getName());
        }
    }
    private void inGameParser22(String echo) {
        if (game != null && game.getCurrentPlayer() != null && game.getState()!=RiskGame.STATE_GAME_OVER ) {
            int type = game.getCurrentPlayer().getType();
            String key;
            if (type==Player.PLAYER_HUMAN) {
                key = "newgame.player.type.human";
            }
            else {
                key = "newgame.player.type."+ai.getCommandFromType(type)+"ai";
            }
            String typeString;
            try {
                typeString = resb.getString(key);
            }
            catch (MissingResourceException ex) {
                typeString = key;
            }
            controller.sendMessage( game.getCurrentPlayer().getName()+ "("+typeString+")>"+echo, false, false );
        }
        else {
            controller.sendMessage( "game>" + echo, false, false );
        }
    }
    private void inGameParser23(Object data, String filename, Exception e) {
        if (data == RiskUtil.SUCCESS) {
            try {
                setMap(filename);
            }
            catch (Exception ex) {
                getMapError(ex.toString());
            }
        }
        else {
            getMapError(e.toString());
        }
    }
    private void inGameParser24(StringTokenizer StringT) {
        String output = null;
        if (StringT.countTokens() >= 1) {
            final String filename = RiskUtil.getAtLeastOne(StringT);
            try {
                setMap(filename);
            }
            catch (final Exception e) {
                RiskUtil.streamOpener.getMap(filename, new Observer() {
                    @Override
                    public void update(Observable observable, Object data) {
                        inGameParser23(data,filename,e);
                    }
                });
            }
            output = null; // we have nothing to output now

        }
        else  { output=RiskUtil.replaceAll(resb.getString( "core.error.syntax"), "{0}", "choosemap filename"); }
    }
    private void inGameParser25(StringTokenizer StringT) {
        String output = null;
        if (StringT.countTokens() >= 1) {
            String filename = RiskUtil.getAtLeastOne(StringT);
            try {
                boolean yesmissions = game.setCardsfile(filename);
                controller.showCardsFile( game.getCardsFile() , yesmissions );
                output=RiskUtil.replaceAll(resb.getString( "core.choosecards.chosen"), "{0}", filename);
            }
            catch (Exception e) {
                output=resb.getString( "core.choosecards.error.unable");
            }
        }
        else  { output=RiskUtil.replaceAll(resb.getString( "core.error.syntax"), "{0}", "choosecards filename"); }
    }
    private void inGameParser26(String input, StringTokenizer StringT) {
        if (input.equals("choosemap")) {
            inGameParser24(StringT);
        }
        if (input.equals("choosecards")) {
            inGameParser25(StringT);
        }
    }
    private void inGameParser27(int color, String name, String Addr, int t, String c) {
        String output = null;
        if ( color != 0 && t != -1 && !name.equals("") &&
                ((unlimitedLocalMode && game.addPlayer(t, name, color, "LOCALGAME"))
                        || (!unlimitedLocalMode && game.addPlayer(t, name, color, Addr)))) {
            //New player created, name: {0} color: {1}
            output=RiskUtil.replaceAll(RiskUtil.replaceAll( resb.getString("core.newplayer.created")
                    , "{0}", name)
                    , "{1}", c);

            controller.addPlayer(t, name, color, Addr);
        }
        else { output=resb.getString( "core.newplayer.error.unable"); }
    }
    private void inGameParser28(StringTokenizer StringT, String name) {
        String n  = null;
        boolean value = StringT.hasMoreTokens();
        while (value) {
            n = name + StringT.nextToken();
            if ( StringT.hasMoreTokens() ) { n = name + " "; }
        }
    }
    private void inGameParser29(StringTokenizer StringT, String Addr) {
        String output = null;
        if (StringT.countTokens()>=3) {
            String type=StringT.nextToken();
            if (type.equals("ai")) {
                type = type+" "+StringT.nextToken();
            }
            String c=StringT.nextToken();
            String name="";
            inGameParser28(StringT,name);
            int t=getType(type);
            int color=ColorUtil.getColor( c );
            inGameParser27(color,name,Addr,t,c);
        }
        else  {
            output=RiskUtil.replaceAll( resb.getString( "core.error.syntax"), "{0}", "newplayer type (skill) color name");
        }
    }
    private void inGame2(String name, StringTokenizer StringT) {
        String output;
        if ( game.delPlayer(name) ) {
            controller.delPlayer(name);
            output=RiskUtil.replaceAll(resb.getString( "core.delplayer.deleted"), "{0}", name);
        }
        else { output=resb.getString( "core.delplayer.error.unable"); }
    }
    private void inGameParser30(String input, StringTokenizer StringT) {
        String output = null;
        if (input.equals("delplayer")) {
            if (StringT.countTokens() >= 1) {
                String name=RiskUtil.getAtLeastOne(StringT);
                inGame2(name,StringT);
            }
            else { output=RiskUtil.replaceAll(resb.getString( "core.error.syntax"), "{0}", "delplayer name"); }
        }
    }
    private void inGameParser31(StringTokenizer StringT) {
        String output = null;
        if (StringT.hasMoreTokens()==false) {
            output=resb.getString( "core.info.title") + "\n";
            List players = game.getPlayers();
            int value = players.size();
            for (int a=0; a< value; a++) {
                output = output + resb.getString( "core.info.player") + " " + ((Player)players.get(a)).getName() +"\n";
            }
            output = output + resb.getString( "core.info.mapfile") + " "+ game.getMapFile() +"\n";
            output = output + resb.getString( "core.info.cardsfile") + " "+ game.getCardsFile() ;
        }
        else { output=RiskUtil.replaceAll(resb.getString( "core.error.syntax"), "{0}", "info"); }
    }
    private void inGameParser32(String input, StringTokenizer StringT, String Addr) {
        if (input.equals("newplayer")) {
            inGameParser29(StringT,Addr);
        }
        inGameParser30(input,StringT);
    }
    private void inGameParser33() {
        String output = null;
        if (!replay) {
            int value = RiskGame.MAX_PLAYERS;
            for (int c=1;c<=value;c++) {
                parser("newplayer " + riskconfig.getProperty("default.player"+c+".type")+" "+ riskconfig.getProperty("default.player"+c+".color")+" "+ riskconfig.getProperty("default.player"+c+".name") );
            }
            output = resb.getString( "core.info.autosetup");
        }
        else {
            output = "replay mode, nothing done";
        }
    }
    private void inGameParser34(StringTokenizer StringT) {
        String output = null;
        if (StringT.hasMoreTokens()==false) {
            if ( game.getPlayers().size() == 0) {
                inGameParser33();
            }
            else {
                output = resb.getString( "core.info.autosetup.error");
            }
        }
        else { output=RiskUtil.replaceAll(resb.getString( "core.error.syntax"), "{0}", "autosetup"); }
    }
    private void inGameParser35(int b,StringBuffer outputa, int a,Random r) {
        for (int c=0; c< b ; c++) {
            if (outputa.length()!=0 ) {
                outputa.append(' ');
            }
            outputa.append( r.nextInt(a) );
            a--;
        }
    }
    private void inGameParser36(String Addr) {
        if (game.getGameMode()== RiskGame.MODE_SECRET_MISSION ) {
            Random r = new Random();
            int a = game.getNoMissions();
            int b = game.getNoPlayers();
            StringBuffer outputa=new StringBuffer();
            inGameParser35(b,outputa,a,r);
            gameCommand(Addr, "MISSION", outputa.toString());
        }
    }
    private void inGameParser37(String Addr) {
        if ( game.getGameMode()==RiskGame.MODE_SECRET_MISSION || autoplaceall ) {
            List a = game.shuffleCountries();
            StringBuffer outputb=new StringBuffer();
            int size = a.size();
            for (int c=0; c< size; c++) {
                if (outputb.length()!=0 ) {
                    outputb.append(' ');
                }
                outputb.append( ((Country)a.get(c)).getColor() );
            }
            gameCommand(Addr, "PLACEALL", outputb.toString());
        }
    }
    private void inGameParser38(String Addr) {
        String output;
        boolean needInput;
        if (game.getState() != RiskGame.STATE_NEW_GAME ) {
            controller.noInput();
            controller.startGame( unlimitedLocalMode );
            if ( shouldGameCommand(Addr) ) {
                gameCommand(Addr, "PLAYER", String.valueOf( game.getRandomPlayer() ) );
                inGameParser36(Addr);
                inGameParser37(Addr);
            }
            output=null;
            needInput=false;
        }
        else {
            output=resb.getString( "core.start.error.players");
        }
    }
    private void inGameParser39(String crap, int newgame_type, int newgame_cardType, int n, String Addr, boolean newgame_autoplaceall) {
        String output;
        if (crap==null) {
            // checks all the options are correct to start a game
            if ( newgame_type!=-1 && newgame_cardType!=-1 && n>=2 && n<=RiskGame.MAX_PLAYERS) {
                autoplaceall = newgame_autoplaceall;
            }
            inGameParser38(Addr);
        }
        else {
            output="unknown option: "+crap;
        }
    }
    private void inGameParser40(StringTokenizer StringT,String Addr) {
        String output;
        if (StringT.countTokens() >= 2 && StringT.countTokens() <= 4) {
            int n=game.getPlayers().size();
            int newgame_type = -1;
            int newgame_cardType = -1;
            boolean newgame_autoplaceall = false;
            
          
            String crap = null;
            inGameParser39(crap,newgame_type,newgame_cardType,n,Addr,newgame_autoplaceall);
        }
        else {
            output=RiskUtil.replaceAll(resb.getString( "core.error.syntax"), "{0}", "startgame gametype cardtype (autoplaceall recycle)");
        }
    }
    private void inGameParser41(StringTokenizer StringT) {
        String output;
        if (StringT.countTokens() >= 1) {
            String filename = RiskUtil.getAtLeastOne(StringT);
            try {
                URL url;
                url = (new File(filename)).toURI().toURL();
                
                replay = true;
                output="playing \""+filename+"\"";
            }
            catch(Exception error) {
                output="unable to play \""+filename+"\" "+error;
            }
        }
        else { output=RiskUtil.replaceAll(resb.getString( "core.error.syntax"), "{0}", "play filename"); }
    }
    private void inGameParser42(String input,StringTokenizer StringT, String Addr) {
        if (input.equals("autosetup")) {
            inGameParser34(StringT);
        }
        if (input.equals("startgame")) {
            inGameParser40(StringT,Addr);
        }
    }
    private void inGameParser43() throws IOException,ClassNotFoundException {
        String output;
        if (game.getState()!=RiskGame.STATE_DEFEND_YOURSELF && Undo!=null && Undo.size()!=0) {
            //game = (RiskGame)Undo.getObject( nullCipher );
            ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(Undo.toByteArray()));
            game = (RiskGame)in.readObject();
            output =resb.getString( "core.undo.undone");
        }
        else {
            output =resb.getString( "core.undo.error.unable");
        }
    }
    private void inGameParser44(StringTokenizer StringT) {
        String output = null;
        if ( StringT.hasMoreTokens()==false ) {
            if ( unlimitedLocalMode ) {
                try {
                    // can not undo when defending yourself as it is not really your go
                    inGameParser43();
                }
                catch (Exception e) {
                    logger.log(Level.WARNING, resb.getString( "core.loadgame.error.undo"),e);
                }
            }
            else {
                output = resb.getString( "core.undo.error.network");
            }
        }
        else { output=RiskUtil.replaceAll(resb.getString( "core.error.syntax"), "{0}", "undo"); }
    }
    private void inGameParser45(String input, StringTokenizer StringT, boolean aiPlayer) {
        if (input.equals("undo")) {
            if(game.getState()!=RiskGame.STATE_GAME_OVER && aiPlayer) {
                throw new IllegalArgumentException("ai is trying to call undo");
            }
            inGameParser44(StringT);
        }
    }
    private void inGameParser46(StringTokenizer StringT) {
        String output = null;
        if (StringT.hasMoreTokens()==false) {
            if ( showHumanPlayerThereInfo() ) {
                output = resb.getString( "core.showmission.mission") + " " + getCurrentMission();
            }
            else { output=resb.getString( "core.showmission.error"); }
        }
        else { output=RiskUtil.replaceAll(resb.getString( "core.error.syntax"), "{0}", "showmission"); }
    }
    private void inGameParser47(Country[] v, int c) {
        String output = null;
        if (game.getGameMode() == 2 && game.getSetupDone() && game.getState() !=RiskGame.STATE_SELECT_CAPITAL) {
            List players = game.getPlayers();
            int size = players.size();
            for (int a=0; a< size; a++) {
                if ( ((Player)players.get(a)).getCapital() != null && ((Player)players.get(a)).getCapital() == v[c]) {
                    output = output + " " + RiskUtil.replaceAll( resb.getString( "core.showarmies.captial")
                            , "{0}", ((Player)players.get(a)).getName());
                }
            }
        }
    }
    private void inGameParser48(Country[] v) {
        String output = null;
        int length = v.length;
        for (int c=0; c< length ; c++) {
            output = output + v[c].getColor() + " " + v[c].getName()+" - "; // Display
            if ( v[c].getOwner() != null ) {
                output = output + ((Player)v[c].getOwner()).getName() +" ("+v[c].getArmies() +")";
                inGameParser47(v, c);
                output = output + System.getProperty("line.separator");
            }
            else {
                output = output + resb.getString( "core.showarmies.noowner") + System.getProperty("line.separator");
            }
        }
    }
    private void inGameParser49(StringTokenizer StringT) {
        String output = null;
        if (StringT.hasMoreTokens()==false) {
            if ( game.getState() != RiskGame.STATE_NEW_GAME) {
                Country[] v = game.getCountries();
                output=resb.getString( "core.showarmies.countries") + System.getProperty("line.separator");
                inGameParser48(v);
            }
            else { output=resb.getString( "core.showarmies.error.unable"); }
        }
        else { output=RiskUtil.replaceAll(resb.getString( "core.error.syntax"), "{0}", "showarmies"); }
    }
    private void inGameParser50(StringTokenizer StringT) {
        String output = null;
        if (StringT.hasMoreTokens()==false) {
            String strSelected;
            if ( game.getCurrentPlayer().getAutoEndGo() ) {
                strSelected = "core.autoendgo.on";
            }
            else {
                strSelected = "core.autoendgo.off";
            }
            output = RiskUtil.replaceAll(resb.getString( "core.autoendgo.setto"), "{0}", resb.getString( strSelected));
        }
    }
    private void inGameParser51(String option) {
        String output = null;
        if (option.equals("on") ) {
            game.getCurrentPlayer().setAutoEndGo(true);
            output = RiskUtil.replaceAll(resb.getString( "core.autoendgo.setto"), "{0}", resb.getString( "core.autoendgo.on"));
        }
        else if (option.equals("off") ) {
            game.getCurrentPlayer().setAutoEndGo(false);
            output = RiskUtil.replaceAll(resb.getString( "core.autoendgo.setto"), "{0}", resb.getString( "core.autoendgo.off"));
        }
        else { output=RiskUtil.replaceAll(resb.getString( "core.autoendgo.error.unknown"), "{0}", option); }
    }
    private void inGameParser52(String option) {
        String output = null;
        if (option.equals("on") ) {
            game.getCurrentPlayer().setAutoDefend(true);
            output = RiskUtil.replaceAll(resb.getString( "core.autodefend.setto"), "{0}", resb.getString( "core.autodefend.on"));
        }
        inGameParser51(option);
        if (option.equals("off") ) {
            game.getCurrentPlayer().setAutoDefend(false);
            output = RiskUtil.replaceAll(resb.getString( "core.autodefend.setto"), "{0}", resb.getString( "core.autodefend.off"));
        }
        else { output=RiskUtil.replaceAll(resb.getString( "core.autodefend.error.unknown"), "{0}", option); }
    }
    private void inGameParser53(StringTokenizer StringT) {
        String output = null;
        if (StringT.hasMoreTokens()==false) {
            String strSelected;
            if ( game.getCurrentPlayer().getAutoDefend() ) {
                strSelected = "core.autodefend.on";
            }
            else {
                strSelected = "core.autodefend.on";
            }
            output = RiskUtil.replaceAll(resb.getString( "core.autodefend.setto"), "{0}", resb.getString( strSelected));
        }
    }
    private void inGameParser54(String Addr, StringTokenizer StringT) {
        String output = null;
        boolean needInput;
        if (Addr.equals("MISSION")) { // a server command
            List m = game.getMissions();
            List p = game.getPlayers();
            int size = p.size();
            for (int c=0; c< size; c++) {

                int i = RiskGame.getNumber( StringT.nextToken() );
                ((Player)p.get(c)).setMission( (Mission)m.get(i) );
                m.remove(i);
            }
            output=null;
            needInput=false;
        }
    }
    private void inGameParser55(String input, StringTokenizer StringT) {
        String output = null;
        if (input.equals("autoendgo")) {
            inGameParser50(StringT);
            if (StringT.countTokens() == 1) {
               
            }
            else { output=RiskUtil.replaceAll(resb.getString( "core.error.syntax"), "{0}", "autoendgo on/off"); }
        }
    }

    private void inGameParser60(boolean a, Country country2,Country country1) {
        String output = null;
        if ( a ) {
            //Attack {0} ({1}) with {2} ({3}). (You can use up to {4} dice to attack)
            output = RiskUtil.replaceAll(RiskUtil.replaceAll(RiskUtil.replaceAll(RiskUtil.replaceAll(RiskUtil.replaceAll(resb.getString( "core.attack.attacking")
                    , "{0}", country2.getName()) // Display
                    , "{1}", "" + country2.getArmies())
                    , "{2}", country1.getName()) // Display
                    , "{3}", "" + country1.getArmies())
                    , "{4}", "" + game.getNoAttackDice() );

        }
        else { throw new IllegalArgumentException(resb.getString( "core.attack.error.unable")); }
    }
    private void inGameParser61(int a1, int a2) {
        Country country1;
        Country country2;

        if (a1 != -1) {
            country1=game.getCountryInt(a1);
        }
        else {
            //YURA:LANG country1=game.getCountryByName(arg1);
            country1=null;
        }
        if (a2 != -1) {
            country2=game.getCountryInt(a2);
        }
        else {
            //YURA:LANG country2=game.getCountryByName(arg2);
            country2=null;
        }
        boolean a=game.attack(country1, country2);
        inGameParser60(a,country2,country1);
    }
    private void inGameParser62(String input, StringTokenizer StringT) {
        if (input.equals("attack")) {
            if (StringT.countTokens()==2) {
                String arg1=StringT.nextToken();
                String arg2=StringT.nextToken();
                int a1=RiskGame.getNumber(arg1);
                int a2=RiskGame.getNumber(arg2);
                inGameParser61(a1,a2);
            }
            else { throw new IllegalArgumentException(RiskUtil.replaceAll(resb.getString( "core.error.syntax"), "{0}", "attack country country")); }
        }
    }
    private void inGameParser63(StringTokenizer StringT) {
        String output = null;
        if (StringT.hasMoreTokens()==false) {
            if ( game.endAttack() ) {
                output=resb.getString( "core.attack.end.ended");
            }
            else { throw new IllegalArgumentException(resb.getString( "core.attack.end.error.unable")); }
        }
        else { throw new IllegalArgumentException(RiskUtil.replaceAll(resb.getString( "core.error.syntax"),
                "{0}", "endattack")); }
    }
    private void inGameParser64(String input, StringTokenizer StringT) {
        if (game.getState()==RiskGame.STATE_ATTACKING) {
            inGameParser62(input,StringT);
            if (input.equals("endattack")) {
                inGameParser63(StringT);
            }
            else { throw new IllegalArgumentException(RiskUtil.replaceAll(resb.getString( "core.error.incorrect"), "{0}", "attack, endattack")); }

        }
    }

    private void inGameParser68(String input, StringTokenizer StringT) {
        String output;
        if (input.equals("autodefend")) {
            inGameParser53(StringT);
            if (StringT.countTokens() == 1) {
                String option = StringT.nextToken();
                inGameParser52(option);
            }
            else { output=RiskUtil.replaceAll(resb.getString( "core.error.syntax"), "{0}", "autodefend on/off"); }
        }
    }
    private void inGameParser69(String input, StringTokenizer StringT) {
        if (input.equals("showmission")) {
            inGameParser46(StringT);
        }
        if (input.equals("showarmies")) {
            inGameParser49(StringT);
        }
    }
    private void inGameParser70(Country country1,Country country2, int noa) {
        String output = null;
        if ( game.moveArmy(country1, country2, noa) ) {
            //Moved {0} armies from {1} to {2}.
            output = RiskUtil.replaceAll(RiskUtil.replaceAll(RiskUtil.replaceAll(resb.getString( "core.tacmove.movedfromto")
                    , "{0}", "" + noa)
                    , "{1}", country1.getName()) // Display
                    , "{2}", country2.getName()); // Display
        }
        else { throw new IllegalArgumentException(resb.getString( "core.tacmove.error.unable")); }
    }
    private void inGameParser71(int a1, int a2, StringTokenizer StringT) {
        Country country1;
        Country country2;
        if (a1 != -1) {
            country1=game.getCountryInt(a1);
        }
        else {
            //YURA:LANG country1=game.getCountryByName(arg1);
            country1=null;
        }
        if (a2 != -1) {
            country2=game.getCountryInt(a2);
        }
        else {
            //YURA:LANG country2=game.getCountryByName(arg2);
            country2=null;
        }
        int noa=RiskGame.getNumber( StringT.nextToken() );
        inGameParser70(country1,country2,noa);
    }
    private void inGameParser72(String input, StringTokenizer StringT) {
        if (input.equals("movearmies")) {
            if (StringT.countTokens()==3) {
                String arg1=StringT.nextToken();
                String arg2=StringT.nextToken();
                int a1=RiskGame.getNumber(arg1);
                int a2=RiskGame.getNumber(arg2);
                inGameParser71(a1,a2,StringT);
            }
            else {
                throw new IllegalArgumentException(RiskUtil.replaceAll(resb.getString(
                        "core.error.syntax"), "{0}", "movearmies country country number")); }
        }
    }
    private void inGameParser73(String input, StringTokenizer StringT) {
        boolean needInput;
        String output;
        if (input.equals("endgo")) {
            if (StringT.hasMoreTokens()==false) {
                needInput=false;
                output=null;
                controller.sendMessage(resb.getString( "core.endgo.ended"), false , false);
                DoEndGo();
            }
            else {
                throw new IllegalArgumentException(RiskUtil.replaceAll(resb.getString(
                        "core.error.syntax"), "{0}", "endgo")); }
        }
        else {
            throw new IllegalArgumentException(RiskUtil.replaceAll(resb.getString( "core.error.incorrect"),
                    "{0}", "emdgo")); }
    }
    private void inGameParser75(String input, StringTokenizer StringT) {
        if (game.getState()==RiskGame.STATE_FORTIFYING) {
            inGameParser72(input,StringT);
        }
        if (game.getState()==RiskGame.STATE_END_TURN) {
            inGameParser73(input,StringT);
        }
    }
    private void inGameParser76(Country t) {
        String output = null;
        if ( t != null && game.setCapital(t) ) {
            if ( showHumanPlayerThereInfo() ) {
                output=RiskUtil.replaceAll(resb.getString( "core.capital.selected"), "{0}", t.getName()); // Display
            }
            else {
                output=resb.getString( "core.capital.hasbeenselected");
            }
        }
        else {
            throw new IllegalArgumentException(resb.getString( "core.capital.error.unable")); }
    }
    private void inGameParser77(int nCountryId, Country t) {
        if (nCountryId != -1) {
            t = game.getCountryInt( nCountryId);
        } else {
            //YURA:LANG t = game.getCountryByName( strCountry);
            t=null;
        }
    }
    private void inGameParser78(StringTokenizer StringT) {
        if (StringT.countTokens()==1) {
            String strCountry = StringT.nextToken();
            int nCountryId = RiskGame.getNumber(strCountry);
            Country t = null;
            inGameParser77(nCountryId,t);
            inGameParser76(t);
        }
        else {
            throw new IllegalArgumentException(RiskUtil.replaceAll(resb.getString(
                    "core.error.syntax"), "{0}", "capital country")); }
    }
    private void inGameParser79(String input, StringTokenizer StringT) {
        if (game.getState()==RiskGame.STATE_SELECT_CAPITAL) {
            if (input.equals("capital")) {
                inGameParser78(StringT);
            }
            else { throw new IllegalArgumentException(RiskUtil.replaceAll(resb.getString( "core.error.incorrect"), "{0}", "capital")); }
        }
    }
    private void inGameParser80(String input, StringTokenizer StringT) {
        String output;
        if (input.equals("play")) {
            inGameParser41(StringT);
        }
        else {
            output=RiskUtil.replaceAll(resb.getString( "core.error.incorrect"),
                    "{0}", "newplayer, delplayer, startgame, choosemap, choosecards, info, autosetup"); }
    }
    private void inGameParser81(int[] attackerResults,int[] defenderResults) {
        String serverRoll = null;
        int size1 = attackerResults.length;
        for (int c=0; c< size1; c++) {
            serverRoll = serverRoll + attackerResults[c] + " ";
        }
        int size2 = defenderResults.length;
        for (int c=0; c<  size2; c++) {
            serverRoll = serverRoll + defenderResults[c] + " ";
        }
    }
    private void inGameParser82(String Addr) {
        if ( shouldGameCommand(Addr) ) { // recursive call
            int[] attackerResults = game.rollDice( game.getAttackerDice() );
            int[] defenderResults = game.rollDice( game.getDefenderDice() );
            String serverRoll = "";
            serverRoll = serverRoll + attackerResults.length + " ";
            serverRoll = serverRoll + defenderResults.length + " ";
            inGameParser81(attackerResults,defenderResults);
            gameCommand(Addr, "DICE", serverRoll );
        }
    }
    private void inGameParser83(int dice) {
        if ( battle ) {
            controller.setNODDefender(dice);
            try{ Thread.sleep(ROLL_DICE_SLEEP); }
            catch(InterruptedException e){
                System.out.println("null");
            }
        }
    }
    private void inGameParser84(int dice, String Addr) {
        String output;
        boolean needInput;
        if ( dice != -1 && game.rollD(dice) ) {
            inGameParser83(dice);
            // client does a roll, and this is not called
            inGameParser82(Addr);
            output=null;
            needInput=false;
        }
        else { throw new IllegalArgumentException(resb.getString( "core.roll.error.unable")); }
    }
    private void inGameParse85(String input, StringTokenizer StringT, String Addr) {
        if (input.equals("roll")) {
            if (StringT.countTokens()==1) {
                int dice=RiskGame.getNumber( StringT.nextToken() );
                inGameParser84(dice,Addr);
            }
            else { throw new IllegalArgumentException(RiskUtil.replaceAll(resb.getString( "core.error.syntax"), "{0}", "roll number")); }
        }
        else {
            throw new IllegalArgumentException(RiskUtil.replaceAll(resb.getString(
                    "core.error.incorrect"), "{0}", "roll")); }
    }
    private void inGameParser86(String input, StringTokenizer StringT, String Addr) {
        if (game.getState()==RiskGame.STATE_DEFEND_YOURSELF) {
            inGameParse85(input,StringT,Addr);
        }
        else { throw new IllegalStateException(resb.getString( "core.error.unknownstate")); }
    }
    private void inGameParser87(String input, StringTokenizer StringT, String Addr) {
        String output;
        if (game.getState()==RiskGame.STATE_NEW_GAME) {
            inGameParser26(input,StringT);
            inGameParser32(input,StringT,Addr);
            inGameParser31(StringT);
            inGameParser42(input,StringT,Addr);
            inGameParser55(input,StringT);
            inGameParser80(input,StringT);
        }
        else {
            boolean aiPlayer = game.getCurrentPlayer().getType()!=Player.PLAYER_HUMAN;
            try {
                // UNDO
                inGameParser45(input,StringT,aiPlayer);
                inGameParser69(input,StringT);
                inGameParser68(input,StringT);
                inGameParser64(input,StringT);
                inGameParser75(input,StringT);
                inGameParser79(input,StringT);
                inGameParser86(input,StringT,Addr);
            }
            catch (IllegalArgumentException ex) {
                output=ex.getMessage();
            }
        }
    }
    private void inGameParser88(String Addr, String message, StringTokenizer StringT) {
        String output;
        if (Addr.equals("RENAME")) {
            Map map = Url.toHashtable( message.substring( Addr.length()+1 ) );
            String oldName = (String)map.get("oldName");
            String newName = (String)map.get("newName");
            String newAddress = (String)map.get("newAddress");
            int newType = Integer.parseInt((String)map.get("newType"));
            renamePlayer(oldName,newName,newAddress,newType);
        }
        else {
            String echo = message.substring( Addr.length()+1 );
            inGameParser22(echo);
            String input=StringT.nextToken();
            output="";
            inGameParser87(input,StringT,Addr);
            updateBattleState();

        }
    }
    private void inGameParser89(String output) {
        boolean b = game.getCurrentPlayer().getAutoEndGo();
        while (b) {
            controller.sendMessage(output, false, false );
            break;
        }
        boolean b2 = !(game.getCurrentPlayer().getAutoEndGo() );
        while (b2) {
            controller.sendMessage(output, true, true );
            break;
        }
    }
    private void inGameParser90(String output) {
        if (output!=null) {
            // give a output
            if (game==null) {
                controller.sendMessage(output, false, true );
            }
            else if ( game.getState()==RiskGame.STATE_NEW_GAME ) {
                controller.sendMessage(output, false, true );
            }
            else if ( game.getState()==RiskGame.STATE_GAME_OVER ) {
                controller.sendMessage(output, true, true );
            }
            else if (game.getState()==RiskGame.STATE_END_TURN) {
                inGameParser89(output);
            }
            else {// if player type is human or neutral or ai
                controller.sendMessage(output, true, true );
            }
        }
    }
    /**
     * This parses the string, calls the relavant method and displays the correct error messages
     */
    protected void inGameParser(final String message) {
        controller.sendDebug(message);
       
        String output=null;
        StringTokenizer StringT = new StringTokenizer( message );
        final String Addr = StringT.nextToken();
        inGameParser10(Addr,message,StringT);
        inGameParser9(StringT,Addr);
        inGameParser16(Addr,StringT);
        inGameParser17(Addr,StringT);
        inGameParser19(Addr,StringT);
        inGameParser20(Addr,StringT);
        inGameParser21(Addr,StringT);
        inGameParser54(Addr,StringT);
        inGameParser88(Addr,message,StringT);
        // give a output if there is one
        inGameParser90(output);
    }

    // TODO is this thread safe???
    private void setMap(String filename) throws Exception {

        if (game.getState()==RiskGame.STATE_NEW_GAME) {

            boolean yesmissions = game.setMapfile(filename);

            setupPreviews(yesmissions);

            //New map file selected: "{0}" (cards have been reset to the default for this map)
            String output= RiskUtil.replaceAll( resb.getString( "core.choosemap.mapselected"), "{0}", filename);

            controller.sendMessage(output, false , true);

        }
        else {
            controller.startGame(unlimitedLocalMode);
        }
    }
    private void getMapError(String exception) {

        String output = resb.getString( "core.choosemap.error.unable")+" "+exception;
        controller.sendMessage(output, false , true);
        showMessageDialog(output);
    }

    private void setupPreviews(boolean yesmissions) {
        controller.showMapPic( game );
        controller.showCardsFile( game.getCardsFile() , yesmissions );
    }

    private boolean doesMapHaveMission() {
        java.util.Map cardsinfo = RiskUtil.loadInfo( game.getCardsFile() ,true);
        String[] missions = (String[])cardsinfo.get("missions");
        return missions.length > 0;
    }

    public int getType(String type) {
        if (type.equals("human")) {
            return Player.PLAYER_HUMAN;
        }
        if (type.startsWith("ai ")) {
            String aiType = type.substring(3);
            try {
                return ai.getTypeFromCommand(aiType);
            }
            catch (IllegalArgumentException ex) {
                return -1;
            }
        }
        return -1;
    }
    public String getType(int type) {
        if (type==Player.PLAYER_HUMAN) {
            return "human";
        }
        else {
            return "ai "+ai.getCommandFromType(type);
        }
    }

    /**
     * return true ONLY if info of this Player p should be disclosed to this computer
     */
    private boolean showHumanPlayerThereInfo(Player p) {
        return game.getState()==RiskGame.STATE_GAME_OVER || ( (p != null) && ( p.getType()==Player.PLAYER_HUMAN ) && ( unlimitedLocalMode || myAddress.equals( p.getAddress() ) ) );
    }

    public boolean showHumanPlayerThereInfo() {
        return showHumanPlayerThereInfo( game.getCurrentPlayer() );
    }

    /**
     * Method that deals with an end of a player's turn
     */
    public void DoEndGo() {

        controller.noInput(); // definatly need to block input at the end of someones go
        String Addr = ((Player)game.getCurrentPlayer()).getAddress();

        if (shouldGameCommand(Addr)) {
            //give them a card if they deserve one
            gameCommand(Addr, "CARD", game.getDesrvedCard() );
        }
    }

    void gameCommand(String address,String command,String options) {
        if (!replay) {
            String fullCommand = command+" "+options;
            if ( onlinePlayClient == null ) {
                inGameParser( fullCommand );
            }
            else if ( address.equals( myAddress ) ) {
                onlinePlayClient.sendGameCommand( fullCommand );
            }
        }
    }
    boolean shouldGameCommand(String Addr) {
        return !replay && (onlinePlayClient == null || myAddress.equals(Addr));
    }

    public void setReplay(boolean a) {
        replay = a;
    }

    /**
     * This deals with trying to find out what input is required for the parser
     */
    private void getInput2() {
        if (game.getState()==RiskGame.STATE_TRADE_CARDS) {
            controller.sendMessage( RiskUtil.replaceAll(resb.getString( "core.input.newarmies"), "{0}", ((Player)game.getCurrentPlayer()).getExtraArmies() + "") , false, false);
            //controller.armiesLeft( ((Player)game.getCurrentPlayer()).getExtraArmies() , game.NoEmptyCountries() );
        }
    }
    private void getInput3() {
        int gs = game.getState();
        boolean c = game.getCurrentPlayer().getAutoDefend();
        int p = ((Player)game.getCurrentPlayer()).getType();
        while(gs == RiskGame.STATE_DEFEND_YOURSELF && c) {
            parser( getBasicPassiveGo() );
            break;
        }
        while (p ==Player.PLAYER_HUMAN ) {
            controller.needInput( game.getState() );
            break;
        }
    }
    private void getInput4() {
        boolean eq = ((Player)game.getCurrentPlayer()).getAddress().equals(myAddress);
        if (!replay) {
            while ( unlimitedLocalMode || eq) {
                getInput3();
                ai.play(this);
                break;
            }
        }
    }
    private void getInput5() {
        if (game.getState()==RiskGame.STATE_PLACE_ARMIES) {
            controller.sendMessage( RiskUtil.replaceAll(resb.getString( "core.input.armiesleft"), "{0}", ((Player)game.getCurrentPlayer()).getExtraArmies() + ""), false, false);
        }
    }
    private void getInput6() {
        if (game==null) {
            controller.needInput( -1 );
        }
    }
    public void getInput() {
        setHelp();
        getInput6();
        getInput2();
        // work out what to do next
        if ( game!=null && game.getCurrentPlayer()!=null && game.getState()!=RiskGame.STATE_GAME_OVER ) {// if player type is human or neutral or ai
            updateBattleState();
            getInput5();
            getInput4();
        }
        else {
            controller.needInput( game.getState() );
        }
    }
    final AIManager ai = new AIManager();

    public String getBasicPassiveGo() {
        return ai.getOutput(game,Player.PLAYER_AI_CRAP);
    }

    public String whoWon() {
        Player winner = getWinner();
        String text = System.getProperty("line.separator") +
                RiskUtil.replaceAll(resb.getString("core.whowon.hehaswon"), "{0}", winner.getName());
        if ( game.getGameMode() == RiskGame.MODE_SECRET_MISSION ) {
            //There mission was: {0}
            text=text + System.getProperty("line.separator") +
                    RiskUtil.replaceAll(resb.getString( "core.whowon.mission"), "{0}", winner.getMission().getDiscription());
        }
        return text;
    }

    public Player getWinner() {
        if (game.getState() == RiskGame.STATE_GAME_OVER) {
            return game.getCurrentPlayer();
        }
        return null;
    }
    private void setHelp2(int type, String strId) {
        if (type==Player.PLAYER_HUMAN) {
            strId = "core.help.move.human";
        }
        else {
            strId = "core.help.move.ai."+ai.getCommandFromType(type);
        }
    }
    private void setHetlp3(String help) {
        if ( game!=null && game.getCurrentPlayer() != null ) {

            String strId = null;

            int type = game.getCurrentPlayer().getType();
            setHelp2(type,strId);
            try {
                help = RiskUtil.replaceAll(resb.getString(strId), "{0}", game.getCurrentPlayer().getName()) +" ";
            }
            catch (MissingResourceException ex) {
                // fallback just in case we dont have a string
                help = strId+": ("+game.getCurrentPlayer().getName()+") ";
            }
        }
    }
    private void setHelp4(String help) {
        if (game == null) {
            help = resb.getString( "core.help.newgame");
        }
        if (game.getState()==RiskGame.STATE_NEW_GAME) {
            help = resb.getString( "core.help.createplayers");
        }
    }
    private void setHelp5(String help) {
        boolean b = game.getSetupDone();
        boolean b2 = game.NoEmptyCountries();
        while(b) { help = help + resb.getString( "core.help.placearmies");
            break;
        }

        while(b2) { help = help + resb.getString( "core.help.placearmy");
            break;
        }
    }
    private void setHelp6(String help) {
        if (game.getState()==RiskGame.STATE_PLACE_ARMIES) {
            setHelp5(help);
            boolean value = (!(game.getSetupDone() ));
            boolean value2 = (!(game.NoEmptyCountries()));
            while(value && value2)
            { help = help + resb.getString( "core.help.placearmyempty");
                break;
            }

        }
    }
    private void setHelp7(String help) {
        if (game.getState()==RiskGame.STATE_ATTACKING) {
            help = help + resb.getString( "core.help.attack");
        }
        if (game.getState()==RiskGame.STATE_ROLLING) {
            help = help + resb.getString( "core.help.rollorretreat");
        }
        if (game.getState()==RiskGame.STATE_BATTLE_WON) {
            help = help + resb.getString( "core.help.youhavewon");
        }
    }
    private void setHelp8(String help) {
        if (game.getState()==RiskGame.STATE_FORTIFYING) {
            help = help + resb.getString( "core.help.fortifyposition");
        }
        if (game.getState()==RiskGame.STATE_END_TURN) {
            help = help + resb.getString( "core.help.endgo");
        }
        if (game.getState()==RiskGame.STATE_GAME_OVER) {
            //the game is over, {0} has won! close the game to create a new one
            help = RiskUtil.replaceAll(resb.getString( "core.help.gameover"), "{0}", ((Player)game.getCurrentPlayer()).getName());
        }
    }
    private void setHelp9(String help) {
        if (game.getState()==RiskGame.STATE_SELECT_CAPITAL) {
            help = help + resb.getString( "core.help.selectcapital");
        }
        if (game.getState()==RiskGame.STATE_DEFEND_YOURSELF) {
            help = help + resb.getString( "core.help.defendyourself");
        }
        else {
            help = resb.getString( "core.help.error.unknownstate");
        }
    }
    /** Shows helpful tips in each game state */
    public void setHelp() {

        String help="";
        setHetlp3(help);
        setHelp4(help);

        if (game.getState()==RiskGame.STATE_TRADE_CARDS) {
            help = help + resb.getString( "core.help.trade");
        }
        setHelp6(help);
        setHelp7(help);
        setHelp8(help);
        setHelp9(help);
        controller.setGameStatus( help );
    }

    boolean skipUndo; // sometimes on some JVMs this just does not work
    private void saveGameToUndoObject() {

        if (skipUndo) return;

        if ( unlimitedLocalMode ) {

            // the game is saved
            try {
                synchronized (Undo) {
                    Undo.reset();
                    game.saveGame(Undo);
                }
            }
            catch (OutOfMemoryError e) {
                // what can we do :-(
                Undo.reset(); // do not keep broken data, TODO, this does NOT clean up memory
                skipUndo = true;
                logger.log(Level.INFO,resb.getString("core.loadgame.error.undo"),e);
            }
            catch (Throwable e) {
                Undo.reset(); // do not keep broken data, TODO, this does NOT clean up memory
                skipUndo = true;
                logger.log(Level.WARNING, resb.getString( "core.loadgame.error.undo"),e);
            }
        }
    }
    public void disconnected() {

        //System.out.print("Got kicked off the server!\n");
        closeGame();

        controller.sendMessage(resb.getString( "core.kicked.error.disconnected"),false,false);

        getInput();

    }
    private void update2(Player attackingPlayer, Player defendingPlayer) {
        if ( showHumanPlayerThereInfo(attackingPlayer) || showHumanPlayerThereInfo(defendingPlayer) ) {
            controller.openBattle( game.getAttacker().getColor() , game.getDefender().getColor() );
            // we are opeing the battle at a strange point, when NODAttacker is already set, so we should update it on the battle
            if (game.getState()==RiskGame.STATE_DEFEND_YOURSELF) {
                controller.setNODAttacker(game.getAttackerDice());
            }
            battle=true;
        }
    }
    private void updateBattleState() {
        if ((game.getState()==RiskGame.STATE_ROLLING || game.getState()==RiskGame.STATE_DEFEND_YOURSELF) && !battle) {
            Player attackingPlayer = game.getAttacker().getOwner();
            Player defendingPlayer = game.getDefender().getOwner();
            update2(attackingPlayer,defendingPlayer);
        }
        // if someone retreats
        else if (game.getState()!=RiskGame.STATE_ROLLING && game.getState()!=RiskGame.STATE_DEFEND_YOURSELF) {
            closeBattle();
        }
    }

    public void closeBattle() {
        if ( battle ) { controller.closeBattle(); battle=false; }
    }
    /**
     * Checks whether a Player has armies in a country
     * @param name The index of the country
     * @return int Returns the number of armies
     */
    public int hasArmiesInt(int name) {

        return ((Country)game.getCountryInt(name)).getArmies();

    }
    /**
     * checks whether a Player can attach a country
     * @param nCountryFrom	The name of the country attacking
     * @param nCountryTo	The name of the country defending
     * @return boolean Returns true if the player can attack the other one, false if not
     */
    public boolean canAttack(int nCountryFrom, int nCountryTo)
    {
        if (game.getCountryInt( nCountryFrom).isNeighbours( game.getCountryInt( nCountryTo))) {
            return true;
        }
        return false;
    }//public boolean canAttack(int nCountryFrom, int nCountryTo)

    /**
     * Checks whether a Player owns a country
     * @param name The name of the country
     * @return boolean Returns true if the player owns the country, else returns false
     */
    public boolean isOwnedCurrentPlayerInt(int name) {

        // not thread safe, so this can cause problems, but this method is used in display to thats ok


        if ( (game!=null && game.getCurrentPlayer()!=null && game.getCountryInt( name )!=null) &&

                ( ((Country)game.getCountryInt( name )).getOwner() == null || ((Country)game.getCountryInt( name )).getOwner() == game.getCurrentPlayer() )

                ) { return true; }
        else { return false; }


    }

    /**
     * Get the current mission of the game, depending on the game mode
     * @return String Returns the current mission
     */
    public String getCurrentMission() {

        if ( game.getGameMode() == RiskGame.MODE_DOMINATION ) {
            return resb.getString( "core.mission.conquerworld");
        }
        //else if ( game.getGameMode() == 1 ) {
        //	return resb.getString( "core.mission.eliminateenemy");
        //}
        else if ( game.getGameMode() == RiskGame.MODE_CAPITAL ) {
            return resb.getString( "core.mission.capturecapitals");
        }
        else if ( game.getGameMode() == RiskGame.MODE_SECRET_MISSION ) {
            return ((Mission)((Player)game.getCurrentPlayer()).getMission()).getDiscription();
        }
        else {
            return resb.getString( "core.mission.error.cantshow");
        }
    }
    /**
     * Get the present game
     * @return RiskGame Return the current game
     */
    public RiskGame getGame() {
        return game;
    }

    public void showMessageDialog(String a) {

        controller.showMessageDialog(a);

    }

    private void closeGame() {
        synchronized(this) {

            // shutdown the network connection for this game
            if (onlinePlayClient != null) {
                onlinePlayClient.closeGame();
                onlinePlayClient = null;

                // in case lobby had set us some other address we reset it
                myAddress = createRandomUniqueAddress();
            }

            // if there are more commands for this game from the network we should clear them
            if (!inbox.isEmpty()) {
                logger.log(Level.INFO, "clearing commands " + inbox);
                inbox.clear();
            }

            // shutdown the GUI for this game
            if (game != null) {
                // does not work from here
                closeBattle();
                controller.closeGame();
                game = null;
            }
        }

    }

    public void setGame(RiskGame b) {
        if (game!=null) {
            closeBattle();
            controller.closeGame();
        }
        inbox.clear();
        game = b;
        controller.startGame(unlimitedLocalMode);// need to always call this as there may be a new map
        getInput();
    }
    private void renameP2(Player player, String newName,String name,Player leaver) {
        Player newNamePlayer = null;
        if (player.getName().equals(newName)) {
            newNamePlayer = player;
        }
        if (player.getName().equals(name)) {
            leaver=player;
        }
    }
    private void renameP3(Player leaver,String name,String newName) {
        Player newNamePlayer = null;
        if (leaver==null) {
            throw new IllegalArgumentException("can not find player with name \""+name+"\"");
        }
        if (newNamePlayer!=null && !name.equals(newName)) {
            throw new IllegalArgumentException("can not rename \""+name+"\". someone with new name \""+newName+"\" is already in this game");
        }
    }
    private void renamePlayer(String name,String newName, String newAddress,int newType) {

        if ("".equals(name) || "".equals(newName) || "".equals(newAddress) || newType==-1) {
            throw new IllegalArgumentException("bad rename "+name+" "+newName+" "+newAddress+" "+newType);
        }

        // get all the players and make all with the ip of the leaver become nutral
        List players = game.getPlayers();
        
        int size = players.size();
        for (int c=0; c< size; c++) {
            Player player = (Player)players.get(c);
            renameP2(player,newName,name,leaver);
        }
        // AI will never have players addr for lobby game
        renameP3(leaver,name,newName);
        leaver.rename( newName );
        leaver.setType( newType );
        leaver.setAddress( newAddress );

        if (onlinePlayClient!=null) {
            onlinePlayClient.playerRenamed(name,newName, newAddress,newType);
        }
    }
}
