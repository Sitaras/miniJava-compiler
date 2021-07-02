@.test07_vtable = global [0 x i8*] []

@.Operator_vtable = global [1 x i8*] [
i8* bitcast (i32 (i8*)* @Operator.compute to i8*)
]


declare i8* @calloc(i32, i32)
declare i32 @printf(i8*, ...)
declare void @exit(i32)

@_cint = constant [4 x i8] c"%d\0a\00"
@_cOOB = constant [15 x i8] c"Out of bounds\0a\00"
@_cNSZ = constant [15 x i8] c"Negative size\0a\00"
define void @print_int(i32 %i) {
  %_str = bitcast [4 x i8]* @_cint to i8*
  call i32 (i8*, ...) @printf(i8* %_str, i32 %i)
  ret void
}

define void @throw_oob() {
  %_str = bitcast [15 x i8]* @_cOOB to i8*
  call i32 (i8*, ...) @printf(i8* %_str)
  call void @exit(i32 1)
  ret void
}

define void @throw_nsz() {
  %_str = bitcast [15 x i8]* @_cNSZ to i8*
  call i32 (i8*, ...) @printf(i8* %_str)
  call void @exit(i32 1)
  ret void
}

define i32 @main() {

  %_0 = call i8* @calloc(i32 1,i32 19)
  %_1 = bitcast i8* %_0 to i8***
  %_2 = getelementptr [1 x i8*], [1 x i8*]* @.Operator_vtable, i32 0, i32 0
  store i8** %_2, i8*** %_1

  %_3 = bitcast i8* %_0 to i8***
  %_4 = load i8**, i8*** %_3
  %_5 = getelementptr i8*, i8** %_4, i32 0
  %_6 = load i8*, i8** %_5
  %_7 = bitcast i8* %_6 to i32 (i8*)* 
  %_8 = call i32 %_7(i8* %_0)

  call void (i32) @print_int(i32 %_8)

  ret i32 0
}

define i32 @Operator.compute(i8* %this) {
  %_0 = getelementptr i8, i8* %this, i32 10
  %_1 = bitcast i8* %_0 to i32*
  store i32 10, i32* %_1

  %_2 = getelementptr i8, i8* %this, i32 14
  %_3 = bitcast i8* %_2 to i32*
  store i32 20, i32* %_3

  %_4 = getelementptr i8, i8* %this, i32 10
  %_5 = bitcast i8* %_4 to i32*
  %_6 = getelementptr i8, i8* %this, i32 14
  %_7 = bitcast i8* %_6 to i32*
  %_8 = load i32, i32* %_5
  %_9 = load i32, i32* %_7
  %_10 = icmp slt i32 %_8, %_9
  %_11 = getelementptr i8, i8* %this, i32 18
  %_12 = bitcast i8* %_11 to i1*
  store i1 %_10, i1* %_12

  ret i32 0
}

