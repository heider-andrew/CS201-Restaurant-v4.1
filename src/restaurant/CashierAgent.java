package restaurant;

import java.util.ArrayList;
import java.util.List;

import agent.Agent;

public class CashierAgent extends Agent
{

	List<Bill> todaysBills = new ArrayList<Bill>();
	List<CustomerAgent> inLine = new ArrayList<CustomerAgent>();
	CustomerAgent currentCustomer;
	int totalMoney;
	String name = "Mr Moneybags (the cashier)";
	
	//Messages
	public void msgImReadyToPay(CustomerAgent c){
		inLine.add(c);
		makeCustomerWait(c);
		stateChanged();
	}
	
	public void msgHereIsABill(Bill b){
		todaysBills.add(b);
		stateChanged();
	}
	
	public void msgPayMyBill(CustomerAgent c, Bill b){
		thanksForTheCashBro(c,b);
		stateChanged();
	}
	
	//Scheduler

	@Override
	protected boolean pickAndExecuteAnAction()
	{
		if (!inLine.isEmpty()){
			currentCustomer = inLine.remove(0);
			print("Its " + currentCustomer + "'s turn to pay");
			currentCustomer.msgTimeToPay(this);
			return true;
		}
		
		return false;
	}
	
	//Actions
	private void makeCustomerWait(CustomerAgent c){
		print("Making " + c + " wait in line");
		c.msgGoStandInLine();
	}
	
	private void thanksForTheCashBro(CustomerAgent c, Bill b){
		print("Taking " + c + "'s money, the total is " + b.total);
		totalMoney += b.total;
		c.msgGetOuttaMyHouse();
	}
	
	public CashierAgent(){
		totalMoney = 0;
	}
}
