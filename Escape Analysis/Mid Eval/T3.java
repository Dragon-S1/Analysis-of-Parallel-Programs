
class T3 {
	public static void main(String[] args) {
		try {
			 A x;
			 A y;
			 B z;
			 
			 x = new A();
			 y = new A();	
			 z = new B();
			 
			 x.start();
			 y.start();
			 
			 synchronized(z) {}
			 y.f = z;
			 
			 x.join();
			 y.join();
			 
			}catch (InterruptedException e) {} 
	}
}
	 
	class A extends Thread{
		static B f;
		
		public void run() {
			try {
				B a;
				a = new B();
				synchronized(a) {}
				f = a;
			}catch(Exception e) {}
		}
	}
	
	class B{
		B f;
	}