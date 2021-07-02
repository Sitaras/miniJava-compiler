@.DerivedCall_vtable = global [0 x i8*] []

@.A_vtable = global [0 x i8*] []
@.B_vtable = global [0 x i8*] []
@.F_vtable = global [1 x i8*] [
i8* bitcast (i32 (i8*,i8*)* @F.foo to i8*)
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
  %i = alloca i32
  store i32 0, i32* %i


  %b = alloca i8*


  %f = alloca i8*



  %_0 = call i8* @calloc(i32 1,i32 8)
  %_1 = bitcast i8* %_0 to i8***
  %_2 = getelementptr [1 x i8*], [1 x i8*]* @.F_vtable, i32 0, i32 0
  store i8** %_2, i8*** %_1
  store i8* %_0, i8** %f


  %_3 = call i8* @calloc(i32 1,i32 16)
  %_4 = bitcast i8* %_3 to i8***
  %_5 = getelementptr [0 x i8*], [0 x i8*]* @.B_vtable, i32 0, i32 0
  store i8** %_5, i8*** %_4
  store i8* %_3, i8** %b


  %_6 = load i8*, i8** %f
  %_7 = bitcast i8* %_6 to i8***
  %_8 = load i8**, i8*** %_7
  %_9 = getelementptr i8*, i8** %_8, i32 0
  %_10 = load i8*, i8** %_9
  %_11 = bitcast i8* %_10 to i32 (i8*,i8*)* 
  %_12 = load i8*, i8** %b
  %_13 = call i32 %_11(i8* %_6, i8* %_12)

  store i32 %_13, i32* %i

  %_14= load i32, i32* %i
  call void (i32) @print_int(i32 %_14)

  ret i32 0
}

define i32 @F.foo(i8* %this, i8* %.a) {
  %a = alloca i8*
  store i8* %.a, i8** %a
  ret i32 0
}

