
class P1 {
	public static void main(String[] args) {
		try {
			 A x;
			 B y;
			 
			 x = new A();
			 y = new B();
			 
			 x.start();
			 
			 x.f = y;
			 
			 synchronized(y){
				 x.join(); 
			 } 
			}catch (InterruptedException e) {
				
			} 
	}
}
	 
	class A extends Thread{
		B f;
		
		public void run() {
			try {
				
			}catch(Exception e) {
				
			}
		}
	}
	
	class B{
		B f;
	}
