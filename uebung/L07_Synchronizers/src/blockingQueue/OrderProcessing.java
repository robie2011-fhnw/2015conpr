package blockingQueue;

import java.util.concurrent.LinkedBlockingQueue;

public class OrderProcessing {
	
	public static void main(String[] args) {
		int nCustomers = 10;
		int nValidators = 2;
		int nProcessors = 3;
		
		LinkedBlockingQueue<Order> newOrders = new LinkedBlockingQueue<Order>();
		LinkedBlockingQueue<Order> validOrders = new LinkedBlockingQueue<Order>();

		for (int i = 0; i < nCustomers; i++) {
			new Customer("" + i, newOrders).start();
		}

		for (int i = 0; i < nValidators; i++) {
			new OrderValidator(newOrders, validOrders).start();
		}

		for (int i = 0; i < nProcessors; i++) {
			new OrderProcessor(validOrders).start();
		}
	}
	
	static class Order {
		public final String customerName;
		public final int itemId;
		public Order(String customerName, int itemId) {
			this.customerName = customerName;
			this.itemId = itemId;
		}
		
		@Override
		public String toString() {
			return "Order: [name = " + customerName + " ], [item = " + itemId +" ]";  
		}
	}
	
	
	static class Customer extends Thread {
		private LinkedBlockingQueue<Order> ordersToValidate;

		public Customer(String name, LinkedBlockingQueue<Order> ordersToValidate) {
			super(name);
			this.ordersToValidate = ordersToValidate;
		}
		
		private Order createOrder() {
			Order o = new Order(getName(), (int) (Math.random()*100));
			System.out.println("Created:   " + o);
			return o;
		}
		
		private void handOverToValidator(Order o) throws InterruptedException {
			ordersToValidate.add(o);
			// TODO: Adding Timer for task 3)
		}
		
		@Override
		public void run() {
			try {
				while(true) {
					Order o = createOrder();
					handOverToValidator(o);
					Thread.sleep((long) (Math.random()*1000));
				}
			} catch (InterruptedException e) {}
		}
	}
	
	
	static class OrderValidator extends Thread {

		private LinkedBlockingQueue<Order> ordersToValidate;
		private LinkedBlockingQueue<Order> validOrders;

		public OrderValidator(LinkedBlockingQueue<Order> ordersToValidate, LinkedBlockingQueue<Order> validOrders) {
			this.ordersToValidate = ordersToValidate;
			this.validOrders = validOrders;
		}
		
		public Order getNextOrder() throws InterruptedException {
			return ordersToValidate.take();
		}
		
		public boolean isValid(Order o) {
			return o.itemId < 50;
		}
		
		public void handOverToProcessor(Order o) throws InterruptedException {
			validOrders.add(o);
		}
		
		@Override
		public void run() {
			try {
				while(true) {
					Order o = getNextOrder();
					if(isValid(o)) {
						handOverToProcessor(o);
					} else {
						System.err.println("Destroyed: " + o);
					}
					Thread.sleep((long) (Math.random()*1000));
				}
			} catch (InterruptedException e) {}
		}
	}
	
	
	static class OrderProcessor extends Thread {

		private LinkedBlockingQueue<Order> validOrders;

		public OrderProcessor(LinkedBlockingQueue<Order> validOrders) {
			this.validOrders = validOrders;
		}
		
		public Order getNextOrder() throws InterruptedException {
			return validOrders.take();
		}
		
		public void processOrder(Order o) {
			System.out.println("Processed: " + o);
		}
		
		@Override
		public void run() {
			try {
				while(true) {
					Order o = getNextOrder();
					processOrder(o);
					Thread.sleep((long) (Math.random()*1000));
				}
			} catch (InterruptedException e) {}
		}
	}
}
