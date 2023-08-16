class P2 {
	public static void main(String[] args) {
		try {
			 A x;
			 P2 z;
			 
			 x = new A();
			 z = new P2(); 
			 x.start();
			 synchronized(x){
				 x.f1 = z;
			 }
			 x.join();
			 
			}catch (Exception e) {
					
			} 
	}
}
	 
class A extends Thread{
		P2 f1;
		
		public void run() {
			try {
				A a;
				P2 b;
				a = this;
				b = new P2();
				a.f1 = b;
			}catch(Exception e) {
				
			}
		}
	}

