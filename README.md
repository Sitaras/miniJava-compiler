## Mini Java Compiler

#### Parts 

Part 1: MiniJava Static Checking (Semantic Analysis)\n
Part 2: Generating intermediate code (MiniJava -> LLVM)

### Getting Started

In order to see that the output of the produced LLVM IR files is the same as compiling the input java file with javac and executing it with java, you will need Clang with version >=4.0.0.

#### In Ubuntu Trusty
```
sudo apt update && sudo apt install clang-4.0
```

#### Compilation command
```
make
```

### Execution

To perform semantic analysis on all files given as arguments:

```
java [MainClassName] [file1] [file2] ... [fileN]
```
To execute .ll files:

``` 
clang-4.0 -o out1 ex.ll
./out1
```

