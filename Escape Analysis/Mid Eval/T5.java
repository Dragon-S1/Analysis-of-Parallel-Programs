
class T5 {
	public static void main(String[] args) {
		try {
			 B c;
			 B d;
			 
			 c = new B();
			 d = new B();
			 
			 c.f = d;
			 
			 synchronized(d) {}
			 
			}catch (InterruptedException e) {} 
	}
}
	 
	class A extends Thread{
		static B f;
		
		public void run() {
			try {
				B a;
				
				a = new B();
				
				synchronized(a.f) {}
				
			}catch(Exception e) {}
		}
	}
	
	class B{
		static B f;
	}