class P3 {
	public static void main(String[] args) {
		try {
			 A x;
			 P3 z;
			 boolean c;
			 boolean d;
			 
			 c = false;
			 d = true;
			 x = new A();
			 z = new P3();
			 x.flag = d;
			 x.start();
			 x.f1 = z;
			 x.flag = c;
			 x.notifyAll();
			 x.join();
			}catch (Exception e) {
					
			} 
	}
}
	 
class A extends Thread{
		P3 f1;
		boolean flag;
		
		public void run() {
			try {
				A a;
				P3 b;
				a = this;
				while(flag){
					a.wait();
				}
				b = new P3();
				a.f1 = b;
			}catch(Exception e) {
				
			}
		}
	}

