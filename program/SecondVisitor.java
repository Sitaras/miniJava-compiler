import syntaxtree.*;
import visitor.*;
import java.util.*;

public class SecondVisitor extends GJDepthFirst<String, Void>{

    private SymbolTable symbolTable = FirstVisitor.symbolTable;
    // to know at what class and method I am in
    private String curClass; 
    private String curMethod;


    /**
     * f0 -> "class" 
     * f1 -> Identifier() 
     * f2 -> "{" 
     * f3 -> "public" 
     * f4 -> "static" 
     * f5-> "void" 
     * f6 -> "main" 
     * f7 -> "(" 
     * f8 -> "String" 
     * f9 -> "[" 
     * f10 -> "]" 
     * f11 ->Identifier() 
     * f12 -> ")"
     * f13 -> "{" 
     * f14 -> ( VarDeclaration() )* 
     * f15 -> (Statement() )* 
     * f16 -> "}" 
     * f17 -> "}"
     */
    @Override
    public String visit(MainClass n, Void argu) throws Exception {

        String className = n.f1.accept(this, null);

        curClass = className;
        curMethod = "main";

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

        curClass = className;

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
        String methodName = n.f2.accept(this, argu);
        String methodType = symbolTable.table.get(curClass).methods.get(methodName).methodType;
        
        curMethod = methodName;

        String expr = n.f10.accept(this, argu);
        String returnType = symbolTable.getType(curClass, curMethod, expr);

        
        super.visit(n, argu);

        if (!symbolTable.sameTypes(methodType,returnType)){ // check if the return value has the same type with the method's return type
            throw new Exception("At method " + methodName + ", the type of the return value doesn't match with the method's return type.");
        }
        
        String parentClassName;
        if ((parentClassName = symbolTable.getDeclarationClassOfMethod(curClass, methodName))!=null){ // check for method overriding
            // yes, store the parent name class, the method in child class is overriding method

            MethodTable parentMethodList= symbolTable.table.get(parentClassName).methods.get(methodName);
            List<String> methodArgsList= symbolTable.table.get(curClass).methods.get(methodName).args;
            
            if (!methodType.equals(parentMethodList.methodType)) // check if the both methods has the same type
            throw new Exception("Overriding method " + methodName + " hasn't the same return type with the parent class.");
            
            if (methodArgsList.size() != parentMethodList.args.size()) { // check if the both methods has the same number of arguments
                throw new Exception("Overriding method " + methodName + " hasn't the same number of arguments with the parent class.");
            } 
            
            for (int i = 0; i < methodArgsList.size(); i++)
                if (!methodArgsList.get(i).equals(parentMethodList.args.get(i))) // check if the both methods has the same arguments types
                    throw new Exception("Overriding method " + methodName + " hasn't the same arguments types with the parent class.");
            
        }
        
        
        curMethod = null;

        return null;
    }



    /**
     * f0 -> PrimaryExpression() 
     * f1 -> "." 
     * f2 -> Identifier() 
     * f3 -> "(" 
     * f4 -> (ExpressionList() )? 
     * f5 -> ")"
     */
    @Override
    public String visit(MessageSend n, Void argu) throws Exception {
        String expr = n.f0.accept(this, argu);
        String className = symbolTable.getType(curClass, curMethod, expr);
        String methodName = n.f2.accept(this, argu);

        if (className == null || !symbolTable.table.containsKey(className)) {
            throw new Exception("Class" + className + " has not been declared.");
        }

        String parentClassName;
        List<String> methodArgsList;
        String methodType;

        if ((parentClassName = symbolTable.getDeclarationClassOfMethod(className, methodName)) != null) {  // check for method overriding
            // yes, the method in child class is overriding method
            // store the method type and the arguments list of parent method
            methodType = symbolTable.table.get(parentClassName).methods.get(methodName).methodType;
            methodArgsList = symbolTable.table.get(parentClassName).methods.get(methodName).args;
        } else if (symbolTable.classMethod(className, methodName)) {
            // no
            // store the method type and the arguments list of the method
            methodType = symbolTable.table.get(className).methods.get(methodName).methodType;
            methodArgsList = symbolTable.table.get(className).methods.get(methodName).args;
        }
        // method has been not declared (at the class or at a parent class ) throw exception
        else {
            throw new Exception("The method " + methodName + " has not been declared.");
        }

        String[] args;
        args = null;
        String argsStrList = n.f4.accept(this, argu); // take the arguments from the method call

        if (argsStrList != null)
            args = argsStrList.split(",");

        if (argsStrList != null) {
            if (args.length != methodArgsList.size()) {
                throw new Exception("Method " + methodName + " hasn't the correct number of arguments.");
            }
        } 

        if (argsStrList != null)
            for (int i = 0; i < methodArgsList.size(); i++) { // check if the types of method call arguments is same with the corresponding method's argurments types
                String argType = symbolTable.getType(curClass, curMethod, args[i]);
                if (!symbolTable.sameTypes(methodArgsList.get(i), argType)) {
                    throw new Exception("The method " + methodName + " is not applicable for the arguments.");
                }
            }

        super.visit(n, argu);

        return methodType; // return the type of the method
    }
    
    
    /**
     * f0 -> AndExpression()
     *       | CompareExpression()
     *       | PlusExpression()
     *       | MinusExpression()
     *       | TimesExpression()
     *       | ArrayLookup()
     *       | ArrayLength()
     *       | MessageSend()
     *       | Clause()
     */

