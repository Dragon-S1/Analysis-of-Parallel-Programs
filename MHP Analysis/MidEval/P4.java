class P4 {
	public static void main(String[] args) {
		try {
			 A x;
			 P4 z;
			 boolean c;
			 boolean d;
			 
			 c = false;
			 d = true;
			 x = new A();
			 z = new P4();
			 x.flag = d;
			 x.start();
			 x.notifyAll();
			 x.f1 = z;
			 x.flag = c;
			 x.join();
			}catch (Exception e) {
					
			} 
	}
}
	 
class A extends Thread{
		P4 f1;
		boolean flag;
		
		public void run() {
			try {
				A a;
				P4 b;
				a = this;
				while(flag){
					a.wait();
				}
				b = new P4();
				a.f1 = b;
			}catch(Exception e) {
				
			}
		}
	}

