class P1{
	public static void main(String[] args){
		try{
			T27A a1;
			T27A a2;
			T27A a3;
			T27A a4;
			T27thread thread1;
			thread1 = new T27thread();
			a1 = new T27A();
			a2 = new T27A();
			a3 = new T27A();
			a2.f = a3;
			a2.foo();
			a4 = a2.f;
			thread1.tf = a4;
			synchronized(a1){
				System.out.println(a1);
			}

		}catch(Exception e){

		}
	}
}

class T27A{
	T27A f;
	public void foo(){
		T27A a1;
		T27A a2;
		T27thread thread1;
		thread1 = new T27thread();
		a1 = this;
		a2 = new T27A();
		thread1.tf = a1;
		synchronized(a2){
			System.out.println(a2);
		}
	}
}

class T27thread extends Thread{
	T27A tf;
	public void run(){
		try{

		}catch(Exception e){

		}
	}
}