    @Override
    public String visit(Expression n, Void argu) throws Exception {
        return n.f0.accept(this, null);
    }


    /**
     * f0 -> Expression() 
     * f1 -> ExpressionTail()
     */
    public String visit(ExpressionList n, Void argu) throws Exception {

        String parList = n.f0.accept(this, null);
        if (n.f1 != null) {
            parList += n.f1.accept(this, null);
        }
        return parList;
    }
    

    /**
     * f0 -> ( ExpressionTerm() )*
     */
    public String visit(ExpressionTail n, Void argu) throws Exception {
        String toReturn = "";
        for (Node node : n.f0.nodes) {
            String id = node.accept(this, null);
            toReturn += "," + id;
        }

        return toReturn;
    }


    /**
     * f0 -> "," f1 -> Expression()
     */
    public String visit(ExpressionTerm n, Void argu) throws Exception {
        return n.f1.accept(this, argu);
    }


    /**
     * f0 -> "new" 
     * f1 -> Identifier() 
     * f2 -> "(" 
     * f3 -> ")"
     */
    @Override
    public String visit(AllocationExpression n, Void argu) throws Exception {
        
        String className = n.f1.accept(this, argu);
        
        if (symbolTable.table.get(className) == null) {
            throw new Exception("Class " + className + " has not been declared.");
        }
        
        super.visit(n, argu);

        return className;
    }
       

    /**
    * f0 -> "(" 
    * f1 -> Expression() 
    * f2 -> ")"
    */
    @Override
    public String visit(BracketExpression n, Void argu) throws Exception {
        String expr = n.f1.accept(this, argu);
        super.visit(n, argu);
        return expr; // just return the expression between the brackets
    }
    

    /**
    * f0 -> IntegerLiteral() | TrueLiteral() | FalseLiteral() | Identifier() |
    * ThisExpression() | ArrayAllocationExpression() | AllocationExpression() |
    * BracketExpression()
    */
    @Override
    public String visit(PrimaryExpression n, Void argu) throws Exception {
        String id = n.f0.accept(this, argu);
        if (id == null) {
            throw new Exception(id + " has not been declared.");
        }
        return id;
    }
    
    
    /**
     * f0 -> Identifier() 
     * f1 -> "[" 
     * f2 -> Expression() 
     * f3 -> "]" 
     * f4 -> "=" 
     * f5 -> Expression() 
     * f6 -> ";"
     */
    @Override
    public String visit(ArrayAssignmentStatement n, Void argu) throws Exception {
        String id = n.f0.accept(this, argu);
        String idType = symbolTable.getType(curClass, curMethod, id);

        if (idType == null)
            throw new Exception("Array " + id + " has not been declared.");

        if (!idType.equals("int[]"))
            throw new Exception("Array " + id + " is of type " + idType + " not int[].");

        String index = n.f2.accept(this, argu);
        String indexType = symbolTable.getType(curClass, curMethod, index);

        if (!indexType.equals("int"))
            throw new Exception("Index of array isn't int type.");

        String expr = n.f5.accept(this, argu);
        String exprType = symbolTable.getType(curClass, curMethod, expr);

        if (!exprType.equals("int"))
            throw new Exception("Assignment to array " + id + " isn't int type.");
        
        super.visit(n, argu);

        
        return null;
    }
    

    /**
     * f0 -> "new" 
     * f1 -> "int" 
     * f2 -> "[" 
     * f3 -> Expression() 
     * f4 -> "]"
     */
    @Override
    public String visit(ArrayAllocationExpression n, Void argu) throws Exception {
        
        String exprName = n.f3.accept(this, argu);
        String exprType = symbolTable.getType(curClass, curMethod, exprName);

        super.visit(n, argu);
        
        if (exprType==null || !exprType.equals("int")) {
            throw new Exception("Expression " + exprName + " inside in array [ ] isn't int type.");
        }
        
        return "int[]";
    }


