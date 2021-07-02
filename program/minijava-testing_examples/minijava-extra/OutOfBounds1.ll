@.OutOfBounds1_vtable = global [0 x i8*] []

@.A_vtable = global [1 x i8*] [
i8* bitcast (i32 (i8*)* @A.run to i8*)
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

  %_0 = call i8* @calloc(i32 1,i32 8)
  %_1 = bitcast i8* %_0 to i8***
  %_2 = getelementptr [1 x i8*], [1 x i8*]* @.A_vtable, i32 0, i32 0
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

define i32 @A.run(i8* %this) {
  %a = alloca i32*


  %_0 = add i32 1, 20
  %_1 = icmp sge i32 %_0, 1
  br i1 %_1, label %nsz_ok_0, label %nsz_err_0

  nsz_err_0:
  call void @throw_nsz()
  br label %nsz_ok_0

  nsz_ok_0:
  %_2 = call i8* @calloc( i32 4, i32 %_0)
  %_3 = bitcast i8* %_2 to i32*
  store i32 20, i32* %_3

  store i32* %_3, i32** %a

  %_4 = load i32*, i32** %a
  %_5 = load i32, i32* %_4
  %_6 = icmp sge i32 10, 0
  %_7 = icmp slt i32 10, %_5
  %_8 = and i1 %_6, %_7
  br i1 %_8, label %oob_ok_1, label %oob_err_1

  oob_err_1:
  call void @throw_oob()
  br label %oob_ok_1

  oob_ok_1:
  %_9 = add i32 1, 10
  %_10 = getelementptr i32, i32* %_4, i32 %_9
  %_11 = load i32, i32* %_10

  call void (i32) @print_int(i32 %_11)

  %_12 = load i32*, i32** %a
  %_13 = load i32, i32* %_12
  %_14 = icmp sge i32 40, 0
  %_15 = icmp slt i32 40, %_13
  %_16 = and i1 %_14, %_15
  br i1 %_16, label %oob_ok_2, label %oob_err_2

  oob_err_2:
  call void @throw_oob()
  br label %oob_ok_2

  oob_ok_2:
  %_17 = add i32 1, 40
  %_18 = getelementptr i32, i32* %_12, i32 %_17
  %_19 = load i32, i32* %_18

  ret i32 %_19
}

