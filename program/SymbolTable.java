import java.util.*;

public class SymbolTable {
    // every class that declared has an "entry" in the following linked hash map, 
    // where all the information of this class is stored
    LinkedHashMap<String, ClassTable> table; // a linked hash map that has an "entry" (Key: class name - Item: ClassTable Object) for every class 

    public SymbolTable(){
        table = new LinkedHashMap<String, ClassTable>();
    } 
    
    public void classDeclaration(String className,String parentName) throws Exception {

        if (parentName!=null && table.get(parentName) == null)
            throw new Exception("Parent Class " + parentName + " has not been declared.");

        if (table.get(className) != null)
            throw new Exception("Class: " + className + " has already been declared.");
        
        // insert class at the symbol table
        ClassTable classTable = new ClassTable(className, parentName); // create the corresponding ClassTable object for this class
        table.put(className, classTable); // insert it at hash map
    }

    public void classInsertVars(String className,String idName, String idType) throws Exception{
        // insert class variable at the symbol table
        if (!table.get(className).vars.containsKey(idName)) {
            table.get(className).vars.put(idName, idType);
            table.get(className).allClassVars.put(idName, idType);
        } 
        else
            throw new Exception("Variable: " + idType + "has already been declared in class " + className);

    }

    public void MethodDeclaration(String className,String methodName,String methodType) throws Exception{
        if (table.get(className).methods.containsKey(methodName))
            throw new Exception("Method: " + methodName + " has already been declared.");

        // insert method at the symbol table
        MethodTable methodTable = new MethodTable(methodName, methodType); // create the corresponding MethodTable object for the main method
        table.get(className).methods.put(methodName, methodTable); // insert it at hash map
        table.get(className).methodsVtable.put(methodName, methodTable); // insert it at hash map
    }

    public void MethoInsertVars(String className,String methodName, String idName, String idType) throws Exception {
        // insert method variable at the symbol table
        if (!table.get(className).methods.get(methodName).vars.containsKey(idName)) {
            table.get(className).methods.get(methodName).vars.put(idName, idType);
            table.get(className).methodsVtable.get(methodName).vars.put(idName, idType);
        } 
        else
            throw new Exception("Variable: " + idName + " has already been declared in method " + methodName);

    }

    public void copyMethods(String className,String parentName){
        LinkedHashMap<String, MethodTable> temp = new LinkedHashMap<String, MethodTable>(table.get(className).methodsVtable); // save the methods of the class
        table.get(className).methodsVtable.clear();
        for (String methodName : table.get(parentName).methodsVtable.keySet()) { // add at LinkedHashMap all the methods (with their arguments and local variables) that the class inherits from all the parent classes
            if(methodName.equals("main"))
                continue;

            String returnType = table.get(parentName).methodsVtable.get(methodName).methodType;
            MethodTable methodTable = new MethodTable(methodName, returnType);
            table.get(className).methodsVtable.put(methodName, methodTable);
            
            for (String argType : table.get(parentName).methodsVtable.get(methodName).args) {
                table.get(className).methodsVtable.get(methodName).args.add(argType);
            }
            for (String var : table.get(parentName).methodsVtable.get(methodName).vars.keySet()) {
                String type=table.get(parentName).methodsVtable.get(methodName).vars.get(var);
                table.get(className).methodsVtable.get(methodName).vars.put(var, type);  
            }
            for (String var : table.get(parentName).methodsVtable.get(methodName).argsNameType.keySet()) {
                String type=table.get(parentName).methodsVtable.get(methodName).argsNameType.get(var);
                table.get(className).methodsVtable.get(methodName).argsNameType.put(var, type);  
            }
        }
        table.get(className).methodsVtable.putAll(temp); // finally add at the end of the LinkedHashMap the methods of the class
 
    }

    public void copyVars(String className,String parentName){
        LinkedHashMap<String, String> temp = new LinkedHashMap<String, String>(table.get(className).allClassVars); // save the variables of the class
        table.get(className).allClassVars.clear();
        for (String classVar : table.get(parentName).allClassVars.keySet()) { // add at LinkedHashMap all the variables that the class inherits from all the parent classes
            String type = table.get(parentName).allClassVars.get(classVar);
            table.get(className).allClassVars.put(classVar,type);
        }
        table.get(className).allClassVars.putAll(temp); // finally add at the end of the LinkedHashMap the variables of the class

    }

    public void MethoInsertVarsPars(String className,String methodName, String idName, String idType) throws Exception {
        // insert method argument at the symbol table
        if (!table.get(className).methods.get(methodName).vars.containsKey(idName)) {
            table.get(className).methods.get(methodName).vars.put(idName, idType);  // add at variables list of method all the arguments
            table.get(className).methods.get(methodName).argsNameType.put(idName, idType);  // add at variables list of method all the arguments
            table.get(className).methods.get(methodName).args.add(idType); // add at arguments list the arguments types
            
            table.get(className).methodsVtable.get(methodName).vars.put(idName, idType);  // add at variables list of method all the arguments
            table.get(className).methodsVtable.get(methodName).argsNameType.put(idName, idType);  // add at variables list of method all the arguments
        } 
        else
            throw new Exception("Variable: " + idName + " has already been declared in method " + methodName);

    }

    public boolean isClass(String className){
        if (className != null)
            return table.containsKey(className);
        else
            return false;
    }
    
    public boolean extendsClass(String className){
        if(isClass(className))
            return isClass(table.get(className).parentName);
        else
            return false;
    }
    
    public boolean classMethod(String className, String methodName){
        if (isClass(className) && table.get(className).methods.containsKey(methodName))
                return true;
        return false;
    }