    /**
     * f0 -> PrimaryExpression() 
     * f1 -> "[" 
     * f2 -> PrimaryExpression() 
     * f3 -> "]"
     */
    @Override
    public String visit(ArrayLookup n, Void argu) throws Exception {
        String array = n.f0.accept(this, argu);
        String index = n.f2.accept(this, argu);
        String arrayType = symbolTable.getType(curClass, curMethod, array);
        String indexType = symbolTable.getType(curClass, curMethod, index);

        super.visit(n, argu);

        if (!arrayType.equals("int[]")) 
            throw new Exception("The expression " + array +" isn't array type (int[])");
        else if (!indexType.equals("int")) 
            throw new Exception("The array index isn't int type");
        
        return "int";
    }
    
    /**
     * f0 -> PrimaryExpression() 
     * f1 -> "." 
     * f2 -> "length"
     */
    @Override
    public String visit(ArrayLength n, Void argu) throws Exception {
        String array = n.f0.accept(this, argu);
        String arrayType = symbolTable.getType(curClass, curMethod, array);

        super.visit(n, argu);

        if (arrayType == null)
            throw new Exception(arrayType + " has not been declared.");

        if (arrayType.equals("int[]"))
            return "int";
        else
            throw new Exception(array + " isn't array type (int[]).");
    }


    /**
     * f0 -> "!" 
     * f1 -> Clause()
     */
    @Override
    public String visit(NotExpression n, Void argu) throws Exception {
        String clause = n.f1.accept(this, argu);
        String clauseType = symbolTable.getType(curClass, curMethod, clause);

        if (clauseType == null || !clauseType.equals("boolean")) {
            throw new Exception("Clause type: " + clauseType + ", the ! opperator not followed by a boolean type clause.");
        }
        super.visit(n, argu);
        return clauseType;
    }
    
    /**
     * f0 -> Clause()
     * f1 -> "&&"
     * f2 -> Clause()
     */
    @Override
    public String visit(AndExpression n, Void argu) throws Exception {
        String leftExpr = n.f0.accept(this, argu);
        n.f1.accept(this, argu);
        String rightExpr = n.f2.accept(this, argu);

        String leftType = symbolTable.getType(curClass, curMethod, leftExpr);
        String rightType = symbolTable.getType(curClass, curMethod, rightExpr);

        if (leftType == null)
            throw new Exception(leftExpr + " has not been declared.");

        if (rightType == null)
            throw new Exception(rightExpr + " has not been declared.");

        if(!leftType.equals("boolean"))
            throw new Exception("Left expression " + leftExpr + " isn't boolean.");
        else if(!rightType.equals("boolean"))
            throw new Exception("Right expression " + rightExpr + " isn't boolean.");

         return "boolean";
    }


    /**
     * f0 -> PrimaryExpression() 
     * f1 -> "<"
     * f2 -> PrimaryExpression()
     */
    @Override
    public String visit(CompareExpression n, Void argu) throws Exception {
        String leftExpr = n.f0.accept(this, argu);
        n.f1.accept(this, argu);
        String rightExpr = n.f2.accept(this, argu);

        String leftType = symbolTable.getType(curClass, curMethod, leftExpr);
        String rightType = symbolTable.getType(curClass, curMethod, rightExpr);

        if (leftType == null)
            throw new Exception(leftExpr + " has not been declared.");

        if (rightType == null)
            throw new Exception(rightExpr + " has not been declared.");

        if (!leftType.equals("int"))
            throw new Exception("Left expression " + leftExpr + " isn't int type.");
        else if (!rightType.equals("int")) 
            throw new Exception("Right expression " + rightExpr + " isn't int type.");

        return "boolean";
    }


    /**
     * f0 -> PrimaryExpression() 
     * f1 -> "+" 
     * f2 -> PrimaryExpression()
     */
    @Override
    public String visit(PlusExpression n, Void argu) throws Exception {
        String leftExpr = n.f0.accept(this, argu);
        n.f1.accept(this, argu);
        String rightExpr = n.f2.accept(this, argu);
        
        String leftType = symbolTable.getType(curClass,curMethod, leftExpr);
        String rightType = symbolTable.getType(curClass,curMethod, rightExpr);

        if (leftType == null) 
            throw new Exception(leftExpr + " has not been declared.");

        if (rightType == null) 
            throw new Exception(rightExpr + " has not been declared.");

        if (!leftType.equals("int"))
            throw new Exception("Left expression " + leftExpr + " isn't int type.");
        else if (!rightType.equals("int"))
            throw new Exception("Right expression " + rightExpr + " isn't int type.");

        return "int";
    }


