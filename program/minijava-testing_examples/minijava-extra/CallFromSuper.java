class CallFromSuper {

    public static void main(String[] args){
        C c;
        int rv;
        c = new C();
        rv = c.foo();
        System.out.println(rv);
    }

}


class A {

    public int foo(){
        return 1;
    }

}


class B extends A {


}

class C extends B {


}