    public String getDeclarationClassOfVariable(String className, String varName){
        // try to find (if the class is derived) the parent class name that the given variable declared
        if (extendsClass(className)) { // check if class has a parent class
            String parentClassName = table.get(className).parentName; // then store the name of the parent class
            if (table.get(parentClassName).vars.containsKey(varName)){
                return parentClassName;
            }
            else
                return getDeclarationClassOfVariable(parentClassName, varName);
        }
        return null;
    }
          
    public String getDeclarationClassOfMethod(String className, String methodName){
        // try to find (if the class is derived) the parent class name that the given method declared
        if (extendsClass(className)) { // check if class has a parent class
            String parentClassName = table.get(className).parentName; // then store the name of the parent class
            if (table.get(parentClassName).methods.containsKey(methodName)) {
                return parentClassName;
            } 
            else
                return getDeclarationClassOfMethod(parentClassName, methodName);
        }
        return null;
    }

    public String getAllMethodsForTheVtable(String className,String currentClass){
        // at the given class name add all methods that inherits from his parent classes
        String parentClassName;
        if (extendsClass(currentClass)) { // check if class has a parent class
            parentClassName = table.get(currentClass).parentName; // then store the name of the parent class
            copyMethods(className, parentClassName);
            return getAllMethodsForTheVtable(className,parentClassName);
        }
        return null;
    }

    public String getAllVariables(String className,String currentClass){
        // at the given class name add all variables that inherits from his parent classes
        String parentClassName;
        if (extendsClass(currentClass)) { // check if class has a parent class
            parentClassName = table.get(currentClass).parentName; // then store the name of the parent class
            copyVars(className, parentClassName);
            return getAllVariables(className,parentClassName);
        }
        return null;
    }

    public boolean sameTypes(String left, String right) {
        if (left.equals(right)){
            return true;
        }
        else if (extendsClass(right)){
            return sameTypes(left, table.get(right).parentName);
        }
        return false;
    }

    public boolean isStringInt(String s) {
        // check if a string is integer literal
        try {
            Integer.parseInt(s);
            return true;
        } catch (NumberFormatException ex) {
            return false;
        }
    }

    public String getType(String className, String methodName, String expr) {
        String parentClassName;        
        if (classMethod(className,methodName) && table.get(className).methods.get(methodName).vars.containsKey(expr) ) { // for method's variable
            return table.get(className).methods.get(methodName).vars.get(expr);
        } 
        else if (isClass(className) && table.get(className).vars.containsKey(expr)) { // for class's variable
            return table.get(className).vars.get(expr);
        } 
        else if ((parentClassName=getDeclarationClassOfVariable(className, expr))!=null) { // for allClassVars class variable
            return table.get(parentClassName).vars.get(expr);
        } 
        else if (expr.equals("int") || expr.equals("int[]") || expr.equals("boolean") ){
            return expr;
        }
        else if (isStringInt(expr)){
            return "int";
        }
        else if (isClass(expr)){
            return expr;
        }
        else {
            return null;
        }
    }
    
}


class ClassTable {
    // to store for every class that declared: 
    // the class name, 
    // the parent class name (if it exists), 
    // the variables that has been declared at this,
    // and the methods
    String className;
    String parentName;
    LinkedHashMap<String, String> vars ; // a linked hash map that has an "entry" (Key: variable name - Item: variable type) for every variable of the class
    LinkedHashMap<String, String> allClassVars ; // a linked hash map that contains all the variables (with the inherited variables) of the class (Key: variable name - Item: variable type)
    LinkedHashMap<String, String> varsOffset ; // a linked hash map that has an "entry" (Key: variable name - Item: the corresponding offset) for every variable of the class
    LinkedHashMap<String, MethodTable> methods ; // a linked hash map that has an "entry" (Key: method name - Item: MethodTable Object) for every method of the class
    LinkedHashMap<String, MethodTable> methodsVtable ; // a linked hash map that contains all the methods (with the inherited methods) of the class (Key: method name - Item: MethodTable Object)
    LinkedHashMap<String, String> methodsOffset ; // a linked hash map that has an "entry" (Key: method name - Item: the corresponding offset) for every method of the class

    public ClassTable(String className, String parentName){
        vars = new LinkedHashMap<String, String>();
        allClassVars = new LinkedHashMap<String, String>();
        varsOffset = new LinkedHashMap<String, String>();
        methods = new LinkedHashMap<String, MethodTable>();
        methodsVtable = new LinkedHashMap<String, MethodTable>();
        methodsOffset = new LinkedHashMap<String, String>();
        this.className = className;
        this.parentName = parentName;
    }
    
}


class MethodTable {
    // to store for every method that declared in a class: 
    // the method name,
    // the method type, 
    // the parameters types, 
    // and the correspoding variables (including the arguments) that has been declared at this method
    String methodName;
    String methodType;
    List<String> args;
    LinkedHashMap<String, String> argsNameType; // a linked hash map to store all the method's arguments (Key: argument name - Item: argument type)
    HashMap<String,String> varNum; // a hash map to store all the local variables of the method (Key: variable name - Item: LLVM_name)
    LinkedHashMap<String, String> vars; // a linked hash map that has an "entry" (Key: variable name - Item: variable type) for every variable of the method

    public MethodTable(String name, String type) {
        args = new ArrayList<String>();
        argsNameType = new LinkedHashMap<String, String>();
        varNum = new HashMap<String, String>();
        vars = new LinkedHashMap<String, String>();
        this.methodName = name;
        this.methodType = type;
    }

}