    /**
     * f0 -> PrimaryExpression() 
     * f1 -> "-" 
     * f2 -> PrimaryExpression()
     */
    @Override
    public String visit(MinusExpression n, Void argu) throws Exception {
        String leftExpr = n.f0.accept(this, argu);
        n.f1.accept(this, argu);
        String rightExpr = n.f2.accept(this, argu);

        String leftType = symbolTable.getType(curClass, curMethod, leftExpr);
        String rightType = symbolTable.getType(curClass, curMethod, rightExpr);

        if (leftType == null)
            throw new Exception(leftExpr + " has not been declared.");

        if (rightType == null)
            throw new Exception(rightExpr + " has not been declared.");

        if (!leftType.equals("int"))
            throw new Exception("Left expression " + leftExpr + " isn't int type.");
         else if (!rightType.equals("int"))
            throw new Exception("Right expression " + rightExpr + " isn't int type.");

        return "int";
    }


    /**
     * f0 -> PrimaryExpression() 
     * f1 -> "*" 
     * f2 -> PrimaryExpression()
     */
    @Override
    public String visit(TimesExpression n, Void argu) throws Exception {
        String leftExpr = n.f0.accept(this, argu);
        n.f1.accept(this, argu);
        String rightExpr = n.f2.accept(this, argu);

        String leftType = symbolTable.getType(curClass, curMethod, leftExpr);
        String rightType = symbolTable.getType(curClass, curMethod, rightExpr);

        if (leftType == null)
            throw new Exception(leftExpr + " has not been declared.");

        if (rightType == null)
            throw new Exception(rightExpr + " has not been declared.");

        if (!leftType.equals("int"))
            throw new Exception("Left expression is " + leftExpr + " type, not int type.");
        else if (!rightType.equals("int"))
            throw new Exception("Right expression is " + rightExpr + " type, not int type.");

        return "int";
    }




    /**
     * f0 -> "if" 
     * f1 -> "(" 
     * f2 -> Expression() 
     * f3 -> ")" 
     * f4 -> Statement() 
     * f5 ->"else"
     *  f6 -> Statement()
     */
    @Override
    public String visit(IfStatement n, Void argu) throws Exception {
        String expr = n.f2.accept(this, argu);
        String exprType = symbolTable.getType(curClass, curMethod, expr);
        
        super.visit(n, argu);

        if (exprType == null || !exprType.equals("boolean")) {
            throw new Exception("If expression isn't boolean type.");
        }

        return null;
    }


    /**
     * f0 -> "while" 
     * f1 -> "(" 
     * f2 -> Expression() 
     * f3 -> ")" 
     * f4 -> Statement()
     */
    @Override
    public String visit(WhileStatement n, Void argu) throws Exception {

        String expr = n.f2.accept(this, argu);
        String exprType = symbolTable.getType(curClass, curMethod, expr);

        super.visit(n, argu);

        if (exprType == null || !exprType.equals("boolean")) {
            throw new Exception("While expression isn't boolean type.");
        }

        return null;
    }


    /**
     * f0 -> "System.out.println" 
     * f1 -> "(" 
     * f2 -> Expression() 
     * f3 -> ")" 
     * f4 -> ";"
     */
    @Override
    public String visit(PrintStatement n, Void argu) throws Exception {
        String expr = n.f2.accept(this, argu);
        String exprType = symbolTable.getType(curClass, curMethod, expr);

        super.visit(n, argu);

        if (exprType == null || !exprType.equals("int")){
            throw new Exception("Expression at System.out.println(...) isn't int type.");
        }

        return null;
    }


    /**
     * f0 -> Identifier() 
     * f1 -> "=" 
     * f2 -> Expression() 
     * f3 -> ";"
     */
    @Override
    public String visit(AssignmentStatement n, Void argu) throws Exception {
        String id = n.f0.accept(this, argu);
        String idType = symbolTable.getType(curClass, curMethod, id);
        String expr = n.f2.accept(this, argu);
        String exprType = symbolTable.getType(curClass, curMethod, expr);
        
        super.visit(n, argu);

        if (idType == null) 
            throw new Exception(id + " has not been declared.");
        else if (!symbolTable.sameTypes(idType,exprType))
            throw new Exception("Try to assign at the id " + id + " which is type of " + idType + ", an expression that is is type of " + exprType );

        return null;
    }
    

    /**
     * f0 -> <INTEGER_LITERAL>
     */
    @Override
    public String visit(IntegerLiteral n, Void argu) throws Exception {
        n.f0.accept(this, argu);
        return "int";
    }   


    /**
     * f0 -> "true"
     */
    @Override
    public String visit(TrueLiteral n, Void argu) throws Exception {
        n.f0.accept(this, argu);
        return "boolean";
    }


    /**
     * f0 -> "false"
     */
    @Override
    public String visit(FalseLiteral n, Void argu) throws Exception {
        n.f0.accept(this, argu);
        return "boolean";
    }

    
    /**
     * f0 -> "this"
     */
    @Override
    public String visit(ThisExpression n, Void argu) throws Exception {
        n.f0.accept(this, argu);
        return curClass;
    }

    
    @Override
    public String visit(Identifier n, Void argu) {
        return n.f0.toString();
    }
    
}
