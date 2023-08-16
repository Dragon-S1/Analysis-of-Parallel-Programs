class P5 {
	public static void main(String[] args) {
		try {
			 A x;
			 P5 z;
			 boolean c;
			 boolean d;
			 
			 c = false;
			 d = true;
			 x = new A();
			 z = new P5();
			 x.flag = d;
			 x.start();
			 x.flag = c;
			 x.notifyAll();
			 x.f1 = z;
			 x.join();
			}catch (Exception e) {
					
			} 
	}
}
	 
class A extends Thread{
		P5 f1;
		boolean flag;
		
		public void run() {
			try {
				A a;
				P5 b;
				a = this;
				while(flag){
					a.wait();
				}
				b = new P5();
				a.f1 = b;
			}catch(Exception e) {
				
			}
		}
	}

