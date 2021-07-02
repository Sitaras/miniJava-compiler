@.ShadowBaseField_vtable = global [0 x i8*] []

@.A_vtable = global [1 x i8*] [
i8* bitcast (i32 (i8*)* @A.getX to i8*)
]

@.B_vtable = global [1 x i8*] [
i8* bitcast (i32 (i8*)* @B.getX to i8*)
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
  %a = alloca i8*



  %_0 = call i8* @calloc(i32 1,i32 12)
  %_1 = bitcast i8* %_0 to i8***
  %_2 = getelementptr [1 x i8*], [1 x i8*]* @.A_vtable, i32 0, i32 0
  store i8** %_2, i8*** %_1
  store i8* %_0, i8** %a


  %_3 = load i8*, i8** %a
  %_4 = bitcast i8* %_3 to i8***
  %_5 = load i8**, i8*** %_4
  %_6 = getelementptr i8*, i8** %_5, i32 0
  %_7 = load i8*, i8** %_6
  %_8 = bitcast i8* %_7 to i32 (i8*)* 
  %_9 = call i32 %_8(i8* %_3)

  call void (i32) @print_int(i32 %_9)


  %_10 = call i8* @calloc(i32 1,i32 16)
  %_11 = bitcast i8* %_10 to i8***
  %_12 = getelementptr [1 x i8*], [1 x i8*]* @.B_vtable, i32 0, i32 0
  store i8** %_12, i8*** %_11
  store i8* %_10, i8** %a


  %_13 = load i8*, i8** %a
  %_14 = bitcast i8* %_13 to i8***
  %_15 = load i8**, i8*** %_14
  %_16 = getelementptr i8*, i8** %_15, i32 0
  %_17 = load i8*, i8** %_16
  %_18 = bitcast i8* %_17 to i32 (i8*)* 
  %_19 = call i32 %_18(i8* %_13)

  call void (i32) @print_int(i32 %_19)

  ret i32 0
}

define i32 @A.getX(i8* %this) {
  %_0 = getelementptr i8, i8* %this, i32 8
  %_1 = bitcast i8* %_0 to i32*
  %_2 = load i32, i32* %_1
  ret i32 %_2
}

define i32 @B.getX(i8* %this) {
  %_0 = getelementptr i8, i8* %this, i32 12
  %_1 = bitcast i8* %_0 to i32*
  store i32 1, i32* %_1

  %_2 = getelementptr i8, i8* %this, i32 12
  %_3 = bitcast i8* %_2 to i32*
  %_4 = load i32, i32* %_3
  ret i32 %_4
}

