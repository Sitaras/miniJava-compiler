import syntaxtree.*;
import visitor.*;

public class FirstVisitor extends GJDepthFirst<String, Void>{

    static public SymbolTable symbolTable = new SymbolTable();
    // to know at what class and method I am in
    private String curClass;
    private String curMethod;


    /**
     * f0 -> "class"
     * f1 -> Identifier()
     * f2 -> "{"
     * f3 -> "public"
     * f4 -> "static"
     * f5 -> "void"
     * f6 -> "main"
     * f7 -> "("
     * f8 -> "String"
     * f9 -> "["
     * f10 -> "]"
     * f11 -> Identifier()
     * f12 -> ")"
     * f13 -> "{"
     * f14 -> ( VarDeclaration() )*
     * f15 -> ( Statement() )*
     * f16 -> "}"
     * f17 -> "}"
     */
    @Override
    public String visit(MainClass n, Void argu) throws Exception {

        String className = n.f1.accept(this, null);
        
        curClass = className;
        curMethod = "main";

        // insert class at the symbol table
        symbolTable.classDeclaration(className,null);

        // insert method main at the symbol table
        symbolTable.MethodDeclaration(className,curMethod,"void");

        super.visit(n, argu);
        
        curClass = null;  
        curMethod = null;  
        
        return null;
    }


    /**
     * f0 -> "class"
     * f1 -> Identifier()
     * f2 -> "{"
     * f3 -> ( VarDeclaration() )*
     * f4 -> ( MethodDeclaration() )*
     * f5 -> "}"
     */
    @Override
    public String visit(ClassDeclaration n, Void argu) throws Exception {

        String className = n.f1.accept(this, null);

        curClass = className;

        // insert class at the symbol table
        symbolTable.classDeclaration(className, null);

        super.visit(n, argu);

        curClass = null;

        return null;
    }


    /**
     * f0 -> "class"
     * f1 -> Identifier()
     * f2 -> "extends"
     * f3 -> Identifier()
     * f4 -> "{"
     * f5 -> ( VarDeclaration() )*
     * f6 -> ( MethodDeclaration() )*
     * f7 -> "}"
     */
    @Override
    public String visit(ClassExtendsDeclaration n, Void argu) throws Exception {
        String className = n.f1.accept(this, null);
        String parentName = n.f3.accept(this, null);

        curClass = className;

        // insert class at the symbol table
        symbolTable.classDeclaration(className, parentName);

        super.visit(n, argu);
        
        curClass = null;

        return null;
    }


    /**
     * f0 -> "public"
     * f1 -> Type()
     * f2 -> Identifier()
     * f3 -> "("
     * f4 -> ( FormalParameterList() )?
     * f5 -> ")"
     * f6 -> "{"
     * f7 -> ( VarDeclaration() )*
     * f8 -> ( Statement() )*
     * f9 -> "return"
     * f10 -> Expression()
     * f11 -> ";"
     * f12 -> "}"
     */
    @Override
    public String visit(MethodDeclaration n, Void argu) throws Exception {
        
        String methodType = n.f1.accept(this, null);
        String methodName = n.f2.accept(this, null);

        curMethod = methodName;

        // insert method at the symbol table
        symbolTable.MethodDeclaration(curClass,methodName,methodType);

        super.visit(n, argu);

        curMethod = null;

        return null;
    }


    /**
     * f0 -> Type()
     * f1 -> Identifier()
     */
    @Override
    public String visit(FormalParameter n, Void argu) throws Exception{
        String type = n.f0.accept(this, null);
        String id = n.f1.accept(this, null);

        // insert method argument at the symbol table
        symbolTable.MethoInsertVarsPars(curClass,curMethod,id,type);

        return null;
    }
    
    
    /**
     * f0 -> Type()
     * f1 -> Identifier()
     * f2 -> ";"
     */
    @Override
    public String visit(VarDeclaration n, Void argu) throws Exception {
        String type = n.f0.accept(this, argu);
        String id = n.f1.accept(this, argu);
        
        if(curMethod != null){
            // method variable
            // insert it at the symbol table
            symbolTable.MethoInsertVars(curClass,curMethod,id,type);
        }
        else{
            // class variable
            // insert it at the symbol table
            symbolTable.classInsertVars(curClass, id, type);
        }
        
        n.f2.accept(this, argu);
        
        return null;
        
    }


    @Override
    public String visit(ArrayType n, Void argu) {
        return "int[]";
    }
    

    @Override
    public String visit(BooleanType n, Void argu) {
        return "boolean";
    }
    

    @Override
    public String visit(IntegerType n, Void argu) {
        return "int";
    }
    

    @Override
    public String visit(Identifier n, Void argu) {
        return n.f0.toString();
    }
}
