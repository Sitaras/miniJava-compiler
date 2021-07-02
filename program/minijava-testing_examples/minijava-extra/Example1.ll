@.Example1_vtable = global [0 x i8*] []

@.Test1_vtable = global [1 x i8*] [
i8* bitcast (i32 (i8*,i32,i1)* @Test1.Start to i8*)
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

  %_0 = call i8* @calloc(i32 1,i32 12)
  %_1 = bitcast i8* %_0 to i8***
  %_2 = getelementptr [1 x i8*], [1 x i8*]* @.Test1_vtable, i32 0, i32 0
  store i8** %_2, i8*** %_1

  %_3 = bitcast i8* %_0 to i8***
  %_4 = load i8**, i8*** %_3
  %_5 = getelementptr i8*, i8** %_4, i32 0
  %_6 = load i8*, i8** %_5
  %_7 = bitcast i8* %_6 to i32 (i8*,i32,i1)* 
  %_8 = call i32 %_7(i8* %_0, i32 5, i1 1)

  call void (i32) @print_int(i32 %_8)

  ret i32 0
}

define i32 @Test1.Start(i8* %this, i32 %.b, i1 %.c) {
  %b = alloca i32
  store i32 %.b, i32* %b
  %c = alloca i1
  store i1 %.c, i1* %c
  %ntb = alloca i1
  store i1 0, i1* %ntb


  %nti = alloca i32*


  %ourint = alloca i32
  store i32 0, i32* %ourint


  %_0 = load i32, i32* %b
  %_1 = add i32 1, %_0
  %_2 = icmp sge i32 %_1, 1
  br i1 %_2, label %nsz_ok_0, label %nsz_err_0

  nsz_err_0:
  call void @throw_nsz()
  br label %nsz_ok_0

  nsz_ok_0:
  %_3 = call i8* @calloc( i32 4, i32 %_1)
  %_4 = bitcast i8* %_3 to i32*
  store i32 %_0, i32* %_4

  store i32* %_4, i32** %nti

  %_5 = load i32*, i32** %nti
  %_6 = load i32, i32* %_5
  %_7 = icmp sge i32 0, 0
  %_8 = icmp slt i32 0, %_6
  %_9 = and i1 %_7, %_8
  br i1 %_9, label %oob_ok_1, label %oob_err_1

  oob_err_1:
  call void @throw_oob()
  br label %oob_ok_1

  oob_ok_1:
  %_10 = add i32 1, 0
  %_11 = getelementptr i32, i32* %_5, i32 %_10
  %_12 = load i32, i32* %_11

  store i32 %_12, i32* %ourint

  %_13= load i32, i32* %ourint
  call void (i32) @print_int(i32 %_13)

  %_14 = load i32*, i32** %nti
  %_15 = load i32, i32* %_14
  %_16 = icmp sge i32 0, 0
  %_17 = icmp slt i32 0, %_15
  %_18 = and i1 %_16, %_17
  br i1 %_18, label %oob_ok_2, label %oob_err_2

  oob_err_2:
  call void @throw_oob()
  br label %oob_ok_2

  oob_ok_2:
  %_19 = add i32 1, 0
  %_20 = getelementptr i32, i32* %_14, i32 %_19
  %_21 = load i32, i32* %_20

  ret i32 %_21
}

