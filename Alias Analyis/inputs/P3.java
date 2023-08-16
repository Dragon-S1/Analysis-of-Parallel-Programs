public class P3 {
	void m1(){
		A x,y,z;
		x = new A();
		z = new A();
		x.f = z;
		y = x;
		x = x.f;
	}

	void m2(){
		A x,y,z;
		x = new A();
		boolean flag = true;
		if(flag){
			y = new A();
			x.f = y;
			y = x;
		}
		z = x.f;
	}
}

class A{
	A f;
}
