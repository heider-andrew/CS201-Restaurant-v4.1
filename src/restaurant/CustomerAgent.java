package restaurant;

import restaurant.gui.RestaurantGui;
import restaurant.layoutGUI.*;
import agent.Agent;
import java.util.*;
import java.awt.Color;

/** Restaurant customer agent. 
 * Comes to the restaurant when he/she becomes hungry.
 * Randomly chooses a menu item and simulates eating 
 * when the food arrives. 
 * Interacts with a waiter only */
public class CustomerAgent extends Agent {
    private String name;
    private int hungerLevel = 5;  // Determines length of meal
    private RestaurantGui gui;
    private int money;
    private Bill whatIOweTheMan;
    private String myFood;
    
    // ** Agent connections **
    private HostAgent host;
    private WaiterAgent waiter;
    private CashierAgent cashier;
    Restaurant restaurant;
    private Menu menu;
    Timer timer = new Timer();
    GuiCustomer guiCustomer; //for gui
   // ** Agent state **
    private boolean isHungry = false; //hack for gui
    public enum AgentState
	    {DoingNothing, WaitingInRestaurant, SeatedWithMenu, WaiterCalled, WaitingForFood, Eating, TryingToPay, WaitingToPay, Paying, Payed};
	//{NO_ACTION,NEED_SEATED,NEED_DECIDE,NEED_ORDER,NEED_EAT,NEED_LEAVE};
    private AgentState state = AgentState.DoingNothing;//The start state
    public enum AgentEvent 
	    {gotHungry, beingSeated, decidedChoice, waiterToTakeOrder, badSelection, foodDelivered, doneEating, ToldToWait, ReadyForPayment, PaymentRecieved};
    List<AgentEvent> events = new ArrayList<AgentEvent>();
    
    /** Constructor for CustomerAgent class 
     * @param name name of the customer
     * @param gui reference to the gui so the customer can send it messages
     */
    public CustomerAgent(String name, RestaurantGui gui, Restaurant restaurant) {
	super();
	this.gui = gui;
	this.name = name;
	this.money = (int)(Math.random()*15) + 5;
	this.restaurant = restaurant;
	guiCustomer = new GuiCustomer(name.substring(0,2), new Color(0,255,0), restaurant);
    }
    public CustomerAgent(String name, Restaurant restaurant) {
	super();
	this.gui = null;
	this.name = name;
	this.money = (int)(Math.random()*15) + 5;
	this.restaurant = restaurant;
	guiCustomer = new GuiCustomer(name.substring(0,1), new Color(0,255,0), restaurant);
    }
    // *** MESSAGES ***
    /** Sent from GUI to set the customer as hungry */
    public void setHungry() {
	events.add(AgentEvent.gotHungry);
	isHungry = true;
	print("I'm hungry");
	stateChanged();
    }
    /** Waiter sends this message so the customer knows to sit down 
     * @param waiter the waiter that sent the message
     * @param menu a reference to a menu */
    public void msgFollowMeToTable(WaiterAgent waiter, Menu menu) {
	this.menu = menu;
	this.waiter = waiter;
	print("Received msgFollowMeToTable from" + waiter);
	events.add(AgentEvent.beingSeated);
	stateChanged();
    }
    
    public void msgHeresYourBill(Bill b){
    	whatIOweTheMan = b;
    }
    
    /** Waiter sends this message to take the customer's order */
    public void msgDecided(){
	events.add(AgentEvent.decidedChoice);
	stateChanged(); 
    }
    /** Waiter sends this message to take the customer's order */
    public void msgWhatWouldYouLike(){
	events.add(AgentEvent.waiterToTakeOrder);
	stateChanged(); 
    }

    /** Waiter sends this when the food is ready 
     * @param choice the food that is done cooking for the customer to eat */
    public void msgHereIsYourFood(String choice) {
	events.add(AgentEvent.foodDelivered);
	stateChanged();
    }
    /** Timer sends this when the customer has finished eating */
    public void msgDoneEating() {
	events.add(AgentEvent.doneEating);
	stateChanged(); 
    }
    
    public void msgResturauntFull(HostAgent h){
    	float shouldILeave;
    	Random myRandom = new Random();
    	shouldILeave = myRandom.nextFloat();
    	if (shouldILeave > .9 || name.equals("steve")){
    		print("this place sucks, im leaving");
    		h.msgIAmLeaving(this);
    		leaveRestaurant();
    		state = AgentState.DoingNothing;
    	}
    }
    
    public void msgGoStandInLine(){
    	events.add(AgentEvent.ToldToWait);
    	stateChanged();
    }
    
    public void msgTimeToPay(CashierAgent c){
    	events.add(AgentEvent.ReadyForPayment);
    	stateChanged();
    }
    
    public void msgGetOuttaMyHouse(){
    	events.add(AgentEvent.PaymentRecieved);
    	stateChanged();
    }
    
    public void msgRepickFood(){
    	events.add(AgentEvent.badSelection);
    	stateChanged();
    }


