class P1 {
	public static void main(String[] args) {
		try {
			 A x;
			 P1 z;
			 P1 y;
			 boolean c;
			 
			 c = true;
			 
			 x = new A();
			 z = new P1(); 
			 y = new P1(); 
			 if(c) {
				x = new A();
			 }
			 else {
				x = new A();
			 }
			 x.start();
			 
			 synchronized (x) {
				x.f = z;
			 }

			 x.join();
			}catch (Exception e) {
					
			} 
	}
}
	 
class A extends Thread{
		P1 f;
		
		public void run() {
			try {
				A a;
				P1 b;
				a = this;
				b = new P1();
				a.f = b;
				synchronized (a) {
					b = a.f;				
				}
			}catch(Exception e) {
				
			}
		}
	}

