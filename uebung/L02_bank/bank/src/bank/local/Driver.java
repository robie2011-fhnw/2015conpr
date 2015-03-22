/*
 * Copyright (c) 2000-2015 Fachhochschule Nordwestschweiz (FHNW)
 * All Rights Reserved. 
 */

package bank.local;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Stream;

import bank.InactiveException;
import bank.OverdrawException;

public class Driver implements bank.BankDriver {
	private static Bank bank = null;

	static {		
		bank = new Bank();
	}
	
	@Override
	public void connect(String[] args) {
		System.out.println("connected...");
	}

	@Override
	public void disconnect() {
		//bank = null;
		System.out.println("disconnected...");
	}

	@Override
	public Bank getBank() {
		return bank;
	}

	static class Bank implements bank.Bank {
		private final Map<String, Account> accounts = new ConcurrentHashMap<String, Account>();
		
		{
			createAccount("Test1");
			createAccount("Test2");
		}
		
		@Override
		public Set<String> getAccountNumbers() {
			Set<String> accountNumbers = new HashSet<String>();
			
			accounts.values()
				.stream()
				.filter(account -> account.isActive())
				.forEach( account -> accountNumbers.add(account.getNumber()));
			
			return accountNumbers;
		}

		@Override
		public String createAccount(String owner) {						
			// final: enforce constructor to run before it returns reference to object
			final Account newAccount = new Account(owner); 
			
			accounts.put(owner, newAccount);			
			return newAccount.getNumber();
		}

		@Override
		public boolean closeAccount(String number) throws IOException {
			Account account = (Account) getAccount(number);
			
			synchronized (account) { // Correction: Use Locking on Account (not bank)				
				if(account == null 
					|| !account.isActive() 
					|| account.getBalance() > 0){				
					return false;
				}else{
					account.setActive(false);
					return true;
				}			
			}
		}

		@Override
		public bank.Account getAccount(String number) {
			//return accounts.get(number);
			
			if(accounts.values()
					.stream()
					.filter(account -> account.getNumber().equals(number))
					.count() == 0) {
				return null;
			}else{				
				return accounts.values()
						.stream()
						.filter(account -> account.getNumber().equals(number))
						.findFirst()
						.get();
			}
			
		}

		@Override		
		public void transfer(bank.Account from, bank.Account to, double amount)
				throws IOException, InactiveException, OverdrawException {

			bank.Account first, second;
			int nFrom = ((Driver.Account) from).accountNumber;
			int nTo = ((Driver.Account) to).accountNumber;
			if(nFrom < nTo){
				first = from;
				second = to;
			}else{
				first = to;
				second = from;
			}
			
			
			// Deadlock if tran(from,to), tran(to,from) called on same time
			// Deadlock avoiding: lock always the smallest accountNr first
			synchronized (first) {
				synchronized (second) {
					if(!from.isActive() || !to.isActive()) throw new InactiveException();
					if(from.getBalance() < amount) throw new OverdrawException();
					if(amount < 0) throw new IllegalArgumentException();
					
					((Account) from).balance -= amount;
					((Account) to).balance += amount;
				}								
			}	
			
		}

	}

	static class Account implements bank.Account {
		private static int index = 1; 
		
		synchronized // only one thread should increment this value and return the old one 
		private static int getNextAccountNr(){
			return index++;
		}
		
		private int accountNumber;
		private String number;
		private String owner;
		
		// volatile: these values can be changed. Changes should be written down to memory
		private volatile double balance;
		private volatile boolean active = true;

		Account(String owner) {
			this.owner = owner;
			this.accountNumber = getNextAccountNr();
			this.number = Integer.toString(this.accountNumber);
		}

		synchronized
		public void setActive(boolean flag){
			this.active = flag;
		}
		
		@Override
		public double getBalance() {
			return balance;
		}

		@Override
		public String getOwner() {
			return owner;
		}

		@Override
		public String getNumber() {
			return number;
		}

		@Override
		public boolean isActive() {
			return active;
		}

		@Override
		synchronized // multiple transaction on one account on the same time impossible
		public void deposit(double amount) throws InactiveException {
			if(!isActive()) throw new InactiveException();
			if(amount < 0) throw new IllegalArgumentException();
			balance += amount;
		}

		@Override
		synchronized // multiple transaction on one account on the same time impossible
		public void withdraw(double amount) throws InactiveException, OverdrawException {
			if(!isActive()) throw new InactiveException();
			if(getBalance() < amount) throw new OverdrawException();
			
			balance -= amount;
		}

	}

}