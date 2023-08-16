public class P2 {
	void m1(){
		A x,y,z;
		x = new A();
		y = new A();
		z=y;
		z.f = x;
		boolean flag = true;
		if(flag){
			y=z.f;
		}
		y=z;
	}
}

class A{
	A f;
}
