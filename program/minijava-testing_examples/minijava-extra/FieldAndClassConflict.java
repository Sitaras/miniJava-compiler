class FieldAndClassConflict {

    public static void main(String[] args){ 
	System.out.println(new B().B());
    }

}



class A {

    A A;

    public int B(){
	return 2;
    }
}

class B extends A{

    B A;

}