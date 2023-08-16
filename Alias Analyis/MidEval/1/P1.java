public class P1 {
	void m1(){
		A x,y,z;
		x = new A();
		y = new A();
		z=y;
		boolean flag = true;
		if(flag){
			y = new A();
		}
		else{
			y = x;
		}
	}
}

class A{
	int a;
}
