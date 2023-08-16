
class T1 {
	public static void main(String[] args) {
		try {
			 A x;
			 
			 x = new A();
			 
			 x.start();
			 x.join();
			 
			}catch (InterruptedException e) {} 
	}
}
	 
	class A extends Thread{
		static B f;
		
		public void run() {
			try {
				B a;
				B b;
				
				a = new B();
				b = new B();
				
				synchronized(f) {
					f = a;
					f = b;
				}
				synchronized(a) {}
			}catch(Exception e) {}
		}
	}
	
	class B{
		B f;
	}