    /** Scheduler.  Determine what action is called for, and do it. */
    protected boolean pickAndExecuteAnAction() {
	if (events.isEmpty()) return false;
	AgentEvent event = events.remove(0); //pop first element
	
	//Simple finite state machine
	if (state == AgentState.DoingNothing){
	    if (event == AgentEvent.gotHungry)	{
		goingToRestaurant();
		state = AgentState.WaitingInRestaurant;
		return true;
	    }
	    // elseif (event == xxx) {}
	}
	if (state == AgentState.WaitingInRestaurant) {
	    if (event == AgentEvent.beingSeated)	{
		makeMenuChoice();
		state = AgentState.SeatedWithMenu;
		return true;
	    }
	}
	if (state == AgentState.SeatedWithMenu) {
	    if (event == AgentEvent.decidedChoice)	{
		callWaiter();
		state = AgentState.WaiterCalled;
		return true;
	    }
	}
	if (state == AgentState.WaiterCalled) {
	    if (event == AgentEvent.waiterToTakeOrder)	{
		orderFood();
		state = AgentState.WaitingForFood;
		return true;
	    }
	}
	if (state == AgentState.WaitingForFood) {
	    if (event == AgentEvent.foodDelivered)	{
		eatFood();
		state = AgentState.Eating;
		return true;
	    }
	    if (event == AgentEvent.badSelection){
	    	repickFood(myFood);
	    	return true;
	    }
	}
	if (state == AgentState.Eating) {
	    if (event == AgentEvent.doneEating)	{
		goToPay();
		state = AgentState.TryingToPay;
	    return true;
	    }
	}
	if (state == AgentState.TryingToPay){
		if (event == AgentEvent.ToldToWait){
			state = AgentState.WaitingToPay;
			return true;
		}
	}
	if (state == AgentState.WaitingToPay){
		if (event == AgentEvent.ReadyForPayment){
			payTheMan();
			state = AgentState.Paying;
			return true;
		}
	}
	if (state == AgentState.Paying){
		if (event == AgentEvent.PaymentRecieved){
			leaveRestaurant();
			state = AgentState.DoingNothing;
			return true;
		}
	}
	
	//done paying time to leave

	print("No scheduler rule fired, should not happen in FSM, event="+event+" state="+state);
	return false;
    }
    
    // *** ACTIONS ***
    
    /** Goes to the restaurant when the customer becomes hungry */
    private void goingToRestaurant() {
	print("Going to restaurant");
	guiCustomer.appearInWaitingQueue();
	host.msgIWantToEat(this);//send him our instance, so he can respond to us
	stateChanged();
    }
    
    private void goToPay(){
    print("Telling cashier im ready to pay");
    cashier.msgImReadyToPay(this);
    stateChanged();
    }
    
    private void payTheMan(){
    	money -= whatIOweTheMan.total;
    	print("Paying the cashier, i have " + money + " money left");
    	cashier.msgPayMyBill(this, whatIOweTheMan);
    }
    
    /** Starts a timer to simulate the customer thinking about the menu */
    private void makeMenuChoice(){
	print("Deciding menu choice...(3000 milliseconds)");
	timer.schedule(new TimerTask() {
	    public void run() {  
		msgDecided();	    
	    }},
	    3000);//how long to wait before running task
	stateChanged();
    }
    private void callWaiter(){
	print("I decided!");
	waiter.msgImReadyToOrder(this);
	stateChanged();
    }

    /** Picks a random choice from the menu and sends it to the waiter */
    private void orderFood(){
	int myChoice;
	String choice;
    	do{
    	myChoice = (int)(Math.random()*4);
		choice = menu.choices[myChoice];
    	} while (money < menu.prices[myChoice]);
    myFood = choice;
    print("Ordering the " + choice);
	waiter.msgHereIsMyChoice(this, choice);
	stateChanged();
    }
    
    private void repickFood(String oldChoice){
    	String choice = menu.choices[(int)(Math.random()*4)];
    	int myChoice;
    	do{
        	myChoice = (int)(Math.random()*4);
    		choice = menu.choices[myChoice];
        } while (money < menu.prices[myChoice] || choice.equals(oldChoice));
    	myFood= choice;
    	print("Reordering some new food, trying " + choice);
    	waiter.msgHereIsMyChoice(this, choice);
    	stateChanged();
    }

    /** Starts a timer to simulate eating */
    private void eatFood() {
	print("Eating for " + hungerLevel*1000 + " milliseconds.");
	timer.schedule(new TimerTask() {
	    public void run() {
		msgDoneEating();    
	    }},
	    getHungerLevel() * 1000);//how long to wait before running task
	stateChanged();
    }
    

    /** When the customer is done eating, he leaves the restaurant */
    private void leaveRestaurant() {
	print("Leaving the restaurant");
	guiCustomer.leave(); //for the animation
	if (waiter != null){
	waiter.msgDoneEatingAndLeaving(this);
	}
	isHungry = false;
	stateChanged();
	gui.setCustomerEnabled(this); //Message to gui to enable hunger button

	//hack to keep customer getting hungry. Only for non-gui customers
	if (gui==null) becomeHungryInAWhile();//set a timer to make us hungry.
    }
    
    /** This starts a timer so the customer will become hungry again.
     * This is a hack that is used when the GUI is not being used */
    private void becomeHungryInAWhile() {
	timer.schedule(new TimerTask() {
	    public void run() {  
		setHungry();		    
	    }},
	    15000);//how long to wait before running task
    }

    // *** EXTRA ***

    /** establish connection to host agent. 
     * @param host reference to the host */
    public void setHost(HostAgent host) {
		this.host = host;
    }
    
    public void setCashier(CashierAgent cashier){
    	this.cashier = cashier;
    }
    
    /** Returns the customer's name
     *@return name of customer */
    public String getName() {
	return name;
    }

    /** @return true if the customer is hungry, false otherwise.
     ** Customer is hungry from time he is created (or button is
     ** pushed, until he eats and leaves.*/
    public boolean isHungry() {
	return isHungry;
    }

    /** @return the hungerlevel of the customer */
    public int getHungerLevel() {
	return hungerLevel;
    }
    
    /** Sets the customer's hungerlevel to a new value
     * @param hungerLevel the new hungerlevel for the customer */
    public void setHungerLevel(int hungerLevel) {
	this.hungerLevel = hungerLevel; 
    }
    public GuiCustomer getGuiCustomer(){
	return guiCustomer;
    }
    
    /** @return the string representation of the class */
    public String toString() {
	return "customer " + getName();
    }

    
}

