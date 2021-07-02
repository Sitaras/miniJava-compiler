@.Classes_vtable = global [0 x i8*] []

@.Base_vtable = global [2 x i8*] [
i8* bitcast (i32 (i8*,i32)* @Base.set to i8*),
i8* bitcast (i32 (i8*)* @Base.get to i8*)
]

@.Derived_vtable = global [2 x i8*] [
i8* bitcast (i32 (i8*,i32)* @Derived.set to i8*),
i8* bitcast (i32 (i8*)* @Base.get to i8*)
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
  %b = alloca i8*


  %d = alloca i8*



  %_0 = call i8* @calloc(i32 1,i32 12)
  %_1 = bitcast i8* %_0 to i8***
  %_2 = getelementptr [2 x i8*], [2 x i8*]* @.Base_vtable, i32 0, i32 0
  store i8** %_2, i8*** %_1
  store i8* %_0, i8** %b


  %_3 = call i8* @calloc(i32 1,i32 8)
  %_4 = bitcast i8* %_3 to i8***
  %_5 = getelementptr [2 x i8*], [2 x i8*]* @.Derived_vtable, i32 0, i32 0
  store i8** %_5, i8*** %_4
  store i8* %_3, i8** %d

  %_6 = load i8*, i8** %d
  store i8* %_6, i8** %b


  %_7 = load i8*, i8** %b
  %_8 = bitcast i8* %_7 to i8***
  %_9 = load i8**, i8*** %_8
  %_10 = getelementptr i8*, i8** %_9, i32 0
  %_11 = load i8*, i8** %_10
  %_12 = bitcast i8* %_11 to i32 (i8*,i32)* 
  %_13 = call i32 %_12(i8* %_7, i32 1)

  call void (i32) @print_int(i32 %_13)


  %_14 = load i8*, i8** %b
  %_15 = bitcast i8* %_14 to i8***
  %_16 = load i8**, i8*** %_15
  %_17 = getelementptr i8*, i8** %_16, i32 0
  %_18 = load i8*, i8** %_17
  %_19 = bitcast i8* %_18 to i32 (i8*,i32)* 
  %_20 = call i32 %_19(i8* %_14, i32 3)

  call void (i32) @print_int(i32 %_20)

  ret i32 0
}

define i32 @Base.set(i8* %this, i32 %.x) {
  %x = alloca i32
  store i32 %.x, i32* %x
  %_0 = getelementptr i8, i8* %this, i32 8
  %_1 = bitcast i8* %_0 to i32*
  %_2 = load i32, i32* %x
  store i32 %_2, i32* %_1

  %_3 = getelementptr i8, i8* %this, i32 8
  %_4 = bitcast i8* %_3 to i32*
  %_5 = load i32, i32* %_4
  ret i32 %_5
}

define i32 @Base.get(i8* %this) {
  %_0 = getelementptr i8, i8* %this, i32 8
  %_1 = bitcast i8* %_0 to i32*
  %_2 = load i32, i32* %_1
  ret i32 %_2
}

define i32 @Derived.set(i8* %this, i32 %.x) {
  %x = alloca i32
  store i32 %.x, i32* %x
  %_0 = load i32, i32* %x
  %_2 = mul i32 %_0, 2
  %_3 = getelementptr i8, i8* %this, i32 8
  %_4 = bitcast i8* %_3 to i32*
  store i32 %_2, i32* %_4

  %_5 = getelementptr i8, i8* %this, i32 8
  %_6 = bitcast i8* %_5 to i32*
  %_7 = load i32, i32* %_6
  ret i32 %_7
}

